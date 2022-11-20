package dev.momostudios.coldsweat.client.event;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.event.client.RenderWorldEvent;
import dev.momostudios.coldsweat.common.event.HearthPathManagement;
import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class HearthDebugRenderer
{
    public static Map<BlockPos, Set<Pair<BlockPos, ArrayList<Direction>>>> HEARTH_LOCATIONS = new HashMap<>();

    @SubscribeEvent
    public static void onLevelRendered(RenderWorldEvent event)
    {
        if (Minecraft.getInstance().gameSettings.showDebugInfo && ClientSettingsConfig.getInstance().hearthDebug())
        {
            PlayerEntity player = Minecraft.getInstance().player;
            if (player == null) return;

            MatrixStack ms = event.getMatrixStack();
            Vector3d camPos = event.getCamera().getProjectedView();
            World world = event.getWorld();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
            IVertexBuilder vertexes = buffer.getBuffer(RenderType.LINES);

            ms.push();
            ms.translate(-camPos.getX(), -camPos.getY(), -camPos.z);
            Matrix4f matrix4f = ms.getLast().getMatrix();
            Matrix3f matrix3f = ms.getLast().getNormal();

            // Points to draw lines
            BiConsumer<Vector3f, Float> nw = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY(), pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 1, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX(), pos.getY()+1, pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 1, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> ne = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY(), pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 1, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY()+1, pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 1, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> sw = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY(), pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, -1, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX(), pos.getY()+1, pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, -1, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> se = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY(), pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 1, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY()+1, pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 1, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> nu = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY()+1, pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, -1, 0, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY()+1, pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, -1, 0, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> nd = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY(), pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 1, 0, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY(), pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 1, 0, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> su = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY()+1, pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 1, 0, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY()+1, pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 1, 0, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> sd = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY(), pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 1, 0, 0).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY(), pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 1, 0, 0).endVertex();
            };
            BiConsumer<Vector3f, Float> eu = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY()+1, pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, 1).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY()+1, pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, 1).endVertex();
            };
            BiConsumer<Vector3f, Float> ed = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY(), pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, -1).endVertex();
                vertexes.pos(matrix4f, pos.getX()+1, pos.getY(), pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, -1).endVertex();
            };
            BiConsumer<Vector3f, Float> wu = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY()+1, pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, 1).endVertex();
                vertexes.pos(matrix4f, pos.getX(), pos.getY()+1, pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, 1).endVertex();
            };
            BiConsumer<Vector3f, Float> wd = (pos, renderAlpha) ->
            {
                vertexes.pos(matrix4f, pos.getX(), pos.getY(), pos.getZ()).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, 1).endVertex();
                vertexes.pos(matrix4f, pos.getX(), pos.getY(), pos.getZ()+1).color(1f, 0.7f, 0.6f, renderAlpha).normal(matrix3f, 0, 0, 1).endVertex();
            };

            Chunk workingChunk = (Chunk) world.getChunk(new BlockPos(0, 0, 0));

            for (Map.Entry<BlockPos, Set<Pair<BlockPos, ArrayList<Direction>>>> entry : HEARTH_LOCATIONS.entrySet())
            {
                if (HearthPathManagement.DISABLED_HEARTHS.contains(Pair.of(entry.getKey(), world.getDimensionKey().getLocation().toString()))) continue;

                Set<Pair<BlockPos, ArrayList<Direction>>> points = entry.getValue();
                for (Pair<BlockPos, ArrayList<Direction>> pair : points)
                {
                    BlockPos pos = pair.getFirst();
                    ArrayList<Direction> directions = pair.getSecond();

                    float x = pos.getX();
                    float y = pos.getY();
                    float z = pos.getZ();

                    float renderAlpha = CSMath.blend(0.2f, 0f, (float) CSMath.getDistance(player, x + 0.5f, y + 0.5f, z + 0.5f), 5, 16);

                    if (renderAlpha > 0.01f && new ClippingHelper(ms.getLast().getMatrix(), event.getLastMatrix()).isBoundingBoxInFrustum(new AxisAlignedBB(pos)))
                    {
                        ChunkPos chunkPos = new ChunkPos(pos);
                        if (!workingChunk.getPos().equals(chunkPos))
                            workingChunk = (Chunk) world.getChunkProvider().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);

                        if (WorldHelper.getBlockState(workingChunk, pos).getShape(world, pos).equals(VoxelShapes.fullCube()))
                        {
                            WorldRenderer.drawBoundingBox(ms, vertexes, x, y, z, x + 1, y + 1, z + 1, 1f, 0.7f, 0.6f, renderAlpha);
                            continue;
                        }

                        //Minecraft.getInstance().player.sendStatusMessage(new StringTextComponent((selectPos.getX() % 2 + " " + selectPos.getZ() % 2) + " " + selectPos.getX() + " " + selectPos.getZ()), true);
                        if (directions.size() == 6) continue;

                        Set<BiConsumer<Vector3f, Float>> lines = Sets.newHashSet(nw, ne, sw, se, nu, nd, su, sd, eu, ed, wu, wd);

                        // Remove the lines if another point is on the adjacent face
                        if (directions.contains(Direction.DOWN))
                            Arrays.asList(nd, sd, ed, wd).forEach(lines::remove);
                        if (directions.contains(Direction.UP))
                            Arrays.asList(nu, su, eu, wu).forEach(lines::remove);
                        if (directions.contains(Direction.NORTH))
                            Arrays.asList(nw, ne, nu, nd).forEach(lines::remove);
                        if (directions.contains(Direction.SOUTH))
                            Arrays.asList(sw, se, su, sd).forEach(lines::remove);
                        if (directions.contains(Direction.WEST))
                            Arrays.asList(nw, sw, wu, wd).forEach(lines::remove);
                        if (directions.contains(Direction.EAST))
                            Arrays.asList(ne, se, eu, ed).forEach(lines::remove);

                        lines.forEach(line -> line.accept(new Vector3f(x, y, z), renderAlpha));
                    }
                }
            }
            RenderSystem.disableBlend();
            ms.pop();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().world != null
        && Minecraft.getInstance().world.getGameTime() % 20 == 0 && Minecraft.getInstance().gameSettings.showDebugInfo
        && ClientSettingsConfig.getInstance().hearthDebug())
        {
            HEARTH_LOCATIONS.clear();
            for (Map.Entry<BlockPos, Integer> entry : HearthPathManagement.HEARTH_POSITIONS.entrySet())
            {
                BlockPos pos = entry.getKey();
                TileEntity tileEntity = Minecraft.getInstance().world.getTileEntity(pos);
                if (tileEntity == null) continue;
                Set<BlockPos> paths = ((HearthTileEntity) tileEntity).getPaths();
                HEARTH_LOCATIONS.put(pos, paths.stream().map(bp ->
                {
                    ArrayList<Direction> dirs = new ArrayList<>();
                    for (Direction dir : Direction.values())
                    {
                        if (paths.contains(bp.offset(dir)))
                            dirs.add(dir);
                    }
                    return Pair.of(bp, dirs);
                }).collect(Collectors.toSet()));
            }
        }
    }
}
