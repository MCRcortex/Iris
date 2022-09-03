package net.coderbot.iris.compat.sodium.mixin.vertex_format;

import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;
import net.caffeinemc.sodium.render.chunk.region.GlobalSingleBufferProvider;
import net.caffeinemc.sodium.render.chunk.region.GlobalSparseAsyncBufferProvider;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlobalSingleBufferProvider.class)
public class MixinSingleGlobalBufferProvider {
	@Redirect(method = "<init>", remap = false,
			at = @At(value = "INVOKE",
					target = "Lnet/caffeinemc/gfx/api/buffer/BufferVertexFormat;stride()I",
					remap = false))
	private int iris$useExtendedStride(BufferVertexFormat format) {
		return BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() ? IrisModelVertexFormats.MODEL_VERTEX_XHFP.getCustomVertexFormat().stride() : format.stride();
	}
}
