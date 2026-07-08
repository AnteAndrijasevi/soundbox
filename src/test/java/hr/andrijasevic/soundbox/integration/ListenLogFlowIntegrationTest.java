package hr.andrijasevic.soundbox.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListenLogFlowIntegrationTest extends BaseIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        stubExternalAlbum();
        token = registerAndGetToken("ante", "ante@example.com");
    }

    @Test
    void logListen_persistsFullContext() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":4.0,"mood":"NOSTALGIC","context":"NIGHT",
                                 "isFirstListen":true,"note":"2am spin.","favoriteTrack":"Let Down"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumTitle").value("OK Computer"))
                .andExpect(jsonPath("$.mood").value("NOSTALGIC"))
                .andExpect(jsonPath("$.context").value("NIGHT"))
                .andExpect(jsonPath("$.isFirstListen").value(true))
                .andExpect(jsonPath("$.note").value("2am spin."))
                .andExpect(jsonPath("$.favoriteTrack").value("Let Down"))
                // iTunes artwork flows through to the log entry
                .andExpect(jsonPath("$.albumCoverArtUrl").value("https://itunes.example/600x600.jpg"));
    }

    @Test
    void logListen_acceptsEmptyBody() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/log")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumMbid").value(MBID))
                .andExpect(jsonPath("$.rating").doesNotExist());
    }

    @Test
    void logListen_rejectsInvalidMood() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mood":"CONFUSED"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void myLog_listsEntriesNewestFirst() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mood":"CALM"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/albums/" + MBID + "/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mood":"EUPHORIC"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/log")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].mood").value("EUPHORIC"));

        // repeated listens of the same album do NOT create duplicate albums
        mockMvc.perform(get("/api/albums/" + MBID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("OK Computer"));
    }
}
