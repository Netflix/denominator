package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.RESOURCE_RECORD_ID;
import static denominator.verisignmdns.VrsnMDNSTest.TEST_PASSWORD;
import static denominator.verisignmdns.VrsnMDNSTest.TEST_USER_NAME;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_OWNER1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RDATA1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RR_TYPE1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_TTL1;
import static denominator.verisignmdns.VrsnMDNSTest.mockAllProfileResourceRecordSetApi;
import static denominator.verisignmdns.VrsnMDNSTest.rrListCNAMETypesResponse;
import static denominator.verisignmdns.VrsnMDNSTest.rrListCNAMETypesTemplete;
import static denominator.verisignmdns.VrsnMDNSTest.rrListValildResponse;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.CNAMEData;
import denominator.verisignmdns.VerisignMDNSAllProfileResourceRecordSetApi;

public class VerisignMDNSAllProfileResourceRecordSetApiTest {

	@Test
	public void getByNameTypeAndQualifier() throws IOException,
			InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListCNAMETypesResponse));
		server.play();

		try {
			VerisignMDNSAllProfileResourceRecordSetApi vrsnAllProfileResourceRecordSetApi = mockAllProfileResourceRecordSetApi(server
					.getPort());

			ResourceRecordSet<?> actualResult = vrsnAllProfileResourceRecordSetApi
					.getByNameTypeAndQualifier(TEST_USER_NAME, VALID_RR_TYPE1,
							RESOURCE_RECORD_ID);
			assertNotNull(actualResult);

			String expectedRequest = format(rrListCNAMETypesTemplete,
					TEST_USER_NAME, TEST_PASSWORD, RESOURCE_RECORD_ID);
			// System.out.println("Actual Request :\n" + new
			// String(server.takeRequest().getBody()) +"\n\nExpected Request:\n"
			// + expectedRequest);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void iterateByNameAndType() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListValildResponse));
		server.play();

		try {
			VerisignMDNSAllProfileResourceRecordSetApi vrsnAllProfileResourceRecordSetApi = mockAllProfileResourceRecordSetApi(server
					.getPort());

			Iterator<ResourceRecordSet<?>> actulResult = vrsnAllProfileResourceRecordSetApi
					.iterateByNameAndType(VALID_OWNER1, VALID_RR_TYPE1);
			assertNotNull(actulResult);

			ResourceRecordSet<?> rrSet = actulResult.next();
			assertNotNull(rrSet);
			assertEquals(rrSet.ttl(), new Integer(Integer.parseInt(VALID_TTL1)));
			assertEquals(rrSet.type(), VALID_RR_TYPE1);
			assertEquals(rrSet.name(), VALID_OWNER1);
			Object entry = rrSet.records().get(0);

			assertTrue(entry instanceof CNAMEData);
			CNAMEData cnameData = (CNAMEData) entry;
			assertEquals(cnameData.values().iterator().next(), VALID_RDATA1);

			// verify we have 2 records as expected.
			assertTrue(actulResult.hasNext());
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void iterator() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListValildResponse));
		server.play();

		try {
			VerisignMDNSAllProfileResourceRecordSetApi vrsnAllProfileResourceRecordSetApi = mockAllProfileResourceRecordSetApi(server
					.getPort());

			Iterator<ResourceRecordSet<?>> actulResult = vrsnAllProfileResourceRecordSetApi
					.iterator();
			assertNotNull(actulResult);

			ResourceRecordSet<?> rrSet = actulResult.next();
			assertNotNull(rrSet);
			assertEquals(rrSet.ttl(), new Integer(Integer.parseInt(VALID_TTL1)));
			assertEquals(rrSet.type(), VALID_RR_TYPE1);
			assertEquals(rrSet.name(), VALID_OWNER1);
			Object entry = rrSet.records().get(0);

			assertTrue(entry instanceof CNAMEData);
			CNAMEData cnameData = (CNAMEData) entry;
			assertEquals(cnameData.values().iterator().next(), VALID_RDATA1);

			// verify we have 2 records as expected.
			assertTrue(actulResult.hasNext());
		} finally {
			server.shutdown();
		}
	}
}
