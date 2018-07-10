/*
** Base communication protocol message data type.
** Packet:  [ProtocolId][Message]
** Message: [KAType][KeepAlive/Hello/Election/Measurement]
** K/H/E:   [KAType][ID]
** M:       [ID][type][Value][Timestamp]
*/

package messages;

public final class Message {

    public enum Type { MSG_HELLO, MSG_KA, MSG_ELECTION, MSG_AVERAGE, MSG_MEASUREMENT }
    private Type type;
    private String data;

    public Message(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }
}
