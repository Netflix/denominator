package denominator.clouddns;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import denominator.DNSApiManagerFactory;
import denominator.Live;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CloudDNSLiveTest.CheckConnectionLiveTest.class,
    CloudDNSLiveTest.ReadOnlyLiveTest.class,
    CloudDNSLiveTest.WriteCommandsLiveTest.class
})
public class CloudDNSLiveTest {

  private static final String url = emptyToNull(getProperty("clouddns.url"));
  private static final String zone = emptyToNull(getProperty("clouddns.zone"));

  public static class TestGraph extends denominator.TestGraph {

    public TestGraph() {
      super(DNSApiManagerFactory.create(new CloudDNSProvider(url)), zone);
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

