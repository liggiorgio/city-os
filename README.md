# CityOS - Edge computing solution for smart cities

This school projects develops a P2P network of edge nodes which collect measurements from simulated PM-10 sensors deployed in a grid representing the city, and compute local averages and sending them to a coordinator node; then it computes global averages and sends them back to other nodes, and upload all data to a RESTful web server. There's also a client app that queries the server to read the data uploaded.

## Artifacts

- **Cloud server** - Main cloud services, stores data and replies to external queries.
- **Edge node** - Collects measurements and participates to the network topology.
  - <u>Joining</u>: not active yet;
  - <u>Active</u>: stores value, send to coordinator;
  - <u>Coordinator</u>: receives values, replies to nodes, sends to server.
- **Client** - Lets analysts query the server and read stored values.



## Project constraints

- Network is supposed to take place in a 100 by 100 discrete grid;
- Edge nodes can't be closer than 20 units (Manhattan distance) to each other;
- Sensor measurements *can* be lost if no node's listening; edge nodes averages *shouldn't*;
- Nodes must be executed with command-line parameters as starting configuration;
- The system must be robust: all exception must be correctly handled without compromising a node's operativity;
- Robust election algorithm to elect a coordinator node without ambiguities, but if coordinator already exists new nodes mustn't override its authority;
- Tasks and queries can be asynchronous, concurrency must be solved with parallelism solutions;
- Cloud server must be stateless, hence REST interface.



## Project features

- Election is managed with a variation of the Bully election algorithm: the process with highest ID is entitled to be the coordinator, exception when first joining the network if a coordinator is already in charge;
- Communication protocol for sensors and edge nodes is built on top of UDP, while communication with the server is based on HTTP requests;
- Web server REST interface is developed using Jersey + JAXB libraries;
- Objects and messages marshalling/unmarshalling is achieved using Gson libraries by Google;
- Thread coordination and concurrent access to shared resources is achieved using synchronous data variables and data structures.



## Data types

- **Measurement/Average** - Contains sensor/node measurements/averages, plus additional information such as sensor/node ID, timestamp, and more.
- **Aggregate** - Stores a global average and the local averages used to compute it, labeled with the process ID that sent them; it can be seen as a city status snapshot.
- **Statistic** - Contains mean value and standard deviation computed as reply for the client application.
- **Message** - This object is used as a wrapper for the UDP-based communication data.
- **EdgeNode** - Contains information about a new node (process) joining the network.



## Protocol messages

Nodes and sensors communicate sending the following type of messages:

- **Keep-alive (request/reply)** - Used to detect if the recipient process is online; sent every two seconds.
  - <u>Sensors</u>: if the node doesn't reply, the server is queried to connect to another node;
  - <u>Edge nodes</u>: if the coordinator doesn't reply, a new election message is sent to all nodes in the network.
- **Measurement (request/reply)** - Contains data collected by sensors, or computed by edge nodes.
  - <u>Sensors</u>: PM-10 values sent as data stream;
  - <u>Edge nodes</u>: local or global averages sent among nodes.
- **Hello (request/reply/coordinator)**: greeting protocol when first joining the network. Edge nodes-only.
- **Election (request/reply/coordinator)**: election protocol when the current coordinator disconnects and its absence is detected by any node, or when no coordinator is found after joining the network. Edge nodes-only.



## API overview

Data collected is sent to a RESTful web server by the coordinator node, the following services are provided:

- **Init** - Request the token used by nodes and sensors to send each other messages, and upload aggregate data on the server.
- **Nodes** - This interface allows to interact with edge node-related services, such as: list of active nodes, nearest node to a grid position, add or remove a node in the network.
- **Measurements** - These services deal with gathered averages, both local and global, and provide sub-services to compute statistics on the fly, push computed values, query statistics.



## API quick reference

All data values returned by the following services are intended to be in JSON format, unless stated otherwise.

### Init service

