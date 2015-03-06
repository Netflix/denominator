package denominator.designate;

import denominator.DNSApiManagerFactory;
import denominator.TestGraph;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

public class DesignateTestGraph extends TestGraph {

  private static final String url = emptyToNull(getProperty("designate.url"));
  private static final String zone = emptyToNull(getProperty("designate.zone"));

  public DesignateTestGraph() {
    super(DNSApiManagerFactory.create(new DesignateProvider(url)), zone);
  }
}
