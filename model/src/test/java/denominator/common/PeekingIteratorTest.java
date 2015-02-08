package denominator.common;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class PeekingIteratorTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void unmodifiable() {
    thrown.expect(UnsupportedOperationException.class);

    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertThat(it).containsExactly(true);
    it.remove();
  }

  @Test
  public void next() {
    thrown.expect(NoSuchElementException.class);

    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertThat(it).containsExactly(true);
    it.next();
  }

  @Test
  public void peek() {
    thrown.expect(NoSuchElementException.class);

    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertTrue(it.peek());
    assertThat(it).containsExactly(true);
    it.peek();
  }

  enum TrueThenDone implements Iterable<Boolean> {
    INSTANCE;

    @Override
    public PeekingIterator<Boolean> iterator() {
      return new PeekingIterator<Boolean>() {
        boolean val = true;

        @Override
        public Boolean computeNext() {
          if (val) {
            val = false;
            return true;
          }
          return endOfData();
        }
      };
    }
  }
}
