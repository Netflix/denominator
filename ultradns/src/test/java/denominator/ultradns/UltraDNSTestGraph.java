package denominator.ultradns;

import denominator.DNSApiManagerFactory;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

public class UltraDNSTestGraph extends denominator.TestGraph {

  private static final String url = emptyToNull(getProperty("ultradns.url"));
  private static final String zone = emptyToNull(getProperty("ultradns.zone"));

  public UltraDNSTestGraph() {
    super(DNSApiManagerFactory.create(new UltraDNSProvider(url)), zone);
  }
}
