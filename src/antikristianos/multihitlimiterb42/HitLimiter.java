package antikristianos.multihitlimiterb42;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import zombie.CombatManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.fields.hit.AttackVars;
import zombie.network.fields.hit.HitInfo;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.WeaponCategory;
import java.util.List;

public final class HitLimiter {
    private static Boolean supported;
    private static boolean acknowledged;
    private static final WeaponCategory[] CATEGORIES = {
        WeaponCategory.AXE, WeaponCategory.BLUNT, WeaponCategory.SMALL_BLUNT,
        WeaponCategory.LONG_BLADE, WeaponCategory.SMALL_BLADE, WeaponCategory.SPEAR, WeaponCategory.UNARMED
    };
    private HitLimiter() {}

    public static boolean supported() {
        if (supported == null) {
            try (InputStream expected = HitLimiter.class.getResourceAsStream("/antikristianos/multihitlimiterb42/combat.sha256");
                 InputStream actual = CombatManager.class.getResourceAsStream("/zombie/CombatManager.class")) {
                if (expected == null || actual == null) throw new IllegalStateException("Combat baseline missing");
                String baseline = new String(expected.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII).trim();
                String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(actual.readAllBytes()));
                supported = baseline.lines().anyMatch(digest::equals);
            } catch (Exception exception) {
                supported = false;
                System.err.println("[MultiHitLimiterB42] Baseline check failed: " + exception);
            }
            System.out.println("[MultiHitLimiterB42] Combat baseline " + (supported ? "verified" : "UNSUPPORTED; limiter disabled"));
        }
        return supported;
    }

    public static void apply(CombatManager manager, IsoGameCharacter character) {
        if (manager == null || !(character instanceof IsoPlayer player)) return;
        // Match Vanilla's remote-client early return; never rewrite another client's cached targets.
        if (GameClient.client && !player.isLocal()) return;
        if (!supported()) return;
        if (!acknowledged) {
            acknowledged = true;
            System.out.println("[MultiHitLimiterB42] ACTIVE 0.1.2; calculateHitInfoList hook reached");
        }
        Settings settings = Settings.current();
        if (!settings.enabled) return;
        AttackVars attack = player.getAttackVars();
        if (attack == null || attack.doGrapple) return;
        HandWeapon weapon = attack.doShove ? player.bareHands : attack.getWeapon(player);
        if (weapon == null || (!attack.doShove && (weapon.isRanged() || weapon.isAimedFirearm()))) return;
        List<HitInfo> hits = player.getHitInfoList();
        if (hits == null || hits.size() <= 1) return;
        int category = attack.doShove ? 6 : category(weapon);
        PerkFactory.Perk perk = attack.doShove ? null : weapon.getPerk();
        int skill = perk == null || perk == PerkFactory.Perks.None ? settings.skill : player.getPerkLevel(perk);
        String itemType = attack.doShove ? "Base.BareHands" : weapon.getFullType();
        int maximum = LimitPolicy.limit(hits.size(), true, false,
                settings.whitelist.contains(itemType), settings.blacklist.contains(itemType),
                category < 0 || settings.categories[category], settings.twoHandRequired,
                weapon.isRequiresEquippedBothHands() || (weapon.getScriptItem() != null && weapon.isTwoHandWeapon()), settings.rangeRequired,
                weapon.getMaxRange(), settings.minimumRange,
                player.getPerkLevel(PerkFactory.Perks.Strength), settings.strength,
                player.getPerkLevel(PerkFactory.Perks.Fitness), settings.fitness,
                attack.doShove ? settings.skill : skill, settings.skill, settings.cap);
        trim(hits, manager.hitInfoPool, maximum);
    }

    public static void trim(List<HitInfo> hits, ObjectPool<HitInfo> pool, int maximum) {
        while (hits.size() > maximum) pool.release(hits.remove(hits.size() - 1));
    }

    private static int category(HandWeapon weapon) {
        for (int i = 0; i < CATEGORIES.length; i++) if (weapon.isOfWeaponCategory(CATEGORIES[i])) return i;
        return -1;
    }
}
