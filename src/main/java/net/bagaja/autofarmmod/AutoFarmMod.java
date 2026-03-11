package net.bagaja.autofarmmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;

@Mod("autofarmmod")
public class AutoFarmMod {
    public static final String MOD_ID = "autofarmmod";

    // No more static HashSet — data now lives in AutoFarmSavedData, tied to each ServerLevel

    public AutoFarmMod() {
        PlayerInteractEvent.RightClickBlock.BUS.addListener(this::onRightClickBlock);
        TickEvent.LevelTickEvent.Post.BUS.addListener(this::onWorldTick);
        BlockEvent.BreakEvent.BUS.addListener(this::onBlockBreak); // cleanup on block removal
    }

    public boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        // Only run server-side, only for crop blocks
        if (world.isClientSide || !(state.getBlock() instanceof CropBlock)) return false;

        if (player.getItemInHand(hand).getItem() == Items.DIAMOND) {
            // Get the SavedData for THIS specific level (dimension-safe)
            AutoFarmSavedData data = AutoFarmSavedData.get((ServerLevel) world);

            if (data.contains(pos)) {
                // Already registered — do nothing, don't consume diamond
                event.setCancellationResult(InteractionResult.PASS);
                return false;
            }

            // Register the block and persist it
            data.add(pos);  // also calls setDirty() internally

            player.getItemInHand(hand).shrink(1);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }

        return false;
    }

    /**
     * When a block is broken, remove it from the saved data if it was registered.
     * This keeps the saved data clean and prevents ghost entries.
     */
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        // BreakEvent fires on the server level
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        AutoFarmSavedData data = AutoFarmSavedData.get(serverLevel);

        // remove() internally checks if the pos exists before calling setDirty()
        data.remove(pos);
    }

    private void harvestAndReplant(Level world, BlockPos pos, CropBlock crop) {
        if (!(world instanceof ServerLevel serverWorld)) return;

        List<ItemStack> drops = CropBlock.getDrops(world.getBlockState(pos), serverWorld, pos, null);
        for (ItemStack drop : drops) {
            Block.popResource(world, pos, drop);
        }

        world.setBlock(pos, crop.defaultBlockState(), 3);
    }

    public void onWorldTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        // Load the saved data for this specific level
        AutoFarmSavedData data = AutoFarmSavedData.get(serverLevel);
        Set<BlockPos> positions = data.getAutoFarmBlocks();

        for (BlockPos pos : positions) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                harvestAndReplant(serverLevel, pos, crop);
            }
        }
    }
}