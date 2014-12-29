package denominator.verisignmdns;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import javax.inject.Provider;

import org.testng.annotations.Test;

import denominator.Credentials;
import denominator.verisignmdns.VrsnConstants;
import denominator.verisignmdns.VrsnUtils;

/**
 * @author sgavhane
 *
 */

public class VrsnUtilsTest {

	@Test
	public void TestGetMapOfCredentials() {
		Provider<Credentials> aCredentials = (Provider<Credentials>) VrsnMDNSTest
				.mockProviderCredentials();
		Map<String, String> actualResult = VrsnUtils
				.getMapOfCredentials(aCredentials);

		assertNotNull(actualResult);
		assertTrue(actualResult
				.containsKey(VrsnConstants.CREDENITAL_USERNAME_KEY));
		assertTrue(actualResult
				.containsKey(VrsnConstants.CREDENTIAL_PASSWORD_KEY));
		assertEquals(actualResult.get(VrsnConstants.CREDENITAL_USERNAME_KEY),
				VrsnMDNSTest.TEST_USER_NAME);
		assertEquals(actualResult.get(VrsnConstants.CREDENTIAL_PASSWORD_KEY),
				VrsnMDNSTest.TEST_PASSWORD);
	}
}
