package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getAvailableRegions;
import static denominator.ultradns.UltraDNSTest.getAvailableRegionsResponse;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSGroupDetails;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSGroupDetailsResponseEurope;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroup;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroupEuropeIPV6;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroupResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroupResponsePresent;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostIPV4;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostIPV6;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostResponsePresent;
import static denominator.ultradns.UltraDNSTest.getDirectionalPoolsOfZoneResponsePresent;
import static denominator.ultradns.UltraDNSTest.updateDirectionalPoolRecordRegions;
import static denominator.ultradns.UltraDNSTest.updateDirectionalPoolRecordResponse;
import static denominator.ultradns.UltraDNSTest.updateDirectionalPoolRecordTemplate;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Ordering;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

@Test
public class UltraDNSGeoResourceRecordSetApiMockTest {

    ResourceRecordSet<CNAMEData> europe = ResourceRecordSet
            .<CNAMEData> builder()
            .name("www.denominator.io.")
            .type("CNAME")
            .qualifier("Europe")
            .ttl(300)
            .add(CNAMEData.create("www-000000001.eu-west-1.elb.amazonaws.com."))
            .addProfile(
                    Geo.create(ImmutableMultimap
                            .<String, String> builder()
                            .orderKeysBy(Ordering.natural())
                            .putAll("Europe", "Aland Islands", "Albania", "Andorra", "Armenia", "Austria",
                                    "Azerbaijan", "Belarus", "Belgium", "Bosnia-Herzegovina", "Bulgaria", "Croatia",
                                    "Czech Republic", "Denmark", "Estonia", "Faroe Islands", "Finland", "France",
                                    "Georgia", "Germany", "Gibraltar", "Greece", "Guernsey", "Hungary", "Iceland",
                                    "Ireland", "Isle of Man", "Italy", "Jersey", "Latvia", "Liechtenstein",
                                    "Lithuania", "Luxembourg", "Macedonia, the former Yugoslav Republic of", "Malta",
                                    "Moldova, Republic of", "Monaco", "Montenegro", "Netherlands", "Norway", "Poland",
                                    "Portugal", "Romania", "San Marino", "Serbia", "Slovakia", "Slovenia", "Spain",
                                    "Svalbard and Jan Mayen", "Sweden", "Switzerland", "Ukraine", "Undefined Europe",
                                    "United Kingdom - England, Northern Ireland, Scotland, Wales", "Vatican City")
                            .build().asMap())).build();

    ResourceRecordSet<CNAMEData> us = ResourceRecordSet
            .<CNAMEData> builder()
            .name("www.denominator.io.")
            .type("CNAME")
            .qualifier("US")
            .ttl(300)
            .add(CNAMEData.create("www-000000001.us-east-1.elb.amazonaws.com."))
            .addProfile(
                    Geo.create(ImmutableMultimap
                            .<String, String> builder()
                            .putAll("United States (US)", "Alabama", "Alaska", "Arizona", "Arkansas",
                                    "Armed Forces Americas", "Armed Forces Europe, Middle East, and Canada",
                                    "Armed Forces Pacific", "California", "Colorado", "Connecticut", "Delaware",
                                    "District of Columbia", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois",
                                    "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland",
                                    "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana",
                                    "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York",
                                    "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania",
                                    "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas",
                                    "Undefined United States", "United States Minor Outlying Islands", "Utah",
                                    "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")
                            .build().asMap())).build();

