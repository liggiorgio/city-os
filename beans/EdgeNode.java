// Edge Node data structure

package beans;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.InetAddress;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class EdgeNode {

    private InetAddress address;
    private int id, nodePort, sensorPort, u, v;

    @JsonCreator
    public EdgeNode(@JsonProperty("id") int id,
                    @JsonProperty("address") InetAddress address,
                    @JsonProperty("nodePort") int nodePort,
                    @JsonProperty("sensorPort") int sensorPort,
                    @JsonProperty("u") int u,
                    @JsonProperty("v") int v) {
        this.id = id;
        this.address = address;
        this.nodePort = nodePort;
        this.sensorPort = sensorPort;
        this.u = u;
        this.v = v;
    }

    public int getId() {
        return id;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getNodePort() {
        return nodePort;
    }

    public int getSensorPort() {
        return sensorPort;
    }

    public int getU() {
        return u;
    }

    public int getV() {
        return v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        EdgeNode n = (EdgeNode) o;
        return (id == n.id) && (address.equals(n.address)) && (nodePort == n.nodePort) && (sensorPort == n.sensorPort) && (u == n.u) && (v == n.v);
    }
}
