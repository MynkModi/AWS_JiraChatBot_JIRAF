## Name
AI(AWS Bedrock) Based Chat bot for Jira JIRAF[Jira Intelligent Response Analytics Framework]


## Description
AWS AI based Jira chat bot – JIRAF, that simplifies Jira data access through a natural conversational flow. It leverages powerful AWS Bedrock Agents for its core AI-driven services, including natural language understanding and query generation. It also uses another child project Jira Connector(https://github.com/MynkModi/aws_ai_jira_dataloader) to load Jira data into RDS tables at periodic intervals.


This application allows users to ask two kind of questions related to JIRA. First is computational question based on JIRA data and second is defect recommendation related question based on historical Jira effects. Defect recommendation questions has to be prefixed with "defect:" word. Please find some simple questions below. Note: The data related to this questions is stored in RDS database(for computational questions) and Bedrock Knowledgebase(for defect recommendation questions).


## Installation
This project is a standard Spring Boot abased chat pplication and can be run locally or deployed as a container. Lambda function(to read data from RDS db) associated with this project is added under resources folder. Similarly instructions for two bedrock agents  being used are also added under resources folder.

### Containerized Deployment (AWS ECS)
The application is designed to be deployed as a container on AWS Elastic Container Service (ECS) for scalability and high availability.

1.  **Build Docker Image**: Build the Docker image using the provided `Dockerfile` and `docker-compose.yml` as a reference.
    ```sh
    docker build -t jiraf-app .
    ```
2.  **Push to ECR**: Push the built image to Amazon Elastic Container Registry (ECR).
3.  **Create ECS Task Definition**: Define an ECS task that uses the image from ECR. Ensure you configure the necessary environment variables (e.g., `AWS_REGION`, `LAMBDA_READER`, Bedrock Agent IDs) in the task definition.
4.  **Configure ECS Service**: Create an ECS service to run and maintain the desired number of tasks.
5.  **Set Up Load Balancer**: Configure an Application Load Balancer (ALB) to route traffic to the containers. Create a target group for port 8080 on the containers and a listener on the ALB to forward HTTP/HTTPS traffic to this target group.

### Local Build
To build and run the project locally, you will need Java 17 and Maven.
```sh
mvn clean install
mvn spring-boot:run
```

## Usage
1. Users submit plain-language questions in  two categories (query/chart) about JIRA data .
2. The agentic system interprets the intent, identifies entities, and generates appropriate queries.
3. Results are presented in both textual summaries /relevant visualizations.
4. Users can refine queries through follow-up questions, drilling down into specific areas of interest.
5. All interactions occur within a chat interface, eliminating the learning curve associated with traditional business intelligence tools.

## Support
Reach out to modimayank@gmail.com for any queries.

## Roadmap
Future enhancements planned for JIRAF include:
- Integration with JIRA MCP (Mission Critical Platform) servers for enhanced enterprise support.
- Advanced analytics dashboards.
- Support for more complex, multi-turn conversational queries.

## License
This project is distributed under a custom proprietary license. The source code is shared for evaluation purposes for the AWS Global Hackathon 2025 only. Running, copying, modifying, or redistributing the code is strictly prohibited.

Please see the `LICENSE` file for full terms and conditions.
