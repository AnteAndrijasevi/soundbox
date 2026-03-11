package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.UserList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserListRepository extends JpaRepository<UserList, Long> {

    List<UserList> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserList> findByIdAndUserId(Long id, Long userId);
}
