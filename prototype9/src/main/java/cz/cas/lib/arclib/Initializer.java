package cz.cas.lib.arclib;

import lombok.extern.log4j.Log4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@Log4j
@SpringBootApplication
@ComponentScan(basePackages = "cz.cas.lib.arclib")
@EntityScan(basePackages = "cz.cas.lib.arclib")
public class Initializer {
    public static void main(String[] args) {
        SpringApplication.run(Initializer.class, args);
    }
}
