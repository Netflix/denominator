package denominator.route53;

import javax.inject.Inject;

import denominator.CheckConnection;

class HostedZonesReadable implements CheckConnection {

  private final Route53 api;

  @Inject
  HostedZonesReadable(Route53 api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      api.listHostedZones();
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
