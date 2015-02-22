package denominator.dynect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import denominator.CheckConnection;
import denominator.Credentials;
import denominator.dynect.DynECT.Data;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import static denominator.common.Preconditions.checkNotNull;

/**
 * gets the last auth token, expiring if the url or credentials changed
 */
// similar to guava MemoizingSupplier
@Singleton
class InvalidatableTokenProvider implements Provider<String>, CheckConnection {

  private final denominator.Provider provider;
  private final Session session;
  private final Provider<Credentials> credentials;
  private final AtomicReference<Boolean> sessionValid;
  transient volatile String lastUrl;
  transient volatile int lastCredentialsHashCode;
  // "value" does not need to be volatile; visibility piggy-backs
  // on above
  transient String value;

  @Inject
  InvalidatableTokenProvider(denominator.Provider provider, Session session,
                             Provider<Credentials> credentials,
                             AtomicReference<Boolean> sessionValid) {
    this.provider = provider;
    this.session = session;
    this.credentials = credentials;
    this.sessionValid = sessionValid;
    // for toString
    this.lastUrl = provider.url();
  }

  @Override
  public boolean ok() {
    try {
      session.check(get());
      return true;
    } catch (RuntimeException e) {
      e.printStackTrace();
      sessionValid.set(false);
      return false;
    }
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
          sessionValid.set(true);
          return t;
        }
      }
    }
    return value;
  }

  private boolean needsRefresh(String currentUrl, Credentials currentCreds) {
    return !sessionValid.get() || currentCreds.hashCode() != lastCredentialsHashCode || !currentUrl
        .equals(lastUrl);
  }

  private String auth(Credentials currentCreds) {
    String customer;
    String username;
    String password;
    if (currentCreds instanceof List) {
      @SuppressWarnings("unchecked")
      List<Object> listCreds = (List<Object>) currentCreds;
      customer = listCreds.get(0).toString();
      username = listCreds.get(1).toString();
      password = listCreds.get(2).toString();
    } else if (currentCreds instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mapCreds = (Map<String, Object>) currentCreds;
      customer = checkNotNull(mapCreds.get("customer"), "customer").toString();
      username = checkNotNull(mapCreds.get("username"), "username").toString();
      password = checkNotNull(mapCreds.get("password"), "password").toString();
    } else {
      throw new IllegalArgumentException("Unsupported credential type: " + currentCreds);
    }

    return session.login(customer, username, password).data;
  }

  @Override
  public String toString() {
    return "InvalidatableTokenSupplier(" + lastUrl + ")";
  }

  interface Session {

    @RequestLine("POST /Session")
    @Body("%7B\"customer_name\":\"{customer_name}\",\"user_name\":\"{user_name}\",\"password\":\"{password}\"%7D")
    Data<String> login(@Param("customer_name") String customer, @Param("user_name") String user,
                       @Param("password") String password);

    @RequestLine("GET /Session")
    @Headers("Auth-Token: {Auth-Token}")
    void check(@Param("Auth-Token") String token);
  }
}
