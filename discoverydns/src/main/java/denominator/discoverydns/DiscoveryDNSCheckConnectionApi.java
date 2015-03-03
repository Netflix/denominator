package denominator.discoverydns;

import denominator.CheckConnection;

final class DiscoveryDNSCheckConnectionApi implements CheckConnection {

  private DiscoveryDNS api;

  DiscoveryDNSCheckConnectionApi(DiscoveryDNS api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      DiscoveryDNS.Users users = api.listUsers();
      if (users == null ||
          users.users == null ||
          users.users.userList == null ||
          users.users.userList.size() == 0) {
        return false;
      }
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
