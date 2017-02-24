package resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
public class TBDService {
	
	//TODO: Construct resource class to provide interface for classifying etc.
	TBDResource tbdResource = new TBDResource();
	
	@GET
	@Path("/classify")
	@Produces(MediaType.APPLICATION_JSON)
	public Response classify(@QueryParam("userid") long userid) {
		//Response object.
		UserClassification response = new UserClassification();
		
		String label = tbdResource.queryModel(userid);
		
		//Set the fields in the response object.
		response.setUserid(userid);
		response.setLabel(label);
		
		//TODO: allow throwing errors to here and catch, respond with error status code.
		
		return Response.ok(response).build();
	}
	
	@POST
	@Path("/label")
	@Produces(MediaType.APPLICATION_JSON)
	public Response label(@QueryParam("userid") long userid, @QueryParam ("label") String label) {
		String message;
		
		try {
			tbdResource.classifyUser(userid, label);
			message = "Successfully updated user:"+userid;
			
			return Response.ok(message).build();
		}
		catch (Exception e) {
			message = String.format("Failed to update user: %d with exception: %s", userid, e.getClass().getName());
			
			return Response.serverError().entity(message).build();
		}
		
		
	}

}
