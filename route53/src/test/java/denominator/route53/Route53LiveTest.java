package denominator.route53;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import denominator.DNSApiManagerFactory;
import denominator.Live;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    Route53LiveTest.CheckConnectionLiveTest.class,
    Route53LiveTest.ReadOnlyLiveTest.class,
    Route53LiveTest.WriteCommandsLiveTest.class,
    Route53LiveTest.RoundRobinWriteCommandsLiveTest.class,
    Route53LiveTest.WeightedReadOnlyLiveTest.class,
    Route53LiveTest.WeightedWriteCommandsLiveTest.class
})
public class Route53LiveTest {

  private static final String url = emptyToNull(getProperty("route53.url"));
  private static final String zone = emptyToNull(getProperty("route53.zone"));

  public static class TestGraph extends denominator.TestGraph {

    public TestGraph() {
      super(DNSApiManagerFactory.create(new Route53Provider(url)), zone);
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
  public static class WeightedReadOnlyLiveTest
      extends denominator.profile.WeightedReadOnlyLiveTest {

  }

  @Live.UseTestGraph(TestGraph.class)
  public static class WeightedWriteCommandsLiveTest
      extends denominator.profile.WeightedWriteCommandsLiveTest {

  }
}

