package pl.admerpro.aBestCrates.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void nullRewardLookupAndRemovalAreSafe() {
        Crate crate = new Crate("daily");

        assertTrue(crate.getReward(null).isEmpty());
        assertFalse(crate.removeReward(null));
    }

    @Test
    public void blankRewardIdUsesSafeFallback() {
        Reward reward = new Reward(" ");

        assertEquals("reward", reward.getId());
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

    @Test
    public void crateIdsValidatePublicFormat() {
        assertTrue(Crate.isValidId("daily_01"));
        assertTrue(Crate.isValidId("Daily-Crate"));
        assertFalse(Crate.isValidId("_hidden"));
        assertFalse(Crate.isValidId("name with spaces"));
        assertFalse(Crate.isValidId("abcdefghijklmnopqrstuvwxyz0123456789"));
    }

    @Test
    public void crateNumericSettingsAreClampedToSafeValues() {
        Crate crate = new Crate("daily");

        crate.setCooldownSeconds(-10L);
        crate.setOpenCost(-2.5D);
        crate.setRewardRolls(20);

        assertEquals(0L, crate.getCooldownSeconds());
        assertEquals(0.0D, crate.getOpenCost(), 0.0001D);
        assertEquals(9, crate.getRewardRolls());

        crate.setRewardRolls(-1);

        assertEquals(1, crate.getRewardRolls());
    }

    @Test
    public void blankTextSettingsFallBackToSafeDefaults() {
        Crate crate = new Crate("daily");

        crate.setDisplayName(" ");
        crate.setNoKeyMessage(" ");
        crate.setPreviewTitle(" ");
        crate.setOpeningTitle(" ");
        crate.setColor(" ");

        assertEquals("daily", crate.getDisplayName());
        assertEquals("&cYou do not have a key for %crate_displayname%", crate.getNoKeyMessage());
        assertEquals("&5Preview: &f%crate_displayname%", crate.getPreviewTitle());
        assertEquals("&5Opening: &f%crate_displayname%", crate.getOpeningTitle());
        assertEquals("&5", crate.getColor());
    }

    @Test
    public void keyRequirementNormalizesBlankIdAndAmount() {
        KeyRequirement requirement = new KeyRequirement(" daily ", -5);

        assertEquals("daily", requirement.crateId());
        assertEquals(1, requirement.amount());
    }

    @Test
    public void allZeroChanceRewardsReturnEmptyRoll() {
        Crate crate = new Crate("daily");
        Reward impossible = new Reward("impossible");
        impossible.setRealChance(0.0D);
        crate.addReward(impossible);

        assertTrue(crate.rollReward(new Random(1L)).isEmpty());
    }

    @Test
    public void rewardPermissionListsAreSanitizedAndCopied() {
        Reward reward = new Reward("permissions");
        List<String> source = new java.util.ArrayList<>(List.of("  vip.reward  ", "", "blocked.test"));

        reward.setRequiredPermissions(source);
        source.set(0, "changed");

        assertEquals(List.of("vip.reward", "blocked.test"), reward.getRequiredPermissions());
    }

    @Test
    public void rewardNumericSettingsAreClampedToSafeValues() {
        Reward reward = new Reward("numbers");

        reward.setRealChance(-1.0D);
        reward.setDisplayChance(-2.0D);
        reward.setWeight(-3.0D);
        reward.setGlobalLimit(-4);
        reward.setPlayerLimit(-5);

        assertEquals(0.0D, reward.getRealChance(), 0.0001D);
        assertEquals(0.0D, reward.getDisplayChance(), 0.0001D);
        assertEquals(0.0D, reward.getWeight(), 0.0001D);
        assertEquals(0, reward.getGlobalLimit());
        assertEquals(0, reward.getPlayerLimit());
    }

    @Test
    public void keyDefinitionTextSettingsFallBackToSafeDefaults() {
        KeyDefinition key = new KeyDefinition();

        key.setDisplayName(" ");
        key.setLore(null);

        assertEquals("&6%crate_displayname% &fKey", key.getDisplayName());
        assertTrue(key.getLore().isEmpty());
    }
}
