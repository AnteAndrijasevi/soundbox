package hr.andrijasevic.soundbox.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    private final RateLimitProperties props = new RateLimitProperties(true, 2, 2);
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(props);

    private MockHttpServletRequest authRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void allowsUpToCapacityThenReturns429() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // capacity is 2 → first two pass
        for (int i = 0; i < 2; i++) {
            filter.doFilter(authRequest("10.0.0.1"), new MockHttpServletResponse(), chain);
        }
        // third from the same IP is blocked
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequest("10.0.0.1"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentType()).startsWith("application/problem+json");
        assertThat(blocked.getContentAsString()).contains("Too Many Requests");
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bucketsArePerIp() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(authRequest("10.0.0.1"), new MockHttpServletResponse(), chain);
        filter.doFilter(authRequest("10.0.0.1"), new MockHttpServletResponse(), chain);
        // a different IP has its own fresh bucket
        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(authRequest("10.0.0.2"), other, chain);

        assertThat(other.getStatus()).isEqualTo(200);
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonAuthPathsAreNotLimited() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/albums/search");
        request.setRemoteAddr("10.0.0.1");
        // many calls, all pass (shouldNotFilter short-circuits)
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }
        verify(chain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
