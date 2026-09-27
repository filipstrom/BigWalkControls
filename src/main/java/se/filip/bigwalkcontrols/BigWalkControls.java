package se.filip.bigwalkcontrols;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import se.filip.bigwalkcontrols.network.ArmNetwork;

@Mod(BigWalkControls.MODID)
public class BigWalkControls {

        public static final String MODID = "bigwalkcontrols";

        public BigWalkControls(
                        IEventBus modEventBus,
                        ModContainer modContainer) {

                // Register multiplayer packets
                modEventBus.addListener(ArmNetwork::register);
        }
}