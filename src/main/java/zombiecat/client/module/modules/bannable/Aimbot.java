package zombiecat.client.module.modules.bannable;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zombiecat.client.module.Module;
import zombiecat.client.module.setting.impl.BooleanSetting;
import zombiecat.client.module.setting.impl.SliderSetting;
import zombiecat.client.utils.Utils;

public class Aimbot extends Module {
    public static BooleanSetting onlyFire;
    public static BooleanSetting wsStair;
    public static BooleanSetting pup;
    public static BooleanSetting skelePriority;
    public static BooleanSetting mobPriority; // <-- Added this new setting
    public static SliderSetting a;
    public static SliderSetting predict;
    public static SliderSetting yPredict;

    public Aimbot() {
        super("Aimbot", ModuleCategory.bannable);
        this.registerSetting(a = new SliderSetting("Fineness", 0.4, 0.1, 1.0, 0.1));
        this.registerSetting(onlyFire = new BooleanSetting("OnlyFire", true));
        this.registerSetting(wsStair = new BooleanSetting("WSStair", true));
        this.registerSetting(pup = new BooleanSetting("Pup", false));
        this.registerSetting(skelePriority = new BooleanSetting("SkelePriority", false));
        this.registerSetting(mobPriority = new BooleanSetting("MobPriority", false)); // <-- Registered here
        this.registerSetting(predict = new SliderSetting("Predict", 4, 0, 10, 0.1));
        this.registerSetting(yPredict = new SliderSetting("YPredict", 4, 0, 10, 0.1));
    }

