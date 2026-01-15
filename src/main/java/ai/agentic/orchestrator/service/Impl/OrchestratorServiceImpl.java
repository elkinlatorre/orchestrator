package ai.agentic.orchestrator.service.Impl;

import ai.agentic.orchestrator.dto.OrchestrationResult;
import ai.agentic.orchestrator.model.ExecutionLog;
import ai.agentic.orchestrator.repository.ExecutionLogRepository;
import ai.agentic.orchestrator.service.CodeGeneratorAgent;
import ai.agentic.orchestrator.service.OrchestratorService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorServiceImpl implements OrchestratorService {

    private final ChatLanguageModel chatModel;
    private final CodeGeneratorAgent codeGeneratorAgent;
    private final DockerSandboxService codeExecutionService;
    private final ExecutionLogRepository logRepository;

    private static final int MAX_RETRIES = 3;

    @Override
    public OrchestrationResult process(String userPrompt) {
        String intent = classifyIntent(userPrompt);
        OrchestrationResult result;

        if (intent.contains("DATA_ANALYSIS") || intent.contains("CODE_GENERATION")) {
            result = executeWithSelfHealing(userPrompt);
        } else {
            result = new OrchestrationResult(chatModel.generate(userPrompt), "GENERAL_LLM_AGENT");
        }
        saveExecutionLog(userPrompt, intent, result);
        return result;
    }

    private void saveExecutionLog(String prompt, String intent, OrchestrationResult result) {
        try {
            ExecutionLog executionLog = ExecutionLog.builder()
                    .userPrompt(prompt)
                    .detectedIntent(intent)
                    .executionResult(result.output())
                    .finalAgent(result.agentName())
                    .retryCount(extractRetries(result.agentName()))
                    .build();

            logRepository.save(executionLog);
            log.info("Execution log persisted to database with ID: {}", executionLog.getId());
        } catch (Exception e) {
            log.error("Failed to persist execution log: {}", e.getMessage());
        }
    }

    private Integer extractRetries(String agentName) {
        try {
            if (agentName.contains("_")) {
                return Integer.parseInt(agentName.substring(agentName.lastIndexOf("_") + 1));
            }
        } catch (Exception e) { return 0; }
        return 0;
    }

    private OrchestrationResult executeWithSelfHealing(String task) {
        String code = codeGeneratorAgent.generatePythonCode(task);
        String lastError = "";

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            log.info("Execution Attempt {}/{}", attempt, MAX_RETRIES);

            String result = codeExecutionService.executePythonCode(code);

            // Check if there's an error in the output
            if (isError(result)) {
                log.warn("Attempt {} failed. Error detected.", attempt);
                lastError = result;

                if (attempt < MAX_RETRIES) {
                    log.info("Starting Self-Healing process...");
                    code = codeGeneratorAgent.healPythonCode(task, code, lastError);
                }
            } else {
                log.info("Success on attempt {}!", attempt);
                return new OrchestrationResult(result, "PYTHON_AGENT_HEALED_" + attempt);
            }
        }

        return new OrchestrationResult("Failed after " + MAX_RETRIES + " attempts. Last Error: " + lastError, "FAILED_AGENT");
    }

    private boolean isError(String output) {
        String lower = output.toLowerCase();
        return lower.contains("error") || lower.contains("traceback") || lower.contains("syntaxerror");
    }

    private String classifyIntent(String prompt) {
        String classificationPrompt = """
            Classify the following user request into one of these categories:
            - DATA_ANALYSIS: If the user wants to calculate, process data, or perform math.
            - CODE_GENERATION: If the user specifically asks for code.
            - GENERAL: For greetings or general questions.
            
            Respond ONLY with the category name.
            
            User request: %s
            """.formatted(prompt);

        return chatModel.generate(classificationPrompt).trim();
    }
}
