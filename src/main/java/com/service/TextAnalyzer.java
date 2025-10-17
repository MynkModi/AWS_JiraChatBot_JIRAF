package com.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * Service responsible for executing SQL queries via an AWS Lambda function
 * and processing the results into a list of formatted strings.
 */
public class TextAnalyzer {

    private final LambdaClient lambdaClient;
    private final String lambdaReaderFunctionName;

    /**
     * Constructs a TextAnalyzer with the necessary AWS Lambda client and function name.
     * @param lambdaClient The AWS Lambda client to use for invoking the Lambda function.
     * @param lambdaReaderFunctionName The name of the Lambda function that executes SQL queries.
     */
    public TextAnalyzer(LambdaClient lambdaClient, String lambdaReaderFunctionName) {
        this.lambdaClient = lambdaClient;
        this.lambdaReaderFunctionName = lambdaReaderFunctionName;
    }

	public List<String> executeQuery(String sqlQuery) throws Exception {
        JSONObject requestPayload = new JSONObject();
        /**
         * Executes a given SQL query by invoking an AWS Lambda function.
         * @param sqlQuery The SQL query string to be executed.
         * @return A list of strings, where each string represents a formatted row of the query result.
         * @throws Exception if the Lambda invocation fails or returns an error status.
         */
        requestPayload.put("action", "query");
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
        
        // Parse the result data - body is a JSON string, so we need to deserialize it
        JSONArray resultsArray = new JSONArray(responseJson.getString("body"));
        List<String> results = new ArrayList<>();
        
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject row = resultsArray.getJSONObject(i);
            StringBuilder rowBuilder = new StringBuilder();
            
            for (String key : row.keySet()) {
                rowBuilder.append(key)
                         .append(": ")
                         //.append(row.getString(key))
                         .append(row.has(key) && !row.isNull(key) ? row.get(key).toString() : "null")
                         .append(" | ");
            }
            
            // Remove trailing separator
            String rowStr = rowBuilder.toString();
            if (rowStr.endsWith(" | ")) {
                rowStr = rowStr.substring(0, rowStr.length() - 3);
            }
            
            results.add(rowStr);
        }
        
        return results;
    }

}
