package me.enderkill98.enderspecimina;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    private Text text(String text) {
        return Text.literal(text);
    }

    private OptionDescription textOptDesc(String text) {
        return OptionDescription.of(Text.literal(text));
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YetAnotherConfigLib.createBuilder()
                .title(text("EnderSpecimina"))
                .category(ConfigCategory.createBuilder()
                        .name(text("EnderSpecimina"))
                        .group(OptionGroup.createBuilder()
                                .name(text("Specimina"))
                                .option(Option.<Config.ActivationPolicy>createBuilder()
                                        .name(text("Fix Ghost Items"))
                                        .binding(Config.HANDLER.defaults().fix2b2tGhostItems, () -> Config.HANDLER.instance().fix2b2tGhostItems, (newVal) -> Config.HANDLER.instance().fix2b2tGhostItems = newVal)
                                        .description(textOptDesc("""
                                                2b2t has a bug (most likely again their item tracking / dupe detection) which causes dragging certain items to create ghost items in your cursor.
                                                
                                                This has been confirmed for:
                                                 - Shulker Boxes
                                                 - Bundles
                                                 - Filled Maps
                                                
                                                The issue can appear to happen randomly but actually has a good reason:
                                                It happens every time one drags an item. You just need to move your cursor a single pixel between pressing and releasing a button for this to occur.
                                                Your client will then do a technically different place, a drag (just one slot, still counts).
                                                
                                                With the above items, 2b2t will then half-undo that drag, resulting in the ghost item in your cursor.
                                                
                                                This disables the dragging for these items. You'll only notice the difference on maps, as dragging unstackable items is quite pointless.
                                                
                                                Should be fine to set to Always. Will just deny dragging filled maps on other servers.
                                                """))
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Config.ActivationPolicy.class))
                                        .build()
                                )
                                .option(Option.<Config.ActivationPolicy>createBuilder()
                                        .name(text("Fix Bundles"))
                                        .binding(Config.HANDLER.defaults().fix2b2tBundles, () -> Config.HANDLER.instance().fix2b2tBundles, (newVal) -> Config.HANDLER.instance().fix2b2tBundles = newVal)
                                        .description(textOptDesc("""
                                                2b2t has a bug (most likely their item tracking / dupe detection) which causes the client to receive the bundle contents in reverse order.
                                                
                                                This can be observed when adding items on higher ping:
                                                 - The client will put the item in front of the bundle
                                                 - The server will then update the bundle
                                                 - The item will now have moved to the end of the bundle
                                                
                                                While this is just a minor cosmetic thing, the more annoying issue is what this fixes:
                                                If you select an item and try to take it out, you'll get the wrong item!
                                                
                                                This happens also because of the reversed order of bundle contents. Your client will
                                                always select the wrong one, unless the bundle contains an even number of items and you
                                                selected the middle one.
                                                
                                                This option will auto-reverse your selection, causing you to select the item you intended.
                                                
                                                On any other server, this will cause the very issue it fixes on 2b2t.
                                                
                                                Only set to Always if you play on lots of Zenith Proxies or otherwise do not
                                                connect to the domain "2b2t.org" (or any subdomain) as this is used for detection.
                                                """))
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Config.ActivationPolicy.class))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(text("Scroll Zoom"))
                                        .binding(Config.HANDLER.defaults().scrollZoom, () -> Config.HANDLER.instance().scrollZoom, (newVal) -> Config.HANDLER.instance().scrollZoom = newVal)
                                        .description(textOptDesc("""
                                                Whether to allow scroll zooming.
                                                
                                                Scroll Zoom can be used by holding down Alt while scrolling.
                                                
                                                If you're in third person, you can also zoom out/away from you. Can be abused as a crappy cave finder.
                                                
                                                Zooming in is done by exponentially narrowing your FOV (just like a Spyglass does) and slowing down camera movement accordingly.
                                                Zooming out (third person) is done by moving your camera exponentially farther away and turning off camera clip-prevention.
                                                
                                                §bWarning about the absence of limits:§r
                                                When §bzooming extremely far out with shaders§r it can lead to the shader erroring out at some point!
                                                Zooming in is usually fine. Can just look funky.
                                                """))
                                        .controller(TickBoxControllerBuilder::create)
                                        .build()
                                )
                                .option(Option.<Config.ScrollZoomModifier>createBuilder()
                                        .name(text("Scroll Zoom Modifier"))
                                        .binding(Config.HANDLER.defaults().scrollZoomModifier, () -> Config.HANDLER.instance().scrollZoomModifier, (newVal) -> Config.HANDLER.instance().scrollZoomModifier = newVal)
                                        .description(textOptDesc("""
                                                The (modifier) key to hold down for ScrollZoom.
                                                
                                                If you specify "Keybind", you need to specify that inside Minecrafts Keybind Settings.
                                                """))
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Config.ScrollZoomModifier.class))
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(text("Highways"))
                                        .binding(Config.HANDLER.defaults().highwaysEnabled, () -> Config.HANDLER.instance().highwaysEnabled, (newVal) -> {
                                            Config.HANDLER.instance().highwaysEnabled = newVal;
                                            Mod.highwayVisualizer.setActive(newVal);
                                        })
                                        .description(textOptDesc("""
                                                Display Highways on Xaero Mini-/World-Map.
                                                
                                                The mod will attempt to use the newest data from the officially linked Desmos-Map
                                                in the HWU-Discord. So these highways will be way more recent than XaeroPlus ones.
                                                """))
                                        .controller(TickBoxControllerBuilder::create)
                                        .build()
                                )
                                .option(Option.<Boolean>createBuilder()
                                        .name(text("Highways Detailed Names"))
                                        .binding(Config.HANDLER.defaults().highwaysDetailed, () -> Config.HANDLER.instance().highwaysDetailed, (newVal) -> Config.HANDLER.instance().highwaysDetailed = newVal)
                                        .description(textOptDesc("Display longer, more informative names when hovering over highways."))
                                        .controller(TickBoxControllerBuilder::create)
                                        .build()
                                )
                                .option(Option.<Config.ActivationPolicy>createBuilder()
                                        .name(text("Map IDs based on Name-Hashes"))
                                        .binding(Config.HANDLER.defaults().mapIdNameHashes, () -> Config.HANDLER.instance().mapIdNameHashes, (newVal) -> Config.HANDLER.instance().mapIdNameHashes = newVal)
                                        .description(textOptDesc("""
                                                This changes the map ids for received packets to be based on the name of map, if has one.
                                                
                                                This combats 2b2t changing all the map ids, each time to something different when you rejoin. With his enabled, mods like Client Maps should actually work!
                                                Only recommended on 2b2t. Any other server does not need this!
                                                
                                                This kinda modifies received packets (the resulting data). Care has been taken to not interfere with packet capture mods, but not extensively tested.
                                                This §lmight cause issues§r with Replay Mod, Flashback, WorldTools and similar mods!
                                                """))
                                        .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Config.ActivationPolicy.class))
                                        .build()
                                )
                                .build())
                        .build()
                )
                .save(() -> Config.HANDLER.save())
                .build()
                .generateScreen(parentScreen);
    }

}
