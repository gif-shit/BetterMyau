package myau;

import java.io.File;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import myau.ConfigProfile;
import myau.MyauClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid="myau", name="BetterMyau", version="5.4", clientSideOnly=true, acceptedMinecraftVersions="[1.8.9]")
public final class MyauForgeMod {
    public static final String MOD_ID = "myau";

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent fMLInitializationEvent) {
        MyauClient.initialize();
        File file = new File("config/Myau/predac1.json");
        if (!file.isFile()) {
            try (InputStream inputStream = MyauForgeMod.class.getResourceAsStream("/myau/predac1.json");){
                if (inputStream == null) {
                    throw new IllegalStateException("Bundled predac1 profile is missing");
                }
                Files.createDirectories(file.toPath().getParent(), new FileAttribute[0]);
                Files.copy(inputStream, file.toPath(), new CopyOption[0]);
            }
            catch (Exception exception) {
                throw new IllegalStateException("Unable to install Myau profile: " + file, exception);
            }
        }
        if (!new ConfigProfile("predac1").load()) {
            throw new IllegalStateException("Unable to load Myau profile: " + file);
        }
    }
}
