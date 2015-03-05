package denominator.designate;

import javax.inject.Inject;

import denominator.Provider;
import denominator.designate.KeystoneV2.TokenIdAndPublicURL;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class DesignateTarget implements Target<Designate> {

  private final Provider provider;
  private final InvalidatableAuthProvider lazyUrlAndToken;

  @Inject
  DesignateTarget(Provider provider, InvalidatableAuthProvider lazyUrlAndToken) {
    this.provider = provider;
    this.lazyUrlAndToken = lazyUrlAndToken;
  }

  @Override
  public Class<Designate> type() {
    return Designate.class;
  }

  @Override
  public String name() {
    return provider.name();
  }

  @Override
  public String url() {
    return lazyUrlAndToken.get().publicURL;
  }

  @Override
  public Request apply(RequestTemplate input) {
    TokenIdAndPublicURL urlAndToken = lazyUrlAndToken.get();
    if (input.url().indexOf("http") != 0) {
      input.insert(0, urlAndToken.publicURL);
    }
    input.header("X-Auth-Token", urlAndToken.tokenId);
    return input.request();
  }
}
