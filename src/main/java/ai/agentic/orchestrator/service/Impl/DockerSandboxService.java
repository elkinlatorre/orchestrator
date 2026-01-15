package ai.agentic.orchestrator.service.Impl;

import ai.agentic.orchestrator.service.CodeExecutionService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerSandboxService implements CodeExecutionService {

    private final DockerClient dockerClient;

    @Override
    public String executePythonCode(String code) {
        log.info("Preparing sandbox for Python execution...");

        String containerId = null;
        try {
            // 0. Resources config (Sandbox)
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(512 * 1024 * 1024L) //RAM limit to 512MB
                    .withCpuQuota(50000L) // Limit CPU Core to 50%
                    .withAutoRemove(true);

            // 1. Create container
            CreateContainerResponse container = dockerClient.createContainerCmd("ai-sandbox:latest")
                    .withHostConfig(hostConfig)
                    .withCmd("python3", "-c", code)
                    .withNetworkDisabled(true)
                    .exec();

            containerId = container.getId();
            log.info("Container created with ID: {}", containerId);

            // 2. Start container
            dockerClient.startContainerCmd(containerId).exec();

            // 3.Get the response and timeout manage
            return waitForContainerResult(containerId);

        } catch (Exception e) {
            log.error("Docker sandbox failure: {}", e.getMessage());
            return "Execution Error: " + e.getMessage();
        } finally {
            // 4. Clean Up
            cleanupContainer(containerId);
        }
    }

    private String waitForContainerResult(String containerId) throws InterruptedException {
        StringBuilder output = new StringBuilder();
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                output.append(new String(frame.getPayload()));
            }
        };
        boolean finishedAtTime = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(callback)
                .awaitCompletion(15, TimeUnit.SECONDS);
        if (!finishedAtTime) {
            log.warn("TIMEOUT: Container {} exceeded time limit. Forcing stop...", containerId);
            stopContainer(containerId);
            return "TIMEOUT_ERROR: The execution exceeded the 15-second safety limit.";
        }
        return output.toString().trim();
    }

    private void stopContainer(String containerId) {
        if (containerId != null) {
            try {
                dockerClient.stopContainerCmd(containerId).withTimeout(2).exec();
                log.info("Container {} stopped by timeout policy.", containerId);
            } catch (Exception e) {
                log.error("Error stopping container {}: {}", containerId, e.getMessage());
            }
        }
    }

    private void cleanupContainer(String containerId) {
        if (containerId != null) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                log.info("Container {} removed successfully.", containerId);
            } catch (Exception e) {
                log.debug("Container already removed or not found: {}", e.getMessage());
            }
        }
    }
}