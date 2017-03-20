package restTests;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.collections.map.MultiValueMap;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Before;
import org.junit.Test;

import resources.TBDResource;
import resources.UserClassification;
import resources.UserLabel;
import tbd.TwitterBotDetection;

public class ServerTest extends JerseyTest {
	
	@Before
	public void setUpModels() throws IOException {
		//Get the properties file.
		Properties props = new Properties();
		FileInputStream in = new FileInputStream("src/main/resources/config.properties");
		props.load(in);
		in.close();
		
		TwitterBotDetection.buildModels(props);
	}
	
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
		expected.setUserid(791455969016442881l);
		
		UserClassification response = target("/rest/classify/")
				.queryParam("userid", 791455969016442881l)
				.request()
				.get(UserClassification.class);
		
		//We don't know the result of the classification so we can
		//Only check for a valid response.
		assertTrue(response != null);
		assertTrue(response.getLabel() != null && response.getUserid() == expected.getUserid());
	}
	
	@Test
	public void testLabel() {
		String userid = "791455969016442881";
		
		UserLabel request = new UserLabel();
		request.setUserid(Long.parseLong(userid));
		request.setLabel("human");
		
		String response = target("/rest/label")
				.request()
				.post(Entity.entity(request,MediaType.APPLICATION_JSON), String.class);
		
		assertTrue(response.compareTo("Successfully updated user:"+userid) == 0);
	}
}
