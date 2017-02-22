package restTests;

import static org.junit.Assert.*;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import resources.UserClassification;

public class ServerTest extends JerseyTest {
	
	@Override
	protected Application configure() {
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages("resources");
		resourceConfig.register(JacksonFeature.class);
		
		return resourceConfig;
	}
	
	@Override
	protected void configureClient(ClientConfig config) {
		config.register(JacksonFeature.class);
	}
	
	@Test
	public void testClassify() {
		UserClassification expected = new UserClassification();
		expected.setLabel("human");
		expected.setUserid(2074813);
		
		final UserClassification response = target("/rest/classify/")
				.queryParam("userid", 2074813)
				.request()
				.get(UserClassification.class);
		
		assertTrue(response.equals(expected));
	}
}
