package io.jamesclonk.cloudfoundry.minecraft;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class S3Storage implements StorageProvider {

    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    private String bucketName = "";

    private S3Service service = null;

    @Override
    public void connect(Map<String, String> config) throws Exception {
        readConfig(config);

        final Jets3tProperties props = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
        if (!endpoint.isEmpty()) {
            props.setProperty("s3service.s3-endpoint", endpoint);
        }
        props.setProperty("s3service.https-only", "true");
        //props.setProperty("s3service.disable-dns-buckets", "false");
        //props.setProperty("s3service.s3-endpoint-https-port", "443");

        AWSCredentials awsCredentials = new AWSCredentials(accessKey, secretKey);
        service = new RestS3Service(awsCredentials);

        S3Bucket bucket;
        if (bucketExists()) {
            bucket = service.getBucket(bucketName);
        } else {
            bucket = service.createBucket(bucketName);
            System.out.println("Created S3 bucket: " + bucket.getName()); // TODO: replace with a logger
        }
    }

    private void readConfig(Map<String, String> config) {
        if (config.containsKey("S3_ENDPOINT")) {
            endpoint = config.get("S3_ENDPOINT");
        }
        if (config.containsKey("S3_ACCESS_KEY")) {
            accessKey = config.get("S3_ACCESS_KEY");
        }
        if (config.containsKey("S3_SECRET_KEY")) {
            secretKey = config.get("S3_SECRET_KEY");
        }
        if (config.containsKey("S3_BUCKET_NAME")) {
            bucketName = config.get("S3_BUCKET_NAME");
        }
    }

    private void deleteBucket() throws ServiceException {
        for (S3Object obj : service.listObjects(bucketName)) {
            service.deleteObject(bucketName, obj.getKey());
        }
        service.deleteBucket(bucketName);
    }

    private boolean bucketExists() throws S3ServiceException {
        S3Bucket[] buckets = service.listAllBuckets();
        for (S3Bucket bucket : buckets) {
            if (bucket.getName().equals(bucketName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> listFiles() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void uploadFile(String filename) throws Exception {
        File fileData = new File(filename);
        S3Object fileObject = new S3Object(fileData);
        fileObject.setKey(filename);

        fileObject = service.putObject(bucketName, fileObject);
        System.out.println("File uploaded to [" + bucketName + "]: " + fileObject.getName()); // TODO: replace with a logger
    }

    @Override
    public void downloadFile(String filename, String targetPath) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
