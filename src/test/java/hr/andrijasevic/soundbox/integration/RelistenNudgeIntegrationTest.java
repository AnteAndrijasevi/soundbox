package hr.andrijasevic.soundbox.integration;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListenLog;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListenLogRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelistenNudgeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private ListenLogRepository listenLogRepository;

    private Album saveAlbum(String mbid, String title) {
        Album album = new Album();
        album.setMbid(mbid);
        album.setTitle(title);
        return albumRepository.save(album);
    }

    private void log(User user, Album album, LocalDateTime when) {
        ListenLog log = new ListenLog();
        log.setUser(user);
        log.setAlbum(album);
        log.setListenedAt(when);
        listenLogRepository.save(log);
    }

    @Test
    void relistenNudges_returnsAlbumsLoggedAroundAYearAgoOnly() throws Exception {
        String token = registerAndGetToken("ante", "ante@example.com");
        User ante = userRepository.findByEmail("ante@example.com").orElseThrow();

        Album yearAgo = saveAlbum("11111111-1111-1111-1111-111111111111", "OK Computer");
        Album recent = saveAlbum("22222222-2222-2222-2222-222222222222", "Fresh Release");

        log(ante, yearAgo, LocalDateTime.now().minusDays(365)); // in the window
        log(ante, recent, LocalDateTime.now().minusDays(3));    // too recent — excluded

        mockMvc.perform(get("/api/users/me/relisten-nudges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].albumTitle").value("OK Computer"));
    }

    @Test
    void relistenNudges_dedupesRepeatedListensOfSameAlbum() throws Exception {
        String token = registerAndGetToken("ante", "ante@example.com");
        User ante = userRepository.findByEmail("ante@example.com").orElseThrow();
        Album album = saveAlbum("11111111-1111-1111-1111-111111111111", "OK Computer");

        log(ante, album, LocalDateTime.now().minusDays(366));
        log(ante, album, LocalDateTime.now().minusDays(364));

        mockMvc.perform(get("/api/users/me/relisten-nudges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)); // two listens, one album
    }

    @Test
    void relistenNudges_emptyWhenNothingLoggedAYearAgo() throws Exception {
        String token = registerAndGetToken("ante", "ante@example.com");

        mockMvc.perform(get("/api/users/me/relisten-nudges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
