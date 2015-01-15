package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.TEST_PASSWORD;
import static denominator.verisignmdns.VrsnMDNSTest.TEST_USER_NAME;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_ZONE_NAME1;
import static denominator.verisignmdns.VrsnMDNSTest.authFailureResponse;
import static denominator.verisignmdns.VrsnMDNSTest.mockZoneApi;
import static denominator.verisignmdns.VrsnMDNSTest.zoneListRequestTemplate;
import static denominator.verisignmdns.VrsnMDNSTest.zoneListResponse;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class VerisignMDNSZoneApiTest {
	@Test
	public void validResponseWithZoneList() throws IOException,
			InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(zoneListResponse));
		server.play();

		try {
			ZoneApi api = mockZoneApi(server.getPort());
			Zone zone = api.iterator().next();
			assertEquals(zone.name(), VALID_ZONE_NAME1);
			assertNull(zone.id());

			assertEquals(server.getRequestCount(), 1);

			String expectedRequest = format(zoneListRequestTemplate,
					TEST_USER_NAME, TEST_PASSWORD);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void authenticationFailResponse() throws IOException,
			InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(authFailureResponse));
		server.play();

		try {
			ZoneApi api = mockZoneApi(server.getPort());
			Iterator<Zone> iter = api.iterator();
			assertEquals(server.getRequestCount(), 1);
			assertFalse(iter.hasNext());

		} finally {
			server.shutdown();
		}
	}
}
