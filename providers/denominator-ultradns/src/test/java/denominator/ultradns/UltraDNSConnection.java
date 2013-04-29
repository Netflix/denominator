package denominator.ultradns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import java.util.Map;

import com.google.common.base.Splitter;

import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.profile.BaseGeoWriteCommandsLiveTest.MutableGeoRRSet;

public class UltraDNSConnection {

    final DNSApiManager manager;
    final String mutableZone;
    final MutableGeoRRSet mutableGeoRRSet;

    UltraDNSConnection() {
        String username = emptyToNull(getProperty("ultradns.username"));
        String password = emptyToNull(getProperty("ultradns.password"));
        if (username != null && password != null) {
            manager = Denominator.create(new UltraDNSProvider(), credentials(username, password));
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("ultradns.zone"));
        String toParse = emptyToNull(getProperty("ultradns.rrset-geo"));
        if (toParse != null) {
            Map<String, String> parsed = Splitter.on(',').withKeyValueSeparator('=').split(toParse);
            mutableGeoRRSet = new MutableGeoRRSet();
            mutableGeoRRSet.zone = parsed.get("zone");
            mutableGeoRRSet.name = parsed.get("name");
            mutableGeoRRSet.type = parsed.get("type");
            mutableGeoRRSet.group = parsed.get("group");
        } else {
            mutableGeoRRSet = null;
        }
    }
}
