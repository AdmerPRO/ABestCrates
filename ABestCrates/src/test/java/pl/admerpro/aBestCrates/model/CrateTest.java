package pl.admerpro.aBestCrates.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
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

    @Test
    public void zeroChanceRewardIsNeverSelectedAtRandomBoundary() {
        Crate crate = new Crate("daily");
        Reward impossible = new Reward("impossible");
        impossible.setRealChance(0.0D);
        Reward winner = new Reward("winner");
        winner.setRealChance(10.0D);
        crate.addReward(impossible);
        crate.addReward(winner);

        Random zeroBoundary = new Random() {
            @Override
            public double nextDouble() {
                return 0.0D;
            }
        };

        assertEquals("winner", crate.rollReward(zeroBoundary).orElseThrow().getId());
    }
}
