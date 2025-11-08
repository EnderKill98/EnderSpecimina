package me.enderkill98.enderspecimina;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.enderkill98.enderspecimina.render.RenderManager;
import me.enderkill98.enderspecimina.render.Renderable;
import me.enderkill98.enderspecimina.render.renderables.Map2DLines;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class HighwayVisualizer implements ClientTickEvents.EndTick, Renderable {

    private final Logger logger = Mod.getLoggerFor(this.getClass());

    public record Highway(@Nullable String roadName, Map2DLines.Line line, Map2DLines.Line lineOverworld, @Nullable String state, @Nullable String folder) {
        @Override
        public @NotNull String toString() {
            String roadName = Config.HANDLER.instance().highwaysDetailed ? roadName() : shortRoadName();
            String state = shortState();

            String prefix = "";
            if(roadName == null) {
                roadName = folder;
            }else if(folder != null) {
                if(Config.HANDLER.instance().highwaysDetailed) {
                    prefix = folder + ": ";
                }else {
                    String roadNorm = roadName.toLowerCase().replaceAll(" ", "");
                    String folderNorm = folder.toLowerCase().replaceAll(" ", "");
                    if (folderNorm.endsWith("s"))
                        folderNorm = folderNorm.substring(0, folderNorm.length() - 1);
                    if (!roadNorm.contains(folderNorm))
                        prefix = folder + ": ";
                }
            }

            return prefix + roadName + " (" + state + ")";
        }

        public String shortRoadName() {
            String roadName = roadName();
            if(roadName != null && roadName.contains("(")) roadName = roadName.substring(0, roadName.indexOf("(")).strip();
            return roadName;
        }

        public String shortState() {
            String state = state();
            if(state != null && state.contains("(")) state = state.substring(0, state.indexOf("(")).strip();
            return state;
        }
    }

    public String SOURCE_URL = "https://www.desmos.com/calc-states/production/version/siw620fvnu/5bf8ddb0-abfc-11f0-9960-3d9cc4158144?cb20221031=1";

    public ArrayList<Highway> HIGHWAYS = new ArrayList<>();
    public String highlightedHighwayName = null;
    public long highlightedHighwayNameAt = -1L;

    public HighwayVisualizer() {
        loadInBackground();
    }

    public void loadInBackground() {
        new Thread(this::load).start();
    }

    public void load() {
        HIGHWAYS.clear();
        try {
            String sourceUrl = Config.HANDLER.instance().highwaysOverwriteSourceUrl != null && !Config.HANDLER.instance().highwaysOverwriteSourceUrl.isBlank() ? Config.HANDLER.instance().highwaysOverwriteSourceUrl : SOURCE_URL;
            URLConnection conn = new URL(sourceUrl).openConnection();
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JsonObject obj = gson.fromJson(br, JsonObject.class);
            br.close();

            HashMap<Integer/*RGB*/, String> stateLegend = new HashMap<>();
            HashMap<Integer/*ID*/, String> folderNames = new HashMap<>();
            String lastText = null;
            for(JsonElement exprObj : obj.getAsJsonObject("expressions").getAsJsonArray("list")) {
                JsonObject expr = exprObj.getAsJsonObject();

                String type = expr.get("type").getAsString();
                String folder = expr.has("folderId") ? folderNames.getOrDefault(expr.get("folderId").getAsInt(), null) : null;

                if(type.equals("folder")) {
                    folderNames.put(expr.get("id").getAsInt(), expr.get("title").getAsString());
                    lastText = null;
                } else if(type.equals("text")) {
                    lastText = expr.get("text").getAsString();
                } else if(type.equals("expression")) {
                    if(folder != null && folder.equals("Legend")) {
                        stateLegend.put(Color.decode(expr.get("color").getAsString()).getRGB(), expr.get("label").getAsString());
                    }
                } else if(type.equals("table")) {
                    JsonArray columns = expr.getAsJsonArray("columns");

                    JsonObject xCol = columns.get(0).getAsJsonObject();
                    JsonArray xColValues = xCol.getAsJsonArray("values");
                    JsonObject zCol = columns.get(1).getAsJsonObject();
                    JsonArray zColValues = zCol.getAsJsonArray("values");

                    int color = 0;
                    if(xCol.has("color"))
                        color = Color.decode(xCol.get("color").getAsString()).getRGB();  // getRGB() can be ARGB!
                    if(zCol.has("color")) // Prefer this over x
                        color = Color.decode(zCol.get("color").getAsString()).getRGB();
                    String state = stateLegend.getOrDefault(color, null);

                    Pair<Double, Double> lastPoint = null;
                    for(int i = 0; i < Math.min(xColValues.size(), zColValues.size()); i++) {
                        String xStr = xColValues.get(i).getAsString();
                        String zStr = zColValues.get(i).getAsString();
                        if(xStr.isBlank() || zStr.isBlank()) {
                            lastPoint = null; // Disconnect lines
                            continue;
                        }

                        Pair<Double, Double> point = new Pair<>(Double.parseDouble(xStr), Double.parseDouble(zStr));
                        if(lastPoint != null) {
                            // Add highway
                            int adjColor = (color & 0xFFFFFF) == 0x000000 ? 0x008014 : color; // Turn black (finished into green)
                            //int adjColor = color;
                            adjColor |= 0xFF000000; // Add alpha
                            double x1 = lastPoint.getLeft();
                            double y1 = lastPoint.getRight() * -1; // Invert, because Z axis is switched to be shown correctly on Desmos
                            double x2 = point.getLeft();
                            double y2 = point.getRight() * -1;
                            Map2DLines.Line lineNether = new Map2DLines.Line(x1, y1, x2, y2, adjColor);
                            Map2DLines.Line lineOverworld = new Map2DLines.Line(x1*8, y1*8, x2*8, y2*8, adjColor);
                            Highway highway = new Highway(lastText, lineNether, lineOverworld, state, folder);
                            //logger.info("Added {} => Color {}", highway, "0x" + new String(Hex.encodeHex(new byte[] { (byte) (highway.line.argb() >> 24), (byte) ((highway.line.argb() >> 16) & 0xFF), (byte) ((highway.line.argb() >> 8) & 0xFF), (byte) (highway.line.argb() & 0xFF)})));
                            HIGHWAYS.add(highway);
                        }
                        lastPoint = point;
                    }
                }
            }

            logger.info("Found {} Highways!", HIGHWAYS.size());
        }catch (Exception ex) {
            logger.warn("Failed to query highways from Desmos source!", ex);
        }
    }

    private boolean active = false;

    public void setActive(boolean active) {
        this.active = active;
        updateRenderables();
        highlightedHighwayName = null;
        highlightedHighwayNameAt = -1L;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onEndTick(MinecraftClient client) {
        if(!active) return;
        updateRenderables();
    }

    public void updateRenderables() {
        RenderManager.removeAllRenderables(HighwayVisualizer.class);
        if(!active) return;
        RenderManager.addRenderable(HighwayVisualizer.class, this);
    }


    @Override
    public void renderMap(MinecraftClient client, DrawContext context, @Nullable TextRenderer textRenderer, MatrixStack matrices, VertexConsumerProvider.Immediate renderTypeBuffers, VertexConsumer lineBufferBuilder, double mapScale, double cameraX, double cameraZ, float tickDelta, RenderManager.MapInfo mapInfo) {
        final String highlightedHighwayName = this.highlightedHighwayName != null && highlightedHighwayNameAt != -1L && Util.getMeasuringTimeMs() - highlightedHighwayNameAt < 50 ? this.highlightedHighwayName : null;
        final double margin = 0;
        // Testing
        /*new Map2DRectangles(new Map2DRectangles.Rectangle(mapInfo.minX() + margin, mapInfo.minZ() + margin, mapInfo.maxX() - margin, mapInfo.maxZ() - margin, 0xFFFF00FF))
                .renderMap(client, context, textRenderer, matrices, renderTypeBuffers, lineBufferBuilder, mapScale, cameraX, cameraZ, tickDelta, mapInfo);*/
        if(mapInfo.world() == null || mapInfo.world() == RenderManager.MapWorld.End) return;

        ArrayList<Map2DLines.Line> lines = new ArrayList<>(HIGHWAYS.size());
        ArrayList<Map2DLines.Line> linesHighlighted = new ArrayList<>();
        //int rendered = 0, highlighted = 0;
        for(Highway highway : HIGHWAYS) {
            Map2DLines.Line line = Map2DLines.clipToViewport(mapInfo.world() == RenderManager.MapWorld.Nether ? highway.line : highway.lineOverworld, mapInfo.minX() + margin, mapInfo.minZ() + margin, mapInfo.maxX() - margin, mapInfo.maxZ() - margin);
            if(line == null) continue; // Got culled
            if(highlightedHighwayName != null && highlightedHighwayName.equals(highway.toString())) {
                int a = line.argb() >> 24;
                int r = (line.argb() >> 16) & 0xff;
                int g = (line.argb() >> 8) & 0xff;
                int b = line.argb() & 0xff;

                // Cheaply brighten color
                r = Math.min(r + 50, 255);
                g = Math.min(g + 50, 255);
                b = Math.min(b + 50, 255);
                linesHighlighted.add(line.withColor(a << 24 | r << 16 | g << 8 | b));
                //highlighted++;
                //rendered++;
            }else {
                lines.add(line);
                //rendered++;
            }
        }

        new Map2DLines(lines.toArray(Map2DLines.Line[]::new))
                .renderMap(client, context, textRenderer, matrices, renderTypeBuffers, lineBufferBuilder, mapScale, cameraX, cameraZ, tickDelta, mapInfo);
        new Map2DLines(linesHighlighted.toArray(Map2DLines.Line[]::new))
                .renderMap(client, context, textRenderer, matrices, renderTypeBuffers, lineBufferBuilder, mapScale, cameraX, cameraZ, tickDelta, mapInfo);
        //logger.info("Rendered {}/{} highways ({} highlighted)!", rendered, HIGHWAYS.size(), highlighted);
    }

    public @Nullable Highway findClosestHighway(int x, int z, double maxDist, boolean isOverworld) {
        Vec3d ownPos = new Vec3d(x, 0, z);
        double maxDistSqrt = maxDist * maxDist;

        Highway bestHighway = null;
        double bestDistSqrt = Double.MAX_VALUE;

        for(Highway highway : HIGHWAYS) {
            Map2DLines.Line line = isOverworld ? highway.lineOverworld : highway.line;
            Vec3d start = new Vec3d(line.x1(), 0, line.y1());
            Vec3d end = new Vec3d(line.x2(), 0, line.y2());
            Vec3d min = new Vec3d(Math.min(start.getX(), end.getX()), 0, Math.min(start.getZ(), end.getZ()));
            Vec3d max = new Vec3d(Math.max(start.getX(), end.getX()), 0, Math.max(start.getZ(), end.getZ()));

            if(x < min.getX() - maxDist || x > max.getX() + maxDist && z < min.getZ() - maxDist || z > max.getZ() + maxDist)
                continue; // Too far (and would possibly flag wrong line that ended ages ago)
            double distSqrt = calculateDistSqrtToLine(start, end.subtract(start), ownPos);

            // Bias selecting more finished versions of roads (to prevent highlighting "digging up" the old road state)
            double bias = 0;
            String shortState = highway.shortState();
            if(shortState != null) {
                if(shortState.equalsIgnoreCase("Dug")) bias = 0.1;
                else if(shortState.equalsIgnoreCase("Paved")) bias = 0.2;
            }
            if(distSqrt <= maxDistSqrt && distSqrt - bias < bestDistSqrt) {
                bestHighway = highway;
                bestDistSqrt = distSqrt;
            }
        }
        return bestHighway;
    }

    public static double calculateDistSqrtToLine(Vec3d pos, Vec3d direction, Vec3d posHowClose) {
        double posToPos = pos.distanceTo(posHowClose);
        Vec3d newPos = pos.add(direction.normalize().multiply(posToPos));
        return newPos.squaredDistanceTo(posHowClose);
    }

}
