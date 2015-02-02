package denominator.mock;

import denominator.DNSApiManager;
import denominator.Denominator;

public class MockConnection {

  final DNSApiManager manager;
  final String mutableZone;

  MockConnection() {
    manager = Denominator.create(new MockProvider());
    mutableZone = "denominator.io.";
  }
}
