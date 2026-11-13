package myau.openb4.util;

import java.util.ArrayList;
import java.util.Comparator;
import myau.openb4.client.Chat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public final class TeleportPath {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private TeleportPath() {
    }

    public static void teleport(BlockPos pos, boolean verbose) {
        pos = pos.func_177981_b(1);
        Point target = new Point(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
        ArrayList<Point> path = path(new Point(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v), target);
        int n = 0;
        for (Point p : path) {
            mc.func_147114_u().func_147297_a(new C03PacketPlayer.C04PacketPlayerPosition(p.x, p.y, p.z, true));
            if (++n < 175)
                continue;
            if (!verbose)
                break;
            Chat.print("&eToo many packets, ending loop.");
            break;
        }
        mc.field_71439_g.func_70107_b(target.x, target.y, target.z);
        if (verbose) {
            Chat.print("&eTeleported to &d(" + (int) target.x + ", " + (int) target.y + ", " + (int) target.z + ") &ewith &b" + n + " &epackets.");
        }
    }

    public static ArrayList<Point> path(Point from, Point to) {
        if (!isPassable(new BlockPos(from.toVec()))) {
            from = from.add(0.0, 1.0, 0.0);
        }
        Search search = new Search(from, to);
        search.run();
        int n = 0;
        Point previous = null;
        Point anchor = null;
        ArrayList<Point> out = new ArrayList<Point>();
        ArrayList<Point> found = search.path();
        for (Point p : found) {
            if (n != 0 && n != found.size() - 1) {
                boolean straight = true;
                if (p.distanceSq(anchor) > 25.0) {
                    straight = false;
                } else {
                    double minX = Math.min(anchor.x, p.x);
                    double minY = Math.min(anchor.y, p.y);
                    double minZ = Math.min(anchor.z, p.z);
                    double maxX = Math.max(anchor.x, p.x);
                    double maxY = Math.max(anchor.y, p.y);
                    double maxZ = Math.max(anchor.z, p.z);
                    outer: for (int x = (int) minX; (double) x <= maxX; ++x) {
                        for (int y = (int) minY; (double) y <= maxY; ++y) {
                            for (int z = (int) minZ; (double) z <= maxZ; ++z) {
                                if (!Search.walkable(new Point(x, y, z))) {
                                    straight = false;
                                    break outer;
                                }
                            }
                        }
                    }
                }
                if (!straight) {
                    out.add(previous.add(0.5, 0.0, 0.5));
                    anchor = previous;
                }
            } else {
                if (previous != null) {
                    out.add(previous.add(0.5, 0.0, 0.5));
                }
                out.add(p.add(0.5, 0.0, 0.5));
                anchor = p;
            }
            previous = p;
            ++n;
        }
        return out;
    }

    public static boolean isPassable(BlockPos pos) {
        Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
        return block.func_149688_o() == Material.field_151579_a
                || block.func_149688_o() == Material.field_151585_k
                || block.func_149688_o() == Material.field_151582_l
                || block == Blocks.field_150468_ap
                || block == Blocks.field_150355_j
                || block == Blocks.field_150358_i
                || block == Blocks.field_150444_as
                || block == Blocks.field_150472_an;
    }

    public static final class Point {

        public final double x, y, z;

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3 toVec() {
            return new Vec3(this.x, this.y, this.z);
        }

        public double distanceSq(Point p) {
            return Math.pow(p.x - this.x, 2.0) + Math.pow(p.y - this.y, 2.0) + Math.pow(p.z - this.z, 2.0);
        }

        public Point add(Point p) {
            return this.add(p.x, p.y, p.z);
        }

        public Point add(double x, double y, double z) {
            return new Point(this.x + x, this.y + y, this.z + z);
        }

        public BlockPos toBlockPos() {
            return new BlockPos(this.x, this.y, this.z);
        }

        public Point floor() {
            return new Point(Math.floor(this.x), Math.floor(this.y), Math.floor(this.z));
        }
    }

    private static final class Node {

        Point point;

        Node parent;

        ArrayList<Point> path;

        double distance;

        double cost;

        double totalCost;

        Node(Point point, Node parent, ArrayList<Point> path, double distance, double cost, double totalCost) {
            this.point = point;
            this.parent = parent;
            this.path = path;
            this.distance = distance;
            this.cost = cost;
            this.totalCost = totalCost;
        }
    }

    private static final Comparator<Node> ORDER = new Comparator<Node>() {

        @Override
        public int compare(Node a, Node b) {
            return (int) (a.distance + a.totalCost - (b.distance + b.totalCost));
        }
    };

    private static final class Search {

        private static final Point[] NEIGHBOURS = { new Point(1.0, 0.0, 0.0), new Point(-1.0, 0.0, 0.0), new Point(0.0, 0.0, 1.0), new Point(0.0, 0.0, -1.0) };
        private final Point start;
        private final Point target;
        private final ArrayList<Node> open = new ArrayList<Node>();
        private final ArrayList<Node> closed = new ArrayList<Node>();
        private ArrayList<Point> result = new ArrayList<Point>();

        Search(Point from, Point to) {
            this.start = from.add(0.0, 0.0, 0.0).floor();
            this.target = to.add(0.0, 0.0, 0.0).floor();
        }

        void run() {
            this.run(1000, 4);
        }

        void run(int loops, int perLoop) {
            this.result.clear();
            this.open.clear();
            ArrayList<Point> first = new ArrayList<Point>();
            Point p = this.start;
            first.add(p);
            this.open.add(new Node(p, null, first, p.distanceSq(this.target), 0.0, 0.0));
            search: for (int i = 0; i < loops; ++i) {
                ArrayList<Node> list = this.open;
                int size = list.size();
                list.sort(ORDER);
                int n = 0;
                if (size == 0)
                    break;
                for (int j = 0; j < size; ++j) {
                    Node node = list.get(j);
                    if (++n > perLoop)
                        continue search;
                    list.remove(node);
                    this.closed.add(node);
                    Point at = node.point;
                    for (Point d : NEIGHBOURS) {
                        Point next = at.add(d).floor();
                        if (walkable(next) && this.visit(node, next, 0.0))
                            break search;
                    }
                    Point up = at.add(0.0, 1.0, 0.0).floor();
                    if (walkable(up) && this.visit(node, up, 0.0))
                        break search;
                    Point down = at.add(0.0, -1.0, 0.0).floor();
                    if (walkable(down) && this.visit(node, down, 0.0))
                        break search;
                }
            }
            this.closed.sort(ORDER);
            this.result = this.closed.get(0).path;
        }

        static boolean walkable(Point p) {
            BlockPos pos = new BlockPos(new Vec3(p.x, p.y, p.z));
            return !solid(pos) && !solid(pos.func_177982_a(0, 1, 0)) && standable(pos.func_177982_a(0, -1, 0));
        }

        static boolean standable(BlockPos pos) {
            Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
            return !(block instanceof BlockFence) && !(block instanceof BlockWall);
        }

        static boolean solid(BlockPos pos) {
            Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
            return block.func_149730_j()
                    || block instanceof BlockSlab
                    || block instanceof BlockStairs
                    || block instanceof BlockCactus
                    || block instanceof BlockChest
                    || block instanceof BlockEnderChest
                    || block instanceof BlockSkull
                    || block instanceof BlockPane
                    || block instanceof BlockFence
                    || block instanceof BlockWall
                    || block instanceof BlockGlass
                    || block instanceof BlockPistonBase
                    || block instanceof BlockPistonExtension
                    || block instanceof BlockPistonMoving
                    || block instanceof BlockStainedGlass
                    || block instanceof BlockTrapDoor;
        }

        Node find(Point p) {
            for (Node node : this.open) {
                Point q = node.point;
                if (q.x != p.x || q.y != p.y || q.z != p.z)
                    continue;
                return node;
            }
            return null;
        }

        boolean visit(Node from, Point p, double cost) {
            Node existing = this.find(p);
            double total = cost;
            if (from != null) {
                total = cost + from.totalCost;
            }
            Point t = this.target;
            ArrayList<Point> path = from.path;
            if (existing == null) {
                if (p.x == t.x && p.y == t.y && p.z == t.z) {
                    this.result.clear();
                    this.result = path;
                    this.result.add(p);
                    return true;
                }
                path.add(p);
                this.open.add(new Node(p, from, path, p.distanceSq(t), cost, total));
            } else if (existing.cost > cost) {
                path.add(p);
                existing.point = p;
                existing.parent = from;
                existing.path = path;
                existing.distance = p.distanceSq(t);
                existing.cost = cost;
                existing.totalCost = total;
            }
            return false;
        }

        ArrayList<Point> path() {
            return this.result;
        }
    }
}
