package cz.cas.lib.arclib;

import cz.cas.lib.arclib.solr.SolrStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.inject.Inject;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "cz.cas.lib.arclib")
public class Initializer {

    @Inject static SolrStore s;

    public static void main(String[] args) {
        SpringApplication.run(Initializer.class, args);
    }
}
