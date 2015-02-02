package denominator.ultradns;

import javax.inject.Inject;

import denominator.CheckConnection;
import denominator.ultradns.UltraDNS.NetworkStatus;

class NetworkStatusReadable implements CheckConnection {

  private final UltraDNS api;

  @Inject
  NetworkStatusReadable(UltraDNS api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      return NetworkStatus.GOOD == api.getNeustarNetworkStatus();
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "NetworkStatusReadable";
  }
}