    ResourceRecordSet<CNAMEData> everywhereElse = ResourceRecordSet
            .<CNAMEData> builder()
            .name("www.denominator.io.")
            .type("CNAME")
            .qualifier("Everywhere Else")
            .ttl(60)
            .add(CNAMEData.create("www-000000002.us-east-1.elb.amazonaws.com."))
            .addProfile(
                    Geo.create(ImmutableMultimap
                            .<String, String> builder()
                            .orderKeysBy(Ordering.natural())
                            .put("Anonymous Proxy (A1)", "Anonymous Proxy")
                            .put("Mexico", "Mexico")
                            .put("Satellite Provider (A2)", "Satellite Provider")
                            .put("Unknown / Uncategorized IPs", "Unknown / Uncategorized IPs")
                            .putAll("Canada (CA)", "Alberta", "British Columbia", "Greenland", "Manitoba",
                                    "New Brunswick", "Newfoundland and Labrador", "Northwest Territories",
                                    "Nova Scotia", "Nunavut", "Ontario", "Prince Edward Island", "Quebec",
                                    "Saint Pierre and Miquelon", "Saskatchewan", "Undefined Canada", "Yukon")
                            .putAll("The Caribbean", "Anguilla", "Antigua and Barbuda", "Aruba", "Bahamas", "Barbados",
                                    "Bermuda", "British Virgin Islands", "Cayman Islands", "Cuba", "Dominica",
                                    "Dominican Republic", "Grenada", "Guadeloupe", "Haiti", "Jamaica", "Martinique",
                                    "Montserrat", "Netherlands Antilles", "Puerto Rico", "Saint Barthelemy",
                                    "Saint Martin", "Saint Vincent and the Grenadines", "St. Kitts and Nevis",
                                    "St. Lucia", "Trinidad and Tobago", "Turks and Caicos Islands",
                                    "U.S. Virgin Islands")
                            .putAll("Central America", "Belize", "Costa Rica", "El Salvador", "Guatemala", "Honduras",
                                    "Nicaragua", "Panama", "Undefined Central America")
                            .putAll("South America", "Argentina", "Bolivia", "Brazil", "Chile", "Colombia", "Ecuador",
                                    "Falkland Islands", "French Guiana", "Guyana", "Paraguay", "Peru",
                                    "South Georgia and the South Sandwich Islands", "Suriname",
                                    "Undefined South America", "Uruguay", "Venezuela, Bolivarian Republic of")
                            .put("Russian Federation", "Russian Federation")
                            .putAll("Middle East", "Afghanistan", "Bahrain", "Cyprus", "Iran", "Iraq", "Israel",
                                    "Jordan", "Kuwait", "Lebanon", "Oman", "Palestinian Territory, Occupied", "Qatar",
                                    "Saudi Arabia", "Syrian Arab Republic", "Turkey, Republic of",
                                    "Undefined Middle East", "United Arab Emirates", "Yemen")
                            .putAll("Africa", "Algeria", "Angola", "Benin", "Botswana", "Burkina Faso", "Burundi",
                                    "Cameroon", "Cape Verde", "Central African Republic", "Chad", "Comoros", "Congo",
                                    "Cote d'Ivoire", "Democratic Republic of the Congo", "Djibouti", "Egypt",
                                    "Equatorial Guinea", "Eritrea", "Ethiopia", "Gabon", "Gambia", "Ghana", "Guinea",
                                    "Guinea-Bissau", "Kenya", "Lesotho", "Liberia", "Libyan Arab Jamahiriya",
                                    "Madagascar", "Malawi", "Mali", "Mauritania", "Mauritius", "Mayotte", "Morocco",
                                    "Mozambique", "Namibia", "Niger", "Nigeria", "Reunion", "Rwanda",
                                    "Sao Tome and Principe", "Senegal", "Seychelles", "Sierra Leone", "Somalia",
                                    "South Africa", "St. Helena", "Sudan", "Swaziland", "Tanzania, United Republic of",
                                    "Togo", "Tunisia", "Uganda", "Undefined Africa", "Western Sahara", "Zambia",
                                    "Zimbabwe")
                            .putAll("Asia", "Bangladesh", "Bhutan", "British Indian Ocean Territory - Chagos Islands",
                                    "Brunei Darussalam", "Cambodia", "China", "Hong Kong", "India", "Indonesia",
                                    "Japan", "Kazakhstan", "Korea, Democratic People's Republic of",
                                    "Korea, Republic of", "Kyrgyzstan", "Lao People's Democratic Republic", "Macao",
                                    "Malaysia", "Maldives", "Mongolia", "Myanmar", "Nepal", "Pakistan", "Philippines",
                                    "Singapore", "Sri Lanka", "Taiwan", "Tajikistan", "Thailand",
                                    "Timor-Leste, Democratic Republic of", "Turkmenistan", "Undefined Asia",
                                    "Uzbekistan", "Vietnam")
                            .putAll("Australia / Oceania", "American Samoa", "Australia", "Christmas Island",
                                    "Cocos (Keeling) Islands", "Cook Islands", "Fiji", "French Polynesia", "Guam",
                                    "Heard Island and McDonald Islands", "Kiribati", "Marshall Islands",
                                    "Micronesia , Federated States of", "Nauru", "New Caledonia", "New Zealand",
                                    "Niue", "Norfolk Island", "Northern Mariana Islands, Commonwealth of", "Palau",
                                    "Papua New Guinea", "Pitcairn", "Samoa", "Solomon Islands", "Tokelau", "Tonga",
                                    "Tuvalu", "Undefined Australia / Oceania", "Vanuatu", "Wallis and Futuna")
                            .putAll("Antarctica", "Antarctica", "Bouvet Island", "French Southern Territories").build()
                            .asMap())).build();

