// Hello request/reply message

package messages;

public class Hello {

    public enum HType { HELLO_REQUEST, HELLO_REPLY, HELLO_COORDINATOR }
    private HType type;
    private int id;

    public Hello(HType type, int id) {
        this.type = type;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public HType getType() {
        return type;
    }
}
