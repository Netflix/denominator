package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.loc;
import static org.assertj.core.api.Assertions.assertThat;

public class LOCDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGoodRecord() {
    loc("www.denominator.io.", "37 48 48.892 S 144 57 57.502 E 26m 10m 100m 10m");
  }

  @Test
  public void testSimpleRecord() {
    loc("www.denominator.io.", "37 48 48.892 S 144 57 57.502 E 0m");
  }

  @Test
  public void testMissingParts() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("could not find longitude");

    loc("www.denominator.io.", "37 48 48.892 S");
  }

  @Test
  public void testSimple() {
    LOCData data = LOCData.create("37 48 48.892 S 144 57 57.502 E 0m");
    assertThat(data.latitude()).isEqualTo("37 48 48.892 S");
    assertThat(data.longitude()).isEqualTo("144 57 57.502 E");
    assertThat(data.altitude()).isEqualTo("0m");
  }

  @Test
  public void testMinimal() {
    LOCData data = LOCData.create("37 S 144 E 0m");
    assertThat(data.latitude()).isEqualTo("37 S");
    assertThat(data.longitude()).isEqualTo("144 E");
    assertThat(data.altitude()).isEqualTo("0m");
  }

  @Test
  public void testFull() {
    LOCData data = LOCData.create("37 48 48.892 S 144 57 57.502 E 26m 1m 2m 3m");
    assertThat(data.latitude()).isEqualTo("37 48 48.892 S");
    assertThat(data.longitude()).isEqualTo("144 57 57.502 E");
    assertThat(data.altitude()).isEqualTo("26m");
    assertThat(data.diameter()).isEqualTo("1m");
    assertThat(data.hprecision()).isEqualTo("2m");
    assertThat(data.vprecision()).isEqualTo("3m");
  }
}
