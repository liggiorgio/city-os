// Blueprint for Edge Node thread workers

package workers;

import beans.Average;
import beans.EdgeNode;
import messages.KeepAlive;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import messages.Election;
import messages.Hello;
import messages.Message;
import simulation.Measurement;
import synds.SynMapFlag;
import synds.SynMapNode;
import synds.SynQTimeout;
import handlers.ElectionHandler;

import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import static messages.Election.EType.ELECTION_ANSWER;
import static messages.Hello.HType.*;
import static messages.KeepAlive.KAType.*;

public abstract class Worker implements Runnable {
    public static volatile int id;
    static volatile int cid = -1;
    static volatile boolean coordinator;
    static volatile boolean stop;
    public static volatile boolean election;
    static volatile boolean coordinatorUpdate;
    static Average latestGlobalAVG = new Average("-1", "gbl", 0, 0);
    static ArrayList<Average> latestLocalAVG = new ArrayList<>();
    public static SynMapNode nodes;
    private static SynMapFlag avgFlag = new SynMapFlag();
    private static SynQTimeout elBuffer = new SynQTimeout();
    static SynQTimeout kaBuffer;
    static DatagramSocket nodeChannel, sensorChannel;
    static long token;
    Thread thread;

    public static void initWorkers(int id, SynMapNode nodes, int cid, DatagramSocket nodeChannel, DatagramSocket sensorChannel, SynQTimeout kaBuffer) {
        Worker.setChannels(nodeChannel, sensorChannel);
        Worker.id = id;
        Worker.setCoordinator(cid);
        Worker.nodes = nodes;
        Worker.kaBuffer = kaBuffer;
        Worker.flagReset();
    }

    public void startWorker() {
        thread = new Thread(this);
        thread.start();
    }

    public void stopWorker() {
        stop = true;
    }

    public static synchronized void setCoordinator(int nid) {
        Worker.cid = nid;
        Worker.coordinator = (nid == id);
    }

    public Thread getThread() {
        return thread;
    }

    // Read incoming messages from nodes and send replies back
    DatagramPacket nodeDispatch(DatagramPacket packet) {
        String[] request = new String(packet.getData()).split("§");
        Object response = null;
        Gson gson = new Gson();
        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        // Check token value
        try {
            if (Long.parseLong(request[0]) != token)
                return null;
        } catch (NumberFormatException e) {
            return null;
        }

        // Get content
        JsonReader reader = new JsonReader(new StringReader(request[1]));
        Message content = gson.fromJson(reader, Message.class);

        // JSON magic
        switch (content.getType()) {
            case MSG_KA: response = parseKeepAlive(gson.fromJson(content.getData(), KeepAlive.class), KA_NODE_REQUEST, KA_NODE_REPLY); break; // Keep-alive messages
            case MSG_HELLO: response = parseHello(gson.fromJson(content.getData(), Hello.class), address, port); break; // Hello messages
            case MSG_ELECTION: response = parseElection(gson.fromJson(content.getData(), Election.class)); break; // Election messages
            case MSG_AVERAGE: response = parseAverage(gson.fromJson(content.getData(), Average.class)); break; // Measurement means from nodes
            default: break; // Not recognized, thus ignore
        }

        if (response == null)
            return null;
        // Message code, then JSON content
        Message reply = new Message(content.getType(), new Gson().toJson(response));
        String json = token + "§" + new Gson().toJson(reply);
        byte[] replyData = json.getBytes();
        return new DatagramPacket(replyData, replyData.length, address, port);
    }

    // Read incoming messages from nodes and send replies back
    DatagramPacket sensorDispatch(DatagramPacket packet) {
        String[] request = new String(packet.getData()).split("§");
        Object response = null;
        Gson gson = new Gson();
        InetAddress address = packet.getAddress();
        int port = packet.getPort();

        // Check token value
        try {
            if (Long.parseLong(request[0]) != token)
                return null;
        } catch (NumberFormatException e) {
            return null;
        }

        // Get content
        JsonReader reader = new JsonReader(new StringReader(request[1]));
        Message content = gson.fromJson(reader, Message.class);

        // JSON magic
        switch (content.getType()) {
            case MSG_KA: response = parseKeepAlive(gson.fromJson(content.getData(), KeepAlive.class), KA_SENSOR_REQUEST, KA_SENSOR_REPLY); break; // Keep-alive messages
            case MSG_MEASUREMENT: response = parseMeasurement(gson.fromJson(content.getData(), Measurement.class)); break; // Measurements
            default: break; // Not recognized, thus ignore
        }

        if (response == null)
            return null;
        // Message code, then JSON content
        Message reply = new Message(content.getType(), gson.toJson(response));
        String json = token + "§" + gson.toJson(reply);
        byte[] replyData = json.getBytes();
        return new DatagramPacket(replyData, replyData.length, address, port);
    }

