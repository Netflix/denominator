package denominator.clouddns;

import com.google.gson.TypeAdapter;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import denominator.clouddns.RackspaceApis.CloudIdentity;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import feign.Feign;
import feign.Target.EmptyTarget;
import feign.gson.GsonDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class KeystoneAccessAdapterTest {

  @Rule
  public final MockWebServer server = new MockWebServer();

  CloudIdentity client = Feign.builder()
      .decoder(
          new GsonDecoder(Arrays.<TypeAdapter<?>>asList(new KeystoneAccessAdapter("rax:dns"))))
      .target(EmptyTarget.create(CloudIdentity.class, "cloudidentity"));

  @Test
  public void publicURLFound() throws Exception {
    server.enqueue(new MockResponse().setBody(ACCESS_HEADER
                                              + "            \"name\": \"cloudDNS\",\n"
                                              + "            \"endpoints\": [{\n"
                                              + "                \"tenantId\": \"1234\",\n"
                                              + "                \"publicURL\": \"https:\\/\\/dns.api.rackspacecloud.com\\/v1.0\\/1234\"\n"
                                              + "            }],\n"
                                              + "            \"type\": \"rax:dns\"\n"
                                              + SERVICE + ACCESS_FOOTER));

    TokenIdAndPublicURL result = client.passwordAuth(server.getUrl("/").toURI(), "u", "p");

    assertThat(result.tokenId).isEqualTo("1bcd122d87494f5ab39a185b9ec5ff73");
    assertThat(result.publicURL)
        .isEqualTo("https://dns.api.rackspacecloud.com/v1.0/1234");
  }

  @Test
  public void noEndpoints() throws Exception {
    server.enqueue(new MockResponse().setBody(ACCESS_HEADER
                                              + "            \"name\": \"cloudDNS\",\n"
                                              + "            \"type\": \"rax:dns\"\n"
                                              + SERVICE + ACCESS_FOOTER));

    TokenIdAndPublicURL result = client.passwordAuth(server.getUrl("/").toURI(), "u", "p");

    assertThat(result.tokenId).isEqualTo("1bcd122d87494f5ab39a185b9ec5ff73");
    assertNull(result.publicURL);
  }

  @Test
  public void serviceNotFound() throws Exception {
    server.enqueue(new MockResponse().setBody(ACCESS_HEADER + SERVICE + ACCESS_FOOTER));

    TokenIdAndPublicURL result = client.passwordAuth(server.getUrl("/").toURI(), "u", "p");

    assertThat(result.tokenId).isEqualTo("1bcd122d87494f5ab39a185b9ec5ff73");
    assertNull(result.publicURL);
  }

  @Test
  public void noServices() throws Exception {
    server.enqueue(new MockResponse().setBody(ACCESS_HEADER + SERVICE + ACCESS_FOOTER));

    TokenIdAndPublicURL result = client.passwordAuth(server.getUrl("/").toURI(), "u", "p");

    assertThat(result.tokenId).isEqualTo("1bcd122d87494f5ab39a185b9ec5ff73");
    assertNull(result.publicURL);
  }

  @Test
  public void noToken() throws Exception {
    server.enqueue(new MockResponse().setBody("{\n"
                                              + "    \"access\": {\n"
                                              + "        \"serviceCatalog\": [{\n"
                                              + ACCESS_FOOTER));

    TokenIdAndPublicURL result = client.passwordAuth(server.getUrl("/").toURI(), "u", "p");

    assertNull(result);
  }

  static final String TOKEN = "        \"token\": {\n"
                              + "            \"id\": \"1bcd122d87494f5ab39a185b9ec5ff73\",\n"
                              + "            \"expires\": \"2013-07-01T10:13:55.109-05:00\",\n"
                              + "            \"tenant\": {\n"
                              + "                \"id\": \"1234\",\n"
                              + "                \"name\": \"1234\"\n"
                              + "            }\n"
                              + "        },\n";
  static final String ACCESS_HEADER = "{\n"
                                      + "    \"access\": {\n"
                                      + TOKEN
                                      + "        \"serviceCatalog\": [{\n";
  static final String SERVICE = "        }, {\n"
                                + "            \"name\": \"cloudMonitoring\",\n"
                                + "            \"endpoints\": [{\n"
                                + "                \"tenantId\": \"1234\",\n"
                                + "                \"publicURL\": \"https:\\/\\/monitoring.api.rackspacecloud.com\\/v1.0\\/1234\"\n"
                                + "            }],\n"
                                + "            \"type\": \"rax:monitor\"\n";
  static final String ACCESS_FOOTER = "        }]\n"
                                      + "    }\n"
                                      + "}";
}
