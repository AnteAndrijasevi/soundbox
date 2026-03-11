package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.CreateListRequest;
import hr.andrijasevic.soundbox.dto.UserListDetailDto;
import hr.andrijasevic.soundbox.dto.UserListDto;
import hr.andrijasevic.soundbox.service.UserListService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UserListController {

    private final UserListService userListService;

    public UserListController(UserListService userListService) {
        this.userListService = userListService;
    }

    @PostMapping("/api/lists")
    public ResponseEntity<UserListDto> createList(@Valid @RequestBody CreateListRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userListService.createList(request, email));
    }

    @GetMapping("/api/users/{userId}/lists")
    public ResponseEntity<List<UserListDto>> getLists(@PathVariable Long userId) {
        return ResponseEntity.ok(userListService.getLists(userId));
    }

    @GetMapping("/api/lists/{listId}")
    public ResponseEntity<UserListDetailDto> getListDetail(@PathVariable Long listId) {
        return ResponseEntity.ok(userListService.getListDetail(listId));
    }

    @PostMapping("/api/lists/{listId}/albums/{mbid}")
    public ResponseEntity<UserListDetailDto> addAlbumToList(
            @PathVariable Long listId,
            @PathVariable String mbid,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = body != null ? body.get("note") : null;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userListService.addAlbumToList(listId, mbid, note, email));
    }

    @DeleteMapping("/api/lists/{listId}/albums/{mbid}")
    public ResponseEntity<Void> removeAlbumFromList(
            @PathVariable Long listId,
            @PathVariable String mbid
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userListService.removeAlbumFromList(listId, mbid, email);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/lists/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable Long listId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userListService.deleteList(listId, email);
        return ResponseEntity.noContent().build();
    }
}
