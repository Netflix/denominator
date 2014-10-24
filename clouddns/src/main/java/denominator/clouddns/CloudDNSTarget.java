package denominator.clouddns;

import javax.inject.Inject;

import denominator.Provider;
import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class CloudDNSTarget implements Target<CloudDNS> {

    private final Provider provider;
    private final javax.inject.Provider<TokenIdAndPublicURL> lazyUrlAndToken;

    @Inject
    CloudDNSTarget(Provider provider, javax.inject.Provider<TokenIdAndPublicURL> lazyUrlAndToken) {
        this.provider = provider;
        this.lazyUrlAndToken = lazyUrlAndToken;
    }

    @Override
    public Class<CloudDNS> type() {
        return CloudDNS.class;
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
        if (input.url().indexOf("http") != 0)
            input.insert(0, urlAndToken.publicURL);
        input.header("X-Auth-Token", urlAndToken.tokenId);
        input.header("Accept", "application/json");
        return input.request();
    }
}
