package io.jamesclonk.cloudfoundry.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class MinecraftApp {

    private String rconHost = "127.0.0.1";
    private int rconPort = 25575;
    private String rconPassword = "c1oudc0w";
    private String levelName = "world";

    private String s3Endpoint = "";
    private String s3AccessKey = "";
    private String s3SecretKey = "";
    private String s3BucketName = "";
    private S3Service s3Service = null;

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
//        // get env vars and update server.properties with them..
//        HashMap<String, String> data = new HashMap<>(8);
//        data.put("server-port", getPort());
//        data.put("enable-rcon", "true");
//        data.put("rcon.port", "" + rconPort);
//        data.put("rcon.password", rconPassword);
//        data.put("level-name", levelName);
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

        s3Endpoint = readFromS3Properties("S3_ENDPOINT");
        s3AccessKey = readFromS3Properties("S3_ACCESS_KEY");
        s3SecretKey = readFromS3Properties("S3_SECRET_KEY");
        s3BucketName = readFromS3Properties("S3_BUCKET", "cf-minecraft-app");

        final Jets3tProperties props = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
        if (!s3Endpoint.isEmpty()) {
            props.setProperty("s3service.s3-endpoint", s3Endpoint);
        }
        props.setProperty("s3service.https-only", "true");
        //props.setProperty("s3service.disable-dns-buckets", "false");
        //props.setProperty("s3service.s3-endpoint-https-port", "443");

        AWSCredentials awsCredentials = new AWSCredentials(s3AccessKey, s3SecretKey);
        s3Service = new RestS3Service(awsCredentials);

        S3Bucket bucket;
        if (bucketExists()) {
            bucket = s3Service.getBucket(s3BucketName);
        } else {
            bucket = s3Service.createBucket(s3BucketName);
            System.out.println("Created S3 bucket: " + bucket.getName());
        }

        uploadAllFiles(bucket);
    }

    private void uploadAllFiles(S3Bucket bucket) throws NoSuchAlgorithmException, IOException, S3ServiceException {
        for (String file : filesToUpload) {
            uploadFile(bucket, file);
        }

        File worldDir = new File(levelName);
        Collection<File> files = FileUtils.listFiles(worldDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files) {
            uploadFile(bucket, file.getPath());
            System.out.println(file.getPath());
        }
    }

    private void uploadFile(S3Bucket bucket, String filename) throws NoSuchAlgorithmException, IOException, S3ServiceException {
        File fileData = new File(filename);
        S3Object fileObject = new S3Object(fileData);
        fileObject.setKey(filename);

        fileObject = s3Service.putObject(bucket, fileObject);
        System.out.println("File uploaded to [" + bucket.getName() + "]: " + fileObject.getName());
    }

    private void deleteBucket() throws ServiceException {
        for (S3Object obj : s3Service.listObjects(s3BucketName)) {
            s3Service.deleteObject(s3BucketName, obj.getKey());
        }
        s3Service.deleteBucket(s3BucketName);
    }

    private boolean bucketExists() throws S3ServiceException {
        S3Bucket[] buckets = s3Service.listAllBuckets();
        for (S3Bucket bucket : buckets) {
            if (bucket.getName().equals(s3BucketName)) {
                return true;
            }
        }
        return false;
    }

    private String readFromS3Properties(String key) throws FileNotFoundException, IOException {
        return readFromS3Properties(key, "");
    }

    private String readFromS3Properties(String key, String defaultValue) throws FileNotFoundException, IOException {
        Properties s3 = new Properties();

        InputStream in = new FileInputStream("s3storage.properties");
        s3.load(in);

        return s3.getProperty(key, defaultValue);
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
