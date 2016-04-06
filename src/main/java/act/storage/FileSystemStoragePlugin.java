package act.storage;

import act.app.App;
import act.handler.builtin.StaticFileGetter;
import act.route.RouteSource;
import org.osgl.http.H;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.FileSystemService;
import org.osgl.util.C;
import org.osgl.util.S;

import java.io.File;
import java.util.Map;

/**
 * Support set up {@link org.osgl.storage.impl.FileSystemService}
 */
public class FileSystemStoragePlugin extends StoragePlugin {

    @Override
    protected IStorageService initStorageService(String id, App app, Map<String, String> conf) {
        conf = calibrate(conf, "storage.fs.");
        IStorageService ss = new FileSystemService(conf);
        String home = conf.get(FileSystemService.CONF_HOME_DIR);
        String url = conf.get(FileSystemService.CONF_HOME_URL);
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        if (S.notBlank(url)) {
            App.instance().router().addMapping(H.Method.GET, url, new StaticFileGetter(new File(home)), RouteSource.BUILD_IN);
        }
        return ss;
    }

}
