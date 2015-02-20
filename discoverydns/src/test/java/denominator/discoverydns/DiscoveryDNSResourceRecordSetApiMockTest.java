package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class DiscoveryDNSResourceRecordSetApiMockTest {

  MockDiscoveryDNSServer server;

  String
      records =
      "{ \"zone\": { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ { \"name\": \"www.denominator.io.\", \"class\": \"IN\", \"ttl\": \"60\", \"type\": \"A\", \"rdata\": \"127.0.0.1\" } ] } }";

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(records));

    Iterator<ResourceRecordSet<?>>
        records =
        server.connect().api().basicRecordSetsInZone("123-123-123-123-123").iterator();

    assertThat(records.next())
        .hasName("www.denominator.io.")
        .hasType("A")
        .hasTtl(60)
        .containsOnlyRecords(AData.create("127.0.0.1"));
    assertFalse(records.hasNext());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/zones/123-123-123-123-123?rdataFormat=raw");
  }

  @Test
  public void listWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "{ \"zone\": { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ ] } }"));

    Iterator<ResourceRecordSet<?>>
        records =
        server.connect().api().basicRecordSetsInZone("123-123-123-123-123").iterator();

    assertFalse(records.hasNext());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/zones/123-123-123-123-123?rdataFormat=raw");
  }

  @Test
  public void putCreatesRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(records));
    server.enqueue(new MockResponse()); // TODO: realistic response!

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("123-123-123-123-123");
    api.put(a("www.denominator.io.", 60, "127.0.0.1"));

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/zones/123-123-123-123-123?rdataFormat=raw");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/zones/123-123-123-123-123/resourcerecords?rdataFormat=raw")
        .hasBody(
            "{\n"
            + "  \"zoneUpdateResourceRecords\": {\n"
            + "    \"id\": \"123-123-123-123-123\",\n"
            + "    \"version\": 10,\n"
            + "    \"resourceRecords\": [\n"
            + "      {\n"
            + "        \"name\": \"www.denominator.io.\",\n"
            + "        \"class\": \"IN\",\n"
            + "        \"ttl\": \"60\",\n"
            + "        \"type\": \"A\",\n"
            + "        \"rdata\": \"127.0.0.1\"\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "}");
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDiscoveryDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
