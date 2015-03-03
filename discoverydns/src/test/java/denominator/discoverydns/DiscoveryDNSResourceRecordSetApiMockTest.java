package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;

public class DiscoveryDNSResourceRecordSetApiMockTest {

  @Rule
  public MockDiscoveryDNSServer server = new MockDiscoveryDNSServer();

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
        .containsExactlyRecords(AData.create("127.0.0.1"));
    assertThat(records).isEmpty();

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/zones/123-123-123-123-123?rdataFormat=raw");
  }

  @Test
  public void listWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "{ \"zone\": { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ ] } }"));

    assertThat(server.connect().api().basicRecordSetsInZone("123-123-123-123-123"))
        .isEmpty();

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

  String
      records =
      "{ \"zone\": { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ { \"name\": \"www.denominator.io.\", \"class\": \"IN\", \"ttl\": \"60\", \"type\": \"A\", \"rdata\": \"127.0.0.1\" } ] } }";
}
