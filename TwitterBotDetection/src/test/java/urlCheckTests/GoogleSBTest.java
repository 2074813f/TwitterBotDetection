package urlCheckTests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import features.URLChecker;

public class GoogleSBTest {

	private URLChecker checker;
	
	@Before
	public void setUp() {
		checker = new URLChecker();
	}
	
	@Test
	public void testEmptyUrls() {
		List<String> unknownURLs = new ArrayList<String>();
		checker.isSpamURLs(unknownURLs);
	}
}
