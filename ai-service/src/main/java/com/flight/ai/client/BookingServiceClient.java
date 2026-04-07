package com.flight.ai.client;

import com.flight.ai.dto.BookingApiResponse;
import com.flight.ai.dto.CreateBookingRequest;
import com.flight.ai.dto.SingleBookingApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "BOOKING-SERVICE")
public interface BookingServiceClient {

    @GetMapping("/bookings/my")
    BookingApiResponse getAllMyBookings(
            @RequestHeader("Authorization") String authorization);

    @GetMapping("/bookings/my/upcoming")
    BookingApiResponse getUpcomingBookings(
            @RequestHeader("Authorization") String authorization);

    @GetMapping("/bookings/my/completed")
    BookingApiResponse getCompletedBookings(
            @RequestHeader("Authorization") String authorization);

    @PostMapping("/bookings")
    SingleBookingApiResponse createBooking(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateBookingRequest request);
}
