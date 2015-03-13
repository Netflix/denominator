package denominator.discoverydns;

import java.util.ArrayList;
import java.util.List;

import denominator.model.ResourceRecordSet;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@Headers({"Accept: application/json", "Content-Type: application/json"})
interface DiscoveryDNS {

  @RequestLine("GET /users")
  Users listUsers();

  @RequestLine("GET /zones")
  Zones listZones();

  @RequestLine("GET /zones/{id}?rdataFormat=raw")
  Zone getZone(@Param("id") String id);

  @RequestLine("PUT /zones/{id}/resourcerecords?rdataFormat=raw")
  void updateZone(@Param("id") String id, Zone zone);

  static final class ResourceRecords {

    List<ResourceRecordSet<?>> records = new ArrayList<ResourceRecordSet<?>>();
  }

  static final class Zones {

    ZoneList zones;

    class ZoneList {

      List<Zone> zoneList;

      class Zone {

        String id;
        String name;
      }
    }
  }

  static final class Zone {

    ZoneData zone;

    ZoneData zoneUpdateResourceRecords;

    class ZoneData {

      String id;
      Long version;

      ResourceRecords resourceRecords;
    }
  }

  static final class Users {

    UserList users;

    class UserList {

      List<User> userList;

      class User {
        String id;
      }
    }
  }
}
