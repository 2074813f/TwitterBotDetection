package modelTests;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.spark.ml.PipelineModel;
import org.junit.Before;
import org.junit.Test;

import resources.TBDResource;
import tbd.TwitterBotDetection;

public class PipelineModelTest {
	
	PipelineModel model;
	TBDResource tbdResource;
	
	@Before
	public void setUp() throws IOException {
		TwitterBotDetection.buildModels();
		
		tbdResource = new TBDResource();
	}
	
	@Test
	public void testClassification() {
		long testuserid = 791455969016442881l;
		
		assertTrue(tbdResource.queryModel(testuserid) != null);
	}

}
