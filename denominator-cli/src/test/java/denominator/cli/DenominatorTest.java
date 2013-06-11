package denominator.cli;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import denominator.Credentials;
import denominator.DNSApiManager;
import denominator.cli.Denominator.ListProviders;
import denominator.cli.Denominator.ZoneList;
import denominator.cli.GeoResourceRecordSetCommands.GeoRegionList;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetApplyTTL;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList;
import denominator.cli.GeoResourceRecordSetCommands.GeoTypeList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetApplyTTL;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetDelete;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetGet;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetRemove;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetReplace;
import denominator.mock.MockProvider;

@Test
public class DenominatorTest {

    @Test(description = "denominator -p mock providers")
    public void listsAllProvidersWithCredentials() {
        assertEquals(ListProviders.providerAndCredentialsTable(), Joiner.on('\n').join(
                "provider   url                                                 duplicateZones credentialType credentialArgs",
                "mock       mem:mock                                            false          ",
                "clouddns   https://identity.api.rackspacecloud.com/v2.0/       true           apiKey         username apiKey",
                "dynect     https://api2.dynect.net/REST                        false          password       customer username password",
                "route53    https://route53.amazonaws.com                       true           accessKey      accessKey secretKey",
                "route53    https://route53.amazonaws.com                       true           session        accessKey secretKey sessionToken",
                "ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 false          password       username password", ""));
    }

    DNSApiManager mgr = denominator.Denominator.create(new MockProvider());

    @Test(description = "denominator -p mock zone list")
    public void testZoneList() {
        assertEquals(Joiner.on('\n').join(new ZoneList().doRun(mgr)), "denominator.io.");
    }

    @Test(description = "denominator -u mem:mock2 -p mock zone list")
    public void testUrlArg() {
        ZoneList zoneList = new ZoneList() {
            @Override
            public Iterator<String> doRun(DNSApiManager mgr) {
                assertEquals(mgr.provider().url(), "mem:mock2");
                return Iterators.emptyIterator();
            }
        };
        zoneList.providerName = "mock";
        zoneList.url = "mem:mock2";
        zoneList.run();
    }

    @Test
    public void testCredentialsFromYaml() {
        String yaml = getTestYaml();
        ZoneList zoneList = new ZoneList();
        zoneList.name = "blah1";
        Map<?, ?> credentials = Map.class.cast(zoneList.getConfigFromYaml(yaml).get("credentials"));
        assertEquals(credentials.get("accessKey"), "foo1");
        assertEquals(credentials.get("secretKey"), "foo2");
    }

    private String getTestYaml() {
        return "name: blah1\n" +
            "provider: route53\n" +
            "credentials:\n" +
            "  accessKey: foo1\n" +
            "  secretKey: foo2\n" +
            "---\n" +
            "name: blah2\n" +
            "provider: mock\n" +
            "credentials:\n" +
            "  accessKey: foo3\n" +
            "  secretKey: foo4\n" +
            "  sessionToken: foo5\n" +
            "\n";
    }

