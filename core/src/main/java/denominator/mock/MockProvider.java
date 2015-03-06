package denominator.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.NothingToClose;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.SOAData;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.profile.WeightedResourceRecordSetApi;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cert;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.mx;
import static denominator.model.ResourceRecordSets.naptr;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.srv;
import static denominator.model.ResourceRecordSets.sshfp;
import static denominator.model.ResourceRecordSets.txt;

/**
 * in-memory {@code Provider}, used for testing.
 */
public class MockProvider extends BasicProvider {

  private final String url;

  public MockProvider() {
    this(null);
  }

  /**
   * @param url if empty or null use default
   */
  public MockProvider(String url) {
    this.url = url == null || url.isEmpty() ? "mem:mock" : url;
  }

  @Override
  public String url() {
    return url;
  }

  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>> profileToRecordTypes = super.profileToRecordTypes();
    profileToRecordTypes.put("geo", Arrays
        .asList("A", "AAAA", "CNAME", "NS", "PTR", "SPF", "TXT", "MX", "SRV", "DS", "CERT", "NAPTR",
                "SSHFP", "LOC", "TLSA"));
    profileToRecordTypes.put("weighted", Arrays
        .asList("A", "AAAA", "CNAME", "NS", "PTR", "SPF", "TXT", "MX", "SRV", "DS", "CERT", "NAPTR",
                "SSHFP", "LOC", "TLSA"));
    return profileToRecordTypes;
  }

  // normally, we'd set package private visibility, but this module is helpful
  // in tests, so we mark it public
  @dagger.Module(injects = DNSApiManager.class, complete = false, // denominator.Provider
      includes = NothingToClose.class)
  public static final class Module {

    @Provides
    CheckConnection alwaysOK() {
      return new CheckConnection() {
        public boolean ok() {
          return true;
        }
      };
    }

    @Provides
    ZoneApi provideZoneApi(MockZoneApi in) {
      return in;
    }

    @Provides
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
        MockResourceRecordSetApi.Factory in) {
      return in;
    }

    @Provides
    AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory(
        MockAllProfileResourceRecordSetApi.Factory in) {
      return in;
    }

    @Provides
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(
        MockGeoResourceRecordSetApi.Factory in) {
      return in;
    }

    // unbound wildcards are not currently injectable in dagger
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Provides
    @Singleton
    Map<Zone, SortedSet<ResourceRecordSet>> provideRecords() {
      String idOrName = "denominator.io.";
      Comparator<ResourceRecordSet<?>> toStringComparator = new Comparator<ResourceRecordSet<?>>() {
        @Override
        public int compare(ResourceRecordSet<?> arg0, ResourceRecordSet<?> arg1) {
          return arg0.toString().compareTo(arg1.toString());
        }
      };
      SortedSet<ResourceRecordSet<?>>
          records =
          new ConcurrentSkipListSet<ResourceRecordSet<?>>(toStringComparator);
      records.add(ResourceRecordSet
                      .builder()
                      .type("SOA")
                      .name(idOrName)
                      .ttl(3600)
                      .add(SOAData.builder().mname("ns1." + idOrName).rname("admin." + idOrName)
                               .serial(1).refresh(3600)
                               .retry(600).expire(604800).minimum(60).build()).build());
      records.add(ns(idOrName, 86400, "ns1." + idOrName));
      records.add(mx(idOrName, 86400, "1 mx1.denominator.io."));
      records.add(spf(idOrName, 86400, "v=spf1 a mx -all"));
      records.add(txt(idOrName, 86400, "blah"));
      records.add(a("www1." + idOrName, 3600, Arrays.asList("192.0.2.1", "192.0.2.2")));
      records.add(a("www2." + idOrName, 3600, "198.51.100.1"));
      records.add(cname("www." + idOrName, 3600, "www1." + idOrName));
      Map<String, Collection<String>> alazona = new LinkedHashMap<String, Collection<String>>();
      alazona.put("United States (US)", Arrays.asList("Alaska", "Arizona"));
      records.add(ResourceRecordSet.builder().name("www2.geo.denominator.io.")
                      .type("A")
                      .qualifier("alazona").ttl(300).add(AData.create("192.0.2.1"))
                      .geo(Geo.create(alazona))
                      .build());
      records.add(ResourceRecordSet.builder().name("www.geo.denominator.io.")
                      .type("CNAME")
                      .qualifier("alazona").ttl(300).add(CNAMEData.create("a.denominator.io."))
                      .geo(Geo.create(alazona)).build());
      Map<String, Collection<String>> columbador = new LinkedHashMap<String, Collection<String>>();
      columbador.put("South America", Arrays.asList("Colombia", "Ecuador"));
      records.add(ResourceRecordSet.builder().name("www.geo.denominator.io.")
                      .type("CNAME")
                      .qualifier("columbador").ttl(86400).add(CNAMEData.create("b.denominator.io."))
                      .geo(Geo.create(columbador)).build());
      Map<String, Collection<String>> antarctica = new LinkedHashMap<String, Collection<String>>();
      antarctica.put("Antarctica",
                     Arrays.asList("Bouvet Island", "French Southern Territories", "Antarctica"));
      records.add(ResourceRecordSet.builder().name("www.geo.denominator.io.")
                      .type("CNAME")
                      .qualifier("antarctica").ttl(0).add(CNAMEData.create("c.denominator.io."))
                      .geo(Geo.create(antarctica)).build());
      records.add(
          ResourceRecordSet.builder().name("www2.weighted.denominator.io.")
              .type("A").qualifier("US-West").ttl(0).add(AData.create("192.0.2.1"))
              .weighted(Weighted.create(0)).build());
      records
          .add(ResourceRecordSet.builder().name("www.weighted.denominator.io.")
                   .type("CNAME").qualifier("US-West").ttl(0)
                   .add(CNAMEData.create("a.denominator.io."))
                   .weighted(Weighted.create(1)).build());
      records
          .add(ResourceRecordSet.builder().name("www.weighted.denominator.io.")
                   .type("CNAME").qualifier("US-East").ttl(0)
                   .add(CNAMEData.create("b.denominator.io."))
                   .weighted(Weighted.create(1)).build());
      records
          .add(ResourceRecordSet.builder().name("www.weighted.denominator.io.")
                   .type("CNAME").qualifier("EU-West").ttl(0)
                   .add(CNAMEData.create("c.denominator.io."))
                   .weighted(Weighted.create(1)).build());
      records.add(ns("subdomain." + idOrName, 3600, "ns1.denominator.io."));
      records.add(naptr("phone." + idOrName, 3600,
                        "1 1 U E2U+sip !^.*$!sip:customer-service@example.com! ."));
      records.add(ptr("ptr." + idOrName, 3600, "www.denominator.io."));
      records.add(srv("server1." + idOrName, 3600, "0 1 80 www.denominator.io."));
      records.add(cert("server1." + idOrName, 3600, "12345 1 1 B33F"));
      records.add(sshfp("server1." + idOrName, 3600, "1 1 B33F"));
      Map<Zone, SortedSet<ResourceRecordSet<?>>>
          zoneToRecords =
          new LinkedHashMap<Zone, SortedSet<ResourceRecordSet<?>>>();
      zoneToRecords.put(Zone.create(idOrName), records);
      return Map.class.cast(zoneToRecords);
    }

    @Provides
    @Singleton
    @Named("geo")
    Map<String, Collection<String>> provideRegions() {
      Map<String, Collection<String>> regions = new LinkedHashMap<String, Collection<String>>();
      regions.put("Anonymous Proxy (A1)", Collections.singleton("Anonymous Proxy"));
      regions.put("Satellite Provider (A2)", Collections.singleton("Satellite Provider"));
      regions
          .put("Unknown / Uncategorized IPs", Collections.singleton("Unknown / Uncategorized IPs"));
      regions.put("United States (US)", Arrays.asList("Alabama", "Alaska", "Arizona", "Arkansas",
                                                      "Armed Forces Americas",
                                                      "Armed Forces Europe, Middle East, and Canada",
                                                      "Armed Forces Pacific",
                                                      "California", "Colorado", "Connecticut",
                                                      "Delaware", "District of Columbia", "Florida",
                                                      "Georgia",
                                                      "Hawaii", "Idaho", "Illinois", "Indiana",
                                                      "Iowa", "Kansas", "Kentucky", "Louisiana",
                                                      "Maine",
                                                      "Maryland", "Massachusetts", "Michigan",
                                                      "Minnesota", "Mississippi", "Missouri",
                                                      "Montana",
                                                      "Nebraska", "Nevada", "New Hampshire",
                                                      "New Jersey", "New Mexico", "New York",
                                                      "North Carolina",
                                                      "North Dakota", "Ohio", "Oklahoma", "Oregon",
                                                      "Pennsylvania", "Rhode Island",
                                                      "South Carolina",
                                                      "South Dakota", "Tennessee", "Texas",
                                                      "Undefined United States",
                                                      "United States Minor Outlying Islands",
                                                      "Utah", "Vermont", "Virginia", "Washington",
                                                      "West Virginia", "Wisconsin", "Wyoming"));
      regions.put("Mexico", Collections.singleton("Mexico"));
      regions
          .put("Canada (CA)", Arrays.asList("Alberta", "British Columbia", "Greenland", "Manitoba",
                                            "New Brunswick", "Newfoundland and Labrador",
                                            "Northwest Territories", "Nova Scotia", "Nunavut",
                                            "Ontario", "Prince Edward Island", "Quebec",
                                            "Saint Pierre and Miquelon", "Saskatchewan",
                                            "Undefined Canada", "Yukon"));
      regions
          .put("The Caribbean", Arrays.asList("Anguilla", "Antigua and Barbuda", "Aruba", "Bahamas",
                                              "Barbados", "Bermuda", "British Virgin Islands",
                                              "Cayman Islands", "Cuba", "Dominica",
                                              "Dominican Republic", "Grenada", "Guadeloupe",
                                              "Haiti", "Jamaica", "Martinique", "Montserrat",
                                              "Netherlands Antilles", "Puerto Rico",
                                              "Saint Barthelemy", "Saint Martin",
                                              "Saint Vincent and the Grenadines",
                                              "St. Kitts and Nevis", "St. Lucia",
                                              "Trinidad and Tobago",
                                              "Turks and Caicos Islands", "U.S. Virgin Islands"));
      regions
          .put("Central America", Arrays.asList("Belize", "Costa Rica", "El Salvador", "Guatemala",
                                                "Honduras", "Nicaragua", "Panama",
                                                "Undefined Central America"));
      regions
          .put("South America", Arrays.asList("Argentina", "Bolivia", "Brazil", "Chile", "Colombia",
                                              "Ecuador", "Falkland Islands", "French Guiana",
                                              "Guyana", "Paraguay", "Peru",
                                              "South Georgia and the South Sandwich Islands",
                                              "Suriname", "Undefined South America", "Uruguay",
                                              "Venezuela, Bolivarian Republic of"));
      regions
          .put("Europe", Arrays.asList("Aland Islands", "Albania", "Andorra", "Armenia", "Austria",
                                       "Azerbaijan", "Belarus", "Belgium", "Bosnia-Herzegovina",
                                       "Bulgaria", "Croatia", "Czech Republic",
                                       "Denmark", "Estonia", "Faroe Islands", "Finland", "France",
                                       "Georgia", "Germany", "Gibraltar",
                                       "Greece", "Guernsey", "Hungary", "Iceland", "Ireland",
                                       "Isle of Man", "Italy", "Jersey", "Latvia",
                                       "Liechtenstein", "Lithuania", "Luxembourg",
                                       "Macedonia, the former Yugoslav Republic of", "Malta",
                                       "Moldova, Republic of", "Monaco", "Montenegro",
                                       "Netherlands", "Norway", "Poland", "Portugal",
                                       "Romania", "San Marino", "Serbia", "Slovakia", "Slovenia",
                                       "Spain", "Svalbard and Jan Mayen",
                                       "Sweden", "Switzerland", "Ukraine", "Undefined Europe",
                                       "United Kingdom - England, Northern Ireland, Scotland, Wales",
                                       "Vatican City"));
      regions.put("Russian Federation", Collections.singleton("Russian Federation"));
      regions.put("Middle East",
                  Arrays.asList("Afghanistan", "Bahrain", "Cyprus", "Iran", "Iraq", "Israel",
                                "Jordan", "Kuwait", "Lebanon", "Oman",
                                "Palestinian Territory, Occupied", "Qatar", "Saudi Arabia",
                                "Syrian Arab Republic", "Turkey, Republic of",
                                "Undefined Middle East", "United Arab Emirates",
                                "Yemen"));
      regions.put("Africa",
                  Arrays.asList("Algeria", "Angola", "Benin", "Botswana", "Burkina Faso", "Burundi",
                                "Cameroon", "Cape Verde", "Central African Republic", "Chad",
                                "Comoros", "Congo",
                                "Cote d\u0027Ivoire", "Democratic Republic of the Congo",
                                "Djibouti", "Egypt", "Equatorial Guinea",
                                "Eritrea", "Ethiopia", "Gabon", "Gambia", "Ghana", "Guinea",
                                "Guinea-Bissau", "Kenya", "Lesotho",
                                "Liberia", "Libyan Arab Jamahiriya", "Madagascar", "Malawi", "Mali",
                                "Mauritania", "Mauritius",
                                "Mayotte", "Morocco", "Mozambique", "Namibia", "Niger", "Nigeria",
                                "Reunion", "Rwanda",
                                "Sao Tome and Principe", "Senegal", "Seychelles", "Sierra Leone",
                                "Somalia", "South Africa",
                                "St. Helena", "Sudan", "Swaziland", "Tanzania, United Republic of",
                                "Togo", "Tunisia", "Uganda",
                                "Undefined Africa", "Western Sahara", "Zambia", "Zimbabwe"));
      regions.put("Asia", Arrays.asList("Bangladesh", "Bhutan",
                                        "British Indian Ocean Territory - Chagos Islands",
                                        "Brunei Darussalam", "Cambodia", "China",
                                        "Hong Kong", "India", "Indonesia", "Japan", "Kazakhstan",
                                        "Korea, Democratic People\u0027s Republic of",
                                        "Korea, Republic of", "Kyrgyzstan",
                                        "Lao People\u0027s Democratic Republic", "Macao",
                                        "Malaysia", "Maldives", "Mongolia", "Myanmar",
                                        "Nepal", "Pakistan", "Philippines", "Singapore",
                                        "Sri Lanka", "Taiwan", "Tajikistan", "Thailand",
                                        "Timor-Leste, Democratic Republic of", "Turkmenistan",
                                        "Undefined Asia", "Uzbekistan", "Vietnam"));
      regions.put("Australia / Oceania",
                  Arrays.asList("American Samoa", "Australia", "Christmas Island",
                                "Cocos (Keeling) Islands", "Cook Islands", "Fiji",
                                "French Polynesia", "Guam",
                                "Heard Island and McDonald Islands", "Kiribati", "Marshall Islands",
                                "Micronesia , Federated States of", "Nauru", "New Caledonia",
                                "New Zealand", "Niue",
                                "Norfolk Island", "Northern Mariana Islands, Commonwealth of",
                                "Palau", "Papua New Guinea",
                                "Pitcairn", "Samoa", "Solomon Islands", "Tokelau", "Tonga",
                                "Tuvalu",
                                "Undefined Australia / Oceania", "Vanuatu", "Wallis and Futuna"));
      regions.put("Antarctica",
                  Arrays.asList("Antarctica", "Bouvet Island", "French Southern Territories"));
      return Collections.unmodifiableMap(regions);
    }

    @Provides
    WeightedResourceRecordSetApi.Factory provideWeightedResourceRecordSetApiFactory(
        MockWeightedResourceRecordSetApi.Factory in) {
      return in;
    }

    @Provides
    @Singleton
    @Named("weighted")
    SortedSet<Integer> provideSupportedWeights() {
      SortedSet<Integer> supportedWeights = new TreeSet<Integer>();
      for (int i = 0; i <= 100; i++) {
        supportedWeights.add(i);
      }
      return Collections.unmodifiableSortedSet(supportedWeights);
    }
  }
}
