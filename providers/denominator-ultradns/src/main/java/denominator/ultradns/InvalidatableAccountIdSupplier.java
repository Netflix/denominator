package denominator.ultradns;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.base.Supplier;

import denominator.Credentials;

/**
 * gets the last account id, expiring if the url or credentials changed
 */
// similar to guava MemoizingSupplier
@Singleton
class InvalidatableAccountIdSupplier implements Supplier<String> {

    private final denominator.Provider provider;
    private final UltraDNS api;
    private final Provider<Credentials> credentials;
    transient volatile String lastUrl;
    transient volatile int lastCredentialsHashCode;
    transient volatile boolean initialized;
    // "value" does not need to be volatile; visibility piggy-backs
    // on above
    transient String value;

    @Inject
    InvalidatableAccountIdSupplier(denominator.Provider provider, UltraDNS api,
            javax.inject.Provider<Credentials> credentials) {
        this.provider = provider;
        this.api = api;
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
                    String accountId = api.accountId();
                    value = accountId;
                    initialized = true;
                    return accountId;
                }
            }
        }
        return value;
    }

    private boolean needsRefresh(String currentUrl, Credentials currentCreds) {
        return !initialized || currentCreds.hashCode() != lastCredentialsHashCode || !currentUrl.equals(lastUrl);
    }

    @Override
    public String toString() {
        return "InvalidatableAccountIdSupplier(" + lastUrl + ")";
    }
}