    static String getDirectionalDNSGroupDetailsResponseEverywhereElse = ""//
            + "<?xml version=\"1.0\"?>\n"//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "        <soap:Body>\n"//
            + "                <ns1:getDirectionalDNSGroupDetailsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "                        <DirectionalDNSGroupDetail xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" GroupName=\"Everywhere Else\">\n"//
            + "                                <ns2:DirectionalDNSRegion>\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Anonymous Proxy (A1)\" TerritoryName=\"Anonymous Proxy\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Mexico\" TerritoryName=\"Mexico\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Satellite Provider (A2)\" TerritoryName=\"Satellite Provider\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Unknown / Uncategorized IPs\" TerritoryName=\"Unknown / Uncategorized IPs\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Canada (CA)\" TerritoryName=\"Alberta;British Columbia;Greenland;Manitoba;New Brunswick;Newfoundland and Labrador;Northwest Territories;Nova Scotia;Nunavut;Ontario;Prince Edward Island;Quebec;Saint Pierre and Miquelon;Saskatchewan;Undefined Canada;Yukon\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"The Caribbean\" TerritoryName=\"Anguilla;Antigua and Barbuda;Aruba;Bahamas;Barbados;Bermuda;British Virgin Islands;Cayman Islands;Cuba;Dominica;Dominican Republic;Grenada;Guadeloupe;Haiti;Jamaica;Martinique;Montserrat;Netherlands Antilles;Puerto Rico;Saint Barthelemy;Saint Martin;Saint Vincent and the Grenadines;St. Kitts and Nevis;St. Lucia;Trinidad and Tobago;Turks and Caicos Islands;U.S. Virgin Islands\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Central America\" TerritoryName=\"Belize;Costa Rica;El Salvador;Guatemala;Honduras;Nicaragua;Panama;Undefined Central America\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"South America\" TerritoryName=\"Argentina;Bolivia;Brazil;Chile;Colombia;Ecuador;Falkland Islands;French Guiana;Guyana;Paraguay;Peru;South Georgia and the South Sandwich Islands;Suriname;Undefined South America;Uruguay;Venezuela, Bolivarian Republic of\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Russian Federation\" TerritoryName=\"Russian Federation\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Middle East\" TerritoryName=\"Afghanistan;Bahrain;Cyprus;Iran;Iraq;Israel;Jordan;Kuwait;Lebanon;Oman;Palestinian Territory, Occupied;Qatar;Saudi Arabia;Syrian Arab Republic;Turkey, Republic of;Undefined Middle East;United Arab Emirates;Yemen\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Africa\" TerritoryName=\"Algeria;Angola;Benin;Botswana;Burkina Faso;Burundi;Cameroon;Cape Verde;Central African Republic;Chad;Comoros;Congo;Cote d'Ivoire;Democratic Republic of the Congo;Djibouti;Egypt;Equatorial Guinea;Eritrea;Ethiopia;Gabon;Gambia;Ghana;Guinea;Guinea-Bissau;Kenya;Lesotho;Liberia;Libyan Arab Jamahiriya;Madagascar;Malawi;Mali;Mauritania;Mauritius;Mayotte;Morocco;Mozambique;Namibia;Niger;Nigeria;Reunion;Rwanda;Sao Tome and Principe;Senegal;Seychelles;Sierra Leone;Somalia;South Africa;St. Helena;Sudan;Swaziland;Tanzania, United Republic of;Togo;Tunisia;Uganda;Undefined Africa;Western Sahara;Zambia;Zimbabwe\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Asia\" TerritoryName=\"Bangladesh;Bhutan;British Indian Ocean Territory - Chagos Islands;Brunei Darussalam;Cambodia;China;Hong Kong;India;Indonesia;Japan;Kazakhstan;Korea, Democratic People's Republic of;Korea, Republic of;Kyrgyzstan;Lao People's Democratic Republic;Macao;Malaysia;Maldives;Mongolia;Myanmar;Nepal;Pakistan;Philippines;Singapore;Sri Lanka;Taiwan;Tajikistan;Thailand;Timor-Leste, Democratic Republic of;Turkmenistan;Undefined Asia;Uzbekistan;Vietnam\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Australia / Oceania\" TerritoryName=\"American Samoa;Australia;Christmas Island;Cocos (Keeling) Islands;Cook Islands;Fiji;French Polynesia;Guam;Heard Island and McDonald Islands;Kiribati;Marshall Islands;Micronesia , Federated States of;Nauru;New Caledonia;New Zealand;Niue;Norfolk Island;Northern Mariana Islands, Commonwealth of;Palau;Papua New Guinea;Pitcairn;Samoa;Solomon Islands;Tokelau;Tonga;Tuvalu;Undefined Australia / Oceania;Vanuatu;Wallis and Futuna\" />\n"//
            + "                                        <ns2:RegionForNewGroups RegionName=\"Antarctica\" TerritoryName=\"Antarctica;Bouvet Island;French Southern Territories\" />\n"//
            + "                                </ns2:DirectionalDNSRegion>\n"//
            + "                        </DirectionalDNSGroupDetail>\n"//
            + "                </ns1:getDirectionalDNSGroupDetailsResponse>\n"//
            + "        </soap:Body>\n"//
            + "</soap:Envelope>";

