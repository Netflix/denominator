package denominator.discoverydns;

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

final class MockDiscoveryDNSServer extends DiscoveryDNSProvider {

  private final MockWebServer delegate = new MockWebServer();
  
  // TODO: actually make this use real pems!
  private String certificatePem = "certificatePem";
  private String keyPem = "keyPem";

  MockDiscoveryDNSServer() throws IOException {
    delegate.play();
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
    delegate.shutdown();
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      DiscoveryDNSProvider.Module.class)
  static final class Module {

  }
}
