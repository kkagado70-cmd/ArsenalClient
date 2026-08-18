package com.example.automace;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoMaceLogic {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum Stage { IDLE, BREAK_SHIELD, SWAP_MACE, SLAM_ATTACK, RESTORE_SLOT }

    private static Stage stage = Stage.IDLE;
    private static int tickTimer = 0;
    private static int originalSlot = -1;
    private static LivingEntity currentTarget = null;

    private static float smoothYaw = 0.0f;
    private static float smoothPitch = 0.0f;

    public static boolean enabled = true;
    public static double searchRange = 350.0;
    public static double swingRange = 2.85;
    public static boolean stunSlam = true;
    public static boolean autoSwitch = true;
    public static boolean swapBack = true;
    public static double minFallDist = 3.0;
    public static double breachThreshold = 7.0;
    public static int comboDelayTicks = 2;
    public static double rotationStep = 45.0;

    public static void init() {
    }

    public static void onTick() {
        if (!enabled || mc.player == null || mc.level == null) return;

        if (tickTimer > 0) {
            tickTimer--;
            return;
        }

        currentTarget = mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(6.0, searchRange, 6.0),
            e -> e != mc.player && e.isAlive() && !e.isDeadOrDying() && mc.player.getY() > e.getY()
        ).stream().min(java.util.Comparator.comparingDouble(e -> mc.player.distanceToSqr(e))).orElse(null);

        if (currentTarget == null) {
            resetState();
            return;
        }

        boolean isFalling = mc.player.fallDistance >= minFallDist && !mc.player.onGround() && !mc.player.isInWater();

        if (isFalling) {
            applyVulcanSafeAim(currentTarget);
            double distance = mc.player.distanceTo(currentTarget);

            if (distance <= swingRange) {
                processComboLogic();
            }
        } else if (mc.player.onGround() || mc.player.fallDistance <= 0.3f) {
            resetState();
        }
    }

    private static void processComboLogic() {
        if (currentTarget == null) return;

        boolean isShielding = false;
        if (currentTarget instanceof Player p) {
            isShielding = p.isUsingItem() && p.getUseItem().getItem() instanceof ShieldItem;
        }

        int delay = Math.max(1, comboDelayTicks);

        switch (stage) {
            case IDLE -> {
                if (originalSlot == -1) {
                    originalSlot = mc.player.getInventory().selected;
                }

                if (stunSlam && isShielding) {
                    int axeSlot = findItemInHotbar(AxeItem.class);
                    if (axeSlot != -1) {
                        mc.player.getInventory().selected = axeSlot;
                        mc.gameMode.attack(mc.player, currentTarget);
                        mc.player.swingHand(InteractionHand.MAIN_HAND);
                        stage = Stage.SWAP_MACE;
                        tickTimer = delay;
                        return;
                    }
                }
                stage = Stage.SWAP_MACE;
                tickTimer = 0;
            }
            case SWAP_MACE -> {
                if (autoSwitch) {
                    boolean preferDensity = mc.player.fallDistance > breachThreshold;
                    int bestMace = findBestMaceSlot(preferDensity);
                    if (bestMace != -1) {
                        mc.player.getInventory().selected = bestMace;
                    }
                }
                stage = Stage.SLAM_ATTACK;
                tickTimer = 1;
            }
            case SLAM_ATTACK -> {
                mc.gameMode.attack(mc.player, currentTarget);
                mc.player.swingHand(InteractionHand.MAIN_HAND);
                stage = Stage.RESTORE_SLOT;
                tickTimer = delay;
            }
            case RESTORE_SLOT -> {
                resetState();
            }
        }
    }

    private static void applyVulcanSafeAim(LivingEntity target) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetPoint = target.getBoundingBox().getCenter();

        double dx = targetPoint.x - eyePos.x;
        double dy = targetPoint.y - eyePos.y;
        double dz = targetPoint.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        if (smoothYaw == 0.0f && smoothPitch == 0.0f) {
            smoothYaw = mc.player.getYRot();
            smoothPitch = mc.player.getXRot();
        }

        float yawDiff = wrapAngle(targetYaw - smoothYaw);
        float pitchDiff = targetPitch - smoothPitch;

        float maxStep = (float) rotationStep;
        float stepYaw = Math.max(-maxStep, Math.min(maxStep, yawDiff));
        float stepPitch = Math.max(-maxStep, Math.min(maxStep, pitchDiff));

        smoothYaw += stepYaw;
        smoothPitch = Math.max(-90.0f, Math.min(90.0f, smoothPitch + stepPitch));

        mc.player.setYRot(smoothYaw);
        mc.player.setXRot(smoothPitch);
        mc.player.yRotO = smoothYaw;
        mc.player.xRotO = smoothPitch;
        mc.player.yHeadRot = smoothYaw;
        mc.player.yHeadRotO = smoothYaw;
    }

    private static float wrapAngle(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private static int findItemInHotbar(Class<?> itemClass) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private static int findBestMaceSlot(boolean preferDensity) {
        int bestSlot = -1;
        int maxLevel = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !stack.is(Items.MACE)) continue;

            var registry = mc.level.registryAccess();
            var enchant = preferDensity ? registry.get(Enchantments.DENSITY) : registry.get(Enchantments.BREACH);

            if (enchant.isPresent()) {
                int level = EnchantmentHelper.getItemEnchantmentLevel(enchant.get(), stack);
                if (level > maxLevel) {
                    maxLevel = level;
                    bestSlot = i;
                }
            } else if (bestSlot == -1) {
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    public static void resetState() {
        if (swapBack && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = originalSlot;
        }
        stage = Stage.IDLE;
        originalSlot = -1;
        currentTarget = null;
        smoothYaw = 0.0f;
        smoothPitch = 0.0f;
        tickTimer = 0;
    }
}