package denominator.ultradns;

import javax.inject.Inject;

import denominator.CheckConnection;

class NetworkStatusReadable implements CheckConnection {

  private final UltraDNS api;

  @Inject
  NetworkStatusReadable(UltraDNS api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      return "GOOD".equals(api.getNeustarNetworkStatus().getMessage().toUpperCase());
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "NetworkStatusReadable";
  }
}
