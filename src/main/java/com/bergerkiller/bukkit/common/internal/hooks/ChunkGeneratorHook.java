package com.bergerkiller.bukkit.common.internal.hooks;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Logging;
import com.bergerkiller.bukkit.common.events.CreaturePreSpawnEvent;
import com.bergerkiller.bukkit.common.internal.CommonBootstrap;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.generated.net.minecraft.server.BlockPositionHandle;
import com.bergerkiller.generated.net.minecraft.server.ChunkProviderServerHandle;
import com.bergerkiller.generated.net.minecraft.server.WorldServerHandle;
import com.bergerkiller.generated.net.minecraft.server.BiomeBaseHandle.BiomeMetaHandle;
import com.bergerkiller.mountiplex.conversion.util.ConvertingList;
import com.bergerkiller.mountiplex.reflection.ClassHook;
import com.bergerkiller.mountiplex.reflection.SafeField;

public class ChunkGeneratorHook {
    private static final SafeField<Object> cpsChunkGeneratorField;
    private static final Class<? extends ClassHook<?>> hookType;
    private static final Function<World, ? extends ClassHook<?>> hookConstructor;

    static {
        if (CommonBootstrap.evaluateMCVersion(">=", "1.9")) {
            cpsChunkGeneratorField = CommonUtil.unsafeCast(SafeField.create(ChunkProviderServerHandle.T.getType(),
                    "chunkGenerator", CommonUtil.getNMSClass("ChunkGenerator")));
        } else {
            cpsChunkGeneratorField = CommonUtil.unsafeCast(SafeField.create(ChunkProviderServerHandle.T.getType(),
                    "chunkProvider", CommonUtil.getNMSClass("IChunkProvider")));
        }
        if (CommonBootstrap.evaluateMCVersion(">=", "1.16")) {
            hookType = ChunkGeneratorHook_1_16.class;
            hookConstructor = ChunkGeneratorHook_1_16::new;
        } else {
            hookType = ChunkGeneratorHook_1_8_to_1_15_2.class;
            hookConstructor = ChunkGeneratorHook_1_8_to_1_15_2::new;
        }
    }

    public static void hook(org.bukkit.World world) {
        Object cps = getCPS(world);
        Object generator = cpsChunkGeneratorField.get(cps);
        ClassHook<?> hook = ClassHook.get(generator, hookType);
        if (hook == null && generator != null && !Modifier.isFinal(generator.getClass().getModifiers())) {
            try {
                hook = hookConstructor.apply(world);
                generator = hook.hook(generator);
                cpsChunkGeneratorField.set(cps, generator);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void unhook(org.bukkit.World world) {
        Object cps = getCPS(world);
        Object generator = cpsChunkGeneratorField.get(cps);
        ClassHook<?> hook = ClassHook.get(generator, hookType);
        if (hook != null) {
            try {
                generator = ClassHook.unhook(generator);
                cpsChunkGeneratorField.set(cps, generator);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static Object getCPS(org.bukkit.World world) {
        return WorldServerHandle.fromBukkit(world).getChunkProviderServer().getRaw();
    }

    private static class ChunkGeneratorHook_1_16 extends ClassHook<ChunkGeneratorHook_1_16> {
        private final World world;

        public ChunkGeneratorHook_1_16(World world) {
            this.world = world;
        }

        @HookMethod("public List getMobsFor(net.minecraft.server.BiomeBase, net.minecraft.server.StructureManager, net.minecraft.server.EnumCreatureType enumcreaturetype, net.minecraft.server.BlockPosition blockposition)")
        public List<?> getMobsFor(Object biomeBase, Object structureManager, Object enumcreaturetype, Object blockposition) {
            List<?> mobs = base.getMobsFor(biomeBase, structureManager, enumcreaturetype, blockposition);

            try {
                // First check if anyone is even interested in this information
                // There is no use wasting CPU time when no one handles the event!
                if (LogicUtil.nullOrEmpty(mobs) || !CommonUtil.hasHandlers(CreaturePreSpawnEvent.getHandlerList())) {
                    return mobs;
                }

                // Wrap the parameters and send the event along
                BlockPositionHandle pos = BlockPositionHandle.createHandle(blockposition);
                List<BiomeMetaHandle> mobsHandles = new ConvertingList<BiomeMetaHandle>(mobs, BiomeMetaHandle.T.getHandleConverter());
                mobsHandles = CommonPlugin.getInstance().getEventFactory().handleCreaturePreSpawn(this.world, 
                        pos.getX(), pos.getY(), pos.getZ(), mobsHandles);

                return new ConvertingList<Object>(mobsHandles, BiomeMetaHandle.T.getHandleConverter().reverse());
            } catch (Throwable t) {
                Logging.LOGGER.log(Level.SEVERE, "Failed to handle mob pre-spawn event", t);
            }

            return mobs;
        }
    }

    private static class ChunkGeneratorHook_1_8_to_1_15_2 extends ClassHook<ChunkGeneratorHook_1_8_to_1_15_2> {
        private final World world;

        public ChunkGeneratorHook_1_8_to_1_15_2(World world) {
            this.world = world;
        }

        @HookMethod("public List getMobsFor(net.minecraft.server.EnumCreatureType enumcreaturetype, net.minecraft.server.BlockPosition blockposition)")
        public List<?> getMobsFor(Object enumcreaturetype, Object blockposition) {
            List<?> mobs = base.getMobsFor(enumcreaturetype, blockposition);

            try {
                if (CommonPlugin.hasInstance()) {
                    // First check if anyone is even interested in this information
                    // There is no use wasting CPU time when no one handles the event!
                    if (LogicUtil.nullOrEmpty(mobs) || !CommonUtil.hasHandlers(CreaturePreSpawnEvent.getHandlerList())) {
                        return mobs;
                    }
                    // Wrap the parameters and send the event along
                    BlockPositionHandle pos = BlockPositionHandle.createHandle(blockposition);
                    List<BiomeMetaHandle> mobsHandles = new ConvertingList<BiomeMetaHandle>(mobs, BiomeMetaHandle.T.getHandleConverter());
                    mobsHandles = CommonPlugin.getInstance().getEventFactory().handleCreaturePreSpawn(this.world, 
                            pos.getX(), pos.getY(), pos.getZ(), mobsHandles);

                    return new ConvertingList<Object>(mobsHandles, BiomeMetaHandle.T.getHandleConverter().reverse());
                }
            } catch (Throwable t) {
                Logging.LOGGER.log(Level.SEVERE, "Failed to handle mob pre-spawn event", t);
            }

            return mobs;
        }
    }
}
