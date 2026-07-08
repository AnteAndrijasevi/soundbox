package hr.andrijasevic.soundbox.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewFlowIntegrationTest extends BaseIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        stubExternalAlbum();
        token = registerAndGetToken("ante", "ante@example.com");
    }

    @Test
    void reviewAlbum_createsAlbumOnFirstReviewAndReturnsDto() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":4.5,"text":"Prophetic."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albumMbid").value(MBID))
                .andExpect(jsonPath("$.albumTitle").value("OK Computer"))
                .andExpect(jsonPath("$.username").value("ante"))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void reviewAlbum_upsertsInsteadOfDuplicating() throws Exception {
        String first = mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":2.0,"text":"meh"}
                                """))
                .andReturn().getResponse().getContentAsString();
        long firstId = objectMapper.readTree(first).get("id").asLong();

        mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":5.0,"text":"It grew on me."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.rating").value(5.0));

        mockMvc.perform(get("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].text").value("It grew on me."));
    }

    @Test
    void reviewAlbum_rejectsRatingAboveFive() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":5.5,"text":"too much"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewAlbum_rejectsRatingBelowHalf() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":0.4,"text":"too little"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewAlbum_rejectsMissingRating() throws Exception {
        mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"text":"no rating"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReview_ownerDeletes_otherUserCannot() throws Exception {
        String body = mockMvc.perform(post("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":4.0,"text":"solid"}
                                """))
                .andReturn().getResponse().getContentAsString();
        JsonNode review = objectMapper.readTree(body);
        long reviewId = review.get("id").asLong();

        String otherToken = registerAndGetToken("vera", "vera@example.com");
        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not authorized"));

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/albums/" + MBID + "/reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
