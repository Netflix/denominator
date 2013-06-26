package denominator.ultradns;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import denominator.model.Zone;

public final class UltraDNSZoneApi implements denominator.ZoneApi {
    private final UltraDNS api;
    private final Lazy<String> account;

    @Inject
    UltraDNSZoneApi(UltraDNS api, @Named("accountID") Lazy<String> account) {
        this.api = api;
        this.account = account;
    }

    /**
     * in UltraDNS, zones are scoped to an account.
     */
    @Override
    public Iterator<Zone> iterator() {
        return api.zonesOfAccount(account.get()).iterator();
    }
}