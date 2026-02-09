package dev.wyfy.createbackpackage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BackpackBlock
    extends HorizontalDirectionalBlock
    implements EntityBlock
{

    public static final MapCodec<BackpackBlock> CODEC = simpleCodec(
        BackpackBlock::new
    );

    // Backpack-sized hitbox matching the model geometry
    private static final VoxelShape SHAPE_NS = Block.box(
        1,
        0,
        1.5,
        15.25,
        14,
        9
    );
    private static final VoxelShape SHAPE_EW = Block.box(
        1.5,
        0,
        1,
        9,
        14,
        15.25
    );

    public BackpackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
        StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(
            FACING,
            context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        Direction facing = state.getValue(FACING);
        return (facing == Direction.NORTH || facing == Direction.SOUTH)
            ? SHAPE_NS
            : SHAPE_EW;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (
            !level.isClientSide && player instanceof ServerPlayer serverPlayer
        ) {
            if (level.getBlockEntity(pos) instanceof BackpackBlockEntity be) {
                BackpackLocation location = new BackpackLocation.InBlock(pos);

                serverPlayer.openMenu(
                    new SimpleMenuProvider(
                        (containerId, playerInventory, p) ->
                            new BackpackMenu(
                                containerId,
                                playerInventory,
                                be.getInventory(),
                                location
                            ),
                        Component.translatable(
                            "container.create_backpackage.backpack"
                        )
                    ),
                    buf -> location.writeToBuf(buf)
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof BackpackBlockEntity be) {
            be.loadFromItem(stack);
        }
    }

    @Override
    public BlockState playerWillDestroy(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (
            !level.isClientSide &&
            level.getBlockEntity(pos) instanceof BackpackBlockEntity be
        ) {
            if (!player.isCreative()) {
                ItemStack drop = new ItemStack(this);
                be.saveToItem(drop);
                Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    drop
                );
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState newState,
        boolean movedByPiston
    ) {
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
