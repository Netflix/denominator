package denominator.ultradns;

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

final class MockUltraDNSRestServer extends UltraDNSRestProvider implements TestRule {

  private final MockWebServer delegate = new MockWebServer();
  private String username = "arghya";
  private String password = "letmein";
  private String sessionResponse;
  private final String accessToken = "007e99c189364";
  private final String refreshToken = "f29f3ca94bcd4fb5ba79";

  MockUltraDNSRestServer() {
    credentials(username, password);
  }

  String token() {
    return accessToken;
  }

  @Override
  public String url() {
    return "http://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return ListCredentials.from(username, password);
  }

  MockUltraDNSRestServer credentials(String username, String password) {
    this.username = username;
    this.password = password;
    this.sessionResponse = "{\n"
                            + "  \"tokenType\": \"Bearer\",\n"
                            + format("  \"refreshToken\": \"%s\",\n", refreshToken)
                            + format("  \"accessToken\": \"%s\",\n", accessToken)
                            + "  \"expiresIn\": \"3600\"\n"
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
            .hasPath("/authorization/token");
  }

  void shutdown() throws IOException {
    delegate.shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      UltraDNSRestProvider.Module.class)
  static final class Module {

  }
}
