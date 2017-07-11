import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;

/**
 * Main application entry. Fires up Spring boot initialization.
 */
@SpringBootApplication
@ComponentScan(basePackages = "cz.inqool.arclib")
@EntityScan(basePackages = "cz.inqool.arclib.domain")
@EnableAsync
public class Initializer {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(Initializer.class, args);
    }
}
