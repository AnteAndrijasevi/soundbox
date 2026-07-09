package hr.andrijasevic.soundbox.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI soundboxOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Soundbox API")
                        .version("v1")
                        .description("""
                                A sonic memory log. Search albums, log listens with mood and context,
                                write reviews, follow other listeners, and curate lists.

                                Most endpoints require a JWT: register or log in via /api/auth,
                                then click Authorize and paste the token."""))
                // apply the bearer scheme globally so the Authorize button covers every operation
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .name(BEARER)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
