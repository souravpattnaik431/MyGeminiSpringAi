# 🚀 Gemini AI - Technical Tool Updates Digest

An automated, AI-powered developer intelligence service built with **Spring Boot 4**, **Spring AI**, and **Google Gemini** with native Google Search Grounding. It periodically aggregates, analyzes, and prioritizes the latest official releases, breaking changes, security CVEs, and action items across your entire software engineering and QA toolchain.

---

## 📋 Prerequisites & Requirements

* **Java**: `25+` (utilizes modern Java language features and module capabilities)
* **Spring Boot**: `4.1.1`
* **Spring AI**: `2.0.0+` (using `spring-ai-starter-model-google-genai` 2.0.1)
* **Apache Maven**: `3.9.16+` *(any modern Maven 3.9+ version will work; built and verified on 3.9.16)*
* **Google Gemini API Key**: with model access (`gemini-3.6-flash`)

---

## 🛡️ Code Quality & SonarQube Compliance

The codebase has been analyzed against strict **SonarQube** quality profiles:

* **0 Bugs / 0 Vulnerabilities / 0 Security Hotspots**
* **0% Code Duplication**: Modular architecture with reusable records, prompt loaders, and services.
* **Low Cognitive Complexity**: All complex methods refactored into single-responsibility helpers, keeping cognitive complexity well below SonarQube's threshold limit of 15.
* **Clean Code Standards**: Explicit timezone handling (`ZoneId.systemDefault()`), robust error handling, full Javadoc method documentation, and clean exception isolation.

---

## 💡 Why This Solution?

Modern engineering and QA teams rely on dozens of development, testing, DevOps, and cloud tools (e.g., Spring Boot, Playwright, Selenium, ArgoCD, SonarQube, Nexus, JFrog). 

* **The Problem**: Staying updated with new releases, deprecations, and breaking changes requires checking countless blogs, GitHub release pages, and changelogs every week. This manual research consumes hours of valuable engineering time.
* **The Solution**: This service automates the discovery process. A single API call queries all your tools in parallel, fetches real-time web developments via Google Search Grounding, and compiles an executive **Markdown Digest (`latest_tool_updates.md`)** with prioritized impact badges and actionable checklists.

---

## 🔍 Why Google Gemini & Spring AI?

During the evaluation of LLM providers for this project:

1. **Perplexity API**: Tested initially, but the API results were notably inconsistent compared to the Perplexity Web UI. Even when asking about the same tool where the Perplexity website returned recent updates, the API frequently failed to retrieve the latest releases or returned outdated information.
2. **OpenAI**: Evaluated, but achieving real-time web search grounding with OpenAI requires integrating third-party search engine APIs (such as Tavily, Bing, or SerpAPI) or building complex custom tool-calling wrappers.
3. **Google Gemini (Chosen)**:
   * **Native Google Search Grounding**: Gemini comes with built-in Google Search retrieval (`spring.ai.google.genai.chat.options.google-search-retrieval=true`). The model retrieves fresh information and cites authoritative sources (official blogs, GitHub releases, CVE databases) directly through Google Search with zero third-party search APIs.
   * **First-Class Spring AI Integration**: Spring AI provides the native starter `spring-ai-starter-model-google-genai` with out-of-the-box support for search grounding and provider structured output.

---

## 🏗️ Architecture & Core Features

### 1. Spring AI Native Structured Outputs
Rather than requesting arbitrary HTML or Markdown from the LLM inside prompt files (which causes formatting drift and fragile regex parsing), prompts focus purely on domain search criteria.

The service uses Spring AI native schema validation:
```java
ResponseEntity<ChatResponse, ToolReport> responseEntity = chatClient.prompt(prompt)
        .call()
        .responseEntity(ToolReport.class, 
                spec -> spec.useProviderStructuredOutput().validateSchema());
```
This guarantees that Gemini always returns a strongly-typed JSON schema mapped directly to Java record DTOs (`ToolReport`, `ToolUpdateItem`).

### 2. High-Performance Parallel Execution & Fault Tolerance
Querying 15+ developer tools sequentially against web search grounding would take several minutes. To make it production-ready, the service uses an asynchronous batch pipeline:

* **`ThreadPoolExecutor` & `CompletableFuture`**: Queries are executed concurrently across dedicated worker threads, reducing total wait time to seconds.
* **Batch Slicing (`batchSize = 10`)**: Prevents API rate limit spikes and thread starvation by processing tools in controlled chunks.
* **Individual Timeouts (`completeOnTimeout`)**: If an individual tool query hangs, it gracefully completes with a `TIMEOUT` status without stalling other queries.
* **Batch Boundary Timeouts**: Guarantees that the entire batch finishes within `app.ai.timeout.batch` seconds.

### 3. Zero-Dependency Markdown Report (`latest_tool_updates.md`)
Instead of heavy template engines (like Thymeleaf) or DOM builders (like Jsoup) that require complex CSS, the report is compiled into clean, portable Markdown using standard Java:
* **Impact Badges**: Categorized into 🔴 `CRITICAL`, 🟠 `MAJOR`, and 🔵 `MINOR`.
* **Interactive Checklists**: Formats actionable steps as Markdown checkboxes (`- [ ]`) ready for GitHub PRs, issues, Jira tickets, or Slack messages.

---

## ➕ How to Introduce a New Tool (Zero Code Changes)

