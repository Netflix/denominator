package denominator.verisignmdns;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import javax.inject.Provider;
import org.testng.annotations.Test;

import denominator.Credentials;
import denominator.verisignmdns.VrsnUtils;

public class VrsnUtilsTest {

	@Test
	public void TestGetMapOfCredentials() {
		Provider<Credentials> aCredentials = (Provider<Credentials>) VrsnMDNSTest
				.mockProviderCredentials();
		Map<String, String> actualResult = VrsnUtils
				.getMapOfCredentials(aCredentials);

		assertNotNull(actualResult);
		assertTrue(actualResult.containsKey("username"));
		assertTrue(actualResult.containsKey("password"));
		assertEquals(actualResult.get("username"), VrsnMDNSTest.TEST_USER_NAME);
		assertEquals(actualResult.get("password"), VrsnMDNSTest.TEST_PASSWORD);
	}
}
