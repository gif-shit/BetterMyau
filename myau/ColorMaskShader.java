package myau;

import java.awt.Color;
import org.lwjgl.opengl.GL20;

public class ColorMaskShader extends ShaderProgram {

    private static final String FRAGMENT_SHADER_SOURCE = "#version 120\nuniform sampler2D texture;\nuniform vec4 color;\nvoid main() {\nvec4 st = texture2D(texture, gl_TexCoord[0].st);\ngl_FragColor = vec4(color.rgb, st.a > 0.0 ? color.a : 0.0);\n}\n";

    public ColorMaskShader() {
        super(FRAGMENT_SHADER_SOURCE);
    }

    @Override
    protected void registerUniforms() {
        this.registerUniform("texture");
        this.registerUniform("color");
    }

    @Override
    protected void updateUniforms() {
        GL20.glUniform1i((int) this.getUniformLocation("texture"), (int) 0);
        GL20.glUniform4f((int) this.getUniformLocation("color"), (float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
    }

    public void setColor(Color color) {
        GL20.glUniform4f((int) this.getUniformLocation("color"), (float) ((float) color.getRed() / 255.0f), (float) ((float) color.getGreen() / 255.0f), (float) ((float) color.getBlue() / 255.0f), (float) ((float) color.getAlpha() / 255.0f));
    }
}
