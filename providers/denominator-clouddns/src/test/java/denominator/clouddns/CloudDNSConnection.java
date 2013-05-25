package denominator.clouddns;


import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;
import denominator.DNSApiManager;
import denominator.Denominator;

public class CloudDNSConnection {

    final DNSApiManager manager;
    final String mutableZone;

    CloudDNSConnection() {
        String username = emptyToNull(getProperty("clouddns.username"));
        String apiKey = emptyToNull(getProperty("clouddns.apiKey"));
        if (username != null && apiKey != null) {
            CloudDNSProvider provider = new CloudDNSProvider(emptyToNull(getProperty("clouddns.url")));
            manager = Denominator.create(provider, credentials(username, apiKey));
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("clouddns.zone"));
    }
}
