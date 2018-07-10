// PM10 sensor unit

package sensors;

import simulation.PM10Simulator;

public class PM10Sensor extends Sensor {

    // Base constructor
    private PM10Sensor() {
        super();
        this.simulator = new PM10Simulator(this);
        System.out.println("\t- " + simulator.getIdentifier() + "\t @ (" + u + ", " + v + ")");
    }

    // Public constructor with cloud server address
    public PM10Sensor(String serverURL, int serverPort) {
        this();
        this.serverAddress = serverURL;
        this.serverPort = serverPort;
        getNode();
        this.simulator.start();
    }
}
