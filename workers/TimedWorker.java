// Edge node thread dealing with timed events: keep-alive and elections

package workers;

import beans.Average;
import com.google.gson.Gson;
import handlers.UploadHandler;
import messages.KeepAlive;
import messages.Message;
import synds.SynQCronJob;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

import static messages.Message.Type.MSG_KA;

public class TimedWorker extends Worker {

    private int counter = (int)(10*Math.random());
    private SynQCronJob avgBuffer;
    private String serverAddress;
    private int serverPort;

    public TimedWorker(SynQCronJob avgBuffer, String serverAddress, int serverPort) {
        this.avgBuffer = avgBuffer;
        this.thread = new Thread(this);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        ArrayList<Average> m;
        Message sendKeepAlive;
        String sendJson;
        byte[] sendData;
        DatagramPacket sendPacket;

        // From now on, it is the coordinator
        while (!stop) {
            if (coordinator) {

                // Compute global averages and send them to other nodes
                m = avgBuffer.dequeue();
                if (m != null) {
                    latestGlobalAVG = averageAVG(m, id);
                    flagReset();
                    System.out.println("[" + latestGlobalAVG.getTimestamp() + "] N/A | " + latestGlobalAVG.getType() + " | " + latestGlobalAVG.getValue());
                    uploadAggregate();
                    latestLocalAVG.clear();
                }
                // Otherwise, list of local averages hasn't been updated since last time.
                // Nothing to push to the server, since there's clearly no new data to send.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    if (stop)
                        System.out.println("Closing TimeWorker...!");
                }

            } else {

                // Keep-alive service to ensure coordinator is online (election can start here)
                if (cid != -1) {

                    // KA request
                    sendKeepAlive = new Message(MSG_KA, new Gson().toJson(new KeepAlive(KeepAlive.KAType.KA_NODE_REQUEST, counter)));
                    sendJson = token + "§" + new Gson().toJson(sendKeepAlive);
                    sendData = sendJson.getBytes();
                    try {
                        sendPacket = new DatagramPacket(sendData, sendData.length, nodesGet(cid).getAddress(), nodesGet(cid).getNodePort());
                        nodeChannel.send(sendPacket);
                    } catch (NullPointerException e) {
                        if (!election) {
                            System.err.println("- WARNING: Invalid CID, requesting new election...");
                            electionStart();
                            //break;
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("- WARNING: data stream invalid, can't send messages!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // KA reply
                    if (!coordinatorUpdate) {
                        try {
                            long timestamp = System.currentTimeMillis() + 1000L;
                            ArrayList<Object> res;
                            Integer value;
                            // Check packets, drop useless ones
                            do {
                                int time = (int) (timestamp - System.currentTimeMillis());
                                if (time <= 0) throw new SocketTimeoutException();
                                res = kaBuffer.dequeue(time);
                                if (res.size() == 1)
                                    value = (Integer) res.get(0);
                                else
                                    value = null;
                            } while (value == null || value != counter);
                            counter++;
                            //System.out.println("Node replied, everything is alright");
                        } catch (SocketTimeoutException e) {
                            if (!coordinatorUpdate && !election && !coordinator) {
                                // KA timed out
                                System.out.println("Keep-alive not returned, requesting new election...");
                                //nodesRemove(cid);
                                //System.out.println("Removed former coordinator " + cid + " from nodes list...");
                                electionStart();
                            }
                        }

                    } else
                        coordinatorUpdate = false;

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        if (stop)
                            System.out.println("Closing TimeWorker...!");
                    }

                } else {

                    // No coordinator was found at startup although other nodes were online.
                    // This means coordinator disconnected when this node was connecting.
                    if (!election) {
                        System.out.println("No coordinator found, requesting new election...");
                        electionStart();
                    }

                }

            }

        }

        uploadAggregate();
        System.out.println("TimedWorker stopped!");
    }

    private void uploadAggregate() {
        new Thread(new UploadHandler(serverAddress, serverPort,latestGlobalAVG, latestLocalAVG)).start();
    }

    @Override
    public void stopWorker() {
        kaBuffer.shutdown();
        thread.interrupt();
        super.stopWorker();
    }

}
