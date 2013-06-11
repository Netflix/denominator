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

    protected ImmutableList<String> supportedRecordTypes = ImmutableList.of("AAAA", "A", "MX", "NS", "PTR", "SPF",
            "SRV", "SSHFP", "TXT");

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(supportedRecordTypes));
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
    private void createNewRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        skipIfRRSetExists(zone, recordSet.name(), recordSet.type());

        rrsApi(zone).add(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.name())
                                              .type(recordSet.type())
                                              .ttl(1800)
                                              .add(recordSet.rdata().get(0)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().ttl().get(), Integer.valueOf(1800));
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().rdata().size(), 1);
        assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(0));
    }

    @Test(dependsOnMethods = "createNewRRS", dataProvider = "roundRobinRecords")
    private void addRecordToExistingRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).add(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.name())
                                              .type(recordSet.type())
                                              .add(recordSet.rdata().get(1)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().rdata().size(), 2);
        assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(0));
        assertEquals(rrs.get().rdata().get(1), recordSet.rdata().get(1));
    }

    @Test(dependsOnMethods = "addRecordToExistingRRS", dataProvider = "roundRobinRecords")
    private void applyTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).applyTTLToNameAndType(200000, recordSet.name(), recordSet.type());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().ttl().get(), Integer.valueOf(200000));
        assertEquals(rrs.get().rdata().size(), 2);
        assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(0));
        assertEquals(rrs.get().rdata().get(1), recordSet.rdata().get(1));
    }

    @Test(dependsOnMethods = "applyTTL", dataProvider = "roundRobinRecords")
    private void replaceExistingRRSUpdatingTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).replace(ResourceRecordSet.<Map<String, Object>> builder()
                                                  .name(recordSet.name())
                                                  .type(recordSet.type())
                                                  .ttl(10000)
                                                  .add(recordSet.rdata().get(0))
                                                  .add(recordSet.rdata().get(2)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().ttl().get(), Integer.valueOf(10000));
        assertEquals(rrs.get().rdata().size(), 2);
        assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(0));
        assertEquals(rrs.get().rdata().get(1), recordSet.rdata().get(2));
    }

    @Test(dependsOnMethods = "replaceExistingRRSUpdatingTTL", dataProvider = "roundRobinRecords")
    private void removeRecordFromExistingRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).remove(ResourceRecordSet.<Map<String, Object>> builder()
                                                 .name(recordSet.name())
                                                 .type(recordSet.type())
                                                 .add(recordSet.rdata().get(2)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().rdata().size(), 1);
        assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(0));
    }

    @Test(dependsOnMethods = "removeRecordFromExistingRRS", dataProvider = "roundRobinRecords")
    private void removeLastRecordFromExistingRRSRemovesRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).remove(ResourceRecordSet.<Map<String, Object>> builder()
                                                 .name(recordSet.name())
                                                 .type(recordSet.type())
                                                 .add(recordSet.rdata().get(0)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertFalse(rrs.isPresent(),
                format("recordset(%s, %s) still present in %s", recordSet.name(), recordSet.type(), zone));
    }

    @Test(dataProvider = "roundRobinRecords")
    private void deleteRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();
        String recordName = recordSet.name().replace("." + zone.name(), "-delete." + zone.name());

        skipIfRRSetExists(zone, recordName, recordSet.type());

        rrsApi(zone).add(
                ResourceRecordSet.<Map<String, Object>> builder().name(recordName).type(recordSet.type())
                        .addAll(recordSet).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone).getByNameAndType(recordName, recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        rrsApi(zone).deleteByNameAndType(recordName, recordSet.type());

        String failureMessage = format("recordset(%s, %s) still exists in %s", recordName, recordSet.type(), zone);
        assertFalse(rrsApi(zone).getByNameAndType(recordName, recordSet.type()).isPresent(), failureMessage);
        assertFalse(any(rrsApi(zone).iterator(), and(nameEqualTo(recordName), typeEqualTo(recordSet.type()))),
                failureMessage);

        // test no exception if re-applied
        rrsApi(zone).deleteByNameAndType(recordName, recordSet.type());
    }
}
