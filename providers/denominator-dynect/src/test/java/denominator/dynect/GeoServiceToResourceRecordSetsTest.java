package denominator.dynect;

import static com.google.common.collect.Multimaps.index;
import static org.jclouds.dynect.v3.domain.rdata.AData.a;
import static org.jclouds.dynect.v3.domain.rdata.CNAMEData.cname;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.jclouds.dynect.v3.domain.GeoRegionGroup;
import org.jclouds.dynect.v3.domain.GeoService;
import org.jclouds.dynect.v3.domain.Node;
import org.jclouds.dynect.v3.domain.RecordSet;
import org.jclouds.dynect.v3.domain.RecordSet.Value;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;

public class GeoServiceToResourceRecordSetsTest {

    static final GeoService differentCountries = GeoService.builder()
            .name("CCS")
            .active(true)
            .ttl(30)
            .addNode(Node.create("denominator.io", "srv.denominator.io"))
            .addGroup(GeoRegionGroup.builder()
                        .name("Everywhere Else")
                        .countries(ImmutableList.of("11", "16", "12", "17", "15", "14"))
                        .addRecordSet(RecordSet.builder()
                            .ttl(300)
                            .type("CNAME")
                            .add(Value.builder()
                                      .rdata(cname("srv-000000001.us-east-1.elb.amazonaws.com."))
                                      .build()).build()).build())
            .addGroup(GeoRegionGroup.builder()
                        .name("Europe")
                        .countries(ImmutableList.of("13"))
                        .addRecordSet(RecordSet.builder()
                            .ttl(300)
                            .type("CNAME")
                            .add(Value.builder()
                                      .rdata(cname("srv-000000001.eu-west-1.elb.amazonaws.com."))
                                      .build()).build()).build())
            .addGroup(GeoRegionGroup.builder()
                        .name("Fallback")
                        .countries(ImmutableList.of("@!", "@@"))
                        .addRecordSet(RecordSet.builder()
                            .ttl(60)
                            .type("CNAME")
                            .add(Value.builder()
                                      .rdata(cname("srv-000000002.us-east-1.elb.amazonaws.com."))
                                      .build()).build()).build()).build();

    @Test
    public void testDifferentProfileBecomeDifferentRRSets() {
        assertEquals(
                geoToRRSets.apply(differentCountries),
                ImmutableList.<ResourceRecordSet<CNAMEData>> builder()
                             .add(ResourceRecordSet.<CNAMEData> builder()
                                                   .name("srv.denominator.io")
                                                   .type("CNAME")
                                                   .ttl(300)
                                                   .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
                                                   .addProfile(Geo.create("Everywhere Else",
                                                                       ImmutableMultimap.<String, String> builder()
                                                                                        .put("11", "11")
                                                                                        .put("16", "16")
                                                                                        .put("12", "12")
                                                                                        .put("17", "17")
                                                                                        .put("15", "15")
                                                                                        .put("14", "14").build()))                                                   
                                                   .build())
                             .add(ResourceRecordSet.<CNAMEData> builder()
                                                   .name("srv.denominator.io")
                                                   .type("CNAME")
                                                   .ttl(300)
                                                   .add(CNAMEData.create("srv-000000001.eu-west-1.elb.amazonaws.com."))
                                                   .addProfile(Geo.create("Europe", ImmutableMultimap.of("13", "13")))
                                                   .build())
                             .add(ResourceRecordSet.<CNAMEData> builder()
                                                   .name("srv.denominator.io")
                                                   .type("CNAME")
                                                   .ttl(60)
                                                   .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
                                                   .addProfile(Geo.create("Fallback",
                                                                       ImmutableMultimap.<String, String> builder()
                                                                                        .put("@!", "@!")
                                                                                        .put("@@", "@@").build()))
                                                   .build()).build());
    }

