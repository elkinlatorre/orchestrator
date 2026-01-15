package ai.agentic.orchestrator.service.Impl;

import ai.agentic.orchestrator.service.CodeGeneratorAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PythonCoderAgent implements CodeGeneratorAgent {

    private final ChatLanguageModel chatModel;

    public PythonCoderAgent(@Qualifier("coderModel") ChatLanguageModel coderModel) {
        this.chatModel = coderModel;
    }

    @Override
    public String generatePythonCode(String taskDescription) {
        log.info("Generating robust Python code for task: {}", taskDescription);

        String systemInstruction = """
        You are a strict Python Code Generator.
        
        OUTPUT RULES:
        - Output ONLY valid Python code.
        - NO explanations.
        - NO markdown blocks (NO ```python).
        - NO text before or after the code.
        - If you explain anything, the system will fail.
        
        Task: %s
        """.formatted(taskDescription);

        String rawCode = chatModel.generate(systemInstruction).trim();

        return sanitizeCode(rawCode);
    }

    @Override
    public String healPythonCode(String taskDescription, String failingCode, String errorLog) {
        log.info("Requesting code correction from LLM...");

        String healingPrompt = """
            The following Python code for the task '%s' failed with a Syntax or Runtime Error.
            
            FAILING CODE:
            %s
            
            ERROR LOG:
            %s
            
            FIX the code. Output ONLY the corrected Python code. No explanations.
            """.formatted(taskDescription, failingCode, errorLog);

        return sanitizeCode(chatModel.generate(healingPrompt));
    }

    private String sanitizeCode(String code) {
        // 1. Remove Markdown blocks if present
        String cleanCode = code.replaceAll("(?s)```python(.*?)```", "$1")
                .replaceAll("```(.*?)```", "$1")
                .replace("```", "")
                .trim();

        log.debug("Sanitized Code for Docker: \n{}", cleanCode);
        return cleanCode;
    }
}
