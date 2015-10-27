package denominator.verisigndns;

import javax.inject.Inject;

import denominator.CheckConnection;
import denominator.verisigndns.VerisignDnsEncoder.Paging;

public class HostedZonesReadable implements CheckConnection {

  private final VerisignDns api;

  @Inject
  HostedZonesReadable(VerisignDns api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      api.getZones(new Paging(1, 1));
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "HostedZonesReadable";
  }
}