    static String getDirectionalDNSGroupDetailsResponseUS = ""//
            + "<?xml version=\"1.0\"?>\n"//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + " <soap:Body>\n"//
            + "         <ns1:getDirectionalDNSGroupDetailsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "                 <DirectionalDNSGroupDetail\n"//
            + "                         xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" GroupName=\"US\">\n"//
            + "                         <ns2:DirectionalDNSRegion>\n"//
            + "                                 <ns2:RegionForNewGroups RegionName=\"United States (US)\" TerritoryName=\"Alabama;Alaska;Arizona;Arkansas;Armed Forces Americas;Armed Forces Europe, Middle East, and Canada;Armed Forces Pacific;California;Colorado;Connecticut;Delaware;District of Columbia;Florida;Georgia;Hawaii;Idaho;Illinois;Indiana;Iowa;Kansas;Kentucky;Louisiana;Maine;Maryland;Massachusetts;Michigan;Minnesota;Mississippi;Missouri;Montana;Nebraska;Nevada;New Hampshire;New Jersey;New Mexico;New York;North Carolina;North Dakota;Ohio;Oklahoma;Oregon;Pennsylvania;Rhode Island;South Carolina;South Dakota;Tennessee;Texas;Undefined United States;United States Minor Outlying Islands;Utah;Vermont;Virginia;Washington;West Virginia;Wisconsin;Wyoming\" />\n"//
            + "                         </ns2:DirectionalDNSRegion>\n"//
            + "                 </DirectionalDNSGroupDetail>\n"//
            + "         </ns1:getDirectionalDNSGroupDetailsResponse>\n"//
            + " </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void listWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalPoolsOfZoneResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseUS));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());

