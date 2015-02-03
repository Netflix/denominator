package denominator.discoverydns;

import java.util.LinkedHashSet;
import java.util.Set;

import denominator.model.ResourceRecordSet;
import feign.Param;
import feign.RequestLine;

interface DiscoveryDNS {

  @RequestLine("GET /users")
  Users listUsers();

  @RequestLine("GET /zones")
  Zones listZones();

  @RequestLine("GET /zones/{id}?rdataFormat=raw")
  Zone getZone(@Param("id") String id);

  @RequestLine("GET /zones?searchName={zone}")
  Zones findZone(@Param("zone") String zone);

  @RequestLine("PUT /zones/{id}/resourcerecords?rdataFormat=raw")
  void updateZone(@Param("id") String id, Zone zone);

  static final class ResourceRecords {

    Set<ResourceRecordSet<?>> records = new LinkedHashSet<ResourceRecordSet<?>>();
  }

  static final class Zones {

    ZoneList zones;

    class ZoneList {

      Set<Zone> zoneList;

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

      Set<User> userList;

      class User {

        String id;
        String username;
        String status;
      }
    }
  }
}
