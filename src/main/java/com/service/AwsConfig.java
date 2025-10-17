package com.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.time.Duration;

/**
 * Configuration class for AWS SDK clients and related services.
 * This class defines beans for AWS LambdaClient, BedrockAgentRuntimeAsyncClient,
 * and the application's service components like TextAnalyzer and ChartAnalyzer.
 */
@Configuration
public class AwsConfig {

    /**
     * Creates and configures an AWS LambdaClient bean.
     * @param awsRegion The AWS region to use for the client, injected from properties.
     * @return A configured LambdaClient instance.
     */
    @Bean
    public LambdaClient lambdaClient(@Value("${aws.region}") String awsRegion) {
        return LambdaClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates and configures a TextAnalyzer service bean.
     * @param lambdaClient The LambdaClient used by the TextAnalyzer.
     * @param lambdaReader The name of the Lambda function for reading data, injected from properties.
     * @return A configured TextAnalyzer instance.
     */
    @Bean
    public TextAnalyzer textAnalyzer(LambdaClient lambdaClient, @Value("${lambda_reader}") String lambdaReader) {
        return new TextAnalyzer(lambdaClient, lambdaReader);
    }

    /**
     * Creates and configures a ChartAnalyzer service bean.
     * @param lambdaClient The LambdaClient used by the ChartAnalyzer.
     * @param lambdaReader The name of the Lambda function for reading data, injected from properties.
     * @return A configured ChartAnalyzer instance.
     */
    @Bean
    public ChartAnalyzer chartAnalyzer(LambdaClient lambdaClient, @Value("${lambda_reader}") String lambdaReader) {
        return new ChartAnalyzer(lambdaClient, lambdaReader);
    }

    /**
     * Helper method to build a BedrockAgentRuntimeAsyncClient with common configurations.
     * @param awsRegion The AWS region for the client.
     * @return A configured BedrockAgentRuntimeAsyncClient instance.
     */
    private BedrockAgentRuntimeAsyncClient buildAgentClient(String awsRegion) {
        return BedrockAgentRuntimeAsyncClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .readTimeout(Duration.ofSeconds(900))
                        .connectionTimeout(Duration.ofSeconds(120))
                        .connectionAcquisitionTimeout(Duration.ofSeconds(120))
                        .maxConcurrency(100)
                        .connectionMaxIdleTime(Duration.ofSeconds(300)))
                .build();
    }

    /**
     * Creates and configures a singleton BedrockAgentRuntimeAsyncClient specifically for SQL agents.
     * The client is configured with increased timeouts to accommodate potentially long-running agent invocations.
     * @param awsRegion The AWS region to use for the client, injected from properties.
     * @return A configured BedrockAgentRuntimeAsyncClient instance.
     */
    @Bean(name = "sqlAgentClient", destroyMethod = "close")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public BedrockAgentRuntimeAsyncClient sqlAgentClient(@Value("${aws.region}") String awsRegion) {
        return buildAgentClient(awsRegion);
    }

    /**
     * Creates and configures a singleton BedrockAgentRuntimeAsyncClient specifically for defect agents.
     * @param awsRegion The AWS region to use for the client, injected from properties.
     * @return A configured BedrockAgentRuntimeAsyncClient instance.
     */
    @Bean(name = "defectAgentClient", destroyMethod = "close")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public BedrockAgentRuntimeAsyncClient defectAgentClient(@Value("${aws.region}") String awsRegion) {
        return buildAgentClient(awsRegion);
    }
}