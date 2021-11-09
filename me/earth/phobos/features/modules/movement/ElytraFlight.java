



package me.earth.phobos.features.modules.movement;

import me.earth.phobos.features.modules.*;
import me.earth.phobos.features.setting.*;
import net.minecraft.entity.*;
import net.minecraft.network.*;
import net.minecraft.network.play.client.*;
import net.minecraftforge.fml.common.eventhandler.*;
import me.earth.phobos.util.*;
import net.minecraft.inventory.*;
import net.minecraft.init.*;
import me.earth.phobos.*;
import net.minecraft.item.*;
import net.minecraft.client.entity.*;
import net.minecraft.client.network.*;
import java.util.*;
import net.minecraft.util.math.*;
import me.earth.phobos.event.events.*;
import net.minecraft.entity.player.*;

public class ElytraFlight extends Module
{
    private static ElytraFlight INSTANCE;
    private final TimerUtil timer;
    public Setting<Mode> mode;
    public Setting<Integer> devMode;
    public Setting<Float> speed;
    public Setting<Float> vSpeed;
    public Setting<Float> hSpeed;
    public Setting<Float> glide;
    public Setting<Float> tooBeeSpeed;
    public Setting<Boolean> autoStart;
    public Setting<Boolean> disableInLiquid;
    public Setting<Boolean> infiniteDura;
    public Setting<Boolean> noKick;
    public Setting<Boolean> allowUp;
    public Setting<Boolean> lockPitch;
    private Double posX;
    private Double flyHeight;
    private Double posZ;
    
    public ElytraFlight() {
        super("ElytraFlight", "Makes Elytra Flight better.", Module.Category.MOVEMENT, true, false, false);
        this.timer = new TimerUtil();
        this.mode = (Setting<Mode>)this.register(new Setting("Mode", (T)Mode.FLY));
        this.devMode = (Setting<Integer>)this.register(new Setting("Type", (T)2, (T)1, (T)3, v -> this.mode.getValue() == Mode.BYPASS || this.mode.getValue() == Mode.BETTER, "EventMode"));
        this.speed = (Setting<Float>)this.register(new Setting("Speed", (T)1.0f, (T)0.0f, (T)10.0f, v -> this.mode.getValue() != Mode.FLY && this.mode.getValue() != Mode.BOOST && this.mode.getValue() != Mode.BETTER && this.mode.getValue() != Mode.OHARE && this.mode.getValue() != Mode.LOOK, "The Speed."));
        this.vSpeed = (Setting<Float>)this.register(new Setting("VSpeed", (T)0.3f, (T)0.0f, (T)10.0f, v -> this.mode.getValue() == Mode.BETTER || this.mode.getValue() == Mode.OHARE || this.mode.getValue() == Mode.LOOK, "Vertical Speed"));
        this.hSpeed = (Setting<Float>)this.register(new Setting("HSpeed", (T)1.0f, (T)0.0f, (T)10.0f, v -> this.mode.getValue() == Mode.BETTER || this.mode.getValue() == Mode.OHARE || this.mode.getValue() == Mode.LOOK, "Horizontal Speed"));
        this.glide = (Setting<Float>)this.register(new Setting("Glide", (T)1.0E-4f, (T)0.0f, (T)0.2f, v -> this.mode.getValue() == Mode.BETTER, "Glide Speed"));
        this.tooBeeSpeed = (Setting<Float>)this.register(new Setting("TooBeeSpeed", (T)1.8000001f, (T)1.0f, (T)2.0f, v -> this.mode.getValue() == Mode.TOOBEE, "Speed for flight on 2b2t"));
        this.autoStart = (Setting<Boolean>)this.register(new Setting("AutoStart", (T)true));
        this.disableInLiquid = (Setting<Boolean>)this.register(new Setting("NoLiquid", (T)true));
        this.infiniteDura = (Setting<Boolean>)this.register(new Setting("InfiniteDura", (T)false));
        this.noKick = (Setting<Boolean>)this.register(new Setting("NoKick", (T)false, v -> this.mode.getValue() == Mode.PACKET));
        this.allowUp = (Setting<Boolean>)this.register(new Setting("AllowUp", (T)true, v -> this.mode.getValue() == Mode.BETTER));
        this.lockPitch = (Setting<Boolean>)this.register(new Setting("LockPitch", (T)false));
        this.setInstance();
    }
    
