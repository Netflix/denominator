package denominator.ultradns;

import denominator.CheckConnection;
import denominator.Credentials;
import denominator.ultradns.model.TokenResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static denominator.common.Preconditions.checkNotNull;

/**
 * gets the last auth token, expiring if the current time exceed token expiry time
 */
public class InvalidatableTokenProvider implements Provider<String>, CheckConnection {

    private final denominator.Provider provider;
    private final Session session;
    private final Provider<Credentials> credentials;
    private final AtomicReference<Boolean> sessionValid;
    private final long durationMillis;
    transient volatile int lastCredentialsHashCode;
    transient volatile long expirationMillis;
    transient String token;

    @Inject
    InvalidatableTokenProvider(denominator.Provider provider, Session session,
                               Provider<Credentials> credentials,
                               AtomicReference<Boolean> sessionValid) {
        this.provider = provider;
        this.session = session;
        this.credentials = credentials;
        this.sessionValid = sessionValid;
        // As per UltraDNS Authentication API Response
        this.durationMillis = 3600 * 1000;
    }

    @Override
    public boolean ok() {
        boolean isValid = System.currentTimeMillis() < expirationMillis;
        if(!isValid) {
            sessionValid.set(false);
        }
        return isValid;
    }

    @Override
    public String get() {
        Credentials currentCreds = credentials.get();
        long currentTime = System.currentTimeMillis();

        if (needsRefresh(currentTime, currentCreds)) {
            lastCredentialsHashCode = currentCreds.hashCode();
            TokenResponse t = auth(currentCreds);
            expirationMillis = currentTime + durationMillis;
            token = t.getAccessToken();
            sessionValid.set(true);
            return token;
        }
        return token;
    }

    private boolean needsRefresh(long currentTime, Credentials currentCreds) {
        return !sessionValid.get() || expirationMillis == 0 || currentTime - expirationMillis >= 0
                || currentCreds.hashCode() != lastCredentialsHashCode;
    }

    private TokenResponse auth(Credentials currentCreds) {
        String username;
        String password;
        if (currentCreds instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listCreds = (List<Object>) currentCreds;
            username = listCreds.get(0).toString();
            password = listCreds.get(1).toString();
        } else if (currentCreds instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapCreds = (Map<String, Object>) currentCreds;
            username = checkNotNull(mapCreds.get("username"), "username").toString();
            password = checkNotNull(mapCreds.get("password"), "password").toString();
        } else {
            throw new IllegalArgumentException("Unsupported credential type: " + currentCreds);
        }
        return session.login("password", username,password);
    }

    interface Session {
        @RequestLine("POST /authorization/token")
        @Headers({"Content-Type: application/x-www-form-urlencoded"})
        TokenResponse login(@Param("grant_type") String grantType, @Param("username") String userName, @Param("password") String password);
    }
}
