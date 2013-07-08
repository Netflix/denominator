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

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(manager.provider().basicRecordTypes()));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            for (ResourceRecordSet<?> rrs : rrsApi(zone)) {
                checkRRS(rrs);
                assertFalse(rrs.qualifier().isPresent(), "Only basic record sets should be returned. found: " + rrs);
                Optional<ResourceRecordSet<?>> byNameAndType = rrsApi(zone).getByNameAndType(rrs.name(), rrs.type());
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
            String name = rrset.name();
            List<ResourceRecordSet<?>> withName = Lists.newArrayList();
            withName.add(rrset);
            while (rrsIterator.hasNext()) {
                rrset = rrsIterator.next();
                if (!name.equalsIgnoreCase(rrset.name()))
                        break;
                withName.add(rrset);
            }
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(rrsApi(zone).iterateByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(rrsApi(zone).iterateByName("ARGHH." + zone.name()).hasNext());
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
    public Object[][] simpleRecords() {
        ImmutableList<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(stockRRSets().values());
        Object[][] data = new Object[rrsets.size()][1];
        for (int i = 0; i < rrsets.size(); i++)
            data[i][0] = rrsets.get(i);
        return data;
    }

    @Test(dataProvider = "simpleRecords")
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

    @Test(dependsOnMethods = "putNewRRS", dataProvider = "simpleRecords")
    private void putChangingTTL(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        rrsApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(200000)
                    .add(recordSet.records().get(0)).build());;

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zone)
                .getByNameAndType(recordSet.name(), recordSet.type());

        assertPresent(rrs, zone, recordSet.name(), recordSet.type());

        checkRRS(rrs.get());
        assertEquals(rrs.get().name(), recordSet.name());
        assertEquals(rrs.get().type(), recordSet.type());
        assertEquals(rrs.get().ttl().get(), Integer.valueOf(200000));
        assertEquals(rrs.get().records().size(), 1);
        assertEquals(rrs.get().records().get(0), recordSet.records().get(0));
    }

    @Test(dependsOnMethods = "putChangingTTL", dataProvider = "simpleRecords")
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
