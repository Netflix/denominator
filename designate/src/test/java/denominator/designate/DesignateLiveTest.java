package denominator.designate;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import denominator.DNSApiManagerFactory;
import denominator.Live;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    DesignateLiveTest.CheckConnectionLiveTest.class,
    DesignateLiveTest.ReadOnlyLiveTest.class,
    DesignateLiveTest.WriteCommandsLiveTest.class,
    DesignateLiveTest.RoundRobinWriteCommandsLiveTest.class
})
public class DesignateLiveTest {

  private static final String url = emptyToNull(getProperty("designate.url"));
  private static final String zone = emptyToNull(getProperty("designate.zone"));

  public static class TestGraph extends denominator.TestGraph {

    public TestGraph() {
      super(DNSApiManagerFactory.create(new DesignateProvider(url)), zone);
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
}

