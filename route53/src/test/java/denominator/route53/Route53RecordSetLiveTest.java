package denominator.route53;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import denominator.BaseRecordSetLiveTest;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

@Test
public class Route53RecordSetLiveTest extends BaseRecordSetLiveTest {
    @BeforeClass
    private void setUp() {
        Route53Connection connection = new Route53Connection();
        manager = connection.manager;
        setMutableZoneIfPresent(connection.mutableZone);
    }

    @DataProvider(name = "target")
    public Object[][] simpleRecords() {
        Zone zone = skipIfNoMutableZone();
        String recordSuffix = recordPrefix + "." + zone.name();
        Object[][] data = new Object[2][1];
        data[0][0] = a("target-ipv4-" + recordSuffix, ImmutableList.of("192.0.2.1", "198.51.100.1", "203.0.113.1"));
        data[1][0] = aaaa("target-ipv6-" + recordSuffix, ImmutableList.of("2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
                "2001:0DB8:85A3:0000:0000:8A2E:0370:7335", "2001:0DB8:85A3:0000:0000:8A2E:0370:7336"));
        return data;
    }

    @Test(dataProvider = "target")
    private void aliasRRSet(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());
        rrsApi(zone).deleteByNameAndType(recordSet.name().replace("target", "alias"), recordSet.type());

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                          .name(recordSet.name())
                                          .type(recordSet.type())
                                          .ttl(1800)
                                          .add(recordSet.records().get(0)).build());

        ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type());
        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        ResourceRecordSet<?> alias = ResourceRecordSet.<Map<String, Object>> builder()
                .name(rrs.name().replace("target", "alias"))//
                .type(rrs.type())//
                .add(AliasTarget.create(zone.id(), rrs.name())).build();

        rrsApi(zone).put(alias);

        alias = rrsApi(zone).getByNameAndType(alias.name(), alias.type());

        assertPresent(alias, zone, alias.name(), alias.type());

        checkRRS(alias);
        assertEquals(alias.name(), recordSet.name().replace("target", "alias"));
        assertNull(alias.ttl());
        assertEquals(alias.type(), recordSet.type());
        assertEquals(alias.records().size(), 1);
        assertEquals(alias.records().get(0), AliasTarget.create(zone.id(), rrs.name()));
    }
}
