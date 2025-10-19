# 🦒 **JIRAF — Jira Intelligent Response Analytics Framework**

### *AI (AWS Bedrock) Based Chatbot for Jira*



## 🧠 **Description**

**JIRAF (Jira Intelligent Response Analytics Framework)** is an **AI-powered Jira chatbot** that simplifies complex data exploration through **natural conversational queries**.
It leverages **AWS Bedrock Agents** for intelligent language understanding, intent recognition, and dynamic query generation.

JIRAF also integrates with a companion project —
🔗 **[Jira Connector](https://github.com/MynkModi/aws_ai_jira_dataloader)** — which loads Jira data into **Amazon RDS** at regular intervals.

This application supports two types of Jira-related queries:

1. 🧮 **Computational Queries** — Analytical questions based on Jira data (stored in RDS).
2. 🪲 **Defect Recommendation Queries** — Predictive or diagnostic questions based on historical defect data (stored in Bedrock Knowledge Base).

To ask a defect recommendation question, simply prefix your query with the keyword **`defect:`**

> Example: `defect: suggest possible cause for environment deployment failure`



## 🤖 **Multi-Agent Bedrock Architecture**

JIRAF uses a **multi-agent Bedrock architecture**:

* 🧩 **SQL Query Generation Agent** — Interprets analytical questions and generates structured SQL queries to retrieve relevant Jira data.
* 🤝 **Defect Recommendation Agent** — Acts as a **collaborator agent**, analyzing issue history to suggest probable root causes and potential resolutions.

The system automatically interprets the user’s intent and dynamically routes the request to the appropriate agent.



## ⚙️ **Installation**

This project is a **Spring Boot-based chatbot application** that can be run locally or deployed as a **containerized microservice**.

It includes:

* A **Lambda function** (under `/resources`) for reading data from RDS.
* Configuration files for **two Bedrock agents** (also under `/resources`).



### 🐳 **Containerized Deployment (AWS ECS)**

Designed for scalable and highly available deployment using **AWS Elastic Container Service (ECS)**.

1. **Build Docker Image**

   ```bash
   docker build -t jiraf-app .
   ```

2. **Push to ECR**
   Push the built image to **Amazon Elastic Container Registry (ECR)**.

3. **Create ECS Task Definition**
   Define a task using the image from ECR and set environment variables such as:

   * `AWS_REGION`
   * `LAMBDA_READER`
   * Bedrock Agent IDs

4. **Configure ECS Service**
   Create an ECS service to maintain desired task instances.

5. **Set Up Load Balancer**
   Configure an **Application Load Balancer (ALB)**:

   * Target group: Port **8080**
   * Listener: Forwards **HTTP/HTTPS** traffic to target group



### 💻 **Local Build**

Requirements:

* **Java 17**
* **Maven**

Build and run locally:

```bash
mvn clean install
mvn spring-boot:run
```



## 💬 **Usage**

1. Users submit **plain-language questions** (query/chart) about Jira data.
2. The **agentic system** interprets intent and generates appropriate SQL or defect recommendations.
3. Results are returned as **text summaries** and/or **visualizations** (pie, bar, trend charts).
4. Users can refine queries with **follow-up questions** to drill deeper.
5. All interactions happen within a **chat interface**, eliminating the need for complex JQL knowledge.



## 🗺️ **Roadmap**

Planned enhancements include:

* 🔗 **Integration with JIRA MCP (Mission Critical Platform)** for enterprise-grade support.
* 📊 **Advanced analytics dashboards** and visual reporting.
* 🧩 **Multi-turn conversational context handling** for complex workflows.
* 🧠 Integration with **Arize Phoenix** for **AI observability**, **traceability**, and **multi-agent performance monitoring**.



## 🛠️ **Support**

For queries or contributions, contact:
📧 **[modimayank@gmail.com](mailto:modimayank@gmail.com)**



## ⚖️ **License**

This project is distributed under a **custom proprietary license**.
The source code is shared **only for evaluation purposes** for the **AWS Global Hackathon 2025**.

> **Note:** Running, copying, modifying, or redistributing the code is **strictly prohibited**.

Please see the `LICENSE` file for complete terms and conditions.
