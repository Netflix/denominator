package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.VALID_OWNER1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RR_TYPE1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RR_TYPE3;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_TTL1;
import static denominator.verisignmdns.VrsnMDNSTest.mockResourceRecordSetApi;
import static denominator.verisignmdns.VrsnMDNSTest.nAPTRDataResponse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.verisignmdns.VrsnMdnsRequestHelper;

/**
 * @author sgavhane
 *
 */
public class VrsnMdnsRequestHelperTest {

	@Test
	public void getNAPTRData() throws IOException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(nAPTRDataResponse));
		server.play();

		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			ResourceRecordSet<?> rrSet = api.getByNameAndType(VALID_OWNER1,
					VALID_RR_TYPE1);
			String actualNAPTRData = VrsnMdnsRequestHelper.getNAPTRData(rrSet);
			assertNotNull(actualNAPTRData);
			assertNotNull(rrSet);
			assertEquals(rrSet.ttl(), new Integer(Integer.parseInt(VALID_TTL1)));
			assertEquals(rrSet.type(), VALID_RR_TYPE3);
			assertEquals(rrSet.name(), VALID_OWNER1);
			assertNotNull(rrSet.records());
		} finally {
			server.shutdown();
		}
	}
}
