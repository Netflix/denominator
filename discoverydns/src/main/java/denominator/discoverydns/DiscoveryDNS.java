package denominator.discoverydns;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Named;

import denominator.model.ResourceRecordSet;
import feign.RequestLine;

public interface DiscoveryDNS {

  @RequestLine("GET /users")
  Users listUsers();

  @RequestLine("GET /zones")
  Zones listZones();

  @RequestLine("GET /zones/{id}?rdataFormat=raw")
  Zone getZone(@Named("id") String id);

  @RequestLine("GET /zones?searchName={zone}")
  Zones findZone(@Named("zone") String zone);

  @RequestLine("PUT /zones/{id}/resourcerecords?rdataFormat=raw")
  void updateZone(@Named("id") String id, Zone zone);

  public static final class ResourceRecords {

    public Set<ResourceRecordSet<?>> records = new LinkedHashSet<ResourceRecordSet<?>>();
  }

  public static final class Zones {

    public ZoneList zones;

    class ZoneList {

      public Set<Zone> zoneList;

      class Zone {

        public String id;
        public String name;
      }
    }
  }

  public static final class Zone {

    public ZoneData zone;

    public ZoneData zoneUpdateResourceRecords;

    class ZoneData {

      public String id;
      public Long version;

      public ResourceRecords resourceRecords;
    }
  }

  public static final class Users {

    public UserList users;

    class UserList {

      public Set<User> userList;

      class User {

        public String id;
        public String username;
        public String status;
      }
    }
  }
}
