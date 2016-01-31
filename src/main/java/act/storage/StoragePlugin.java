package act.storage;

import act.ActComponent;
import act.app.App;
import act.plugin.Plugin;
import act.util.DestroyableBase;
import org.osgl.storage.IStorageService;

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
}
