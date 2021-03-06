package resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
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
	
	TBDResource tbdResource = new TBDResource();
	
	@GET
	@Path("/classify")
	@Produces(MediaType.APPLICATION_JSON)
	public Response classify(@QueryParam("userid") String userid) {
		//Response object.
		UserClassification response = new UserClassification();
		
        long parsedUserid = Long.parseLong(userid);
		Result result = tbdResource.queryModel(parsedUserid);
		
		//Set the fields in the response object.
		response.setUserid(parsedUserid);
		response.setLabel(result.getLabel());
		response.setFeatures(result.getFeatures());
		response.setProbability(result.getProbability());
		
		return Response.ok(response).build();
	}
	
	@POST
	@Path("/label")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response label(final UserLabel request) {
		String message;
		
		try {
			tbdResource.classifyUser(request.getUserid(), request.getLabel());
			message = "Successfully updated user:"+request.getUserid();
			
			return Response.ok(message).build();
		}
		catch (Exception e) {
			message = String.format("Failed to update user: %d with exception: %s", request.getUserid(), e.getClass().getName());
			
			return Response.serverError().entity(message).build();
		}
		
		
	}

}
