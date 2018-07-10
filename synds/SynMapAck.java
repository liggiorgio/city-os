package synds;

import beans.Average;

import java.util.ArrayList;
import java.util.HashMap;

public class SynMapAck implements SynMap<String, Average> {

    private HashMap<String, Average> map;

    public SynMapAck() {
        map = new HashMap<>();
    }

    private HashMap<String, Average> read() {
        return new HashMap<>(map);
    }

    public Average get(String i) {
        return read().get(i);
    }

    public synchronized void put(String k, Average v) {
        map.put(k, v);
    }

    public synchronized void replace(String k, Average v) {
        map.replace(k, v);
    }

    public synchronized void remove(String k) {
        map.remove(k);
    }

    public ArrayList<Average> values() {
        return new ArrayList<>(read().values());
    }

    public int size() {
        return read().size();
    }

    public synchronized void clear() {
        map.clear();
    }
}
