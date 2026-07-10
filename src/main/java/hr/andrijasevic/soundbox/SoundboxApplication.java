package hr.andrijasevic.soundbox;

import hr.andrijasevic.soundbox.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class SoundboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoundboxApplication.class, args);
    }

}
