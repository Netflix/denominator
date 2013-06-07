package denominator;

import static com.google.common.collect.Ordering.usingToString;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
            for (Iterator<ResourceRecordSet<?>> rrsIterator = roApi(zone).iterator(); rrsIterator.hasNext();) {
                ResourceRecordSet<?> rrs = rrsIterator.next();
                recordTypeCounts.getUnchecked(rrs.getType()).addAndGet(rrs.size());
                checkRRS(rrs);
                checkListByNameAndTypeConsistent(zone, rrs);
            }
        }
        logRecordSummary();
    }

    protected void checkListByNameAndTypeConsistent(Zone zone, ResourceRecordSet<?> rrs) {
        List<ResourceRecordSet<?>> byNameAndType = ImmutableList.copyOf(roApi(zone)
                .listByNameAndType(rrs.getName(), rrs.getType()));
        assertFalse(byNameAndType.isEmpty(), "could not lookup by name and type: " + rrs);
        assertTrue(byNameAndType.contains(rrs), rrs + " not found in list by name and type: " + byNameAndType);
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            Iterator<ResourceRecordSet<?>> rrsIterator = roApi(zone).iterator();
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
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(roApi(zone).listByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(roApi(zone).listByName("ARGHH." + zone.name()).hasNext());
            break;
        }
    }

    @Test
    private void testListByNameAndTypeWhenEmpty() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(roApi(zone).listByNameAndType("ARGHH." + zone.name(), "TXT").hasNext());
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

    private AllProfileResourceRecordSetApi roApi(Zone zone) {
        return manager.api().recordSetsInZone(zone.idOrName());
    }
}
