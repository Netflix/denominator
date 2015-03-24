package denominator.route53;

import java.util.Map.Entry;

import javax.inject.Inject;

import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class Route53Target implements Target<Route53> {

  private final Provider provider;
  private final InvalidatableAuthenticationHeadersProvider lazyAuthHeaders;

  @Inject
  Route53Target(Provider provider, InvalidatableAuthenticationHeadersProvider lazyAuthHeaders) {
    this.provider = provider;
    this.lazyAuthHeaders = lazyAuthHeaders;
  }

  @Override
  public Class<Route53> type() {
    return Route53.class;
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
    if (input.url().indexOf("http") != 0) {
      input.insert(0, url());
    }
    for (Entry<String, String> entry : lazyAuthHeaders.get().entrySet()) {
      input.header(entry.getKey(), entry.getValue());
    }
    return input.request();
  }
}
