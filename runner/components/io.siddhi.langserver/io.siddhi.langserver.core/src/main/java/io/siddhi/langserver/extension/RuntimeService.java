package io.siddhi.langserver.extension;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.langserver.request.DeployAppRequest;
import io.siddhi.langserver.request.StartAppRequest;
import io.siddhi.langserver.response.DeployAppResponse;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.wso2.carbon.siddhi.editor.core.commons.response.DebugRuntimeResponse;
import org.wso2.carbon.siddhi.editor.core.commons.response.Status;
import org.wso2.carbon.siddhi.editor.core.exception.SiddhiAppDeployerServiceStubException;
import org.wso2.carbon.siddhi.editor.core.util.siddhiappdeployer.SiddhiAppDeployerApiHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@JsonSegment("runtime")
public class RuntimeService extends ExtensionService {

    @JsonRequest
    public CompletableFuture<DebugRuntimeResponse> startSiddhiApp(StartAppRequest startAppRequest) {
        return CompletableFuture.supplyAsync(() -> {
            SiddhiManager siddhiManager = new SiddhiManager();
            //Generate runtime
            String siddhiAppString = new String(Base64.getDecoder().decode(startAppRequest.getSiddhiApp()), StandardCharsets.UTF_8);
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiAppString);

            siddhiAppRuntime.start();
            return new DebugRuntimeResponse(Status.SUCCESS, null, startAppRequest.getPath(), null, null);
        });
    }

    @JsonRequest
    public CompletableFuture<DeployAppResponse> deploy(DeployAppRequest deployAppRequest) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<String> messages = new ArrayList<>();
            boolean success = true;
            String fileName;
            String siddhiFile;
            SiddhiAppDeployerApiHelper siddhiAppDeployerApiHelper = new SiddhiAppDeployerApiHelper();
            for (DeployAppRequest.SiddhiFile siddhiApp: deployAppRequest.getSiddhiFileList()) {
                fileName = siddhiApp.getFileName().replaceAll("\"", "");
                try {
                    siddhiFile = Files.readString(Paths.get(siddhiApp.getFilePath()));
                } catch (IOException e) {
                    success = false;
                    messages.add(e.getMessage());
                    continue;
                }
                for (DeployAppRequest.Server server: deployAppRequest.getServerList()) {
                    String host = server.getHost().replaceAll("\"", "");
                    String port = server.getPort().replaceAll("\"", "");
                    String hostAndPort = host + ":" + port;
                    String username = server.getUsername().replaceAll("\"", "");
                    String password = server.getPassword().replaceAll("\"", "");
                    try {
                        boolean response = siddhiAppDeployerApiHelper.
                                deploySiddhiApp(hostAndPort, username, password, siddhiFile, fileName);
                        if (response) {
                            messages.add(fileName + " was successfully deployed to " + hostAndPort);
                        }
                    } catch (Exception e) {
                        messages.add(e.getMessage());
                        success = false;
                    }
                }
            }
            return new DeployAppResponse(success, messages);
        });
    }

}
