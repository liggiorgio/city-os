// Edge node connecting to the network

import beans.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import messages.Hello;
import messages.Message;
import synds.*;
import workers.*;
import handlers.MulticastHandler;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static messages.Hello.HType.*;
import static messages.Message.Type.MSG_HELLO;

public class NodeApp {

    private static int id, serverPort, nodePort = 0, sensorPort = 0;
    private static String serverAddress;
    private static int u, v;
    private static long token = -1;
    private static DatagramSocket nodeChannel, sensorChannel;
    private static SynQBulk sensorBuffer = new SynQBulk();
    private static SynQCronJob avgBuffer = new SynQCronJob();
    private static SynQTimeout kaBuffer = new SynQTimeout();
    private static HashMap<Integer, EdgeNode> nodes = new HashMap<>();
    private static SynMapAck sentAverages = new SynMapAck();
    private static int cid = -1;

    public static void main(String[] args) {
        int attempts = 0;
        String[] server;

        // Init: manual or automatic
        if (args.length < 1) {
            // Missing server address/port
            System.err.println("ERROR: too few arguments");
            System.exit(1);
        } else if (args.length == 1) {
            // Automatic configuration
            if (!args[0].matches(".+:.+")) {
                System.err.println("ERROR: Wrong server address format, expected: HOST:PORT");
                System.exit(2);
            }

            server = args[0].split(":");
            serverAddress = server[0];
            try {
                serverPort = Integer.parseInt(server[1]);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: Server port must be an integer.");
                System.exit(3);
            }
            initID();
        } else if (args.length == 4) {
            // Manual configuration
            try {
                id = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: First argument must be an integer.");
                System.exit(3);
            }

            try {
                nodePort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: Second argument must be an integer.");
                System.exit(3);
            }

            try {
                sensorPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: First argument must be an integer.");
                System.exit(3);
            }

            if (!args[3].matches(".+:.+")) {
                System.err.println("ERROR: Wrong server address format, expected: HOST:PORT");
                System.exit(2);
            }

            server = args[3].split(":");
            serverAddress = server[0];
            try {
                serverPort = Integer.parseInt(server[1]);
            } catch (NumberFormatException e) {
                System.err.println("ERROR: Server port must be an integer.");
                System.exit(3);
            }

        } else {
            // Missing id, node port, sensor port, server address/port
            System.err.println("ERROR: too few arguments");
            System.exit(1);
        }

        initChannels();
        initLocation();
        System.out.println("Connecting...");

        // Request session token
        for (boolean success = false; (!success && attempts<10); attempts++) {
            token = getToken();
            if (token != -1)
                success = true;
            else
                attempts++;
        }

        // Too many attempts
        if (attempts == 10) {
            System.err.println("- ERROR: Too many attempts to request token. Shutting down...");
            System.exit(1);
        }
        attempts = 0;

        // Connect to main server
        for (boolean success = false; (!success && attempts<10); attempts++) {
            try {
                EdgeNode self = new EdgeNode(id,InetAddress.getLocalHost(), nodePort, sensorPort, u, v);
                String selfJson = new Gson().toJson(self);
                URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/nodes");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.getOutputStream().write(selfJson.getBytes());
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    InputStream input = connection.getInputStream();
                    Reader reader = new InputStreamReader(input);
                    ArrayList<EdgeNode> temp = new Gson().fromJson(reader, new TypeToken<ArrayList<EdgeNode>>(){}.getType());
                    EdgeNode me = new EdgeNode(-1, null, -1, -1, -1, -1);
                    for (EdgeNode e : temp) {
                        nodes.put(e.getId(), e);
                        if (e.getId() == id) {
                            me = e;
                        }
                    }
                    System.out.println("Connected!" +
                            "\n * Pr. ID:\t" + me.getId() +
                            "\n * Address:\t" + me.getAddress() +
                            "\n * N. port:\t" + me.getNodePort() +
                            "\n * S. port:\t" + me.getSensorPort());
                    success = true;
                } else {
                    System.err.println("Not connected! Status: " + connection.getResponseCode());

                    // Spatial constraint violated, generate new location
                    if (connection.getResponseCode() == 403) {
                        initLocation();
                        continue;
                    }

                    // ID uniqueness constraint violated, generate new ID
                    if (connection.getResponseCode() == 409) {
                        initID();
                        continue;
                    }

                    // Wait a little before retrying
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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

        // Too many attempts
        if (attempts == 10) {
            System.err.println("- ERROR: Too many attempts to join network. Shutting down...");
            System.exit(1);
        }

        // Connect to other nodes in the network
        if (nodes.size() == 1 && nodes.containsKey(id)) {
            System.out.println("I'm coordinator!");
            cid = id;
        } else {
            // Say hello to other nodes and get to know the coordinator
            Gson gson = new Gson();
            Message helloRequest = new Message(MSG_HELLO, gson.toJson(new Hello(HELLO_REQUEST, id))), helloReply;
            byte[] helloRequestData = (token + "§" + (gson.toJson(helloRequest))).getBytes(), helloReplyData;
            DatagramPacket helloRequestPacket, helloReplyPacket;

            //System.out.println("Sending hello requests...");

            // Create threads to send packets simultaneously
            ArrayList<Thread> p2pWorkers = new ArrayList<>();
            for (EdgeNode n : nodes.values()) {
                if (n.getId() == id)
                    continue;
                helloRequestPacket = new DatagramPacket(helloRequestData, helloRequestData.length, n.getAddress(), n.getNodePort());
                p2pWorkers.add(new Thread(new MulticastHandler(n.getId(), nodeChannel, helloRequestPacket)));
            }

            // Send packets concurrently!
            for (Thread t : p2pWorkers) {
                t.start();
            }

            // Clear list to save only self and those who reply in time.
            // Non-replying nodes are considered offline as well as the former coordinator.
            int nodesNo = nodes.size() - 1;
            EdgeNode replied = nodes.get(id);
            nodes.clear();
            nodes.put(id, replied);

            // Wait for replies from other nodes
            //System.out.println("Waiting hello replies...");
            try {
                nodeChannel.setSoTimeout(1000);
            } catch (SocketException e) {
                System.err.println("- WARNING: couldn't setup socket correctly!");
            }
            for (int i = 0; i < nodesNo; ) {
                try {
                    helloReplyData = new byte[256];
                    helloReplyPacket = new DatagramPacket(helloReplyData, helloReplyData.length);
                    nodeChannel.receive(helloReplyPacket);
                    String[] reply = new String(helloReplyPacket.getData()).split("§");
                    if (Long.parseLong(reply[0]) == token) {
                        JsonReader reader = new JsonReader(new StringReader(reply[1]));
                        helloReply = gson.fromJson(reader, Message.class);
                        if (helloReply.getType() == MSG_HELLO) {
                            Hello helloContent = gson.fromJson(helloReply.getData(), Hello.class);
                            replied = new EdgeNode(helloContent.getId(), helloReplyPacket.getAddress(), helloReplyPacket.getPort(), -1, -1, -1);
                            // Replying node is alive
                            if (helloContent.getType() == HELLO_REQUEST || helloContent.getType() == HELLO_REPLY || helloContent.getType() == HELLO_COORDINATOR) {
                                nodes.put(replied.getId(), replied);
                                i++;

                                // Reply back to new node
                                if (helloContent.getType() == HELLO_REQUEST) {
                                    System.out.println("Node " + replied.getId() + "(" + replied.getAddress() + ":" + replied.getNodePort() + ") joined the network!");
                                    Message helloBack = new Message(MSG_HELLO, gson.toJson(new Hello(HELLO_REPLY, id)));
                                    byte[] helloBackData = (token + "§" + (new Gson().toJson(helloBack))).getBytes();
                                    DatagramPacket helloBackPacket = new DatagramPacket(helloBackData, helloBackData.length, replied.getAddress(), replied.getNodePort());
                                    nodeChannel.send(helloBackPacket);
                                    i--;
                                }

                                // Replying node is the coordinator
                                if (helloContent.getType() == HELLO_COORDINATOR) {
                                    System.out.println("Coordinator is " + replied.getId() + "!");
                                    cid = replied.getId();
                                }
                            }
                        }
                    }

                } catch (SocketTimeoutException ignored) {
                    System.out.println("Reply expired, not all nodes are online.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // If nobody replied as coordinator, election starts as soon as services are online
        }

        try {
            nodeChannel.setSoTimeout(0);
        } catch (SocketException e) {
            System.err.println("- WARNING: couldn't setup socket correctly!");
        }

        // Connection established, execute threads
        Worker.initWorkers(id, new SynMapNode(nodes), cid, nodeChannel, sensorChannel, kaBuffer);

        ArrayList<Worker> workers = new ArrayList<>();
        workers.add(new NetSensorWorker(sensorBuffer));
        workers.add(new NetNodeWorker(avgBuffer, sentAverages));
        workers.add(new ValueWorker(sensorBuffer, avgBuffer, sentAverages));
        workers.add(new TimedWorker(avgBuffer, serverAddress, serverPort));
        for (Worker w : workers) {
            w.setToken(token);
            w.startWorker();
        }
        System.out.println("Starting! Press ENTER to stop application...");

        // Wait for key to stop execution
        Scanner keyIn = new Scanner(System.in);
        keyIn.nextLine();

        // Stop job and leave the network
        for (Worker w : workers) {
            w.stopWorker();
            try {
                w.getThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // HTTP request to server : leave network
        try {
            EdgeNode self = new EdgeNode(id,InetAddress.getLocalHost(), nodePort, sensorPort, u, v);
            String selfJson = new Gson().toJson(self);
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/nodes");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(selfJson.getBytes());
            connection.connect();
            if (connection.getResponseCode() == 204) {
                System.out.println("Disconnected! Shutting down...");
            } else {
                System.err.println("Unknown error! Status: " + connection.getResponseCode());
                connection.disconnect();
            }
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

    // Main initialization
    private static void initID() {
        id = (int)(10000*Math.random());
    }

    private static void initChannels() {
        try {
            nodeChannel = new DatagramSocket(nodePort);
            sensorChannel = new DatagramSocket(sensorPort);
            nodePort = nodeChannel.getLocalPort();
            sensorPort = sensorChannel.getLocalPort();
        } catch (SocketException e) {
            System.err.println("ERROR: Couldn't initialize socket!");
            nodeChannel = sensorChannel = null;
            nodePort = sensorPort = -1;
        }
    }

    private static void initLocation() {
        u = (int)(100*Math.random());
        v = (int)(100*Math.random());
    }

    // Ask for session token
    private static long getToken() {
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
            System.err.println("- WARNING: malformed server URL!");
            result = -1;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            result = -1;
        }
        return result;
    }
}
