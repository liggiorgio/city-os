package synds;

import java.util.ArrayList;
import java.util.HashMap;

public class SynMapFlag implements SynMap<Integer, Boolean> {

    private HashMap<Integer, Boolean> map;

    public SynMapFlag() {
        map = new HashMap<>();
    }

    private HashMap<Integer, Boolean> read() {
        return new HashMap<>(map);
    }

    public Boolean get(Integer i) {
        return read().get(i);
    }

    public synchronized void put(Integer k, Boolean v) {
        map.put(k, v);
    }

    public synchronized void replace(Integer k, Boolean v) {
        map.replace(k, v);
    }

    public synchronized void remove(Integer k) {
        map.remove(k);
    }

    public ArrayList<Boolean> values() {
        return new ArrayList<>(read().values());
    }

}
