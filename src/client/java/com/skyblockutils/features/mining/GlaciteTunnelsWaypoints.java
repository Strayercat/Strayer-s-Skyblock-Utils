package com.skyblockutils.features.mining;

import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.SideBarUtils;
import com.skyblockutils.utils.WaypointRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class GlaciteTunnelsWaypoints {
    private static final int UMBER_COLOR   = 0xFFAA00;
    private static final int TUNGSTEN_COLOR = 0xFFAAAAAA;

    private static final List<WaypointRenderer.Waypoint> GLACITE_WAYPOINTS = new ArrayList<>(List.of(
            new WaypointRenderer.Waypoint(new BlockPos(25, 120, 269), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(49, 116, 287), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(76, 119, 267), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(21, 123, 351), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(23, 126, 363), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(33, 123, 355), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(48, 120, 376), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(38, 120, 373), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(60, 121, 356), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(99, 124, 346), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(108, 122, 309), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(98, 125, 370), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(34, 126, 445), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-1, 121, 450), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-39, 123, 429), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-22, 119, 466), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-87, 127, 454), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-91, 126, 454), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-102, 128, 476), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-66, 119, 261), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-76, 119, 262), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-55, 120, 272), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-40, 124, 283), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-98, 133, 357), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-62, 126, 341), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-46, 123, 355), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-50, 127, 367), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-79, 131, 382), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-107, 134, 396), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(83, 123, 387), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(41, 125, 441), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-73, 147, 417), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-45, 144, 446), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-29, 131, 369), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(21, 140, 418), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(32, 146, 403), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(50, 150, 406), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(105, 152, 334), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(96, 152, 324), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(85, 150, 349), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(72, 153, 320), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(10, 151, 339), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(5, 151, 322), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-2, 150, 296), "Umber", UMBER_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(48, 116, 270), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(81, 118, 301), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(84, 119, 332), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(90, 129, 371), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(9, 139, 416), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-34, 145, 432), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-58, 144, 443), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-74, 123, 445), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-46, 116, 465), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(0, 120, 460), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(40, 121, 325), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(104, 125, 359), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(52, 134, 329), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(61, 153, 334), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(26, 156, 329), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(40, 143, 330), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-2, 148, 347), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT),
            new WaypointRenderer.Waypoint(new BlockPos(-98, 150, 366), "Tungsten", TUNGSTEN_COLOR, WaypointRenderer.WaypointStyle.TEXT)
    ));

    public static void onWorldRender(WorldRenderContext context) {
        if (!ModConfig.INSTANCE.displayGlaciteWaypoints) return;

        String loc = SideBarUtils.getSideBarInfo("location");
        if (!loc.equals("⏣ Glacite Tunnels") && !loc.equals("⏣ Dwarven Base Camp")) return;

        List<WaypointRenderer.Waypoint> filtered = GLACITE_WAYPOINTS.stream()
                .filter(w -> switch (ModConfig.INSTANCE.glaciteWaypoints) {
                    case UMBER    -> w.label().equals("Umber");
                    case TUNGSTEN -> w.label().equals("Tungsten");
                    default       -> true;
                })
                .toList();

        for (WaypointRenderer.Waypoint waypoint: filtered) {
            WaypointRenderer.render(context, waypoint, 75);
        }
    }
}
