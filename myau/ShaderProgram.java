package myau;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

public abstract class ShaderProgram {

    private static final String VERTEX_SHADER_SOURCE = "#version 120\nvoid main(void) {\ngl_TexCoord[0] = gl_MultiTexCoord0;\ngl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n}\n";
    private final Map<String, Integer> uniformLocations = new HashMap<String, Integer>();
    private int programId;

    protected ShaderProgram(String fragmentShaderSource) {
        this.createProgram(fragmentShaderSource);
    }

    private int compileShader(String source, int shaderType) {
        int shaderId = GL20.glCreateShader((int) shaderType);
        GL20.glShaderSource((int) shaderId, (CharSequence) source);
        GL20.glCompileShader((int) shaderId);
        return GL20.glGetShaderi((int) shaderId, (int) 35713) == 0 ? -1 : shaderId;
    }

    private void createProgram(String fragmentShaderSource) {
        this.programId = GL20.glCreateProgram();
        GL20.glAttachShader((int) this.programId, (int) this.compileShader(VERTEX_SHADER_SOURCE, 35633));
        GL20.glAttachShader((int) this.programId, (int) this.compileShader(fragmentShaderSource, 35632));
        GL20.glLinkProgram((int) this.programId);
        if (GL20.glGetProgrami((int) this.programId, (int) 35714) == 0) {
            this.programId = -1;
            return;
        }
        this.registerUniforms();
    }

    protected final int getUniformLocation(String name) {
        return this.uniformLocations.get(name);
    }

    protected final void registerUniform(String name) {
        this.uniformLocations.put(name, GL20.glGetUniformLocation((int) this.programId, (CharSequence) name));
    }

    protected abstract void registerUniforms();
    protected abstract void updateUniforms();

    public final void use() {
        GL20.glUseProgram((int) this.programId);
        this.updateUniforms();
    }

    public final void stop() {
        GL20.glUseProgram((int) 0);
    }
}
