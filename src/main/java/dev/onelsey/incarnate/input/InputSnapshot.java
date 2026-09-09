package dev.onelsey.incarnate.input;

import org.bukkit.Input;

public record InputSnapshot(
    boolean forward,
    boolean backward,
    boolean left,
    boolean right,
    boolean jump,
    boolean sneak,
    boolean sprint
) {
    public static final InputSnapshot NONE = new InputSnapshot(false, false, false, false, false, false, false);

    public static InputSnapshot from(Input input) {
        return new InputSnapshot(
            input.isForward(),
            input.isBackward(),
            input.isLeft(),
            input.isRight(),
            input.isJump(),
            input.isSneak(),
            input.isSprint()
        );
    }

    public double forwardAxis() {
        return (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
    }

    public double strafeAxis() {
        return (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
    }
}
