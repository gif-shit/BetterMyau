package myau.render.raven;

import java.util.ArrayList;
import java.util.List;
import myau.render.raven.Shader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class Blur {

    private static final Minecraft mc = Minecraft.func_71410_x();
    private static Blur instance;
    public Framebuffer first = new Framebuffer(1, 1, false);
    public List<Framebuffer> buffers = new ArrayList<Framebuffer>();
    public Shader up = new Shader("#version 120\n\nuniform sampler2D inTexture, textureToCheck;\nuniform vec2 halfpixel, offset, iResolution;\nuniform int check;\n\nvoid main() {\n    vec2 uv = gl_FragCoord.xy / iResolution;\n    vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;\n    sum += texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;\n    sum += texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;\n    vec4 average = sum / 12.0;\n    gl_FragColor = vec4(average.rgb, mix(1.0, texture2D(textureToCheck, gl_TexCoord[0].st).a, float(check)));\n}");
    public Shader down = new Shader("#version 120\n\nuniform sampler2D inTexture;\nuniform vec2 offset, halfpixel, iResolution;\n\nvoid main() {\n    vec2 uv = gl_FragCoord.xy / iResolution;\n    vec4 sum = texture2D(inTexture, uv) * 4.0;\n    sum += texture2D(inTexture, uv - halfpixel.xy * offset);\n    sum += texture2D(inTexture, uv + halfpixel.xy * offset);\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n    sum += texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);\n    gl_FragColor = vec4(sum.rgb * 0.125, 1.0);\n}");
    public Framebuffer stencil = new Framebuffer(1, 1, true);
    public int iterations;

    public boolean ready() {
        return up.program > 0 && down.program > 0;
    }

    public static Blur get() {
        if (instance == null) {
            instance = new Blur();
        }
        return instance;
    }

    public void setup(float n) {
        for (Framebuffer fb : this.buffers) {
            fb.func_147608_a();
        }
        this.buffers.clear();
        this.first = this.resize(null, false);
        this.buffers.add(this.first);
        int i = 1;
        while ((float) i <= n) {
            Framebuffer fb;
            fb = new Framebuffer(RavenShapes.downsample(mc.field_71443_c, i), RavenShapes.downsample(mc.field_71440_d, i), false);
            fb.func_147607_a(9729);
            GlStateManager.func_179144_i((int) fb.field_147617_g);
            GL11.glTexParameteri((int) 3553, (int) 10242, (int) 33648);
            GL11.glTexParameteri((int) 3553, (int) 10243, (int) 33648);
            GlStateManager.func_179144_i((int) 0);
            this.buffers.add(fb);
            ++i;
        }
    }

    public Framebuffer resize(Framebuffer fb, boolean depth) {
        if (fb == null || fb.field_147621_c != Blur.mc.field_71443_c || fb.field_147618_d != Blur.mc.field_71440_d) {
            if (fb != null) {
                fb.func_147608_a();
            }
            return new Framebuffer(Blur.mc.field_71443_c, Blur.mc.field_71440_d, depth);
        }
        return fb;
    }

    public void pass(Framebuffer fb, int texture, Shader shader, float offset) {
        fb.func_147614_f();
        fb.func_147610_a(false);
        shader.use();
        GL11.glBindTexture((int) 3553, (int) texture);
        shader.uniformf("offset", offset, offset);
        shader.uniformi("inTexture", 0);
        shader.uniformi("check", 0);
        shader.uniformf("halfpixel", 1.0f / (float) fb.field_147621_c, 1.0f / (float) fb.field_147618_d);
        shader.uniformf("iResolution", fb.field_147621_c, fb.field_147618_d);
        RavenShapes.screenQuad();
        shader.stop();
    }

    public void startStencil() {
        this.stencil = this.resize(this.stencil, false);
        this.stencil.func_147614_f();
        this.stencil.func_147610_a(false);
    }

    public void render(int n, float offset) {
        int i;
        this.stencil.func_147609_e();
        if (n <= 0 || offset <= 0.0f) {
            mc.func_147110_a().func_147610_a(true);
            return;
        }
        if (this.iterations != n
                || this.first.field_147621_c != Blur.mc.field_71443_c
                || this.first.field_147618_d != Blur.mc.field_71440_d) {
            this.setup(n);
            this.iterations = n;
        }
        this.pass(this.buffers.get(1), Blur.mc.func_147110_a().field_147617_g, this.down, offset);
        for (i = 1; i < n; ++i) {
            this.pass(this.buffers.get(i + 1), this.buffers.get((int) i).field_147617_g, this.down, offset);
        }
        for (i = n; i > 1; --i) {
            this.pass(this.buffers.get(i - 1), this.buffers.get((int) i).field_147617_g, this.up, offset);
        }
        Framebuffer out = this.buffers.get(0);
        out.func_147614_f();
        out.func_147610_a(false);
        this.up.use();
        this.up.uniformf("offset", offset, offset);
        this.up.uniformi("inTexture", 0);
        this.up.uniformi("check", 1);
        this.up.uniformi("textureToCheck", 1);
        this.up.uniformf("halfpixel", 1.0f / (float) out.field_147621_c, 1.0f / (float) out.field_147618_d);
        this.up.uniformf("iResolution", out.field_147621_c, out.field_147618_d);
        GL13.glActiveTexture((int) 33985);
        GL11.glBindTexture((int) 3553, (int) this.stencil.field_147617_g);
        GL13.glActiveTexture((int) 33984);
        GL11.glBindTexture((int) 3553, (int) this.buffers.get((int) 1).field_147617_g);
        RavenShapes.screenQuad();
        this.up.stop();
        mc.func_147110_a().func_147610_a(true);
        GL11.glBindTexture((int) 3553, (int) this.buffers.get((int) 0).field_147617_g);
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a((int) 516, (float) 0.0f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
        RavenShapes.screenQuad();
        GlStateManager.func_179144_i((int) 0);
        GlStateManager.func_179084_k();
    }
}
