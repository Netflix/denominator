package denominator.dynect;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import denominator.DNSApiManagerFactory;
import denominator.Live;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    DynECTLiveTest.CheckConnectionLiveTest.class,
    DynECTLiveTest.ReadOnlyLiveTest.class,
    DynECTLiveTest.WriteCommandsLiveTest.class,
    DynECTLiveTest.RoundRobinWriteCommandsLiveTest.class,
    DynECTLiveTest.GeoReadOnlyLiveTest.class,
})
public class DynECTLiveTest {

  private static final String url = emptyToNull(getProperty("dynect.url"));
  private static final String zone = emptyToNull(getProperty("dynect.zone"));

  public static class TestGraph extends denominator.TestGraph {

    public TestGraph() {
      super(DNSApiManagerFactory.create(new DynECTProvider(url)), zone);
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

  @Live.UseTestGraph(TestGraph.class)
  public static class RoundRobinWriteCommandsLiveTest
      extends denominator.RoundRobinWriteCommandsLiveTest {

  }

  @Live.UseTestGraph(TestGraph.class)
  public static class GeoReadOnlyLiveTest extends denominator.profile.GeoReadOnlyLiveTest {

  }
}

