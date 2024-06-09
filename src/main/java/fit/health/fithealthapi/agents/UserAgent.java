package fit.health.fithealthapi.agents;


import jade.core.Agent;

public class UserAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("User Agent " + getLocalName() + " started.");
    }

    @Override
    protected void takeDown() {
        System.out.println("User Agent " + getLocalName() + " terminated.");
    }
}