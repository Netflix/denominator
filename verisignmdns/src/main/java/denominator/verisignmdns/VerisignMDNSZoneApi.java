package denominator.verisignmdns;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Provider;

import denominator.Credentials;
import denominator.model.Zone;

public final class VerisignMDNSZoneApi implements denominator.ZoneApi {
    private final VrsnMdns api;
    private final Provider<Credentials> credentialsProvider;

    @Inject
    VerisignMDNSZoneApi(VrsnMdns api, Provider<Credentials> credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        this.api = api;
    }

    /*
     * (non-Javadoc)
     * 
     * @see denominator.ZoneApi#iterator()
     */
    @Override
    public Iterator<Zone> iterator() {
        return api.getZonesForUser().iterator();
    }

}
