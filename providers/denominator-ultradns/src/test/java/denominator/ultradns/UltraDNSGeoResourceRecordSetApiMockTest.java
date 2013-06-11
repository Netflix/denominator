package denominator.ultradns;

import static com.google.common.io.Resources.getResource;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.model.ResourceRecordSets.toProfile;
import static java.lang.String.format;
import static org.jclouds.util.Strings2.toStringAndClose;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

@Test(singleThreaded = true)
public class UltraDNSGeoResourceRecordSetApiMockTest {

    ResourceRecordSet<CNAMEData> europe = ResourceRecordSet.<CNAMEData> builder()
            .name("srv.denominator.io.")
            .type("CNAME")
            .ttl(300)
            .add(CNAMEData.create("srv-000000001.eu-west-1.elb.amazonaws.com."))
            .addProfile(Geo.create("Europe", ImmutableMultimap.<String, String> builder()
                                .putAll("Europe", "Aland Islands", "Albania", "Andorra", "Armenia", "Austria",
                                        "Azerbaijan", "Belarus", "Belgium", "Bosnia-Herzegovina", "Bulgaria",
                                        "Croatia", "Czech Republic", "Denmark", "Estonia", "Faroe Islands",
                                        "Finland", "France", "Georgia", "Germany", "Gibraltar", "Greece",
                                        "Guernsey", "Hungary", "Iceland", "Ireland", "Isle of Man", "Italy",
                                        "Jersey", "Latvia", "Liechtenstein", "Lithuania", "Luxembourg",
                                        "Macedonia, the former Yugoslav Republic of", "Malta",
                                        "Moldova, Republic of", "Monaco", "Montenegro", "Netherlands", "Norway",
                                        "Poland", "Portugal", "Romania", "San Marino", "Serbia", "Slovakia",
                                        "Slovenia", "Spain", "Svalbard and Jan Mayen", "Sweden", "Switzerland",
                                        "Ukraine", "Undefined Europe",
                                        "United Kingdom - England, Northern Ireland, Scotland, Wales",
                                        "Vatican City").build()))                             
            .build();

    ResourceRecordSet<CNAMEData> us = ResourceRecordSet.<CNAMEData> builder()
            .name("srv.denominator.io.")
            .type("CNAME")
            .ttl(300)
            .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
            .addProfile(Geo.create("US", ImmutableMultimap.<String, String> builder()
                                .putAll("United States (US)", "Alabama", "Alaska", "Arizona", "Arkansas",
                                        "Armed Forces Americas", "Armed Forces Europe, Middle East, and Canada",
                                        "Armed Forces Pacific", "California", "Colorado", "Connecticut",
                                        "Delaware", "District of Columbia", "Florida", "Georgia", "Hawaii",
                                        "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
                                        "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
                                        "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
                                        "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina",
                                        "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania",
                                        "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas",
                                        "Undefined United States", "United States Minor Outlying Islands", "Utah",
                                        "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin",
                                        "Wyoming").build())).build();
    
