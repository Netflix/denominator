package denominator;

import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

import static java.util.Arrays.asList;

public class Live extends Suite {

  /**
   * Add this annotation to specify a {@link TestGraph} that isn't mock.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Target(ElementType.TYPE)
  public @interface UseTestGraph {

    Class<? extends TestGraph> value();
  }

  protected final TestGraph graph;
  private final List<Runner> runners;

  public Live(Class<?> klass) throws InitializationError {
    super(klass, Collections.<Runner>emptyList());
    graph = testGraph(klass);
    if (graph.manager() != null && graph.manager().checkConnection()) {
      runners = createRunners(klass);
    } else {
      runners = Collections.emptyList();
    }
  }

  protected List<Runner> createRunners(Class<?> klass) throws InitializationError {
    List<Runner> result = new ArrayList<Runner>();
    TestWithParameters test = new TestWithParameters(graph.manager().toString(), getTestClass(),
                                                     Arrays.<Object>asList(graph.manager()));
    result.add(new BlockJUnit4ClassRunnerWithParameters(test));
    return Collections.unmodifiableList(result);
  }

  private static TestGraph testGraph(Class<?> klass) throws InitializationError {
    try {
      return klass.isAnnotationPresent(UseTestGraph.class) ?
             klass.getAnnotation(UseTestGraph.class).value().newInstance() : new TestGraph();
    } catch (Exception e) {
      throw new InitializationError(e);
    }
  }

  @Override
  protected List<Runner> getChildren() {
    return runners;
  }

  public static final class Write extends Live {

    public Write(Class<?> klass) throws InitializationError {
      super(klass);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Target(ElementType.TYPE)
    public @interface Profile {

      String value() default "basic";
    }

    @Override
    protected List<Runner> createRunners(Class<?> klass) throws InitializationError {
      // Special case the only test for zone mutation
      if (ZoneWriteCommandsLiveTest.class.isAssignableFrom(klass)) {
        String zoneToCreate = graph.deleteTestZone();
        TestWithParameters test = new TestWithParameters("[" + zoneToCreate + "]", getTestClass(),
                                                         asList(graph.manager(), zoneToCreate));
        return Collections.<Runner>singletonList(new BlockJUnit4ClassRunnerWithParameters(test));
      }
      String profile = klass.isAnnotationPresent(Profile.class) ?
                       klass.getAnnotation(Profile.class).value() : "basic";
      List<Runner> result = new ArrayList<Runner>();

      List<ResourceRecordSet<?>>
          rrsets =
          profile.equals("basic") ? graph.basicRecordSets(klass)
                                  : graph.recordSetsForProfile(klass, profile);

      Zone zone = graph.createZoneIfAbsent();
      for (ResourceRecordSet<?> rrs : rrsets) {
        TestWithParameters test = new TestWithParameters("[" + rrs + "]", getTestClass(),
                                                         asList(graph.manager(), zone, rrs));
        result.add(new BlockJUnit4ClassRunnerWithParameters(test));
      }
      return Collections.unmodifiableList(result);
    }
  }
}


