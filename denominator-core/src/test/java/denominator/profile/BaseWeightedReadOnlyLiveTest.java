package denominator.profile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.profile.Weighted.asWeighted;
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
import denominator.model.profile.Weighted;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseWeightedReadOnlyLiveTest extends BaseProviderLiveTest {

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            for (ResourceRecordSet<?> weightedRRS : weightedApi(zone)) {
                assertTrue(weightedRRS.qualifier().isPresent(), "Weighted record sets should include a qualifier: " + weightedRRS);
                checkWeightedRRS(weightedRRS);

                Weighted weighted = asWeighted(weightedRRS);
                assertTrue(weightedApi(zone).supportedWeights().contains(weighted.weight()));
                assertTrue(manager.provider().profileToRecordTypes().get("weighted").contains(weightedRRS.type()));

                getAnonymousLogger().info(format("%s ::: weightedRRS: %s", manager, weightedRRS));
                recordTypeCounts.getUnchecked(weightedRRS.type()).addAndGet(weightedRRS.rdata().size());
                weightedRecordCounts.getUnchecked(asWeighted(weightedRRS)).addAndGet(weightedRRS.rdata().size());
                
                Iterator<ResourceRecordSet<?>> byNameAndType = weightedApi(zone).iterateByNameAndType(weightedRRS.name(), weightedRRS.type());
                assertTrue(byNameAndType.hasNext(), "could not list by name and type: " + weightedRRS);
                assertTrue(Iterators.elementsEqual(weightedApi(zone).iterateByNameAndType(weightedRRS.name(), weightedRRS.type()), byNameAndType));
                
                Optional<ResourceRecordSet<?>> byNameTypeAndQualifier = weightedApi(zone)
                        .getByNameTypeAndQualifier(weightedRRS.name(), weightedRRS.type(), weightedRRS.qualifier().get());
                assertTrue(byNameTypeAndQualifier.isPresent(), "could not lookup by name, type, and qualifier: " + weightedRRS);
                assertEquals(byNameTypeAndQualifier.get(), weightedRRS);
            }
        }
        logRecordSummary();
    }

    static void checkWeightedRRS(ResourceRecordSet<?> weightedRRS) {
        assertFalse(weightedRRS.profiles().isEmpty(), "Profile absent: " + weightedRRS);
        checkNotNull(weightedRRS.qualifier().orNull(), "Qualifier: ResourceRecordSet %s", weightedRRS);

        Weighted weighted = asWeighted(weightedRRS);
        assertTrue(weighted.weight() >= 0, "Weight negative on ResourceRecordSet: " + weightedRRS);
        
        checkNotNull(weightedRRS.name(), "Name: ResourceRecordSet %s", weightedRRS);
        checkNotNull(weightedRRS.type(), "Type: ResourceRecordSet %s", weightedRRS);
        checkNotNull(weightedRRS.ttl(), "TTL: ResourceRecordSet %s", weightedRRS);
        assertFalse(weightedRRS.rdata().isEmpty(), "Values absent on ResourceRecordSet: " + weightedRRS);
    }

    @Test
    private void testListByName() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            Iterator<ResourceRecordSet<?>> weightedRRSIterator = weightedApi(zone).iterator();
            if (!weightedRRSIterator.hasNext())
                continue;
            ResourceRecordSet<?> weightedRRSet = weightedRRSIterator.next();
            String name = weightedRRSet.name();
            List<ResourceRecordSet<?>> withName = Lists.newArrayList();
            withName.add(weightedRRSet);
            while (weightedRRSIterator.hasNext()) {
                weightedRRSet = weightedRRSIterator.next();
                if (!name.equalsIgnoreCase(weightedRRSet.name()))
                        break;
                withName.add(weightedRRSet);
            }
            List<ResourceRecordSet<?>> fromApi = Lists.newArrayList(weightedApi(zone).iterateByName(name));
            assertEquals(usingToString().immutableSortedCopy(fromApi), usingToString().immutableSortedCopy(withName));
            break;
        }
    }

    @Test
    private void testListByNameWhenNotFound() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(weightedApi(zone).iterateByName("ARGHH." + zone.name()).hasNext());
            break;
        }
    }

    @Test
    private void testListByNameAndTypeWhenNone() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertFalse(weightedApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT").hasNext());
            break;
        }
    }

    @Test
    private void testGetByNameTypeAndQualifierWhenAbsent() {
        skipIfNoCredentials();
        for (Zone zone : zones()) {
            assertEquals(weightedApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"), Optional.absent());
            break;
        }
    }
 
    private void logRecordSummary() {
        for (Entry<String, AtomicLong> entry : recordTypeCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
        for (Entry<Weighted, AtomicLong> entry : weightedRecordCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
    }

    private LoadingCache<String, AtomicLong> recordTypeCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<String, AtomicLong>() {
                public AtomicLong load(String key) throws Exception {
                    return new AtomicLong();
                }
            });

    private LoadingCache<Weighted, AtomicLong> weightedRecordCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<Weighted, AtomicLong>() {
                public AtomicLong load(Weighted key) throws Exception {
                    return new AtomicLong();
                }
            });
}
