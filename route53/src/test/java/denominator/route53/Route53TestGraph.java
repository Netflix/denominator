package denominator.route53;

import denominator.DNSApiManagerFactory;
import denominator.TestGraph;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

public class Route53TestGraph extends TestGraph {

  private static final String url = emptyToNull(getProperty("route53.url"));
  private static final String zone = emptyToNull(getProperty("route53.zone"));

  public Route53TestGraph() {
    super(DNSApiManagerFactory.create(new Route53Provider(url)), zone);
  }
}
