package denominator.discoverydns;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import denominator.DNSApiManager;
import denominator.DNSApiManagerFactory.HttpLog;
import denominator.Denominator;
import denominator.Live;

import static denominator.CredentialsConfiguration.credentials;
import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    DiscoveryDNSLiveTest.CheckConnectionLiveTest.class,
    DiscoveryDNSLiveTest.ReadOnlyLiveTest.class,
    DiscoveryDNSLiveTest.WriteCommandsLiveTest.class
})
public class DiscoveryDNSLiveTest {

  private static final String url = emptyToNull(getProperty("discoverydns.url"));
  private static final String zone = emptyToNull(getProperty("discoverydns.zone"));

  public static class TestGraph extends denominator.TestGraph {

    /** Lazy initializing manager */
    public TestGraph() {
      super(null, zone);
    }

    /**
     * Since discoverydns uses client auth, it cannot be eagerly initialized. This lazy initializes
     * it, if the required credentials are present.
     */
    @Override
    protected DNSApiManager manager() {
      String x509CertificatePem = emptyToNull(getProperty("discoverydns.x509CertificatePem"));
      String privateKeyPem = emptyToNull(getProperty("discoverydns.privateKeyPem"));
      if (x509CertificatePem != null && privateKeyPem != null) {
        // Use bouncycastle to read pems
        X509Certificate certificate = null; // TODO: from x509CertificatePem
        PrivateKey privateKey = null; // TODO: from privateKeyPem
        Object credentials = credentials(certificate, privateKey);
        return Denominator.create(new DiscoveryDNSProvider(url), credentials, new HttpLog());
      }
      return null;
    }
  }

  @Live.UseTestGraph(TestGraph.class)
  public static class CheckConnectionLiveTest extends denominator.CheckConnectionLiveTest {

  }

  @Live.UseTestGraph(TestGraph.class)
  public static class ReadOnlyLiveTest extends denominator.ReadOnlyLiveTest {

  }

  @Live.UseTestGraph(TestGraph.class)
  public static class WriteCommandsLiveTest extends denominator.WriteCommandsLiveTest {

  }
}

