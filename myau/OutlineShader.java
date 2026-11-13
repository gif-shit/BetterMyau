package myau;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

public class OutlineShader extends ShaderProgram {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final String FRAGMENT_SHADER_SOURCE = "#version 120\nuniform sampler2D texture;\nuniform vec2 size;\nuniform float radius;\nvoid main(void) {\ngl_FragColor = vec4(0.0);\nvec4 xy = texture2D(texture, gl_TexCoord[0].xy);\nif (xy.a == 0.0) {\nfor (float x = -radius; x <= radius; x++) {\nfor (float y = -radius; y <= radius; y++) {\nvec4 color = texture2D(texture, gl_TexCoord[0].xy + vec2(size.x * x, size.y * y));\nif (color.a != 0.0) {\ngl_FragColor = color;\n}\n}\n}\n}\n}\n";

    public OutlineShader() {
        super(FRAGMENT_SHADER_SOURCE);
    }

    @Override
    protected void registerUniforms() {
        this.registerUniform("texture");
        this.registerUniform("size");
        this.registerUniform("radius");
    }

    @Override
    protected void updateUniforms() {
        GL20.glUniform1i((int) this.getUniformLocation("texture"), (int) 0);
        GL20.glUniform2f((int) this.getUniformLocation("size"), (float) (1.0f / (float) minecraft.field_71443_c), (float) (1.0f / (float) minecraft.field_71440_d));
        GL20.glUniform1f((int) this.getUniformLocation("radius"), (float) 1.5f);
    }
}
