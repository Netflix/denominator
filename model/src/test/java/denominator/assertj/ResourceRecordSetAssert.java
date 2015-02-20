package denominator.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.Maps;
import org.assertj.core.internal.Objects;

import java.util.List;
import java.util.Map;

import denominator.model.ResourceRecordSet;

import static org.assertj.core.data.MapEntry.entry;
import static org.assertj.core.util.Arrays.array;

public class ResourceRecordSetAssert
    extends AbstractAssert<ResourceRecordSetAssert, ResourceRecordSet> {

  Iterables iterables = Iterables.instance();
  Maps maps = Maps.instance();
  Objects objects = Objects.instance();

  public ResourceRecordSetAssert(ResourceRecordSet actual) {
    super(actual, ResourceRecordSetAssert.class);
  }

  public ResourceRecordSetAssert hasName(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.name(), expected);
    return this;
  }

  public ResourceRecordSetAssert hasType(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.type(), expected);
    return this;
  }

  public ResourceRecordSetAssert hasQualifier(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.qualifier(), expected);
    return this;
  }

  public ResourceRecordSetAssert hasTtl(Integer expected) {
    isNotNull();
    objects.assertEqual(info, actual.ttl(), expected);
    return this;
  }

  public ResourceRecordSetAssert containsOnlyRecords(Map<String, Object>... records) {
    iterables.assertContainsOnly(info, actual.records(), records);
    return this;
  }

  public ResourceRecordSetAssert containsOnlyRecords(List<? extends Map<String, Object>> records) {
    return containsOnlyRecords(records.toArray(new Map[records.size()]));
  }

  public ResourceRecordSetAssert containsRegion(String region, String... territories) {
    isNotNull();
    maps.assertContains(info, actual.geo().regions(),
                        array(entry(region, java.util.Arrays.asList(territories))));
    return this;
  }

  public ResourceRecordSetAssert hasWeight(int expected) {
    isNotNull();
    objects.assertNotNull(info, actual.weighted());
    objects.assertEqual(info, actual.weighted().weight(), expected);
    return this;
  }
}
