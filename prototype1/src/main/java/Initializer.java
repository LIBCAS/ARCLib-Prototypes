import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = "cz.inqool.arclib")
@EntityScan(basePackages = "cz.inqool.arclib.domain")
@EnableJms
@EnableAsync
public class Initializer {

    public static void main(String[] args) {
        SpringApplication.run(Initializer.class, args);
    }
}
