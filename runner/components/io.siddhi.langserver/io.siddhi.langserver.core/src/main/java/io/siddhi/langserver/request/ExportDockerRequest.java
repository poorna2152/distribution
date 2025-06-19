package io.siddhi.langserver.request;

import java.util.List;

public class ExportDockerRequest {
    private List<String> siddhiApps;
    private List<String> bundles;
    private List<String> jars;
    private DockerConifguration dockerConifguration;
    private List<TemplatedVariable> templatedVariables;

    public List<String> getSiddhiApps() {
        return siddhiApps;
    }

    public List<String> getBundles() {
        return bundles;
    }

    public List<String> getJars() {
        return jars;
    }

    public DockerConifguration getDockerConifguration() {
        return dockerConifguration;
    }

    public List<TemplatedVariable> getTemplatedVariables() {
        return templatedVariables;
    }
}
