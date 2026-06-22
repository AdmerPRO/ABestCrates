package pl.admerpro.aBestCrates.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class CrateTest {
    @Test
    public void newCrateRequiresItsOwnKeyByDefault() {
        Crate crate = new Crate("daily");

        assertEquals(List.of(new KeyRequirement("daily", 1)), crate.getKeyRequirements());
    }

    @Test
    public void explicitlyEmptyRequirementsRemainEmpty() {
        Crate crate = new Crate("free");

        crate.setKeyRequirements(List.of());

        assertTrue(crate.getKeyRequirements().isEmpty());
    }

    @Test
    public void nullIdIsNormalizedWithoutConstructorFailure() {
        Crate crate = new Crate(null);

        assertEquals("", crate.getId());
    }
}
