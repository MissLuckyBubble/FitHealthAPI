package fit.health.fithealthapi;

import fit.health.fithealthapi.services.OntologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fit.health.fithealthapi")
public class FitHealthApiApplication {
    public static void main(String[] args) {
        System.out.println("Before SpringApplication.run");
        try {
            SpringApplication.run(FitHealthApiApplication.class, args);
            System.out.println("SERVER running");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

