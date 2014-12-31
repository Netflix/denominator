package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.METHODKEY;
import static denominator.verisignmdns.VrsnMDNSTest.mockResponse;
import static denominator.verisignmdns.VrsnMDNSTest.rrListInvalidZoneResponse;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * @author sgavhane
 *
 */
public class VrsnMdnsErrorDecoderTest {

	@Test
	public void decode() {
		Response response = mockResponse(rrListInvalidZoneResponse);
		ErrorDecoder errorDecoder = VrsnMDNSTest.mockErrorDecoder();

		Exception actualResult = errorDecoder.decode(METHODKEY, response);

		assertNotNull(actualResult);
	}

}
