package denominator.dynect;

import javax.inject.Inject;

import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class DynECTTarget implements Target<DynECT> {

  private final Provider provider;
  private final InvalidatableTokenProvider lazyToken;

  @Inject
  DynECTTarget(Provider provider, InvalidatableTokenProvider lazyToken) {
    this.provider = provider;
    this.lazyToken = lazyToken;
  }

  @Override
  public Class<DynECT> type() {
    return DynECT.class;
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
    input.header("Auth-Token", lazyToken.get());
    input.insert(0, url());
    return input.request();
  }
};
