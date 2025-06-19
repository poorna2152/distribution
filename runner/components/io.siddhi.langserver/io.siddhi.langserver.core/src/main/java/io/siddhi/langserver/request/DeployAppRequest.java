package io.siddhi.langserver.request;

import java.util.List;

public class DeployAppRequest {

    private List<Server> serverList;
    private List<SiddhiFile> siddhiFileList;

    public List<Server> getServerList() {
        return serverList;
    }

    public void setServerList(List<Server> serverList) {
        this.serverList = serverList;
    }

    public List<SiddhiFile> getSiddhiFileList() {
        return siddhiFileList;
    }

    public void setSiddhiFileList(List<SiddhiFile> siddhiFileList) {
        this.siddhiFileList = siddhiFileList;
    }

    public static class Server {
        private String host;
        private String port;
        private String username;
        private String password;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class SiddhiFile {
        private String fileName;
        private String filePath;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }
}
