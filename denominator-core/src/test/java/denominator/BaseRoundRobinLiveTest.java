package denominator;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterators.any;
import static com.google.common.collect.Maps.filterKeys;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseRoundRobinLiveTest extends BaseProviderLiveTest {

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(manager.provider().profileToRecordTypes().get("roundRobin")));
    }

    @DataProvider(name = "roundRobinRecords")
    public Object[][] roundRobinRecords() {
        ImmutableList<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(stockRRSets().values());
        Object[][] data = new Object[rrsets.size()][1];
        for (int i = 0; i < rrsets.size(); i++)
            data[i][0] = rrsets.get(i);
        return data;
    }

    @Test(dataProvider = "roundRobinRecords")
    private void putNewRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        skipIfRRSetExists(zone, recordSet.name(), recordSet.type());

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                          .name(recordSet.name())
                                          .type(recordSet.type())
                                          .ttl(1800)
                                          .add(recordSet.rdata().get(0)).build());

        ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs);
        assertEquals(rrs.name(), recordSet.name());
        assertEquals(rrs.ttl(), Integer.valueOf(1800));
        assertEquals(rrs.type(), recordSet.type());
        assertEquals(rrs.rdata().size(), 1);
        assertEquals(rrs.rdata().get(0), recordSet.rdata().get(0));
    }

    @Test(dependsOnMethods = "putNewRRS", dataProvider = "roundRobinRecords")
    private void putAddingRData(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(1800)
                    .add(recordSet.rdata().get(0))
                    .add(recordSet.rdata().get(1)).build());

        ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs);
        assertEquals(rrs.name(), recordSet.name());
        assertEquals(rrs.type(), recordSet.type());
        assertEquals(rrs.rdata().size(), 2);
        assertEquals(rrs.rdata().get(0), recordSet.rdata().get(0));
        assertEquals(rrs.rdata().get(1), recordSet.rdata().get(1));
    }

    @Test(dependsOnMethods = "putAddingRData", dataProvider = "roundRobinRecords")
    private void putChangingTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(200000)
                    .add(recordSet.rdata().get(0))
                    .add(recordSet.rdata().get(1)).build());

        ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs);
        assertEquals(rrs.name(), recordSet.name());
        assertEquals(rrs.type(), recordSet.type());
        assertEquals(rrs.ttl(), Integer.valueOf(200000));
        assertEquals(rrs.rdata().size(), 2);
        assertEquals(rrs.rdata().get(0), recordSet.rdata().get(0));
        assertEquals(rrs.rdata().get(1), recordSet.rdata().get(1));
    }

    @Test(dependsOnMethods = "putChangingTTL", dataProvider = "roundRobinRecords")
    private void putRemovingRData(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(200000)
                    .add(recordSet.rdata().get(0)).build());

        ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs);
        assertEquals(rrs.name(), recordSet.name());
        assertEquals(rrs.type(), recordSet.type());
        assertEquals(rrs.rdata().size(), 1);
        assertEquals(rrs.rdata().get(0), recordSet.rdata().get(0));
    }

    @Test(dependsOnMethods = "putRemovingRData", dataProvider = "roundRobinRecords")
    private void deleteRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());

        String failureMessage = format("recordset(%s, %s) still exists in %s", recordSet.name(), recordSet.type(), zone);
        assertTrue(rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type()) == null, failureMessage);
        assertFalse(any(rrsApi(zone).iterator(), predicate(nameAndTypeEqualTo(recordSet.name(), recordSet.type()))),
                failureMessage);

        // test no exception if re-applied
        rrsApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());
    }
}