            Iterator<ResourceRecordSet<?>> iterator = api.iterator();
            assertEquals(iterator.next().toString(), europe.toString());
            assertEquals(iterator.next().toString(), everywhereElse.toString());
            assertEquals(iterator.next().toString(), us.toString());
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 5);

            assertEquals(new String(server.takeRequest().getBody()), UltraDNSTest.getDirectionalPoolsOfZone);

            RecordedRequest getDirectionalDNSRecordsForHostIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForHostIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForHostIPV4.getBody()),
                    UltraDNSTest.getDirectionalDNSRecordsForHost);

            for (String groupId : ImmutableList.of("C000000000000001", "C000000000000003", "C000000000000002")) {
                RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
                assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
                assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                        UltraDNSTest.getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", groupId));
            }

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseUS));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());

            Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("www.denominator.io.");
            assertEquals(iterator.next().toString(), europe.toString());
            assertEquals(iterator.next().toString(), everywhereElse.toString());
            assertEquals(iterator.next().toString(), us.toString());
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 4);

            RecordedRequest getDirectionalDNSRecordsForHostIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForHostIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForHostIPV4.getBody()),
                    UltraDNSTest.getDirectionalDNSRecordsForHost);

            for (String groupId : ImmutableList.of("C000000000000001", "C000000000000003", "C000000000000002")) {
                RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
                assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
                assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                        UltraDNSTest.getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", groupId));
            }

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponseAbsent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseUS));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());

            Iterator<ResourceRecordSet<?>> iterator = api.iterateByNameAndType("www.denominator.io.", "CNAME");
            assertEquals(iterator.next().toString(), europe.toString());
            assertEquals(iterator.next().toString(), everywhereElse.toString());
            assertEquals(iterator.next().toString(), us.toString());
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 5);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForHostIPV4);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForHostIPV6);
            for (String groupId : ImmutableList.of("C000000000000001", "C000000000000003", "C000000000000002")) {
                assertEquals(new String(server.takeRequest().getBody()),
                        UltraDNSTest.getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", groupId));
            }
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void supportedRegionsCache() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());

            assertEquals(api.supportedRegions().size(), 2);
            assertEquals(api.supportedRegions().size(), 2);

            assertEquals(server.getRequestCount(), 1);
            assertEquals(new String(server.takeRequest().getBody()), getAvailableRegions);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void putWhenMatches() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());

            api.put(europe);

            assertEquals(server.getRequestCount(), 3);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForGroup);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForGroupEuropeIPV6);
            assertEquals(new String(server.takeRequest().getBody()),
                    getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void putWhenRegionsDiffer() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setBody(updateDirectionalPoolRecordResponse));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());

            ResourceRecordSet<CNAMEData> lessOfEurope = ResourceRecordSet.<CNAMEData> builder()//
                    .name(europe.name())//
                    .type(europe.type())//
                    .qualifier(europe.qualifier())//
                    .ttl(europe.ttl())//
                    .addAll(europe.records())//
                    .addProfile(Geo.create(ImmutableMultimap.of("Europe", "Aland Islands").asMap())).build();
            api.put(lessOfEurope);

            assertEquals(server.getRequestCount(), 4);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForGroup);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForGroupEuropeIPV6);
            assertEquals(new String(server.takeRequest().getBody()),
                    getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
            assertEquals(new String(server.takeRequest().getBody()), updateDirectionalPoolRecordRegions);
        } finally {
            server.shutdown();
        }
    }

    static String updateDirectionalPoolRecordTTL = format(
            updateDirectionalPoolRecordTemplate,
            "A000000000000001",
            600,
            "www-000000001.eu-west-1.elb.amazonaws.com.",
            "<GeolocationGroupDetails groupName=\"Europe\"><GeolocationGroupDefinitionData regionName=\"Europe\" territoryNames=\"Aland Islands;Albania;Andorra;Armenia;Austria;Azerbaijan;Belarus;Belgium;Bosnia-Herzegovina;Bulgaria;Croatia;Czech Republic;Denmark;Estonia;Faroe Islands;Finland;France;Georgia;Germany;Gibraltar;Greece;Guernsey;Hungary;Iceland;Ireland;Isle of Man;Italy;Jersey;Latvia;Liechtenstein;Lithuania;Luxembourg;Macedonia, the former Yugoslav Republic of;Malta;Moldova, Republic of;Monaco;Montenegro;Netherlands;Norway;Poland;Portugal;Romania;San Marino;Serbia;Slovakia;Slovenia;Spain;Svalbard and Jan Mayen;Sweden;Switzerland;Ukraine;Undefined Europe;United Kingdom - England, Northern Ireland, Scotland, Wales;Vatican City\" /></GeolocationGroupDetails><forceOverlapTransfer>true</forceOverlapTransfer>");

    @Test
    public void putWhenTTLDiffers() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
        server.enqueue(new MockResponse().setBody(updateDirectionalPoolRecordResponse));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getPort());
            ResourceRecordSet<CNAMEData> lessTTL = ResourceRecordSet.<CNAMEData> builder()//
                    .name(europe.name())//
                    .type(europe.type())//
                    .qualifier(europe.qualifier())//
                    .ttl(600)//
                    .addAll(europe.records())//
                    .addAllProfile(europe.profiles()).build();
            api.put(lessTTL);

            assertEquals(server.getRequestCount(), 3);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForGroup);
            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSRecordsForGroupEuropeIPV6);
            assertEquals(new String(server.takeRequest().getBody()), updateDirectionalPoolRecordTTL);
        } finally {
            server.shutdown();
        }
    }

    private static GeoResourceRecordSetApi mockApi(final int port) {
        return Denominator.create(new UltraDNSProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port;
            }
        }, credentials("joe", "letmein")).api().geoRecordSetsInZone("denominator.io.");
    }
}
