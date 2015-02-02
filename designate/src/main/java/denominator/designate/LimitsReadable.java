package denominator.designate;

import javax.inject.Inject;

import denominator.CheckConnection;

class LimitsReadable implements CheckConnection {

  private final Designate api;

  @Inject
  LimitsReadable(Designate api) {
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
