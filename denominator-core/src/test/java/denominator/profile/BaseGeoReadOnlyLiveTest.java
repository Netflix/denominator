package denominator.profile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.size;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSets.toProfile;
import static java.lang.String.format;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.SkipException;
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
import denominator.model.profile.Geo;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseGeoReadOnlyLiveTest extends BaseProviderLiveTest {

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
            for (Iterator<ResourceRecordSet<?>> geoRRSIterator = geoApi(zoneName).list(); geoRRSIterator.hasNext();) {
                ResourceRecordSet<?> geoRRS = geoRRSIterator.next();
                checkGeoRRS(geoRRS);
                getAnonymousLogger().info(format("%s ::: geoRRS: %s", manager, geoRRS));
                recordTypeCounts.getUnchecked(geoRRS.getType()).addAndGet(geoRRS.size());
                geoRecordCounts.getUnchecked(toProfile(Geo.class).apply(geoRRS)).addAndGet(geoRRS.size());
                
                Iterator<ResourceRecordSet<?>> byNameAndType = geoApi(zoneName).listByNameAndType(geoRRS.getName(), geoRRS.getType());
                assertTrue(byNameAndType.hasNext(), "could not list by name and type: " + geoRRS);
                assertTrue(Iterators.elementsEqual(geoApi(zoneName).listByNameAndType(geoRRS.getName(), geoRRS.getType()), byNameAndType));
                
                Optional<ResourceRecordSet<?>> byNameTypeAndGroup = geoApi(zoneName)
                        .getByNameTypeAndGroup(geoRRS.getName(), geoRRS.getType(), toProfile(Geo.class).apply(geoRRS).getName());
                assertTrue(byNameTypeAndGroup.isPresent(), "could not lookup by name, type, and group: " + geoRRS);
                assertEquals(byNameTypeAndGroup.get(), geoRRS);
            }
        }
        logRecordSummary();
    }

    protected void checkGeoRRS(ResourceRecordSet<?> geoRRS) {
        assertFalse(geoRRS.getProfiles().isEmpty(), "Profile absent: " + geoRRS);
        Geo geo = toProfile(Geo.class).apply(geoRRS);
        checkNotNull(geo.getName(), "GroupName: Geo %s", geoRRS);
        assertTrue(!geo.getRegions().isEmpty(), "Regions empty on Geo: " + geoRRS);
        
        checkNotNull(geoRRS.getName(), "Name: ResourceRecordSet %s", geoRRS);
        checkNotNull(geoRRS.getType(), "Type: ResourceRecordSet %s", geoRRS);
        checkNotNull(geoRRS.getTTL(), "TTL: ResourceRecordSet %s", geoRRS);
        assertFalse(geoRRS.isEmpty(), "Values absent on ResourceRecordSet: " + geoRRS);
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            Iterator<ResourceRecordSet<?>> geoRRSIterator = geoApi(zoneName).list();
            if (!geoRRSIterator.hasNext())
                continue;
            ResourceRecordSet<?> geoRRSet = geoRRSIterator.next();
            String name = geoRRSet.getName();
            List<ResourceRecordSet<?>> withName = Lists.newArrayList();
            withName.add(geoRRSet);
            while (geoRRSIterator.hasNext()) {
                geoRRSet = geoRRSIterator.next();
                if (!name.equalsIgnoreCase(geoRRSet.getName()))
                        break;
                withName.add(geoRRSet);
            }
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(geoApi(zoneName).listByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertFalse(geoApi(zoneName).listByName("ARGHH." + zoneName).hasNext());
            break;
        }
    }

    @Test
    private void testListByNameAndTypeWhenNone() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertFalse(geoApi(zoneName).listByNameAndType("ARGHH." + zoneName, "TXT").hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameTypeAndGroupWhenAbsent() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertEquals(geoApi(zoneName).getByNameTypeAndGroup("ARGHH." + zoneName, "TXT", "Mars"), Optional.absent());
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

    protected GeoResourceRecordSetApi geoApi(String zoneName) {
        Optional<GeoResourceRecordSetApi> geoOption = manager.getApi().getGeoResourceRecordSetApiForZone(zoneName);
        if (!geoOption.isPresent())
            throw new SkipException("geo not available or not available in zone " + zoneName);
        return geoOption.get();
    }

}
