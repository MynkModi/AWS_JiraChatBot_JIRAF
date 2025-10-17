package com.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A container class for all chat-related Data Transfer Objects (DTOs).
 * This class groups various DTOs used for communication between the frontend and backend
 * of the JIRAF chatbot application.
 */
public final class ChatDtos {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All DTOs are static nested classes.
     */
    private ChatDtos() {}

    /**
     * Represents a chat request sent from the client to the server.
     */
    public static class ChatRequest {
        private String message;
        private String sessionId;
        private Map<String, Object> metadata;

        /**
         * Default constructor for JSON deserialization.
         */
        public ChatRequest() {}

        /**
         * Constructs a ChatRequest with a message.
         * @param message The user's message.
         */
        public ChatRequest(String message) {
            this.message = message;
        }

        /**
         * Gets the message from the request.
         * @return The message string.
         */
        public String getMessage() { return message; }
        /**
         * Sets the message for the request.
         * @param message The message string.
         */
        public void setMessage(String message) { this.message = message; }

        /**
         * Gets the session ID associated with the request.
         * @return The session ID string.
         */
        public String getSessionId() { return sessionId; }
        /**
         * Sets the session ID for the request.
         * @param sessionId The session ID string.
         */
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * Represents a chat response sent from the server to the client.
     */
    public static class ChatResponse {
        private String response;
        private String type;
        private String sessionId;
        private String chartUrl;
        private String downloadUrl;
        private long timestamp;

        /**
         * Constructs a ChatResponse.
         * @param response The main text response.
         * @param type The type of response (e.g., "text", "chart", "summary", "error").
         * @param sessionId The session ID.
         * @param chartUrl Optional URL for a generated chart.
         * @param downloadUrl Optional URL for downloading full results.
         */
        public ChatResponse(String response, String type, String sessionId, String chartUrl, String downloadUrl) {
            this.response = response;
            this.type = type;
            this.sessionId = sessionId;
            this.chartUrl = chartUrl;
            this.downloadUrl = downloadUrl;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Gets the main text response.
         * @return The response string.
         */
        public String getResponse() { return response; }
        /**
         * Sets the main text response.
         * @param response The response string.
         */
        public void setResponse(String response) { this.response = response; }

        /**
         * Gets the type of the response.
         * @return The response type string.
         */
        public String getType() { return type; }
        /**
         * Sets the type of the response.
         * @param type The response type string.
         */
        public void setType(String type) { this.type = type; }

        /**
         * Gets the session ID.
         * @return The session ID string.
         */
        public String getSessionId() { return sessionId; }
        /**
         * Sets the session ID.
         * @param sessionId The session ID string.
         */
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        /**
         * Gets the chart URL.
         * @return The chart URL string, or null if not applicable.
         */
        public String getChartUrl() { return chartUrl; }
        /**
         * Sets the chart URL.
         * @param chartUrl The chart URL string.
         */
        public void setChartUrl(String chartUrl) { this.chartUrl = chartUrl; }

        /**
         * Gets the download URL for full results.
         * @return The download URL string, or null if not applicable.
         */
        public String getDownloadUrl() { return downloadUrl; }
        /**
         * Sets the download URL for full results.
         * @param downloadUrl The download URL string.
         */
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        /**
         * Gets the timestamp when the response was created.
         * @return The timestamp in milliseconds.
         */
        public long getTimestamp() { return timestamp; }
        /**
         * Sets the timestamp when the response was created.
         * @param timestamp The timestamp in milliseconds.
         */
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Represents a single message within a chat session, including sender and content.
     */
    public static class ChatMessage {
        private String sender;
        private String message;
        private long timestamp;

        /**
         * Constructs a ChatMessage.
         * @param sender The sender of the message (e.g., "user", "bot").
         * @param message The content of the message.
         */
        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Gets the sender of the message.
         * @return The sender string.
         */
        public String getSender() { return sender; }
        /**
         * Sets the sender of the message.
         * @param sender The sender string.
         */
        public void setSender(String sender) { this.sender = sender; }

        /**
         * Gets the message content.
         * @return The message content string.
         */
        public String getMessage() { return message; }
        /**
         * Sets the message content.
         * @param message The message content string.
         */
        public void setMessage(String message) { this.message = message; }

        /**
         * Gets the timestamp when the message was created.
         * @return The timestamp in milliseconds.
         */
        public long getTimestamp() { return timestamp; }
        /**
         * Sets the timestamp when the message was created.
         * @param timestamp The timestamp in milliseconds.
         */
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Represents an active chat session, storing its ID, message history, creation time, and last activity time.
     */
    public static class ChatSession {
        private String sessionId;
        private List<ChatMessage> messages;
        private long createdAt;
        private long lastActivity;

        /**
         * Constructs a new ChatSession.
         * @param sessionId The unique ID for this session.
         */
        public ChatSession(String sessionId) {
            this.sessionId = sessionId;
            this.messages = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
        }

        /**
         * Adds a new message to the session's history and updates the last activity timestamp.
         * @param sender The sender of the message (e.g., "user", "bot").
         * @param message The content of the message.
         */
        public void addMessage(String sender, String message) {
            messages.add(new ChatMessage(sender, message));
            this.lastActivity = System.currentTimeMillis();
        }

        /**
         * Gets the session ID.
         * @return The session ID string.
         */
        public String getSessionId() { return sessionId; }
        /**
         * Gets the list of messages in this session.
         * @return A list of ChatMessage objects.
         */
        public List<ChatMessage> getMessages() { return messages; }
        /**
         * Gets the timestamp when the session was created.
         * @return The creation timestamp in milliseconds.
         */
        public long getCreatedAt() { return createdAt; }
        /**
         * Gets the timestamp of the last activity in this session.
         * @return The last activity timestamp in milliseconds.
         */
        public long getLastActivity() { return lastActivity; }
    }

    /**
     * Stores data related to a large query result set, allowing it to be downloaded later.
     */
    public static class SummaryData {
        private String summaryId;
        private String originalQuery;
        private List<String> fullResults;
        private long createdAt;

        /**
         * Constructs a SummaryData object.
         * @param summaryId A unique ID for this summary.
         * @param originalQuery The original natural language query that generated these results.
         * @param fullResults The complete list of results.
         */
        public SummaryData(String summaryId, String originalQuery, List<String> fullResults) {
            this.summaryId = summaryId;
            this.originalQuery = originalQuery;
            this.fullResults = new ArrayList<>(fullResults);
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * Gets the unique ID of this summary.
         * @return The summary ID string.
         */
        public String getSummaryId() { return summaryId; }
        /**
         * Gets the original query that generated these results.
         * @return The original query string.
         */
        public String getOriginalQuery() { return originalQuery; }
        /**
         * Gets the full list of results.
         * @return A list of result strings.
         */
        public List<String> getFullResults() { return fullResults; }
        /**
         * Gets the timestamp when this summary data was created.
         * @return The creation timestamp in milliseconds.
         */
        public long getCreatedAt() { return createdAt; }
    }
}