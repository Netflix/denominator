package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSException.DIRECTIONAL_NOT_ENABLED;
import static denominator.ultradns.UltraDNSTest.getAvailableRegions;
import static denominator.ultradns.UltraDNSTest.getAvailableRegionsResponse;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSGroupDetails;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSGroupDetailsResponseEurope;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroup;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroupEuropeIPV6;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroupResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForGroupResponsePresent;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHost;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostIPV4;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostIPV6;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getDirectionalDNSRecordsForHostResponsePresent;
import static denominator.ultradns.UltraDNSTest.getDirectionalPoolsOfZone;
import static denominator.ultradns.UltraDNSTest.getDirectionalPoolsOfZoneResponsePresent;
import static denominator.ultradns.UltraDNSTest.updateDirectionalPoolRecordRegions;
import static denominator.ultradns.UltraDNSTest.updateDirectionalPoolRecordResponse;
import static denominator.ultradns.UltraDNSTest.updateDirectionalPoolRecordTemplate;
import static java.lang.String.format;

public class UltraDNSGeoResourceRecordSetApiMockTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();

  @Test
  public void apiWhenUnsupported() throws Exception {
    server.enqueueError(DIRECTIONAL_NOT_ENABLED,
                        "Directional feature not Enabled or Directional migration is not done.");

    assertThat(server.connect().api().geoRecordSetsInZone("denominator.io.")).isNull();

    server.assertSoapBody(getAvailableRegions);
  }

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
    server.enqueue(new MockResponse().setBody(getDirectionalPoolsOfZoneResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseUS));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");

    Iterator<ResourceRecordSet<?>> iterator = api.iterator();
    assertEurope(iterator.next());
    assertEverywhereElse(iterator.next());
    assertUS(iterator.next());
    assertThat(iterator).isEmpty();

    server.assertSoapBody(getAvailableRegions);
    server.assertSoapBody(getDirectionalPoolsOfZone);
    server.assertSoapBody(getDirectionalDNSRecordsForHost);
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000003"));
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000002"));
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseUS));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");

    Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("www.denominator.io.");
    assertEurope(iterator.next());
    assertEverywhereElse(iterator.next());
    assertUS(iterator.next());
    assertThat(iterator).isEmpty();

    server.assertSoapBody(getAvailableRegions);
    server.assertSoapBody(getDirectionalDNSRecordsForHost);
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000003"));
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000002"));
  }

  @Test
  public void iterateByNameAndTypeWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponseAbsent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEverywhereElse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseUS));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");

    Iterator<ResourceRecordSet<?>>
        iterator =
        api.iterateByNameAndType("www.denominator.io.", "CNAME");
    assertEurope(iterator.next());
    assertEverywhereElse(iterator.next());
    assertUS(iterator.next());
    assertThat(iterator).isEmpty();

    server.assertSoapBody(getAvailableRegions);
    server.assertSoapBody(getDirectionalDNSRecordsForHostIPV4);
    server.assertSoapBody(getDirectionalDNSRecordsForHostIPV6);
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000003"));
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000002"));
  }

  @Test
  public void supportedRegionsCache() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");

    assertThat(api.supportedRegions()).hasSize(2);
    api.supportedRegions(); // cached

    server.assertSoapBody(getAvailableRegions);
  }

  @Test
  public void putWhenMatches() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");

    api.put(europe);

    server.assertSoapBody(getAvailableRegions);
    server.assertSoapBody(getDirectionalDNSRecordsForGroup);
    server.assertSoapBody(getDirectionalDNSRecordsForGroupEuropeIPV6);
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
  }

  @Test
  public void putWhenRegionsDiffer() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
    server.enqueue(new MockResponse().setBody(updateDirectionalPoolRecordResponse));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");

    ResourceRecordSet<CNAMEData> lessOfEurope = ResourceRecordSet.<CNAMEData>builder()
        .name(europe.name())
        .type(europe.type())
        .qualifier(europe.qualifier())
        .ttl(europe.ttl())
        .addAll(europe.records())
        .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
          {
            put("Europe", Arrays.asList("Aland Islands"));
          }
        })).build();
    api.put(lessOfEurope);

    server.assertSoapBody(getAvailableRegions);
    server.assertSoapBody(getDirectionalDNSRecordsForGroup);
    server.assertSoapBody(getDirectionalDNSRecordsForGroupEuropeIPV6);
    server.assertSoapBody(
        getDirectionalDNSGroupDetails.replace("AAAAAAAAAAAAAAAA", "C000000000000001"));
    server.assertSoapBody(updateDirectionalPoolRecordRegions);
  }

  @Test
  public void putWhenTTLDiffers() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
    server.enqueue(new MockResponse().setBody(updateDirectionalPoolRecordResponse));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
    ResourceRecordSet<CNAMEData> lessTTL = ResourceRecordSet.<CNAMEData>builder()
        .name(europe.name())
        .type(europe.type())
        .qualifier(europe.qualifier())
        .ttl(600)
        .addAll(europe.records())
        .geo(europe.geo()).build();
    api.put(lessTTL);

    server.assertSoapBody(getAvailableRegions);
    server.assertSoapBody(getDirectionalDNSRecordsForGroup);
    server.assertSoapBody(getDirectionalDNSRecordsForGroupEuropeIPV6);
    server.assertSoapBody(format(
        updateDirectionalPoolRecordTemplate,
        "A000000000000001",
        600,
        "www-000000001.eu-west-1.elb.amazonaws.com.",
        "<GeolocationGroupDetails groupName=\"Europe\"><GeolocationGroupDefinitionData regionName=\"Europe\" territoryNames=\"Aland Islands;Albania;Andorra;Armenia;Austria;Azerbaijan;Belarus;Belgium;Bosnia-Herzegovina;Bulgaria;Croatia;Czech Republic;Denmark;Estonia;Faroe Islands;Finland;France;Georgia;Germany;Gibraltar;Greece;Guernsey;Hungary;Iceland;Ireland;Isle of Man;Italy;Jersey;Latvia;Liechtenstein;Lithuania;Luxembourg;Macedonia, the former Yugoslav Republic of;Malta;Moldova, Republic of;Monaco;Montenegro;Netherlands;Norway;Poland;Portugal;Romania;San Marino;Serbia;Slovakia;Slovenia;Spain;Svalbard and Jan Mayen;Sweden;Switzerland;Ukraine;Undefined Europe;United Kingdom - England, Northern Ireland, Scotland, Wales;Vatican City\" /></GeolocationGroupDetails><forceOverlapTransfer>true</forceOverlapTransfer>"));
  }

  void assertUS(ResourceRecordSet<?> actual) {
    assertThat(actual)
        .hasName("www.denominator.io.")
        .hasType("CNAME")
        .hasQualifier("US")
        .hasTtl(300)
        .containsRegion("United States (US)", "Alabama", "Alaska", "Arizona", "Arkansas",
                        "Armed Forces Americas", "Armed Forces Europe, Middle East, and Canada",
                        "Armed Forces Pacific", "California", "Colorado", "Connecticut", "Delaware",
                        "District of Columbia", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois",
                        "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland",
                        "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri",
                        "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey",
                        "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
                        "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina",
                        "South Dakota", "Tennessee", "Texas", "Undefined United States",
                        "United States Minor Outlying Islands", "Utah", "Vermont", "Virginia",
                        "Washington", "West Virginia", "Wisconsin", "Wyoming")
        .containsExactlyRecords(CNAMEData.create("www-000000001.us-east-1.elb.amazonaws.com."));
  }

  void assertEurope(ResourceRecordSet<?> actual) {
    assertThat(actual).isEqualTo(europe);
  }

  void assertEverywhereElse(ResourceRecordSet<?> actual) {
    assertThat(actual)
        .hasName("www.denominator.io.")
        .hasType("CNAME")
        .hasQualifier("Everywhere Else")
        .hasTtl(60)
        .containsRegion("Anonymous Proxy (A1)", "Anonymous Proxy")
        .containsRegion("Mexico", "Mexico")
        .containsRegion("Satellite Provider (A2)", "Satellite Provider")
        .containsRegion("Unknown / Uncategorized IPs", "Unknown / Uncategorized IPs")
        .containsRegion("Canada (CA)", "Alberta", "British Columbia", "Greenland", "Manitoba",
                        "New Brunswick", "Newfoundland and Labrador", "Northwest Territories",
                        "Nova Scotia", "Nunavut", "Ontario", "Prince Edward Island", "Quebec",
                        "Saint Pierre and Miquelon", "Saskatchewan", "Undefined Canada", "Yukon")
        .containsRegion("The Caribbean", "Anguilla", "Antigua and Barbuda", "Aruba", "Bahamas",
                        "Barbados", "Bermuda", "British Virgin Islands", "Cayman Islands", "Cuba",
                        "Dominica", "Dominican Republic", "Grenada", "Guadeloupe", "Haiti",
                        "Jamaica", "Martinique", "Montserrat", "Netherlands Antilles",
                        "Puerto Rico", "Saint Barthelemy", "Saint Martin",
                        "Saint Vincent and the Grenadines", "St. Kitts and Nevis", "St. Lucia",
                        "Trinidad and Tobago", "Turks and Caicos Islands", "U.S. Virgin Islands")
        .containsRegion("Central America", "Belize", "Costa Rica", "El Salvador", "Guatemala",
                        "Honduras", "Nicaragua", "Panama", "Undefined Central America")
        .containsRegion("South America", "Argentina", "Bolivia", "Brazil", "Chile", "Colombia",
                        "Ecuador", "Falkland Islands", "French Guiana", "Guyana", "Paraguay",
                        "Peru", "South Georgia and the South Sandwich Islands", "Suriname",
                        "Undefined South America", "Uruguay", "Venezuela, Bolivarian Republic of")
        .containsRegion("Russian Federation", "Russian Federation")
        .containsRegion("Middle East", "Afghanistan", "Bahrain", "Cyprus", "Iran", "Iraq", "Israel",
                        "Jordan", "Kuwait", "Lebanon", "Oman", "Palestinian Territory, Occupied",
                        "Qatar", "Saudi Arabia", "Syrian Arab Republic", "Turkey, Republic of",
                        "Undefined Middle East", "United Arab Emirates", "Yemen")
        .containsRegion("Africa", "Algeria", "Angola", "Benin", "Botswana", "Burkina Faso",
                        "Burundi", "Cameroon", "Cape Verde", "Central African Republic", "Chad",
                        "Comoros", "Congo", "Cote d'Ivoire", "Democratic Republic of the Congo",
                        "Djibouti", "Egypt", "Equatorial Guinea", "Eritrea", "Ethiopia", "Gabon",
                        "Gambia", "Ghana", "Guinea", "Guinea-Bissau", "Kenya", "Lesotho", "Liberia",
                        "Libyan Arab Jamahiriya", "Madagascar", "Malawi", "Mali", "Mauritania",
                        "Mauritius", "Mayotte", "Morocco", "Mozambique", "Namibia", "Niger",
                        "Nigeria", "Reunion", "Rwanda", "Sao Tome and Principe", "Senegal",
                        "Seychelles", "Sierra Leone", "Somalia", "South Africa", "St. Helena",
                        "Sudan", "Swaziland", "Tanzania, United Republic of", "Togo", "Tunisia",
                        "Uganda", "Undefined Africa", "Western Sahara", "Zambia", "Zimbabwe")
        .containsRegion("Asia", "Bangladesh", "Bhutan",
                        "British Indian Ocean Territory - Chagos Islands", "Brunei Darussalam",
                        "Cambodia", "China", "Hong Kong", "India", "Indonesia", "Japan",
                        "Kazakhstan", "Korea, Democratic People's Republic of",
                        "Korea, Republic of", "Kyrgyzstan", "Lao People's Democratic Republic",
                        "Macao", "Malaysia", "Maldives", "Mongolia", "Myanmar", "Nepal", "Pakistan",
                        "Philippines", "Singapore", "Sri Lanka", "Taiwan", "Tajikistan", "Thailand",
                        "Timor-Leste, Democratic Republic of", "Turkmenistan", "Undefined Asia",
                        "Uzbekistan", "Vietnam")
        .containsRegion("Australia / Oceania", "American Samoa", "Australia", "Christmas Island",
                        "Cocos (Keeling) Islands", "Cook Islands", "Fiji", "French Polynesia",
                        "Guam", "Heard Island and McDonald Islands", "Kiribati", "Marshall Islands",
                        "Micronesia , Federated States of", "Nauru", "New Caledonia", "New Zealand",
                        "Niue", "Norfolk Island", "Northern Mariana Islands, Commonwealth of",
                        "Palau", "Papua New Guinea", "Pitcairn", "Samoa", "Solomon Islands",
                        "Tokelau", "Tonga", "Tuvalu", "Undefined Australia / Oceania", "Vanuatu",
                        "Wallis and Futuna")
        .containsRegion("Antarctica", "Antarctica", "Bouvet Island", "French Southern Territories")
        .containsExactlyRecords(
            CNAMEData.create("www-000000002.us-east-1.elb.amazonaws.com."));
  }

  ResourceRecordSet<CNAMEData> europe = ResourceRecordSet
      .<CNAMEData>builder()
      .name("www.denominator.io.")
      .type("CNAME")
      .qualifier("Europe")
      .ttl(300)
      .add(CNAMEData.create("www-000000001.eu-west-1.elb.amazonaws.com."))
      .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
        {
          put("Europe", Arrays
              .asList("Aland Islands", "Albania", "Andorra", "Armenia", "Austria", "Azerbaijan",
                      "Belarus", "Belgium", "Bosnia-Herzegovina", "Bulgaria", "Croatia",
                      "Czech Republic", "Denmark", "Estonia", "Faroe Islands", "Finland", "France",
                      "Georgia", "Germany", "Gibraltar", "Greece", "Guernsey", "Hungary", "Iceland",
                      "Ireland", "Isle of Man", "Italy", "Jersey", "Latvia", "Liechtenstein",
                      "Lithuania", "Luxembourg", "Macedonia, the former Yugoslav Republic of",
                      "Malta", "Moldova, Republic of", "Monaco", "Montenegro", "Netherlands",
                      "Norway", "Poland", "Portugal", "Romania", "San Marino", "Serbia", "Slovakia",
                      "Slovenia", "Spain", "Svalbard and Jan Mayen", "Sweden", "Switzerland",
                      "Ukraine", "Undefined Europe",
                      "United Kingdom - England, Northern Ireland, Scotland, Wales",
                      "Vatican City"));
        }
      })).build();

  String getDirectionalDNSGroupDetailsResponseEverywhereElse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "        <soap:Body>\n"
      + "                <ns1:getDirectionalDNSGroupDetailsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "                        <DirectionalDNSGroupDetail xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" GroupName=\"Everywhere Else\">\n"
      + "                                <ns2:DirectionalDNSRegion>\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Anonymous Proxy (A1)\" TerritoryName=\"Anonymous Proxy\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Mexico\" TerritoryName=\"Mexico\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Satellite Provider (A2)\" TerritoryName=\"Satellite Provider\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Unknown / Uncategorized IPs\" TerritoryName=\"Unknown / Uncategorized IPs\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Canada (CA)\" TerritoryName=\"Alberta;British Columbia;Greenland;Manitoba;New Brunswick;Newfoundland and Labrador;Northwest Territories;Nova Scotia;Nunavut;Ontario;Prince Edward Island;Quebec;Saint Pierre and Miquelon;Saskatchewan;Undefined Canada;Yukon\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"The Caribbean\" TerritoryName=\"Anguilla;Antigua and Barbuda;Aruba;Bahamas;Barbados;Bermuda;British Virgin Islands;Cayman Islands;Cuba;Dominica;Dominican Republic;Grenada;Guadeloupe;Haiti;Jamaica;Martinique;Montserrat;Netherlands Antilles;Puerto Rico;Saint Barthelemy;Saint Martin;Saint Vincent and the Grenadines;St. Kitts and Nevis;St. Lucia;Trinidad and Tobago;Turks and Caicos Islands;U.S. Virgin Islands\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Central America\" TerritoryName=\"Belize;Costa Rica;El Salvador;Guatemala;Honduras;Nicaragua;Panama;Undefined Central America\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"South America\" TerritoryName=\"Argentina;Bolivia;Brazil;Chile;Colombia;Ecuador;Falkland Islands;French Guiana;Guyana;Paraguay;Peru;South Georgia and the South Sandwich Islands;Suriname;Undefined South America;Uruguay;Venezuela, Bolivarian Republic of\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Russian Federation\" TerritoryName=\"Russian Federation\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Middle East\" TerritoryName=\"Afghanistan;Bahrain;Cyprus;Iran;Iraq;Israel;Jordan;Kuwait;Lebanon;Oman;Palestinian Territory, Occupied;Qatar;Saudi Arabia;Syrian Arab Republic;Turkey, Republic of;Undefined Middle East;United Arab Emirates;Yemen\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Africa\" TerritoryName=\"Algeria;Angola;Benin;Botswana;Burkina Faso;Burundi;Cameroon;Cape Verde;Central African Republic;Chad;Comoros;Congo;Cote d'Ivoire;Democratic Republic of the Congo;Djibouti;Egypt;Equatorial Guinea;Eritrea;Ethiopia;Gabon;Gambia;Ghana;Guinea;Guinea-Bissau;Kenya;Lesotho;Liberia;Libyan Arab Jamahiriya;Madagascar;Malawi;Mali;Mauritania;Mauritius;Mayotte;Morocco;Mozambique;Namibia;Niger;Nigeria;Reunion;Rwanda;Sao Tome and Principe;Senegal;Seychelles;Sierra Leone;Somalia;South Africa;St. Helena;Sudan;Swaziland;Tanzania, United Republic of;Togo;Tunisia;Uganda;Undefined Africa;Western Sahara;Zambia;Zimbabwe\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Asia\" TerritoryName=\"Bangladesh;Bhutan;British Indian Ocean Territory - Chagos Islands;Brunei Darussalam;Cambodia;China;Hong Kong;India;Indonesia;Japan;Kazakhstan;Korea, Democratic People's Republic of;Korea, Republic of;Kyrgyzstan;Lao People's Democratic Republic;Macao;Malaysia;Maldives;Mongolia;Myanmar;Nepal;Pakistan;Philippines;Singapore;Sri Lanka;Taiwan;Tajikistan;Thailand;Timor-Leste, Democratic Republic of;Turkmenistan;Undefined Asia;Uzbekistan;Vietnam\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Australia / Oceania\" TerritoryName=\"American Samoa;Australia;Christmas Island;Cocos (Keeling) Islands;Cook Islands;Fiji;French Polynesia;Guam;Heard Island and McDonald Islands;Kiribati;Marshall Islands;Micronesia , Federated States of;Nauru;New Caledonia;New Zealand;Niue;Norfolk Island;Northern Mariana Islands, Commonwealth of;Palau;Papua New Guinea;Pitcairn;Samoa;Solomon Islands;Tokelau;Tonga;Tuvalu;Undefined Australia / Oceania;Vanuatu;Wallis and Futuna\" />\n"
      + "                                        <ns2:RegionForNewGroups RegionName=\"Antarctica\" TerritoryName=\"Antarctica;Bouvet Island;French Southern Territories\" />\n"
      + "                                </ns2:DirectionalDNSRegion>\n"
      + "                        </DirectionalDNSGroupDetail>\n"
      + "                </ns1:getDirectionalDNSGroupDetailsResponse>\n"
      + "        </soap:Body>\n"
      + "</soap:Envelope>";

  String getDirectionalDNSGroupDetailsResponseUS =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + " <soap:Body>\n"
      + "         <ns1:getDirectionalDNSGroupDetailsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "                 <DirectionalDNSGroupDetail\n"
      + "                         xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" GroupName=\"US\">\n"
      + "                         <ns2:DirectionalDNSRegion>\n"
      + "                                 <ns2:RegionForNewGroups RegionName=\"United States (US)\" TerritoryName=\"Alabama;Alaska;Arizona;Arkansas;Armed Forces Americas;Armed Forces Europe, Middle East, and Canada;Armed Forces Pacific;California;Colorado;Connecticut;Delaware;District of Columbia;Florida;Georgia;Hawaii;Idaho;Illinois;Indiana;Iowa;Kansas;Kentucky;Louisiana;Maine;Maryland;Massachusetts;Michigan;Minnesota;Mississippi;Missouri;Montana;Nebraska;Nevada;New Hampshire;New Jersey;New Mexico;New York;North Carolina;North Dakota;Ohio;Oklahoma;Oregon;Pennsylvania;Rhode Island;South Carolina;South Dakota;Tennessee;Texas;Undefined United States;United States Minor Outlying Islands;Utah;Vermont;Virginia;Washington;West Virginia;Wisconsin;Wyoming\" />\n"
      + "                         </ns2:DirectionalDNSRegion>\n"
      + "                 </DirectionalDNSGroupDetail>\n"
      + "         </ns1:getDirectionalDNSGroupDetailsResponse>\n"
      + " </soap:Body>\n"
      + "</soap:Envelope>";
}
