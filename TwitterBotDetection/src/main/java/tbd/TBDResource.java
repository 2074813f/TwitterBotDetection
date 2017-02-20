package tbd;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class TBDResource {
	
	@GET
	@Produces("text/plain")
	public String helloWorld() {
		return "Hello World!";
	}

}
