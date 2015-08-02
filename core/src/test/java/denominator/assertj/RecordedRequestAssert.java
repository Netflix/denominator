package denominator.assertj;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Objects;
import org.assertj.core.internal.Strings;

public final class RecordedRequestAssert
    extends AbstractAssert<RecordedRequestAssert, RecordedRequest> {

  Strings strings = Strings.instance();
  Objects objects = Objects.instance();

  public RecordedRequestAssert(RecordedRequest actual) {
    super(actual, RecordedRequestAssert.class);
  }

  public RecordedRequestAssert hasMethod(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.getMethod(), expected);
    return this;
  }

  public RecordedRequestAssert hasPath(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.getPath(), expected);
    return this;
  }

  public RecordedRequestAssert hasBody(String utf8Expected) {
    isNotNull();
    objects.assertEqual(info, actual.getBody().readUtf8(), utf8Expected);
    return this;
  }

  public RecordedRequestAssert hasXMLBody(String utf8Expected) {
    isNotNull();
    hasHeaderContaining("Content-Type", "application/xml");
    strings.assertXmlEqualsTo(info, actual.getBody().readUtf8(), utf8Expected);
    return this;
  }

  public RecordedRequestAssert hasHeaderContaining(String name, String value) {
    isNotNull();
    if (actual.getHeader(name) == null) {
      failWithMessage("\nExpecting request to have header:<%s>", name);
    }
    strings.assertContains(info, actual.getHeader(name), value);
    return this;
  }
}
