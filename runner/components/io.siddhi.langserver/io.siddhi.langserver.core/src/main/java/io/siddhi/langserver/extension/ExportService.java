package io.siddhi.langserver.extension;

import com.google.gson.JsonSyntaxException;
import io.siddhi.langserver.response.ExportResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.siddhi.editor.core.commons.configs.DockerBuildConfig;
import org.wso2.carbon.siddhi.editor.core.commons.request.ExportAppsRequest;
import org.wso2.carbon.siddhi.editor.core.exception.DockerGenerationException;
import org.wso2.carbon.siddhi.editor.core.exception.KubernetesGenerationException;
import org.wso2.carbon.siddhi.editor.core.internal.DockerBuilder;
import org.wso2.carbon.siddhi.editor.core.internal.DockerBuilderStatus;
import org.wso2.carbon.siddhi.editor.core.internal.ExportUtils;
import org.wso2.carbon.siddhi.editor.core.vscode.PublicExportUtils;
import org.wso2.carbon.utils.Utils;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@JsonSegment("export")
public class ExportService extends ExtensionService {
    ConfigProvider configProvider;
    private static final String EXPORT_TYPE_KUBERNETES = "kubernetes";
    private static final String EXPORT_TYPE_DOCKER = "docker";

    public ExportService() {
    }

    @JsonRequest
    public CompletableFuture<ExportResponse> exportDocker(ExportAppsRequest exportAppsRequest) throws ConfigurationException {
        System.setProperty("carbon.home", "/Users/poorna/Documents/packs/wso2si-4.3.0/");
        System.setProperty("wso2.runtime", "server");
        configProvider = ConfigProviderFactory.getConfigProvider(Paths.get("/Users/poorna/Documents/packs/wso2si-4.3.0/conf/server/deployment.yaml"));
        return CompletableFuture.supplyAsync(() -> {
            String errorMessage = "";
            try {
                ExportUtils exportUtils = PublicExportUtils.getExportUtils(configProvider, exportAppsRequest, EXPORT_TYPE_DOCKER);
                System.setProperty("user.dir", Utils.getRuntimePath().toString());
                exportUtils.createZipFile();

                boolean pushDocker = exportAppsRequest.getDockerConfiguration().isPushDocker();
                if (!pushDocker) {
                    return new ExportResponse(true, errorMessage);
                }

                DockerBuildConfig dockerBuildConfig = exportAppsRequest.getDockerConfiguration();
                DockerBuilderStatus dockerBuilderStatus = PublicExportUtils.getDockerBuilderStatusInstance("", "" );
                if ((StringUtils.isEmpty(dockerBuildConfig.getImageName())) ||
                        (StringUtils.isEmpty(dockerBuildConfig.getUserName())) ||
                        (StringUtils.isEmpty(dockerBuildConfig.getEmail())) ||
                        (StringUtils.isEmpty(dockerBuildConfig.getPassword()))
                ) {
                    errorMessage = "Missing required Docker build configuration " +
                            "of (DockerImageName|UserName|Email|Password)";
                }
                if (!dockerBuildConfig.getImageName().equals(dockerBuildConfig.getImageName().toLowerCase())) {
                    errorMessage = "Invalid docker image name " +
                            dockerBuildConfig.getImageName() +
                            ". Docker image name must be in lowercase.";
                }
                DockerBuilder dockerBuilder = new DockerBuilder(
                        dockerBuildConfig.getImageName(),
                        dockerBuildConfig.getUserName(),
                        dockerBuildConfig.getEmail(),
                        dockerBuildConfig.getPassword(),
                        exportUtils.getTempDockerPath(),
                        dockerBuilderStatus
                );
                dockerBuilder.start();
            } catch (JsonSyntaxException e) {
                errorMessage = "Incorrect JSON configuration format found while exporting Docker/K8s" + e.getMessage();
            } catch (DockerGenerationException e) {
                errorMessage = "Exception caught while generating Docker export artifacts. " + e.getMessage();
            } catch (KubernetesGenerationException e) {
                errorMessage = "Exception caught while generating Kubernetes export artifacts. " + e.getMessage();
            } catch (Exception e) {
                errorMessage = "Cannot generate export-artifacts archive. " + e.getMessage();
            }
            return new ExportResponse(errorMessage.isEmpty(), errorMessage);
        });
    }
}
