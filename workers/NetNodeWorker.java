// Edge node thread dealing with nodes I/O

package workers;

import beans.Average;
import synds.SynMapAck;
import synds.SynQCronJob;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class NetNodeWorker extends Worker {

    private final SynQCronJob avgBuffer;
    private final SynMapAck sentAverages;

    public NetNodeWorker(SynQCronJob avgBuffer, SynMapAck sentAverages) {
        this.avgBuffer = avgBuffer;
        this.sentAverages = sentAverages;
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        byte[] receiveData;
        DatagramPacket receivePacket, sendPacket;

        while (!stop) {
            // Dispatch messages from other nodes
            try {
                receiveData = new byte[256];
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                nodeChannel.receive(receivePacket);

                // Work out message from packet
                sendPacket = nodeDispatch(receivePacket);
                if (sendPacket==null)
                    continue;

                // Send reply back to sender
                nodeChannel.send(sendPacket);
            } catch (SocketException e) {
                System.out.println(" - Node channel closed!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("NetNodeWorker stopped!");
    }

    @Override
    // Manage incoming local average from other nodes
    Average parseAverage(Average average) {
        if (coordinator) {
            // Store local average and reply with global average
            avgBuffer.enqueue(average);
            latestLocalAVG.add(average);
            int sender = Integer.valueOf(average.getId());
            System.out.println("[" + average.getTimestamp() + "] " + sender + " | " + average.getType() + " | " + average.getValue());
            // Send global average only if node needs update
            if (!flagStatus(sender)) {
                flagRaise(sender);
                Average global = new Average(latestGlobalAVG);
                global.setId(average.getId() + "-" + average.getTimestamp());
                return global;
            }
            return new Average(average.getId() + "-" + average.getTimestamp(), "gbl", 0, 0);
        } else {
            // Coordinator sent global average, store and display
            if (average != null)
            {
                // GBL average ID is the reply to the previous avg sent. Use this value to discard previous avg from values stack
                if (average.getType().equals("gbl")) {
                    if (average.getValue() == 0.0 && average.getTimestamp() == 0) {
                        System.out.println("[" + latestGlobalAVG.getTimestamp() + "] " + "gbl | " + latestGlobalAVG.getValue() + " (unchanged)");
                    } else {
                        latestGlobalAVG = average;
                        System.out.println("[" + latestGlobalAVG.getTimestamp() + "] " + "gbl | " + latestGlobalAVG.getValue());
                    }
                    sentAverages.remove(average.getId());
                }
            }
            else
                System.out.println("Read a broken packet");
            return null;
        }
    }

    @Override
    public void stopWorker() {
        nodeChannel.close();
        avgBuffer.shutdown();
        super.stopWorker();
    }
}
