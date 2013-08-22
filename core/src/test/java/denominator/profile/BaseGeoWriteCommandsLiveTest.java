package denominator.profile;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.profile.BaseGeoReadOnlyLiveTest.checkGeoRRS;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.BaseProviderLiveTest;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseGeoWriteCommandsLiveTest extends BaseProviderLiveTest {

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(manager.provider().profileToRecordTypes().get("geo")));
    }

    String qualifier1 = "US-East";
    String qualifier2 = "US-West";

    /**
     * value at index 1 in the rrs is used for qualifier2
     */
    @DataProvider(name = "geoRecords")
    public Object[][] geoRecords() {
        ImmutableList<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(stockRRSets().values());
        Object[][] data = new Object[rrsets.size()][1];
        for (int i = 0; i < rrsets.size(); i++)
            data[i][0] = rrsets.get(i);
        return data;
    }
    
    @Test(dataProvider = "geoRecords")
    private void createNewRRSWithAllProfileApi(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();
        
        Multimap<String, String> regions = multimap(geoApi(skipIfNoMutableZone()).supportedRegions());

        Multimap<String, String> allButOne = filterValues(regions, not(equalTo(getLast(regions.values()))));
        Multimap<String, String> onlyOne = filterValues(regions, equalTo(getLast(regions.values())));

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            skipIfRRSetExists(zone, recordSet.name(), recordSet.type(), qualifier);

            Geo territories = Geo.create(i == 0 ? allButOne.asMap() : onlyOne.asMap());

            allApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.name())
                                              .type(recordSet.type())
                                              .ttl(1800)
                                              .qualifier(qualifier)
                                              .geo(territories)
                                              .add(recordSet.records().get(i)).build());
    
            ResourceRecordSet<?> rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier);
    
            assertPresent(rrs, zone, recordSet.name(), recordSet.type(), qualifier);
    
            checkGeoRRS(rrs);
            assertEquals(rrs.name(), recordSet.name());
            assertEquals(rrs.ttl(), Integer.valueOf(1800));
            assertEquals(rrs.type(), recordSet.type());
            assertEquals(rrs.qualifier(), qualifier);
            assertEquals(json.toJson(rrs.geo()), json.toJson(territories));
            assertEquals(rrs.records().size(), 1);
            assertEquals(rrs.records().get(0), recordSet.records().get(i++));
        }
    }

    @Test(dependsOnMethods = "createNewRRSWithAllProfileApi", dataProvider = "geoRecords")
    private void yankTerritoryIntoAnotherRRSet(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        ResourceRecordSet<?> rrs1 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        Multimap<String, String> regionsFrom1 = multimap(rrs1.geo().regions());
        Multimap<String, String> toYank = filterValues(regionsFrom1, equalTo(getLast(regionsFrom1.values())));

        ResourceRecordSet<?> rrs2 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        Multimap<String, String> plus1 = ImmutableMultimap.<String, String> builder()//
                .putAll(multimap(rrs2.geo().regions()))//
                .putAll(toYank).build();

        geoApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                          .name(recordSet.name())
                                          .type(recordSet.type())
                                          .qualifier(qualifier2)
                                          .ttl(rrs2.ttl())
                                          .geo(Geo.create(plus1.asMap()))
                                          .addAll(rrs2.records()).build());

        rrs1 = geoApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier1);

        Multimap<String, String> minus1 = ImmutableMultimap.copyOf(
                filterValues(regionsFrom1, not(equalTo(getLast(regionsFrom1.values())))));

        assertEquals(multimap(rrs1.geo().regions()), minus1);

        rrs2 = geoApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier2);

        assertEquals(multimap(rrs2.geo().regions()), plus1);
    }

    @Test(dependsOnMethods = "yankTerritoryIntoAnotherRRSet", dataProvider = "geoRecords")
    private void changeTTLWithAllProfileApi(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            ResourceRecordSet<?> rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier);

            int ttl = Optional.fromNullable(rrs.ttl()).or(60000) + 60000;
            Geo oldGeo = rrs.geo();

            allApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.name())
                                              .type(recordSet.type())
                                              .ttl(ttl)
                                              .qualifier(qualifier)
                                              .geo(oldGeo)
                                              .add(recordSet.records().get(i)).build());

            rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier);

            checkGeoRRS(rrs);
            assertEquals(rrs.name(), recordSet.name());
            assertEquals(rrs.ttl(), Integer.valueOf(ttl));
            assertEquals(rrs.type(), recordSet.type());
            assertEquals(rrs.qualifier(), qualifier);
            assertEquals(rrs.geo().regions(), oldGeo.regions());
            assertEquals(rrs.records().size(), 1);
            assertEquals(rrs.records().get(0), recordSet.records().get(i++));
        }
    }

    @Test(dependsOnMethods = "changeTTLWithAllProfileApi", dataProvider = "geoRecords")
    private void deleteOneQualifierDoesntAffectOther(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        geoApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        ResourceRecordSet<?> rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                qualifier1);

        assertNull(rrs, format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier1, zone));

        rrs = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertPresent(rrs, zone, recordSet.name(), recordSet.type(), qualifier2);
        
        // safe to call twice
        geoApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        // clean up the other one
        allApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());

        rrs = allApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertNull(rrs, format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier2, zone));
    }
}
