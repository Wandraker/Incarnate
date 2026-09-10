package dev.onelsey.incarnate.possession;

import org.bukkit.entity.Fox;
import org.bukkit.entity.Panda;

public final class BodyStateCodec {
    static final int FOX_CROUCHING = 1;
    static final int FOX_SLEEPING = 1 << 1;
    static final int FOX_INTERESTED = 1 << 2;
    static final int FOX_LEAPING = 1 << 3;
    static final int FOX_DEFENDING = 1 << 4;
    static final int FOX_FACEPLANTED = 1 << 5;

    static final int PANDA_ROLLING = 1;
    static final int PANDA_SNEEZING = 1 << 1;
    static final int PANDA_ON_BACK = 1 << 2;

    private BodyStateCodec() {
    }

    public static int captureFox(Fox fox) {
        return packFox(
            fox.isCrouching(),
            fox.isSleeping(),
            fox.isInterested(),
            fox.isLeaping(),
            fox.isDefending(),
            fox.isFaceplanted()
        );
    }

    public static void restoreFox(Fox fox, int mask) {
        fox.setCrouching(has(mask, FOX_CROUCHING));
        fox.setSleeping(has(mask, FOX_SLEEPING));
        fox.setInterested(has(mask, FOX_INTERESTED));
        fox.setLeaping(has(mask, FOX_LEAPING));
        fox.setDefending(has(mask, FOX_DEFENDING));
        fox.setFaceplanted(has(mask, FOX_FACEPLANTED));
    }

    public static void clearFoxForPossession(Fox fox) {
        fox.setCrouching(false);
        fox.setSleeping(false);
        fox.setInterested(false);
        fox.setLeaping(false);
        fox.setDefending(false);
        fox.setFaceplanted(false);
    }

    public static int capturePanda(Panda panda) {
        return packPanda(panda.isRolling(), panda.isSneezing(), panda.isOnBack());
    }

    public static void restorePanda(Panda panda, int mask) {
        panda.setRolling(has(mask, PANDA_ROLLING));
        panda.setSneezing(has(mask, PANDA_SNEEZING));
        panda.setOnBack(has(mask, PANDA_ON_BACK));
    }

    public static void clearPandaForPossession(Panda panda) {
        panda.setRolling(false);
        panda.setSneezing(false);
        panda.setOnBack(false);
    }

    static int packFox(
        boolean crouching,
        boolean sleeping,
        boolean interested,
        boolean leaping,
        boolean defending,
        boolean faceplanted
    ) {
        int mask = 0;
        if (crouching) mask |= FOX_CROUCHING;
        if (sleeping) mask |= FOX_SLEEPING;
        if (interested) mask |= FOX_INTERESTED;
        if (leaping) mask |= FOX_LEAPING;
        if (defending) mask |= FOX_DEFENDING;
        if (faceplanted) mask |= FOX_FACEPLANTED;
        return mask;
    }

    static int packPanda(boolean rolling, boolean sneezing, boolean onBack) {
        int mask = 0;
        if (rolling) mask |= PANDA_ROLLING;
        if (sneezing) mask |= PANDA_SNEEZING;
        if (onBack) mask |= PANDA_ON_BACK;
        return mask;
    }

    static boolean has(int mask, int flag) {
        return (mask & flag) != 0;
    }
}
