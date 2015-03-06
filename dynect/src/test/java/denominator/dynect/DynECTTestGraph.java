package denominator.dynect;

import denominator.DNSApiManagerFactory;

import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

public class DynECTTestGraph extends denominator.TestGraph {

  private static final String url = emptyToNull(getProperty("dynect.url"));
  private static final String zone = emptyToNull(getProperty("dynect.zone"));

  public DynECTTestGraph() {
    super(DNSApiManagerFactory.create(new DynECTProvider(url)), zone);
  }
}
