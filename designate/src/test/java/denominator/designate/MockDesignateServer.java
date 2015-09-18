package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

import denominator.Credentials;
import denominator.CredentialsConfiguration;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.assertj.RecordedRequestAssert;

import static denominator.Credentials.ListCredentials;
import static denominator.assertj.MockWebServerAssertions.assertThat;
import static java.lang.String.format;

final class MockDesignateServer extends DesignateProvider implements TestRule {

  private final MockWebServer delegate = new MockWebServer();
  private final String tokenId = "b84f4a37-5126-4603-9521-ccd0665fbde1";
  private String tenantId = "123123";
  private String username = "joe";
  private String password = "letmein";
  private String accessResponse;

  MockDesignateServer() {
    credentials(tenantId, username, password);
  }

  String tokenId() {
    return tokenId;
  }

  @Override
  public String url() {
    return "http://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return ListCredentials.from(tenantId, username, password);
  }

  MockDesignateServer credentials(String tenantId, String username, String password) {
    this.tenantId = tenantId;
    this.username = username;
    this.password = password;
    this.accessResponse = "{\"access\": {\n"
                          + "  \"token\": {\n"
                          + "    \"expires\": \"2013-07-08T05:55:31.809Z\",\n"
                          + format("    \"id\": \"%s\",\n", tokenId)
                          + "    \"tenant\": {\n"
                          + format("      \"id\": \"%s\",\n", tenantId)
                          + "      \"name\": \"denominator\"\n"
                          + "    }\n"
                          + "  },\n"
                          + "  \"serviceCatalog\": [\n"
                          + "    {\n"
                          + "      \"name\": \"DNS\",\n"
                          + "      \"type\": \"hpext:dns\",\n"
                          + "      \"endpoints\": [{\n"
                          + format("        \"tenantId\": \"%s\",\n", tenantId)
                          + format("        \"publicURL\": \"http://localhost:%s\\/v1\\/\",\n",
                                   delegate.getPort())
                          + "        \"publicURL2\": \"\",\n"
                          + "        \"region\": \"region-a.geo-1\",\n"
                          + "        \"versionId\": \"1\"\n"
                          + "      }]\n"
                          + "    }\n"
                          + "  ]\n"
                          + "}}";
    return this;
  }

  void enqueueAuthResponse() {
    delegate.enqueue(new MockResponse().setBody(accessResponse));
  }

  void enqueue(MockResponse mockResponse) {
    delegate.enqueue(mockResponse);
  }

  RecordedRequestAssert assertRequest() throws InterruptedException {
    return assertThat(delegate.takeRequest());
  }

  RecordedRequestAssert assertAuthRequest() throws InterruptedException {
    return assertThat(delegate.takeRequest())
        .hasMethod("POST")
        .hasPath("/tokens")
        .hasBody(format(
            "{\"auth\":{\"passwordCredentials\":{\"username\":\"%s\",\"password\":\"%s\"},\"tenantId\":\"%s\"}}",
            username, password, tenantId));
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      DesignateProvider.Module.class)
  static final class Module {

  }

  void shutdown() throws IOException {
    delegate.shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }
}
