package user_service.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user_service.user_service.dto.SavedPassengerRequest;
import user_service.user_service.dto.SavedPassengerResponse;
import user_service.user_service.exception.BadRequestException;
import user_service.user_service.exception.UserNotFoundException;
import user_service.user_service.model.SavedPassenger;
import user_service.user_service.repository.SavedPassengerRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedPassengerService {

    private static final int MAX_SAVED_PASSENGERS = 10;

    private final SavedPassengerRepository repository;

    // ─── List ──────────────────────────────────────────────────────────────────

    public List<SavedPassengerResponse> getAll(String username) {
        log.info("Fetching saved passengers for user: {}", username);
        return repository.findByUsernameOrderByCreatedAtAsc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Get one ───────────────────────────────────────────────────────────────

    public SavedPassengerResponse getOne(Long id, String username) {
        return toResponse(findOwned(id, username));
    }

    // ─── Add ───────────────────────────────────────────────────────────────────

    @Transactional
    public SavedPassengerResponse add(String username, SavedPassengerRequest request) {
        log.info("Adding saved passenger '{}' for user: {}", request.getLabel(), username);

        if (repository.countByUsername(username) >= MAX_SAVED_PASSENGERS) {
            throw new BadRequestException(
                    "Maximum of " + MAX_SAVED_PASSENGERS + " saved passengers allowed per account");
        }
        if (repository.existsByUsernameAndLabel(username, request.getLabel())) {
            throw new BadRequestException(
                    "A saved passenger with label '" + request.getLabel() + "' already exists");
        }

        SavedPassenger entity = SavedPassenger.builder()
                .username(username)
                .label(request.getLabel())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .age(request.getAge())
                .gender(request.getGender())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passportNumber(request.getPassportNumber())
                .build();

        SavedPassenger saved = repository.save(entity);
        log.info("Saved passenger id={} '{}' created for user: {}", saved.getId(), saved.getLabel(), username);
        return toResponse(saved);
    }

    // ─── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public SavedPassengerResponse update(Long id, String username, SavedPassengerRequest request) {
        log.info("Updating saved passenger id={} for user: {}", id, username);

        SavedPassenger entity = findOwned(id, username);

        // If label is being changed, check the new label is not a duplicate
        if (!entity.getLabel().equals(request.getLabel())
                && repository.existsByUsernameAndLabel(username, request.getLabel())) {
            throw new BadRequestException(
                    "A saved passenger with label '" + request.getLabel() + "' already exists");
        }

        entity.setLabel(request.getLabel());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setAge(request.getAge());
        entity.setGender(request.getGender());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setPassportNumber(request.getPassportNumber());

        return toResponse(repository.save(entity));
    }

    // ─── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String username) {
        log.info("Deleting saved passenger id={} for user: {}", id, username);
        SavedPassenger entity = findOwned(id, username);
        repository.delete(entity);
        log.info("Saved passenger id={} deleted for user: {}", id, username);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private SavedPassenger findOwned(Long id, String username) {
        return repository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new UserNotFoundException(
                        "Saved passenger not found with id: " + id));
    }

    private SavedPassengerResponse toResponse(SavedPassenger sp) {
        return SavedPassengerResponse.builder()
                .id(sp.getId())
                .label(sp.getLabel())
                .firstName(sp.getFirstName())
                .lastName(sp.getLastName())
                .age(sp.getAge())
                .gender(sp.getGender())
                .email(sp.getEmail())
                .phone(sp.getPhone())
                .passportNumber(sp.getPassportNumber())
                .createdAt(sp.getCreatedAt())
                .updatedAt(sp.getUpdatedAt())
                .build();
    }
}
