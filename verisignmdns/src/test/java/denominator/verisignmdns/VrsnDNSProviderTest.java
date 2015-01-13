package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.VALID_URL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;

import org.testng.annotations.Test;

import denominator.verisignmdns.VerisignMDNSProvider;

public class VrsnDNSProviderTest {

	@Test
	public void credentialTypeToParameterNames() {
		VerisignMDNSProvider vrsnDNSProvider = new VerisignMDNSProvider();
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
		VerisignMDNSProvider vrsnDNSProvider = new VerisignMDNSProvider();
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
		VerisignMDNSProvider vrsnDNSProvider = new VerisignMDNSProvider();
		String actualResult = vrsnDNSProvider.url();
		assertNotNull(actualResult);

		assertEquals(actualResult, VALID_URL);
	}
}
