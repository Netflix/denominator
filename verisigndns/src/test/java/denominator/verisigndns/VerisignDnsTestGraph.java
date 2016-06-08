package denominator.verisigndns;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;
import denominator.DNSApiManagerFactory;

public class VerisignDnsTestGraph extends denominator.TestGraph {

  private static final String url = emptyToNull(getProperty("verisigndns.url"));
  private static final String zone = emptyToNull(getProperty("verisigndns.zone"));

  public VerisignDnsTestGraph() {
    super(DNSApiManagerFactory.create(new VerisignDnsProvider(url)), zone);
  }
}
