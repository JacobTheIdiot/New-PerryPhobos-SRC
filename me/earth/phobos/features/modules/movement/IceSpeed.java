



package me.earth.phobos.features.modules.movement;

import me.earth.phobos.features.modules.*;
import me.earth.phobos.features.setting.*;
import net.minecraft.init.*;

public class IceSpeed extends Module
{
    private static IceSpeed INSTANCE;
    private final Setting<Float> speed;
    
    public IceSpeed() {
        super("IceSpeed", "Speeds you up on ice.", Module.Category.MOVEMENT, false, false, false);
        this.speed = (Setting<Float>)this.register(new Setting("Speed", (T)0.4f, (T)0.1f, (T)1.5f));
        IceSpeed.INSTANCE = this;
    }
    
    public static IceSpeed getINSTANCE() {
        if (IceSpeed.INSTANCE == null) {
            IceSpeed.INSTANCE = new IceSpeed();
        }
        return IceSpeed.INSTANCE;
    }
    
    public void onUpdate() {
        Blocks.ICE.slipperiness = this.speed.getValue();
        Blocks.PACKED_ICE.slipperiness = this.speed.getValue();
        Blocks.FROSTED_ICE.slipperiness = this.speed.getValue();
    }
    
    public void onDisable() {
        Blocks.ICE.slipperiness = 0.98f;
        Blocks.PACKED_ICE.slipperiness = 0.98f;
        Blocks.FROSTED_ICE.slipperiness = 0.98f;
    }
    
    static {
        IceSpeed.INSTANCE = new IceSpeed();
    }
}
