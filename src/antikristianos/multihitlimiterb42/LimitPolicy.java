package antikristianos.multihitlimiterb42;

import java.util.HashSet;
import java.util.Set;

/** Item filters grant eligibility; character requirements and the cap still apply. */
public final class LimitPolicy {
    private LimitPolicy() {}

    public static Set<String> itemTypes(String text) {
        Set<String> types = new HashSet<>();
        if (text != null) for (String entry : text.split(";")) {
            String type = entry.trim();
            if (!type.isEmpty()) types.add(type);
        }
        return Set.copyOf(types);
    }

    public static int limit(int vanillaCount, boolean enabled, boolean ranged, boolean whitelisted,
                            boolean blacklisted, boolean categoryAllowed, boolean twoHandRequired,
                            boolean twoHanded, boolean rangeRequired, double range, double minimumRange,
                            int strength, int minimumStrength, int fitness, int minimumFitness,
                            int skill, int minimumSkill, int cap) {
        if (!enabled || ranged || vanillaCount <= 1) return vanillaCount;
        boolean eligible = whitelisted || (!blacklisted && categoryAllowed
                && (!twoHandRequired || twoHanded) && (!rangeRequired || range >= minimumRange));
        if (!eligible || strength < minimumStrength || fitness < minimumFitness || skill < minimumSkill) return 1;
        return cap > 0 ? Math.min(vanillaCount, cap) : vanillaCount;
    }
}
