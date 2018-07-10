// Thread-safe LIFO list (queue)

package synds;

import beans.Average;

import java.util.ArrayList;

public class SynQCronJob implements SynQ<Average> {

    private ArrayList<Average> buffer = new ArrayList<>();
    private boolean updated, end;

    // Insert item at the end of the queue
    public synchronized void enqueue(Average item) {
        buffer.add(item);
        updated = true;
        notify();
    }

    // Get all current records
    public synchronized ArrayList<Average> dequeue() {
        if (updated && !end) {
            updated = false;
            return new ArrayList<>(buffer);
        }
        return null;
    }

    // Close the production line
    public synchronized void shutdown() {
        end = true;
        notify();
    }
}
