package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    Optional<Artist> findByMbid(String mbid);
}