The application is designed to be completely extensible without touching Java code or recompiling:

1. **Create the Prompt File**:
   Add a new text file under `src/main/resources/prompts/<tool-name>.txt` (e.g., `src/main/resources/prompts/kubernetes.txt`).

2. **Generate the Prompt Content (You can use ChatGPT / Gemini Web UI)**:
   Ask an AI assistant to generate the prompt text for your tool using this simple pattern:
   ```text
   You are a <Tool Name> expert and cloud engineer. Your role is to:
   1. Search for and identify the most recent and significant developments in the <Tool Name> ecosystem.
   2. Focus on official releases, security advisories, new features, and breaking changes.
   3. Prioritize authoritative sources like:
      - Official blogs
      - GitHub releases and changelogs
      - Official documentation and release notes

   Search for the latest <Tool Name> news from the past {days} days, including:
   - Version releases and updates
   - Deprecations and breaking changes
   - Security vulnerabilities and patches
   ```
   > [!NOTE]
   > Notice that **no output format instructions** (JSON/Markdown/HTML) are required in the prompt. Spring AI's native schema provider automatically enforces the output structure. Always keep the `{days}` placeholder for dynamic timeframe substitution.

3. **Call the API Immediately**:
   * For single-tool lookup: `GET /gemini/<tool-name>` (e.g., `GET /gemini/kubernetes`)
   * In the batch request: Add `"kubernetes"` to the `tools` array in `POST /gemini/getAllToolChanges`

**No Java classes, controllers, or configurations need to be changed.**

---

## ⚙️ Configuration & Environment

Set your Gemini API key as an environment variable before running the application:

```bash
# Windows PowerShell
$env:GEMINI_AI_KEY="your-gemini-api-key-here"

# Linux / macOS
export GEMINI_AI_KEY="your-gemini-api-key-here"
```

### Key Properties (`application.properties`)

```properties
spring.ai.google.genai.api-key=${GEMINI_AI_KEY}
spring.ai.google.genai.chat.options.model=gemini-3.6-flash
spring.ai.google.genai.chat.options.google-search-retrieval=true

tool.last.changes.days=30
app.ai.batch-size=10
app.ai.timeout.individual=180
app.ai.timeout.batch=300
```

---

## 📡 API Endpoints & Usage

### 1. Batch Tool Updates (Generates Markdown Report)

Aggregates updates for multiple tools in parallel, saves the formatted digest to `latest_tool_updates.md` on disk, and returns the Markdown text in the response.

* **Endpoint**: `POST /gemini/getAllToolChanges`
* **Content-Type**: `application/json`
* **Produces**: `text/markdown`

#### Request Body
```json
{
    "tools": [
        "azure-devops",
        "playwright",
        "saucelabs",
        "selenium-java",
        "spring-boot",
        "appium-java",
        "automation-anywhere-360",
        "java-openjdk",
        "spring-ai-java",
        "gitlab",
        "sonarqube",
        "argocd",
        "elasticsearch",
        "nexus",
        "jfrog-artifactory"
    ]
}
```

#### cURL Example
```bash
curl -X POST http://localhost:8080/gemini/getAllToolChanges \
  -H "Content-Type: application/json" \
  -d '{
    "tools": [
      "spring-boot",
      "spring-ai-java",
      "playwright",
      "selenium-java"
    ]
  }'
```

---

### 2. Single Tool Query (Structured JSON)

Fetches live updates and structured metadata for an individual tool on demand.

* **Endpoint**: `GET /gemini/{toolName}`
* **Produces**: `application/json`

#### Example
```bash
curl -X GET http://localhost:8080/gemini/playwright
```

#### Response Structure (`ToolChangeResult`)
```json
{
  "toolName": "playwright",
  "report": {
    "overview": "Playwright recent releases introduce new locator APIs and enhanced trace viewer capabilities.",
    "updates": [
      {
        "title": "Playwright v1.47 Release",
        "date": "2026-08-20",
        "source": "https://github.com/microsoft/playwright/releases",
        "impactLevel": "MAJOR",
        "summary": "Added support for component testing updates and new cross-browser assertions.",
        "actionItems": [
          "Update @playwright/test to 1.47.0 in package.json",
          "Review updated locator assertion best practices"
        ]
      }
    ]
  },
  "inputTokens": 450,
  "outputTokens": 230,
  "processingTimeMs": 1420,
  "status": "SUCCESS",
  "errorMessage": null
}
```

---

## 🛠️ Supported Tools Out-of-the-Box

Customized prompts focusing on authoritative documentation, GitHub changelogs, and official blogs are preconfigured in `src/main/resources/prompts/`:

| Category                         | Supported Tools                                           |
|:---------------------------------|:----------------------------------------------------------|
| **Frameworks & Languages**       | `java-openjdk`, `spring-boot`, `spring-ai-java`           |
| **Test Automation**              | `playwright`, `selenium-java`, `appium-java`, `saucelabs` |
| **DevOps & CI/CD**               | `argocd`, `azure-devops`, `gitlab`                        |
| **Artifact & Code Quality**      | `sonarqube`, `nexus`, `jfrog-artifactory`                 |
| **Data & Enterprise Automation** | `elasticsearch`, `automation-anywhere-360`                |

---

## 🧪 Build & Run

```bash
# Compile and package
mvn clean package

# Run the application
mvn spring-boot:run
```
