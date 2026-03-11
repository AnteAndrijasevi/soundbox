package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.ListItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ListItemRepository extends JpaRepository<ListItem, Long> {

    List<ListItem> findByUserListIdOrderByPosition(Long listId);

    boolean existsByUserListIdAndAlbumId(Long listId, Long albumId);

    @Transactional
    @Modifying
    void deleteByUserListIdAndAlbumId(Long listId, Long albumId);

    int countByUserListId(Long listId);
}
