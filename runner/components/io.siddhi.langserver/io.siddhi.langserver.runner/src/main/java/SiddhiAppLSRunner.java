import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SiddhiAppLSRunner {
    public static void main(String[] args) throws IOException {
        SiddhiJsonRpcHandler handler = new SiddhiJsonRpcHandler();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            String response = handler.handleRequest(line);
            System.out.println(response);
            System.out.flush();
        }
    }
}
