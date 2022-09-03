package net.coderbot.iris.compat.sodium.impl.shader_overrides;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.opengl.texture.GlTexture;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.draw.ChunkRenderMatrices;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.TextureUtil;
import net.coderbot.iris.shadows.ShadowRenderingState;

import java.util.Collection;

public abstract class AbstractIrisBatchedMdChunkRenderer<B extends AbstractIrisBatchedMdChunkRenderer.MdChunkRenderBatch>
        extends AbstractIrisMdChunkRenderer {
    protected Collection<B>[] renderLists;


    public AbstractIrisBatchedMdChunkRenderer(IrisChunkProgramOverrides overrides, RenderDevice device, ChunkCameraContext camera, ChunkRenderPassManager renderPassManager, TerrainVertexType vertexType) {
        super(overrides, device, camera, renderPassManager, vertexType);
    }


    //// RENDER METHODS

    @Override
    public void render(ChunkRenderPass renderPass, ChunkRenderMatrices matrices, int frameIndex) {
        // make sure a render list was created for this pass, if any
        if (this.renderLists == null) {
            return;
        }

        int passId = renderPass.getId();
        if (passId < 0 || this.renderLists.length < passId) {
            return;
        }

        var renderList = this.renderLists[passId];
        if (renderList == null) {
            return;
        }

        // if the render list exists, the pipeline probably exists (unless a new render pass was added without a reload)
        RenderPipeline<IrisChunkShaderInterface, BufferTarget> pipeline;
        if(ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            pipeline = this.shadowPipelines[passId];
        } else {
            pipeline = this.pipelines[passId];
        }

        RenderSystem.setShaderTexture(0, GlTexture.getHandle(TextureUtil.getBlockAtlasTexture()));
        pipeline.getProgram().getInterface().setup();
        this.device.useRenderPipeline(pipeline, (commandList, programInterface, pipelineState) -> {
            this.setupPerRenderList(renderPass, matrices, frameIndex, pipeline, commandList, programInterface, pipelineState);

            for (B batch : renderList) {
                this.setupPerBatch(renderPass, matrices, frameIndex, pipeline, commandList, programInterface, pipelineState, batch);

                this.issueDraw(renderPass, matrices, frameIndex, pipeline, commandList, programInterface, pipelineState, batch);
            }
        });
        pipeline.getProgram().getInterface().restore();
    }



    //// OVERRIDABLE RENDERING METHODS

    protected void setupPerRenderList(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            RenderPipeline<IrisChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            IrisChunkShaderInterface programInterface,
            PipelineState pipelineState
    ) {
        this.setupTextures(renderPass, pipelineState);
        this.setupUniforms(matrices, programInterface, pipelineState, frameIndex);

        commandList.bindElementBuffer(this.indexBuffer.getBuffer());
    }

    protected void setupPerBatch(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            RenderPipeline<IrisChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            IrisChunkShaderInterface programInterface,
            PipelineState pipelineState,
            B batch
    ) {
        commandList.bindVertexBuffer(
                BufferTarget.VERTICES,
                batch.getVertexBuffer(),
                0,
                batch.getVertexStride()
        );
    }

    protected abstract void issueDraw(
            ChunkRenderPass renderPass,
            ChunkRenderMatrices matrices,
            int frameIndex,
            RenderPipeline<IrisChunkShaderInterface, BufferTarget> pipeline,
            RenderCommandList<BufferTarget> commandList,
            IrisChunkShaderInterface programInterface,
            PipelineState pipelineState,
            B batch
    );


    //// OVERRIDABLE BATCH

    protected static class MdChunkRenderBatch {
        protected final Buffer vertexBuffer;
        protected final int vertexStride;
        protected final int commandCount;
        protected final long transformBufferOffset;

        public MdChunkRenderBatch(
                Buffer vertexBuffer,
                int vertexStride,
                int commandCount,
                long transformBufferOffset
        ) {
            this.vertexBuffer = vertexBuffer;
            this.vertexStride = vertexStride;
            this.commandCount = commandCount;
            this.transformBufferOffset = transformBufferOffset;
        }

        public Buffer getVertexBuffer() {
            return this.vertexBuffer;
        }

        public int getVertexStride() {
            return this.vertexStride;
        }

        public int getCommandCount() {
            return this.commandCount;
        }

        public long getTransformsBufferOffset() {
            return this.transformBufferOffset;
        }
    }

}
