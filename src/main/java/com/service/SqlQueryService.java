package com.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;

@Service
public class SqlQueryService {

    private final BedrockAgentRuntimeAsyncClient sqlAgentClient;
    private final AgentInvocationService agentInvocationService;

    @Value("${sql_agent_id}")
    private String sqlAgentId;
    @Value("${sql_agent_alias_id}")
    private String sqlAgentAliasId;

    public SqlQueryService(@Qualifier("sqlAgentClient") BedrockAgentRuntimeAsyncClient sqlAgentClient,
                           AgentInvocationService agentInvocationService) {
        this.sqlAgentClient = sqlAgentClient;
        this.agentInvocationService = agentInvocationService;
    }

    /**
     * Check if the user input is likely asking for a chart.
     */
    public boolean isChartRelatedQuery(String userInput) {
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("chart") || lowerInput.contains("graph") || lowerInput.contains("visualize")
                || lowerInput.contains("plot");
    }

    /**
     * Generate SQL query using the SQL Query Generation Agent.
     */
    public String generateSqlQuery(String userInput, String sessionId) {
        try {
            System.out.println("🔍 Generating SQL query for: " + userInput);
            String fullPrompt = userInput + "\n instructions: generate sql query only for above prompt";
            String agentResponse = agentInvocationService.invokeAgent(sqlAgentClient, sqlAgentId, sqlAgentAliasId, fullPrompt, sessionId, "SQL");
            String sqlQuery = extractSqlQuery(agentResponse);
            System.out.println("✅ Generated SQL Query: " + sqlQuery);
            return sqlQuery;
        } catch (Exception e) {
            System.err.println("❌ Error generating SQL query with agent: " + e.getMessage());
            e.printStackTrace();
            return "Error generating SQL: " + e.getMessage();
        }
    }

    /**
     * Extract SQL query from the agent's text response.
     */
    private String extractSqlQuery(String agentResponse) {
        String cleanedResponse = agentResponse.replaceAll("```sql|```", "").trim();
        String lowerResponse = cleanedResponse.toLowerCase();
        int selectIndex = lowerResponse.indexOf("select");
        if (selectIndex != -1) {
            return cleanedResponse.substring(selectIndex).split(";")[0].trim();
        }
        return cleanedResponse;
    }
}