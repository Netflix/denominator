package denominator.clouddns;

import javax.inject.Inject;

import denominator.CheckConnection;
import denominator.clouddns.RackspaceApis.CloudDNS;

class LimitsReadable implements CheckConnection {

  private final CloudDNS api;

  @Inject
  LimitsReadable(CloudDNS api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      return api.limits() != null;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "LimitsReadable";
  }
}
