package com.controller;

import com.controller.ChatDtos.*;
import com.service.ChartAnalyzer;
import com.service.TextAnalyzer;
import com.service.DefectService;
import com.service.SqlQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for handling web chat interactions with the JIRAF chatbot.
 * Provides endpoints for chat messages, chart downloads, summary downloads, session management, and health checks.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WebChatController {

    private final TextAnalyzer textAnalyzer;
    private final ChartAnalyzer chartAnalyzer;
    private final SqlQueryService sqlQueryService;
    private final DefectService defectService;

    /**
     * Constructs a new WebChatController with the given services.
     * @param textAnalyzer Service for analyzing text queries and executing SQL.
     * @param chartAnalyzer Service for generating charts from SQL query results.
     * @param sqlQueryService Service for generating SQL queries from natural language.
     * @param defectService Service for handling defect-related queries and recommendations.
     */
    @Autowired
    public WebChatController(TextAnalyzer textAnalyzer, ChartAnalyzer chartAnalyzer, SqlQueryService sqlQueryService, DefectService defectService) {
        this.textAnalyzer = textAnalyzer;
        this.chartAnalyzer = chartAnalyzer;
        this.sqlQueryService = sqlQueryService;
        this.defectService = defectService;
    }

    // Session management for web users
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sessionCleanupExecutor = Executors.newScheduledThreadPool(1);
    
    // Rate limiting
    private final Map<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long RATE_LIMIT_WINDOW = 60000; // 1 minute in milliseconds
    
    // Summary storage for downloadable files
    private final Map<String, SummaryData> summaryStorage = new ConcurrentHashMap<>();
    private static final int SUMMARY_THRESHOLD = 50; // Show summary if more than 50 results

    /**
     * Initializes the controller after dependency injection. Starts the session cleanup task.
     */
    @PostConstruct
    public void initialize() {
        try {
            // Start session cleanup task (runs every 30 minutes)
            sessionCleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 30, 30, TimeUnit.MINUTES);
            
            System.out.println("🦒 JIRAF Web Chat Bot initialized successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing JIRAF Web Chat Bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handles incoming chat messages from the web client.
     * @param request The chat request containing the user's message.
     * @param sessionId The session ID, if available from the client.
     * @return A ResponseEntity containing the chat response.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> handleChatMessage(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        
        try {
            // Generate session ID if not provided
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = generateSessionId();
            }
            
            // Rate limiting check
            if (!isRequestAllowed(sessionId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ChatResponse("⚠️ Too many requests. Please wait a moment before sending another message.", 
                                         "error", sessionId, null, null));
            }
            
            // Get or create session
            ChatSession session = getOrCreateSession(sessionId);
            
            String query = request.getMessage().trim();
            if (query.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ChatResponse("❌ Message cannot be empty.", "error", sessionId, null, null));
            }
            
            // Add user message to session history
            session.addMessage("user", query);
            
            ChatResponse response;
            
            // Check if this is a defect recommendation query
            if (query.toLowerCase().startsWith("defect:")) {
                response = handleDefectRecommendationQuery(sessionId, query);
            } else {
                // Process as regular SQL query
                boolean isChartQuery = sqlQueryService.isChartRelatedQuery(query);
                String sqlQuery = sqlQueryService.generateSqlQuery(query, sessionId);
                
                if (isChartQuery) {
                    response = handleChartQuery(sessionId, sqlQuery, query, session);
                } else {
                    response = handleTextQuery(sessionId, sqlQuery, query, session);
                }
            }
            
            // Add bot response to session history
            session.addMessage("bot", response.getResponse());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing chat message: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChatResponse("❌ I encountered an error processing your request. Please try again.", 
                                     "error", sessionId, null, null));
        }
    }

    /**
     * Handles defect recommendation queries.
     * @param sessionId The current session ID.
     * @param query The user's query, starting with "defect:".
     * @return A ChatResponse containing the defect recommendation or an error message.
     * @throws Exception if an error occurs during defect recommendation generation.
     * Handle defect recommendation queries that start with "defect:"
     */
    private ChatResponse handleDefectRecommendationQuery(String sessionId, String query) {
        try {
            // Call the dedicated defect recommendation agent
            String defectRecommendation = defectService.generateDefectRecommendation(query, sessionId);
            
            // Check if there was an error
            if (defectRecommendation.startsWith("Error")) {
                return new ChatResponse("❌ " + defectRecommendation, "error", sessionId, null, null);
            }
            
            return new ChatResponse(defectRecommendation, "defect_recommendation", sessionId, null, null);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing defect recommendation query: " + e.getMessage());
            e.printStackTrace();
            
            return new ChatResponse("❌ Error processing defect recommendation. Please try again.", 
                                   "error", sessionId, null, null);
        }
    }
    
    
    /**
     * Downloads a generated chart image.
     * @param filename The name of the chart file to download.
     * @return A ResponseEntity containing the chart image as a resource.
     */
    @GetMapping("/chart/{filename}")
    public ResponseEntity<Resource> downloadChart(@PathVariable String filename) {
        try {
            System.out.println("🔍 Looking for chart file: " + filename);
            
            // Charts are now in a predictable "charts" subdirectory
            Path chartDir = Paths.get("charts").toAbsolutePath();
            Path filePath = chartDir.resolve(filename).normalize();

            // Security check: ensure file is within the intended directory
            if (!filePath.startsWith(chartDir)) {
                System.err.println("❌ Path traversal attempt blocked for: " + filename);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Resource resource = new FileSystemResource(filePath);
            
            if (resource == null || !resource.exists()) {
                System.err.println("❌ Chart file not found in any location: " + filename);
                System.err.println("📁 Current working directory: " + System.getProperty("user.dir"));
                
                // List files in current directory for debugging
                try {
                    Files.list(chartDir)
                        .filter(Files::isRegularFile)
                        .forEach(file -> System.out.println("📄 Available file: " + file.getFileName()));
                } catch (IOException e) {
                    System.err.println("Could not list files in current directory");
                }
                
                return ResponseEntity.notFound().build();
            }
            
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/png"; // Default to PNG for chart images
            }
            
            System.out.println("📊 Serving chart: " + filename + " with content type: " + contentType);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
                
        } catch (IOException e) {
            System.err.println("❌ Error serving chart file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Downloads a summary of query results as a text file.
     * @param summaryId The ID of the summary data to download.
     * @return A ResponseEntity containing the summary text file as a resource.
     */
    @GetMapping("/download/summary/{summaryId}")
    public ResponseEntity<Resource> downloadSummary(@PathVariable String summaryId) {
        try {
            SummaryData summaryData = summaryStorage.get(summaryId);
            if (summaryData == null) {
                System.err.println("❌ Summary not found: " + summaryId);
                return ResponseEntity.notFound().build();
            }
            
            // Create temporary file for download
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "jiraf_results_" + timestamp + ".txt";
            Path tempFile = Files.createTempFile("jiraf_summary_", ".txt");
            
            // Write full results to file
            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write("JIRAF Query Results\n");
                writer.write("==================\n\n");
                writer.write("Query: " + summaryData.getOriginalQuery() + "\n");
                writer.write("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("Total Results: " + summaryData.getFullResults().size() + "\n");
                writer.write("\n" + "=".repeat(80) + "\n\n");
                
                // Write headers
                if (!summaryData.getFullResults().isEmpty()) {
                    String firstResult = summaryData.getFullResults().get(0);
                    String[] headers = extractHeaders(firstResult);
                    
                    // Write formatted headers
                    for (String header : headers) {
                        writer.write(String.format("%-25s", header));
                    }
                    writer.write("\n");
                    writer.write("-".repeat(headers.length * 25) + "\n");
                    
                    // Write data rows
                    for (String result : summaryData.getFullResults()) {
                        String[] values = extractValues(result);
                        for (String value : values) {
                            writer.write(String.format("%-25s", truncateValue(value, 24)));
                        }
                        writer.write("\n");
                    }
                }
                
                writer.write("\n\n" + "=".repeat(80) + "\n");
                writer.write("End of Results\n");
                writer.write("Generated by JIRAF - AI Assistant for JIRA\n");
            }
            
            Resource resource = new FileSystemResource(tempFile.toFile());
            
            System.out.println("📁 Serving summary download: " + filename);
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
                
        } catch (Exception e) {
            System.err.println("❌ Error creating summary download: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves the chat history for a given session.
     * @param sessionId The ID of the session.
     * @return A ResponseEntity containing a list of ChatMessage objects, or 404 if the session is not found.
     */
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(session.getMessages());
    }

    /**
     * Clears and removes a chat session and its associated data.
     * @param sessionId The ID of the session to clear.
     * @return A ResponseEntity indicating the success of the operation.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        activeSessions.remove(sessionId);
        requestTimestamps.remove(sessionId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session cleared successfully");
        response.put("sessionId", sessionId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Provides a health check endpoint for the application.
     * @return A ResponseEntity with the application's health status.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("activeSessions", String.valueOf(activeSessions.size()));
        
        return ResponseEntity.ok(health);
    }

    /**
     * Handles queries that require chart generation.
     * @param sessionId The current session ID.
     * @param sqlQuery The SQL query generated for chart data.
     * @param originalQuery The original natural language query from the user.
     * @param session The current chat session.
     * @return A ChatResponse containing the chart URL or an error message.
     */
    private ChatResponse handleChartQuery(String sessionId, String sqlQuery, String originalQuery, ChatSession session) {
        try {
            System.out.println("🔄 Processing chart query: " + originalQuery);
            String filePath = chartAnalyzer.generateChart(sqlQuery, originalQuery);
            System.out.println("📊 Chart generated at: " + filePath);
            
            File chartFile = new File(filePath);
            
            if (chartFile.exists()) {
                System.out.println("✅ Chart file exists: " + chartFile.getAbsolutePath());
                
                // Extract just the filename from the full path
                String fileName = chartFile.getName();
                String chartUrl = "/api/chart/" + fileName;
                
                System.out.println("🔗 Chart URL: " + chartUrl);
                
                String responseText = "📊 I've generated a chart for your query. Here's your visualization:";
                
                return new ChatResponse(responseText, "chart", sessionId, chartUrl, null);
            } else {
                System.err.println("❌ Chart file does not exist at: " + chartFile.getAbsolutePath());
                System.err.println("📁 Checking directory contents:");
                
                File parentDir = chartFile.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    File[] files = parentDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            System.err.println("  📄 " + file.getName());
                        }
                    }
                } else {
                    System.err.println("❌ Parent directory does not exist: " + 
                                     (parentDir != null ? parentDir.getAbsolutePath() : "null"));
                }
                
                return new ChatResponse("❌ Unable to generate chart. The chart file was not created successfully.", 
                                      "error", sessionId, null, null);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error generating chart: " + e.getMessage());
            e.printStackTrace();
            return new ChatResponse("❌ Error generating chart: " + e.getMessage(), "error", sessionId, null, null);
        }
    }

    /**
     * Handles text-based queries, executing the SQL and formatting the results.
     * @param sessionId The current session ID.
     * @param sqlQuery The SQL query to execute.
     * @param originalQuery The original natural language query from the user.
     * @param session The current chat session.
     * @return A ChatResponse containing the formatted text results or an error message.
     */
    private ChatResponse handleTextQuery(String sessionId, String sqlQuery, String originalQuery, ChatSession session) {
        try {
            List<String> results = textAnalyzer.executeQuery(sqlQuery);
            
            if (results.isEmpty()) {
                return new ChatResponse("🔍 No results found for your query. Try refining your search criteria.", 
                                      "text", sessionId, null, null);
            }
            
            // Check if results are large enough to show summary
            if (results.size() > SUMMARY_THRESHOLD) {
                return handleLargeResultSet(sessionId, results, originalQuery);
            }
            
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("**Results for your query:**\n\n");
            
            // Format results in a more web-friendly way
            if (results.size() == 1) {
                responseBuilder.append("```\n");
                responseBuilder.append(formatSingleResult(results.get(0)));
                responseBuilder.append("\n```");
            } else {
                responseBuilder.append("```\n");
                responseBuilder.append(formatMultipleResults(results));
                responseBuilder.append("\n```");
            }
            
            // Add summary
            responseBuilder.append("\n\n📋 **Summary:** Found ").append(results.size()).append(" result(s)");
            
            return new ChatResponse(responseBuilder.toString(), "text", sessionId, null, null);
            
        } catch (Exception e) {
            System.err.println("❌ Error executing text query: " + e.getMessage());
            e.printStackTrace();
            return new ChatResponse("❌ Error executing query: " + e.getMessage(), "error", sessionId, null, null);
        }
    }

    /**
     * Handles large result sets by providing a preview and a download link for the full results.
     * @param sessionId The current session ID.
     * @param results The full list of query results.
     * @param originalQuery The original natural language query.
     * @return A ChatResponse containing a summary and download link.
     */
    private ChatResponse handleLargeResultSet(String sessionId, List<String> results, String originalQuery) {
        try {
            // Generate unique summary ID
            String summaryId = "summary_" + System.currentTimeMillis() + "_" + 
                             UUID.randomUUID().toString().substring(0, 8);
            
            // Store full results for download
            SummaryData summaryData = new SummaryData(summaryId, originalQuery, results);
            summaryStorage.put(summaryId, summaryData);
            
            // Create summary response
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("📊 **Large Result Set Found**\n\n");
            responseBuilder.append("Found **").append(results.size()).append(" results** for your query.\n\n");
            
            // Show first few results as preview
            responseBuilder.append("**Preview (first 10 results):**\n\n");
            responseBuilder.append("```\n");
            
            List<String> preview = results.subList(0, Math.min(10, results.size()));
            responseBuilder.append(formatMultipleResults(preview));
            
            responseBuilder.append("\n... and ").append(results.size() - preview.size()).append(" more results");
            responseBuilder.append("\n```");
            
            // Add download link
            responseBuilder.append("\n\n📁 **Download Complete Results:**\n");
            responseBuilder.append("Click [here](/api/download/summary/").append(summaryId).append(") to download all results as a text file.");
            
            String downloadUrl = "/api/download/summary/" + summaryId;
            
            return new ChatResponse(responseBuilder.toString(), "summary", sessionId, null, downloadUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling large result set: " + e.getMessage());
            e.printStackTrace();
            return new ChatResponse("❌ Error processing large result set: " + e.getMessage(), 
                                  "error", sessionId, null, null);
        }
    }

    /**
     * Extracts headers from a formatted result string.
     * @param result The result string (e.g., "Key: Value | AnotherKey: AnotherValue").
     * @return An array of header strings.
     */
    private String[] extractHeaders(String result) {
        String[] parts = result.split(" \\| ");
        String[] headers = new String[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            String[] splitParts = parts[i].split(": ");
            headers[i] = (splitParts.length > 0) ? splitParts[0].trim() : parts[i].trim();
        }
        
        return headers;
    }

    /**
     * Extracts values from a formatted result string.
     * @param result The result string (e.g., "Key: Value | AnotherKey: AnotherValue").
     * @return An array of value strings.
     */
    private String[] extractValues(String result) {
        String[] parts = result.split(" \\| ");
        String[] values = new String[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            String[] splitParts = parts[i].split(": ");
            values[i] = (splitParts.length > 1) ? splitParts[1].trim() : parts[i].trim();
        }
        
        return values;
    }

    /**
     * Truncates a string value if it exceeds a maximum length, appending "..." if truncated.
     * @param value The string to truncate.
     * @param maxLength The maximum allowed length.
     * @return The truncated or original string.
     */
    private String truncateValue(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Formats a single result string into a more readable, multi-line format.
     * @param result The single result string.
     * @return The formatted string.
     */
    private String formatSingleResult(String result) {
        String[] parts = result.split(" \\| ");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            String[] splitParts = part.split(": ");
            if (splitParts.length > 1) {
                String key = splitParts[0].trim();
                String value = splitParts[1].trim();
                formatted.append(String.format("%-20s: %s\n", key, value));
            } else {
                formatted.append(part.trim()).append("\n");
            }
        }
        
        return formatted.toString();
    }

    /**
     * Formats multiple result strings into a table-like structure for display.
     * @param results A list of result strings.
     * @return The formatted string, including headers and a preview of rows.
     */
    private String formatMultipleResults(List<String> results) {
        StringBuilder formatted = new StringBuilder();
        
        // Format headers from first result
        if (!results.isEmpty()) {
            String[] firstResultParts = results.get(0).split(" \\| ");
            for (String part : firstResultParts) {
                String[] splitParts = part.split(": ");
                String header = (splitParts.length > 0) ? splitParts[0].trim() : part.trim();
                formatted.append(String.format("%-20s ", header)).append("| ");
            }
            formatted.append("\n");
            formatted.append("-".repeat(Math.min(100, formatted.length() - 1))).append("\n");
        }
        
        // Format data rows (limit to first 10 for web display)
        int displayCount = Math.min(results.size(), 10);
        for (int i = 0; i < displayCount; i++) {
            String result = results.get(i);
            String[] parts = result.split(" \\| ");
            for (String part : parts) {
                String[] splitParts = part.split(": ");
                String value = (splitParts.length > 1) ? splitParts[1].trim() : part.trim();
                formatted.append(String.format("%-20s ", value)).append("| ");
            }
            formatted.append("\n");
        }
        
        if (results.size() > 10) {
            formatted.append("\n... and ").append(results.size() - 10).append(" more results");
        }
        
        return formatted.toString();
    }

    /**
     * Retrieves an existing chat session or creates a new one if it doesn't exist.
     * @param sessionId The ID of the session to retrieve or create.
     * @return The ChatSession object.
     */
    private ChatSession getOrCreateSession(String sessionId) {
        return activeSessions.computeIfAbsent(sessionId, id -> new ChatSession(id));
    }

    /**
     * Generates a unique session ID.
     * The ID is composed of a timestamp and a random alphanumeric string.
     * @return A new unique session ID.
     */
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isRequestAllowed(String sessionId) {
        long currentTime = System.currentTimeMillis();
        List<Long> timestamps = requestTimestamps.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // Remove old timestamps outside the rate limit window
        timestamps.removeIf(timestamp -> currentTime - timestamp > RATE_LIMIT_WINDOW);
        
        // Check if under the limit
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        // Add current timestamp
        timestamps.add(currentTime);
        return true;
    }

    /**
     * Cleans up expired chat sessions and summary data.
     */
    private void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 30 * 60 * 1000; // 30 minutes
        
        activeSessions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getLastActivity() > sessionTimeout);
            
        requestTimestamps.entrySet().removeIf(entry -> {
            List<Long> timestamps = entry.getValue();
            timestamps.removeIf(timestamp -> currentTime - timestamp > RATE_LIMIT_WINDOW);
            return timestamps.isEmpty();
        });
        
        // Clean up old summaries (older than 1 hour)
        long summaryTimeout = 60 * 60 * 1000; // 1 hour
        summaryStorage.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getCreatedAt() > summaryTimeout);
        
        System.out.println("🧹 Session cleanup completed. Active sessions: " + activeSessions.size() + 
                          ", Stored summaries: " + summaryStorage.size());
    }

    /**
     * Shuts down the session cleanup executor when the application is destroyed.
     */
    @PreDestroy
    public void cleanup() {
        sessionCleanupExecutor.shutdown();
        System.out.println("🦒 JIRAF Web Chat Bot shutdown completed.");
    }
}