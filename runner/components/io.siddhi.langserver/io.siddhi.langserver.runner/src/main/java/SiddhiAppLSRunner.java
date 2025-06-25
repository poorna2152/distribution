import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;
import org.wso2.carbon.utils.Utils;

import java.io.*;
import java.nio.file.Paths;

public class SiddhiAppLSRunner {
    public static void main(String[] args) throws IOException {
        SiddhiJsonRpcHandler handler = new SiddhiJsonRpcHandler();
        EventSimulatorDataHolder.getInstance().setMaximumFileSize(8388608);
        EventSimulatorDataHolder.getInstance().setCsvFileDirectory(Paths.get(Utils.getRuntimePath().toString(),
                EventSimulatorConstants.DIRECTORY_DEPLOYMENT, EventSimulatorConstants.DIRECTORY_CSV_FILES).toString());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            String response = handler.handleRequest(line);
            System.out.println(response);
            System.out.flush();
        }
    }
}
