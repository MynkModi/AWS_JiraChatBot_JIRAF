package com.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;

@Service
public class DefectService {

    private final BedrockAgentRuntimeAsyncClient defectAgentClient;
    private final AgentInvocationService agentInvocationService;

    @Value("${defect_agent_id}")
    private String defectAgentId;
    @Value("${defect_agent_alias_id}")
    private String defectAgentAliasId;

    public DefectService(@Qualifier("defectAgentClient") BedrockAgentRuntimeAsyncClient defectAgentClient,
                         AgentInvocationService agentInvocationService) {
        this.defectAgentClient = defectAgentClient;
        this.agentInvocationService = agentInvocationService;
    }

    /**
     * Generate defect recommendation using the Defect Recommendation Agent.
     */
    public String generateDefectRecommendation(String defectQuery, String sessionId) {
        try {
            System.out.println("🔍 Generating defect recommendation for: " + defectQuery);
            String fullPrompt = constructDefectPrompt(defectQuery);
            String agentResponse = agentInvocationService.invokeAgent(defectAgentClient, defectAgentId, defectAgentAliasId, fullPrompt, sessionId, "DEFECT");
            System.out.println("✅ Defect Recommendation Response received");
            return agentResponse;
        } catch (Exception e) {
            System.err.println("❌ Error generating defect recommendation: " + e.getMessage());
            e.printStackTrace();
            return "Error processing defect recommendation: " + e.getMessage();
        }
    }

    private String constructDefectPrompt(String defectQuery) {
        return defectQuery +
                "\n\nProvide response with following headers each with different paragraphs: " +
                "Matching Defect, Root Cause, Resolution";
    }
}