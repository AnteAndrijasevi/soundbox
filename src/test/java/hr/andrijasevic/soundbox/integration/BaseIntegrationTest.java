package hr.andrijasevic.soundbox.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.andrijasevic.soundbox.external.itunes.ITunesClient;
import hr.andrijasevic.soundbox.external.musicbrainz.MusicBrainzClient;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.ArtistCreditDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.ArtistDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzAlbumResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Shared setup for full-stack tests: real MVC + security + JPA on H2
 * (application-test.yml), with the external HTTP clients mocked out.
 * {@code @Transactional} rolls each test back so tests stay independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected static final String MBID = "11111111-1111-1111-1111-111111111111";
    protected static final String ARTIST_MBID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected MusicBrainzClient musicBrainzClient;

    @MockitoBean
    protected ITunesClient iTunesClient;

    /** Stubs MusicBrainz + iTunes so any album fetch resolves to a fixed test album. */
    protected void stubExternalAlbum() {
        ArtistDto artist = new ArtistDto();
        artist.setId(ARTIST_MBID);
        artist.setName("Radiohead");
        ArtistCreditDto credit = new ArtistCreditDto();
        credit.setArtist(artist);

        MusicBrainzAlbumResponse response = new MusicBrainzAlbumResponse();
        response.setId(MBID);
        response.setTitle("OK Computer");
        response.setDate("1997-06-16");
        response.setArtistCredit(List.of(credit));

        when(musicBrainzClient.getAlbum(MBID)).thenReturn(response);
        when(musicBrainzClient.getCoverArtUrl(MBID)).thenReturn(null);
        when(iTunesClient.findArtworkUrl(anyString(), anyString()))
                .thenReturn("https://itunes.example/600x600.jpg");
    }

    /** Registers a user through the real endpoint and returns a bearer token. */
    protected String registerAndGetToken(String username, String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"password123"}
                                """.formatted(username, email)))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }
}
