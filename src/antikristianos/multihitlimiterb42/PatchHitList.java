package antikristianos.multihitlimiterb42;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.CombatManager;
import zombie.characters.IsoGameCharacter;

@Patch(className = "zombie.CombatManager", methodName = "calculateHitInfoList", warmUp = true, strictMatch = true)
public final class PatchHitList {
    @Patch.OnExit
    public static void exit(@Patch.This CombatManager manager, @Patch.Argument(0) IsoGameCharacter character) {
        HitLimiter.apply(manager, character);
    }
}
