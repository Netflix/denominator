package denominator.discoverydns;

import javax.inject.Inject;

import denominator.Denominator.Version;
import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

final class DiscoveryDNSTarget implements Target<DiscoveryDNS> {

  private static final String CLIENT_ID = "Denominator " + Version.INSTANCE;
  private final Provider provider;

  @Inject
  DiscoveryDNSTarget(Provider provider) {
    this.provider = provider;
  }

  @Override
  public Class<DiscoveryDNS> type() {
    return DiscoveryDNS.class;
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
  public Request apply(RequestTemplate in) {
    in.insert(0, url());
    in.header("X-Requested-By", CLIENT_ID);
    return in.request();
  }
}
