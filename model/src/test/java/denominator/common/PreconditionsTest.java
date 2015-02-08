package denominator.common;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;

public class PreconditionsTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void checkArgumentFormatted() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("should be foo");

    checkArgument(false, "should be %s", "foo");
  }

  @Test
  public void checkArgumentPass() {
    checkArgument(true, "should be %s", "foo");
  }

  @Test
  public void checkStateFormatted() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("should be foo");

    checkState(false, "should be %s", "foo");
  }

  @Test
  public void checkStatePass() {
    checkState(true, "should be %s", "foo");
  }

  @Test
  public void checkNotNullFormatted() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("should be foo");

    checkNotNull(null, "should be %s", "foo");
  }

  @Test
  public void checkNotNullPass() {
    assertThat(checkNotNull("foo", "should be %s", "foo")).isEqualTo("foo");
  }
}
