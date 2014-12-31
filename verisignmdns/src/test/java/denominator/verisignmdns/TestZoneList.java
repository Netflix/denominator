package denominator.verisignmdns;

import static denominator.CredentialsConfiguration.credentials;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class TestZoneList {

	public static void main(String[] args) {
		DNSApiManager manager = Denominator.create("vrsndns",
				credentials("username", "password"));

		for (Zone zone : manager.api().zones()) {
			  for (ResourceRecordSet<?> rrs : manager.api().recordSetsInZone(zone.idOrName())) {
			    System.out.println(rrs.name());
			  }
			}
	}

}
