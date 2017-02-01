package denominator.ultradns;

import javax.inject.Inject;

import denominator.CheckConnection;

class NetworkConnection implements CheckConnection {

  private final UltraDNSRest api;

  @Inject
  NetworkConnection(UltraDNSRest api) {
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
    return "NetworkConnection";
  }
}
