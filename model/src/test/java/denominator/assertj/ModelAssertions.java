package denominator.assertj;

import org.assertj.core.api.Assertions;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class ModelAssertions extends Assertions {

  public static ResourceRecordSetAssert assertThat(ResourceRecordSet actual) {
    return new ResourceRecordSetAssert(actual);
  }

  public static ZoneAssert assertThat(Zone actual) {
    return new ZoneAssert(actual);
  }
}
