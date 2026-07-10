package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.ListenLogDto;
import hr.andrijasevic.soundbox.dto.ListenLogRequest;
import hr.andrijasevic.soundbox.service.ListenLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Listen log", description = "Log listens and read relisten history (Then vs Now)")
@RestController
public class ListenLogController {

    private final ListenLogService listenLogService;

    public ListenLogController(ListenLogService listenLogService) {
        this.listenLogService = listenLogService;
    }

    @PostMapping("/api/albums/{mbid}/log")
    public ResponseEntity<ListenLogDto> logListen(
            @PathVariable String mbid,
            @Valid @RequestBody(required = false) ListenLogRequest request
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (request == null) request = new ListenLogRequest(null, null, null, null, null, null);
        return ResponseEntity.ok(listenLogService.logListen(mbid, request, email));
    }

    @GetMapping("/api/users/me/log")
    public ResponseEntity<Page<ListenLogDto>> getMyListenLog(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(listenLogService.getMyListenLog(email, pageable));
    }

    @GetMapping("/api/users/{userId}/log")
    public ResponseEntity<Page<ListenLogDto>> getListenLog(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(listenLogService.getListenLog(userId, pageable));
    }

    // "Then vs Now" — a user's listens of one album over time, oldest first.

    @GetMapping("/api/users/me/albums/{mbid}/history")
    public ResponseEntity<List<ListenLogDto>> getMyRelistenHistory(@PathVariable String mbid) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(listenLogService.getMyRelistenHistory(mbid, email));
    }

    @GetMapping("/api/users/{userId}/albums/{mbid}/history")
    public ResponseEntity<List<ListenLogDto>> getRelistenHistory(
            @PathVariable Long userId,
            @PathVariable String mbid
    ) {
        return ResponseEntity.ok(listenLogService.getRelistenHistory(userId, mbid));
    }

    // "One year ago" nudge — albums the user logged ~365 days ago, a retention hook.

    @GetMapping("/api/users/me/relisten-nudges")
    public ResponseEntity<List<ListenLogDto>> getMyRelistenNudges() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(listenLogService.getRelistenNudges(email));
    }
}