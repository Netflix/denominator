package denominator.route53;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.io.BaseEncoding.base64;
import static com.google.common.net.HttpHeaders.DATE;
import static java.util.Locale.US;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;

// http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/RESTAuthentication.html
public class InvalidatableAuthenticationHeadersSupplier implements Supplier<Map<String, String>> {
    private final javax.inject.Provider<Credentials> credentials;
    private final long durationMillis;

    transient volatile String lastKeystoneUrl;
    transient volatile int lastCredentialsHashCode;
    // The special value 0 means "not yet initialized".
    transient volatile long expirationMillis;
    // "value" does not need to be volatile; visibility piggy-backs
    // on above
    transient Map<String, String> value;

    @Inject
    InvalidatableAuthenticationHeadersSupplier(javax.inject.Provider<Credentials> credentials) {
        this.credentials = credentials;
        // variance allowed is 5 minutes, so only re-hashing every 1m.
        this.durationMillis = TimeUnit.MINUTES.toMillis(1);
    }

    public void invalidate() {
        expirationMillis = 0;
    }

    @Override
    public Map<String, String> get() {
        long currentTime = System.currentTimeMillis();
        Credentials currentCreds = credentials.get();
        if (needsRefresh(currentTime, currentCreds)) {
            synchronized (this) {
                if (needsRefresh(currentTime, currentCreds)) {
                    lastCredentialsHashCode = currentCreds.hashCode();
                    Map<String, String> t = auth(currentCreds);
                    value = t;
                    expirationMillis = currentTime + durationMillis;
                    return t;
                }
            }
        }
        return value;
    }

    private boolean needsRefresh(long currentTime, Credentials currentCreds) {
        return expirationMillis == 0 || currentTime - expirationMillis >= 0
                || currentCreds.hashCode() != lastCredentialsHashCode;
    }

    Map<String, String> auth(Credentials currentCreds) {
        List<Object> creds = ListCredentials.asList(currentCreds);
        String accessKey = creds.get(0).toString();
        String secretKey = creds.get(1).toString();
        String token = null;
        if (creds.size() == 3)
            token = creds.get(2).toString();
        String rfc1123Date = RFC1123.format(new Date());
        String signature = sign(rfc1123Date, secretKey);
        String auth = String.format(AUTH_FORMAT, accessKey, signature);

        Map<String, String> canContainNull = Maps.newLinkedHashMap();
        canContainNull.put(DATE, rfc1123Date);
        canContainNull.put("X-Amzn-Authorization", auth);
        // will remove if token is not set
        canContainNull.put("X-Amz-Security-Token", token);
        return canContainNull;
    }

    String sign(String rfc1123Date, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMACSHA256);
            mac.init(new SecretKeySpec(secretKey.getBytes(UTF_8), HMACSHA256));
            byte[] result = mac.doFinal(rfc1123Date.getBytes(UTF_8));
            return base64().encode(result);
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    private static final String AUTH_FORMAT = "AWS3-HTTPS AWSAccessKeyId=%s,Algorithm=HmacSHA256,Signature=%s";
    private static final String HMACSHA256 = "HmacSHA256";
    private static final SimpleDateFormat RFC1123 = new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss Z", US);
}