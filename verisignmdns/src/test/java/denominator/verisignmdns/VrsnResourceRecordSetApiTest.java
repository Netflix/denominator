/**
 * 
 */
package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.TEST_PASSWORD;
import static denominator.verisignmdns.VrsnMDNSTest.TEST_USER_NAME;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_OWNER1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RDATA1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RR_TYPE1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_TTL1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_ZONE_NAME1;
import static denominator.verisignmdns.VrsnMDNSTest.createRequestARecordResponse;
import static denominator.verisignmdns.VrsnMDNSTest.createRequestARecordTemplete;
import static denominator.verisignmdns.VrsnMDNSTest.mockResourceRecordSetApi;
import static denominator.verisignmdns.VrsnMDNSTest.rrByNameAndTypeTemplate;
import static denominator.verisignmdns.VrsnMDNSTest.rrDeleteResponse;
import static denominator.verisignmdns.VrsnMDNSTest.rrDeleteTemplete;
import static denominator.verisignmdns.VrsnMDNSTest.rrListCNAMETypesResponse;
import static denominator.verisignmdns.VrsnMDNSTest.rrListInvalidZoneResponse;
import static denominator.verisignmdns.VrsnMDNSTest.rrListRequestTemplate;
import static denominator.verisignmdns.VrsnMDNSTest.rrListValidResponseNoRecords;
import static denominator.verisignmdns.VrsnMDNSTest.rrListValildResponse;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.CNAMEData;
import denominator.verisignmdns.VrsnContentConversionHelper;
import denominator.verisignmdns.VrsnMdns.Record;

/**
 * @author smahurpawar
 *
 */
public class VrsnResourceRecordSetApiTest {

	@Test
	public void rrListInvalidZone() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListInvalidZoneResponse));
		server.play();

		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			assertFalse(api.iterator().hasNext());

			assertEquals(server.getRequestCount(), 1);
			String expectedRequest = format(rrListRequestTemplate,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void rrListvalidZoneNoRRs() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListValidResponseNoRecords));
		server.play();

		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			assertFalse(api.iterator().hasNext());

			assertEquals(server.getRequestCount(), 1);
			String expectedRequest = format(rrListRequestTemplate,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void rrListvalidResponse() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListValildResponse));
		server.play();

		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			Iterator<ResourceRecordSet<?>> iter = api.iterator();
			ResourceRecordSet<?> rrSet = iter.next();
			assertNotNull(rrSet);
			assertEquals(rrSet.ttl(), new Integer(Integer.parseInt(VALID_TTL1)));
			assertEquals(rrSet.type(), VALID_RR_TYPE1);
			assertEquals(rrSet.name(), VALID_OWNER1);
			Object entry = rrSet.records().get(0);

			assertTrue(entry instanceof CNAMEData);
			CNAMEData cnameData = (CNAMEData) entry;
			assertEquals(cnameData.values().iterator().next(), VALID_RDATA1);

			// verify we have 2 records as expected.
			assertTrue(iter.hasNext());

			String expectedRequest = format(rrListRequestTemplate,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void rrListByNameAndTypeValidResponse() throws IOException,
			InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListValildResponse));
		server.play();

		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			ResourceRecordSet<?> rrSet = api.getByNameAndType(VALID_OWNER1,
					VALID_RR_TYPE1);
			assertNotNull(rrSet);
			assertEquals(rrSet.ttl(), new Integer(Integer.parseInt(VALID_TTL1)));
			assertEquals(rrSet.type(), VALID_RR_TYPE1);
			assertEquals(rrSet.name(), VALID_OWNER1);
			Object entry = rrSet.records().get(0);

			assertTrue(entry instanceof CNAMEData);
			CNAMEData cnameData = (CNAMEData) entry;
			assertEquals(cnameData.values().iterator().next(), VALID_RDATA1);

			String expectedRequest = format(rrByNameAndTypeTemplate,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1,
					VALID_RR_TYPE1, VALID_OWNER1);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}

	@Test
	public void testGetByNameAndType() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListValildResponse));
		server.play();
		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			ResourceRecordSet<?> actualResult = api.getByNameAndType(
					VALID_OWNER1, VALID_RR_TYPE1);

			assertNotNull(actualResult);
			assertEquals(actualResult.ttl(),
					new Integer(Integer.parseInt(VALID_TTL1)));
			assertEquals(actualResult.type(), VALID_RR_TYPE1);
			assertEquals(actualResult.name(), VALID_OWNER1);
			Object entry = actualResult.records().get(0);

			assertTrue(entry instanceof CNAMEData);
			CNAMEData cnameData = (CNAMEData) entry;
			assertEquals(cnameData.values().iterator().next(), VALID_RDATA1);
			String expectedRequest = format(rrByNameAndTypeTemplate,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1,
					VALID_RR_TYPE1, VALID_OWNER1);

			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}
	 	
	@Test
	public void TestDeleteByNameAndType() throws IOException,
			InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(rrListCNAMETypesResponse));
		server.enqueue(new MockResponse().setBody(rrDeleteResponse));
		server.play();

		try {
			ResourceRecordSetApi apiforDelete = mockResourceRecordSetApi(server
					.getPort());
			apiforDelete.deleteByNameAndType(VALID_OWNER1, VALID_RR_TYPE1);
			String expectedRequest1 = format(rrByNameAndTypeTemplate,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1,
					VALID_RR_TYPE1, VALID_OWNER1);
			String expectedRequest2 = format(rrDeleteTemplete, TEST_USER_NAME,
					TEST_PASSWORD, VALID_ZONE_NAME1, VALID_RR_TYPE1,
					VALID_OWNER1);

			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest1);
			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest2);

		} finally {
			server.shutdown();
		}
	}
	
	@Test
	public void testPut() throws IOException, InterruptedException {
		MockWebServer server = new MockWebServer();
		server.enqueue(new MockResponse().setBody(createRequestARecordResponse));
		server.play();

		try {
			ResourceRecordSetApi api = mockResourceRecordSetApi(server
					.getPort());
			Record aMDNSRecord = VrsnMDNSTest.mockCNameRecord();
			ResourceRecordSet<?> inputRecordSet = VrsnContentConversionHelper
					.convertMDNSRecordToDenominator(aMDNSRecord);
			api.put(inputRecordSet);

			String expectedRequest = format(createRequestARecordTemplete,
					TEST_USER_NAME, TEST_PASSWORD, VALID_ZONE_NAME1,
					VALID_RR_TYPE1, VALID_OWNER1);

			assertEquals(new String(server.takeRequest().getBody()),
					expectedRequest);
		} finally {
			server.shutdown();
		}
	}
}
