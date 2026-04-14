package com.guardmod;

import com.guardmod.client.ClientOnlyEntrypoint;
import com.guardmod.config.GuardCommonConfig;
import com.guardmod.net.GuardNetwork;
import com.guardmod.server.GuardServerEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(GuardMod.MOD_ID)
public class GuardMod {
    public static final String MOD_ID = "guardmod";

    public GuardMod() {
        GuardCommonConfig.register();
        GuardNetwork.init();

        MinecraftForge.EVENT_BUS.register(new GuardServerEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientOnlyEntrypoint::init);
    }
}