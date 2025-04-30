package fit.health.fithealthapi.configurations;

import fit.health.fithealthapi.agents.MealPlanAgent;
import fit.health.fithealthapi.agents.MealScoringAgent;
import fit.health.fithealthapi.services.*;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JadeConfig {

    public JadeConfig(UserService userService, MealService mealService, SharedService sharedService, UserPreferenceService userPreferenceService) {
        this.userService = userService;
        this.mealService = mealService;
        this.sharedService = sharedService;
        this.userPreferenceService = userPreferenceService;
    }

    private final UserService userService;
    private final MealService mealService;
    private final SharedService sharedService;
    private final UserPreferenceService userPreferenceService;
    private AgentContainer mainContainer;

    private void initializeJade() {
        System.out.println("START JADE!!!");
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");

        mainContainer = runtime.createMainContainer(profile);

        System.out.println("JADE!!! STARTED");

        startMealPlanAgent();
        startMealScoringAgent();
    }

    private void startMealPlanAgent() {
        try {
            // Create an instance of MealPlanAgent with services
            MealPlanAgent agentInstance = new MealPlanAgent();
            agentInstance.init(userService,mealService,sharedService);
            AgentController agent = mainContainer.acceptNewAgent("MealPlanAgent", agentInstance);
            agent.start();
        } catch (StaleProxyException e) {
            System.err.println("Failed to start MealPlanAgent.");
        }
    }

    private void startMealScoringAgent() {
        try {
            // Create an instance of MealPlanAgent with services
            MealScoringAgent agentInstance = new MealScoringAgent();
            agentInstance.init(userPreferenceService);
            AgentController agent = mainContainer.acceptNewAgent("MealScoringAgent", agentInstance);
            agent.start();
        } catch (StaleProxyException e) {
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
