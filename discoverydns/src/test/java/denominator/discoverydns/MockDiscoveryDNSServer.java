package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
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

final class MockDiscoveryDNSServer extends DiscoveryDNSProvider implements TestRule {

  private final MockWebServerRule delegate = new MockWebServerRule();
  
  // TODO: actually make this use real pems!
  private String certificatePem = "certificatePem";
  private String keyPem = "keyPem";

  MockDiscoveryDNSServer() {
    credentials(certificatePem, keyPem);
  }

  @Override
  public String url() {
    return "http://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return ListCredentials.from(certificatePem, keyPem);
  }

  MockDiscoveryDNSServer credentials(String certificatePem, String keyPem) {
    this.certificatePem = certificatePem;
    this.keyPem = keyPem;
    return this;
  }

  void enqueue(MockResponse mockResponse) {
    delegate.enqueue(mockResponse);
  }

  RecordedRequestAssert assertRequest() throws InterruptedException {
    return assertThat(delegate.takeRequest());
  }

  void shutdown() throws IOException {
    delegate.get().shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      DiscoveryDNSProvider.Module.class)
  static final class Module {

  }
}
