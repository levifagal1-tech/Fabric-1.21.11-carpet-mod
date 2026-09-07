package com.example.collision.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Targets the shared Block base class (guarded by `instanceof CarpetBlock`) rather than
// CarpetBlock directly: plain wool carpet doesn't declare its own createBlockStateDefinition,
// getStateForPlacement or getCollisionShape, so there's nothing on CarpetBlock's own class for
// Mixin to attach to for those - they only exist on Block, and CarpetBlock just inherits them
// unchanged (its custom thin shape lives in getShape, which getCollisionShape delegates to by
// default). canSurvive IS commonly overridden by simple blocks like carpet; if placement on a
// stair still gets rejected in-game, that override not calling super() is the first thing to
// check - it would need a dedicated @Mixin(CarpetBlock.class) instead of this base-class hook.
//
// Only reacts to bottom-half stairs: an upside-down (half=top) stair already has a flat, full
// top face, so there's no notch for the carpet to drop into - it's left as plain carpet.
@Mixin(Block.class)
public abstract class CarpetStairShapeMixin {

	@Inject(method = "createBlockStateDefinition", at = @At("RETURN"))
	private void examplecollision$addStairShapeProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
		if ((Object) this instanceof CarpetBlock) {
			builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.STAIRS_SHAPE);
		}
	}

	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void examplecollision$copyStairShapeOnPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		if (!((Object) this instanceof CarpetBlock)) {
			return;
		}

		BlockState result = cir.getReturnValue();
		if (result == null) {
			return;
		}

		Level level = context.getLevel();
		BlockState below = level.getBlockState(context.getClickedPos().below());

		if (!examplecollision$isBottomStair(below)) {
			return;
		}

		result = result.setValue(BlockStateProperties.HORIZONTAL_FACING, below.getValue(BlockStateProperties.HORIZONTAL_FACING));
		result = result.setValue(BlockStateProperties.STAIRS_SHAPE, below.getValue(BlockStateProperties.STAIRS_SHAPE));

		cir.setReturnValue(result);
	}

	@Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
	private void examplecollision$allowCarpetOnStairs(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof CarpetBlock) || cir.getReturnValueZ()) {
			return;
		}

		if (examplecollision$isBottomStair(level.getBlockState(pos.below()))) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getCollisionShape", at = @At("RETURN"), cancellable = true)
	private void examplecollision$carpetStairCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
		if (!((Object) this instanceof CarpetBlock)) {
			return;
		}

		BlockState below = level.getBlockState(pos.below());
		if (!examplecollision$isBottomStair(below)) {
			return;
		}

		Direction facing = below.getValue(BlockStateProperties.HORIZONTAL_FACING);
		StairsShape shape = below.getValue(BlockStateProperties.STAIRS_SHAPE);
		cir.setReturnValue(examplecollision$carpetStairShape(facing, shape));
	}

	private static boolean examplecollision$isBottomStair(BlockState state) {
		return state.is(BlockTags.STAIRS)
				&& state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
				&& state.hasProperty(BlockStateProperties.STAIRS_SHAPE)
				&& state.hasProperty(BlockStateProperties.HALF)
				&& state.getValue(BlockStateProperties.HALF) == Half.BOTTOM;
	}

	// Splits the carpet's footprint into 4 pixel-scale quadrants and drops each one 8 pixels -
	// still only 1 pixel thick - wherever the stair below is low there instead of full height,
	// so the carpet's thin slice always rests directly on the stair's actual surface.
	//
	// The low/tall and clockwise/counterclockwise classification below was derived by fetching
	// vanilla's own block/stairs.json + block/inner_stairs.json + block/outer_stairs.json models
	// and the oak_stairs.json blockstate rotation table from a public Minecraft assets mirror on
	// GitHub, and working out the actual box coordinates for facing=east (the one variant with
	// zero applied rotation, so it's the canonical reference) plus a couple of rotated variants
	// to confirm the rotation math. Two things came out backwards from the first version:
	//  - `facing` points at the TALL side, not the low/tread side (the unrotated east model's
	//    raised box sits at x:8-16, the east half - same direction as facing).
	//  - inner and outer don't share one left/right convention: inner_right's low corner sits
	//    counterclockwise of facing, while outer_right's tall corner sits clockwise of facing -
	//    they're mirror-image rules, not the same rule reused.
	//
	// NOTE: the "down" boxes have negative Y, i.e. they extend below this block's own bounds
	// into the stair's block space. That's untested against real Minecraft in this environment
	// (no network access to build/run here) - if collision behaves oddly, the alternative is to
	// instead extend the *stair's* own collision shape upward by 1 pixel for the matching
	// quadrants, keeping each block's shape within its own bounds.
	private static VoxelShape examplecollision$carpetStairShape(Direction facing, StairsShape shape) {
		Direction clockwise = facing.getClockWise();
		VoxelShape result = Shapes.empty();

		for (int cornerX : new int[] {0, 8}) {
			for (int cornerZ : new int[] {0, 8}) {
				double dx = (cornerX + 4) - 8;
				double dz = (cornerZ + 4) - 8;
				boolean low = dx * facing.getStepX() + dz * facing.getStepZ() < 0;
				boolean clockwiseSide = dx * clockwise.getStepX() + dz * clockwise.getStepZ() > 0;
				boolean down = examplecollision$isQuadrantDown(shape, low, clockwiseSide);

				double minY = down ? -8.0 : 0.0;
				double maxY = down ? -7.0 : 1.0;
				result = Shapes.or(result, Block.box(cornerX, minY, cornerZ, cornerX + 8, maxY, cornerZ + 8));
			}
		}

		return result;
	}

	private static boolean examplecollision$isQuadrantDown(StairsShape shape, boolean low, boolean clockwiseSide) {
		return switch (shape) {
			case INNER_LEFT -> low && clockwiseSide;
			case INNER_RIGHT -> low && !clockwiseSide;
			case OUTER_LEFT -> low || clockwiseSide;
			case OUTER_RIGHT -> low || !clockwiseSide;
			default -> low;
		};
	}
}
