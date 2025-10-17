package com.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.ResponseStream;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service class for invoking AWS Bedrock Agents.
 * Provides a generic method to interact with Bedrock agents and retrieve their responses.
 */
@Service
public class AgentInvocationService {

    /**
     * Generic method to invoke a Bedrock Agent.
     *
     * @param client       The BedrockAgentRuntimeAsyncClient to use.
     * @param agentId      The agent ID.
     * @param agentAliasId The agent alias ID.
     * @param inputText    The input text/query.
     * @param sessionId    The session ID.
     * @param agentType    Type of agent for logging (e.g., "SQL", "DEFECT").
     * @return The agent's response as a string.
     * @throws Exception if the invocation fails.
     */
    public String invokeAgent(BedrockAgentRuntimeAsyncClient client, String agentId, String agentAliasId,
                              String inputText, String sessionId, String agentType) throws Exception {

        StringBuilder completeResponseTextBuffer = new StringBuilder();
        Map<String, String> sessionAttributes = new HashMap<>();
        sessionAttributes.put("session_id", sessionId);

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🤖 Invoking " + agentType + " Agent");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📝 Input: " + inputText);
        System.out.println("🆔 Agent ID: " + agentId);
        System.out.println("🏷️  Alias ID: " + agentAliasId);
        System.out.println("🔑 Session ID: " + sessionId);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        InvokeAgentRequest invokeAgentRequest = InvokeAgentRequest.builder()
                .agentId(agentId)
                .agentAliasId(agentAliasId)
                .sessionId(sessionId)
                .inputText(inputText)
                .sessionState(session -> session.sessionAttributes(sessionAttributes)).build();
       

        CompletableFuture<Void> agentInvocation = client.invokeAgent(invokeAgentRequest,
                new InvokeAgentResponseHandler() {
                    @Override
                    public void onEventStream(SdkPublisher<ResponseStream> publisher) {
                        publisher.subscribe(event -> {
                            if (event instanceof PayloadPart) {
                                PayloadPart payloadPart = (PayloadPart) event;
                                completeResponseTextBuffer.append(payloadPart.bytes().asUtf8String());
                            }
                        });
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        System.err.println("❌ Error in " + agentType + " agent invocation: " + throwable.getMessage());
                    }

                    @Override
                    public void complete() {
                        System.out.println("✅ " + agentType + " Agent invocation stream completed.");
                        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    }

					@Override
					public void responseReceived(InvokeAgentResponse response) {
						// TODO Auto-generated method stub
						
					}
                });

        agentInvocation.get(120, TimeUnit.SECONDS);

        String agentResponse = completeResponseTextBuffer.toString().trim();

        if (agentResponse.isEmpty()) {
            System.err.println("⚠️ Warning: " + agentType + " agent returned empty response");
            return "No response from " + agentType + " agent";
        }

        System.out.println("📤 Raw " + agentType + " agent response length: " + agentResponse.length() + " characters");

        return agentResponse;
    }
}