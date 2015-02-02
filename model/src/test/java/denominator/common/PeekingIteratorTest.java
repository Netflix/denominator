package denominator.common;

import org.testng.annotations.Test;

import java.util.NoSuchElementException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class PeekingIteratorTest {

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void unmodifiable() {
    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertTrue(it.next());
    it.remove();
  }

  ;

  @Test(expectedExceptions = NoSuchElementException.class)
  public void next() {
    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertFalse(it.hasNext());
    it.next();
  }

  @Test(expectedExceptions = NoSuchElementException.class)
  public void peek() {
    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertTrue(it.hasNext());
    assertTrue(it.peek());
    assertTrue(it.next());
    assertFalse(it.hasNext());
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
