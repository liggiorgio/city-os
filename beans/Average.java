package beans;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import simulation.Measurement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Average extends Measurement {

    public Average(Measurement m) {
        this(m.getId(), m.getType(), m.getValue(), m.getTimestamp());
    }

    @JsonCreator
    public Average(@JsonProperty("id") String id,
                   @JsonProperty("type") String type,
                   @JsonProperty("value") double value,
                   @JsonProperty("timestamp") long timestamp) {
        super(id, type, value, timestamp);
    }

}