    private boolean isPumpkinHead(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) return false;
        EntityLivingBase living = (EntityLivingBase) entity;
        ItemStack helmet = living.getEquipmentInSlot(4);
        return helmet != null && helmet.getItem() == Item.getItemFromBlock(Blocks.pumpkin);
    }

    private boolean isArmoredSkele(Entity entity) {
        if (!(entity instanceof EntitySkeleton)) return false;
        EntitySkeleton skele = (EntitySkeleton) entity;
        ItemStack boots = skele.getEquipmentInSlot(1);
        ItemStack leggings = skele.getEquipmentInSlot(2);
        ItemStack chest = skele.getEquipmentInSlot(3);
        ItemStack held = skele.getHeldItem();

        return boots != null && boots.getItem() == Items.iron_boots
                && leggings != null && leggings.getItem() == Items.iron_leggings
                && chest != null && chest.getItem() == Items.iron_chestplate
                && held != null && held.getItem() == Items.stone_sword;
    }

    @SubscribeEvent
    public void re(RenderWorldLastEvent e) {
        if (onlyFire.getValue() && !mc.gameSettings.keyBindUseItem.isKeyDown()) {
            return;
        }

        double dis = 9999999;
        Vec3 target = null;
        Entity targetEntity = null;

        boolean anyPriorityFound = false;
        boolean anyNonPriorityFound = false; // <-- Added this

        if (Utils.Player.isPlayerInGame()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityLivingBase
                        && !(entity instanceof EntityArmorStand)
                        && !(entity instanceof EntityWither)
                        && !(entity instanceof EntityVillager)
                        && !(entity instanceof EntityPlayer)
                        && !(entity instanceof EntityChicken)
                        && !(entity instanceof EntityPig)
                        && !(entity instanceof EntityCow)
                        && entity.isEntityAlive()) {

                    if (entity instanceof EntityWolf) {
                        EntityWolf wolf = (EntityWolf) entity;
                        if (!pup.getValue() && wolf.isChild()) {
                            continue;
                        }
                    }

                    boolean isPriority = isPumpkinHead(entity) || isArmoredSkele(entity);
                    if (skelePriority.getValue() && isPriority) {
                        anyPriorityFound = true;
                    }
                    if (mobPriority.getValue() && !isPriority) {
                        anyNonPriorityFound = true; // <-- Track normal mobs if mobPriority enabled
                    }
                }
            }

            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityLivingBase
                        && !(entity instanceof EntityArmorStand)
                        && !(entity instanceof EntityWither)
                        && !(entity instanceof EntityVillager)
                        && !(entity instanceof EntityPlayer)
                        && !(entity instanceof EntityChicken)
                        && !(entity instanceof EntityPig)
                        && !(entity instanceof EntityCow)
                        && entity.isEntityAlive()) {

                    if (entity instanceof EntityWolf) {
                        EntityWolf wolf = (EntityWolf) entity;
                        if (!pup.getValue() && wolf.isChild()) {
                            continue;
                        }
                    }

                    boolean isPriority = isPumpkinHead(entity) || isArmoredSkele(entity);
                    if (skelePriority.getValue() && anyPriorityFound && !isPriority) {
                        continue; // Skip non-priority mobs if priority ones exist
                    }
                    if (mobPriority.getValue() && anyNonPriorityFound && isPriority) {
                        continue; // <-- Skip priority mobs if non-priority mobs exist
                    }

                    Vec3 offset = getMotionVec(entity, (float) predict.getValue(), (float) yPredict.getValue());
                    double distance = fovDistance(entity.getPositionEyes(1).add(offset));
                    boolean canWall = canWallShot(mc.thePlayer.getPositionEyes(1), entity.getPositionEyes(1).add(offset));

                    if (distance < dis && canWall) {
                        dis = distance;
                        target = entity.getPositionEyes(1).add(offset);
                        targetEntity = entity;
                        continue;
                    }

                    double yOffset = entity.getPositionEyes(1).yCoord - entity.getPositionVector().yCoord;
                    for (double yMult = 0.2; yMult <= 0.9; yMult += 0.1) {
                        Vec3 aiming = entity.getPositionVector().add(offset).add(new Vec3(0, -yOffset * yMult, 0));
                        distance = fovDistance(aiming);
                        canWall = canWallShot(mc.thePlayer.getPositionEyes(1), aiming);

                        if (distance < dis && canWall) {
                            dis = distance;
                            target = aiming;
                            targetEntity = entity;
                            break;
                        }
                    }
                }
            }

            if (targetEntity instanceof EntityLivingBase) {
                Vec3 headPos = targetEntity.getPositionEyes(1);
                Vec3 torsoPos = targetEntity.getPositionVector().addVector(0, targetEntity.getEyeHeight() * 0.5, 0);
                Vec3 feetPos = targetEntity.getPositionVector();

                // Try head first
                if (canWallShot(mc.thePlayer.getPositionEyes(1), headPos)) {
                    target = headPos;
                }
                // If head blocked, try torso
                else if (canWallShot(mc.thePlayer.getPositionEyes(1), torsoPos)) {
                    target = torsoPos;
                }
                // If torso blocked, try feet
                else {
                    target = feetPos;
                }
            }

            if (target != null) {
                float[] angle = calculateYawPitch(mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.getEyeHeight(), 0), target);
                mc.thePlayer.rotationYaw = angle[0];
                mc.thePlayer.rotationPitch = angle[1];
            }
        }
    }

    public static double fovDistance(Vec3 vec3) {
        float[] angle = calculateYawPitch(mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.getEyeHeight(), 0), vec3);
        return angleBetween(angle[0], mc.thePlayer.rotationYaw) +
                Math.abs(Math.max(angle[1], mc.thePlayer.rotationPitch) - Math.min(angle[1], mc.thePlayer.rotationPitch));
    }

    public static double angleBetween(double first, double second) {
        return Math.abs(subtractAngles(first, second));
    }

    public static double subtractAngles(double start, double end) {
        return MathHelper.wrapAngleTo180_double(end - start);
    }

    public static boolean canWallShot(Vec3 start, Vec3 end) {
        float[] angle = calculateYawPitch(start, end);

        Vec3 temp = fromPolar(angle[1], angle[0]);
        Vec3 forward = new Vec3(temp.xCoord * a.getValue(), temp.yCoord * a.getValue(), temp.zCoord * a.getValue());
        Vec3 now = start;
        while (now.distanceTo(end) > a.getValue() + 0.1) {
            Block block = mc.theWorld.getBlockState(new BlockPos(now)).getBlock();
            if (block == Blocks.sandstone_stairs) return false;
            if (block instanceof BlockSlab && ((BlockSlab) block).isDouble()) return false;
            if (block instanceof BlockSlab || wsStair.getValue() && block instanceof BlockStairs && block != Blocks.spruce_stairs || block == Blocks.iron_door || block == Blocks.iron_bars || block instanceof BlockSign || block instanceof BlockBarrier) {
                return true;
            }
            if (block != Blocks.air && block != Blocks.grass && block != Blocks.tallgrass) {
                return false;
            }
            now = now.add(forward);
        }
        Block endBlock = mc.theWorld.getBlockState(new BlockPos(end)).getBlock();
        return endBlock == Blocks.air || endBlock == Blocks.iron_bars || endBlock instanceof BlockSlab || endBlock instanceof BlockSign || endBlock instanceof BlockBarrier;
    }

    public static Vec3 fromPolar(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float g = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float h = -MathHelper.cos(-pitch * 0.017453292F);
        float i = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(g * h, i, f * h);
    }

    public static float[] calculateYawPitch(Vec3 start, Vec3 vec) {
        double diffX = vec.xCoord - start.xCoord;
        double diffY = vec.yCoord - start.yCoord;
        double diffZ = vec.zCoord - start.zCoord;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapAngleTo180_float(yaw), MathHelper.wrapAngleTo180_float(pitch)};
    }

    public static Vec3 getMotionVec(Entity entity, float ticks, float yTicks) {
        double dX = entity.posX - entity.prevPosX;
        double dY = entity.posY - entity.prevPosY;
        double dZ = entity.posZ - entity.prevPosZ;
        double entityMotionPosX = 0;
        double entityMotionPosY = 0;
        double entityMotionPosZ = 0;
        for (double i = 1; i <= ticks; i = i + 0.3) {
            for (double i2 = 1; i2 <= yTicks; i2 = i2 + 0.3) {
                if (!mc.theWorld.checkBlockCollision(entity.getEntityBoundingBox().offset(dX * i, dY * i2, dZ * i))) {
                    entityMotionPosX = dX * i;
                    entityMotionPosY = dY * i2;
                    entityMotionPosZ = dZ * i;
                } else {
                    break;
                }
            }
        }

        return new Vec3(entityMotionPosX, entityMotionPosY, entityMotionPosZ);
    }
}
