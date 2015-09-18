package denominator.dynect;

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

final class MockDynECTServer extends DynECTProvider implements TestRule {

  private final MockWebServer delegate = new MockWebServer();
  private final String token = "FFFFFFFFFF";
  private String customer = "jclouds";
  private String username = "joe";
  private String password = "letmein";
  private String sessionResponse;

  MockDynECTServer() {
    credentials(customer, username, password);
  }

  String token() {
    return token;
  }

  @Override
  public String url() {
    return "http://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return ListCredentials.from(customer, username, password);
  }

  MockDynECTServer credentials(String customer, String username, String password) {
    this.customer = customer;
    this.username = username;
    this.password = password;
    this.sessionResponse = "{\n"
                           + "  \"status\": \"success\",\n"
                           + "  \"data\": {\n"
                           + format("    \"token\": \"%s\",\n", token)
                           + "    \"version\": \"3.5.0\"\n"
                           + "  },\n"
                           + "  \"job_id\": 254417252,\n"
                           + "  \"msgs\": [\n"
                           + "    {\n"
                           + "      \"INFO\": \"login: Login successful\",\n"
                           + "      \"SOURCE\": \"BLL\",\n"
                           + "      \"ERR_CD\": null,\n"
                           + "      \"LVL\": \"INFO\"\n"
                           + "    }\n"
                           + "  ]\n"
                           + "}";
    return this;
  }

  void enqueueSessionResponse() {
    delegate.enqueue(new MockResponse().setBody(sessionResponse));
  }

  void enqueue(MockResponse mockResponse) {
    delegate.enqueue(mockResponse);
  }

  RecordedRequestAssert assertRequest() throws InterruptedException {
    return assertThat(delegate.takeRequest());
  }

  RecordedRequestAssert assertSessionRequest() throws InterruptedException {
    return assertThat(delegate.takeRequest())
        .hasMethod("POST")
        .hasPath("/Session")
        .hasBody(format("{\"customer_name\":\"%s\",\"user_name\":\"%s\",\"password\":\"%s\"}",
            customer, username, password));
  }

  void shutdown() throws IOException {
    delegate.shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      DynECTProvider.Module.class)
  static final class Module {

  }
}
