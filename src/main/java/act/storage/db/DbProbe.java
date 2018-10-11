package act.storage.db;

/*-
 * #%L
 * ACT Storage
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.App;
import act.app.event.SysEventId;
import act.event.ActEventListenerBase;
import act.plugin.AppServicePlugin;
import act.storage.StorageServiceManager;
import act.storage.StorageServiceManagerInitialized;
import org.osgl.$;

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
            app.jobManager().on(SysEventId.CLASS_LOADER_INITIALIZED, new Runnable() {
                @Override
                public void run() {
                    Class<DbHooker> hookerClass = $.classForName(dbHookerClass(), app.classLoader());
                    final DbHooker hooker = app.getInstance(hookerClass);
                    final StorageServiceManager ssm = StorageServiceManager.instance();
                    if (null != ssm) {
                        app.jobManager().on(SysEventId.DB_SVC_LOADED, new Runnable() {
                            @Override
                            public void run() {
                                ssm.addDbHooker(hooker);
                            }
                        });
                    } else {
                        app.eventBus().bind(StorageServiceManagerInitialized.class, new ActEventListenerBase<StorageServiceManagerInitialized>(getClass().getName() + ":hook-to-ssm") {
                            @Override
                            public void on(StorageServiceManagerInitialized event) throws Exception {
                                final StorageServiceManager ssm = event.source();
                                app.jobManager().on(SysEventId.DB_SVC_LOADED, new Runnable() {
                                    @Override
                                    public void run() {
                                        ssm.addDbHooker(hooker);
                                    }
                                });
                            }
                        });
                    }
                }
            });
        }
    }
}
