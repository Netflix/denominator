package denominator;

import static com.google.common.collect.Iterators.size;
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

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import denominator.model.ResourceRecordSet;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseReadOnlyLiveTest extends BaseProviderLiveTest {

    @Test
    public void testListZones() {
        skipIfNoCredentials();
        int zoneCount = size(zoneApi().list());
        getAnonymousLogger().info(format("%s ::: zones: %s", manager, zoneCount));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            for (Iterator<ResourceRecordSet<?>> rrsIterator = rrsApi(zoneName).list(); rrsIterator.hasNext();) {
                ResourceRecordSet<?> rrs = rrsIterator.next();
                recordTypeCounts.getUnchecked(rrs.getType()).addAndGet(rrs.size());
                checkRRS(rrs);
                Optional<ResourceRecordSet<?>> byNameAndType = rrsApi(zoneName).getByNameAndType(rrs.getName(), rrs.getType());
                assertTrue(byNameAndType.isPresent(), "could not lookup by name and type: " + rrs);
                assertEquals(rrsApi(zoneName).getByNameAndType(rrs.getName(), rrs.getType()).get(), rrs);
            }
        }
        logRecordSummary();
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            Iterator<ResourceRecordSet<?>> rrsIterator = rrsApi(zoneName).list();
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
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(rrsApi(zoneName).listByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertFalse(rrsApi(zoneName).listByName("ARGHH." + zoneName).hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameAndTypeWhenAbsent() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertEquals(rrsApi(zoneName).getByNameAndType("ARGHH." + zoneName, "TXT"), Optional.absent());
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
