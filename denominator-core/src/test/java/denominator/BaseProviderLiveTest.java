package denominator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.size;
import static com.google.common.io.Closeables.close;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import denominator.model.ResourceRecordSet;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseProviderLiveTest {

    protected DNSApiManager manager;

    @Test
    public void testListZones() {
        skipIfNoCredentials();
        int zoneCount = size(manager.getApi().getZoneApi().list());
        getAnonymousLogger().info(format("%s ::: zones: %s", manager, zoneCount));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Iterator<String> zone = manager.getApi().getZoneApi().list(); zone.hasNext();) {
            ResourceRecordSetApi api = manager.getApi().getResourceRecordSetApiForZone(zone.next());
            for (Iterator<ResourceRecordSet<?>> rrsIterator = api.list(); rrsIterator.hasNext();) {
                ResourceRecordSet<?> rrs = rrsIterator.next();
                recordTypeCounts.getUnchecked(rrs.getType()).addAndGet(rrs.size());
                checkRRS(rrs);
            }
        }
        logRecordSummary();
    }

    private void logRecordSummary() {
        for (Entry<String, AtomicLong> entry : recordTypeCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
    }

    private void checkRRS(ResourceRecordSet<?> rrs) {
        checkNotNull(rrs.getName(), "Name: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.getType(), "Type: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.getTTL(), "TTL: ResourceRecordSet %s", rrs);
        assertTrue(!rrs.isEmpty(), "Values absent on ResourceRecordSet: " + rrs);
    }

    @AfterClass
    private void tearDown() throws IOException {
        close(manager, true);
    }

    protected void skipIfNoCredentials() {
        if (manager == null)
            throw new SkipException("manager not configured");
    }

    LoadingCache<String, AtomicLong> recordTypeCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<String, AtomicLong>() {
                public AtomicLong load(String key) throws Exception {
                    return new AtomicLong();
                }
            });
}
