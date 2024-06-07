package fit.health.fithealthapi.configurations;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JadeConfig {
    @PostConstruct
    public void startJade() {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");

        AgentContainer mainContainer = runtime.createMainContainer(profile);

        try {
            AgentController userAgent = mainContainer.createNewAgent(
                    "UserAgent", "fit.health.fithealthapi.agents.UserAgent", null);
            userAgent.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
