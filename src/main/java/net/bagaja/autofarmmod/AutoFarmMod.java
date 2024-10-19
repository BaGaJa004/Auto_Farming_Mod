package net.bagaja.autofarmmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod("autofarmmod")
public class AutoFarmMod {
    public static final String MOD_ID = "autofarmmod";
    private static final Set<BlockPos> autoFarmBlocks = new HashSet<>();

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

        if (world.isClientSide || !(state.getBlock() instanceof CropBlock)) return;

        CropBlock crop = (CropBlock) state.getBlock();
        if (crop.isMaxAge(state)) {
            // Harvest and replant
            harvestAndReplant(world, pos, player, crop);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);

            // Check if player is holding a diamond to make the crop automatic
            if (player.getItemInHand(hand).getItem() == Items.DIAMOND) {
                autoFarmBlocks.add(pos);
                player.getItemInHand(hand).shrink(1);
            }
        }
    }

    private void harvestAndReplant(Level world, BlockPos pos, Player player, CropBlock crop) {
        if (!(world instanceof ServerLevel)) return;
        ServerLevel serverWorld = (ServerLevel) world;

        // Get drops
        List<ItemStack> drops = CropBlock.getDrops(world.getBlockState(pos), serverWorld, pos, null);
        for (ItemStack drop : drops) {
            Block.popResource(world, pos, drop);
        }

        // Replant
        world.setBlock(pos, crop.defaultBlockState(), 3);
    }

    // You might want to add a tick event to handle automatic farms
    @SubscribeEvent
    public void onWorldTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.type == net.minecraftforge.event.TickEvent.Type.LEVEL && !event.level.isClientSide) {
            for (BlockPos pos : autoFarmBlocks) {
                BlockState state = event.level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock) {
                    CropBlock crop = (CropBlock) state.getBlock();
                    if (crop.isMaxAge(state)) {
                        harvestAndReplant(event.level, pos, null, crop);
                    }
                }
            }
        }
    }
}
