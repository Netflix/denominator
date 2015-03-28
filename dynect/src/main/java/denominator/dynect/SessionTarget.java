package denominator.dynect;

import javax.inject.Inject;

import denominator.Provider;
import denominator.dynect.InvalidatableTokenProvider.Session;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class SessionTarget implements Target<Session> {

  private final Provider provider;

  @Inject
  SessionTarget(Provider provider) {
    this.provider = provider;
  }

  @Override
  public Class<Session> type() {
    return Session.class;
  }

  @Override
  public String name() {
    return provider.name();
  }

  @Override
  public String url() {
    return provider.url();
  }

  @Override
  public Request apply(RequestTemplate input) {
    input.header("API-Version", "3.5.10");
    input.header("Content-Type", "application/json");
    input.insert(0, url());
    return input.request();
  }
};