    ResourceRecordSet<CNAMEData> everywhereElse = ResourceRecordSet.<CNAMEData> builder()
            .name("srv.denominator.io.")
            .type("CNAME")
            .ttl(60)
            .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
            .addProfile(Geo.create("Everywhere Else", ImmutableMultimap.<String, String> builder()
                                .put("Anonymous Proxy (A1)", "Anonymous Proxy")
                                .put("Mexico", "Mexico")
                                .put("Satellite Provider (A2)", "Satellite Provider")
                                .put("Unknown / Uncategorized IPs", "Unknown / Uncategorized IPs")
                                .putAll("Canada (CA)", "Alberta", "British Columbia", "Greenland", "Manitoba",
                                        "New Brunswick", "Newfoundland and Labrador", "Northwest Territories",
                                        "Nova Scotia", "Nunavut", "Ontario", "Prince Edward Island", "Quebec",
                                        "Saint Pierre and Miquelon", "Saskatchewan", "Undefined Canada", "Yukon")
                                .putAll("The Caribbean", "Anguilla", "Antigua and Barbuda", "Aruba", "Bahamas",
                                        "Barbados", "Bermuda", "British Virgin Islands", "Cayman Islands", "Cuba",
                                        "Dominica", "Dominican Republic", "Grenada", "Guadeloupe", "Haiti",
                                        "Jamaica", "Martinique", "Montserrat", "Netherlands Antilles",
                                        "Puerto Rico", "Saint Barthelemy", "Saint Martin",
                                        "Saint Vincent and the Grenadines", "St. Kitts and Nevis", "St. Lucia",
                                        "Trinidad and Tobago", "Turks and Caicos Islands", "U.S. Virgin Islands")
                                .putAll("Central America", "Belize", "Costa Rica", "El Salvador", "Guatemala",
                                        "Honduras", "Nicaragua", "Panama", "Undefined Central America")
                                .putAll("South America", "Argentina", "Bolivia", "Brazil", "Chile", "Colombia",
                                        "Ecuador", "Falkland Islands", "French Guiana", "Guyana", "Paraguay",
                                        "Peru", "South Georgia and the South Sandwich Islands", "Suriname",
                                        "Undefined South America", "Uruguay", "Venezuela, Bolivarian Republic of")
                                .put("Russian Federation", "Russian Federation")
                                .putAll("Middle East", "Afghanistan", "Bahrain", "Cyprus", "Iran", "Iraq",
                                        "Israel", "Jordan", "Kuwait", "Lebanon", "Oman",
                                        "Palestinian Territory, Occupied", "Qatar", "Saudi Arabia",
                                        "Syrian Arab Republic", "Turkey, Republic of", "Undefined Middle East",
                                        "United Arab Emirates", "Yemen")
                                .putAll("Africa", "Algeria", "Angola", "Benin", "Botswana", "Burkina Faso",
                                        "Burundi", "Cameroon", "Cape Verde", "Central African Republic", "Chad",
                                        "Comoros", "Congo", "Cote d'Ivoire", "Democratic Republic of the Congo",
                                        "Djibouti", "Egypt", "Equatorial Guinea", "Eritrea", "Ethiopia", "Gabon",
                                        "Gambia", "Ghana", "Guinea", "Guinea-Bissau", "Kenya", "Lesotho",
                                        "Liberia", "Libyan Arab Jamahiriya", "Madagascar", "Malawi", "Mali",
                                        "Mauritania", "Mauritius", "Mayotte", "Morocco", "Mozambique", "Namibia",
                                        "Niger", "Nigeria", "Reunion", "Rwanda", "Sao Tome and Principe",
                                        "Senegal", "Seychelles", "Sierra Leone", "Somalia", "South Africa",
                                        "St. Helena", "Sudan", "Swaziland", "Tanzania, United Republic of", "Togo",
                                        "Tunisia", "Uganda", "Undefined Africa", "Western Sahara", "Zambia",
                                        "Zimbabwe")
                                .putAll("Asia", "Bangladesh", "Bhutan",
                                        "British Indian Ocean Territory - Chagos Islands", "Brunei Darussalam",
                                        "Cambodia", "China", "Hong Kong", "India", "Indonesia", "Japan",
                                        "Kazakhstan", "Korea, Democratic People's Republic of",
                                        "Korea, Republic of", "Kyrgyzstan", "Lao People's Democratic Republic",
                                        "Macao", "Malaysia", "Maldives", "Mongolia", "Myanmar", "Nepal",
                                        "Pakistan", "Philippines", "Singapore", "Sri Lanka", "Taiwan",
                                        "Tajikistan", "Thailand", "Timor-Leste, Democratic Republic of",
                                        "Turkmenistan", "Undefined Asia", "Uzbekistan", "Vietnam")
                                .putAll("Australia / Oceania", "American Samoa", "Australia", "Christmas Island",
                                        "Cocos (Keeling) Islands", "Cook Islands", "Fiji", "French Polynesia",
                                        "Guam", "Heard Island and McDonald Islands", "Kiribati",
                                        "Marshall Islands", "Micronesia , Federated States of", "Nauru",
                                        "New Caledonia", "New Zealand", "Niue", "Norfolk Island",
                                        "Northern Mariana Islands, Commonwealth of", "Palau", "Papua New Guinea",
                                        "Pitcairn", "Samoa", "Solomon Islands", "Tokelau", "Tonga", "Tuvalu",
                                        "Undefined Australia / Oceania", "Vanuatu", "Wallis and Futuna")
                                .putAll("Antarctica", "Antarctica", "Bouvet Island", "French Southern Territories")
                                .build())).build();

