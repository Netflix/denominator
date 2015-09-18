package denominator.common;

import denominator.model.rdata.TXTData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void slurpDoesntScrewWithWhitespance() throws IOException {
    assertThat(slurp(new StringReader(" foo\n"))).isEqualTo(" foo\n");
  }

  @Test
  public void slurpsLargerThan2KCharBuffer() throws IOException {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 2001; i++) {
      builder.append('a');
    }
    String twoK1 = builder.toString();
    assertThat(slurp(new StringReader(twoK1))).isEqualTo(twoK1);
  }

  @Test
  public void joinNadaReturnsEmpty() {
    assertThat(join(';', (Object[]) null)).isEmpty();
    assertThat(join(';', new Object[]{})).isEmpty();
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
    assertThat(join(';', "one", 2)).isEqualTo("one;2");
  }

  @Test
  public void splitNull() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("toSplit");

    split(';', null);
  }

  @Test
  public void splitMiss() {
    assertThat(split(';', "one,2")).containsExactly("one,2");
  }

  @Test
  public void splitWin() {
    assertThat(split(';', "one;2;2")).containsExactly("one", "2", "2");
  }

  @Test
  public void splitEmpty() {
    assertThat(split(';', "one;;2")).containsExactly("one", null, "2");
    assertThat(split(';', ";;")).containsExactly(null, null, null);
  }

  @Test
  public void testNextOrNull() {
    PeekingIterator<Boolean> it = TrueThenDone.INSTANCE.iterator();
    assertTrue(it.next());
    assertThat(nextOrNull(it)).isNull();
  }

  @Test
  public void peekingIteratorWhenPresent() {
    PeekingIterator<Boolean> it = peekingIterator(Arrays.asList(true).iterator());
    assertTrue(it.peek());
    assertThat(it).containsExactly(true);
  }

  @Test
  public void peekingIteratorWhenAbsent() {
    PeekingIterator<Object> it = peekingIterator(Arrays.asList().iterator());
    assertThat(it).isEmpty();
  }

  @Test
  public void concatIteratorsWhenPresent() {
    Iterator<Boolean>
        it =
        concat(TrueThenDone.INSTANCE.iterator(), TrueThenDone.INSTANCE.iterator());
    assertThat(it).containsExactly(true, true);
  }

  @Test
  public void concatIteratorsWhenAbsent() {
    Iterator<Boolean> first = TrueThenDone.INSTANCE.iterator();
    first.next();
    Iterator<Boolean> second = TrueThenDone.INSTANCE.iterator();
    second.next();
    Iterator<Boolean> it = concat(first, second);
    assertThat(it).isEmpty();
  }

  @Test
  public void concatIteratorsWhenFirstIsEmpty() {
    Iterator<Boolean> first = TrueThenDone.INSTANCE.iterator();
    first.next();
    Iterator<Boolean> it = concat(first, TrueThenDone.INSTANCE.iterator());
    assertThat(it).containsExactly(true);
  }

  @Test
  public void concatIterablesWhenPresent() {
    Iterator<Boolean> it = concat(Arrays.asList(Arrays.asList(true), Arrays.asList(true)));
    assertThat(it).containsExactly(true, true);
  }

  @Test
  public void concatIterablesWhenAbsent() {
    Iterator<Object> it = concat(Arrays.asList(Arrays.asList(), Arrays.asList()));
    assertThat(it).isEmpty();
  }

  @Test
  public void concatIterablesWhenFirstIsEmpty() {
    Iterator<Boolean>
        it =
        concat(Arrays.asList(Arrays.<Boolean>asList(), Arrays.asList(true)));
    assertThat(it).containsExactly(true);
  }

  @Test
  public void filterRetains() {
    Iterator<String> it = filter(Arrays.asList("waffles", "poo", "pancakes", "eggs").iterator(),
                                 new Filter<String>() {

                                   @Override
                                   public boolean apply(String breakfast) {
                                     return "pancakes".equals(breakfast);
                                   }
                                 });
    assertThat(it).containsExactly("pancakes");
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

  @Test
  public void toMapDoesntSplitTXT() {
    assertThat(Util.toMap("TXT", "ONE TWO THREE")).isEqualTo(TXTData.create("ONE TWO THREE"));
  }
}
