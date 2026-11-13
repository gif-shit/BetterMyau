package myau;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class BpsModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ArrayDeque<double[]> samples = new ArrayDeque();
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;
    private double peakBps = 0.0;
    private boolean serverSprinting = false;
    private boolean lastSentSprinting = false;
    private int attacks = 0;
    private int slowedAttacks = 0;
    public final IntegerSetting averageSeconds = new IntegerSetting("average-seconds", 5, 1, 30);
    public final BooleanSetting showPeak = new BooleanSetting("show-peak", true);
    public final BooleanSetting showHits = new BooleanSetting("show-hits", true);
    public final PercentSetting posX = new PercentSetting("pos-x", 2);
    public final PercentSetting posY = new PercentSetting("pos-y", 40);

    public BpsModule() {
        super("BPS", false, 0);
    }

    @EventHandler
    public void onPreTick(PreTickEvent preTickEvent) {
        double d;
        if (!this.isEnabled() || minecraft.field_71439_g == null) {
            return;
        }
        double d2 = minecraft.field_71439_g.field_70165_t;
        double d3 = minecraft.field_71439_g.field_70161_v;
        long l = System.currentTimeMillis();
        if (!Double.isNaN(this.lastX) && (d = Math.hypot(d2 - this.lastX, d3 - this.lastZ)) < 10.0) {
            this.samples.addLast(new double[] { l, d });
        }
        this.lastX = d2;
        this.lastZ = d3;
        long l2 = Math.max(1000L, (long) this.averageSeconds.getValue() * 1000L);
        while (!this.samples.isEmpty() && l - (long) this.samples.peekFirst()[0] > l2) {
            this.samples.removeFirst();
        }
        this.peakBps = Math.max(this.peakBps, this.bpsOver(1000L, l));
    }

    private double bpsOver(long l, long l2) {
        double d = 0.0;
        for (double[] dArray : this.samples) {
            if (l2 - (long) dArray[0] > l)
                continue;
            d += dArray[1];
        }
        return d / ((double) l / 1000.0);
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent packetSendEvent) {
        if (packetSendEvent.getPacket() instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction.Action action = ((C0BPacketEntityAction) packetSendEvent.getPacket()).func_180764_b();
            if (action == C0BPacketEntityAction.Action.START_SPRINTING) {
                this.serverSprinting = true;
            } else if (action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                this.serverSprinting = false;
            }
        } else if (packetSendEvent.getPacket() instanceof C03PacketPlayer) {
            this.lastSentSprinting = this.serverSprinting;
        } else if (this.isEnabled()
                && packetSendEvent.getPacket() instanceof C02PacketUseEntity
                && ((C02PacketUseEntity) packetSendEvent.getPacket()).func_149565_c() == C02PacketUseEntity.Action.ATTACK) {
            ++this.attacks;
            if (this.lastSentSprinting) {
                ++this.slowedAttacks;
            }
        }
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent renderOverlayEvent) {
        if (!this.isEnabled() || minecraft.field_71439_g == null) {
            return;
        }
        long l = System.currentTimeMillis();
        int n = this.averageSeconds.getValue();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("BPS: %.2f", this.bpsOver(1000L, l)));
        stringBuilder.append(String.format("  avg(%ds): %.2f", n, this.bpsOver((long) n * 1000L, l)));
        if (this.showPeak.getValue()) {
            stringBuilder.append(String.format("  peak: %.2f", this.peakBps));
        }
        HUDModule hUDModule = (HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class);
        float f = hUDModule.scale.getValue();
        boolean bl = hUDModule.shadow.getValue();
        FontRenderer fontRenderer = minecraft.field_71466_p;
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        float f2 = (float) scaledResolution.func_78326_a() * this.posX.getValue().floatValue() / 100.0f / f;
        float f3 = (float) scaledResolution.func_78328_b() * this.posY.getValue().floatValue() / 100.0f / f;
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float) f, (float) f, (float) 0.0f);
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
        fontRenderer.func_175065_a(stringBuilder.toString(), f2, f3, -1, bl);
        if (this.showHits.getValue()) {
            int n2 = this.attacks - this.slowedAttacks;
            String string = String.format("hits: %d  no-slowdown: %d  slowed: %d", this.attacks, n2, this.slowedAttacks);
            fontRenderer.func_175065_a(string, f2, f3 + (float) fontRenderer.field_78288_b + 2.0f, this.slowedAttacks == 0 ? -11141291 : -21931, bl);
        }
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179121_F();
    }

    @Override
    public void onEnable() {
        this.samples.clear();
        this.lastX = Double.NaN;
        this.lastZ = Double.NaN;
        this.peakBps = 0.0;
        this.attacks = 0;
        this.slowedAttacks = 0;
    }
}