    private String getAccountsListOfUser = format(SOAP_TEMPLATE, "<v01:getAccountsListOfUser/>");
    private String getAccountsListOfUserResponse = "<soap:Envelope><soap:Body><ns1:getAccountsListOfUserResponse><AccountsList><ns2:AccountDetailsData accountID=\"AAAAAAAAAAAAAAAA\" accountName=\"denominator\" /></AccountsList></ns1:getAccountsListOfUserResponse></soap:Body></soap:Envelope>";
    private String getAvailableRegions = format(SOAP_TEMPLATE, "<v01:getAvailableRegions/>");
    private String getAvailableRegionsResponse;
    private String getDirectionalPoolsOfZone = format(SOAP_TEMPLATE, "<v01:getDirectionalPoolsOfZone><zoneName>denominator.io.</zoneName></v01:getDirectionalPoolsOfZone>");
    private String getDirectionalPoolsOfZoneResponse;
    private String getDirectionalDNSGroupDetails = format(SOAP_TEMPLATE,
            "<v01:getDirectionalDNSGroupDetails><GroupId>%s</GroupId></v01:getDirectionalDNSGroupDetails>");
    private String getDirectionalDNSRecordsForHostTemplate = format(
            SOAP_TEMPLATE,
            "<v01:getDirectionalDNSRecordsForHost><zoneName>%s</zoneName><hostName>%s</hostName><poolRecordType>%s</poolRecordType></v01:getDirectionalDNSRecordsForHost>");
    private String getDirectionalDNSRecordsForHost = format(getDirectionalDNSRecordsForHostTemplate,
            "denominator.io.", "srv.denominator.io.", 0);
    private String getDirectionalDNSRecordsForHostIPV4 = format(getDirectionalDNSRecordsForHostTemplate,
            "denominator.io.", "srv.denominator.io.", 1);
    private String getDirectionalDNSRecordsForHostIPV4Response;
    private String getDirectionalDNSGroupDetailsResponseEurope;
    private String getDirectionalDNSGroupDetailsResponseEverywhereElse;
    private String getDirectionalDNSGroupDetailsResponseUS;

    UltraDNSGeoResourceRecordSetApiMockTest() throws IOException {
        getAvailableRegionsResponse = toStringAndClose(getResource("getAvailableRegionsResponse.xml").openStream());
        getDirectionalPoolsOfZoneResponse = toStringAndClose(getResource("getDirectionalPoolsOfZoneResponse.xml").openStream());
        getDirectionalDNSRecordsForHostIPV4Response = toStringAndClose(getResource(
                "getDirectionalDNSRecordsForHostResponse.xml").openStream());
        getDirectionalDNSGroupDetailsResponseEurope = toStringAndClose(getResource(
                "getDirectionalDNSGroupDetailsResponseEurope.xml").openStream());
        getDirectionalDNSGroupDetailsResponseEverywhereElse = toStringAndClose(getResource(
                "getDirectionalDNSGroupDetailsResponseEverywhereElse.xml").openStream());
        getDirectionalDNSGroupDetailsResponseUS = toStringAndClose(getResource(
                "getDirectionalDNSGroupDetailsResponseUS.xml").openStream());
        getDirectionalDNSRecordsForGroupResponseEurope = toStringAndClose(getResource(
                "getDirectionalDNSRecordsForGroupResponseEurope.xml").openStream());
   }

    private String getDirectionalDNSRecordsForHostIPV6 = format(getDirectionalDNSRecordsForHostTemplate, "denominator.io.", "srv.denominator.io.", 28);
    private String noDirectionalDNSRecordsForHostResponse = "<soap:Envelope><soap:Body><ns1:getDirectionalDNSRecordsForHostResponse /></soap:Body></soap:Envelope>";

