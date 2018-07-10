// Edge node thread dealing with local averages

package workers;

import beans.Average;
import com.google.gson.Gson;
import messages.Message;
import simulation.Measurement;
import synds.SynMapAck;
import synds.SynQBulk;
import synds.SynQCronJob;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;

public class ValueWorker extends Worker {

    private SynQBulk sensorBuffer;
    private SynQCronJob avgBuffer;
    private SynMapAck sentAverages;

    public ValueWorker(SynQBulk sensorBuffer, SynQCronJob avgBuffer, SynMapAck sentAverages) {
        this.sensorBuffer = sensorBuffer;
        this.avgBuffer = avgBuffer;
        this.sentAverages = sentAverages;
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        ArrayList<Measurement> m;
        Average localAVG;
        String message;
        byte[] messageData;
        DatagramPacket messagePacket;

        while (!stop) {
            m = sensorBuffer.dequeue();
            if (m == null)
                continue;
            localAVG = measurementAVG(m, id);
            if (coordinator) {
                // If the current node just became coordinator, unACKed averages are computed as well
                if (sentAverages.size() > 0) {
                    for (Average a : sentAverages.values()) {
                        avgBuffer.enqueue(a);
                        latestLocalAVG.add(a);
                    }
                    sentAverages.clear();
                }
                // Enqueue to local buffer for computation
                avgBuffer.enqueue(localAVG);
                latestLocalAVG.add(localAVG);
                System.out.println("[" + localAVG.getTimestamp() + "] " + localAVG.getId() + " | " + localAVG.getType() + " | " + localAVG.getValue());
            } else {
                // Setting ACK for current average: 'ID-TIMESTAMP'
                sentAverages.put(localAVG.getId() + "-" + localAVG.getTimestamp(), localAVG);
                // Send all computed averages not ACKed by the coordinator
                for (Average a : sentAverages.values()) {
                    Message sendMessage = new Message(Message.Type.MSG_AVERAGE, new Gson().toJson(a));
                    message = token + "§" + new Gson().toJson(sendMessage);
                    messageData = message.getBytes();
                    try {
                        messagePacket = new DatagramPacket(messageData, messageData.length, nodesGet(cid).getAddress(), nodesGet(cid).getNodePort());
                        nodeChannel.send(messagePacket);
                    } catch (NullPointerException e) {
                        if (!election) {
                            System.err.println("- WARNING: Invalid CID, requesting new election...");
                            election = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("[" + localAVG.getTimestamp() + "] lcl | " + localAVG.getValue());
            }
        }

        System.out.println("ValueWorker stopped!");
    }
}
