package io.siddhi.langserver.extension;

import io.siddhi.langserver.request.ExtensionNameRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.wso2.carbon.siddhi.extensions.installer.core.constants.ExtensionsInstallerConstants;
import org.wso2.carbon.siddhi.extensions.installer.core.config.mapping.ConfigMapper;
import org.wso2.carbon.siddhi.extensions.installer.core.exceptions.ExtensionsInstallerException;
import org.wso2.carbon.siddhi.extensions.installer.core.config.mapping.models.ExtensionConfig;
import org.wso2.carbon.siddhi.extensions.installer.core.execution.DependencyInstaller;
import org.wso2.carbon.siddhi.extensions.installer.core.execution.DependencyInstallerImpl;
import org.wso2.carbon.siddhi.extensions.installer.core.execution.DependencyRetriever;
import org.wso2.carbon.siddhi.extensions.installer.core.execution.DependencyRetrieverImpl;
import org.wso2.carbon.siddhi.extensions.installer.core.execution.SiddhiAppExtensionUsageDetector;
import org.wso2.carbon.siddhi.extensions.installer.core.execution.SiddhiAppExtensionUsageDetectorImpl;
import org.wso2.carbon.siddhi.extensions.installer.core.models.SiddhiAppStore;
import org.wso2.carbon.siddhi.extensions.installer.core.util.MissingExtensionsInstaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@JsonSegment("extensionInstaller")
public class SiddhiExtensionInstallerService extends ExtensionService {

    private Map<String, ExtensionConfig> extensionConfigs;
    private SiddhiAppStore siddhiAppStore;

    public SiddhiExtensionInstallerService() {
        try {
            this.extensionConfigs = ConfigMapper.loadAllExtensionConfigs(ExtensionsInstallerConstants.CONFIG_FILE_LOCATION);
        } catch (ExtensionsInstallerException e) {
            throw new RuntimeException(e);
        }
        this.siddhiAppStore = new SiddhiAppStore();
    }

    @JsonRequest
    public CompletableFuture<Object> getAllExtensionStatuses() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyRetriever retriever = new DependencyRetrieverImpl(extensionConfigs);
                return retriever.getAllExtensionStatuses(false);
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Object> getExtensionStatus(ExtensionNameRequest extensionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyRetriever retriever = new DependencyRetrieverImpl(extensionConfigs);
                return retriever.getExtensionStatusFor(extensionId.extensionName);
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Object> getDependencyStatuses(ExtensionNameRequest extensionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyRetriever retriever = new DependencyRetrieverImpl(extensionConfigs);
                return retriever.getDependencyStatusesFor(extensionId.extensionName);
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Object> getDependencySharingExtensions(ExtensionNameRequest extensionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyRetriever retriever = new DependencyRetrieverImpl(extensionConfigs);
                return retriever.getDependencySharingExtensionsFor(extensionId.extensionName);
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Object> installDependencies(ExtensionNameRequest extensionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyInstaller installer = new DependencyInstallerImpl(extensionConfigs);
                return installer.installDependenciesFor(extensionId.extensionName);
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Object> uninstallDependencies(ExtensionNameRequest extensionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyInstaller installer = new DependencyInstallerImpl(extensionConfigs);
                Map<String, Object> result = installer.unInstallDependenciesFor(extensionId.extensionName);
                return result;
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<Object> installMissingExtensions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SiddhiAppExtensionUsageDetector usageDetector = new SiddhiAppExtensionUsageDetectorImpl(extensionConfigs);
                DependencyInstaller installer = new DependencyInstallerImpl(extensionConfigs);
                return MissingExtensionsInstaller.installMissingExtensions(siddhiAppStore, usageDetector, installer);
            } catch (ExtensionsInstallerException e) {
                return errorResponse(e);
            }
        });
    }

    private Map<String, Object> errorResponse(Exception e) {
        return Map.of(
                "error", true,
                "message", e.getMessage()
        );
    }

    private void copyFileIfNotExists(Path sourceFile, Path targetDirectory) throws IOException {
        if (!Files.exists(sourceFile)) {
            throw new NoSuchFileException("Source file does not exist: " + sourceFile);
        }

        if (!Files.isDirectory(targetDirectory)) {
            throw new NotDirectoryException("Target is not a directory: " + targetDirectory);
        }

        Path targetFile = targetDirectory.resolve(sourceFile.getFileName());

        if (!Files.exists(targetFile)) {
            Files.copy(sourceFile, targetFile);
        }
    }
}
