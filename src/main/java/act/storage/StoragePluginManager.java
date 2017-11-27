package act.storage;

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

import act.Act;
import act.db.DbPlugin;
import act.plugin.Plugin;
import act.util.DestroyableBase;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Set;

/**
 * Manage/Configure {@link StoragePlugin}
 */
public class StoragePluginManager extends DestroyableBase implements Plugin {

    private Map<String, StoragePlugin> plugins = C.newMap();

    @Override
    protected void releaseResources() {
        super.releaseResources();
        DestroyableBase.Util.tryDestroyAll(plugins.values(), ApplicationScoped.class);
        plugins.clear();
    }

    @Override
    public void register() {
        Act.registerPlugin(this);
        for (StoragePlugin storagePlugin : pendingStoragePlugins) {
            register(storagePlugin);
        }
        pendingStoragePlugins.clear();
    }

    public synchronized void register(StoragePlugin storagePlugin) {
        plugins.put(storagePlugin.getClass().getCanonicalName().intern(), storagePlugin);
    }

    public synchronized StoragePlugin plugin(String type) {
        return plugins.get(type);
    }

    public boolean hasPlugin() {
        return !plugins.isEmpty();
    }

    /**
     * Returns the plugin if there is only One plugin inside
     * the register, otherwise return {@code null}
     */
    public synchronized StoragePlugin theSolePlugin() {
        if (plugins.size() == 1) {
            return plugins.values().iterator().next();
        } else {
            return null;
        }
    }

    private static Set<StoragePlugin> pendingStoragePlugins = C.newSet();

    public static void deplayedRegister(StoragePlugin storagePlugin) {
        StoragePluginManager manager = instance();
        if (null == manager) {
            pendingStoragePlugins.add(storagePlugin);
        } else {
            manager.register(storagePlugin);
        }
    }

    public static StoragePluginManager instance() {
        return Act.registeredPlugin(StoragePluginManager.class);
    }
}
