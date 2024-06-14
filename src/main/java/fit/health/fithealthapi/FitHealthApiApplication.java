package fit.health.fithealthapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fit.health.fithealthapi")
public class FitHealthApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitHealthApiApplication.class, args);
    }
}