    public static ElytraFlight getInstance() {
        if (ElytraFlight.INSTANCE == null) {
            ElytraFlight.INSTANCE = new ElytraFlight();
        }
        return ElytraFlight.INSTANCE;
    }
    
    private void setInstance() {
        ElytraFlight.INSTANCE = this;
    }
    
    public void onEnable() {
        if (this.mode.getValue() == Mode.BETTER && !this.autoStart.getValue() && this.devMode.getValue() == 1) {
            ElytraFlight.mc.player.connection.sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
        }
        this.flyHeight = null;
        this.posX = null;
        this.posZ = null;
    }
    
    public String getDisplayInfo() {
        return this.mode.currentEnumName();
    }
    
    public void onUpdate() {
        if (this.mode.getValue() == Mode.BYPASS && this.devMode.getValue() == 1 && ElytraFlight.mc.player.isElytraFlying()) {
            ElytraFlight.mc.player.motionX = 0.0;
            ElytraFlight.mc.player.motionY = -1.0E-4;
            ElytraFlight.mc.player.motionZ = 0.0;
            final double forwardInput = ElytraFlight.mc.player.movementInput.moveForward;
            final double strafeInput = ElytraFlight.mc.player.movementInput.moveStrafe;
            final double[] result = this.forwardStrafeYaw(forwardInput, strafeInput, ElytraFlight.mc.player.rotationYaw);
            final double forward = result[0];
            final double strafe = result[1];
            final double yaw = result[2];
            if (forwardInput != 0.0 || strafeInput != 0.0) {
                final double cos = Math.cos(Math.toRadians(yaw + 90.0));
                final double sin = Math.sin(Math.toRadians(yaw + 90.0));
                ElytraFlight.mc.player.motionX = forward * this.speed.getValue() * cos + strafe * this.speed.getValue() * sin;
                ElytraFlight.mc.player.motionZ = forward * this.speed.getValue() * sin - strafe * this.speed.getValue() * cos;
            }
            if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                ElytraFlight.mc.player.motionY = -1.0;
            }
        }
    }
    
    @SubscribeEvent
    public void onSendPacket(final PacketEvent.Send event) {
        if (event.getPacket() instanceof CPacketPlayer && this.mode.getValue() == Mode.TOOBEE) {
            event.getPacket();
            ElytraFlight.mc.player.isElytraFlying();
        }
        if (event.getPacket() instanceof CPacketPlayer && this.mode.getValue() == Mode.TOOBEEBYPASS) {
            event.getPacket();
            ElytraFlight.mc.player.isElytraFlying();
        }
    }
    
    @SubscribeEvent
    public void onMove(final MoveEvent event) {
        if (this.mode.getValue() == Mode.LOOK && ElytraFlight.mc.player.isElytraFlying()) {
            ElytraFlight.mc.player.motionY = 0.0;
            ElytraFlight.mc.player.motionX = 0.0;
            ElytraFlight.mc.player.motionZ = 0.0;
            if (ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                ElytraFlight.mc.player.motionY = this.hSpeed.getValue() * -(ElytraFlight.mc.player.movementInput.moveForward * Math.sin(MathUtil.degToRad(ElytraFlight.mc.player.rotationPitch)));
            }
            if (ElytraFlight.mc.player.movementInput.jump) {
                ElytraFlight.mc.player.motionY = this.vSpeed.getValue();
            }
            else if (ElytraFlight.mc.player.movementInput.sneak) {
                ElytraFlight.mc.player.motionY = -1.0f * this.vSpeed.getValue();
            }
            double forward = ElytraFlight.mc.player.movementInput.moveForward;
            double strafe = ElytraFlight.mc.player.movementInput.moveStrafe;
            float yaw = ElytraFlight.mc.player.rotationYaw;
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += ((forward > 0.0) ? -45 : 45);
                }
                else if (strafe < 0.0) {
                    yaw += ((forward > 0.0) ? 45 : -45);
                }
                strafe = 0.0;
                if (forward > 0.0) {
                    forward = 1.0;
                }
                else if (forward < 0.0) {
                    forward = -1.0;
                }
            }
            final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
            final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
            if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f && ElytraFlight.mc.player.movementInput.moveForward == 0.0f) {
                ElytraFlight.mc.player.motionX = forward * this.hSpeed.getValue() * cos + strafe * this.hSpeed.getValue() * sin;
                ElytraFlight.mc.player.motionZ = forward * this.hSpeed.getValue() * sin - strafe * this.hSpeed.getValue() * cos;
            }
            else if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f || ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                ElytraFlight.mc.player.motionX = (forward * this.hSpeed.getValue() * cos + strafe * this.hSpeed.getValue() * sin) * Math.cos(Math.abs(MathUtil.degToRad(ElytraFlight.mc.player.rotationPitch)));
                ElytraFlight.mc.player.motionZ = (forward * this.hSpeed.getValue() * sin - strafe * this.hSpeed.getValue() * cos) * Math.cos(Math.abs(MathUtil.degToRad(ElytraFlight.mc.player.rotationPitch)));
            }
            event.setX(ElytraFlight.mc.player.motionX);
            event.setY(ElytraFlight.mc.player.motionY);
            event.setZ(ElytraFlight.mc.player.motionZ);
        }
        if (this.mode.getValue() == Mode.OHARE) {
            final ItemStack itemstack = ElytraFlight.mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            if (itemstack.getItem() == Items.ELYTRA && ItemElytra.isUsable(itemstack) && ElytraFlight.mc.player.isElytraFlying()) {
                event.setY(ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown() ? ((double)this.vSpeed.getValue()) : (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown() ? (-this.vSpeed.getValue()) : 0.0));
                ElytraFlight.mc.player.addVelocity(0.0, ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown() ? ((double)this.vSpeed.getValue()) : (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown() ? (-this.vSpeed.getValue()) : 0.0), 0.0);
                ElytraFlight.mc.player.rotateElytraX = 0.0f;
                ElytraFlight.mc.player.rotateElytraY = 0.0f;
                ElytraFlight.mc.player.rotateElytraZ = 0.0f;
                ElytraFlight.mc.player.moveVertical = (ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown() ? this.vSpeed.getValue() : (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown() ? (-this.vSpeed.getValue()) : 0.0f));
                double forward2 = ElytraFlight.mc.player.movementInput.moveForward;
                double strafe2 = ElytraFlight.mc.player.movementInput.moveStrafe;
                float yaw2 = ElytraFlight.mc.player.rotationYaw;
                if (forward2 == 0.0 && strafe2 == 0.0) {
                    event.setX(0.0);
                    event.setZ(0.0);
                }
                else {
                    if (forward2 != 0.0) {
                        if (strafe2 > 0.0) {
                            yaw2 += ((forward2 > 0.0) ? -45 : 45);
                        }
                        else if (strafe2 < 0.0) {
                            yaw2 += ((forward2 > 0.0) ? 45 : -45);
                        }
                        strafe2 = 0.0;
                        if (forward2 > 0.0) {
                            forward2 = 1.0;
                        }
                        else if (forward2 < 0.0) {
                            forward2 = -1.0;
                        }
                    }
                    final double cos2 = Math.cos(Math.toRadians(yaw2 + 90.0f));
                    final double sin2 = Math.sin(Math.toRadians(yaw2 + 90.0f));
                    event.setX(forward2 * this.hSpeed.getValue() * cos2 + strafe2 * this.hSpeed.getValue() * sin2);
                    event.setZ(forward2 * this.hSpeed.getValue() * sin2 - strafe2 * this.hSpeed.getValue() * cos2);
                }
            }
        }
        else if (event.getStage() == 0 && this.mode.getValue() == Mode.BYPASS && this.devMode.getValue() == 3) {
            if (ElytraFlight.mc.player.isElytraFlying()) {
                event.setX(0.0);
                event.setY(-1.0E-4);
                event.setZ(0.0);
                final double forwardInput = ElytraFlight.mc.player.movementInput.moveForward;
                final double strafeInput = ElytraFlight.mc.player.movementInput.moveStrafe;
                final double[] result = this.forwardStrafeYaw(forwardInput, strafeInput, ElytraFlight.mc.player.rotationYaw);
                final double forward3 = result[0];
                final double strafe3 = result[1];
                final double yaw3 = result[2];
                if (forwardInput != 0.0 || strafeInput != 0.0) {
                    final double cos3 = Math.cos(Math.toRadians(yaw3 + 90.0));
                    final double sin3 = Math.sin(Math.toRadians(yaw3 + 90.0));
                    event.setX(forward3 * this.speed.getValue() * cos3 + strafe3 * this.speed.getValue() * sin3);
                    event.setY(forward3 * this.speed.getValue() * sin3 - strafe3 * this.speed.getValue() * cos3);
                }
                if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                    event.setY(-1.0);
                }
            }
        }
        else if (this.mode.getValue() == Mode.TOOBEE) {
            if (!ElytraFlight.mc.player.isElytraFlying()) {
                return;
            }
            if (ElytraFlight.mc.player.movementInput.jump) {
                return;
            }
            if (ElytraFlight.mc.player.movementInput.sneak) {
                ElytraFlight.mc.player.motionY = -(this.tooBeeSpeed.getValue() / 2.0f);
                event.setY((double)(-(this.speed.getValue() / 2.0f)));
            }
            else if (event.getY() != -1.01E-4) {
                event.setY(-1.01E-4);
                ElytraFlight.mc.player.motionY = -1.01E-4;
            }
            this.setMoveSpeed(event, this.tooBeeSpeed.getValue());
        }
        else if (this.mode.getValue() == Mode.TOOBEEBYPASS) {
            if (!ElytraFlight.mc.player.isElytraFlying()) {
                return;
            }
            if (ElytraFlight.mc.player.movementInput.jump) {
                return;
            }
            if (this.lockPitch.getValue()) {
                ElytraFlight.mc.player.rotationPitch = 4.0f;
            }
            if (Phobos.speedManager.getSpeedKpH() > 180.0) {
                return;
            }
            final double yaw4 = Math.toRadians(ElytraFlight.mc.player.rotationYaw);
            final EntityPlayerSP player = ElytraFlight.mc.player;
            player.motionX -= ElytraFlight.mc.player.movementInput.moveForward * Math.sin(yaw4) * 0.04;
            final EntityPlayerSP player2 = ElytraFlight.mc.player;
            player2.motionZ += ElytraFlight.mc.player.movementInput.moveForward * Math.cos(yaw4) * 0.04;
        }
    }
    
    private void setMoveSpeed(final MoveEvent event, final double speed) {
        double forward = ElytraFlight.mc.player.movementInput.moveForward;
        double strafe = ElytraFlight.mc.player.movementInput.moveStrafe;
        float yaw = ElytraFlight.mc.player.rotationYaw;
        if (forward == 0.0 && strafe == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
            ElytraFlight.mc.player.motionX = 0.0;
            ElytraFlight.mc.player.motionZ = 0.0;
        }
        else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += ((forward > 0.0) ? -45 : 45);
                }
                else if (strafe < 0.0) {
                    yaw += ((forward > 0.0) ? 45 : -45);
                }
                strafe = 0.0;
                if (forward > 0.0) {
                    forward = 1.0;
                }
                else if (forward < 0.0) {
                    forward = -1.0;
                }
            }
            final double x = forward * speed * -Math.sin(Math.toRadians(yaw)) + strafe * speed * Math.cos(Math.toRadians(yaw));
            final double z = forward * speed * Math.cos(Math.toRadians(yaw)) - strafe * speed * -Math.sin(Math.toRadians(yaw));
            event.setX(x);
            event.setZ(z);
            ElytraFlight.mc.player.motionX = x;
            ElytraFlight.mc.player.motionZ = z;
        }
    }
    
    public void onTick() {
        if (!ElytraFlight.mc.player.isElytraFlying()) {
            return;
        }
        switch (this.mode.getValue()) {
            case BOOST: {
                if (ElytraFlight.mc.player.isInWater()) {
                    Objects.requireNonNull(ElytraFlight.mc.getConnection()).sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                    return;
                }
                if (ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown()) {
                    final EntityPlayerSP player = ElytraFlight.mc.player;
                    player.motionY += 0.08;
                }
                else if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                    final EntityPlayerSP player2 = ElytraFlight.mc.player;
                    player2.motionY -= 0.04;
                }
                if (ElytraFlight.mc.gameSettings.keyBindForward.isKeyDown()) {
                    final float yaw = (float)Math.toRadians(ElytraFlight.mc.player.rotationYaw);
                    final EntityPlayerSP player3 = ElytraFlight.mc.player;
                    player3.motionX -= MathHelper.sin(yaw) * 0.05f;
                    final EntityPlayerSP player4 = ElytraFlight.mc.player;
                    player4.motionZ += MathHelper.cos(yaw) * 0.05f;
                    break;
                }
                if (!ElytraFlight.mc.gameSettings.keyBindBack.isKeyDown()) {
                    break;
                }
                final float yaw = (float)Math.toRadians(ElytraFlight.mc.player.rotationYaw);
                final EntityPlayerSP player5 = ElytraFlight.mc.player;
                player5.motionX += MathHelper.sin(yaw) * 0.05f;
                final EntityPlayerSP player6 = ElytraFlight.mc.player;
                player6.motionZ -= MathHelper.cos(yaw) * 0.05f;
                break;
            }
            case FLY: {
                ElytraFlight.mc.player.capabilities.isFlying = true;
                break;
            }
        }
    }
    
    @SubscribeEvent
    public void onUpdateWalkingPlayer(final UpdateWalkingPlayerEvent event) {
        if (ElytraFlight.mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            return;
        }
        switch (event.getStage()) {
            case 0: {
                if (this.disableInLiquid.getValue() && (ElytraFlight.mc.player.isInWater() || ElytraFlight.mc.player.isInLava())) {
                    if (ElytraFlight.mc.player.isElytraFlying()) {
                        Objects.requireNonNull(ElytraFlight.mc.getConnection()).sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                    }
                    return;
                }
                if (this.autoStart.getValue() && ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown() && !ElytraFlight.mc.player.isElytraFlying() && ElytraFlight.mc.player.motionY < 0.0 && this.timer.passedMs(250L)) {
                    Objects.requireNonNull(ElytraFlight.mc.getConnection()).sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                    this.timer.reset();
                }
                if (this.mode.getValue() == Mode.BETTER) {
                    final double[] dir = MathUtil.directionSpeed((this.devMode.getValue() == 1) ? ((double)this.speed.getValue()) : ((double)this.hSpeed.getValue()));
                    switch (this.devMode.getValue()) {
                        case 1: {
                            ElytraFlight.mc.player.setVelocity(0.0, 0.0, 0.0);
                            ElytraFlight.mc.player.jumpMovementFactor = this.speed.getValue();
                            if (ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown()) {
                                final EntityPlayerSP player = ElytraFlight.mc.player;
                                player.motionY += this.speed.getValue();
                            }
                            if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                                final EntityPlayerSP player2 = ElytraFlight.mc.player;
                                player2.motionY -= this.speed.getValue();
                            }
                            if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f || ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                                ElytraFlight.mc.player.motionX = dir[0];
                                ElytraFlight.mc.player.motionZ = dir[1];
                                break;
                            }
                            ElytraFlight.mc.player.motionX = 0.0;
                            ElytraFlight.mc.player.motionZ = 0.0;
                            break;
                        }
                        case 2: {
                            if (ElytraFlight.mc.player.isElytraFlying()) {
                                if (this.flyHeight == null) {
                                    this.flyHeight = ElytraFlight.mc.player.posY;
                                }
                                if (this.noKick.getValue()) {
                                    this.flyHeight -= (Double)this.glide.getValue();
                                }
                                this.posX = 0.0;
                                this.posZ = 0.0;
                                if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f || ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                                    this.posX = dir[0];
                                    this.posZ = dir[1];
                                }
                                if (ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown()) {
                                    this.flyHeight = ElytraFlight.mc.player.posY + this.vSpeed.getValue();
                                }
                                if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                                    this.flyHeight = ElytraFlight.mc.player.posY - this.vSpeed.getValue();
                                }
                                ElytraFlight.mc.player.setPosition(ElytraFlight.mc.player.posX + this.posX, (double)this.flyHeight, ElytraFlight.mc.player.posZ + this.posZ);
                                ElytraFlight.mc.player.setVelocity(0.0, 0.0, 0.0);
                                break;
                            }
                            this.flyHeight = null;
                            return;
                        }
                        case 3: {
                            if (ElytraFlight.mc.player.isElytraFlying()) {
                                if (this.flyHeight == null || this.posX == null || this.posX == 0.0 || this.posZ == null || this.posZ == 0.0) {
                                    this.flyHeight = ElytraFlight.mc.player.posY;
                                    this.posX = ElytraFlight.mc.player.posX;
                                    this.posZ = ElytraFlight.mc.player.posZ;
                                }
                                if (this.noKick.getValue()) {
                                    this.flyHeight -= (Double)this.glide.getValue();
                                }
                                if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f || ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                                    this.posX += dir[0];
                                    this.posZ += dir[1];
                                }
                                if (this.allowUp.getValue() && ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown()) {
                                    this.flyHeight = ElytraFlight.mc.player.posY + this.vSpeed.getValue() / 10.0f;
                                }
                                if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                                    this.flyHeight = ElytraFlight.mc.player.posY - this.vSpeed.getValue() / 10.0f;
                                }
                                ElytraFlight.mc.player.setPosition((double)this.posX, (double)this.flyHeight, (double)this.posZ);
                                ElytraFlight.mc.player.setVelocity(0.0, 0.0, 0.0);
                                break;
                            }
                            this.flyHeight = null;
                            this.posX = null;
                            this.posZ = null;
                            return;
                        }
                    }
                }
                final double rotationYaw = Math.toRadians(ElytraFlight.mc.player.rotationYaw);
                if (ElytraFlight.mc.player.isElytraFlying()) {
                    switch (this.mode.getValue()) {
                        case VANILLA: {
                            final float speedScaled = this.speed.getValue() * 0.05f;
                            if (ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown()) {
                                final EntityPlayerSP player3 = ElytraFlight.mc.player;
                                player3.motionY += speedScaled;
                            }
                            if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                                final EntityPlayerSP player4 = ElytraFlight.mc.player;
                                player4.motionY -= speedScaled;
                            }
                            if (ElytraFlight.mc.gameSettings.keyBindForward.isKeyDown()) {
                                final EntityPlayerSP player5 = ElytraFlight.mc.player;
                                player5.motionX -= Math.sin(rotationYaw) * speedScaled;
                                final EntityPlayerSP player6 = ElytraFlight.mc.player;
                                player6.motionZ += Math.cos(rotationYaw) * speedScaled;
                            }
                            if (!ElytraFlight.mc.gameSettings.keyBindBack.isKeyDown()) {
                                break;
                            }
                            final EntityPlayerSP player7 = ElytraFlight.mc.player;
                            player7.motionX += Math.sin(rotationYaw) * speedScaled;
                            final EntityPlayerSP player8 = ElytraFlight.mc.player;
                            player8.motionZ -= Math.cos(rotationYaw) * speedScaled;
                            break;
                        }
                        case PACKET: {
                            this.freezePlayer((EntityPlayer)ElytraFlight.mc.player);
                            this.runNoKick((EntityPlayer)ElytraFlight.mc.player);
                            final double[] directionSpeedPacket = MathUtil.directionSpeed(this.speed.getValue());
                            if (ElytraFlight.mc.player.movementInput.jump) {
                                ElytraFlight.mc.player.motionY = this.speed.getValue();
                            }
                            if (ElytraFlight.mc.player.movementInput.sneak) {
                                ElytraFlight.mc.player.motionY = -this.speed.getValue();
                            }
                            if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f || ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                                ElytraFlight.mc.player.motionX = directionSpeedPacket[0];
                                ElytraFlight.mc.player.motionZ = directionSpeedPacket[1];
                            }
                            Objects.requireNonNull(ElytraFlight.mc.getConnection()).sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                            ElytraFlight.mc.getConnection().sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                            break;
                        }
                        case BYPASS: {
                            if (this.devMode.getValue() != 3) {
                                break;
                            }
                            if (ElytraFlight.mc.gameSettings.keyBindJump.isKeyDown()) {
                                ElytraFlight.mc.player.motionY = 0.019999999552965164;
                            }
                            if (ElytraFlight.mc.gameSettings.keyBindSneak.isKeyDown()) {
                                ElytraFlight.mc.player.motionY = -0.20000000298023224;
                            }
                            if (ElytraFlight.mc.player.ticksExisted % 8 == 0 && ElytraFlight.mc.player.posY <= 240.0) {
                                ElytraFlight.mc.player.motionY = 0.019999999552965164;
                            }
                            ElytraFlight.mc.player.capabilities.isFlying = true;
                            ElytraFlight.mc.player.capabilities.setFlySpeed(0.025f);
                            final double[] directionSpeedBypass = MathUtil.directionSpeed(0.5199999809265137);
                            if (ElytraFlight.mc.player.movementInput.moveStrafe != 0.0f || ElytraFlight.mc.player.movementInput.moveForward != 0.0f) {
                                ElytraFlight.mc.player.motionX = directionSpeedBypass[0];
                                ElytraFlight.mc.player.motionZ = directionSpeedBypass[1];
                                break;
                            }
                            ElytraFlight.mc.player.motionX = 0.0;
                            ElytraFlight.mc.player.motionZ = 0.0;
                            break;
                        }
                    }
                }
                if (!this.infiniteDura.getValue()) {
                    break;
                }
                ElytraFlight.mc.player.connection.sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                break;
            }
            case 1: {
                if (!this.infiniteDura.getValue()) {
                    break;
                }
                ElytraFlight.mc.player.connection.sendPacket((Packet)new CPacketEntityAction((Entity)ElytraFlight.mc.player, CPacketEntityAction.Action.START_FALL_FLYING));
                break;
            }
        }
    }
    
    private double[] forwardStrafeYaw(final double forward, final double strafe, final double yaw) {
        final double[] result = { forward, strafe, yaw };
        if ((forward != 0.0 || strafe != 0.0) && forward != 0.0) {
            if (strafe > 0.0) {
                result[2] += ((forward > 0.0) ? -45 : 45);
            }
            else if (strafe < 0.0) {
                result[2] += ((forward > 0.0) ? 45 : -45);
            }
            result[1] = 0.0;
            if (forward > 0.0) {
                result[0] = 1.0;
            }
            else if (forward < 0.0) {
                result[0] = -1.0;
            }
        }
        return result;
    }
    
    private void freezePlayer(final EntityPlayer player) {
        player.motionX = 0.0;
        player.motionY = 0.0;
        player.motionZ = 0.0;
    }
    
    private void runNoKick(final EntityPlayer player) {
        if (this.noKick.getValue() && !player.isElytraFlying() && player.ticksExisted % 4 == 0) {
            player.motionY = -0.03999999910593033;
        }
    }
    
    public void onDisable() {
        if (fullNullCheck() || ElytraFlight.mc.player.capabilities.isCreativeMode) {
            return;
        }
        ElytraFlight.mc.player.capabilities.isFlying = false;
    }
    
    static {
        ElytraFlight.INSTANCE = new ElytraFlight();
    }
    
    public enum Mode
    {
        VANILLA, 
        PACKET, 
        BOOST, 
        FLY, 
        BYPASS, 
        BETTER, 
        OHARE, 
        TOOBEE, 
        TOOBEEBYPASS, 
        LOOK;
    }
}
