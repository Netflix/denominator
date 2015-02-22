package denominator.designate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import denominator.Credentials;
import denominator.designate.KeystoneV2.TokenIdAndPublicURL;

import static denominator.common.Preconditions.checkNotNull;

/**
 * gets the current endpoint and authorization token from the identity service using password or api
 * key auth.
 */
// similar to guava MemoizingSupplier
@Singleton
class InvalidatableAuthProvider implements Provider<TokenIdAndPublicURL> {

  private final denominator.Provider provider;
  private final KeystoneV2 identityService;
  private final Provider<Credentials> credentials;
  transient volatile String lastKeystoneUrl;
  transient volatile int lastCredentialsHashCode;
  transient volatile boolean initialized;
  // "value" does not need to be volatile; visibility piggy-backs
  // on above
  transient TokenIdAndPublicURL value;

  @Inject
  InvalidatableAuthProvider(denominator.Provider provider, KeystoneV2 identityService,
                            javax.inject.Provider<Credentials> credentials) {
    this.provider = provider;
    this.identityService = identityService;
    this.credentials = credentials;
    // for toString
    this.lastKeystoneUrl = provider.url();
  }

  public void invalidate() {
    initialized = false;
  }

  @Override
  public TokenIdAndPublicURL get() {
    String currentUrl = provider.url();
    Credentials currentCreds = credentials.get();

    if (needsRefresh(currentUrl, currentCreds)) {
      synchronized (this) {
        if (needsRefresh(currentUrl, currentCreds)) {
          lastCredentialsHashCode = currentCreds.hashCode();
          lastKeystoneUrl = currentUrl;
          TokenIdAndPublicURL t = auth(currentCreds);
          value = t;
          initialized = true;
          return t;
        }
      }
    }
    return value;
  }

  private boolean needsRefresh(String currentUrl, Credentials currentCreds) {
    return !initialized || currentCreds.hashCode() != lastCredentialsHashCode
           || !currentUrl.equals(lastKeystoneUrl);
  }

  private TokenIdAndPublicURL auth(Credentials currentCreds) {
    String tenantId;
    String username;
    String password;
    if (currentCreds instanceof List) {
      @SuppressWarnings("unchecked")
      List<Object> listCreds = (List<Object>) currentCreds;
      tenantId = listCreds.get(0).toString();
      username = listCreds.get(1).toString();
      password = listCreds.get(2).toString();
    } else if (currentCreds instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mapCreds = (Map<String, Object>) currentCreds;
      tenantId = checkNotNull(mapCreds.get("tenantId"), "tenantId").toString();
      username = checkNotNull(mapCreds.get("username"), "username").toString();
      password = checkNotNull(mapCreds.get("password"), "password").toString();
    } else {
      throw new IllegalArgumentException("Unsupported credential type: " + currentCreds);
    }

    URI url = URI.create(lastKeystoneUrl);
    return identityService.passwordAuth(url, tenantId, username, password);
  }

  @Override
  public String toString() {
    return "InvalidatableAuthProvider(" + lastKeystoneUrl + ")";
  }
}
