package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.io.IOException;

import denominator.Credentials;
import denominator.CredentialsConfiguration;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.assertj.RecordedRequestAssert;

import static denominator.Credentials.ListCredentials;
import static denominator.assertj.MockWebServerAssertions.assertThat;

final class MockUltraDNSServer extends UltraDNSProvider {

  private final MockWebServer delegate = new MockWebServer();
  private String username = "joe";
  private String password = "letmein";

  MockUltraDNSServer() throws IOException {
    delegate.play();
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
    delegate.shutdown();
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      UltraDNSProvider.Module.class)
  static final class Module {

  }
}
