// Election request/reply message

package messages;

public class Election {

    public enum EType { ELECTION_ELECTION, ELECTION_ANSWER, ELECTION_COORDINATOR }
    private EType type;
    private int id;

    public Election(EType type, int id) {
        this.type = type;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public EType getType() {
        return type;
    }
}
