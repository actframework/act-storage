package act.storage;

import act.app.App;
import act.handler.builtin.StaticFileGetter;
import act.route.RouteSource;
import act.util.UploadFileStorageService;
import org.osgl.http.H;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.FileSystemService;
import org.osgl.util.S;

import java.io.File;
import java.util.Map;

import static org.osgl.storage.impl.FileSystemService.CONF_HOME_DIR;

/**
 * Support set up {@link org.osgl.storage.impl.FileSystemService}
 */
public class FileSystemStoragePlugin extends StoragePlugin {

    @Override
    protected IStorageService initStorageService(String id, App app, Map<String, String> conf) {
        conf = calibrate(conf, "storage.fs.");
        FileSystemService ss = new FileSystemService(conf);
        ss.setKeyNameProvider(UploadFileStorageService.ACT_STORAGE_KEY_NAME_PROVIDER);
        String home = conf.get(CONF_HOME_DIR);
        String url = ss.getStaticWebEndpoint();
        if (null != url) {
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            if (S.notBlank(url) && !url.startsWith("http") && !url.startsWith("//")) {
                App.instance().router().addMapping(H.Method.GET, url, new StaticFileGetter(new File(home)), RouteSource.BUILD_IN);
            }
        }
        return ss;
    }

}
