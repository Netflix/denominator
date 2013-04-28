package denominator.mock;

import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.profile.BaseGeoWriteCommandsLiveTest.MutableGeoRRSet;

public class MockConnection {
    final DNSApiManager manager;
    final String mutableZone;
    final MutableGeoRRSet mutableGeoRRSet;

    MockConnection() {
        manager = Denominator.create(new MockProvider());
        mutableZone = "denominator.io.";
        mutableGeoRRSet = new MutableGeoRRSet();
        mutableGeoRRSet.zone = mutableZone;
        mutableGeoRRSet.name = "www2.geo.denominator.io.";
        mutableGeoRRSet.type = "A";
        mutableGeoRRSet.group = "alazona";
    }
}
