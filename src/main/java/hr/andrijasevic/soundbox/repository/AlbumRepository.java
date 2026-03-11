package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByMbid(String mbid);
}
