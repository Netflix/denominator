package denominator.discoverydns;

import com.squareup.okhttp.internal.SslContextBuilder;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import denominator.Credentials;
import denominator.CredentialsConfiguration;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.assertj.RecordedRequestAssert;

import static denominator.Credentials.ListCredentials;
import static denominator.assertj.MockWebServerAssertions.assertThat;

final class MockDiscoveryDNSServer extends DiscoveryDNSProvider implements TestRule {

  private final MockWebServerRule delegate = new MockWebServerRule();
  private final X509Certificate certificate;
  private final PrivateKey privateKey;

  MockDiscoveryDNSServer() {
    try {
      KeyPair keyPair = new SslContextBuilder("localhost").generateKeyPair();
      X509Certificate
          certificate =
          new SslContextBuilder("localhost").selfSignedCertificate(keyPair, "1");
      this.certificate = certificate;
      this.privateKey = keyPair.getPrivate();
      KeyStore serverKeyStore = FeignModule.keyStore(certificate, privateKey);
      delegate.get().useHttps(new FeignModule().sslSocketFactory(serverKeyStore), false);
    } catch (GeneralSecurityException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public String url() {
    return "https://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return ListCredentials.from(certificate, privateKey);
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
