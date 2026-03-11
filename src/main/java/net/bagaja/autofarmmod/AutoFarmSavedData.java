package net.bagaja.autofarmmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AutoFarmSavedData extends SavedData {

    private static final String KEY = "autofarmmod_blocks";
    private static final String TAG_POSITIONS = "positions";

    // BiFunction<CompoundTag, HolderLookup.Provider, T> is required in 1.21.3
    public static final SavedData.Factory<AutoFarmSavedData> FACTORY = new SavedData.Factory<>(
            AutoFarmSavedData::new,
            (tag, provider) -> AutoFarmSavedData.load(tag),
            null
    );

    private final Set<BlockPos> autoFarmBlocks;

    public AutoFarmSavedData() {
        this.autoFarmBlocks = new HashSet<>();
    }

    private AutoFarmSavedData(Set<BlockPos> positions) {
        this.autoFarmBlocks = positions;
    }

    public static AutoFarmSavedData load(CompoundTag tag) {
        Set<BlockPos> positions = new HashSet<>();
        ListTag list = tag.getList(TAG_POSITIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            NbtUtils.readBlockPos(entry, "pos").ifPresent(positions::add);
        }
        return new AutoFarmSavedData(positions);
    }

    // Correct 1.21.3 signature: save(CompoundTag, HolderLookup.Provider)
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (BlockPos pos : autoFarmBlocks) {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", NbtUtils.writeBlockPos(pos));
            list.add(entry);
        }
        tag.put(TAG_POSITIONS, list);
        return tag;
    }

    public static AutoFarmSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public Set<BlockPos> getAutoFarmBlocks() {
        return autoFarmBlocks;
    }

    public boolean contains(BlockPos pos) {
        return autoFarmBlocks.contains(pos);
    }

    public void add(BlockPos pos) {
        autoFarmBlocks.add(pos);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (autoFarmBlocks.remove(pos)) {
            setDirty();
        }
    }
}