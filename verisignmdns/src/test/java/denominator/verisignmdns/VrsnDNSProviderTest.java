package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.VALID_URL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;

import org.testng.annotations.Test;

import denominator.verisignmdns.VrsnDNSProvider;

/**
 * @author sgavhane
 *
 */
public class VrsnDNSProviderTest {

	@Test
	public void credentialTypeToParameterNames() {
		VrsnDNSProvider vrsnDNSProvider = new VrsnDNSProvider();
		Map<String, Collection<String>> actualResult = vrsnDNSProvider
				.credentialTypeToParameterNames();
		assertNotNull(actualResult);

		Map<String, Collection<String>> expectedResult = VrsnMDNSTest
				.MockcredentialTypeToParameterNamesResponse();
		assertNotNull(expectedResult);
		assertEquals(actualResult.containsKey("password"),
				expectedResult.containsKey("password"));
	}

	@Test
	public void profileToRecordTypes() {
		VrsnDNSProvider vrsnDNSProvider = new VrsnDNSProvider();
		Map<String, Collection<String>> actualResult = vrsnDNSProvider
				.profileToRecordTypes();
		assertNotNull(actualResult);

		Map<String, Collection<String>> expectedResult = VrsnMDNSTest
				.mockProfileToRecordTypesResponse();
		assertNotNull(actualResult);

		assertEquals(actualResult, expectedResult);
	}

	@Test
	public void url() {
		VrsnDNSProvider vrsnDNSProvider = new VrsnDNSProvider();
		String actualResult = vrsnDNSProvider.url();
		assertNotNull(actualResult);

		assertEquals(actualResult, VALID_URL);
	}
}
