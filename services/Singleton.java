// Edge Nodes data singleton

package services;

import beans.Aggregate;
import beans.EdgeNode;
import beans.Average;
import simulation.Measurement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

final class Singleton {

    private static final Singleton self = new Singleton();
    private final long token;
    private HashMap<Integer, EdgeNode> nodes;
    private ArrayList<Aggregate> aggregates;
    private ArrayList<Measurement> globals;
    private HashMap<Integer, ArrayList<Measurement>> locals;

    // Constructor
    private Singleton() {
        token = makeToken();
        nodes = new HashMap<>();
        aggregates = new ArrayList<>();
        globals = new ArrayList<>();
        locals = new HashMap<>();
    }

    // Get self reference, create one if needed, for further usage
    static Singleton getInstance() {
        return self;
    }

    // Add a new edge node to the network
    synchronized ArrayList<EdgeNode> nodeAdd(EdgeNode e) {
        if (!nodes.containsKey(e.getId())) {
            if (nodes.isEmpty()) {
                nodes.put(e.getId(), e);
                return nodeList();
            } else {
                EdgeNode n = nodeNearest(e.getU(), e.getV());
                if (dist(e, n) >= 20) {
                    nodes.put(e.getId(), e);
                    return nodeList();      // Nodes in the network
                }
            }
            return new ArrayList<>();   // Too close to a node
        }
        return null;                    // Duplicate ID
    }

    // Remove an existing edge node from the network
    synchronized boolean nodeRemove(int id) {
        if (!nodes.containsKey(id))
            return false;
        nodes.remove(id);
        return true;
    }

    // Find out closest edge node to (u,v)
    EdgeNode nodeNearest(int u, int v) {
        ArrayList<EdgeNode> _nodes = nodeList();
        EdgeNode _nearest = null;
        int _minDist = 200, _tempDist;

        for (EdgeNode _node : _nodes) {
            _tempDist = dist(u, v, _node.getU(), _node.getV());
            if (_tempDist < _minDist) {
                _minDist = _tempDist;
                _nearest = _node;
            }
        }
        return _nearest;
    }

    // Get information about a node given its ID
    synchronized EdgeNode nodeGet(int id) {
        return nodes.getOrDefault(id, null);
    }

    // Get an array of the nodes set for further usage
    synchronized ArrayList<EdgeNode> nodeList() {
        return new ArrayList<>(nodes.values());
    }

    // Update values according to coordinator's latest data
    synchronized void averagePut(Aggregate a) {
        aggregates.add(a);
        globals.add(a.getGlobal());
        ArrayList<Average> localValues = a.getLocals();
        for (Measurement m : localValues)
            locals.computeIfAbsent(Integer.valueOf(m.getId()), ignored -> new ArrayList<>()).add(m);
    }

    // Get latest N aggregates
    synchronized ArrayList<Aggregate> averagesRead(int n) {
        n = Math.min(n, aggregates.size());
        return new ArrayList<>(aggregates.subList(aggregates.size() - n, aggregates.size()));
    }

    // Get latest N globals
    synchronized ArrayList<Measurement> globalsRead(int n) {
        n = Math.min(n, globals.size());
        return new ArrayList<>(globals.subList(globals.size() - n, globals.size()));
    }

    // Get latest N locals
    synchronized ArrayList<Measurement> localsRead(int id, int n) {
        ArrayList<Measurement> nodeLocals = locals.getOrDefault(id, null);
        if (nodeLocals != null) {
            n = Math.min(n, nodeLocals.size());
            return new ArrayList<>(nodeLocals.subList(nodeLocals.size() - n, nodeLocals.size()));
        }
        return null;
    }

    // Read token
    long getToken() {
        return token;
    }

    // Compute Manhattan distance
    private int dist(EdgeNode n1, EdgeNode n2) {
        return dist(n1.getU(), n1.getV(), n2.getU(), n2.getV());
    }

    private int dist(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    // Generate communication token for current server session
    private long makeToken() {
        Random rnd = new Random();
        rnd.setSeed(System.currentTimeMillis());
        return Math.abs(rnd.nextLong());
    }

}
