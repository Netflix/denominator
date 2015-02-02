package denominator.common;

import com.google.common.collect.ImmutableList;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;

import denominator.common.PeekingIteratorTest.TrueThenDone;

import static denominator.common.Util.and;
import static denominator.common.Util.concat;
import static denominator.common.Util.equal;
import static denominator.common.Util.filter;
import static denominator.common.Util.join;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.peekingIterator;
import static denominator.common.Util.slurp;
import static denominator.common.Util.split;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test
public class UtilTest {

  @Test
  public void slurpDoesntScrewWithWhitespance() throws IOException {
    assertEquals(slurp(new StringReader(" foo\n")), " foo\n");
  }

  @Test
  public void slurpsLargerThan2KCharBuffer() throws IOException {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 2001; i++) {
      builder.append('a');
    }
    String twoK1 = builder.toString();
    assertEquals(slurp(new StringReader(twoK1)), twoK1);
  }

  @Test
  public void joinNadaReturnsEmpty() {
    assertEquals(join(';', (Object[]) null), "");
    assertEquals(join(';', new Object[]{}), "");
  }

  @Test
  public void equalTest() {
    assertTrue(equal(null, null));
    assertTrue(equal("1", "1"));
    assertFalse(equal(null, "1"));
    assertFalse(equal("1", null));
    assertFalse(equal("1", "2"));
  }

  @Test
  public void joinMultiple() {
    assertEquals(join(';', "one", 2), "one;2");
  }

  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "toSplit")
  public void splitNull() {
    split(';', null);
  }

  @Test
  public void splitMiss() {
    assertEquals(split(';', "one,2"), Arrays.asList("one,2"));
  }

  @Test
  public void splitWin() {
    assertEquals(split(';', "one;2;2"), Arrays.asList("one", "2", "2"));
  }

  @Test
  public void splitEmpty() {
    assertEquals(split(';', "one;;2"), Arrays.asList("one", null, "2"));
    assertEquals(split(';', ";;"), Arrays.asList(null, null, null));
  }

  @Test
  public void testNextOrNull() {
    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertTrue(it.next());
    assertNull(nextOrNull(it));
  }

  @Test
  public void peekingIteratorWhenPresent() {
    PeekingIterator<Boolean> it = peekingIterator(ImmutableList.of(true).iterator());
    assertTrue(it.hasNext());
    assertTrue(it.peek());
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void peekingIteratorWhenAbsent() {
    PeekingIterator<Object> it = peekingIterator(ImmutableList.of().iterator());
    assertFalse(it.hasNext());
  }

  @Test
  public void concatIteratorsWhenPresent() {
    Iterator<Boolean>
        it =
        concat(TrueThenDone.INSTANCE.iterator(), TrueThenDone.INSTANCE.iterator());
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void concatIteratorsWhenAbsent() {
    Iterator<Boolean> first = TrueThenDone.INSTANCE.iterator();
    first.next();
    Iterator<Boolean> second = TrueThenDone.INSTANCE.iterator();
    second.next();
    Iterator<Boolean> it = concat(first, second);
    assertFalse(it.hasNext());
  }

  @Test
  public void concatIteratorsWhenFirstIsEmpty() {
    Iterator<Boolean> first = TrueThenDone.INSTANCE.iterator();
    first.next();
    Iterator<Boolean> it = concat(first, TrueThenDone.INSTANCE.iterator());
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void concatIterablesWhenPresent() {
    Iterator<Boolean> it = concat(ImmutableList.of(ImmutableList.of(true), ImmutableList.of(true)));
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void concatIterablesWhenAbsent() {
    Iterator<Object> it = concat(ImmutableList.of(ImmutableList.of(), ImmutableList.of()));
    assertFalse(it.hasNext());
  }

  @Test
  public void concatIterablesWhenFirstIsEmpty() {
    Iterator<Boolean>
        it =
        concat(ImmutableList.of(ImmutableList.<Boolean>of(), ImmutableList.of(true)));
    assertTrue(it.hasNext());
    assertTrue(it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void filterRetains() {
    Iterator<String> it = filter(ImmutableList.of("waffles", "poo", "pancakes", "eggs").iterator(),
                                 new Filter<String>() {

                                   @Override
                                   public boolean apply(String breakfast) {
                                     return "pancakes".equals(breakfast);
                                   }

                                 });
    assertTrue(it.hasNext());
    assertEquals(it.next(), "pancakes");
    assertFalse(it.hasNext());
  }

  @Test
  public void andFilter() {
    Filter<String> startsWithP = new Filter<String>() {

      @Override
      public boolean apply(String breakfast) {
        return breakfast.startsWith("p");
      }

    };

    Filter<String> notPoo = new Filter<String>() {

      @Override
      public boolean apply(String breakfast) {
        return !"poo".equals(breakfast);
      }

    };
    assertTrue(and(startsWithP, notPoo).apply("pancakes"));
    assertFalse(and(startsWithP, notPoo).apply("poo"));
  }
}
