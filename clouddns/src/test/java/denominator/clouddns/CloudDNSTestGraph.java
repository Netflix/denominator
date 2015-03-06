package denominator.clouddns;

import denominator.DNSApiManagerFactory;
import denominator.TestGraph;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

public class CloudDNSTestGraph extends TestGraph {

  private static final String url = emptyToNull(getProperty("clouddns.url"));
  private static final String zone = emptyToNull(getProperty("clouddns.zone"));

  public CloudDNSTestGraph() {
    super(DNSApiManagerFactory.create(new CloudDNSProvider(url)), zone);
  }
}
