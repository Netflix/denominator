package denominator.profile;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.toProfile;
import static org.testng.Assert.assertEquals;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.BaseProviderLiveTest;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseGeoWriteCommandsLiveTest extends BaseProviderLiveTest {

    @Test
    private void applyRegionsToNameTypeAndGroup() {
        skipIfNoCredentials();
        ResourceRecordSet<?> existing = skipIfNoMutableRRSet();
        
        Geo existingGeo = toProfile(Geo.class).apply(existing);
        
        String last = getLast(existingGeo.getRegions().values());
        
        Multimap<String, String> regions = filterValues(existingGeo.getRegions(), not(equalTo(last)));

        geoApi().applyRegionsToNameTypeAndGroup(regions, existing.getName(), existing.getType(), mutableGeoRRSet.group);

        ResourceRecordSet<?> rrs = 
                geoApi().getByNameTypeAndGroup(existing.getName(), existing.getType(), mutableGeoRRSet.group).get();

        checkRRS(rrs);
        assertEquals(rrs.getName(), existing.getName());
        assertEquals(rrs.getType(), existing.getType());
        assertEquals(rrs.getTTL(), existing.getTTL());
        assertEquals(ImmutableList.copyOf(rrs), ImmutableList.copyOf(existing));
        assertEquals(toProfile(Geo.class).apply(rrs).getRegions(), ImmutableMultimap.copyOf(regions));

        // reset back
        geoApi().applyRegionsToNameTypeAndGroup(
                existingGeo.getRegions(), existing.getName(), existing.getType(), mutableGeoRRSet.group);
    }

    @Test
    private void applyTTLToNameTypeAndGroup() {
        skipIfNoCredentials();
        ResourceRecordSet<?> existing = skipIfNoMutableRRSet();
        int ttl = existing.getTTL().or(300) + 300;

        geoApi().applyTTLToNameTypeAndGroup(ttl, existing.getName(), existing.getType(), mutableGeoRRSet.group);

        ResourceRecordSet<?> rrs = 
                geoApi().getByNameTypeAndGroup(existing.getName(), existing.getType(), mutableGeoRRSet.group).get();

        checkRRS(rrs);
        assertEquals(rrs.getName(), existing.getName());
        assertEquals(rrs.getType(), existing.getType());
        assertEquals(rrs.getTTL().get(), Integer.valueOf(ttl));
        assertEquals(ImmutableList.copyOf(rrs), ImmutableList.copyOf(existing));
        assertEquals(rrs.getProfiles(), existing.getProfiles());
        
        // reset back
        geoApi().applyTTLToNameTypeAndGroup(
                existing.getTTL().or(300), existing.getName(), existing.getType(), mutableGeoRRSet.group);
    }

    protected ResourceRecordSet<?> skipIfNoMutableRRSet() {
        if (mutableGeoRRSet == null)
            throw new SkipException("mutable rrset not configured");
        Optional<GeoResourceRecordSetApi> option = 
                manager.getApi().getGeoResourceRecordSetApiForZone(mutableGeoRRSet.zone);
        if (!option.isPresent())
            throw new SkipException("geo not supported or not supported on zone " + mutableGeoRRSet.zone);
        Optional<ResourceRecordSet<?>> rrset = 
                geoApi().getByNameTypeAndGroup(mutableGeoRRSet.name, mutableGeoRRSet.type, mutableGeoRRSet.group);
        if (!rrset.isPresent())
            throw new SkipException("rrset not found " + mutableGeoRRSet);
        if (!profileContainsType(Geo.class).apply(rrset.get()))
            throw new SkipException("rrset does not have geo profile " + rrset.get());
        return rrset.get();
    }

    public static class MutableGeoRRSet {
        public String zone;
        public String name;
        public String type;
        public String group;
    }

    protected MutableGeoRRSet mutableGeoRRSet;

    protected GeoResourceRecordSetApi geoApi() {
        return manager.getApi().getGeoResourceRecordSetApiForZone(mutableGeoRRSet.zone).get();
    }
}
