package com.xzfg.app.model;

import com.xzfg.app.Application;

import java.util.LinkedList;
import java.util.List;

public class PingResults {
    private AgentSettings agentSettings;
    private List<String> commands = new LinkedList<>();

    public static PingResults parse(Application application, String pingResponse) throws Exception {
        PingResults results = new PingResults();
        results.agentSettings = AgentSettings.parse(application, pingResponse);
        results.commands = Commands.parse(pingResponse);

        return results;
    }

    public AgentSettings getAgentSettings() {
        return agentSettings;
    }

    public List<String> getCommands() {
        return commands;
    }
}
