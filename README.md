🤖 Agentic AI Orchestrator & Sandbox

Autonomous AI Code Execution Platform

Java 25 · Spring Boot 4 · LangChain4j · Docker · Virtual Threads

This project is a Technical Proof of Concept (PoC) for an Agentic AI Orchestrator capable of:

Understanding user intent

Generating specialized Python code

Executing it securely inside isolated sandboxes

Detecting failures and self-healing automatically

It represents a production-grade architecture for safely running LLM-generated code.

🚀 Core Capabilities
🧠 Multi-Model Orchestration

Uses a two-tier AI architecture:

Role	Model	Responsibility
Router	Llama 3.2	Intent detection & task routing
Coder	llama3.1:8b	Python code synthesis & reasoning

This keeps simple requests fast while reserving heavy reasoning for complex tasks.

🐳 Secure Docker Sandboxing

All AI-generated code runs in ephemeral Docker containers with:

512 MB RAM limit

50% CPU quota

Network disabled

Auto-remove on exit

This provides strong isolation against:

Infinite loops

Data leaks

Malicious instructions

🔄 Autonomous Self-Healing

When execution fails:

The container returns the traceback

The agent receives the error

A corrected version of the code is generated

The code is re-executed

Up to 3 automatic recovery attempts are performed without user intervention.

📊 Real-Time Audit Dashboard

A built-in web UI shows:

The model’s reasoning steps

Generated Python code

Execution output

Errors and retries

This gives full transparency into how the agent is behaving.

🛠️ Technology Stack
Backend

Java 25

Spring Boot 4 (Snapshot)

JPA

H2 Database

Virtual Threads

AI & Orchestration

LangChain4j

Ollama

Llama 3.2

llama3.1:8b

Infrastructure

Docker Engine API for Java

Frontend

HTML5

Tailwind CSS

Vanilla JavaScript

📐 System Architecture

The system follows a fully agentic execution pipeline:

User Prompt
↓
Intent Classification (Router Model)
↓
Python Code Generation (Coder Model)
↓
LLM Output Sanitization
↓
Docker Sandbox Execution
↓
Success → Persist logs
↓
Failure → Self-Healing Loop → Retry

Execution Flow

Intent Classification
Determines whether the request requires computation, code execution, or plain text.

Code Generation
The Coder model generates a pure Python script for analysis tasks.

Sanitization Layer
Removes Markdown, prose, and artifacts from LLM output.

Sandbox Execution
The script is executed inside a locked-down Docker container.

Self-Healing
If an error occurs, the traceback is fed back to the model for correction.

Async Persistence
All logs are stored in H2 using Virtual Threads to avoid blocking the UI.

🔧 Setup & Installation
Prerequisites

Docker Engine running locally

Ollama installed with:

llama3.2:3b

llama3.1:8b

JDK 25

Installation

Clone the repository

Configure application.yml with:

Ollama base URL

Model names

Run:

mvn spring-boot:run


Open the dashboard:

http://localhost:8080/index.html

🛡️ Security Architecture

This system is designed for safe execution of untrusted AI code.

🔒 Network Isolation

Docker containers are created with:

withNetworkDisabled(true)


This prevents:

Data exfiltration

External API calls

Lateral movement

⚙️ Resource Quotas

Hard limits prevent DoS attacks:

Resource	Limit
RAM	512 MB
CPU	50%
Lifetime	Auto-removed
🧹 Ephemeral Execution

Containers are:

Auto-removed

Force-killed after execution

Never reused

No filesystem state or processes survive after execution.

🎯 Why This Matters

This project demonstrates a production-grade pattern for:

Running LLM-generated code safely

Enabling autonomous reasoning agents

Preventing sandbox escapes

Supporting self-correcting AI workflows

It can be extended to:

Data analysis

AI copilots

Autonomous ETL

Secure AI tooling

Agent-based automation