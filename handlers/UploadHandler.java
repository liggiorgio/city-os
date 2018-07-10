// This thread sends a multicastPacket to a given node to
// emulate a concurrent multicast multicastPacket delivery.

package handlers;

import beans.Aggregate;
import beans.Average;
import com.google.gson.Gson;
import simulation.Measurement;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class UploadHandler implements Runnable {


    private String serverAddress;
    private int serverPort;
    private Average latestGlobalAVG;
    private ArrayList<Average> latestLocalAVG;

    public UploadHandler(String serverAddress, int serverPort, Average latestGlobalAVG, ArrayList<Average> latestLocalAVG) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.latestGlobalAVG = latestGlobalAVG;
        this.latestLocalAVG = latestLocalAVG;
    }

    @Override
    public void run() {
        try {
            Average global = new Average(latestGlobalAVG);
            ArrayList<Average> local = new ArrayList<>();
            for (Measurement _m : latestLocalAVG)
                local.add(new Average(_m));
            Aggregate aggregate = new Aggregate(global, local);
            String avgJson = new Gson().toJson(aggregate);
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/measurements");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(avgJson.getBytes());
            connection.connect();
            if (connection.getResponseCode() != 204) {
                System.err.println("Couldn't upload! Status: " + connection.getResponseCode());
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
        }
    }
}
