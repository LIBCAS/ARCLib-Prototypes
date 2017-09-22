package cz.cas.lib.arclib;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "cz.cas.lib")
@EntityScan(basePackages = "cz.cas.lib")
public class Initializer {

    public static void main(String[] args) {
        SpringApplication.run(Initializer.class, args);
    }
}
