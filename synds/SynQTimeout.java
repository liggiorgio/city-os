// Thread-safe LIFO list (queue)

package synds;

import java.util.ArrayList;

public class SynQTimeout implements SynQ {

    private ArrayList<Object> buffer = new ArrayList<>();
    private boolean woke, end;

    // Insert item at the end of the queue
    public synchronized void enqueue(Object value) {
        buffer.add(value);
        woke = true;
        notify();
    }

    // This DS needs time value to work properly
    public ArrayList<Object> dequeue() {
        return dequeue(0);
    }

    // Read the whole buffer
    public synchronized ArrayList<Object> dequeue(int time) {
        ArrayList<Object> value = new ArrayList<>();

        if (buffer.isEmpty() && !end) {
            try {
                wait(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!buffer.isEmpty() && woke && !end) {
            value.add(buffer.get(0));
            buffer.remove(0);
        }

        return value;
    }

    // Clear the queue
    public synchronized void clear() {
        buffer.clear();
    }

    // Close the production line
    public synchronized void shutdown() {
        end = true;
        notify();
    }
}
