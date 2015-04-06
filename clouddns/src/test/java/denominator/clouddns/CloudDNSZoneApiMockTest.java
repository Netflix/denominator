package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.clouddns.RackspaceApisTest.domainsResponse;
import static denominator.clouddns.RackspaceApisTest.soaResponse;

public class CloudDNSZoneApiMockTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(new MockResponse().setBody(soaResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("1234", "denominator.io", 3601, "nil@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
    server.assertRequest()
        .hasPath("/v1.0/123123/domains/1234/records?name=denominator.io&type=SOA");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
  }

  @Test
  public void iteratorByNameWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(new MockResponse().setBody(soaResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io")).containsExactly(
        Zone.create("1234", "denominator.io", 3601, "nil@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
    server.assertRequest()
        .hasPath("/v1.0/123123/domains/1234/records?name=denominator.io&type=SOA");
  }

  @Test
  public void iteratorByNameWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{\"domains\":[],\"totalEntries\":0}"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io")).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
  }

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(creating);
    server.enqueue(created);

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo("1234");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/v1.0/123123/domains")
        .hasBody(
            "{\"domains\":[{\"name\":\"denominator.io\",\"emailAddress\":\"nil@denominator.io\",\"ttl\":3601}]}");
    server.assertRequest()
        .hasPath("/v1.0/123123/status/JOB_ID?showDetails=true");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(creating);
    server.enqueue(domainAlreadyExists);
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(updating);
    server.enqueue(updated);

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo("1234");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/v1.0/123123/domains")
        .hasBody(
            "{\"domains\":[{\"name\":\"denominator.io\",\"emailAddress\":\"nil@denominator.io\",\"ttl\":3601}]}");
    server.assertRequest()
        .hasPath("/v1.0/123123/status/JOB_ID?showDetails=true");
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/v1.0/123123/domains")
        .hasBody(
            "{\"domains\":[{\"id\":\"1234\",\"emailAddress\":\"nil@denominator.io\",\"ttl\":3601}]}");
    server.assertRequest()
        .hasPath("/v1.0/123123/status/JOB_ID?showDetails=true");
  }

  @Test
  public void putWhenPresent_withId() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(updating);
    server.enqueue(updated);

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("1234", "denominator.io", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo("1234");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/v1.0/123123/domains")
        .hasBody(
            "{\"domains\":[{\"id\":\"1234\",\"emailAddress\":\"nil@denominator.io\",\"ttl\":3601}]}");
    server.assertRequest()
        .hasPath("/v1.0/123123/status/JOB_ID?showDetails=true");
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(deleting);
    server.enqueue(deleted);

    ZoneApi api = server.connect().api().zones();
    api.delete("1234");

    server.assertAuthRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/v1.0/123123/domains/1234");
    server.assertRequest().hasPath("/v1.0/123123/status/JOB_ID?showDetails=true");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(deleting);
    server.enqueue(objectNotFound);

    ZoneApi api = server.connect().api().zones();
    api.delete("1234");

    server.assertAuthRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/v1.0/123123/domains/1234");
    server.assertRequest().hasPath("/v1.0/123123/status/JOB_ID?showDetails=true");
  }

  private MockResponse creating = new MockResponse().setBody("{\n"
                                                             + "  \"status\": \"RUNNING\",\n"
                                                             + "  \"request\": \"{\\\"domains\\\":[{\\\"name\\\":\\\"denominator.io\\\",\\\"emailAddress\\\":\\\"test@denominator.io\\\",\\\"ttl\\\":3601}]}\",\n"
                                                             + "  \"verb\": \"POST\",\n"
                                                             + "  \"jobId\": \"JOB_ID\",\n"
                                                             + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                             + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains\"\n"
                                                             + "}");
  private MockResponse created = new MockResponse().setBody("{\n"
                                                            + "  \"status\": \"COMPLETED\",\n"
                                                            + "  \"request\": \"{\\\"domains\\\":[{\\\"name\\\":\\\"denominator.io\\\",\\\"emailAddress\\\":\\\"test@denominator.io\\\",\\\"ttl\\\":3601}]}\",\n"
                                                            + "  \"response\": {\n"
                                                            + "    \"domains\": [\n"
                                                            + "      {\n"
                                                            + "        \"name\": \"denominator.io\",\n"
                                                            + "        \"id\": 1234,\n"
                                                            + "        \"accountId\": 829000,\n"
                                                            + "        \"ttl\": 3601,\n"
                                                            + "        \"nameservers\": [\n"
                                                            + "          {\n"
                                                            + "            \"name\": \"dns1.stabletransit.com\"\n"
                                                            + "          },\n"
                                                            + "          {\n"
                                                            + "            \"name\": \"dns2.stabletransit.com\"\n"
                                                            + "          }\n"
                                                            + "        ],\n"
                                                            + "        \"emailAddress\": \"test@denominator.io\",\n"
                                                            + "        \"updated\": \"2015-04-04T14:58:39.000+0000\",\n"
                                                            + "        \"created\": \"2015-04-04T14:58:39.000+0000\"\n"
                                                            + "      }\n"
                                                            + "    ]\n"
                                                            + "  },\n"
                                                            + "  \"verb\": \"POST\",\n"
                                                            + "  \"jobId\": \"JOB_ID\",\n"
                                                            + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                            + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains\"\n"
                                                            + "}");
  private MockResponse domainAlreadyExists = new MockResponse().setBody("{\n"
                                                                        + "  \"status\": \"ERROR\",\n"
                                                                        + "  \"request\": \"{\\\"domains\\\":[{\\\"name\\\":\\\"denominator.io\\\",\\\"emailAddress\\\":\\\"nil@denominator.io\\\",\\\"ttl\\\":200000}]}\",\n"
                                                                        + "  \"error\": {\n"
                                                                        + "    \"message\": \"Conflict\",\n"
                                                                        + "    \"code\": 409,\n"
                                                                        + "    \"details\": \"Domain already exists\"\n"
                                                                        + "  },\n"
                                                                        + "  \"verb\": \"POST\",\n"
                                                                        + "  \"jobId\": \"JOB_ID\",\n"
                                                                        + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                                        + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains\"\n"
                                                                        + "}");

  private MockResponse updating = new MockResponse().setBody("{\n"
                                                             + "  \"status\": \"RUNNING\",\n"
                                                             + "  \"request\": \"{\\\"domains\\\":[{\\\"id\\\":\\\"4617747\\\",\\\"emailAddress\\\":\\\"test@denominator.io\\\",\\\"ttl\\\":3601}]}\",\n"
                                                             + "  \"verb\": \"PUT\",\n"
                                                             + "  \"jobId\": \"JOB_ID\",\n"
                                                             + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                             + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains\"\n"
                                                             + "}");
  private MockResponse updated = new MockResponse().setBody("{\n"
                                                            + "  \"status\": \"COMPLETED\",\n"
                                                            + "  \"request\": \"{\\\"domains\\\":[{\\\"id\\\":\\\"4617747\\\",\\\"emailAddress\\\":\\\"test@denominator.io\\\",\\\"ttl\\\":3601}]}\",\n"
                                                            + "  \"verb\": \"PUT\",\n"
                                                            + "  \"jobId\": \"JOB_ID\",\n"
                                                            + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                            + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains\"\n"
                                                            + "}");

  private MockResponse deleting = new MockResponse().setBody("{\n"
                                                             + "  \"status\": \"RUNNING\",\n"
                                                             + "  \"verb\": \"DELETE\",\n"
                                                             + "  \"jobId\": \"JOB_ID\",\n"
                                                             + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                             + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains/1234\"\n"
                                                             + "}");
  private MockResponse deleted = new MockResponse().setBody("{\n"
                                                            + "  \"status\": \"COMPLETED\",\n"
                                                            + "  \"verb\": \"DELETE\",\n"
                                                            + "  \"jobId\": \"JOB_ID\",\n"
                                                            + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                            + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains/1234\"\n"
                                                            + "}");
  private MockResponse objectNotFound = new MockResponse().setBody("{\n"
                                                                   + "  \"status\": \"ERROR\",\n"
                                                                   + "  \"error\": {\n"
                                                                   + "    \"failedItems\": {\n"
                                                                   + "      \"faults\": [\n"
                                                                   + "        {\n"
                                                                   + "          \"message\": \"Service Unavailable\",\n"
                                                                   + "          \"code\": 503,\n"
                                                                   + "          \"details\": \"com.rackspace.cloud.dns.exceptions.ObjectNotFoundException:  ; Domain ID: 1234\"\n"
                                                                   + "        }\n"
                                                                   + "      ]\n"
                                                                   + "    },\n"
                                                                   + "    \"message\": \"One or more items could not be deleted.\",\n"
                                                                   + "    \"code\": 500,\n"
                                                                   + "    \"details\": \"See errors list for details.\"\n"
                                                                   + "  },\n"
                                                                   + "  \"verb\": \"DELETE\",\n"
                                                                   + "  \"jobId\": \"JOB_ID\",\n"
                                                                   + "  \"callbackUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/status/JOB_ID\",\n"
                                                                   + "  \"requestUrl\": \"https://dns.api.rackspacecloud.com/v1.0/829000/domains/1234\"\n"
                                                                   + "}");
}
