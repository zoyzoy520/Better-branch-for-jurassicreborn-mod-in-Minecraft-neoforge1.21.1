package net.example.jrtameall;

import net.example.jrtameall.item.TamingStickItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.CreativeModeTabs;

/**
 * JR Tame All - unlocks the imprinting (taming) system for ALL Jurassic Reborn
 * dinosaur species, adds peace rules between dinosaurs, a creative-mode
 * Taming Stick, and machine speed/luck boosts.
 */
@Mod("jr_tame_all")
public class JRTameAll {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.Items.createItems("jr_tame_all");

    /** Creative-only tool: right-click a wild dinosaur to force-tame it. */
    public static final DeferredItem<TamingStickItem> TAMING_STICK =
            ITEMS.registerItem("taming_stick", TamingStickItem::new);

    public JRTameAll(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
    }

    /** Puts the taming stick in the vanilla Tools & Utilities creative tab. */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TAMING_STICK.get());
        }
    }
}
