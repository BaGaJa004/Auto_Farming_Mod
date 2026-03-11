package net.bagaja.autofarmmod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AutoFarmSavedData extends SavedData {

    private static final String KEY = "autofarmmod_blocks";
    private static final String TAG_POSITIONS = "positions";

    // In 1.20.4, Factory deserializer is Function<CompoundTag, T> — no Provider
    public static final SavedData.Factory<AutoFarmSavedData> FACTORY = new SavedData.Factory<>(
            AutoFarmSavedData::new,
            AutoFarmSavedData::load,
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
            positions.add(NbtUtils.readBlockPos(entry.getCompound("pos")));
        }
        return new AutoFarmSavedData(positions);
    }

    // In 1.20.4, save() takes only CompoundTag — no HolderLookup.Provider
    @Override
    public CompoundTag save(CompoundTag tag) {
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