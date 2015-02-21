package denominator.ultradns;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import denominator.Credentials;

/**
 * gets the last account id, expiring if the url or credentials changed
 */
// similar to guava MemoizingSupplier
@Singleton
class InvalidatableAccountIdSupplier {

  private final denominator.Provider provider;
  private final UltraDNS api;
  private final Provider<Credentials> credentials;
  transient volatile String lastUrl;
  transient volatile int lastCredentialsHashCode;
  transient volatile boolean initialized;
  transient String value; // "value" does not need to be volatile; visibility piggy-backs on above

  @Inject
  InvalidatableAccountIdSupplier(denominator.Provider provider, UltraDNS api,
                                 javax.inject.Provider<Credentials> credentials) {
    this.provider = provider;
    this.api = api;
    this.credentials = credentials;
    this.lastUrl = provider.url(); // for toString
  }

  public void invalidate() {
    initialized = false;
  }

  public String get() {
    String currentUrl = provider.url();
    Credentials currentCreds = credentials.get();

    if (needsRefresh(currentUrl, currentCreds)) {
      synchronized (this) {
        if (needsRefresh(currentUrl, currentCreds)) {
          lastCredentialsHashCode = currentCreds.hashCode();
          lastUrl = currentUrl;
          String accountId = api.getAccountsListOfUser();
          value = accountId;
          initialized = true;
          return accountId;
        }
      }
    }
    return value;
  }

  private boolean needsRefresh(String currentUrl, Credentials currentCreds) {
    return !initialized || currentCreds.hashCode() != lastCredentialsHashCode || !currentUrl
        .equals(lastUrl);
  }

  @Override
  public String toString() {
    return "InvalidatableAccountIdSupplier(" + lastUrl + ")";
  }
}
