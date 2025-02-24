package fit.health.fithealthapi.configurations;

import fit.health.fithealthapi.agents.MealPlanAgent;
import fit.health.fithealthapi.services.RecipeService;
import fit.health.fithealthapi.services.UserService;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JadeConfig {

    private final RecipeService recipeService;
    private final UserService userService;
    private AgentContainer mainContainer;

    public JadeConfig(RecipeService recipeService, UserService userService) {
        this.recipeService = recipeService;
        this.userService = userService;
    }

    private void initializeJade() {
        System.out.println("START JADE!!!");
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");

        mainContainer = runtime.createMainContainer(profile);

        System.out.println("JADE!!! STARTED");

        startMealPlanAgent();
    }

    private void startMealPlanAgent() {
        try {
            // Create an instance of MealPlanAgent with services
            MealPlanAgent agentInstance = new MealPlanAgent();
            agentInstance.init(userService,recipeService);
            AgentController agent = mainContainer.acceptNewAgent("MealPlanAgent", agentInstance);
            agent.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
            System.err.println("Failed to start MealPlanAgent.");
        }
    }

    @Bean
    public AgentContainer agentContainer() {
        if (mainContainer == null) {
            initializeJade();  // Ensure JADE is started before returning the container
        }
        return mainContainer;
    }
}
