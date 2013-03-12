package denominator;

import static com.google.common.collect.Iterators.size;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
    private void testGetByNameAndTypeWhenAbsent() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertEquals(rrsApi(zoneName).getByNameAndType("ARGHH." + zoneName, "TXT"), Optional.absent());
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
