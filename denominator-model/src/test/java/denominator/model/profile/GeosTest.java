package denominator.model.profile;

import static denominator.model.profile.Geos.groupEqualTo;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;

@Test
public class GeosTest {

    Geo geo = Geo.create("US-East", ImmutableMultimap.of("US", "US-VA"));

    public void nameEqualToReturnsFalseOnNull() {
        assertFalse(groupEqualTo("US-East").apply(null));
    }

    public void nameEqualToReturnsFalseOnDifferentType() {
        assertFalse(groupEqualTo("US-West").apply(geo));
    }

    public void nameEqualToReturnsTrueOnSameType() {
        assertTrue(groupEqualTo("US-East").apply(geo));
    }
}
