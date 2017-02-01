package denominator.ultradns;

import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

import javax.inject.Inject;

public class SessionTarget implements Target<InvalidatableTokenProvider.Session> {
    private final Provider provider;

    @Inject
    SessionTarget(Provider provider) {
        this.provider = provider;
    }

    @Override
    public Class<InvalidatableTokenProvider.Session> type() {
        return InvalidatableTokenProvider.Session.class;
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
        input.header("Content-Type", "application/x-www-form-urlencoded");
        input.insert(0, url());
        return input.request();
    }
}
