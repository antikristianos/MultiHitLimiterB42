package antikristianos.multihitlimiterb42;

import java.util.Set;
import zombie.SandboxOptions;

/** One snapshot per second; lists are parsed only when their authored value changes. */
public final class Settings {
    private static long nextRefresh;
    private static Settings cached;
    public final boolean enabled, twoHandRequired, rangeRequired;
    public final double minimumRange;
    public final int strength, fitness, skill, cap;
    public final boolean[] categories;
    public final Set<String> whitelist, blacklist;
    private final String whitelistText, blacklistText;
    private static final String[] CATEGORY_OPTIONS = {
        "Axe", "LongBlunt", "SmallBlunt", "LongBlade", "SmallBlade", "Spear", "Unarmed"
    };

    private Settings(Settings previous) {
        enabled = SandboxOptions.instance.multiHitZombies.getValue();
        twoHandRequired = bool("TwoHandReq", false);
        rangeRequired = bool("RangeBool", true);
        minimumRange = number("RangeNum", 1.21);
        strength = (int) number("StrReq", 0);
        fitness = (int) number("FitReq", 0);
        skill = (int) number("SkillReq", 0);
        cap = (int) number("MaxHitCap", 0);
        categories = new boolean[CATEGORY_OPTIONS.length];
        for (int i = 0; i < categories.length; i++) categories[i] = bool(CATEGORY_OPTIONS[i], true);
        whitelistText = value("Whitelist", "Base.BareHands");
        blacklistText = value("Blacklist", "Base.Nightstick");
        whitelist = previous != null && whitelistText.equals(previous.whitelistText)
                ? previous.whitelist : LimitPolicy.itemTypes(whitelistText);
        blacklist = previous != null && blacklistText.equals(previous.blacklistText)
                ? previous.blacklist : LimitPolicy.itemTypes(blacklistText);
    }

    public static Settings current() {
        long now = System.nanoTime();
        if (cached == null || now - nextRefresh >= 0) {
            cached = new Settings(cached);
            nextRefresh = now + 1_000_000_000L;
        }
        return cached;
    }

    private static String value(String name, String fallback) {
        SandboxOptions.SandboxOption option = SandboxOptions.instance.getOptionByName("MultiHitLimiter." + name);
        return option == null ? fallback : option.asConfigOption().getValueAsString();
    }
    private static boolean bool(String name, boolean fallback) {
        return Boolean.parseBoolean(value(name, Boolean.toString(fallback)));
    }
    private static double number(String name, double fallback) {
        try { return Double.parseDouble(value(name, Double.toString(fallback))); }
        catch (NumberFormatException exception) { return fallback; }
    }
}