    @Test
    public void testGroupFilterRetainsExpectedGroup() {
        assertEquals(
                geoToRRSets.group("Everywhere Else").apply(differentCountries),
                ImmutableList.<ResourceRecordSet<CNAMEData>> builder()
                             .add(ResourceRecordSet.<CNAMEData> builder()
                                                   .name("srv.denominator.io")
                                                   .type("CNAME")
                                                   .ttl(300)
                                                   .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
                                                   .addProfile(Geo.create("Everywhere Else",
                                                                       ImmutableMultimap.<String, String> builder()
                                                                                        .put("11", "11")
                                                                                        .put("16", "16")
                                                                                        .put("12", "12")
                                                                                        .put("17", "17")
                                                                                        .put("15", "15")
                                                                                        .put("14", "14").build()))                                                   
                                                   .build()).build());
    }

    static final GeoService multipleNodes = GeoService.builder()
            .name("CCS")
            .active(true)
            .ttl(30)
            .addNode(Node.create("denominator.io", "srv1.denominator.io"))
            .addNode(Node.create("denominator.io", "srv2.denominator.io"))
            .addGroup(GeoRegionGroup.builder()
                        .name("Default")
                        .countries(ImmutableList.of("@!", "@@"))
                        .addRecordSet(RecordSet.builder()
                            .ttl(60)
                            .type("CNAME")
                            .add(Value.builder()
                                      .rdata(cname("srv-000000002.us-east-1.elb.amazonaws.com."))
                                      .build()).build()).build()).build();

    @Test
    public void testMultipleNodesBecomeDifferentRRSets() {
        Builder<CNAMEData> builder = ResourceRecordSet.<CNAMEData> builder()
                        .type("CNAME")
                        .ttl(60)
                        .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
                        .addProfile(Geo.create("Default", ImmutableMultimap.of("@!", "@!", "@@", "@@")));
        
        assertEquals(geoToRRSets.apply(multipleNodes), 
                ImmutableList.of(builder.name("srv1.denominator.io").build(),
                                 builder.name("srv2.denominator.io").build()));
    }

    static final GeoService mixedTypesSameGroup = GeoService.builder()
            .name("CCS")
            .active(true)
            .ttl(30)
            .addNode(Node.create("denominator.io", "srv.denominator.io"))
            .addGroup(GeoRegionGroup.builder()
                        .name("Default")
                        .countries(ImmutableList.of("@!", "@@"))
                        .addRecordSet(RecordSet.builder()
                            .ttl(60)
                            .type("A")
                            .add(Value.builder()
                                      .rdata(a("192.0.2.1"))
                                      .build()).build())            
                        .addRecordSet(RecordSet.builder()
                            .ttl(60)
                            .type("CNAME")
                            .add(Value.builder()
                                      .rdata(cname("srv-000000002.us-east-1.elb.amazonaws.com."))
                                      .build()).build()).build()).build();

    @Test
    public void testTypeFilterRetainsExpectedType() {
        Geo geo = Geo.create("Default", ImmutableMultimap.of("@!", "@!", "@@", "@@"));

        assertEquals(geoToRRSets.type("A").apply(mixedTypesSameGroup), 
                ImmutableList.of(ResourceRecordSet.builder()
                                                  .name("srv.denominator.io")
                                                  .type("A")
                                                  .ttl(60)
                                                  .addProfile(geo)
                                                  .add(AData.create("192.0.2.1")).build()));
    }

    @Test
    public void testMixedTypesInSameGroupBecomeDifferentRRSets() {
        Geo geo = Geo.create("Default", ImmutableMultimap.of("@!", "@!", "@@", "@@"));

        assertEquals(geoToRRSets.apply(mixedTypesSameGroup), 
                ImmutableList.of(ResourceRecordSet.builder()
                                                  .name("srv.denominator.io")
                                                  .type("A")
                                                  .ttl(60)
                                                  .addProfile(geo)
                                                  .add(AData.create("192.0.2.1")).build(),
                                 ResourceRecordSet.builder()
                                                  .name("srv.denominator.io")
                                                  .type("CNAME")
                                                  .ttl(60)
                                                  .addProfile(geo)
                                                  .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
                                                  .build()));
    }

    private static final Function<List<String>, Multimap<String, String>> countryIndexer = new Function<List<String>, Multimap<String, String>>() {
        public Multimap<String, String> apply(List<String> input) {
            return index(input, Functions.<String> identity());
        }
    };

    private static final GeoServiceToResourceRecordSets geoToRRSets = new GeoServiceToResourceRecordSets(countryIndexer);
}
