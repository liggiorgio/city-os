// Data structure storing standard deviation and average

package beans;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Statistic {

    private double mn;
    private double sd;

    @JsonCreator
    public Statistic (@JsonProperty("mn") double mn,
                      @JsonProperty("sd") double sd) {
        this.mn = mn;
        this.sd = sd;
    }

    public double getMn() {
        return mn;
    }

    public double getSd() {
        return sd;
    }

}