- `api/init`

  **Description**: get the protocol token
  **Method**: GET
  **Parameters**: none
  **Returns**: *long* (plain text)

  | Status code | Description                 |
  | ----------- | --------------------------- |
  | 200         | Token obtained successfully |

### Nodes services

- `api/nodes`

  **Description**: returns a list of edge nodes currently connected to the network
  **Method**: GET
  **Parameters**: none
  **Returns**: *EdgeNode[]*

  | Status code | Description                              |
  | ----------- | ---------------------------------------- |
  | 200         | List returned correctly                  |
  | 404         | There are no nodes connected to the network |

  **Description**: add a node to the network
  **Method**: POST
  **Parameters**: *EdgeNode*
  **Returns**: *EdgeNode[]* containing information for the current connecting node

  | Status code | Description                              |
  | ----------- | ---------------------------------------- |
  | 200         | Node connected, list containing all other nodes plus self returned successfully |
  | 400         | Wrong parameter format                   |
  | 403         | Current node would be too close to another one in the network |
  | 409         | The ID of the current node is already taken |

  **Description**: remove a node from the network
  **Method**: DELETE
  **Parameters**: *EdgeNode*
  **Returns**: no content

  | Status code | Description                         |
  | ----------- | ----------------------------------- |
  | 204         | Node disconnected successfully      |
  | 404         | There is no such node to be removed |

- `api/node/nearest`

  **Description**: get the closest node to a grid cell
  **Method**: GET
  **Parameters**: *int* u, *int* v, horizontal and vertical coordinates in the grid respectively
  **Returns**: *EdgeNode*

  | Status code | Description                              |
  | ----------- | ---------------------------------------- |
  | 200         | Nearest node returned successfully       |
  | 400         | Parameters missing or out of bounds      |
  | 404         | There are no nodes connected to the network |

### Measurements services

- `api/measurements`

  **Description**: get latest *n* aggregates
  **Method**: GET
  **Parameters**: *int* n, number of desired aggregate values
  **Returns**: *Aggregate[]*, or no content

  | Status code | Description                              |
  | ----------- | ---------------------------------------- |
  | 200         | List of aggregate values returned successfully |
  | 204         | There are no values yet                  |
  | 400         | Parameter missing or out of bounds       |

  **Description**: upload a new aggregate value
  **Method**: POST
  **Parameters**: *Aggregate*
  **Returns**: no content

  | Status code | Description                 |
  | ----------- | --------------------------- |
  | 204         | Value uploaded successfully |

- `api/measurements/{id}`

  **Description**: get latest *n* local averages computed by a given node in the network
  **Method**: GET
  **Parameters**: *int* id, *int* n, process ID and number of desired values
  **Returns**: *Average[]*, or no content

  | Status code | Description                         |
  | ----------- | ----------------------------------- |
  | 200         | Averages returned successfully      |
  | 204         | There are no values yet             |
  | 400         | Parameters missing or out of bounds |
  | 404         | There are no nodes with such ID     |

- `api/measurements/stats`

  **Description**: get mean value and standard deviation of latest *n* global averages
  **Method**: GET
  **Parameters**: *int* n, number of desider values
  **Returns**: *Statistic*, or no content

  | Status code | Description                              |
  | ----------- | ---------------------------------------- |
  | 200         | Statistics returned successfully         |
  | 204         | There are no values to compute statistics on |
  | 400         | Parameter missing or out of bounds       |

- `api/measurements/stats/{id}`

  **Description**: get mean value and standard deviation of latest *n* local averages computed by a given node in the network
  **Method**: GET
  **Parameters**: *int* id, *int* n, process ID and number of desired values*
  **Returns**: *Statistic*, or no content

  | Status code | Description                              |
  | ----------- | ---------------------------------------- |
  | 200         | Statistics returned successfully         |
  | 204         | There are no values to compute statistics on |
  | 400         | Parameters missing or out of bounds      |
  | 404         | There are no nodes with such ID          |

## Classes and packages overview

