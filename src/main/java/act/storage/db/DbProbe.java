package act.storage.db;

import act.app.App;
import act.app.event.AppEventId;
import act.event.ActEventListenerBase;
import act.plugin.AppServicePlugin;
import act.storage.StorageServiceManager;
import act.storage.StorageServiceManagerInitialized;
import org.osgl.$;

import java.util.EventObject;

/**
 * The implementation of this interface probe
 * if a specific {@link act.db.DbPlugin database layer}
 * is presented in the current application class loader
 */
public abstract class DbProbe extends AppServicePlugin {
    /**
     * Check if the database layer exists
     * @return {@code true if the database layer exists}
     */
    public abstract boolean exists();

    /**
     * Returns the class name of {@link act.storage.db.DbHooker}
     * implementation
     * @return the class name
     */
    public abstract String dbHookerClass();

    @Override
    protected void applyTo(final App app) {
        if (exists()) {

            app.jobManager().on(AppEventId.CLASS_LOADER_INITIALIZED, new Runnable() {
                @Override
                public void run() {
                    Class<DbHooker> hookerClass = $.classForName(dbHookerClass(), app.classLoader());
                    final DbHooker hooker = app.getInstance(hookerClass);
                    StorageServiceManager ssm = StorageServiceManager.instance();
                    if (null != ssm) {
                        ssm.addDbHooker(hooker);
                    } else {
                        app.eventBus().bind(StorageServiceManagerInitialized.class, new ActEventListenerBase<StorageServiceManagerInitialized>(getClass().getName() + ":hook-to-ssm") {
                            @Override
                            public void on(StorageServiceManagerInitialized event) throws Exception {
                                StorageServiceManager ssm = event.source();
                                ssm.addDbHooker(hooker);
                            }
                        });
                    }
                }
            });
        }
    }
}
