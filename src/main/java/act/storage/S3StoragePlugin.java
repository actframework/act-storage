package act.storage;

import act.app.App;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.FileSystemService;
import org.osgl.storage.impl.S3Service;
import org.osgl.util.C;

import java.util.Map;

/**
 * Support set up {@link S3Service}
 */
public class S3StoragePlugin extends StoragePlugin {

    @Override
    protected IStorageService initStorageService(String id, App app, Map<String, String> conf) {
        return new S3Service(calibrate(conf, "storage.s3."));
    }

}
