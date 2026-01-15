package ai.agentic.orchestrator.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String userPrompt;

    private String detectedIntent;

    @Column(columnDefinition = "TEXT")
    private String generatedCode;

    @Column(columnDefinition = "TEXT")
    private String executionResult;

    private String finalAgent;

    private Integer retryCount;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}