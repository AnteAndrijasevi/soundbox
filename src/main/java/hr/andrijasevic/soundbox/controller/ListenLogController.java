package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.ListenLogDto;
import hr.andrijasevic.soundbox.dto.ListenLogRequest;
import hr.andrijasevic.soundbox.service.ListenLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class ListenLogController {

    private final ListenLogService listenLogService;

    public ListenLogController(ListenLogService listenLogService) {
        this.listenLogService = listenLogService;
    }

    @PostMapping("/api/albums/{mbid}/log")
    public ResponseEntity<ListenLogDto> logListen(
            @PathVariable String mbid,
            @RequestBody(required = false) ListenLogRequest request
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
}