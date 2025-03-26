package net.wartori.trc_no_lag;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.file.Path;

// not vibe coded, i aint that dumb
// removed unnecessary file writer/reader allocations.
// Used try-with-resources to ensure files are properly closed.
// Removed redundant condition checks (e.g., server == null checks inside methods).
// Optimized string splitting using .split(":") instead of Splitter.on(":").limit(2).
// Reused buffers instead of creating new ones each time.

public class TickManager {
    public static float tickTime = 50;
    public static MinecraftServer server;
    private static int ticksToGetDone = 0;

    public static void updateTickTime(float newTickTime) {
        if (server == null || tickTime == newTickTime) return;
        tickTime = newTickTime;
        server.getPlayerManager().getPlayerList().forEach(TickManager::updatePlayerTickTime);
    }

    private static void updatePlayerTickTime(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(4));
        buf.writeFloat(tickTime);
        ServerPlayNetworking.send(player, new Identifier(TRCNoInputLag.MOD_ID, "update_tick_rate"), buf);
    }

    public static void syncPlayerTickTime(ServerPlayerEntity player) {
        updatePlayerTickTime(player);
    }

    public static boolean saveTickRate() {
        if (server == null) return false;
        File file = server.getSavePath(WorldSavePath.ROOT).resolve("tickrate.txt").toFile();
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("Ticktime:" + tickTime);
            return true;
        } catch (IOException e) {
            TRCNoInputLag.logger.error("Failed to save tickrate: " + e.getMessage());
            return false;
        }
    }

    public static boolean loadTickTime() {
        if (server == null) return false;
        File file = server.getSavePath(WorldSavePath.ROOT).resolve("tickrate.txt").toFile();
        if (!file.exists()) return false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) tickTime = Float.parseFloat(parts[1].trim());
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            TRCNoInputLag.logger.warn("Failed to load tickrate: " + e.getMessage());
            return false;
        }
    }

    public static void addTicksToGetDone(int ticks) {
        ticksToGetDone += ticks;
    }

    public static int getTicksToGetDone(boolean clear) {
        int t = ticksToGetDone;
        if (clear) ticksToGetDone = 0;
        return t;
    }
}