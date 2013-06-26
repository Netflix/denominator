package denominator.dynect;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.google.common.base.Supplier;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import feign.RequestTemplate.Body;

/**
 * gets the last auth token, expiring if the url or credentials changed
 */
// similar to guava MemoizingSupplier
@Singleton
class InvalidatableTokenSupplier implements Supplier<String> {
    interface Session {
        @POST
        @Path("/Session")
        @Body("%7B\"customer_name\":\"{customer_name}\",\"user_name\":\"{user_name}\",\"password\":\"{password}\"%7D")
        String login(@FormParam("customer_name") String customer, @FormParam("user_name") String user,
                @FormParam("password") String password);
    }

    private final denominator.Provider provider;
    private final Session session;
    private final Provider<Credentials> credentials;
    transient volatile String lastUrl;
    transient volatile int lastCredentialsHashCode;
    transient volatile boolean initialized;
    // "value" does not need to be volatile; visibility piggy-backs
    // on above
    transient String value;

    @Inject
    InvalidatableTokenSupplier(denominator.Provider provider, Session session,
            javax.inject.Provider<Credentials> credentials) {
        this.provider = provider;
        this.session = session;
        this.credentials = credentials;
        // for toString
        this.lastUrl = provider.url();
    }

    public void invalidate() {
        initialized = false;
    }

    @Override
    public String get() {
        String currentUrl = provider.url();
        Credentials currentCreds = credentials.get();

        if (needsRefresh(currentUrl, currentCreds)) {
            synchronized (this) {
                if (needsRefresh(currentUrl, currentCreds)) {
                    lastCredentialsHashCode = currentCreds.hashCode();
                    lastUrl = currentUrl;
                    String t = auth(currentCreds);
                    value = t;
                    initialized = true;
                    return t;
                }
            }
        }
        return value;
    }

    private boolean needsRefresh(String currentUrl, Credentials currentCreds) {
        return !initialized || currentCreds.hashCode() != lastCredentialsHashCode || !currentUrl.equals(lastUrl);
    }

    private String auth(Credentials currentCreds) {
        List<Object> listCreds = ListCredentials.asList(currentCreds);
        return session.login(listCreds.get(0).toString(), listCreds.get(1).toString(), listCreds.get(2).toString());
    }

    @Override
    public String toString() {
        return "InvalidatableTokenSupplier(" + lastUrl + ")";
    }
}
