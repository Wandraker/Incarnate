package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Shulker;
import org.bukkit.util.Vector;

public final class ShulkerVesselController implements VesselController {
    private final int stepIntervalTicks;
    private final boolean allowVerticalWallCrawl;

    public ShulkerVesselController(FileConfiguration config) {
        this.stepIntervalTicks = Math.max(1, config.getInt("movement.shulker.step-interval-ticks", 5));
        this.allowVerticalWallCrawl = config.getBoolean("movement.shulker.allow-vertical-wall-crawl", true);
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        if (!(vessel instanceof Shulker shulker)) {
            return;
        }

        shulker.setAware(false);
        shulker.setTarget(null);
        shulker.setGravity(false);
        shulker.setVelocity(new Vector());

        if (session.isMovementControlLocked() || session.controlTick() % stepIntervalTicks != 0) {
            session.lastKnownVesselLocation(shulker.getLocation());
            return;
        }

        BlockFace move = requestedMove(session.input(), session.view().yaw(), shulker.getAttachedFace());
        if (move == null) {
            session.lastKnownVesselLocation(shulker.getLocation());
            return;
        }

        Location current = shulker.getLocation();
        Block currentCell = current.getBlock();
        Block targetCell = currentCell.getRelative(move);
        BlockFace attachedFace = shulker.getAttachedFace();
        Block support = targetCell.getRelative(attachedFace);

        if (!Bukkit.isOwnedByCurrentRegion(targetCell.getLocation())
            || !Bukkit.isOwnedByCurrentRegion(support.getLocation())
            || !targetCell.isPassable()
            || !support.getType().isSolid()) {
            session.lastKnownVesselLocation(current);
            return;
        }

        Location destination = targetCell.getLocation().add(0.5, 0.0, 0.5);
        destination.setYaw(current.getYaw());
        destination.setPitch(current.getPitch());
        if (shulker.teleport(destination)) {
            shulker.setAttachedFace(attachedFace);
        }
        session.lastKnownVesselLocation(shulker.getLocation());
    }

    private BlockFace requestedMove(InputSnapshot input, float yaw, BlockFace attachedFace) {
        if (allowVerticalWallCrawl && isWall(attachedFace)) {
            if (input.jump() != input.sneak()) {
                return input.jump() ? BlockFace.UP : BlockFace.DOWN;
            }
        }

        Vector horizontal = MovementMath.horizontal(input, yaw);
        if (horizontal.lengthSquared() < 0.0001) {
            return null;
        }
        if (Math.abs(horizontal.getX()) >= Math.abs(horizontal.getZ())) {
            return horizontal.getX() >= 0.0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return horizontal.getZ() >= 0.0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private static boolean isWall(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST;
    }
}
