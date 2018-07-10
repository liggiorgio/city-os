// Edge node thread dealing with sensors I/O

package workers;

import simulation.Measurement;
import synds.SynQBulk;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class NetSensorWorker extends Worker {

    private final SynQBulk sensorBuffer;

    public NetSensorWorker(SynQBulk sensorBuffer) {
        this.sensorBuffer = sensorBuffer;
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        byte[] receiveData;
        DatagramPacket receivePacket, sendPacket;

        while (!stop) {
            // Dispatch messages from sensors
            try {
                receiveData = new byte[256];
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                sensorChannel.receive(receivePacket);

                // Work out message from packet
                sendPacket = sensorDispatch(receivePacket);
                if (sendPacket==null)
                    continue;

                // Send reply back to sender
                sensorChannel.send(sendPacket);
            } catch (SocketException e) {
                System.out.println(" - Sensor channel closed!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("NetSensorWorker stopped!");
    }

    @Override
    // Manage incoming measurements from connected sensors
    Measurement parseMeasurement(Measurement measurement) {
        sensorBuffer.enqueue(measurement);
        return null;
    }

    @Override
    public void stopWorker() {
        sensorChannel.close();
        sensorBuffer.shutdown();
        super.stopWorker();
    }
}
