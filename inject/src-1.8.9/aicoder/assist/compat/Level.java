package aicoder.assist.compat;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;

/** The world, as far as the modules ask about it: is a block solid, can a block be put in its place. */
public final class Level {
    private final State state = new State();

    private static WorldClient w() {
        return net.minecraft.client.Minecraft.getMinecraft().theWorld;
    }

    public State getBlockState(BlockPos pos) {
        state.pos = pos;
        state.state = w().getBlockState(pos);
        return state;
    }

    public static final class State {
        BlockPos pos;
        IBlockState state;
        private final Shape shape = new Shape();

        public Shape getCollisionShape(Level level, BlockPos at) {
            Block block = state.getBlock();
            shape.empty = block.getCollisionBoundingBox(w(), at, state) == null;
            return shape;
        }

        public boolean canBeReplaced() {
            return state.getBlock().isReplaceable(w(), pos);
        }
    }

    public static final class Shape {
        boolean empty;

        public boolean isEmpty() {
            return empty;
        }
    }
}
