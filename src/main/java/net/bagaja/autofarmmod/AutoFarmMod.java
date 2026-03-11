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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod(AutoFarmMod.MOD_ID)
public class AutoFarmMod {
    public static final String MOD_ID = "autofarmmod";

    public AutoFarmMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        if (world.isClientSide() || !(state.getBlock() instanceof CropBlock)) return;

        if (player.getItemInHand(hand).getItem() == Items.DIAMOND) {
            AutoFarmSavedData data = AutoFarmSavedData.get((ServerLevel) world);

            if (data.contains(pos)) {
                event.setCancellationResult(InteractionResult.PASS);
                return;
            }

            data.add(pos);
            player.getItemInHand(hand).shrink(1);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        AutoFarmSavedData data = AutoFarmSavedData.get(serverLevel);
        data.remove(pos);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        // Only run at END of tick, and only server-side
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        AutoFarmSavedData data = AutoFarmSavedData.get(serverLevel);
        Set<BlockPos> positions = data.getAutoFarmBlocks();

        for (BlockPos pos : positions) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                harvestAndReplant(serverLevel, pos, crop);
            }
        }
    }

    private void harvestAndReplant(ServerLevel world, BlockPos pos, CropBlock crop) {
        for (ItemStack drop : CropBlock.getDrops(world.getBlockState(pos), world, pos, null)) {
            Block.popResource(world, pos, drop);
        }
        world.setBlock(pos, crop.defaultBlockState(), 3);
    }
}