- **Simulation** - Provides a simulation environment to generate values to be collected and computed.
- **Sensors** - Provides agents in charge of sending measurements.
- **Beans** - Main data types stored and sent over the network.
- **Messages** - Provides message types used in the communication protocol, and a wrapper for them.
- **SynDS** - Custom implementation of thread-safe sunchronous shared buffers and data structures.
- **Workers** - Classes implementing long-term runnable threads for edge nodes' functionalities.
- **Handlers** - Classes implementing delegated runnable threads for edge nodes' functionalities.
- **Services** - Implementation of web services and singleton dispatcher.

## Applications setup

The four applications must be run with some command-line arguments.

- **ServerApp** - Server address is obtained automatically; server port is hard-coded.

- **NodeApp** - `HOSTNAME:PORT` or `ID NODEPORT SENSORPORT HOSTNAME:PORT`

  The app resembles an edge node in the network.`ID` is the process ID assigned, `NODEPORT` and `SENSORPORT` are socket ports used for two-ways communication with nodes and sensors respectively, and `HOSTNAME:PORT` are the web server address and dedicated port. If only the address and port arguments are provided, ID and other ports are chosen randomly.

- **SimulationApp** - `SENSORNUMBER HOSTNAME:PORT`

  This application deploys `SENSORNUMBER` number of *PM10Sensor* threads, each one is a different sensor in the city network, and waits for them to stop their execution.

- **ClientApp** - Server address and port are hard-coded.

## Election

The election algorithm implements the Bully algorithm, which can be executed any time one of the standard edge nodes detects the coordinator isn't online anymore. The failure detector is based on a timeout on the keep-alive reply message not delivered by the coordinator. Unlike the original algorithm, elections don't take place when a node first joins the network and there's already a coordinator in charge, even if the joining process has a higher ID than coordinator's.

## Edge cases

There are some particular stress situations the developed network is able to deal with.

#### Two nodes join concurrently

On the server side, we have concurrency management via critical sections; on the network side, edge nodes are able to hello-reply when still hello-requesting.

#### A node joins when another one is leaving

The Hello protocol detects offline nodes at startup, allowing the requesting node to have an up-to-date list of other nodes in the network.

#### A node joins during an election

When first joining the network, a node can only accept hello requests, hello replies, and a hello reply from the coordinator; all other messages are discarded. If an election is occuring, the new node will join the network without any knowledge of a coordinator, and finally requesting a new election shortly after. It is allowed to do so, as when joining the network there was no actual coordinator, thus the election constraint isn't violated.

#### A node leaves during an election

The Bully election algorithm is tolerant to failures of nodes even if an election is taking place. If the leaving node was supposed to be the new coordinator, all other nodes will request a new election after a timeout is over and no coordinator message was received; otherwise, the occurring election will be over but the message sent to the leaving node won't simply be delivered.

#### Measurements sent to an offline node

Sensor measurements are supposed to be a data stream, and some measurements can be lost during the time spent by a sensor for requesting a new node address to the web server. As well, if sensors are online but there are no edge nodes connected, all measurements will be lost.

#### Averages sent to an offline coordinator

An acknowledgement system takes care of which averages are successfully delivered to the coordinator by any node. When sending local averages to the coordinator, these are stored in a temporary data stack instead of being discarded; anytime the coordinator receives a local average from a node, it replies back with the latest global average computed, plus the ID of the latest local average received. A node receiving the reply from the coordinator can then safely remove the corresponding entry from its own stack, assuming it was received. Otherwise, after a coordinator has been elected, all averages stored in the stack are sent to the new coordinator, ACK-ing them as well.

#### The coordinator leaves before a global average is computed

A global average is computed every five seconds. If a coordinator leaves the network during this time interval, a global average is computed on the fly and sent to the server, discarding other incoming packets from other nodes, so no values are discarded by mistake.

#### The cloud server is offline

The cloud server isn't supposed to be offline at the moment, so the coordinator isn't able to delay the upload task. This edge case will be taken care of in a future commit.