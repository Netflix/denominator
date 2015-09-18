package denominator.route53;

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

final class MockRoute53Server extends Route53Provider implements TestRule {

  private final MockWebServer delegate = new MockWebServer();
  private String accessKey = "accessKey";
  private String secretKey = "secretKey";
  private String token = null;

  MockRoute53Server() {
    credentials(accessKey, secretKey, token);
  }

  @Override
  public String url() {
    return "http://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return token == null ? ListCredentials.from(accessKey, secretKey)
                         : ListCredentials.from(accessKey, secretKey, token);
  }

  MockRoute53Server credentials(String accessKey, String secretKey, String token) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.token = token;
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

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      Route53Provider.Module.class)
  static final class Module {

  }
}
