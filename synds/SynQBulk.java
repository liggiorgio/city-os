// Thread-safe LIFO list (queue) for measurements

package synds;

import simulation.Measurement;

import java.util.ArrayList;

public class SynQBulk implements SynQ<Measurement> {

    private ArrayList<Measurement> buffer = new ArrayList<>();
    private boolean end;

    // Insert item at the end of the queue
    public synchronized void enqueue(Measurement item) {
        buffer.add(item);
        if (buffer.size() == 40)
            notify();
    }

    // Read the whole buffer
    public synchronized ArrayList<Measurement> dequeue() {
        ArrayList<Measurement> chunk = null;

        while (buffer.size() < 40 && !end) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (buffer.size() >= 40 && !end) {
            chunk = new ArrayList<>(buffer.subList(0, 40));
            buffer = new ArrayList<>(buffer.subList(20, buffer.size()));
        }

        return chunk;
    }

    // Close the production line
    public synchronized void shutdown() {
        end = true;
        notify();
    }
}
