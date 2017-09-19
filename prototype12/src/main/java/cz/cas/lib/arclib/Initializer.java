package cz.cas.lib.arclib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

/**
 * Main application entry. Fires up Spring boot initialization.
 */
@SpringBootApplication
@ComponentScan(basePackages = "cz.cas.lib.arclib")
@EntityScan(basePackages = "cz.cas.lib.arclib.domain")
@EnableScheduling
public class Initializer {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(Initializer.class, args);
    }
}
