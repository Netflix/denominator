package denominator.discoverydns;

import denominator.DNSApiManager;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;

public class DiscoveryDNSTest {

  static String
      users =
      "{ \"users\": { \"@uri\": \"https://api.discoverydns.com/users\", \"userList\": [ { \"id\": \"123-123-123-123-123\" } ] } }";
  static String
      noUsers =
      "{ \"users\": { \"@uri\": \"https://api.discoverydns.com/users\", \"userList\": [ ] } }";

  static String
      zones =
      "{ \"zones\": { \"@uri\": \"https://api.discoverydns.com/zones\", \"zoneList\": [ { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"name\": \"denominator.io.\" } ] } }";
  static String
      noZones =
      "{ \"zones\": { \"@uri\": \"https://api.discoverydns.com/zones\", \"zoneList\": [ ] } }";

  static String
      records =
      "{ \"zone\": { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ { \"name\": \"www.denominator.io.\", \"class\": \"IN\", \"ttl\": \"60\", \"type\": \"A\", \"rdata\": \"127.0.0.1\" } ] } }";
  static String
      noRecords =
      "{ \"zone\": { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ ] } }";
  static String
      updateRecords =
      "{ \"zoneUpdateResourceRecords\": { \"id\": \"123-123-123-123-123\", \"version\": 10, \"resourceRecords\": [ { \"name\": \"www.denominator.io.\", \"class\": \"IN\", \"ttl\": \"60\", \"type\": \"A\", \"rdata\": \"127.0.0.1\" } ] } }";

  static DNSApiManager mockApi(final int port) {
    return Denominator.create(new DiscoveryDNSProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials("accessKey", "secretKey"));
  }
}
