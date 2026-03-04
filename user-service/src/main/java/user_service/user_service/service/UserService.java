package user_service.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user_service.user_service.dto.*;
import user_service.user_service.exception.BadRequestException;
import user_service.user_service.exception.UserNotFoundException;
import user_service.user_service.model.User;
import user_service.user_service.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Profile ───────────────────────────────────────────────────────────────

    public UserProfileResponse getMyProfile(String username) {
        log.info("Fetching profile for user: {}", username);
        User user = findByUsername(username);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", username);
        User user = findByUsername(username);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            log.debug("Email updated for user '{}'", username);
        }
        if (request.getFirstName()   != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()    != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        User saved = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", username);
        return toProfileResponse(saved);
    }

    // ─── Password ──────────────────────────────────────────────────────────────

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        log.info("Password change request for user: {}", username);
        User user = findByUsername(username);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed — wrong current password for user: {}", username);
            throw new BadRequestException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", username);
    }

    // ─── Account ───────────────────────────────────────────────────────────────

    @Transactional
    public void deactivateAccount(String username) {
        log.info("Deactivating account for user: {}", username);
        User user = findByUsername(username);

        if (!user.isActive()) {
            throw new BadRequestException("Account is already deactivated");
        }
        user.setActive(false);
        userRepository.save(user);
        log.info("Account deactivated for user: {}", username);
    }

    // ─── Admin — User Management ───────────────────────────────────────────────

    public List<UserProfileResponse> getAllUsers() {
        log.info("Admin: fetching all users");
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    public UserProfileResponse getUserById(Long id) {
        log.info("Admin: fetching user by id: {}", id);
        return toProfileResponse(findById(id));
    }

    @Transactional
    public UserProfileResponse updateUserRole(Long id, RoleUpdateRequest request) {
        log.info("Admin: updating role to '{}' for userId: {}", request.getRole(), id);
        User user = findById(id);
        user.setRole(request.getRole());
        User saved = userRepository.save(user);
        log.info("Role updated to '{}' for user: {}", request.getRole(), user.getUsername());
        return toProfileResponse(saved);
    }

    @Transactional
    public void activateUser(Long id) {
        log.info("Admin: activating userId: {}", id);
        User user = findById(id);
        if (user.isActive()) {
            throw new BadRequestException("Account is already active");
        }
        user.setActive(true);
        userRepository.save(user);
        log.info("Account activated for user: {}", user.getUsername());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
