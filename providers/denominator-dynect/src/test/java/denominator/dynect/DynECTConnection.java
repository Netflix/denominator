package denominator.dynect;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;
import denominator.DNSApiManager;
import denominator.Denominator;

public class DynECTConnection {

    final DNSApiManager manager;
    final String mutableZone;

    DynECTConnection() {
        String customer = emptyToNull(getProperty("dynect.customer"));
        String username = emptyToNull(getProperty("dynect.username"));
        String password = emptyToNull(getProperty("dynect.password"));
        if (customer != null && username != null && password != null) {
            DynECTProvider provider = new DynECTProvider(emptyToNull(getProperty("dynect.url")));
            manager = Denominator.create(provider, credentials(customer, username, password));
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("dynect.zone"));
    }
}
