package denominator.common;

import org.testng.annotations.Test;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Preconditions.checkState;
import static org.testng.Assert.assertEquals;

@Test
public class PreconditionsTest {

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "should be foo")
  public void checkArgumentFormatted() {
    checkArgument(false, "should be %s", "foo");
  }

  @Test
  public void checkArgumentPass() {
    checkArgument(true, "should be %s", "foo");
  }

  @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "should be foo")
  public void checkStateFormatted() {
    checkState(false, "should be %s", "foo");
  }

  @Test
  public void checkStatePass() {
    checkState(true, "should be %s", "foo");
  }

  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "should be foo")
  public void checkNotNullFormatted() {
    checkNotNull(null, "should be %s", "foo");
  }

  @Test
  public void checkNotNullPass() {
    assertEquals(checkNotNull("foo", "should be %s", "foo"), "foo");
  }
}
