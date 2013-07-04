package denominator;

import static com.google.common.collect.Ordering.usingToString;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseReadOnlyLiveTest extends BaseProviderLiveTest {

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            for (ResourceRecordSet<?> rrs : allApi(zone)) {
                recordTypeCounts.getUnchecked(rrs.type()).addAndGet(rrs.rdata().size());
                checkRRS(rrs);
                checkListByNameAndTypeConsistent(zone, rrs);
                if (rrs.qualifier() != null) {
                    checkGetByNameTypeAndQualifierConsistent(zone, rrs);
                }
            }
        }
        logRecordSummary();
    }

    protected void checkGetByNameTypeAndQualifierConsistent(Zone zone, ResourceRecordSet<?> rrs) {
        ResourceRecordSet<?> byNameTypeAndQualifier = allApi(zone).getByNameTypeAndQualifier(rrs.name(), rrs.type(),
                rrs.qualifier());
        assertTrue(byNameTypeAndQualifier != null, "could not lookup by name, type, and qualifier: " + rrs);
        assertEquals(byNameTypeAndQualifier, rrs);
    }

    protected void checkListByNameAndTypeConsistent(Zone zone, ResourceRecordSet<?> rrs) {
        List<ResourceRecordSet<?>> byNameAndType = ImmutableList.copyOf(allApi(zone)
                .iterateByNameAndType(rrs.name(), rrs.type()));
        assertFalse(byNameAndType.isEmpty(), "could not lookup by name and type: " + rrs);
        assertTrue(byNameAndType.contains(rrs), rrs + " not found in list by name and type: " + byNameAndType);
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            Iterator<ResourceRecordSet<?>> rrsIterator = allApi(zone).iterator();
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
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(allApi(zone).iterateByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(allApi(zone).iterateByName("ARGHH." + zone.name()).hasNext());
            break;
        }
    }

    @Test
    private void testListByNameAndTypeWhenEmpty() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(allApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT").hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameTypeAndGroupWhenAbsent() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertNull(allApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"));
            break;
        }
    }

    private void logRecordSummary() {
        for (Entry<String, AtomicLong> entry : recordTypeCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
    }

    private LoadingCache<String, AtomicLong> recordTypeCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<String, AtomicLong>() {
                public AtomicLong load(String key) throws Exception {
                    return new AtomicLong();
                }
            });
}
