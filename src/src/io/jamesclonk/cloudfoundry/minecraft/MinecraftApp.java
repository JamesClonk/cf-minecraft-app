package io.jamesclonk.cloudfoundry.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class MinecraftApp {

    private String rconHost = "127.0.0.1";
    private int rconPort = 25575;
    private String rconPassword = "c1oudc0w";
    private String levelName = "world";

    private StorageProvider storage = null;
    private Map<String, String> storageConfig = new HashMap<>(8);

    private final String[] filesToUpload = {
        "banned-ips.json",
        "banned-players.json",
        "ops.json",
        "usercache.json",
        "whitelist.json",
        "server.properties"
    };

    public static void main(String[] args) throws Exception {
        MinecraftApp app = new MinecraftApp();
        app.run();
    }

    public void run() throws Exception {
        writeEula();
        writeServerProperties();

        // get env vars and update server.properties with them..
        HashMap<String, String> data = new HashMap<>(8);
        data.put("server-port", getPort());
        data.put("enable-rcon", "true");
        data.put("rcon.port", "" + rconPort);
        data.put("rcon.password", rconPassword);
        data.put("level-name", levelName);
        updateServerProperties(data);

        // start minecraft server
        Thread serverThread = startServer();

//        //Thread.sleep(10 * 1000);
//        Thread.sleep(5000);
//        RCon rcon = new RCon(rconHost, rconPort, rconPassword.toCharArray());
//        rcon.say("Hello World!");
//        //Thread.sleep(5000);
//        rcon.stop();
        serverThread.join();

//        boolean isS3 = true;
//
//        if (isS3) {
//            storage = new S3Storage();
//            storageConfig.putAll(getProperties("s3storage.properties"));
//        }
//
//        storage.connect(storageConfig);
//        uploadAllFiles();
    }

    private void uploadAllFiles() throws Exception {
        for (String file : filesToUpload) {
            storage.uploadFile(file);
        }

        File worldDir = new File(levelName);
        Collection<File> files = FileUtils.listFiles(worldDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files) {
            storage.uploadFile(file.getPath());
            System.out.println(file.getPath()); // TODO: replace with a logger
        }
    }

    private Map<String, String> getProperties(String filename) throws FileNotFoundException, IOException {
        Properties props = new Properties();

        InputStream in = new FileInputStream(filename);
        props.load(in);

        Map<String, String> data = new HashMap<>(8);
        for (String key : props.stringPropertyNames()) {
            data.put(key, props.getProperty(key, ""));
        }
        return data;
    }

    private String readFromEnv(String key) {
        String output = System.getenv(key);
        if (output == null || output.isEmpty()) {
            output = "";
        }
        return output;
    }

    private void writeEula() throws IOException {
        File file = new File("eula.txt");
        if (!file.exists()) {
            file.createNewFile();
            try (PrintWriter pr = new PrintWriter(file)) {
                pr.print("eula=true");
                pr.flush();
            }
        }
    }

    private void writeServerProperties() throws IOException {
        File file = new File("server.properties");
        if (!file.exists()) {
            file.createNewFile();
            try (PrintWriter pr = new PrintWriter(file)) {
                pr.print("max-tick-time=60000");
                pr.print("generator-settings=");
                pr.print("force-gamemode=true");
                pr.print("allow-nether=true");
                pr.print("gamemode=0");
                pr.print("enable-query=false");
                pr.print("player-idle-timeout=0");
                pr.print("difficulty=1");
                pr.print("spawn-monsters=true");
                pr.print("op-permission-level=4");
                pr.print("resource-pack-hash=");
                pr.print("announce-player-achievements=true");
                pr.print("pvp=true");
                pr.print("snooper-enabled=false");
                pr.print("level-type=DEFAULT");
                pr.print("hardcore=false");
                pr.print("enable-command-block=true");
                pr.print("max-players=12");
                pr.print("network-compression-threshold=256");
                pr.print("max-world-size=29999984");
                pr.print("rcon.port=25575");
                pr.print("server-port=25565");
                pr.print("server-ip=");
                pr.print("spawn-npcs=true");
                pr.print("allow-flight=true");
                pr.print("level-name=world");
                pr.print("view-distance=10");
                pr.print("resource-pack=");
                pr.print("spawn-animals=true");
                pr.print("white-list=false");
                pr.print("rcon.password=c1oudc0w");
                pr.print("generate-structures=true");
                pr.print("online-mode=true");
                pr.print("max-build-height=256");
                pr.print("level-seed=");
                pr.print("use-native-transport=true");
                pr.print("motd=A Minecraft Server on CloudFoundry");
                pr.print("enable-rcon=true");
                pr.flush();
            }
        }
    }

    private void updateServerProperties(Map<String, String> values) throws FileNotFoundException, IOException {
        Properties serverProperties = new Properties();

        InputStream in = new FileInputStream("server.properties");
        serverProperties.load(in);

        for (String key : values.keySet()) {
            if (values.get(key) != null) {
                serverProperties.setProperty(key, values.get(key));
            }
        }

        OutputStream out = new FileOutputStream("server.properties");
        serverProperties.store(out, null);
    }

    private String getPort() {
        String port = readFromEnv("PORT");
        System.out.println("ENV PORT = " + port); // TODO: replace with a logger
        if (port == null || port.isEmpty()) {
            port = "25565"; // default port is 25565
        }
        return port;
    }

    private Thread startServer() {
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] arguments = {"nogui"};
                MinecraftServer.main(arguments);
            }
        });
        server.start();
        return server;
    }
}
