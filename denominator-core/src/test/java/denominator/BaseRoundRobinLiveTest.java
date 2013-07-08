package denominator;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterators.any;
import static com.google.common.collect.Maps.filterKeys;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
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
                                          .add(recordSet.records().get(0)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().ttl().get(), Integer.valueOf(1800));
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().records().size(), 1);
        assertEquals(rrs.get().records().get(0), recordSet.records().get(0));
    }

    @Test(dependsOnMethods = "putNewRRS", dataProvider = "roundRobinRecords")
    private void putAddingRData(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(1800)
                    .add(recordSet.records().get(0))
                    .add(recordSet.records().get(1)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().records().size(), 2);
        assertEquals(rrs.get().records().get(0), recordSet.records().get(0));
        assertEquals(rrs.get().records().get(1), recordSet.records().get(1));
    }

    @Test(dependsOnMethods = "putAddingRData", dataProvider = "roundRobinRecords")
    private void putChangingTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(200000)
                    .add(recordSet.records().get(0))
                    .add(recordSet.records().get(1)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().ttl().get(), Integer.valueOf(200000));
        assertEquals(rrs.get().records().size(), 2);
        assertEquals(rrs.get().records().get(0), recordSet.records().get(0));
        assertEquals(rrs.get().records().get(1), recordSet.records().get(1));
    }

    @Test(dependsOnMethods = "putChangingTTL", dataProvider = "roundRobinRecords")
    private void putRemovingRData(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(200000)
                    .add(recordSet.records().get(0)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().records().size(), 1);
        assertEquals(rrs.get().records().get(0), recordSet.records().get(0));
    }

    @Test(dependsOnMethods = "putRemovingRData", dataProvider = "roundRobinRecords")
    private void deleteRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());

        String failureMessage = format("recordset(%s, %s) still exists in %s", recordSet.name(), recordSet.type(), zone);
        assertFalse(rrsApi(zone).getByNameAndType(recordSet.name(), recordSet.type()).isPresent(), failureMessage);
        assertFalse(any(rrsApi(zone).iterator(), and(nameEqualTo(recordSet.name()), typeEqualTo(recordSet.type()))),
                failureMessage);

        // test no exception if re-applied
        rrsApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());
    }
}
