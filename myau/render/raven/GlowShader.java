package myau.render.raven;

import java.util.ArrayList;
import java.util.List;
import myau.render.raven.Shader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public class GlowShader {

    private static final Minecraft mc = Minecraft.func_71410_x();
    private static GlowShader instance;
    private int iterations;
    private final Shader down;
    private final Shader up;
    private Framebuffer capture = new Framebuffer(1, 1, false);
    private Framebuffer first = new Framebuffer(1, 1, true);
    private final List<Framebuffer> buffers = new ArrayList<Framebuffer>();

    public boolean ready() {
        return up.program > 0 && down.program > 0;
    }

    public static GlowShader get() {
        if (instance == null) {
            instance = new GlowShader();
        }
        return instance;
    }

    public GlowShader() {
        this.up = new Shader("#version 120\n\nuniform sampler2D inTexture, textureToCheck;\nuniform vec2 halfpixel, offset, iResolution;\nuniform int check;\n\nvoid main() {\n    vec2 uv = gl_FragCoord.xy / iResolution;\n\n    vec2 offset1 = vec2(-halfpixel.x, 0.0) * offset;\n    vec2 offset2 = vec2(-halfpixel.x, halfpixel.y) * offset;\n    vec2 offset3 = vec2(0.0, halfpixel.y * 2.0) * offset;\n    vec2 offset4 = vec2(halfpixel.x, halfpixel.y) * offset;\n    vec2 offset5 = vec2(halfpixel.x * 2.0, 0.0) * offset;\n    vec2 offset6 = vec2(halfpixel.x, -halfpixel.y) * offset;\n    vec2 offset7 = vec2(0.0, -halfpixel.y * 2.0) * offset;\n    vec2 offset8 = vec2(-halfpixel.x, -halfpixel.y) * offset;\n\n    vec4 sum = texture2D(inTexture, uv + offset1);\n    sum.rgb *= sum.a;\n    vec4 smpl1 = texture2D(inTexture, uv + offset2);\n    smpl1.rgb *= smpl1.a;\n    sum += smpl1 * 2.0;\n    vec4 smp2 = texture2D(inTexture, uv + offset3);\n    smp2.rgb *= smp2.a;\n    sum += smp2;\n    vec4 smp3 = texture2D(inTexture, uv + offset4);\n    smp3.rgb *= smp3.a;\n    sum += smp3 * 2.0;\n    vec4 smp4 = texture2D(inTexture, uv + offset5);\n    smp4.rgb *= smp4.a;\n    sum += smp4;\n    vec4 smp5 = texture2D(inTexture, uv + offset6);\n    smp5.rgb *= smp5.a;\n    sum += smp5 * 2.0;\n    vec4 smp6 = texture2D(inTexture, uv + offset7);\n    smp6.rgb *= smp6.a;\n    sum += smp6;\n    vec4 smp7 = texture2D(inTexture, uv + offset8);\n    smp7.rgb *= smp7.a;\n    sum += smp7 * 2.0;\n\n    vec4 result = sum / 12.0;\n    float checkAlpha = texture2D(textureToCheck, gl_TexCoord[0].st).a;\n    gl_FragColor = vec4(result.rgb / max(result.a, 0.0001), mix(result.a, result.a * (1.0 - checkAlpha), float(check)));\n}\n");
        this.down = new Shader("#version 120\n\nuniform sampler2D inTexture;\nuniform vec2 offset, halfpixel, iResolution;\n\nvoid main() {\n    vec2 uv = gl_FragCoord.xy / iResolution;\n\n    vec4 sum = texture2D(inTexture, uv);\n    sum.rgb *= sum.a;\n    sum *= 4.0;\n    \n    vec4 smp1 = texture2D(inTexture, uv - halfpixel.xy * offset);\n    smp1.rgb *= smp1.a;\n    sum += smp1;\n    \n    vec4 smp2 = texture2D(inTexture, uv + halfpixel.xy * offset);\n    smp2.rgb *= smp2.a;\n    sum += smp2;\n    \n    vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n    smp3.rgb *= smp3.a;\n    sum += smp3;\n    \n    vec4 smp4 = texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);\n    smp4.rgb *= smp4.a;\n    sum += smp4;\n    \n    vec4 result = sum / 8.0;\n    gl_FragColor = vec4(result.rgb / max(result.a, 0.0001), result.a);\n}\n");
    }

    public void render(int n, float f) {
        int i;
        this.capture.func_147609_e();
        if (n <= 0 || f <= 0.0f) {
            mc.func_147110_a().func_147610_a(true);
            return;
        }
        if (this.iterations != n
                || this.first.field_147621_c != GlowShader.mc.field_71443_c
                || this.first.field_147618_d != GlowShader.mc.field_71440_d) {
            this.setup(n);
            this.iterations = n;
        }
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a((int) 516, (float) 0.0f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 1, (int) 1);
        GL11.glClearColor((float) 0.0f, (float) 0.0f, (float) 0.0f, (float) 0.0f);
        this.pass(this.buffers.get(1), this.capture.field_147617_g, this.down, f);
        for (i = 1; i < n; ++i) {
            this.pass(this.buffers.get(i + 1), this.buffers.get((int) i).field_147617_g, this.down, f);
        }
        for (i = n; i > 1; --i) {
            this.pass(this.buffers.get(i - 1), this.buffers.get((int) i).field_147617_g, this.up, f);
        }
        Framebuffer fb = this.buffers.get(0);
        fb.func_147614_f();
        fb.func_147610_a(false);
        this.up.use();
        this.up.uniformf("offset", f, f);
        this.up.uniformi("inTexture", 0);
        this.up.uniformi("check", 1);
        this.up.uniformi("textureToCheck", 1);
        this.up.uniformf("halfpixel", 1.0f / (float) fb.field_147621_c, 1.0f / (float) fb.field_147618_d);
        this.up.uniformf("iResolution", fb.field_147621_c, fb.field_147618_d);
        GlStateManager.func_179138_g((int) 33985);
        GL11.glBindTexture((int) 3553, (int) this.capture.field_147617_g);
        GlStateManager.func_179138_g((int) 33984);
        GL11.glBindTexture((int) 3553, (int) this.buffers.get((int) 1).field_147617_g);
        RavenShapes.screenQuad();
        this.up.stop();
        GlStateManager.func_179082_a((float) 0.0f, (float) 0.0f, (float) 0.0f, (float) 0.0f);
        mc.func_147110_a().func_147610_a(false);
        GL11.glBindTexture((int) 3553, (int) this.buffers.get((int) 0).field_147617_g);
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a((int) 516, (float) 0.0f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
        RavenShapes.screenQuad();
        GlStateManager.func_179144_i((int) 0);
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a((int) 516, (float) 0.0f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
    }

    private Framebuffer resize(Framebuffer fb, boolean depth) {
        if (fb == null || fb.field_147621_c != GlowShader.mc.field_71443_c || fb.field_147618_d != GlowShader.mc.field_71440_d) {
            if (fb != null) {
                fb.func_147608_a();
            }
            return new Framebuffer(GlowShader.mc.field_71443_c, GlowShader.mc.field_71440_d, depth);
        }
        return fb;
    }

    private void setup(float f) {
        for (Framebuffer fb : this.buffers) {
            fb.func_147608_a();
        }
        this.buffers.clear();
        this.first = this.resize(null, true);
        this.buffers.add(this.first);
        int n = 1;
        while ((float) n <= f) {
            Framebuffer fb;
            fb = new Framebuffer(RavenShapes.downsample(mc.field_71443_c, n), RavenShapes.downsample(mc.field_71440_d, n), true);
            fb.func_147607_a(9729);
            GlStateManager.func_179144_i((int) fb.field_147617_g);
            GL11.glTexParameteri((int) 3553, (int) 10242, (int) 33648);
            GL11.glTexParameteri((int) 3553, (int) 10243, (int) 33648);
            GlStateManager.func_179144_i((int) 0);
            this.buffers.add(fb);
            ++n;
        }
    }

    private void pass(Framebuffer fb, int texture, Shader shader, float f) {
        fb.func_147614_f();
        fb.func_147610_a(false);
        shader.use();
        GL11.glBindTexture((int) 3553, (int) texture);
        shader.uniformf("offset", f, f);
        shader.uniformi("inTexture", 0);
        shader.uniformi("check", 0);
        shader.uniformf("halfpixel", 1.0f / (float) fb.field_147621_c, 1.0f / (float) fb.field_147618_d);
        shader.uniformf("iResolution", fb.field_147621_c, fb.field_147618_d);
        RavenShapes.screenQuad();
        shader.stop();
    }

    public void startCapture() {
        this.capture = this.resize(this.capture, false);
        this.capture.func_147614_f();
        this.capture.func_147610_a(false);
    }

    public void render(Framebuffer fb, int n, float f) {
        Framebuffer saved = this.capture;
        this.capture = fb;
        try {
            this.render(n, f);
        } finally {
            this.capture = saved;
        }
    }
}
