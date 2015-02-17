package io.jamesclonk.cloudfoundry.minecraft;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import net.minecraft.server.MinecraftServer;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;

public class MinecraftApp {

    private String rconHost = "127.0.0.1";
    private int rconPort = 25575;
    private String rconPassword = "c1oudc0w";

    private String s3Endpoint = "";
    private String s3AccessKey = "";
    private String s3SecretKey = "";

    public static void main(String[] args) throws Exception {
        MinecraftApp app = new MinecraftApp();
        app.run();
    }

    public void run() throws Exception {
//        // get env vars and update server.properties with them..
//        HashMap<String, String> data = new HashMap<>(8);
//        data.put("server-port", getPort());
//        data.put("enable-rcon", "true");
//        data.put("rcon.port", "" + rconPort);
//        data.put("rcon.password", rconPassword);
//        updateServerProperties(data);
//
//        // start minecraft server
//        Thread serverThread = startServer();
//
//        //Thread.sleep(10 * 1000);
//        Thread.sleep(5000);
//        RCon rcon = new RCon(rconHost, rconPort, rconPassword.toCharArray());
//        rcon.say("Hello World!");
//        //Thread.sleep(5000);
//        rcon.stop();

        s3Endpoint = readFromEnv("S3_ENDPOINT");
        s3AccessKey = readFromEnv("S3_ACCESS_KEY");
        s3SecretKey = readFromEnv("S3_SECRET_KEY");

        final Jets3tProperties props = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
        if (!s3Endpoint.isEmpty()) {
            props.setProperty("s3service.s3-endpoint", s3Endpoint);
        }
        props.setProperty("s3service.https-only", "true");
        //props.setProperty("s3service.disable-dns-buckets", "false");
        //props.setProperty("s3service.s3-endpoint-https-port", "443");

        AWSCredentials awsCredentials = new AWSCredentials(s3AccessKey, s3SecretKey);
        S3Service s3Service = new RestS3Service(awsCredentials);

        s3Service.createBucket("minecraft/server");

        S3Bucket[] myBuckets = s3Service.listAllBuckets();
        System.out.println("How many buckets do I have in S3? " + myBuckets.length);
    }

    private String readFromEnv(String key) {
        Properties props = System.getProperties();
        String output = "";
        if (props.containsKey(key)) {
            output = props.getProperty(key);
            if (output == null || output.isEmpty()) {
                output = "";
            }
        }
        return output;
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
        Properties props = System.getProperties();
        String port = props.getProperty("PORT");
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
