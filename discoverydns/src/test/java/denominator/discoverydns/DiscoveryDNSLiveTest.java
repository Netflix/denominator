package denominator.discoverydns;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import denominator.DNSApiManagerFactory;
import denominator.Live;

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

    public TestGraph() {
      super(DNSApiManagerFactory.create(new DiscoveryDNSProvider(url)), zone);
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

