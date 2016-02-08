package act.storage;

import act.ActComponent;
import act.app.App;
import act.plugin.Plugin;
import act.util.DestroyableBase;
import org.osgl.storage.IStorageService;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Map;

/**
 * The base class for Storage Plugin
 */
@ActComponent
public abstract class StoragePlugin extends DestroyableBase implements Plugin {

    @Override
    public void register() {
        StoragePluginManager.deplayedRegister(this);
    }

    protected abstract IStorageService initStorageService(String id, App app, Map<String, String> conf);

    protected static Map<String, String> calibrate(Map<String, String> conf, String prefix) {
        Map<String, String> map = C.newMap();
        for (String key : conf.keySet()) {
            String val = conf.get(key);
            if (!key.startsWith(prefix) && S.neq("storage.id", key)) {
                key = prefix + key;
            }
            map.put(key, val);
        }
        return map;
    }

}
