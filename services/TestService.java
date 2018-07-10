// Cloud services provided for testing purposes

package services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("api/test")
public class TestService {

    // Echo service
    @GET
    public Response testEcho() {
        return Response.ok().build();
    }

}
