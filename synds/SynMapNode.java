package synds;

import beans.EdgeNode;

import java.util.ArrayList;
import java.util.HashMap;

public class SynMapNode implements SynMap<Integer, EdgeNode> {

    private HashMap<Integer, EdgeNode> map;

    public SynMapNode(HashMap<Integer, EdgeNode> hashMap) {
        map = new HashMap<>(hashMap);
    }

    private HashMap<Integer, EdgeNode> read() {
        return new HashMap<>(map);
    }

    public EdgeNode get(Integer i) {
        return read().get(i);
    }

    public synchronized void put(Integer k, EdgeNode v) {
        map.put(k, v);
    }

    public synchronized void replace(Integer k, EdgeNode v) {
        map.replace(k, v);
    }

    public synchronized void remove(Integer k) {
        map.remove(k);
    }

    public ArrayList<EdgeNode> values() {
        return new ArrayList<>(read().values());
    }

}
