package denominator.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Integers;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.Maps;
import org.assertj.core.internal.Objects;

import java.util.List;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;

import static org.assertj.core.data.MapEntry.entry;
import static org.assertj.core.util.Arrays.array;

public class ResourceRecordSetAssert
    extends AbstractAssert<ResourceRecordSetAssert, ResourceRecordSet> {

  Objects objects = Objects.instance();
  Integers integers = Integers.instance();
  Iterables iterables = Iterables.instance();
  Maps maps = Maps.instance();

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

  /** Asserts {@code records} are the only ones present, in any order. */
  public ResourceRecordSetAssert containsOnlyRecords(Map<String, Object>... records) {
    iterables.assertContainsOnly(info, actual.records(), records);
    return this;
  }

  public ResourceRecordSetAssert containsExactlyRecords(Map<String, Object>... records) {
    iterables.assertContainsExactly(info, actual.records(), records);
    return this;
  }

  public ResourceRecordSetAssert containsExactlyRecords(List<? extends Map<String, Object>> records) {
    return containsExactlyRecords(records.toArray(new Map[records.size()]));
  }

  public ResourceRecordSetAssert hasGeo(Geo expected) {
    isNotNull();
    objects.assertEqual(info, actual.geo(), expected);
    return this;
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
    integers.assertEqual(info, actual.weighted().weight(), expected);
    return this;
  }

  public ResourceRecordSetAssert isValid() {
    isNotNull();
    objects.assertNotNull(info, actual.name());
    objects.assertNotNull(info, actual.type());
    iterables.assertNotEmpty(info, actual.records());
    return this;
  }

  public ResourceRecordSetAssert isValidWeighted() {
    isValid();
    objects.assertNotNull(info, actual.qualifier());
    objects.assertNotNull(info, actual.weighted());
    integers.assertIsNotNegative(info, actual.weighted().weight());
    return this;
  }

  public ResourceRecordSetAssert isValidGeo() {
    isValid();
    objects.assertNotNull(info, actual.qualifier());
    objects.assertNotNull(info, actual.geo());
    maps.assertNotEmpty(info, actual.geo().regions());
    return this;
  }
}
