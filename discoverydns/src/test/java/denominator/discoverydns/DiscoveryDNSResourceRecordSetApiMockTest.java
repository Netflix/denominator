package denominator.discoverydns;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Iterables;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import denominator.DNSApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class DiscoveryDNSResourceRecordSetApiMockTest {

  @Test
  public void records() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.zones));
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.records));
    server.play();

    try {
      DNSApi api = DiscoveryDNSTest.mockApi(server.getPort()).api();
      Iterator<ResourceRecordSet<?>>
          records =
          api.basicRecordSetsInZone("denominator.io").iterator();
      assertEquals(server.getRequestCount(), 2);
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /zones?searchName=denominator.io HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /zones/123-123-123-123-123?rdataFormat=raw HTTP/1.1");

      assertTrue(records.hasNext());
      ResourceRecordSet<?> record = records.next();
      assertEquals("www.denominator.io.", record.name());
      assertEquals("A", record.type());
      assertEquals(Integer.valueOf(60), record.ttl());
      assertNull(record.qualifier());
      assertNull(record.geo());
      assertEquals(1, record.records().size());
      assertTrue(record.records().get(0) instanceof AData);
      AData aData = (AData) record.records().get(0);
      assertEquals("127.0.0.1", aData.get("address"));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void noRecords() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.zones));
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.noRecords));
    server.play();

    try {
      DNSApi api = DiscoveryDNSTest.mockApi(server.getPort()).api();
      assertEquals(0, Iterables.size(api.basicRecordSetsInZone("denominator.io")));

      assertEquals(server.getRequestCount(), 2);
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /zones?searchName=denominator.io HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /zones/123-123-123-123-123?rdataFormat=raw HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void updateRecords() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.zones));
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.records));
    server.enqueue(new MockResponse());
    server.play();

    try {
      DNSApi api = DiscoveryDNSTest.mockApi(server.getPort()).api();
      ResourceRecordSet<?>
          rrset =
          ResourceRecordSet.builder().name("www.denominator.io.").type("A").ttl(60)
              .add(Util.toMap("A", "127.0.0.1")).build();
      api.basicRecordSetsInZone("denominator.io").put(rrset);

      assertEquals(server.getRequestCount(), 3);
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /zones?searchName=denominator.io HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /zones/123-123-123-123-123?rdataFormat=raw HTTP/1.1");
      RecordedRequest request = server.takeRequest();
      assertEquals(request.getRequestLine(),
                   "PUT /zones/123-123-123-123-123/resourcerecords?rdataFormat=raw HTTP/1.1");
      assertEquals(DiscoveryDNSTest.updateRecords,
                   CharMatcher.WHITESPACE.trimAndCollapseFrom(new String(request.getBody()), ' '));
    } finally {
      server.shutdown();
    }
  }
}
