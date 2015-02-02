package denominator.dynect;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import denominator.CheckConnection;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.dynect.DynECT.Data;
import feign.Body;
import feign.Headers;
import feign.RequestLine;

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
    List<Object> listCreds = ListCredentials.asList(currentCreds);
    return session.login(listCreds.get(0).toString(), listCreds.get(1).toString(),
                         listCreds.get(2).toString()).data;
  }

  @Override
  public String toString() {
    return "InvalidatableTokenSupplier(" + lastUrl + ")";
  }

  interface Session {

    @RequestLine("POST /Session")
    @Body("%7B\"customer_name\":\"{customer_name}\",\"user_name\":\"{user_name}\",\"password\":\"{password}\"%7D")
    Data<String> login(@Named("customer_name") String customer, @Named("user_name") String user,
                       @Named("password") String password);

    @RequestLine("GET /Session")
    @Headers("Auth-Token: {Auth-Token}")
    void check(@Named("Auth-Token") String token);
  }
}
