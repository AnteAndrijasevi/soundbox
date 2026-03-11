package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.AlbumDto;
import hr.andrijasevic.soundbox.service.AlbumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<AlbumDto>> searchAlbums(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(albumService.searchAlbums(query, limit, offset));
    }

    @GetMapping("/{mbid}")
    public ResponseEntity<AlbumDto> getAlbum(@PathVariable String mbid) {
        return ResponseEntity.ok(albumService.getAlbum(mbid));
    }
}
