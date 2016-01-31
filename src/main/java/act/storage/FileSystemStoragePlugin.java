package act.storage;

import act.app.App;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.FileSystemService;

import java.util.Map;

/**
 * Support set up {@link org.osgl.storage.impl.FileSystemService}
 */
public class FileSystemStoragePlugin extends StoragePlugin {

    @Override
    protected IStorageService initStorageService(String id, App app, Map<String, String> conf) {
        return new FileSystemService(conf);
    }

}