    @Test
    public void testFileContentsFromPath() {
        ZoneList zoneList = new ZoneList();
        String contents = null;
        try {
            contents = zoneList.getFileContentsFromPath(getTestConfigPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(contents, getTestYaml());
    }

    @Test(description = "denominator -C test-config.yml -n blah2 zone list")
    public void testConfigArgMock() {
        ZoneList zoneList = new ZoneList() {
            @Override
            public Iterator<String> doRun(DNSApiManager mgr) {
                assertEquals(configPath, getTestConfigPath());
                Map<?, ?> configFromFile = getConfigFromFile();
                assertEquals(configFromFile.get("provider"), "mock");
                Map<?, ?> credentials = Map.class.cast(configFromFile.get("credentials"));
                assertEquals(credentials.get("accessKey"), "foo3");
                assertEquals(credentials.get("secretKey"), "foo4");
                assertEquals(credentials.get("sessionToken"), "foo5");
                return Iterators.emptyIterator();
            }
        };
        zoneList.name = "blah2";
        zoneList.configPath = getTestConfigPath();
        zoneList.run();
    }

    @Test(description = "denominator -C test-config.yml -n blah1 zone list")
    public void testConfigArgRoute53() {
        ZoneList zoneList = new ZoneList() {
            @Override
            public Iterator<String> doRun(DNSApiManager mgr) {
                assertEquals(configPath, getTestConfigPath());
                Map<?, ?> configFromFile = getConfigFromFile();
                assertEquals(configFromFile.get("provider"), "route53");
                Map<?, ?> credentials = Map.class.cast(configFromFile.get("credentials"));
                assertEquals(credentials.get("accessKey"), "foo1");
                assertEquals(credentials.get("secretKey"), "foo2");
                return Iterators.emptyIterator();
            }
        };
        zoneList.name = "blah1";
        zoneList.configPath = getTestConfigPath();
        zoneList.run();
    }

    @Test(description = "denominator -C test-config.yml -p route53 -c user pass -n blah1 zone list")
    public void testConfigArgWithCliOverride() {
        ZoneList zoneList = new ZoneList() {
            @Override
            public Iterator<String> doRun(DNSApiManager mgr) {
                assertEquals(credentialArgs.get(0), "user");
                assertEquals(credentialArgs.get(1), "pass");
                // CLI credential args should override config file args
                assertTrue(credentials instanceof Credentials.ListCredentials);
                assertEquals(Credentials.ListCredentials.class.cast(credentials).get(0), "user");
                assertEquals(Credentials.ListCredentials.class.cast(credentials).get(1), "pass");
                return Iterators.emptyIterator();
            }
        };
        zoneList.name = "blah1";
        zoneList.providerName = "route53";
        zoneList.configPath = getTestConfigPath();
        zoneList.credentialArgs = ImmutableList.of("user", "pass").asList();
        zoneList.run();
    }

    private String getTestConfigPath() {
        URL res = getClass().getClassLoader().getResource("test-config.yml");
        return res != null ? res.getFile() : null;
    }

    @Test(description = "denominator -p mock record -z denominator.io. list")
    public void testResourceRecordSetList() {
        ResourceRecordSetList command = new ResourceRecordSetList();
        command.zoneIdOrName = "denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "denominator.io.                                   NS     86400 ns1.denominator.io.",
                "denominator.io.                                   SOA    3600  ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60",
                "www.denominator.io.                               CNAME  3600  www1.denominator.io.",
                "www1.denominator.io.                              A      3600  192.0.2.1",
                "www1.denominator.io.                              A      3600  192.0.2.2",
                "www2.denominator.io.                              A      3600  198.51.100.1"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. list -n www1.denominator.io.")
    public void testResourceRecordSetListByName() {
        ResourceRecordSetList command = new ResourceRecordSetList();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "www1.denominator.io.                              A      3600  192.0.2.1",
                "www1.denominator.io.                              A      3600  192.0.2.2"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. get -n www1.denominator.io. -t A ")
    public void testResourceRecordSetGetWhenPresent() {
        ResourceRecordSetGet command = new ResourceRecordSetGet();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "www1.denominator.io.                              A      3600  192.0.2.1",
                "www1.denominator.io.                              A      3600  192.0.2.2"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. get -n www3.denominator.io. -t A ")
    public void testResourceRecordSetGetWhenAbsent() {
        ResourceRecordSetGet command = new ResourceRecordSetGet();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www3.denominator.io.";
        command.type = "A";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), "");
    }

    @Test(description = "denominator -p mock record -z denominator.io. applyttl -n www3.denominator.io. -t A 10000")
    public void testResourceRecordSetApplyTTL() {
        ResourceRecordSetApplyTTL command = new ResourceRecordSetApplyTTL();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www3.denominator.io.";
        command.type = "A";
        command.ttl = 10000;
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. applying ttl 10000 to rrset www3.denominator.io. A",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2")
    public void testResourceRecordSetAdd() {
        ResourceRecordSetAdd command = new ResourceRecordSetAdd();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.values = ImmutableList.of("192.0.2.1", "192.0.2.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1},{address=192.0.2.2}]",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ttl 3600 -d 192.0.2.1 -d 192.0.2.2")
    public void testResourceRecordSetAddWithTTL() {
        ResourceRecordSetAdd command = new ResourceRecordSetAdd();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.ttl = 3600;
        command.values = ImmutableList.of("192.0.2.1", "192.0.2.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1},{address=192.0.2.2}] applying ttl 3600",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. replace -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2")
    public void testResourceRecordSetReplace() {
        ResourceRecordSetReplace command = new ResourceRecordSetReplace();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.values = ImmutableList.of("192.0.2.1", "192.0.2.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. replacing rrset www1.denominator.io. A with values: [{address=192.0.2.1},{address=192.0.2.2}]",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. replace -n www1.denominator.io. -t A --ttl 3600 -d 192.0.2.1 -d 192.0.2.2")
    public void testResourceRecordSetReplaceWithTTL() {
        ResourceRecordSetReplace command = new ResourceRecordSetReplace();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.ttl = 3600;
        command.values = ImmutableList.of("192.0.2.1", "192.0.2.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. replacing rrset www1.denominator.io. A with values: [{address=192.0.2.1},{address=192.0.2.2}] and ttl 3600",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. remove -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2")
    public void testResourceRecordSetRemove() {
        ResourceRecordSetRemove command = new ResourceRecordSetRemove();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.values = ImmutableList.of("192.0.2.1", "192.0.2.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=192.0.2.1},{address=192.0.2.2}]",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. delete -n www3.denominator.io. -t A ")
    public void testResourceRecordSetDelete() {
        ResourceRecordSetDelete command = new ResourceRecordSetDelete();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www3.denominator.io.";
        command.type = "A";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. deleting rrset www3.denominator.io. A",
                ";; ok"));
    }

    @Test(description = "denominator -p mock geo -z denominator.io. types")
    public void testGeoTypeList() {
        GeoTypeList command = new GeoTypeList();
        command.zoneIdOrName = "denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "A",
                "CNAME"));
    }

    @Test(description = "denominator -p mock geo -z denominator.io. regions")
    public void testGeoRegionList() {
        GeoRegionList command = new GeoRegionList();
        command.zoneIdOrName = "denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "Anonymous Proxy (A1)        : Anonymous Proxy",
                "Satellite Provider (A2)     : Satellite Provider",
                "Unknown / Uncategorized IPs : Unknown / Uncategorized IPs",
                "United States (US)          : Alabama;Alaska;Arizona;Arkansas;Armed Forces Americas;Armed Forces Europe, Middle East, and Canada;Armed Forces Pacific;California;Colorado;Connecticut;Delaware;District of Columbia;Florida;Georgia;Hawaii;Idaho;Illinois;Indiana;Iowa;Kansas;Kentucky;Louisiana;Maine;Maryland;Massachusetts;Michigan;Minnesota;Mississippi;Missouri;Montana;Nebraska;Nevada;New Hampshire;New Jersey;New Mexico;New York;North Carolina;North Dakota;Ohio;Oklahoma;Oregon;Pennsylvania;Rhode Island;South Carolina;South Dakota;Tennessee;Texas;Undefined United States;United States Minor Outlying Islands;Utah;Vermont;Virginia;Washington;West Virginia;Wisconsin;Wyoming",
                "Mexico                      : Mexico",
                "Canada (CA)                 : Alberta;British Columbia;Greenland;Manitoba;New Brunswick;Newfoundland and Labrador;Northwest Territories;Nova Scotia;Nunavut;Ontario;Prince Edward Island;Quebec;Saint Pierre and Miquelon;Saskatchewan;Undefined Canada;Yukon",
                "The Caribbean               : Anguilla;Antigua and Barbuda;Aruba;Bahamas;Barbados;Bermuda;British Virgin Islands;Cayman Islands;Cuba;Dominica;Dominican Republic;Grenada;Guadeloupe;Haiti;Jamaica;Martinique;Montserrat;Netherlands Antilles;Puerto Rico;Saint Barthelemy;Saint Martin;Saint Vincent and the Grenadines;St. Kitts and Nevis;St. Lucia;Trinidad and Tobago;Turks and Caicos Islands;U.S. Virgin Islands",
                "Central America             : Belize;Costa Rica;El Salvador;Guatemala;Honduras;Nicaragua;Panama;Undefined Central America",
                "South America               : Argentina;Bolivia;Brazil;Chile;Colombia;Ecuador;Falkland Islands;French Guiana;Guyana;Paraguay;Peru;South Georgia and the South Sandwich Islands;Suriname;Undefined South America;Uruguay;Venezuela, Bolivarian Republic of",
                "Europe                      : Aland Islands;Albania;Andorra;Armenia;Austria;Azerbaijan;Belarus;Belgium;Bosnia-Herzegovina;Bulgaria;Croatia;Czech Republic;Denmark;Estonia;Faroe Islands;Finland;France;Georgia;Germany;Gibraltar;Greece;Guernsey;Hungary;Iceland;Ireland;Isle of Man;Italy;Jersey;Latvia;Liechtenstein;Lithuania;Luxembourg;Macedonia, the former Yugoslav Republic of;Malta;Moldova, Republic of;Monaco;Montenegro;Netherlands;Norway;Poland;Portugal;Romania;San Marino;Serbia;Slovakia;Slovenia;Spain;Svalbard and Jan Mayen;Sweden;Switzerland;Ukraine;Undefined Europe;United Kingdom - England, Northern Ireland, Scotland, Wales;Vatican City",
                "Russian Federation          : Russian Federation",
                "Middle East                 : Afghanistan;Bahrain;Cyprus;Iran;Iraq;Israel;Jordan;Kuwait;Lebanon;Oman;Palestinian Territory, Occupied;Qatar;Saudi Arabia;Syrian Arab Republic;Turkey, Republic of;Undefined Middle East;United Arab Emirates;Yemen",
                "Africa                      : Algeria;Angola;Benin;Botswana;Burkina Faso;Burundi;Cameroon;Cape Verde;Central African Republic;Chad;Comoros;Congo;Cote d'Ivoire;Democratic Republic of the Congo;Djibouti;Egypt;Equatorial Guinea;Eritrea;Ethiopia;Gabon;Gambia;Ghana;Guinea;Guinea-Bissau;Kenya;Lesotho;Liberia;Libyan Arab Jamahiriya;Madagascar;Malawi;Mali;Mauritania;Mauritius;Mayotte;Morocco;Mozambique;Namibia;Niger;Nigeria;Reunion;Rwanda;Sao Tome and Principe;Senegal;Seychelles;Sierra Leone;Somalia;South Africa;St. Helena;Sudan;Swaziland;Tanzania, United Republic of;Togo;Tunisia;Uganda;Undefined Africa;Western Sahara;Zambia;Zimbabwe",
                "Asia                        : Bangladesh;Bhutan;British Indian Ocean Territory - Chagos Islands;Brunei Darussalam;Cambodia;China;Hong Kong;India;Indonesia;Japan;Kazakhstan;Korea, Democratic People's Republic of;Korea, Republic of;Kyrgyzstan;Lao People's Democratic Republic;Macao;Malaysia;Maldives;Mongolia;Myanmar;Nepal;Pakistan;Philippines;Singapore;Sri Lanka;Taiwan;Tajikistan;Thailand;Timor-Leste, Democratic Republic of;Turkmenistan;Undefined Asia;Uzbekistan;Vietnam",
                "Australia / Oceania         : American Samoa;Australia;Christmas Island;Cocos (Keeling) Islands;Cook Islands;Fiji;French Polynesia;Guam;Heard Island and McDonald Islands;Kiribati;Marshall Islands;Micronesia , Federated States of;Nauru;New Caledonia;New Zealand;Niue;Norfolk Island;Northern Mariana Islands, Commonwealth of;Palau;Papua New Guinea;Pitcairn;Samoa;Solomon Islands;Tokelau;Tonga;Tuvalu;Undefined Australia / Oceania;Vanuatu;Wallis and Futuna",
                "Antarctica                  : Antarctica;Bouvet Island;French Southern Territories"));
    }

    @Test(description = "denominator -p mock geo -z denominator.io. list")
    public void testGeoResourceRecordSetList() {
        GeoResourceRecordSetList command = new GeoResourceRecordSetList();
        command.zoneIdOrName = "denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "www.geo.denominator.io.                           CNAME  0     c.denominator.io. antarctica {Antarctica=[Bouvet Island, French Southern Territories, Antarctica]}",
                "www.geo.denominator.io.                           CNAME  300   a.denominator.io. alazona {United States (US)=[Alaska, Arizona]}",
                "www.geo.denominator.io.                           CNAME  86400 b.denominator.io. columbador {South America=[Colombia, Ecuador]}",
                "www2.geo.denominator.io.                          A      300   192.0.2.1 alazona {United States (US)=[Alaska, Arizona]}"));
    }

    @Test(description = "denominator -p mock geo -z denominator.io. list -n www.geo.denominator.io.")
    public void testGeoResourceRecordSetListByName() {
        GeoResourceRecordSetList command = new GeoResourceRecordSetList();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www.geo.denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "www.geo.denominator.io.                           CNAME  0     c.denominator.io. antarctica {Antarctica=[Bouvet Island, French Southern Territories, Antarctica]}",
                "www.geo.denominator.io.                           CNAME  300   a.denominator.io. alazona {United States (US)=[Alaska, Arizona]}",
                "www.geo.denominator.io.                           CNAME  86400 b.denominator.io. columbador {South America=[Colombia, Ecuador]}"));
    }

    @Test(description = "denominator -p mock geo -z denominator.io. list -n www.geo.denominator.io. -t CNAME")
    public void testGeoResourceRecordSetListByNameAndType() {
        GeoResourceRecordSetList command = new GeoResourceRecordSetList();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www.geo.denominator.io.";
        command.type = "CNAME";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "www.geo.denominator.io.                           CNAME  0     c.denominator.io. antarctica {Antarctica=[Bouvet Island, French Southern Territories, Antarctica]}",
                "www.geo.denominator.io.                           CNAME  300   a.denominator.io. alazona {United States (US)=[Alaska, Arizona]}",
                "www.geo.denominator.io.                           CNAME  86400 b.denominator.io. columbador {South America=[Colombia, Ecuador]}"));
    }

    @Test(description = "denominator -p mock geo -z denominator.io. get -n www.geo.denominator.io. -t CNAME -g alazona")
    public void testGeoResourceRecordSetGetWhenPresent() {
        GeoResourceRecordSetGet command = new GeoResourceRecordSetGet();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www.geo.denominator.io.";
        command.type = "CNAME";
        command.group = "alazona";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)),
                "www.geo.denominator.io.                           CNAME  300   a.denominator.io. alazona {United States (US)=[Alaska, Arizona]}");
    }

    @Test(description = "denominator -p mock geo -z denominator.io. get -n www.geo.denominator.io. -t A -g alazona")
    public void testGeoResourceRecordSetGetWhenAbsent() {
        GeoResourceRecordSetGet command = new GeoResourceRecordSetGet();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www.geo.denominator.io.";
        command.type = "A";
        command.group = "alazona";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), "");
    }

    @Test(description = "denominator -p mock geo -z denominator.io. applyttl -n www2.geo.denominator.io. -t A -g alazona 300")
    public void testGeoResourceRecordSetApplyTTL() {
        GeoResourceRecordSetApplyTTL command = new GeoResourceRecordSetApplyTTL();
        command.zoneIdOrName = "denominator.io.";
        command.name = "www2.geo.denominator.io.";
        command.type = "A";
        command.group = "alazona";
        command.ttl = 300;
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. applying ttl 300 to rrset www2.geo.denominator.io. A alazona",
                ";; ok"));
        assertEquals(mgr.api().geoRecordSetsInZone(command.zoneIdOrName).get()
                        .getByNameTypeAndGroup(command.name, command.type, command.group).get()
                        .ttl().get(), Integer.valueOf(command.ttl));
    }
}
