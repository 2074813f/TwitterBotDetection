package resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource class for TBD project, providing rest API
 * paths.
 * 
 * @author Adam
 *
 */
@Path("/rest")
public class TBDResource {
	
	@GET
	@Path("/classify")
	@Produces(MediaType.APPLICATION_JSON)
	public Response classify(@QueryParam("userid") long userid) {
		//Expected response object.
		UserClassification response = new UserClassification();
		response.setUserid(userid);
		response.setLabel("human");
		
		return Response.ok(response).build();
	}

}
