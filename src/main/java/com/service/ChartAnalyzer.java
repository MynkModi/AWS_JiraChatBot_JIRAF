package com.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * Service responsible for generating charts based on SQL query results.
 * It interacts with an AWS Lambda function to execute SQL queries and then uses JFreeChart
 * to create visual representations (pie charts, bar charts) of the data.
 */
public class ChartAnalyzer {

    private final LambdaClient lambdaClient;
    private final String lambdaReaderFunctionName;
    private final Path chartOutputPath = Paths.get("charts").toAbsolutePath();

    /**
     * Constructs a ChartAnalyzer with the necessary AWS Lambda client and function name.
     */
    public ChartAnalyzer(LambdaClient lambdaClient, String lambdaReaderFunctionName) {
        this.lambdaClient = lambdaClient;
        this.lambdaReaderFunctionName = lambdaReaderFunctionName;
    }

	public String generateChart(String sqlQuery, String userInput) throws Exception {
        // Create the request payload
        /**
         * Generates a chart (pie or bar) based on the provided SQL query and user input.
         * The SQL query is executed via an AWS Lambda function, and the results are then used to create a chart image.
         * @param sqlQuery The SQL query to execute to retrieve chart data.
         * @param userInput The original user input, used to determine chart type (e.g., "pie").
         * @return The absolute file path to the generated chart image.
         * @throws Exception if there's an error invoking the Lambda, parsing the response, or generating the chart.
         */
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("action", "chartData");
        requestPayload.put("sqlQuery", sqlQuery);
        
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(this.lambdaReaderFunctionName)
                .payload(SdkBytes.fromUtf8String(requestPayload.toString()))
                .build();
        
        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
        
        // Parse the response
        String rawResp = invokeResponse.payload().asUtf8String();
        System.out.println("Raw Lambda response: " + rawResp);
        System.out.println("HTTP Status Code: " + invokeResponse.statusCode());
        
        if (invokeResponse.statusCode() != 200) {
            throw new Exception("Lambda invocation failed with status: " + invokeResponse.statusCode());
        }
        
        JSONObject responseJson = new JSONObject(rawResp);
        
        if (responseJson.getInt("statusCode") != 200) {
            throw new Exception("Lambda execution failed: " + responseJson.getString("body"));
        }
        
        // Parse chart data from the response - body is a JSON string
        JSONObject chartData = new JSONObject(responseJson.getString("body"));
        Map<String, Integer> dataMap = new HashMap<>();
        
        JSONArray keys = chartData.names();
        for (int i = 0; i < keys.length(); i++) {
            String key = keys.getString(i);
            dataMap.put(key, chartData.getInt(key));
        }
        
        // Create chart as before
        JFreeChart chart;
        if (userInput.toLowerCase().contains("pie")) {
            chart = createPieChart(dataMap, "Issue Distribution");
        } else {
            chart = createBarChart(dataMap, "Issue Categories", "Count");
        }

        // Ensure the output directory exists
        if (!Files.exists(chartOutputPath)) {
            try {
                Files.createDirectories(chartOutputPath);
            } catch (IOException e) {
                System.err.println("Failed to create chart directory: " + chartOutputPath);
                throw new RuntimeException("Could not create chart output directory", e);
            }
        }

        String filePath = "chart_output_" + System.currentTimeMillis() + ".png";
        Path fullPath = chartOutputPath.resolve(filePath);
        ChartUtils.saveChartAsPNG(fullPath.toFile(), chart, 600, 400);
        return fullPath.toString();
    }

    
    /**
     * Creates a JFreeChart pie chart from the given data.
     * @param data A map where keys are labels and values are integer counts.
     * @param title The title of the pie chart.
     * @return A JFreeChart object representing the pie chart.
     */
    private static JFreeChart createPieChart(Map<String, Integer> data, String title) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }
        return ChartFactory.createPieChart(title, dataset, true, true, false);
    }

    /**
     * Creates a JFreeChart bar chart from the given data.
     * @param data A map where keys are categories and values are integer counts.
     * @param categoryAxis The label for the category axis.
     * @param valueAxis The label for the value axis.
     * @return A JFreeChart object representing the bar chart.
     */
    private static JFreeChart createBarChart(Map<String, Integer> data, String categoryAxis, String valueAxis) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            dataset.addValue(entry.getValue(), "Issues", entry.getKey());
        }
        return ChartFactory.createBarChart("Issue Summary", categoryAxis, valueAxis, dataset);
    }
}
