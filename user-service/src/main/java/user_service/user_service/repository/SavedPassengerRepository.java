package user_service.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import user_service.user_service.model.SavedPassenger;

import java.util.List;
import java.util.Optional;

public interface SavedPassengerRepository extends JpaRepository<SavedPassenger, Long> {

    List<SavedPassenger> findByUsernameOrderByCreatedAtAsc(String username);

    Optional<SavedPassenger> findByIdAndUsername(Long id, String username);

    boolean existsByUsernameAndLabel(String username, String label);

    long countByUsername(String username);
}
