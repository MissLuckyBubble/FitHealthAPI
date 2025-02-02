package fit.health.fithealthapi.configurations;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JadeConfig {

    private AgentContainer mainContainer;

    @PostConstruct
    public void startJade() {
        new Thread(this::initializeJade).start();
    }

    private void initializeJade() {
        System.out.println("START JADE!!!");
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");

        mainContainer = runtime.createMainContainer(profile);

        System.out.println("JADE!!! STARTED");
    }

    @Bean
    public AgentContainer agentContainer() {
        return mainContainer;
    }
}