    // Return ready-to-send reply to keep-alive requests
    private KeepAlive parseKeepAlive(KeepAlive message, KeepAlive.KAType expected, KeepAlive.KAType response) {
        // Is protocol correct?
        if (message.getType() == expected)
            return new KeepAlive(response, message.getId());
        if (message.getType() == KA_NODE_REPLY)
            kaBuffer.enqueue(message.getId());
        return null;
    }

    // Return ready-to-send reply to hello requests
    private Hello parseHello(Hello message, InetAddress address, int port) {
        // Is protocol correct?
        if (message.getType() == HELLO_REQUEST) {
            // Add new node to current list
            EdgeNode newbie = new EdgeNode(message.getId(), address, port, -1, -1, -1);
            nodesAdd(newbie);
            System.out.println("A new node joined the network!" +
                    "\n - " + newbie.getId() + " | " + newbie.getAddress() + ":" + newbie.getNodePort());
            // Reply
            if (coordinator)
                return new Hello(HELLO_COORDINATOR, id);
            else
                return new Hello(HELLO_REPLY, id);
        }
        return null;
    }

    // Return ready-to-send reply to elections requests
    private Election parseElection(Election message) {
        switch (message.getType()) {
            // Someone is requiring a new election
            case ELECTION_ELECTION: {
                if (!election) {
                    electionStart();
                    System.out.println("Node " + message.getId() + " requested a new election...");
                }
                return new Election(ELECTION_ANSWER, id);
            }
            // Someone replied as alive during the election
            case ELECTION_ANSWER: {
                if (election)
                    elBuffer.enqueue(message);
            } break;
            // The coordinator disclosed itself
            case ELECTION_COORDINATOR: {
                if (election)
                    elBuffer.enqueue(message);
                else {
                    coordinatorUpdate = true;
                    setCoordinator(message.getId());
                    System.out.println("New async coordinator message: " + message.getId());
                }
            } break;
            default: break; // Ignore packet
        }
        return null;
    }

    void electionStart() {
        election = true;
        new Thread(new ElectionHandler(id, nodesList(),nodeChannel, elBuffer, token)).start();
    }

    Average parseAverage(Average average) { return null; }

    Measurement parseMeasurement(Measurement measurement) { return null; }

    Average measurementAVG(ArrayList<Measurement> list, int nid) {
        if (list.isEmpty())
            return null;
        double value = 0;
        int n = 0;
        for (Measurement m : list) {
            value += m.getValue();
            n++;
        }
        value /= n;
        return new Average(String.valueOf(nid), "lcl", value, System.currentTimeMillis());
    }

    Average averageAVG(ArrayList<Average> list, int nid) {
        if (list.isEmpty())
            return null;
        double value = 0;
        int n = 0;
        for (Average m : list) {
            value += m.getValue();
            n++;
        }
        value /= n;
        return new Average(String.valueOf(nid), "gbl", value, System.currentTimeMillis());
    }

    EdgeNode nodesGet(int id) {
        return nodes.get(id);
    }

    private ArrayList<EdgeNode> nodesList() {
        return new ArrayList<>(nodes.values());
    }

    private void nodesAdd(EdgeNode e) {
        nodes.put(e.getId(), e);
        flagAdd(e.getId());
        //System.out.println(nodes);
    }

    public static void nodesRemove(int id) {
        nodes.remove(id);
    }

    boolean flagStatus(int id) {
        return avgFlag.get(id);
    }

    private void flagAdd(int id) {
        avgFlag.put(id, false);
    }

    void flagRaise(int id) {
        avgFlag.replace(id, true);
    }

    static void flagReset() {
        avgFlag = new SynMapFlag();
        for (EdgeNode e : nodes.values())
            avgFlag.put(e.getId(), false);
    }

    public abstract void run();

    public void setToken(long token) {
        Worker.token = token;
    }

    private static void setChannels(DatagramSocket nodeChannel, DatagramSocket sensorChannel) {
        Worker.nodeChannel = nodeChannel;
        Worker.sensorChannel = sensorChannel;
    }
}
