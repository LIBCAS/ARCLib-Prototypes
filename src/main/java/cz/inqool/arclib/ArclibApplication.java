package cz.inqool.arclib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application entry. Fires up Spring boot initialization.
 */
@SpringBootApplication
@ComponentScan(basePackages = "cz.inqool.arclib")
@EnableAsync
public class ArclibApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArclibApplication.class, args);
	}
}
