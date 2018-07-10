// This thread sends a multicastPacket to a given node to
// emulate a concurrent multicast multicastPacket delivery.

package handlers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MulticastHandler implements Runnable {

    private int id;
    private DatagramSocket nodeChannel;
    private DatagramPacket multicastPacket;

    public MulticastHandler(int id, DatagramSocket nodeChannel, DatagramPacket multicastPacket) {
        this.id = id;
        this.nodeChannel = nodeChannel;
        this.multicastPacket = multicastPacket;
    }

    @Override
    public void run() {
        try {
            nodeChannel.send(multicastPacket);
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't send message request to node " + id);
        }
    }
}
