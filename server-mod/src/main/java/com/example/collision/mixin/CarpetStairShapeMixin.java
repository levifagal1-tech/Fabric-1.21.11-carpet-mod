package com.example.collision.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Targets the shared Block base class (guarded by `instanceof CarpetBlock`) rather than
// CarpetBlock directly: plain wool carpet doesn't declare its own createBlockStateDefinition
// or getStateForPlacement, so there's nothing on CarpetBlock's own class for Mixin to attach
// to for those two - they only exist on Block, and CarpetBlock just inherits them unchanged.
// canSurvive IS commonly overridden by simple blocks like carpet; if placement on a stair
// still gets rejected in-game, that override not calling super() is the first thing to check -
// it would need a dedicated @Mixin(CarpetBlock.class) instead of this base-class hook.
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

		if (!below.is(BlockTags.STAIRS)) {
			return;
		}

		if (below.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			result = result.setValue(BlockStateProperties.HORIZONTAL_FACING, below.getValue(BlockStateProperties.HORIZONTAL_FACING));
		}
		if (below.hasProperty(BlockStateProperties.STAIRS_SHAPE)) {
			result = result.setValue(BlockStateProperties.STAIRS_SHAPE, below.getValue(BlockStateProperties.STAIRS_SHAPE));
		}

		cir.setReturnValue(result);
	}

	@Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
	private void examplecollision$allowCarpetOnStairs(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof CarpetBlock) || cir.getReturnValueZ()) {
			return;
		}

		if (level.getBlockState(pos.below()).is(BlockTags.STAIRS)) {
			cir.setReturnValue(true);
		}
	}
}
