package cz.inqool.arclib.bpm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BpmConfiguration {
    @Bean
    public ScriptInjector ingestServiceInjector(IngestBpmService service) {
        return new ScriptInjector() {
            @Override
            public String getName() {
                return "ingest";
            }

            @Override
            public Object getObject() {
                return service;
            }
        };
    }
}
