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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod("autofarmmod")
public class AutoFarmMod {
    public static final String MOD_ID = "autofarmmod";
    private static final Set<BlockPos> autoFarmBlocks = new HashSet<>();

    public AutoFarmMod() {
        // Register to the specific event buses instead of MinecraftForge.EVENT_BUS
        PlayerInteractEvent.RightClickBlock.BUS.addListener(this::onRightClickBlock);
        TickEvent.LevelTickEvent.Post.BUS.addListener(this::onWorldTick);
        net.minecraftforge.event.level.LevelEvent.Unload.BUS.addListener(this::onLevelUnload);
    }

    public boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        if (world.isClientSide || !(state.getBlock() instanceof CropBlock)) return false;

        if (player.getItemInHand(hand).getItem() == Items.DIAMOND) {
            if (autoFarmBlocks.contains(pos)) {
                // Already registered, don't consume diamond
                event.setCancellationResult(InteractionResult.PASS);
                return false;
            }
            autoFarmBlocks.add(pos);
            player.getItemInHand(hand).shrink(1);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }

        return false;
    }

    public void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        autoFarmBlocks.clear();
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

        for (BlockPos pos : autoFarmBlocks) {
            BlockState state = event.level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                harvestAndReplant(event.level, pos, crop);
            }
        }
    }
}