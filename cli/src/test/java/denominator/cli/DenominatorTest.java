package denominator.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import denominator.AllProfileResourceRecordSetApi;
import denominator.Credentials.ListCredentials;
import denominator.DNSApiManager;
import denominator.cli.Denominator.ListProviders;
import denominator.cli.Denominator.ZoneList;
import denominator.cli.GeoResourceRecordSetCommands.GeoRegionList;
import denominator.cli.GeoResourceRecordSetCommands.GeoTypeList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetApplyTTL;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetDelete;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetGet;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetRemove;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetReplace;
import denominator.common.Util;
import denominator.mock.MockProvider;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.route53.AliasTarget;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.junit.Assert.fail;

public class DenominatorTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  DNSApiManager mgr = denominator.Denominator.create(new MockProvider());

  @Test // denominator providers
  public void listsAllProvidersWithCredentials() {
    assertThat(ListProviders.providerAndCredentialsTable())
        .isEqualTo(Util.join('\n',
                             "provider   url                                                 duplicateZones credentialType credentialArgs",
                             "clouddns   https://identity.api.rackspacecloud.com/v2.0        true           password       username password",
                             "clouddns   https://identity.api.rackspacecloud.com/v2.0        true           apiKey         username apiKey",
                             "designate  http://localhost:5000/v2.0                          true           password       tenantId username password",
                             "dynect     https://api2.dynect.net/REST                        false          password       customer username password",
                             "mock       mem:mock                                            false          ",
                             "route53    https://route53.amazonaws.com                       true           accessKey      accessKey secretKey",
                             "route53    https://route53.amazonaws.com                       true           session        accessKey secretKey sessionToken",
                             "ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 false          password       username password",
                             ""));
  }

  @Test // denominator -p mock zone list
  public void testZoneList() {
    assertThat(new ZoneList().doRun(mgr)).containsExactly("denominator.io.");
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
  public void testEnvConfig() {
    ZoneList zoneList = new ZoneList() {
      @Override
      String getEnvValue(String name) {
        if (name.equals("PROVIDER")) {
          return "route53";
        } else if (name.equals("URL")) {
          return "mem:mock2";
        } else if (name.equals("ACCESS_KEY")) {
          return "foo1";
        } else if (name.equals("SECRET_KEY")) {
          return "foo2";
        }
        return null;
      }
    };
    Map<String, Object> contents = (Map) zoneList.getConfigFromEnv();
    assertThat(contents)
        .containsEntry("provider", "route53")
        .containsEntry("url", "mem:mock2");
    Map<String, String> credentials = (Map) contents.get("credentials");
    assertThat(credentials)
        .containsEntry("accessKey", "foo1")
        .containsEntry("secretKey", "foo2");
  }

  @Test // denominator zone list
  public void testConfigFromEnv() {
    ZoneList zoneList = new ZoneList() {
      @Override
      String getEnvValue(String name) {
        if (name.equals("PROVIDER")) {
          return "dynect";
        } else if (name.equals("URL")) {
          return "mem:mock2";
        } else if (name.equals("CUSTOMER")) {
          return "foo1";
        } else if (name.equals("USERNAME")) {
          return "foo2";
        } else if (name.equals("PASSWORD")) {
          return "foo3";
        }
        return null;
      }

      @Override
      public Iterator<String> doRun(DNSApiManager mgr) {
        assertThat(providerName).isEqualTo("dynect");
        assertThat(url).isEqualTo("mem:mock2");
        assertThat(Map.class.cast(credentials))
            .containsEntry("customer", "foo1")
            .containsEntry("username", "foo2")
            .containsEntry("password", "foo3");
        return Collections.<String>emptyList().iterator();
      }
    };
    zoneList.run();
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
    zoneList.credentialArgs = Arrays.asList("user", "pass");
    zoneList.run();
  }

  private String getTestConfigPath() {
    URL res = getClass().getClassLoader().getResource("test-config.yml");
    return res != null ? res.getFile() : null;
  }

  @Test // denominator -p mock record -z denominator.io. list
  public void testResourceRecordSetList() {
    ResourceRecordSetList command = new ResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "denominator.io.                                   MX                         86400 1 mx1.denominator.io.",
        "denominator.io.                                   NS                         86400 ns1.denominator.io.",
        "denominator.io.                                   SOA                        3600  ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60",
        "denominator.io.                                   SPF                        86400 v=spf1 a mx -all",
        "denominator.io.                                   TXT                        86400 blah",
        "phone.denominator.io.                             NAPTR                      3600  1 1 U E2U+sip !^.*$!sip:customer-service@example.com! .",
        "ptr.denominator.io.                               PTR                        3600  www.denominator.io.",
        "server1.denominator.io.                           CERT                       3600  12345 1 1 B33F",
        "server1.denominator.io.                           SRV                        3600  0 1 80 www.denominator.io.",
        "server1.denominator.io.                           SSHFP                      3600  1 1 B33F",
        "subdomain.denominator.io.                         NS                         3600  ns1.denominator.io.",
        "www.denominator.io.                               CNAME                      3600  www1.denominator.io.",
        "www.geo.denominator.io.                           CNAME  alazona             300   a.denominator.io.",
        "www.geo.denominator.io.                           CNAME  antarctica          0     c.denominator.io.",
        "www.geo.denominator.io.                           CNAME  columbador          86400 b.denominator.io.",
        "www.weighted.denominator.io.                      CNAME  EU-West             0     c.denominator.io.",
        "www.weighted.denominator.io.                      CNAME  US-East             0     b.denominator.io.",
        "www.weighted.denominator.io.                      CNAME  US-West             0     a.denominator.io.",
        "www1.denominator.io.                              A                          3600  192.0.2.1\n"
        + "www1.denominator.io.                              A                          3600  192.0.2.2",
        "www2.denominator.io.                              A                          3600  198.51.100.1",
        "www2.geo.denominator.io.                          A      alazona             300   192.0.2.1",
        "www2.weighted.denominator.io.                     A      US-West             0     192.0.2.1");
  }

  @Test // denominator -p mock record -z denominator.io. list -n www1.denominator.io.
  public void testResourceRecordSetListByName() {
    ResourceRecordSetList command = new ResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "www1.denominator.io.                              A                          3600  192.0.2.1\n"
        + "www1.denominator.io.                              A                          3600  192.0.2.2");
  }

  @Test // denominator -p mock record -z denominator.io. get -n www1.denominator.io. -t A
  public void testResourceRecordSetGetWhenPresent() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    assertThat(command.doRun(mgr)).containsExactly(
        "www1.denominator.io.                              A                          3600  192.0.2.1\n"
        + "www1.denominator.io.                              A                          3600  192.0.2.2");
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
        "www.geo.denominator.io.                           CNAME  alazona             300   a.denominator.io. {\"United States (US)\":[\"Alaska\",\"Arizona\"]}");
  }

  @Test
  // denominator -p mock record -z denominator.io. get -n www.geo.denominator.io. -t A -g alazona
  public void testResourceRecordSetGetWhenQualifierAbsent() {
    ResourceRecordSetGet command = new ResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
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
  public void testResourceRecordSetAdd() {
    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.values = Arrays.asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1},{address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type))
        .contains(ResourceRecordSet.<AData>builder()
                      .name(command.name)
                      .type(command.type)
                      .ttl(3600)
                      .add(AData.create("192.0.2.1"))
                      .add(AData.create("192.0.2.2")).build());
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
    command.aliasHostedZoneId = "denominator.com.";
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
    command.values = Arrays.asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=192.0.2.1},{address=192.0.2.2}] applying ttl 3600",
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
    command.values = Arrays.asList("www1.denominator.io.", "www2.denominator.io.");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. replacing rrset www.denominator.io. CNAME with values: [{cname=www1.denominator.io.},{cname=www2.denominator.io.}]",
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
    command.values = Arrays.asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. replacing rrset www1.denominator.io. A with values: [{address=192.0.2.1},{address=192.0.2.2}] and ttl 3600",
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
    command.values = Arrays.asList("192.0.2.1", "192.0.2.2");
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=192.0.2.1},{address=192.0.2.2}]",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .iterateByNameAndType(command.name, command.type)).isEmpty();
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
        "A"
        , "AAAA"
        , "CNAME"
        , "NS"
        , "PTR"
        , "SPF"
        , "TXT"
        , "MX"
        , "SRV"
        , "DS"
        , "CERT"
        , "NAPTR"
        , "SSHFP"
        , "LOC"
        , "TLSA");
  }

  @Test // denominator -p mock geo -z denominator.io. regions
  public void testGeoRegionList() {
    GeoRegionList command = new GeoRegionList();
    command.zoneIdOrName = "denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
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
        "Antarctica                  : Antarctica;Bouvet Island;French Southern Territories");
  }

  @Test // denominator -p mock geo -z denominator.io. list
  @Deprecated
  public void testGeoResourceRecordSetList() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             300   a.denominator.io. {\"United States (US)\":[\"Alaska\",\"Arizona\"]}",
        "www.geo.denominator.io.                           CNAME  antarctica          0     c.denominator.io. {\"Antarctica\":[\"Bouvet Island\",\"French Southern Territories\",\"Antarctica\"]}",
        "www.geo.denominator.io.                           CNAME  columbador          86400 b.denominator.io. {\"South America\":[\"Colombia\",\"Ecuador\"]}",
        "www2.geo.denominator.io.                          A      alazona             300   192.0.2.1 {\"United States (US)\":[\"Alaska\",\"Arizona\"]}");
  }

  @Test // denominator -p mock geo -z denominator.io. list -n www.geo.denominator.io.
  @Deprecated
  public void testGeoResourceRecordSetListByName() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             300   a.denominator.io. {\"United States (US)\":[\"Alaska\",\"Arizona\"]}",
        "www.geo.denominator.io.                           CNAME  antarctica          0     c.denominator.io. {\"Antarctica\":[\"Bouvet Island\",\"French Southern Territories\",\"Antarctica\"]}",
        "www.geo.denominator.io.                           CNAME  columbador          86400 b.denominator.io. {\"South America\":[\"Colombia\",\"Ecuador\"]}");
  }

  @Test // denominator -p mock geo -z denominator.io. list -n www.geo.denominator.io. -t CNAME
  @Deprecated
  public void testGeoResourceRecordSetListByNameAndType() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    command.type = "CNAME";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             300   a.denominator.io. {\"United States (US)\":[\"Alaska\",\"Arizona\"]}",
        "www.geo.denominator.io.                           CNAME  antarctica          0     c.denominator.io. {\"Antarctica\":[\"Bouvet Island\",\"French Southern Territories\",\"Antarctica\"]}",
        "www.geo.denominator.io.                           CNAME  columbador          86400 b.denominator.io. {\"South America\":[\"Colombia\",\"Ecuador\"]}");
  }

  @Test
  // denominator -p mock geo -z denominator.io. get -n www.geo.denominator.io. -t CNAME -g alazona
  @Deprecated
  public void testGeoResourceRecordSetGetWhenPresent() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    command.type = "CNAME";
    command.group = "alazona";
    assertThat(command.doRun(mgr)).containsExactly(
        "www.geo.denominator.io.                           CNAME  alazona             300   a.denominator.io. {\"United States (US)\":[\"Alaska\",\"Arizona\"]}");
  }

  @Test // denominator -p mock geo -z denominator.io. get -n www.geo.denominator.io. -t A -g alazona
  @Deprecated
  public void testGeoResourceRecordSetGetWhenAbsent() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    assertThat(command.doRun(mgr)).containsExactly("");
  }

  @Test
  // denominator -p mock geo -z denominator.io. applyttl -n www2.geo.denominator.io. -t A -g alazona 300
  @Deprecated
  public void testGeoResourceRecordSetApplyTTL() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetApplyTTL
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetApplyTTL();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.ttl = 300;
    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. applying ttl 300 to rrset www2.geo.denominator.io. A alazona",
        ";; ok");
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .hasTtl(command.ttl);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Mexico\"]}
  public void testGeoResourceRecordSetAddRegionsEntireRegion() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"Mexico\"]}";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"Mexico\":[\"Mexico\"]} to rrset www2.geo.denominator.io. A alazona",
        ";; current rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"]}}}",
        ";; revised rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"],\"Mexico\":[\"Mexico\"]}}}",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .containsRegion("United States (US)", "Alaska", "Arizona")
        .containsRegion("Mexico", "Mexico");

    api.put(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r {\"United States (US)\":[\"Arizona\"]}
  public void testGeoResourceRecordSetAddRegionsSkipsWhenSame() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"United States (US)\":[\"Arizona\"]}";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"United States (US)\":[\"Arizona\"]} to rrset www2.geo.denominator.io. A alazona",
        ";; current rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"]}}}",
        ";; ok");

    assertThat(
        mgr.api().recordSetsInZone(command.zoneIdOrName)
            .getByNameTypeAndQualifier(command.name, command.type, command.group)).isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}
  public void testGeoResourceRecordSetAddRegionsEntireRegionAndTerritory() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}";

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]} to rrset www2.geo.denominator.io. A alazona",
        ";; current rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"]}}}",
        ";; revised rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"],\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}}}",
        ";; ok");

    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .containsRegion("United States (US)", "Alaska", "Arizona")
        .containsRegion("Mexico", "Mexico")
        .containsRegion("South America", "Ecuador");

    api.put(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]} --dry-run --validate-regions
  public void testGeoResourceRecordSetAddRegionsEntireRegionAndTerritoryValidatedDryRun() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}";
    command.validateRegions = true;
    command.dryRun = true;

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    assertThat(command.doRun(mgr)).containsExactly(
        ";; in zone denominator.io. adding regions {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]} to rrset www2.geo.denominator.io. A alazona",
        ";; validated regions: {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}",
        ";; current rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"]}}}",
        ";; revised rrset: {\"name\":\"www2.geo.denominator.io.\",\"type\":\"A\",\"qualifier\":\"alazona\",\"ttl\":300,\"records\":[{\"address\":\"192.0.2.1\"}],\"geo\":{\"regions\":{\"United States (US)\":[\"Alaska\",\"Arizona\"],\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}}}",
        ";; ok");

    assertThat(
        mgr.api().recordSetsInZone(command.zoneIdOrName)
            .getByNameTypeAndQualifier(command.name, command.type, command.group)).isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]} --validate-regions
  public void testGeoResourceRecordSetAddRegionsValidationFailureOnTerritory() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"Mexico\"],\"South America\":[\"Equador\"]}";
    command.validateRegions = true;

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    try {
      Iterator<String> iterator = command.doRun(mgr);
      assertThat(iterator.next())
          .isEqualTo(
              ";; in zone denominator.io. adding regions {\"Mexico\":[\"Mexico\"],\"South America\":[\"Equador\"]} to rrset www2.geo.denominator.io. A alazona");
      iterator.next();
      fail("should have failed on validation");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("unsupported territories in South America: [[Equador]]");
    }
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .isEqualTo(old);
  }

  @Test
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r {\"Mexico\":[\"Mexico\"],\"Suth America\":[\"Ecuador\"]} --validate-regions
  public void testGeoResourceRecordSetAddRegionsValidationFailureOnRegion() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
    command.type = "A";
    command.group = "alazona";
    command.regions = "{\"Mexico\":[\"Mexico\"],\"Suth America\":[\"Ecuador\"]}";
    command.validateRegions = true;

    AllProfileResourceRecordSetApi api = mgr.api().recordSetsInZone(command.zoneIdOrName);
    ResourceRecordSet<?>
        old =
        api.getByNameTypeAndQualifier(command.name, command.type, command.group);

    try {
      Iterator<String> iterator = command.doRun(mgr);
      assertThat(iterator.next())
          .isEqualTo(
              ";; in zone denominator.io. adding regions {\"Mexico\":[\"Mexico\"],\"Suth America\":[\"Ecuador\"]} to rrset www2.geo.denominator.io. A alazona");
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
  // denominator -p mock geo -z denominator.io. add -n www2.geo.denominator.io. -t A -g alazona -r Mexico
  public void testGeoResourceRecordSetAddRegionsBadJson() {
    denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions
        command =
        new denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www2.geo.denominator.io.";
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
          "parse failure on regions! check json syntax. ex. {\"United States (US)\":[\"Arizona\"]}");
    }
    assertThat(mgr.api().recordSetsInZone(command.zoneIdOrName)
                   .getByNameTypeAndQualifier(command.name, command.type, command.group))
        .isEqualTo(old);
  }
}
