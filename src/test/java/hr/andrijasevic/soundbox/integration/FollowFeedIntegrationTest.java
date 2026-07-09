package hr.andrijasevic.soundbox.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FollowFeedIntegrationTest extends BaseIntegrationTest {

    private String anteToken;
    private String veraToken;
    private long anteId;
    private long veraId;

    @BeforeEach
    void setUp() throws Exception {
        stubExternalAlbum();
        anteToken = registerAndGetToken("ante", "ante@example.com");
        veraToken = registerAndGetToken("vera", "vera@example.com");
        anteId = fetchMyId(anteToken);
        veraId = fetchMyId(veraToken);
    }

    private long fetchMyId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("id").asLong();
    }

    @Test
    void feed_showsReviewsFromFollowedUsersOnly() throws Exception {
        // vera reviews an album
        mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + veraToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":4.5,"text":"Prophetic."}
                                """))
                .andExpect(status().isOk());

        // before following: empty feed
        mockMvc.perform(get("/api/feed").header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // follow → review appears, with the reviewer's id for profile links
        mockMvc.perform(post("/api/users/" + veraId + "/follow")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/feed").header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("vera"))
                .andExpect(jsonPath("$.content[0].userId").value(veraId));

        // toggling again unfollows → feed empties
        mockMvc.perform(post("/api/users/" + veraId + "/follow")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/feed").header("Authorization", "Bearer " + anteToken))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void follow_updatesProfileCounts() throws Exception {
        mockMvc.perform(post("/api/users/" + veraId + "/follow")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + veraId).header("Authorization", "Bearer " + anteToken))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0));

        mockMvc.perform(get("/api/users/" + anteId).header("Authorization", "Bearer " + anteToken))
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(1));
    }

    @Test
    void follow_rejectsSelfFollow() throws Exception {
        mockMvc.perform(post("/api/users/" + anteId + "/follow")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot follow yourself"));
    }

    @Test
    void likeToggle_updatesLikeCount() throws Exception {
        String body = mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + veraToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":4.0,"text":"solid"}
                                """))
                .andReturn().getResponse().getContentAsString();
        long reviewId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(post("/api/reviews/" + reviewId + "/like")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(jsonPath("$.content[0].likeCount").value(1));

        mockMvc.perform(post("/api/reviews/" + reviewId + "/like")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + anteToken))
                .andExpect(jsonPath("$.content[0].likeCount").value(0));
    }
}
