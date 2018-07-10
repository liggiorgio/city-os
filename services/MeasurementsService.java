// Cloud services provided for measurements

package services;

import beans.Aggregate;
import beans.Average;
import beans.Statistic;
import simulation.Measurement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("api/measurements")
public class MeasurementsService {

    // Returns latest N global values and related local values
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response measurementsGlobal(@QueryParam("n") @DefaultValue("-1") int n) {
        System.out.println("Client requesting latest " + n +" global measurements");

        // Return '400 Bad Request': parameters missing or out of bounds
        if (n<=0)
            return Response.status(Response.Status.BAD_REQUEST).build();

        ArrayList<Aggregate> aggregates = Singleton.getInstance().averagesRead(n);

        // Return '200 OK': latest n measurements returned
        // Return '204 No Content': no measurements yet
        if (aggregates.size() > 0)
            return Response.ok(aggregates).build();
        else
            return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response measurementsLocals(@PathParam("id") @DefaultValue("-1") int id, @QueryParam("n")@DefaultValue("0") int n) {
        System.out.println("Client requesting ID:" + id + "'s latest " + n +" local measurements");

        // Return '400 Bad Request': parameters missing or out of bounds
        if (n<=0)
            return Response.status(Response.Status.BAD_REQUEST).build();

        ArrayList<Measurement> _values = Singleton.getInstance().localsRead(id, n);

        // Return '404 Not Found': node doesn't exists
        if (_values == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        // Return '200 OK': latest n measurements returned
        // Return '204 No Content': no measurements yet
        if (_values.size() > 0) {
            ArrayList<Average> locals = new ArrayList<>();
            for (Measurement m : _values)
                locals.add(new Average(m));
            return Response.ok(locals).build();
        }
        else
            return Response.noContent().build();
    }

    // Push global statistic from the coordinator
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response nodePush(Aggregate a) {
        System.out.println("Coordinator node requesting to upload new data...");
        Singleton.getInstance().averagePut(a);
        return Response.noContent().build();
    }

    // Returns latest N global values and related local values
    @GET
    @Path("/stats")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response statsGlobal(@QueryParam("n") @DefaultValue("-1") int n) {
        System.out.println("Client requesting latest " + n +" global stats");

        // Return '400 Bad Request': parameters missing or out of bounds
        if (n<=0)
            return Response.status(Response.Status.BAD_REQUEST).build();

        ArrayList<Measurement> globals = Singleton.getInstance().globalsRead(n);

        // Return '200 OK': latest n measurements stats returned
        // Return '204 No Content': no measurements yet
        if (globals.size() > 0) {
            double mn = mean(globals);
            double sd = stdDev(globals, mn);
            return Response.ok(new Statistic(mn, sd)).build();
        }
        else
            return Response.noContent().build();
    }

    @GET
    @Path("/stats/{id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response statsLocals(@PathParam("id") @DefaultValue("-1") int id, @QueryParam("n")@DefaultValue("0") int n) {
        System.out.println("Client requesting ID:" + id + "'s latest " + n +" local stats");

        // Return '400 Bad Request': parameters missing or out of bounds
        if (n<=0)
            return Response.status(Response.Status.BAD_REQUEST).build();

        ArrayList<Measurement> locals = Singleton.getInstance().localsRead(id, n);

        // Return '404 Not Found': node doesn't exists
        if (locals == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        // Return '200 OK': latest n measurements stats returned
        // Return '204 No Content': no measurements yet
        if (locals.size() > 0) {
            double mn = mean(locals);
            double sd = stdDev(locals, mn);
            return Response.ok(new Statistic(mn, sd)).build();
        }
        else
            return Response.noContent().build();
    }

    // Compute one-pass mean
    private double mean(ArrayList<Measurement> values) {
        double mean = 0;
        for (Measurement v : values)
            mean += v.getValue();
        return mean/values.size();
    }

    // Compute two-passes standard deviation
    private double stdDev(ArrayList<Measurement> values, double mean) {
        double squaredDiff = 0;
        for (Measurement v : values) {
            double diff = v.getValue() - mean;
            squaredDiff += diff * diff;
        }
        double variance = squaredDiff / values.size();
        return Math.sqrt(variance);
    }

}
