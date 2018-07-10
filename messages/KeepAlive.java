// Keep-alive request/reply message

package messages;

public class KeepAlive {

    public enum KAType { KA_NODE_REQUEST, KA_NODE_REPLY, KA_SENSOR_REQUEST, KA_SENSOR_REPLY }
    private KAType type;
    private int id;

    public KeepAlive(KAType type, int id) {
        this.type = type;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public KAType getType() {
        return type;
    }
}
