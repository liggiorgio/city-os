// Data structure sent to server to store averages from nodes

package beans;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Aggregate {

    private Average global;
    private ArrayList<Average> local;

    @JsonCreator
    public Aggregate(@JsonProperty("global") Average global,
                     @JsonProperty("local") ArrayList<Average> local) {
        this.global = global;
        this.local = local;
    }

    public Average getGlobal() {
        return global;
    }

    public ArrayList<Average> getLocals() {
        return local;
    }
}
