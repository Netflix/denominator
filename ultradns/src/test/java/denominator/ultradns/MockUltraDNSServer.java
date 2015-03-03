package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;

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

final class MockUltraDNSServer extends UltraDNSProvider implements TestRule {

  private final MockWebServerRule delegate = new MockWebServerRule();
  private String username = "joe";
  private String password = "letmein";

  MockUltraDNSServer() {
    credentials(username, password);
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

  MockUltraDNSServer credentials(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  void enqueue(MockResponse mockResponse) {
    delegate.enqueue(mockResponse);
  }

  RecordedRequestAssert assertRequestHasBody(String xmlBody) throws InterruptedException {
    return assertThat(delegate.takeRequest())
        .hasMethod("POST")
        .hasPath("/")
        .hasXMLBody(xmlBody);
  }

  void shutdown() throws IOException {
    delegate.get().shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      UltraDNSProvider.Module.class)
  static final class Module {

  }
}
