// Generic sensor blueprint

package sensors;

import beans.EdgeNode;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import messages.KeepAlive;
import messages.Message;
import simulation.Measurement;
import simulation.SensorStream;
import simulation.Simulator;

import java.io.*;
import java.net.*;

import static messages.KeepAlive.KAType.KA_SENSOR_REPLY;
import static messages.KeepAlive.KAType.KA_SENSOR_REQUEST;
import static messages.Message.Type.MSG_KA;
import static messages.Message.Type.MSG_MEASUREMENT;

public abstract class Sensor implements SensorStream, Runnable {
    int u, v;
    private int counter = (int)(10*Math.random());
    private long token = -1;
    Simulator simulator;
    String serverAddress;
    int serverPort;
    private InetAddress nodeAddress;
    private int nodePort;
    private DatagramSocket sensorChannel;
    private volatile boolean stopCondition;

    Sensor() {
        this.u = (int)(100*Math.random());
        this.v = (int)(100*Math.random());
        this.stopCondition = false;
        try {
            this.sensorChannel = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("ERROR: Couldn't initialize socket!");
            this.sensorChannel = null;
        }
    }

    // Ask server for nearest node address
    void getNode() {
        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/nodes/nearest?u=" + u + "&v=" + v);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                Reader reader = new InputStreamReader(input);
                EdgeNode result  = new Gson().fromJson(reader, EdgeNode.class);
                setNode(result.getAddress(), result.getSensorPort());
                System.out.println("- " + simulator.getIdentifier() + " : EdgeNode @ " + nodeAddress + ":" + nodePort);
            } else {
                System.err.println("- WARNING: no edge nodes online! (" + simulator.getIdentifier() + ")");
                setNode(null, -1);
            }
            connection.disconnect();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL! (" + simulator.getIdentifier() + ")");
            setNode(null, -1);
            token = -1;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server! (" + simulator.getIdentifier() + ")");
            setNode(null, -1);
            token = -1;
        }
    }

    // Ask for session token
    private long getToken() {
        long result;
        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/init");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                result  = Long.parseLong(reader.readLine());
                //System.out.println("- Server token: " + result);
            } else {
                System.err.println("- WARNING: no token read!");
                result = -1;
            }
            connection.disconnect();
        } catch (ProtocolException e) {
            e.printStackTrace();
            result = -1;
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL! (" + simulator.getIdentifier() + ")");
            result = -1;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server! (" + simulator.getIdentifier() + ")");
            result = -1;
        }
        return result;
    }

    // Update private fields concerning reference edge node
    private void setNode(InetAddress nodeAddress, int nodePort) {
        this.nodeAddress = nodeAddress;
        this.nodePort = nodePort;
    }

    // Send measurement to reference edge node
    @Override
    public void sendMeasurement(Measurement m) {
        if (nodeAddress == null)
            return;
        Message sendMessage = new Message(MSG_MEASUREMENT, new Gson().toJson(m));
        String json = new Gson().toJson(sendMessage);
        json = token + "§" + json;
        byte[] sendData = json.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nodeAddress, nodePort);
        try {
            sensorChannel.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Keep-alive service with current node
    @Override
    public void run() {
        long timestamp;
        Message sendMessage;
        byte[] sendData, receiveData;
        DatagramPacket sendPacket, receivePacket;

        while (!stopCondition) {
            // Request token at startup or when connection is lost with server
            if (token == -1)
                token = getToken();
            // Request closest node every 10 seconds
            if (counter > 0 && counter % 5 == 0)
                getNode();

            // KA request
            sendMessage = new Message(MSG_KA, new Gson().toJson(new KeepAlive(KA_SENSOR_REQUEST, counter)));
            String json = new Gson().toJson(sendMessage);
            json = token + "§" + json;
            sendData = json.getBytes();
            try {
                sendPacket = new DatagramPacket(sendData, sendData.length, nodeAddress, nodePort);
                sensorChannel.send(sendPacket);
            } catch (IllegalArgumentException e) {
                System.err.println("- WARNING: data stream interrupted, measurements are being lost!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // KA reply
            timestamp = System.currentTimeMillis() + 1000L;
            receiveData = new byte[256];
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                // Check packets, drop useless ones
                do {
                    int time = (int)(timestamp - System.currentTimeMillis());
                    if (time<=0) throw new SocketTimeoutException();
                    sensorChannel.setSoTimeout(time); // one second only
                    sensorChannel.receive(receivePacket);
                } while (!validateReply(receivePacket));
                counter++;
                //System.out.println("Node replied, everything is alright");
            } catch (SocketTimeoutException e) {
                // Reply timed out, requesting new node
                System.err.println("Node timed out, requesting new node...");
                counter = 1;
                getNode();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Sensor " + simulator.getIdentifier() + " retired!");
    }

    // Check reply validity
    private boolean validateReply(DatagramPacket packet) {
        if (!packet.getAddress().equals(nodeAddress) || (packet.getPort() != nodePort))
            return false;
        String[] message = new String(packet.getData()).split("§");
        if (Long.parseLong(message[0]) != token || token == -1)
            return false;
        JsonReader reader = new JsonReader(new StringReader(message[1]));
        Message reply = new Gson().fromJson(reader, Message.class);
        if (reply.getType() != MSG_KA)
            return false;
        KeepAlive content = new Gson().fromJson(reply.getData(), KeepAlive.class);
        return content.getType() == KA_SENSOR_REPLY && content.getId() == counter;
    }

    public void retire() {
        simulator.stopMeGently();
        stopCondition = true;
    }
}
