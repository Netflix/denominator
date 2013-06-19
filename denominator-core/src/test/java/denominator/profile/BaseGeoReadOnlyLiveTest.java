package denominator.profile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.profile.Geo.asGeo;
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
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import denominator.BaseProviderLiveTest;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseGeoReadOnlyLiveTest extends BaseProviderLiveTest {

    @Test
    public void testListZones() {
        skipIfNoCredentials();
        int zoneCount = size(zones().iterator());
        getAnonymousLogger().info(format("%s ::: zones: %s", manager, zoneCount));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            for (ResourceRecordSet<?> geoRRS : geoApi(zone)) {
                assertTrue(geoRRS.qualifier().isPresent(), "Geo record sets should include a qualifier: " + geoRRS);
                checkGeoRRS(geoRRS);
                assertTrue(manager.provider().profileToRecordTypes().get("geo").contains(geoRRS.type()));

                getAnonymousLogger().info(format("%s ::: geoRRS: %s", manager, geoRRS));
                recordTypeCounts.getUnchecked(geoRRS.type()).addAndGet(geoRRS.rdata().size());
                geoRecordCounts.getUnchecked(asGeo(geoRRS)).addAndGet(geoRRS.rdata().size());
                
                Iterator<ResourceRecordSet<?>> byNameAndType = geoApi(zone).iterateByNameAndType(geoRRS.name(), geoRRS.type());
                assertTrue(byNameAndType.hasNext(), "could not list by name and type: " + geoRRS);
                assertTrue(Iterators.elementsEqual(geoApi(zone).iterateByNameAndType(geoRRS.name(), geoRRS.type()), byNameAndType));
                
                Optional<ResourceRecordSet<?>> byNameTypeAndQualifier = geoApi(zone)
                        .getByNameTypeAndQualifier(geoRRS.name(), geoRRS.type(), geoRRS.qualifier().get());
                assertTrue(byNameTypeAndQualifier.isPresent(), "could not lookup by name, type, and qualifier: " + geoRRS);
                assertEquals(byNameTypeAndQualifier.get(), geoRRS);
            }
        }
        logRecordSummary();
    }

    static void checkGeoRRS(ResourceRecordSet<?> geoRRS) {
        assertFalse(geoRRS.profiles().isEmpty(), "Profile absent: " + geoRRS);
        checkNotNull(geoRRS.qualifier().orNull(), "Qualifier: ResourceRecordSet %s", geoRRS);

        Geo geo = asGeo(geoRRS);
        assertTrue(!geo.regions().isEmpty(), "Regions empty on Geo: " + geoRRS);
        checkNotNull(geoRRS.name(), "Name: ResourceRecordSet %s", geoRRS);
        checkNotNull(geoRRS.type(), "Type: ResourceRecordSet %s", geoRRS);
        checkNotNull(geoRRS.ttl(), "TTL: ResourceRecordSet %s", geoRRS);
        assertFalse(geoRRS.rdata().isEmpty(), "Values absent on ResourceRecordSet: " + geoRRS);
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            Iterator<ResourceRecordSet<?>> geoRRSIterator = geoApi(zone).iterator();
            if (!geoRRSIterator.hasNext())
                continue;
            ResourceRecordSet<?> geoRRSet = geoRRSIterator.next();
            String name = geoRRSet.name();
            List<ResourceRecordSet<?>> withName = Lists.newArrayList();
            withName.add(geoRRSet);
            while (geoRRSIterator.hasNext()) {
                geoRRSet = geoRRSIterator.next();
                if (!name.equalsIgnoreCase(geoRRSet.name()))
                        break;
                withName.add(geoRRSet);
            }
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(geoApi(zone).iterateByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(geoApi(zone).iterateByName("ARGHH." + zone.name()).hasNext());
            break;
        }
    }

    @Test
    private void testListByNameAndTypeWhenNone() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(geoApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT").hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameTypeAndQualifierWhenAbsent() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertEquals(geoApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"), Optional.absent());
            break;
        }
    }
 
    private void logRecordSummary() {
        for (Entry<String, AtomicLong> entry : recordTypeCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
        for (Entry<Geo, AtomicLong> entry : geoRecordCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
    }

    private LoadingCache<String, AtomicLong> recordTypeCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<String, AtomicLong>() {
                public AtomicLong load(String key) throws Exception {
                    return new AtomicLong();
                }
            });

    private LoadingCache<Geo, AtomicLong> geoRecordCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<Geo, AtomicLong>() {
                public AtomicLong load(Geo key) throws Exception {
                    return new AtomicLong();
                }
            });
}
