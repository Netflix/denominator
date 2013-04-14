package denominator.mock;

import static com.google.common.collect.Multimaps.synchronizedListMultimap;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ns;

import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.NothingToClose;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.SOAData;
import denominator.profile.GeoResourceRecordSetApi;

/**
 * in-memory {@code Provider}, used for testing.
 */
@Module(entryPoints = DNSApiManager.class, includes = NothingToClose.class)
public class MockProvider extends Provider {

    @Provides
    protected Provider provideThis() {
        return this;
    }

    @Provides
    ZoneApi provideZoneApi(MockZoneApi in) {
        return in;
    }

    @Provides
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
        return in;
    }

    @Provides
    AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory(
            MockAllProfileResourceRecordSetApi.Factory in) {
        return in;
    }

    @Provides
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(MockGeoResourceRecordSetApi.Factory in) {
        return in;
    }

    // wildcard types are not currently injectable in dagger
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Provides
    @Singleton
    Multimap<String, ResourceRecordSet> provideRecords() {
        String zoneName = "denominator.io.";
        ListMultimap<String, ResourceRecordSet<?>> records = LinkedListMultimap.create();
        records = synchronizedListMultimap(records);
        records.put(zoneName, ResourceRecordSet.builder()
                                            .type("SOA")
                                            .name(zoneName)
                                            .ttl(3600)
                                            .add(SOAData.builder()
                                                        .mname("ns1." + zoneName)
                                                        .rname("admin." + zoneName)
                                                        .serial(1)
                                                        .refresh(3600)
                                                        .retry(600)
                                                        .expire(604800)
                                                        .minimum(60).build()).build());
        records.put(zoneName, ns(zoneName, 86400, "ns1." + zoneName));
        records.put(zoneName, a("www1." + zoneName, 3600, ImmutableSet.of("192.0.2.1", "192.0.2.2")));
        records.put(zoneName, a("www2." + zoneName, 3600, "198.51.100.1"));
        records.put(zoneName, cname("www." + zoneName, 3600, "www1." + zoneName));
        records.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                .name("www2.geo.denominator.io.")
                .type("A")
                .ttl(300)
                .add(AData.create("192.0.2.1"))
                .addProfile(Geo.create("alazona", ImmutableMultimap.<String, String> builder()
                        .putAll("United States (US)", ImmutableList.of("Alaska", "Arizona"))
                        .build()))
                .build());
        records.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                .name("www.geo.denominator.io.")
                .type("CNAME")
                .ttl(300)
                .add(CNAMEData.create("a.denominator.io."))
                .addProfile(Geo.create("alazona", ImmutableMultimap.<String, String> builder()
                        .putAll("United States (US)", ImmutableList.of("Alaska", "Arizona"))
                        .build()))
                .build());
        records.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                .name("www.geo.denominator.io.")
                .type("CNAME")
                .ttl(86400)
                .add(CNAMEData.create("b.denominator.io."))
                .addProfile(Geo.create("columbador", ImmutableMultimap.<String, String> builder()
                        .putAll("South America", ImmutableList.of("Colombia", "Ecuador"))
                        .build()))
                .build());
        records.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                .name("www.geo.denominator.io.")
                .type("CNAME")
                .ttl(0)
                .add(CNAMEData.create("c.denominator.io."))
                .addProfile(Geo.create("antarctica", ImmutableMultimap.<String, String> builder()
                        .putAll("Antarctica", ImmutableList.<String> builder()
                                                    .add("Bouvet Island")
                                                    .add("French Southern Territories")
                                                    .add("Antarctica").build()).build()))
                .build());
        return Multimap.class.cast(records);
    }

    @Provides
    @Singleton
    @denominator.config.profile.Geo
    Set<String> provideSupportedGeoRecordTypes() {
        return ImmutableSet.of("A", "CNAME");
    }

    @Provides
    @Singleton
    @denominator.config.profile.Geo
    Multimap<String, String> provideRegions() {
        return ImmutableMultimap.<String, String> builder()
                .put("Anonymous Proxy (A1)", "Anonymous Proxy")
                .put("Satellite Provider (A2)", "Satellite Provider")
                .put("Unknown / Uncategorized IPs", "Unknown / Uncategorized IPs")
                .putAll("United States (US)",
                        ImmutableList.of("Alabama", "Alaska", "Arizona", "Arkansas", "Armed Forces Americas",
                                "Armed Forces Europe, Middle East, and Canada", "Armed Forces Pacific", "California",
                                "Colorado", "Connecticut", "Delaware", "District of Columbia", "Florida", "Georgia",
                                "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
                                "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi",
                                "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey",
                                "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma",
                                "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
                                "Tennessee", "Texas", "Undefined United States",
                                "United States Minor Outlying Islands", "Utah", "Vermont", "Virginia", "Washington",
                                "West Virginia", "Wisconsin", "Wyoming"))
                .put("Mexico", "Mexico")
                .putAll("Canada (CA)",
                        ImmutableList.of("Alberta", "British Columbia", "Greenland", "Manitoba", "New Brunswick",
                                "Newfoundland and Labrador", "Northwest Territories", "Nova Scotia", "Nunavut",
                                "Ontario", "Prince Edward Island", "Quebec", "Saint Pierre and Miquelon",
                                "Saskatchewan", "Undefined Canada", "Yukon"))
                .putAll("The Caribbean",
                        ImmutableList.of("Anguilla", "Antigua and Barbuda", "Aruba", "Bahamas", "Barbados", "Bermuda",
                                "British Virgin Islands", "Cayman Islands", "Cuba", "Dominica", "Dominican Republic",
                                "Grenada", "Guadeloupe", "Haiti", "Jamaica", "Martinique", "Montserrat",
                                "Netherlands Antilles", "Puerto Rico", "Saint Barthelemy", "Saint Martin",
                                "Saint Vincent and the Grenadines", "St. Kitts and Nevis", "St. Lucia",
                                "Trinidad and Tobago", "Turks and Caicos Islands", "U.S. Virgin Islands"))
                .putAll("Central America",
                        ImmutableList.of("Belize", "Costa Rica", "El Salvador", "Guatemala", "Honduras", "Nicaragua",
                                "Panama", "Undefined Central America"))
                .putAll("South America",
                        ImmutableList.of("Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador",
                                "Falkland Islands", "French Guiana", "Guyana", "Paraguay", "Peru",
                                "South Georgia and the South Sandwich Islands", "Suriname", "Undefined South America",
                                "Uruguay", "Venezuela, Bolivarian Republic of"))
                .putAll("Europe",
                        ImmutableList.of("Aland Islands", "Albania", "Andorra", "Armenia", "Austria", "Azerbaijan",
                                "Belarus", "Belgium", "Bosnia-Herzegovina", "Bulgaria", "Croatia", "Czech Republic",
                                "Denmark", "Estonia", "Faroe Islands", "Finland", "France", "Georgia", "Germany",
                                "Gibraltar", "Greece", "Guernsey", "Hungary", "Iceland", "Ireland", "Isle of Man",
                                "Italy", "Jersey", "Latvia", "Liechtenstein", "Lithuania", "Luxembourg",
                                "Macedonia, the former Yugoslav Republic of", "Malta", "Moldova, Republic of",
                                "Monaco", "Montenegro", "Netherlands", "Norway", "Poland", "Portugal", "Romania",
                                "San Marino", "Serbia", "Slovakia", "Slovenia", "Spain", "Svalbard and Jan Mayen",
                                "Sweden", "Switzerland", "Ukraine", "Undefined Europe",
                                "United Kingdom - England, Northern Ireland, Scotland, Wales", "Vatican City"))
                .put("Russian Federation", "Russian Federation")
                .putAll("Middle East",
                        ImmutableList.of("Afghanistan", "Bahrain", "Cyprus", "Iran", "Iraq", "Israel", "Jordan",
                                "Kuwait", "Lebanon", "Oman", "Palestinian Territory, Occupied", "Qatar",
                                "Saudi Arabia", "Syrian Arab Republic", "Turkey, Republic of", "Undefined Middle East",
                                "United Arab Emirates", "Yemen"))
                .putAll("Africa",
                        ImmutableList.of("Algeria", "Angola", "Benin", "Botswana", "Burkina Faso", "Burundi",
                                "Cameroon", "Cape Verde", "Central African Republic", "Chad", "Comoros", "Congo",
                                "Cote d\u0027Ivoire", "Democratic Republic of the Congo", "Djibouti", "Egypt",
                                "Equatorial Guinea", "Eritrea", "Ethiopia", "Gabon", "Gambia", "Ghana", "Guinea",
                                "Guinea-Bissau", "Kenya", "Lesotho", "Liberia", "Libyan Arab Jamahiriya", "Madagascar",
                                "Malawi", "Mali", "Mauritania", "Mauritius", "Mayotte", "Morocco", "Mozambique",
                                "Namibia", "Niger", "Nigeria", "Reunion", "Rwanda", "Sao Tome and Principe", "Senegal",
                                "Seychelles", "Sierra Leone", "Somalia", "South Africa", "St. Helena", "Sudan",
                                "Swaziland", "Tanzania, United Republic of", "Togo", "Tunisia", "Uganda",
                                "Undefined Africa", "Western Sahara", "Zambia", "Zimbabwe"))
                .putAll("Asia",
                        ImmutableList.of("Bangladesh", "Bhutan", "British Indian Ocean Territory - Chagos Islands",
                                "Brunei Darussalam", "Cambodia", "China", "Hong Kong", "India", "Indonesia", "Japan",
                                "Kazakhstan", "Korea, Democratic People\u0027s Republic of", "Korea, Republic of",
                                "Kyrgyzstan", "Lao People\u0027s Democratic Republic", "Macao", "Malaysia", "Maldives",
                                "Mongolia", "Myanmar", "Nepal", "Pakistan", "Philippines", "Singapore", "Sri Lanka",
                                "Taiwan", "Tajikistan", "Thailand", "Timor-Leste, Democratic Republic of",
                                "Turkmenistan", "Undefined Asia", "Uzbekistan", "Vietnam"))
                .putAll("Australia / Oceania",
                        ImmutableList.of("American Samoa", "Australia", "Christmas Island", "Cocos (Keeling) Islands",
                                "Cook Islands", "Fiji", "French Polynesia", "Guam",
                                "Heard Island and McDonald Islands", "Kiribati", "Marshall Islands",
                                "Micronesia , Federated States of", "Nauru", "New Caledonia", "New Zealand", "Niue",
                                "Norfolk Island", "Northern Mariana Islands, Commonwealth of", "Palau",
                                "Papua New Guinea", "Pitcairn", "Samoa", "Solomon Islands", "Tokelau", "Tonga",
                                "Tuvalu", "Undefined Australia / Oceania", "Vanuatu", "Wallis and Futuna"))
                .putAll("Antarctica", ImmutableList.of("Antarctica", "Bouvet Island", "French Southern Territories"))
                .build();
    }
}