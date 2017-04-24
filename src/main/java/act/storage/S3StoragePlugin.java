package act.storage;

import act.app.App;
import act.util.UploadFileStorageService;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.S3Service;

import java.util.Map;

/**
 * Support set up {@link S3Service}
 */
public class S3StoragePlugin extends StoragePlugin {

    @Override
    protected IStorageService initStorageService(String id, App app, Map<String, String> conf) {
        S3Service ss = new S3Service(calibrate(conf, "storage.s3."));
        ss.setKeyNameProvider(UploadFileStorageService.ACT_STORAGE_KEY_NAME_PROVIDER);
        return ss;
    }

}
