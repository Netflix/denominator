package denominator;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterators.any;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseRecordSetLiveTest extends BaseProviderLiveTest {
    protected ImmutableList<String> supportedRecordTypes = ImmutableList.of("AAAA", "A", "CNAME", "MX", "NS", "PTR",
            "SPF", "SRV", "SSHFP", "TXT");

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(supportedRecordTypes));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            for (Iterator<ResourceRecordSet<?>> rrsIterator = rrsApi(zone).iterator(); rrsIterator.hasNext();) {
                ResourceRecordSet<?> rrs = rrsIterator.next();
                checkRRS(rrs);
                Optional<ResourceRecordSet<?>> byNameAndType = rrsApi(zone).getByNameAndType(rrs.getName(), rrs.getType());
                assertTrue(byNameAndType.isPresent(), "could not lookup by name and type: " + rrs);
                assertEquals(byNameAndType.get(), rrs);
            }
        }
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            Iterator<ResourceRecordSet<?>> rrsIterator = rrsApi(zone).iterator();
            if (!rrsIterator.hasNext())
                continue;
            ResourceRecordSet<?> rrset = rrsIterator.next();
            String name = rrset.getName();
            List<ResourceRecordSet<?>> withName = Lists.newArrayList();
            withName.add(rrset);
            while (rrsIterator.hasNext()) {
                rrset = rrsIterator.next();
                if (!name.equalsIgnoreCase(rrset.getName()))
                        break;
                withName.add(rrset);
            }
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(rrsApi(zone).listByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(rrsApi(zone).listByName("ARGHH." + zone.name()).hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameAndTypeWhenAbsent() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertEquals(rrsApi(zone).getByNameAndType("ARGHH." + zone.name(), "TXT"), Optional.absent());
            break;
        }
    }

    /**
     * value at index 1 in the rrs is used for testing replace
     */
    @DataProvider(name = "simpleRecords")
    public Object[][] roundRobinRecords() {
        ImmutableList<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(stockRRSets().values());
        Object[][] data = new Object[rrsets.size()][1];
        for (int i = 0; i < rrsets.size(); i++)
            data[i][0] = rrsets.get(i);
        return data;
    }

    @Test(dataProvider = "simpleRecords")
    private void createNewRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        skipIfRRSetExists(zone, recordSet.getName(), recordSet.getType());

        rrsApi(zone).add(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.getName())
                                              .type(recordSet.getType())
                                              .ttl(1800)
                                              .add(recordSet.get(0)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.getName(), recordSet.getType());

        assertPresent(rrs, zone, recordSet.getName(), recordSet.getType());

        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordSet.getName());
        assertEquals(rrs.get().getTTL().get(), Integer.valueOf(1800));
        assertEquals(rrs.get().getType(), recordSet.getType());
        assertEquals(rrs.get().size(), 1);
        assertEquals(rrs.get().get(0), recordSet.get(0));
    }

    @Test(dependsOnMethods = "createNewRRS", dataProvider = "simpleRecords")
    private void applyTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).applyTTLToNameAndType(200000, recordSet.getName(), recordSet.getType());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.getName(), recordSet.getType());

        assertPresent(rrs, zone, recordSet.getName(), recordSet.getType());

        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordSet.getName());
        assertEquals(rrs.get().getType(), recordSet.getType());
        assertEquals(rrs.get().getTTL().get(), Integer.valueOf(200000));
        assertEquals(rrs.get().size(), 1);
        assertEquals(rrs.get().get(0), recordSet.get(0));
    }

    @Test(dependsOnMethods = "applyTTL", dataProvider = "simpleRecords")
    private void replaceExistingRRSUpdatingTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).replace(ResourceRecordSet.<Map<String, Object>> builder()
                                                  .name(recordSet.getName())
                                                  .type(recordSet.getType())
                                                  .ttl(10000)
                                                  .add(recordSet.get(1)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.getName(), recordSet.getType());

        assertPresent(rrs, zone, recordSet.getName(), recordSet.getType());

        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordSet.getName());
        assertEquals(rrs.get().getType(), recordSet.getType());
        assertEquals(rrs.get().getTTL().get(), Integer.valueOf(10000));
        assertEquals(rrs.get().size(), 1);
        assertEquals(rrs.get().get(0), recordSet.get(1));
    }

    @Test(dependsOnMethods = "replaceExistingRRSUpdatingTTL", dataProvider = "simpleRecords")
    private void removeLastRecordFromExistingRRSRemovesRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).remove(ResourceRecordSet.<Map<String, Object>> builder()
                                                 .name(recordSet.getName())
                                                 .type(recordSet.getType())
                                                 .add(recordSet.get(1)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.getName(), recordSet.getType());

        assertFalse(rrs.isPresent(),
                format("recordset(%s, %s) still present in %s", recordSet.getName(), recordSet.getType(), zone));
    }

    @Test(dataProvider = "simpleRecords")
    private void deleteRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();
        String recordName = recordSet.getName().replace("." + zone.name(), "-delete." + zone.name());

        skipIfRRSetExists(zone, recordName, recordSet.getType());

        rrsApi(zone).add(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordName)
                                              .type(recordSet.getType())
                                              .add(recordSet.get(0)).build());

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone).getByNameAndType(recordName, recordSet.getType());

        assertPresent(rrs, zone, recordName, recordSet.getType());

        rrsApi(zone).deleteByNameAndType(recordName, recordSet.getType());

        String failureMessage = format("recordset(%s, %s) still exists in %s", recordName, recordSet.getType(), zone);
        assertFalse(rrsApi(zone).getByNameAndType(recordName, recordSet.getType()).isPresent(), failureMessage);
        assertFalse(any(rrsApi(zone).iterator(), and(nameEqualTo(recordName), typeEqualTo(recordSet.getType()))),
                failureMessage);

        // test no exception if re-applied
        rrsApi(zone).deleteByNameAndType(recordName, recordSet.getType());
    }
}
