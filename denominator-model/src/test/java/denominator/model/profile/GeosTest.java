package denominator.model.profile;

import static denominator.model.profile.Geos.nameEqualTo;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;

@Test
public class GeosTest {

    Geo geo = Geo.create("US-East", ImmutableMultimap.of("US", "US-VA"));

    public void nameEqualToReturnsFalseOnNull() {
        assertFalse(nameEqualTo("US-East").apply(null));
    }

    public void nameEqualToReturnsFalseOnDifferentType() {
        assertFalse(nameEqualTo("US-West").apply(geo));
    }

    public void nameEqualToReturnsTrueOnSameType() {
        assertTrue(nameEqualTo("US-East").apply(geo));
    }
}
