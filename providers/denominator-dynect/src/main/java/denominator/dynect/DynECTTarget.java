package denominator.dynect;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.inject.Named;

import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class DynECTTarget implements Target<DynECT> {

    private final Provider provider;
    private final javax.inject.Provider<String> lazyToken;

    @Inject
    DynECTTarget(Provider provider, @Named("Auth-Token") javax.inject.Provider<String> lazyToken) {
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
        input.header("API-Version", "3.5.0");
        input.header(CONTENT_TYPE, APPLICATION_JSON);
        input.header("Auth-Token", lazyToken.get());
        input.insert(0, url());
        return input.request();
    }
};
