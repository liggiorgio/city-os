// Cloud services provided for nodes

package services;

import beans.EdgeNode;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("api/nodes")
public class NodesService {

    // Returns set of edge node currently active in the grid
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response nodesStatus() {
        ArrayList<EdgeNode> nodes = Singleton.getInstance().nodeList();

        // Return '200 OK': edge nodes list returned
        if (nodes.size() > 0) {
            return Response.ok(nodes).build();
        }

        // Return '404 Not Found': city grid is empty
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Returns closest edge node available given sensor coordinates
    @GET
    @Path("/nearest")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response nodesNearest(@QueryParam("u") @DefaultValue("-1") int u,
                                 @QueryParam("v") @DefaultValue("-1") int v) {
        System.out.println("Sensor requesting nearest node @ (" + u + ", " + v + ")");

        // Return '400 Bad Request': parameters missing or out of bounds
        if ((u<0)||(v<0))
            return Response.status(Response.Status.BAD_REQUEST).build();

        EdgeNode nearest = Singleton.getInstance().nodeNearest(u, v);

        // Return '200 OK': nearest node returned
        if (nearest!=null) {
            return Response.ok(nearest).build();
        }

        // Return '404 Not Found': node doesn't exists
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Add requesting node to the network
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response nodesAdd(EdgeNode e) {
        System.out.println("Node " + e.getId() + " requesting to join the network...");
        ArrayList<EdgeNode> list = Singleton.getInstance().nodeAdd(e);

        // Return '400 Bad Request': Json format is wrong

        // Return '409 Conflict': edge node ID is duplicate
        if (list == null) {
            System.err.println("Node " + e.getId() + " has a duplicate ID.");
            return Response.status(Response.Status.CONFLICT).build();
        }

        // Return '403 Forbidden': edge node is too close to another one
        if (list.isEmpty()) {
            System.err.println("Node " + e.getId() + " is too close to another node.");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Return '200 OK': node joined the network, nodes list is returned
        System.out.println("Node " + e.getId() + " joined the network!");
        return Response.ok(list).build();
    }

    // Remove requesting node from the network
    @DELETE
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response nodesRemove(EdgeNode e) {
        System.out.println("Node " + e.getId() + " requesting to leave the network...");
        EdgeNode stored = Singleton.getInstance().nodeGet(e.getId());

        // Return '204 No Content' : node successfully removed
        if (e.equals(stored)) {
            if (Singleton.getInstance().nodeRemove(e.getId())) {
                System.out.println("Node " + e.getId() + " left the network!");
                return Response.noContent().build();
            }
        }

        // Return '404 Not Found' : no such ID
        System.out.println("Node " + e.getId() + " doesn't exists.");
        return Response.status(Response.Status.NOT_FOUND).build();
    }

}
