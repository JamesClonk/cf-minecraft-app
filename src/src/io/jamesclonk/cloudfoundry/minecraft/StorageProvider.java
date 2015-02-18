package io.jamesclonk.cloudfoundry.minecraft;

import java.util.List;
import java.util.Map;

public interface StorageProvider {

    public void connect(Map<String, String> config) throws Exception;

    public List<String> listFiles() throws Exception;

    public void uploadFile(String filename) throws Exception;

    public void downloadFile(String filename, String targetPath) throws Exception;
}
