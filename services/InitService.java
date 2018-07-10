// Cloud services provided for testing purposes

package services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("api/init")
public class InitService {

    // Get protocol token
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response tokenGet() {
        return Response.ok(Singleton.getInstance().getToken()).build();
    }

}
