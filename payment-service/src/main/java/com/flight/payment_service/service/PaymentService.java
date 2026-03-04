package com.flight.payment_service.service;

import com.flight.payment_service.dto.InitiatePaymentRequest;
import com.flight.payment_service.dto.PaymentResponse;
import com.flight.payment_service.exception.BadRequestException;
import com.flight.payment_service.exception.PaymentNotFoundException;
import com.flight.payment_service.model.Payment;
import com.flight.payment_service.model.PaymentMethod;
import com.flight.payment_service.model.PaymentStatus;
import com.flight.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request, String userId) {
        log.info("Initiating payment: bookingRef={}, method={}, user={}",
                request.getBookingReference(), request.getPaymentMethod(), userId);

        if (paymentRepository.findByBookingId(request.getBookingId()).isPresent()) {
            Payment existing = paymentRepository.findByBookingId(request.getBookingId()).get();
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                throw new BadRequestException("Payment already successful for booking: "
                        + request.getBookingReference());
            }
        }

        String payRef = generatePaymentReference();
        Payment payment = Payment.builder()
                .paymentReference(payRef)
                .bookingId(request.getBookingId())
                .bookingReference(request.getBookingReference())
                .userId(userId)
                .amount(request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PROCESSING)
                .build();
        payment = paymentRepository.save(payment);

        GatewayResult result = simulateGateway(request.getPaymentMethod(), request.isSimulateFailure());

        payment.setStatus(result.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setTransactionId(result.transactionId());
        payment.setGatewayResponse(result.gatewayResponse());
        payment.setFailureReason(result.success() ? null : result.failureReason());
        payment = paymentRepository.save(payment);

        log.info("Payment {} -- status={}, txn={}", payRef, payment.getStatus(), result.transactionId());

        String bookingStatus = result.success() ? "CONFIRMED" : "PAYMENT_FAILED";
        bookingServiceClient.updateBookingStatus(
                request.getBookingReference(), payRef, bookingStatus);

        String message = result.success()
                ? "Payment successful! Booking " + request.getBookingReference() + " is CONFIRMED."
                : "Payment failed: " + result.failureReason();

        return toResponse(payment, message, bookingStatus);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id, String userId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        if (!payment.getUserId().equals(userId)) {
            throw new BadRequestException("Access denied to this payment");
        }
        return toResponse(payment, null, null);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByReference(String ref, String userId) {
        Payment payment = paymentRepository.findByPaymentReference(ref)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + ref));
        if (!payment.getUserId().equals(userId)) {
            throw new BadRequestException("Access denied to this payment");
        }
        return toResponse(payment, null, null);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByBookingId(Long bookingId, String userId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for booking: " + bookingId));
        if (!payment.getUserId().equals(userId)) {
            throw new BadRequestException("Access denied to this payment");
        }
        return toResponse(payment, null, null);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(p -> toResponse(p, null, null)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(p -> toResponse(p, null, null)).collect(Collectors.toList());
    }

    private GatewayResult simulateGateway(PaymentMethod method, boolean forceFailure) {
        if (forceFailure) {
            log.info("Simulating forced payment failure");
            return GatewayResult.failure(
                    "TXN-FAILED-" + System.currentTimeMillis(),
                    getFailureReason(method));
        }

        int successThreshold = switch (method) {
            case CREDIT_CARD  -> 92;
            case DEBIT_CARD   -> 88;
            case UPI          -> 90;
            case NET_BANKING  -> 82;
        };

        boolean success = new Random().nextInt(100) < successThreshold;
        String txnId = (success ? "TXN-" : "TXN-FAIL-") + System.currentTimeMillis();

        if (success) {
            log.info("Payment gateway: SUCCESS -- txnId={}", txnId);
            return GatewayResult.success(txnId,
                    "Payment authorized by " + methodGatewayName(method));
        } else {
            String reason = getFailureReason(method);
            log.warn("Payment gateway: FAILED -- reason={}", reason);
            return GatewayResult.failure(txnId, reason);
        }
    }

    private String getFailureReason(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD  -> "Card declined by issuing bank";
            case DEBIT_CARD   -> "Insufficient balance";
            case UPI          -> "UPI transaction limit exceeded";
            case NET_BANKING  -> "Net banking session expired";
        };
    }

    private String methodGatewayName(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD  -> "VISA/Mastercard Gateway";
            case DEBIT_CARD   -> "Bank Gateway";
            case UPI          -> "UPI Gateway";
            case NET_BANKING  -> "NetBanking Gateway";
        };
    }

    private String generatePaymentReference() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("PAY-");
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        String ref = sb.toString();
        return paymentRepository.existsByPaymentReference(ref) ? generatePaymentReference() : ref;
    }

    private PaymentResponse toResponse(Payment p, String message, String bookingStatus) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentReference(p.getPaymentReference())
                .bookingId(p.getBookingId())
                .bookingReference(p.getBookingReference())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod().name())
                .status(p.getStatus().name())
                .transactionId(p.getTransactionId())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .message(message)
                .bookingStatus(bookingStatus)
                .build();
    }

    private record GatewayResult(boolean success, String transactionId,
                                  String gatewayResponse, String failureReason) {
        static GatewayResult success(String txnId, String response) {
            return new GatewayResult(true, txnId, response, null);
        }
        static GatewayResult failure(String txnId, String reason) {
            return new GatewayResult(false, txnId, "DECLINED", reason);
        }
    }
}
