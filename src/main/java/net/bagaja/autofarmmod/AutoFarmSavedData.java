package net.bagaja.autofarmmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoFarmSavedData extends SavedData {

    // Codec: serialize the set as a list of BlockPos
    private static final Codec<AutoFarmSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.listOf()
                            .fieldOf("positions")
                            .forGetter(d -> List.copyOf(d.autoFarmBlocks))
            ).apply(instance, AutoFarmSavedData::new)
    );

    // SavedDataType wires together the id, constructor, codec, and DataFixTypes
    // The simple constructor accepts a Supplier + plain Codec (no Context needed here)
    // DataFixTypes can be null — we have no legacy data to migrate
    public static final SavedDataType<@org.jetbrains.annotations.NotNull AutoFarmSavedData> TYPE = new SavedDataType<>(
            "autofarmmod_blocks",       // id → saves to <world>/data/autofarmmod_blocks.dat
            AutoFarmSavedData::new,     // Supplier<T> — called when no file exists yet
            CODEC,                      // Codec<T> — used for both save and load
            null                        // DataFixTypes — null = no datafixer needed
    );

    private final Set<BlockPos> autoFarmBlocks;

    // Called by the Codec when loading existing data from disk
    private AutoFarmSavedData(List<BlockPos> positions) {
        this.autoFarmBlocks = new HashSet<>(positions);
    }

    // Called by the Supplier in TYPE when no save file exists yet
    public AutoFarmSavedData() {
        this.autoFarmBlocks = new HashSet<>();
    }

    // Get (or create) the instance for a specific ServerLevel
    // Each dimension has its own DimensionDataStorage, so data is world+dimension scoped
    public static AutoFarmSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Accessors ---

    public Set<BlockPos> getAutoFarmBlocks() {
        return autoFarmBlocks;
    }

    public boolean contains(BlockPos pos) {
        return autoFarmBlocks.contains(pos);
    }

    public void add(BlockPos pos) {
        autoFarmBlocks.add(pos);
        setDirty(); // tells Forge to write to disk on next save
    }

    public void remove(BlockPos pos) {
        if (autoFarmBlocks.remove(pos)) {
            setDirty();
        }
    }
}