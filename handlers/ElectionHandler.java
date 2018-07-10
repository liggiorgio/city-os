// This thread sends packets to other nodes when an election occurs.

package handlers;

import beans.EdgeNode;
import com.google.gson.Gson;
import messages.Election;
import messages.Message;
import synds.SynQTimeout;
import workers.Worker;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import static messages.Election.EType.ELECTION_ANSWER;
import static messages.Election.EType.ELECTION_COORDINATOR;
import static messages.Election.EType.ELECTION_ELECTION;
import static messages.Message.Type.MSG_ELECTION;
import static workers.Worker.nodesRemove;
import static workers.Worker.setCoordinator;

public class ElectionHandler implements Runnable {

    private final long token;
    private final SynQTimeout elBuffer;
    private int id;
    private ArrayList<EdgeNode> nodes;
    private DatagramSocket nodeChannel;

    public ElectionHandler(int id, ArrayList<EdgeNode> nodes, DatagramSocket nodeChannel, SynQTimeout elBuffer, long token) {
        this.id = id;
        this.nodes = new ArrayList<>(nodes);
        this.nodeChannel = nodeChannel;
        this.elBuffer = elBuffer;
        this.token = token;
    }

    @Override
    public void run() {
        Message sendElection;
        String sendJson;
        byte[] sendData;
        DatagramPacket sendPacket;

        // Send election message to other nodes
        //System.out.println("\nElection time!");
        HashMap<Integer, EdgeNode> majors = new HashMap<>(), minors = new HashMap<>();
        for (EdgeNode node : nodes) {
            int nid = node.getId();
            if (nid > id)
                majors.put(nid, node);
            if (nid < id)
                minors.put(nid, node);
        }
        //System.out.println("- Majors: " + majors.size() + "\n- Minors: " + minors.size());

        int newCid = -1;
        boolean cReply = false;
        ArrayList<Integer> replied = new ArrayList<>();

        if (majors.size() > 0) {
            // Creates thread to send packet simultaneously
            ArrayList<Thread> p2pWorkers = new ArrayList<>();
            sendElection = new Message(MSG_ELECTION, new Gson().toJson(new Election(ELECTION_ELECTION, id)));
            sendJson = token + "§" + new Gson().toJson(sendElection);
            sendData = sendJson.getBytes();
            for (EdgeNode node : majors.values()) {
                sendPacket = new DatagramPacket(sendData, sendData.length, node.getAddress(), node.getNodePort());
                p2pWorkers.add(new Thread(new MulticastHandler(node.getId(), nodeChannel, sendPacket)));
            }

            // Send packets concurrently!
            for (Thread t : p2pWorkers) {
                t.start();
            }

            // Wait at most one second to gather all packets
            long timestamp = System.currentTimeMillis() + 1000L;
            final int total = majors.size();

            try {
                ArrayList<Object> res;
                Election message;
                do {
                    // Check packets, drop useless ones
                    int time = (int) (timestamp - System.currentTimeMillis());
                    if (time <= 0) throw new SocketTimeoutException();
                    res = elBuffer.dequeue(time);
                    if (res.size() == 1)
                        message = (Election) res.get(0);
                    else
                        message = null;
                    if (message != null) {
                        int mid = message.getId();
                        //System.out.println("- Reply received from " + message.getId());
                        if (majors.containsKey(mid)) {
                            Election.EType type = message.getType();
                            //System.out.println("  - It's from a valid node!\n  - KAType: " + type);
                            if (type == ELECTION_ANSWER || type == ELECTION_COORDINATOR) {
                                replied.add(mid);
                                timestamp += 250L; // More time to let electing nodes complete
                                //System.out.println("  - Reply registered");
                                if (message.getType() == ELECTION_COORDINATOR) {
                                    //System.out.println("  - New coordinator! CID: " + mid);
                                    newCid = mid;
                                    cReply = true;
                                }
                            }
                        }
                    }
                } while (!cReply || replied.size() < total);

            } catch (SocketTimeoutException e) {
                // Reply timed out, some other nodes may be offline too
                //System.out.println("Replies timed out, removing offline nodes...");
                for (Integer nid : replied) {
                    majors.remove(nid);
                }
                for (Integer nid : majors.keySet()) {
                    nodesRemove(nid);
                }
            }

        }

        // Update internal info
        if (cReply) {
            System.out.println("Coordinator is " + newCid + "!");
            setCoordinator(newCid);
            Worker.election = false;
        } else {
            if (replied.size() == 0) {
                // Notify minors this process is the new coordinator
                setCoordinator(id);
                System.out.println("I'm coordinator!");
                sendElection = new Message(MSG_ELECTION, new Gson().toJson(new Election(ELECTION_COORDINATOR, id)));
                sendJson = token + "§" + new Gson().toJson(sendElection);
                sendData = sendJson.getBytes();
                ArrayList<Thread> p2pWorkers = new ArrayList<>();
                for (EdgeNode node : minors.values()) {
                    sendPacket = new DatagramPacket(sendData, sendData.length, node.getAddress(), node.getNodePort());
                    p2pWorkers.add(new Thread(new MulticastHandler(node.getId(), nodeChannel, sendPacket)));
                }

                // Send packets concurrently!
                for (Thread t : p2pWorkers) {
                    t.start();
                }

                Worker.election = false;
            } else {
                System.out.println("No coordinator elected, starting new election...");
                Worker.election = true;
            }
        }

        elBuffer.clear();
    }

}
