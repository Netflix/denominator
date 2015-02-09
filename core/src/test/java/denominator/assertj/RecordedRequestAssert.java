package denominator.assertj;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Objects;

public final class RecordedRequestAssert
    extends AbstractAssert<RecordedRequestAssert, RecordedRequest> {

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
    objects.assertEqual(info, actual.getUtf8Body(), utf8Expected);
    return this;
  }
}