    @Test
    public void listWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalPoolsOfZoneResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForHostIPV4Response));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseUS));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
                         
            Iterator<ResourceRecordSet<?>> iterator = api.iterator();
            assertEquals(iterator.next().toString(), europe.toString());
            assertEquals(iterator.next().toString(), everywhereElse.toString());
            assertEquals(iterator.next().toString(), us.toString());
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 7);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);

            RecordedRequest getDirectionalPoolsOfZone = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalPoolsOfZone.getBody()), this.getDirectionalPoolsOfZone);

            RecordedRequest getDirectionalDNSRecordsForHostIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForHostIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForHostIPV4.getBody()), this.getDirectionalDNSRecordsForHost);

            for (String groupId : ImmutableList.of("C000000000000001", "C000000000000003", "C000000000000002")) {
                RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
                assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
                assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                        format(this.getDirectionalDNSGroupDetails, groupId));
            }

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForHostIPV4Response));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseUS));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
                         
            Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("srv.denominator.io.");
            assertEquals(iterator.next().toString(), europe.toString());
            assertEquals(iterator.next().toString(), everywhereElse.toString());
            assertEquals(iterator.next().toString(), us.toString());
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 6);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);
            
            RecordedRequest getDirectionalDNSRecordsForHostIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForHostIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForHostIPV4.getBody()), this.getDirectionalDNSRecordsForHost);

            for (String groupId : ImmutableList.of("C000000000000001", "C000000000000003", "C000000000000002")) {
                RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
                assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
                assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                        format(this.getDirectionalDNSGroupDetails, groupId));
            }

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForHostIPV4Response));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noDirectionalDNSRecordsForHostResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseUS));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
                         
            Iterator<ResourceRecordSet<?>> iterator = api.iterateByNameAndType("srv.denominator.io.", "CNAME");
            assertEquals(iterator.next().toString(), europe.toString());
            assertEquals(iterator.next().toString(), everywhereElse.toString());
            assertEquals(iterator.next().toString(), us.toString());
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 7);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);
            
            RecordedRequest getDirectionalDNSRecordsForHostIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForHostIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForHostIPV4.getBody()), this.getDirectionalDNSRecordsForHostIPV4);

            RecordedRequest getDirectionalDNSRecordsForHostIPV6 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForHostIPV6.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForHostIPV6.getBody()), this.getDirectionalDNSRecordsForHostIPV6);

            for (String groupId : ImmutableList.of("C000000000000001", "C000000000000003", "C000000000000002")) {
                RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
                assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
                assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                        format(this.getDirectionalDNSGroupDetails, groupId));
            }

        } finally {
            server.shutdown();
        }
    }

    private String getDirectionalDNSRecordsForGroupTemplate = format(
            SOAP_TEMPLATE,
            "<v01:getDirectionalDNSRecordsForGroup><groupName>%s</groupName><hostName>%s</hostName><zoneName>%s</zoneName><poolRecordType>%s</poolRecordType></v01:getDirectionalDNSRecordsForGroup>");

    private String getDirectionalDNSRecordsForGroupEuropeIPV4 = format(getDirectionalDNSRecordsForGroupTemplate,
            "Europe", "srv.denominator.io.", "denominator.io.", 1);
    private String getDirectionalDNSRecordsForGroupResponseEurope;
    private String getDirectionalDNSRecordsForGroupEuropeIPV6 = format(getDirectionalDNSRecordsForGroupTemplate,
            "Europe", "srv.denominator.io.", "denominator.io.", 28);
    private String noDirectionalDNSRecordsForGroupResponse = "<soap:Envelope><soap:Body><ns1:getDirectionalDNSRecordsForGroupResponse /></soap:Body></soap:Envelope>";

    @Test
    public void applyRegionsToNameTypeAndGroupWhenTTLMatches() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForGroupResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noDirectionalDNSRecordsForGroupResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));

            Multimap<String, String> regions = toProfile(Geo.class).apply(europe).regions();
            api.applyRegionsToNameTypeAndGroup(regions, "srv.denominator.io.", "CNAME", "Europe");

            assertEquals(server.getRequestCount(), 5);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV4.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV4);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV6 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV6.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV6.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV6);
            
            RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
            assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                    format(this.getDirectionalDNSGroupDetails, "C000000000000001"));
        } finally {
            server.shutdown();
        }
    }

    private String updateDirectionalPoolRecordTemplate = format(
            SOAP_TEMPLATE,
            "<v01:updateDirectionalPoolRecord><transactionID /><UpdateDirectionalRecordData directionalPoolRecordId=\"%s\"><DirectionalRecordConfiguration TTL=\"%s\" ><InfoValues Info1Value=\"%s\" /></DirectionalRecordConfiguration>%s</UpdateDirectionalRecordData></v01:updateDirectionalPoolRecord>");

    private String updateDirectionalPoolRecordRegions = format(
            updateDirectionalPoolRecordTemplate,
            "A000000000000001",
            300,
            "srv-000000001.eu-west-1.elb.amazonaws.com.",
            "<GeolocationGroupDetails groupName=\"Europe\" ><GeolocationGroupDefinitionData regionName=\"Europe\" territoryNames=\"Aland Islands\" /></GeolocationGroupDetails><forceOverlapTransfer>true</forceOverlapTransfer>");

    @Test
    public void applyRegionsToNameTypeAndGroupWhenRegionsDiffer() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForGroupResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noDirectionalDNSRecordsForGroupResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(updateDirectionalPoolRecordResponse));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));

            Multimap<String, String> regions = ImmutableMultimap.of("Europe", "Aland Islands");
            api.applyRegionsToNameTypeAndGroup(regions, "srv.denominator.io.", "CNAME", "Europe");

            assertEquals(server.getRequestCount(), 6);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV4.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV4);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV6 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV6.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV6.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV6);

            RecordedRequest getDirectionalDNSGroupDetails = server.takeRequest();
            assertEquals(getDirectionalDNSGroupDetails.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSGroupDetails.getBody()),
                    format(this.getDirectionalDNSGroupDetails, "C000000000000001"));

            RecordedRequest updateDirectionalPoolRecordRegions = server.takeRequest();
            assertEquals(updateDirectionalPoolRecordRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(updateDirectionalPoolRecordRegions.getBody()), this.updateDirectionalPoolRecordRegions);

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void applyTTLToNameTypeAndGroupWhenTTLMatches() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForGroupResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noDirectionalDNSRecordsForGroupResponse));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));

            api.applyTTLToNameTypeAndGroup(300, "srv.denominator.io.", "CNAME", "Europe");

            assertEquals(server.getRequestCount(), 4);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV4.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV4);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV6 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV6.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV6.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV6);

        } finally {
            server.shutdown();
        }
    }

    private String updateDirectionalPoolRecordTTL = format(updateDirectionalPoolRecordTemplate, "A000000000000001",
            600, "srv-000000001.eu-west-1.elb.amazonaws.com.", "");
    private String updateDirectionalPoolRecordResponse = "<soap:Envelope><soap:Body><ns1:updateDirectionalPoolRecordResponse></ns1:updateDirectionalPoolRecordResponse></soap:Body></soap:Envelope>";

    @Test
    public void applyTTLToNameTypeAndGroupWhenTTLDiffers() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAvailableRegionsResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getDirectionalDNSRecordsForGroupResponseEurope));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noDirectionalDNSRecordsForGroupResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(updateDirectionalPoolRecordResponse));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));

            api.applyTTLToNameTypeAndGroup(600, "srv.denominator.io.", "CNAME", "Europe");

            assertEquals(server.getRequestCount(), 5);

            RecordedRequest getAvailableRegions = server.takeRequest();
            assertEquals(getAvailableRegions.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAvailableRegions.getBody()), this.getAvailableRegions);

            RecordedRequest getAccountsListOfUser = server.takeRequest();
            assertEquals(getAccountsListOfUser.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getAccountsListOfUser.getBody()), this.getAccountsListOfUser);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV4 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV4.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV4.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV4);

            RecordedRequest getDirectionalDNSRecordsForGroupEuropeIPV6 = server.takeRequest();
            assertEquals(getDirectionalDNSRecordsForGroupEuropeIPV6.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getDirectionalDNSRecordsForGroupEuropeIPV6.getBody()), this.getDirectionalDNSRecordsForGroupEuropeIPV6);

            RecordedRequest updateDirectionalPoolRecordTTL = server.takeRequest();
            assertEquals(updateDirectionalPoolRecordTTL.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(updateDirectionalPoolRecordTTL.getBody()), this.updateDirectionalPoolRecordTTL);

        } finally {
            server.shutdown();
        }
    }

    private static GeoResourceRecordSetApi mockApi(final URL url) {
        return Denominator.create(new UltraDNSProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("joe", "letmein")).api().geoRecordSetsInZone("denominator.io.").get();
    }

    private static final String SOAP_TEMPLATE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body>%s</soapenv:Body></soapenv:Envelope>";
}
