package denominator.cli;

import denominator.AllProfileResourceRecordSetApi;
import denominator.Credentials.ListCredentials;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.cli.Denominator.ListProviders;
import denominator.cli.GeoResourceRecordSetCommands.GeoRegionList;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions;
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
import denominator.cli.ZoneCommands.ZoneAdd;
import denominator.cli.ZoneCommands.ZoneDelete;
import denominator.cli.ZoneCommands.ZoneList;
import denominator.cli.ZoneCommands.ZoneUpdate;
import denominator.common.Util;
import denominator.mock.MockProvider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.route53.AliasTarget;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cert;
import static denominator.model.ResourceRecordSets.srv;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;


public class DenominatorTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  // Use this to make sure the message logged
  @Rule
  public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  // use this to test exit occurred on malformed
  @Rule
  public final ExpectedSystemExit exit = ExpectedSystemExit.none();

  DNSApiManager mgr = denominator.Denominator.create(new MockProvider());

  @Test // denominator providers
  public void listsAllProvidersWithCredentials() {
    assertThat(ListProviders.providerAndCredentialsTable())
        .isEqualTo(Util.join('\n',
                             "provider   url                                                 duplicateZones credentialType credentialArgs",
                             "clouddns   https://identity.api.rackspacecloud.com/v2.0        false          password       username password",
                             "clouddns   https://identity.api.rackspacecloud.com/v2.0        false          apiKey         username apiKey",
                             "designate  http://localhost:5000/v2.0                          false          password       tenantId username password",
                             "dynect     https://api2.dynect.net/REST                        false          password       customer username password",
                             "mock       mem:mock                                            false          ",
                             "route53    https://route53.amazonaws.com                       true           accessKey      accessKey secretKey",
                             "route53    https://route53.amazonaws.com                       true           session        accessKey secretKey sessionToken",
                             "ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 false          password       username password",
                             ""));
  }

  @Test // denominator -p mock zone list
  public void testZoneList() {
    assertThat(new ZoneList().doRun(mgr)).containsExactly(
        "denominator.io.          denominator.io.                      nil@denominator.io.                  86400"
    );
  }

  @Test // denominator -p mock zone list -n denominator.com.
  public void testZoneListByNameWhenEmpty() {
    ZoneList zoneList = new ZoneList();
    zoneList.name = "denominator.com.";
    assertThat(zoneList.doRun(mgr)).isEmpty();
  }

  @Test // denominator -u mem:mock2 -p mock zone list
  public void testUrlArg() {
    ZoneList zoneList = new ZoneList() {
      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        assertThat(mgr.provider().url()).isEqualTo("mem:mock2");
        return Collections.<String>emptyList().iterator();
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
    zoneList.providerConfigurationName = "blah1";
    Map<String, String> credentials = (Map) zoneList.getConfigFromYaml(yaml).get("credentials");
    assertThat(credentials)
        .containsEntry("accessKey", "foo1")
        .containsEntry("secretKey", "foo2");
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
           "url: mem:mock2\n" +
           "credentials:\n" +
           "  accessKey: foo3\n" +
           "  secretKey: foo4\n" +
           "  sessionToken: foo5\n" +
           "\n";
  }

  @Test
  public void testFileContentsFromPath() throws IOException {
    ZoneList zoneList = new ZoneList();
    assertThat(zoneList.getFileContentsFromPath(getTestConfigPath()))
        .isEqualTo(getTestYaml());
  }

  @Test
  public void testOverrideFromEnv() {
    ZoneList zoneList = new ZoneList();
    Map<String, String> env = new LinkedHashMap<String, String>();
    env.put("DENOMINATOR_PROVIDER", "route53");
    env.put("DENOMINATOR_URL", "mem:mock2");
    env.put("DENOMINATOR_ACCESS_KEY", "foo1");
    env.put("DENOMINATOR_SECRET_KEY", "foo2");
    zoneList.overrideFromEnv(env);

    assertThat(zoneList.providerName).isEqualTo("route53");
    assertThat(zoneList.url).isEqualTo("mem:mock2");

    assertThat((Map<String, Object>) zoneList.credentials)
        .isInstanceOf(MapCredentials.class)
        .containsEntry("accessKey", "foo1")
        .containsEntry("secretKey", "foo2");
  }

  @Test // denominator -C test-config.yml -n blah2 zone list
  public void testConfigArgMock() {
    ZoneList zoneList = new ZoneList() {
      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        assertThat(configPath).isEqualTo(getTestConfigPath());
        Map<String, Object> contents = (Map) getConfigFromFile();
        assertThat(contents)
            .containsEntry("provider", "mock")
            .containsEntry("url", "mem:mock2");
        Map<String, String> credentials = (Map) contents.get("credentials");
        assertThat(credentials)
            .containsEntry("accessKey", "foo3")
            .containsEntry("secretKey", "foo4")
            .containsEntry("sessionToken", "foo5");
        return Collections.<String>emptyList().iterator();
      }
    };

    zoneList.providerConfigurationName = "blah2";
    zoneList.configPath = getTestConfigPath();
    zoneList.run();
  }

  @Test // denominator -C test-config.yml -n blah1 zone list
  public void testConfigArgRoute53() {
    ZoneList zoneList = new ZoneList() {
      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        assertThat(configPath).isEqualTo(getTestConfigPath());
        assertThat(Map.class.cast(credentials))
            .containsEntry("accessKey", "foo1")
            .containsEntry("secretKey", "foo2");
        return Collections.<String>emptyList().iterator();
      }
    };
    zoneList.providerConfigurationName = "blah1";
    zoneList.configPath = getTestConfigPath();
    zoneList.run();
  }

  @Test
  public void testParseCertAndPrivateKeyInCredentials() {
    ZoneList zoneList = new ZoneList() {
      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        assertThat(configPath).isEqualTo(getTestConfigPath("test-config-cert.yml"));
        Map mapCredentials = Map.class.cast(credentials);

        assertThat(((X509Certificate) mapCredentials.get("x509Certificate")).getSubjectDN().getName())
            .containsIgnoringCase("C=US,ST=California,O=NetflixOSS,OU=Denominator,CN=Denominator");

        PrivateKey privateKey = (PrivateKey) mapCredentials.get("privateKey");
        assertThat(privateKey.getAlgorithm()).isEqualToIgnoringCase("RSA");
        assertThat(privateKey.getFormat()).isEqualToIgnoringCase("PKCS#8");

        return Collections.<String>emptyList().iterator();
      }
    };
    zoneList.providerConfigurationName = "blah";
    zoneList.configPath = getTestConfigPath("test-config-cert.yml");
    zoneList.run();
  }

  @Test
  // denominator -p mock zone add -n denominator.io. -e nil@denominator.io
  public void testZoneAdd() {
    ZoneAdd command = new ZoneAdd();
    command.name = "denominator.io.";
    command.email = "nil@denominator.io";

    assertThat(command.doRun(mgr)).containsExactly(
        ";; adding zone denominator.io. with ttl 86400 and email nil@denominator.io",
        "denominator.io.",
        ";; ok");

    assertThat(mgr.api().zones().iterateByName(command.name))
        .contains(Zone.create(command.name, command.name, command.ttl, command.email));
  }

  @Test
  // denominator -p mock zone add -n denominator.io. -t 3601 -e nil@denominator.io
  public void testZoneAdd_ttl() {
    ZoneAdd command = new ZoneAdd();
    command.name = "denominator.io.";
    command.ttl = 3601;
    command.email = "nil@denominator.io";

    assertThat(command.doRun(mgr)).containsExactly(
        ";; adding zone denominator.io. with ttl 3601 and email nil@denominator.io",
        "denominator.io.",
        ";; ok");

    assertThat(mgr.api().zones().iterateByName(command.name))
        .contains(Zone.create(command.name, command.name, command.ttl, command.email));
  }

  @Test
  // denominator -p mock zone update -i denominator.io. -e nil@denominator.io
  public void testZoneUpdate_email() {
    ZoneUpdate command = new ZoneUpdate();
    command.id = "denominator.io.";
    command.email = "nil@denominator.io";

    Zone before = mgr.api().zones().iterateByName(command.id).next();

    assertThat(command.doRun(mgr)).containsExactly(
        ";; updating zone denominator.io. with email nil@denominator.io",
        ";; ok");

    assertThat(mgr.api().zones().iterateByName(before.name()))
        .contains(Zone.create(before.id(), before.name(), before.ttl(), command.email));
  }

  @Test
  // denominator -p mock zone update -i denominator.io. -t 3601
  public void testZoneUpdate_ttl() {
    ZoneUpdate command = new ZoneUpdate();
    command.id = "denominator.io.";
    command.ttl = 3601;

    Zone before = mgr.api().zones().iterateByName(command.id).next();

    assertThat(command.doRun(mgr)).containsExactly(
        ";; updating zone denominator.io. with ttl 3601",
        ";; ok");

    assertThat(mgr.api().zones().iterateByName(before.name()))
        .contains(Zone.create(before.id(), before.name(), command.ttl, before.email()));
  }

  @Test
  // denominator -p mock zone update -i denominator.io. -t 3601 -e nil@denominator.io
  public void testZoneUpdate_ttlAndEmail() {
    ZoneUpdate command = new ZoneUpdate();
    command.id = "denominator.io.";
    command.ttl = 3601;
    command.email = "nil@denominator.io";

    Zone before = mgr.api().zones().iterateByName(command.id).next();

    assertThat(command.doRun(mgr)).containsExactly(
        ";; updating zone denominator.io. with ttl 3601 and email nil@denominator.io",
        ";; ok");

    assertThat(mgr.api().zones().iterateByName(before.name()))
        .contains(Zone.create(before.id(), before.name(), command.ttl, command.email));
  }

  @Test
  // denominator -p mock zone update -i denominator.io. -t 3601 -e nil@denominator.io
  public void testZoneUpdate_noop() {
    Zone before = mgr.api().zones().iterateByName("denominator.io.").next();

    ZoneUpdate command = new ZoneUpdate();
    command.id = before.id();
    command.ttl = before.ttl();
    command.email = before.email();

    assertThat(command.doRun(mgr)).containsExactly(
        ";; ok");

    assertThat(mgr.api().zones().iterateByName(before.name()))
        .contains(before);
  }

  @Test
  // denominator -p mock zone update -i denominator.com. -e nil@denominator.io
  public void testZoneUpdate_doesntExist() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("zone denominator.com. not found");

    ZoneUpdate command = new ZoneUpdate();
    command.id = "denominator.com.";
    command.email = "nil@denominator.io";

    command.doRun(mgr);
  }

  @Test
  // denominator -p mock zone delete -i denominator.io.
  public void testZoneDelete() {
    ZoneDelete command = new ZoneDelete();
    command.id = "denominator.io.";

    assertThat(command.doRun(mgr)).containsExactly(
        ";; deleting zone denominator.io.",
        ";; ok");

    assertThat(mgr.api().zones()).isEmpty();
  }

  @Test // denominator -C test-config.yml -p route53 -c user pass -n blah1 zone list
  public void testConfigArgWithCliOverride() {
    ZoneList zoneList = new ZoneList() {
      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        assertThat(credentialArgs).containsExactly("user", "pass");
        // CLI credential args should override config file args
        assertThat(credentials).isInstanceOf(ListCredentials.class);
        assertThat((ListCredentials) credentials).containsExactly("user", "pass");
        return Collections.<String>emptyList().iterator();
      }
    };
    zoneList.providerConfigurationName = "blah1";
    zoneList.providerName = "route53";
    zoneList.configPath = getTestConfigPath();
    zoneList.credentialArgs = asList("user", "pass");
    zoneList.run();
  }

  @Test
  public void testParseCertAndPrivateKeyInCLICredentials() throws IOException {
    ZoneList zoneList = new ZoneList() {
      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        ListCredentials listCredentials = (ListCredentials) credentials;

        assertThat(((X509Certificate) listCredentials.get(0)).getSubjectDN().getName())
            .containsIgnoringCase("C=US,ST=California,O=NetflixOSS,OU=Denominator,CN=Denominator");

        PrivateKey privateKey = (PrivateKey) listCredentials.get(1);
        assertThat(privateKey.getAlgorithm()).isEqualToIgnoringCase("RSA");
        assertThat(privateKey.getFormat()).isEqualToIgnoringCase("PKCS#8");

        return Collections.<String>emptyList().iterator();
      }
    };
    zoneList.providerConfigurationName = "blah";
    zoneList.providerName = "mock";
    Map configFromYaml = zoneList.getConfigFromYaml(
        zoneList.getFileContentsFromPath(getTestConfigPath("test-config-cert.yml")));
    Map credentials = Map.class.cast(configFromYaml.get("credentials"));
    zoneList.credentialArgs = asList(credentials.get("x509Certificate").toString(),
        credentials.get("privateKey").toString());
    zoneList.run();
  }

  private String getTestConfigPath() {
    return getTestConfigPath("test-config.yml");
  }

  private String getTestConfigPath(String fileName) {
    URL res = getClass().getClassLoader().getResource(fileName);
    return res != null ? res.getFile() : null;
  }

  @Test // denominator -p mock record -z denominator.io. list
  public void testResourceRecordSetList() {
    ResourceRecordSetList command = new ResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "a.denominator.io.                                 A      alazona             null  192.0.2.1",
        "denominator.io.                                   NS                         86400 ns1.denominator.io.",
        "denominator.io.                                   SOA                        86400 ns1.denominator.io. nil@denominator.io. 1 3600 600 604800 86400",
        "server1.denominator.io.                           CERT                       3600  12345 1 1 B33F",
        "server1.denominator.io.                           SRV                        3600  0 1 80 www.denominator.io.",
        "www.geo.denominator.io.                           CNAME  alazona             86400 a.denominator.io.",
        "www.geo.denominator.io.                           CNAME  allElse             600   b.denominator.io.",
        "www1.denominator.io.                              A                          3600  192.0.2.1\n"
        + "www1.denominator.io.                              A                          3600  192.0.2.2");
  }

  @Test // denominator -p mock record -z denominator.io. list -n server1.denominator.io.
  public void testResourceRecordSetListByName() {
    ResourceRecordSetList command = new ResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "server1.denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "server1.denominator.io.                           CERT                       3600  12345 1 1 B33F",
        "server1.denominator.io.                           SRV                        3600  0 1 80 www.denominator.io.");
  }

  @Test // denominator -p mock record -z denominator.io. list -n server1.denominator.io. -t SRV
  public void testResourceRecordSetListByNameAndType() {
    ResourceRecordSetList command = new ResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "server1.denominator.io.";
    command.type = "CERT";
    assertThat(command.doRun(mgr)).containsExactly(
        "server1.denominator.io.                           CERT                       3600  12345 1 1 B33F");
  }

  @Test // denominator -p mock record -z denominator.io. get -n server1.denominator.io. -t SRV
  public void testResourceRecordSetGetWhenPresent() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "server1.denominator.io.";
    command.type = "CERT";
    assertThat(command.doRun(mgr)).containsExactly(
        "server1.denominator.io.                           CERT                       3600  12345 1 1 B33F");
  }

  @Test
  public void testResourceRecordSetGet_longName() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = new String(new char[51 - 16]).replace("\0", "a") + ".denominator.io.";
    command.type = "A";

    mgr.api().basicRecordSetsInZone(command.zoneIdOrName).put(a(command.name, asList("192.0.1.1")));

    assertThat(command.doRun(mgr)).containsExactly(
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.denominator.io. A                          null  192.0.1.1");
  }

  @Test
  public void testResourceRecordSetGet_longType() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "server1.denominator.io.";
    command.type = "IPSECKEY";
    command.qualifier = "mavenlover"; // just to ensure type doesn't overrun

    Map<String, Object> rdata = new LinkedHashMap<String, Object>();
    rdata.put("precedence", 1);
    rdata.put("gatetype", 0);
    rdata.put("algorithm", 0);
    rdata.put("gateway", "foo.");
    rdata.put("public_key", "AAAAB3");

    mgr.api().recordSetsInZone(command.zoneIdOrName).put(
        ResourceRecordSet.builder()
            .name(command.name)
            .type(command.type)
            .qualifier(command.qualifier)
            .add(rdata).build());

    assertThat(command.doRun(mgr)).containsExactly(
        "server1.denominator.io.                           IPSECKEY mavenlover          null  1 0 0 foo. AAAAB3");
  }

  @Test
  public void testResourceRecordSetGet_longTtl() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "server1.denominator.io.";
    command.type = "A";

    mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
        .put(a(command.name, 99999999, asList("192.0.1.1")));

    assertThat(command.doRun(mgr)).containsExactly(
        "server1.denominator.io.                           A                          99999999 192.0.1.1");
  }

  @Test
  public void testResourceRecordSetGet_longQualifier() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "server1.denominator.io.";
    command.type = "A";
    command.qualifier = new String(new char[21]).replace("\0", "a");

    mgr.api().recordSetsInZone(command.zoneIdOrName).put(
        ResourceRecordSet.<AData>builder()
            .name(command.name)
            .type(command.type)
            .qualifier(command.qualifier)
            .add(AData.create("192.0.1.1")).build());

    assertThat(command.doRun(mgr)).containsExactly(
        "server1.denominator.io.                           A      aaaaaaaaaaaaaaaaaaaaa null  192.0.1.1");
  }

  @Test // denominator -p mock record -z denominator.io. get -n www3.denominator.io. -t A
  public void testResourceRecordSetGetWhenAbsent() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www3.denominator.io.";
    command.type = "A";
    assertThat(command.doRun(mgr)).containsExactly("");
  }

  @Test
  // denominator -p mock record -z denominator.io. get -n www.geo.denominator.io. -t CNAME -g alazona
  public void testResourceRecordSetGetWhenQualifierPresent() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    command.type = "CNAME";
    command.qualifier = "alazona";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             86400 a.denominator.io. {\"United States\":[\"AK\",\"AZ\"]}");
  }

  @Test
  // denominator -p mock record -z denominator.io. get -n b.denominator.io. -t A -g alazona
  public void testResourceRecordSetGetWhenQualifierAbsent() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "b.denominator.io.";
    command.type = "A";
    command.qualifier = "alazona";
    assertThat(command.doRun(mgr)).containsExactly("");
  }

  @Test // denominator -p mock record -z denominator.io. applyttl -n www3.denominator.io. -t A 10000
  public void testResourceRecordSetApplyTTL() {
    ResourceRecordSetApplyTTL command = new ResourceRecordSetApplyTTL();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ttl = 10000;

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. applying ttl 10000 to rrset www1.denominator.io. A",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(ResourceRecordSet.<AData>builder()
                      .name(command.name)
                      .type(command.type)
                      .ttl(command.ttl)
                      .add(AData.create("192.0.2.1"))
                      .add(AData.create("192.0.2.2")).build());
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void resourceRecordSetAdd() {
    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.1", "192.0.2.2");

    // Ensure the rrset didn't formerly exist.
    mgr.api().basicRecordSetsInZone(command.zoneIdOrName).deleteByNameAndType(command.name,
                                                                              command.type);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
                   .getByNameAndType(command.name, command.type))
        .containsExactlyRecords(AData.create("192.0.2.1"), AData.create("192.0.2.2"));
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void resourceRecordSetAdd_addsOne() {
    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.1", "192.0.2.2");

    // Setup base record with only one value.
    mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
        .put(a(command.name, asList(command.values.get(0))));

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
                   .getByNameAndType(command.name, command.type))
        .containsExactlyRecords(AData.create("192.0.2.1"), AData.create("192.0.2.2"));
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void resourceRecordSetAdd_whenRecordsAlreadyExist() {
    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.1", "192.0.2.2");

    // Setup base record which already has the values.
    mgr.api().basicRecordSetsInZone(command.zoneIdOrName).put(a(command.name, command.values));

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
                   .getByNameAndType(command.name, command.type))
        .containsExactlyRecords(AData.create("192.0.2.1"), AData.create("192.0.2.2"));
  }

  @Test
  // denominator -p route53 record -z denominator.io. add -n www3.denominator.io. -t A --elb-dnsname nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.
  public void testResourceRecordSetAddELBAlias() {
    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www3.denominator.io.";
    command.type = "A";
    command.elbDNSName = "nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.";

    AliasTarget
        target =
        AliasTarget
            .create(ResourceRecordSetAdd.REGION_TO_HOSTEDZONE.get("us-west-2"), command.elbDNSName);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www3.denominator.io. A values: [" + target
        + "]",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(ResourceRecordSet.<AliasTarget>builder()
                      .name(command.name)
                      .type(command.type)
                      .add(target).build());
  }

  @Test
  // denominator -p route53 record -z denominator.io. replace -n www4.denominator.io. -t A --elb-dnsname nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.
  public void testResourceRecordSetReplaceELBAlias() {
    ResourceRecordSetReplace command = new ResourceRecordSetReplace();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www4.denominator.io.";
    command.type = "A";
    command.elbDNSName = "nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.";

    AliasTarget
        target =
        AliasTarget.create(ResourceRecordSetReplace.REGION_TO_HOSTEDZONE.get("us-west-2"),
                           command.elbDNSName);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. replacing rrset www4.denominator.io. A with values: [" + target
        + "]",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(
            ResourceRecordSet.<AliasTarget>builder()
                .name(command.name)
                .type(command.type)
                .add(target).build());
  }

  @Test
  // denominator -p route53 record -z AAAAAAAA replace -n cbp.nccp.us-east-1.dynprod.netflix.net. -t A --alias-hosted-zone-id BBBBBBBB --alias-dnsname cbp.nccp.us-west-2.dynprod.netflix.net.
  public void testResourceRecordSetReplaceRoute53Alias() {
    ResourceRecordSetReplace command = new ResourceRecordSetReplace();
    command.zoneIdOrName = "denominator.io.";
    command.name = "cbp.nccp.us-east-1.dynprod.netflix.net.";
    command.type = "A";
    command.aliasHostedZoneId = "Z3I0BTR7N27QRM";
    command.aliasDNSName = "cbp.nccp.us-west-2.dynprod.netflix.net.";

    AliasTarget target = AliasTarget.create(command.aliasHostedZoneId, command.aliasDNSName);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. replacing rrset cbp.nccp.us-east-1.dynprod.netflix.net. A with values: ["
        + target + "]",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(
            ResourceRecordSet.<AliasTarget>builder()
                .name(command.name)
                .type(command.type)
                .add(target).build());
  }

  @Test
  public void testResourceRecordSetReplaceRoute53AliasForgotZone() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("--alias-hosted-zone-id must be present");

    ResourceRecordSetReplace command = new ResourceRecordSetReplace();
    command.zoneIdOrName = "denominator.io.";
    command.name = "cbp.nccp.us-east-1.dynprod.netflix.net.";
    command.type = "A";
    command.aliasDNSName = "cbp.nccp.us-west-2.dynprod.netflix.net.";
    command.doRun(mgr);
  }

  @Test
  public void testResourceRecordSetReplaceRoute53AliasZoneNameInsteadOfZoneId() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("--alias-hosted-zone-id must be a hosted zone id, not a zone name");

    ResourceRecordSetReplace command = new ResourceRecordSetReplace();
    command.zoneIdOrName = "denominator.io.";
    command.name = "cbp.nccp.us-east-1.dynprod.netflix.net.";
    command.type = "A";
    command.aliasHostedZoneId = "denominator.io.";
    command.aliasDNSName = "cbp.nccp.us-west-2.dynprod.netflix.net.";
    command.doRun(mgr);
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ttl 3600 -d 192.0.2.1 -d 192.0.2.2
  public void testResourceRecordSetAddWithTTL() {
    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ttl = 3600;
    command.values = asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}] applying ttl 3600",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(ResourceRecordSet.<AData>builder()
                      .name(command.name)
                      .type(command.type)
                      .ttl(command.ttl)
                      .add(AData.create("192.0.2.1"))
                      .add(AData.create("192.0.2.2")).build());
  }

  @Test
  // denominator -p mock record -z denominator.io. replace -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void testResourceRecordSetReplace() {
    ResourceRecordSetReplace command = new ResourceRecordSetReplace();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.denominator.io.";
    command.type = "CNAME";
    command.values = asList("www1.denominator.io.", "www2.denominator.io.");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. replacing rrset www.denominator.io. CNAME with values: [{cname=www1.denominator.io.}, {cname=www2.denominator.io.}]",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(ResourceRecordSet.<CNAMEData>builder()
                      .name(command.name)
                      .type(command.type)
                      .add(CNAMEData.create("www1.denominator.io."))
                      .add(CNAMEData.create("www2.denominator.io.")).build());
  }

  @Test
  // denominator -p mock record -z denominator.io. replace -n www1.denominator.io. -t A --ttl 3600 -d 192.0.2.1 -d 192.0.2.2
  public void testResourceRecordSetReplaceWithTTL() {
    ResourceRecordSetReplace command = new ResourceRecordSetReplace();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ttl = 3600;
    command.values = asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. replacing rrset www1.denominator.io. A with values: [{address=192.0.2.1}, {address=192.0.2.2}] and ttl 3600",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(ResourceRecordSet.<AData>builder()
                      .name(command.name)
                      .type(command.type)
                      .ttl(command.ttl)
                      .add(AData.create("192.0.2.1"))
                      .add(AData.create("192.0.2.2")).build());
  }

  @Test
  // denominator -p mock record -z denominator.io. remove -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void testResourceRecordSetRemove() {
    ResourceRecordSetRemove command = new ResourceRecordSetRemove();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type)).isEmpty();
  }


  @Test
  // denominator -p mock record -z denominator.io. remove -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void resourceRecordSetRemove() {
    ResourceRecordSetRemove command = new ResourceRecordSetRemove();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.1", "192.0.2.2");

    // Setup base record which has both values.
    mgr.api().basicRecordSetsInZone(command.zoneIdOrName).put(a(command.name, command.values));

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
                   .getByNameAndType(command.name, command.type)).isNull();
  }

  @Test
  // denominator -p mock record -z denominator.io. remove -n www1.denominator.io. -t A -d 192.0.2.2
  public void resourceRecordSetRemove_removesOne() {
    ResourceRecordSetRemove command = new ResourceRecordSetRemove();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.2");

    // Setup base record which has two values.
    mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
        .put(a(command.name, asList("192.0.2.1", "192.0.2.2")));

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
                   .getByNameAndType(command.name, command.type))
        .containsExactlyRecords(AData.create("192.0.2.1"));
  }

  @Test
  // denominator -p mock record -z denominator.io. remove -n www1.denominator.io. -t A -d 192.0.2.1 -d 192.0.2.2
  public void resourceRecordSetRemove_whenDoesntExist() {
    ResourceRecordSetRemove command = new ResourceRecordSetRemove();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = asList("192.0.2.1", "192.0.2.2");

    // Setup base record which already has the values.
    mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
        .deleteByNameAndType(command.name, command.type);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=192.0.2.1}, {address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().basicRecordSetsInZone(command.zoneIdOrName)
                   .getByNameAndType(command.name, command.type)).isNull();
  }

  @Test // denominator -p mock record -z denominator.io. delete -n www3.denominator.io. -t A
  public void testResourceRecordSetDelete() {
    ResourceRecordSetDelete command = new ResourceRecordSetDelete();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www3.denominator.io.";
    command.type = "A";
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. deleting rrset www3.denominator.io. A",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type)).isEmpty();
  }

  @Test // denominator -p mock geo -z denominator.io. types
  public void testGeoTypeList() {
    GeoTypeList command = new GeoTypeList();
    command.zoneIdOrName = "denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "A", "AAAA", "CERT", "CNAME", "MX", "NAPTR", "NS", "PTR", "SPF", "SRV", "SSHFP", "TXT");
  }

  @Test // denominator -p mock geo -z denominator.io. regions
  public void testGeoRegionList() {
    GeoRegionList command = new GeoRegionList();
    command.zoneIdOrName = "denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "United States               : AL;AK;AS;AZ;AR;AA;AE;AP;CA;CO;CT;DE;DC;FM;FL;GA;GU;HI;ID;IL;IN;IA;KS;KY;LA;ME;MH;MD;MA;MI;MN;MS;MO;MT;NE;NV;NH;NJ;NM;NY;NC;ND;MP;OH;OK;OR;PW;PA;PR;RI;SC;SD;TN;TX;UT;VT;VI;VA;WA;WV;WI;WY",
        "Mexico                      : AG;CM;CP;CH;CA;CL;DU;GJ;GR;HI;JA;MX;MC;MR;NA;OA;PU;QE;SI;SO;TB;TM;TL;VE;YU;ZA"
    );
  }

  @Test // denominator -p mock geo -z denominator.io. list
  @Deprecated
  public void testGeoResourceRecordSetList() {
    GeoResourceRecordSetList command = new GeoResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";

    assertThat(command.doRun(mgr)).containsExactly(
        "a.denominator.io.                                 A      alazona             null  192.0.2.1 {\"United States\":[\"AK\",\"AZ\"]}",
        "www.geo.denominator.io.                           CNAME  alazona             86400 a.denominator.io. {\"United States\":[\"AK\",\"AZ\"]}",
        "www.geo.denominator.io.                           CNAME  allElse             600   b.denominator.io. {\"United States\":[\"AL\",\"AS\",\"AR\",\"AA\",\"AE\",\"AP\",\"CA\",\"CO\",\"CT\",\"DE\",\"DC\",\"FM\",\"FL\",\"GA\",\"GU\",\"HI\",\"ID\",\"IL\",\"IN\",\"IA\",\"KS\",\"KY\",\"LA\",\"ME\",\"MH\",\"MD\",\"MA\",\"MI\",\"MN\",\"MS\",\"MO\",\"MT\",\"NE\",\"NV\",\"NH\",\"NJ\",\"NM\",\"NY\",\"NC\",\"ND\",\"MP\",\"OH\",\"OK\",\"OR\",\"PW\",\"PA\",\"PR\",\"RI\",\"SC\",\"SD\",\"TN\",\"TX\",\"UT\",\"VT\",\"VI\",\"VA\",\"WA\",\"WV\",\"WI\",\"WY\"],\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}");
  }

  @Test // denominator -p mock geo -z denominator.io. list -n www.geo.denominator.io.
  @Deprecated
  public void testGeoResourceRecordSetListByName() {
    GeoResourceRecordSetList command = new GeoResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             86400 a.denominator.io. {\"United States\":[\"AK\",\"AZ\"]}",
        "www.geo.denominator.io.                           CNAME  allElse             600   b.denominator.io. {\"United States\":[\"AL\",\"AS\",\"AR\",\"AA\",\"AE\",\"AP\",\"CA\",\"CO\",\"CT\",\"DE\",\"DC\",\"FM\",\"FL\",\"GA\",\"GU\",\"HI\",\"ID\",\"IL\",\"IN\",\"IA\",\"KS\",\"KY\",\"LA\",\"ME\",\"MH\",\"MD\",\"MA\",\"MI\",\"MN\",\"MS\",\"MO\",\"MT\",\"NE\",\"NV\",\"NH\",\"NJ\",\"NM\",\"NY\",\"NC\",\"ND\",\"MP\",\"OH\",\"OK\",\"OR\",\"PW\",\"PA\",\"PR\",\"RI\",\"SC\",\"SD\",\"TN\",\"TX\",\"UT\",\"VT\",\"VI\",\"VA\",\"WA\",\"WV\",\"WI\",\"WY\"],\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}");
  }

  @Test // denominator -p mock geo -z denominator.io. list -n www.geo.denominator.io. -t CNAME
  @Deprecated
  public void testGeoResourceRecordSetListByNameAndType() {
    GeoResourceRecordSetList command = new GeoResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    assertThat(command.doRun(mgr)).containsExactly(
        "a.denominator.io.                                 A      alazona             null  192.0.2.1 {\"United States\":[\"AK\",\"AZ\"]}");
  }

  @Test
  // denominator -p mock geo -z denominator.io. get -n www.geo.denominator.io. -t CNAME -g alazona
  @Deprecated
  public void testGeoResourceRecordSetGetWhenPresent() {
    GeoResourceRecordSetGet command = new GeoResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    command.type = "CNAME";
    command.group = "alazona";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             86400 a.denominator.io. {\"United States\":[\"AK\",\"AZ\"]}");
  }

  @Test // denominator -p mock geo -z denominator.io. get -n a.denominator.io. -t A -g alazona
  @Deprecated
  public void testGeoResourceRecordSetGetWhenAbsent() {
    GeoResourceRecordSetGet command = new GeoResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    assertThat(command.doRun(mgr)).containsExactly("");
  }

  @Test
  // denominator -p mock geo -z denominator.io. applyttl -n a.denominator.io. -t A -g alazona 10000
  @Deprecated
  public void testGeoResourceRecordSetApplyTTL() {
    GeoResourceRecordSetApplyTTL command = new GeoResourceRecordSetApplyTTL();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.ttl = 10000;

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. applying ttl 10000 to rrset a.denominator.io. A alazona",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .hasTtl(command.ttl);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r '{"Mexico":["AG","CM","CP","CH","CA","CL","DU","GJ","GR","HI","JA","MX","MC","MR","NA","OA","PU","QE","SI","SO","TB","TM","TL","VE","YU","ZA"]}'
  public void testGeoResourceRecordSetAddRegionsEntireRegion() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions =
        "{\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]} to rrset a.denominator.io. A alazona",
        ";; current rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\"]}}}",
        ";; revised rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\"],\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}}}",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .containsRegion("United States", "AK", "AZ")
        .containsRegion("Mexico", mexicanStates(command));

    api.put(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r '{"United States":["AZ"]}'
  public void testGeoResourceRecordSetAddRegionsSkipsWhenSame() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"United States\":[\"AZ\"]}";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"United States\":[\"AZ\"]} to rrset a.denominator.io. A alazona",
        ";; current rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\"]}}}",
        ";; ok");

    assertThat(
        mgr.api().recordSetsInZone(command.zoneIdOrName)
            .getByNameTypeAndQualifier(command.name, command.type, command.group)).isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r '{"Mexico":["AG","CM","CP","CH","CA","CL","DU","GJ","GR","HI","JA","MX","MC","MR","NA","OA","PU","QE","SI","SO","TB","TM","TL","VE","YU","ZA"], "United States":["MD"]}'
  public void testGeoResourceRecordSetAddRegionsEntireRegionAndTerritory() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions =
        "{\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"], \"United States\":[\"MD\"]}";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"], \"United States\":[\"MD\"]} to rrset a.denominator.io. A alazona",
        ";; current rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\"]}}}",
        ";; revised rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\",\"MD\"],\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}}}",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .containsRegion("United States", "AK", "AZ", "MD")
        .containsRegion("Mexico", mexicanStates(command));

    api.put(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r '{"Mexico":["AG","CM","CP","CH","CA","CL","DU","GJ","GR","HI","JA","MX","MC","MR","NA","OA","PU","QE","SI","SO","TB","TM","TL","VE","YU","ZA"]}' --dry-run --validate-regions
  public void testGeoResourceRecordSetAddRegionsEntireRegionAndTerritoryValidatedDryRun() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions =
        "{\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}";
    command.validateRegions = true;
    command.dryRun = true;

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]} to rrset a.denominator.io. A alazona",
        ";; validated regions: {\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}",
        ";; current rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\"]}}}",
        ";; revised rrset: {\"name\":\"a.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States\":[\"AK\",\"AZ\"],\"Mexico\":[\"AG\",\"CM\",\"CP\",\"CH\",\"CA\",\"CL\",\"DU\",\"GJ\",\"GR\",\"HI\",\"JA\",\"MX\",\"MC\",\"MR\",\"NA\",\"OA\",\"PU\",\"QE\",\"SI\",\"SO\",\"TB\",\"TM\",\"TL\",\"VE\",\"YU\",\"ZA\"]}}}",
        ";; ok");

    assertThat(
        mgr.api().recordSetsInZone(command.zoneIdOrName)
            .getByNameTypeAndQualifier(command.name, command.type, command.group)).isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Ru\"]} --validate-regions
  public void testGeoResourceRecordSetAddRegionsValidationFailureOnTerritory() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"Ru\"]}";
    command.validateRegions = true;

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    try {
      Iterator<String> iterator = command.doRun(mgr);
      assertThat(iterator.next())
          .isEqualTo(
              ";; in zone denominator.io. adding regions {\"Mexico\":[\"Ru\"]} to rrset a.denominator.io. A alazona");
      iterator.next();
      fail("should have failed on validation");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unsupported territories in Mexico: [[Ru]]");
    }
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Mexico\"],\"Suth America\":[\"Ecuador\"]} --validate-regions
  public void testGeoResourceRecordSetAddRegionsValidationFailureOnRegion() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"AG\"],\"Suth America\":[\"EC\"]}";
    command.validateRegions = true;

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    try {
      Iterator<String> iterator = command.doRun(mgr);
      assertThat(iterator.next())
          .isEqualTo(
              ";; in zone denominator.io. adding regions {\"Mexico\":[\"AG\"],\"Suth America\":[\"EC\"]} to rrset a.denominator.io. A alazona");
      iterator.next();
      fail("should have failed on validation");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unsupported regions: [Suth America]");
    }
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n a.denominator.io. -t A -g alazona -r Mexico
  public void testGeoResourceRecordSetAddRegionsBadJson() {
    GeoResourceRecordAddRegions command = new GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "a.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "Mexico";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    try {
      command.doRun(mgr);
      fail("should have failed on bad json");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "parse failure on regions! check json syntax. ex. {\"United States\":[\"AZ\"]}");
    }
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .isEqualTo(old);
  }

  @Test
  public void testProxySettingsWithPort() {
    Denominator.DenominatorCommand.setProtocolProxyFromEnv("http", "http://localhost:7878");
    Denominator.DenominatorCommand.setProtocolProxyFromEnv("https", "https://10.0.0.1:8989");
    assertThat(System.getProperty("http.proxyHost")).isEqualTo("localhost");
    assertThat(System.getProperty("http.proxyPort")).isEqualTo("7878");
    assertThat(System.getProperty("https.proxyHost")).isEqualTo("10.0.0.1");
    assertThat(System.getProperty("https.proxyPort")).isEqualTo("8989");
  }

  @Test
  public void testProxySettingsWithDefaultPorts() {
    Denominator.DenominatorCommand.setProtocolProxyFromEnv("http", "http://localhost");
    Denominator.DenominatorCommand.setProtocolProxyFromEnv("https", "https://10.0.0.1");
    assertThat(System.getProperty("http.proxyHost")).isEqualTo("localhost");
    assertThat(System.getProperty("http.proxyPort")).isEqualTo("80");
    assertThat(System.getProperty("https.proxyHost")).isEqualTo("10.0.0.1");
    assertThat(System.getProperty("https.proxyPort")).isEqualTo("443");
  }

  @Test
  public void testProxySettingsFromProperties() {
    System.setProperty("http.proxyHost", "192.168.0.1");
    System.setProperty("https.proxyHost", "192.168.0.2");

    Denominator.DenominatorCommand.setProtocolProxyFromEnv("http", "http://localhost");
    Denominator.DenominatorCommand.setProtocolProxyFromEnv("https", "https://10.0.0.1");

    assertThat(System.getProperty("http.proxyHost")).isEqualTo("192.168.0.1");
    assertThat(System.getProperty("http.proxyPort")).isNull();
    assertThat(System.getProperty("https.proxyHost")).isEqualTo("192.168.0.2");
    assertThat(System.getProperty("https.proxyPort")).isNull();
  }

  @Test
  public void testInvalidEnvProxySettings() {
    exit.expectSystemExitWithStatus(1);
    exit.checkAssertionAfterwards(new Assertion() {
      @Override
      public void checkAssertion() throws Exception {
        assertThat(System.getProperty("http.proxyHost")).isEqualTo("localhost");
        assertThat(System.getProperty("http.proxyPort")).isEqualTo("80");
        assertThat(System.getProperty("https.proxyHost")).isNull();
        assertThat(System.getProperty("https.proxyPort")).isNull();
        assertThat(systemErrRule.getLog())
            .isEqualToIgnoringCase(
                "invalid https proxy configuration: no protocol: 10.0.0.1:8443\n");
      }
    });

    Denominator.DenominatorCommand.setProtocolProxyFromEnv("http", "http://localhost");
    Denominator.DenominatorCommand.setProtocolProxyFromEnv("https", "10.0.0.1:8443");
  }

  @Before
  public void stockRecords() {
    {
      ResourceRecordSetApi api = mgr.api().basicRecordSetsInZone("denominator.io.");
      api.put(a("www1.denominator.io.", 3600, asList("192.0.2.1", "192.0.2.2")));
      api.put(srv("server1.denominator.io.", 3600, "0 1 80 www.denominator.io."));
      api.put(cert("server1.denominator.io.", 3600, "12345 1 1 B33F"));
    }

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone("denominator.io.");
    Map<String, Collection<String>> alazona = new LinkedHashMap<String, Collection<String>>();
    alazona.put("United States", asList("AK", "AZ"));
    api.put(ResourceRecordSet.builder()
                .name("a.denominator.io.")
                .type("A")
                .qualifier("alazona")
                .add(AData.create("192.0.2.1"))
                .geo(Geo.create(alazona)).build());

    api.put(ResourceRecordSet.builder().name("www.geo.denominator.io.")
                .type("CNAME")
                .qualifier("alazona")
                .ttl(86400)
                .add(CNAMEData.create("a.denominator.io."))
                .geo(Geo.create(alazona)).build());

    Map<String, Collection<String>> allElse = new LinkedHashMap<String, Collection<String>>();
    allElse.putAll(mgr.api().geoRecordSetsInZone("denominator.io.").supportedRegions());
    Collection<String> us = new LinkedHashSet<String>(allElse.get("United States"));
    us.removeAll(alazona.get("United States"));
    allElse.put("United States", us);

    api.put(ResourceRecordSet.builder().name("www.geo.denominator.io.")
                .type("CNAME")
                .qualifier("allElse")
                .ttl(600)
                .add(CNAMEData.create("b.denominator.io."))
                .geo(Geo.create(allElse)).build());
  }

  private String[] mexicanStates(GeoResourceRecordAddRegions command) {
    return mgr.api().geoRecordSetsInZone(command.zoneIdOrName).supportedRegions().get("Mexico")
        .toArray(new String[]{});
  }
}
