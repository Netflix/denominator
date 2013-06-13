package denominator.ultradns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;
import denominator.DNSApiManager;
import denominator.Denominator;

public class UltraDNSConnection {

    final DNSApiManager manager;
    final String mutableZone;

    UltraDNSConnection() {
        String username = emptyToNull(getProperty("ultradns.username"));
        String password = emptyToNull(getProperty("ultradns.password"));
        if (username != null && password != null) {
            UltraDNSProvider provider = new UltraDNSProvider(emptyToNull(getProperty("ultradns.url")));
            manager = Denominator.create(provider, credentials(username, password));
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("ultradns.zone"));
    }
}
