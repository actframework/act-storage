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

import act.app.App;
import act.plugin.Plugin;
import act.util.DestroyableBase;
import org.osgl.bootstrap.Version;
import org.osgl.storage.IStorageService;
import org.osgl.util.C;

import java.util.Map;

/**
 * The base class for Storage Plugin
 */
public abstract class StoragePlugin extends DestroyableBase implements Plugin {

    public static final Version VERSION = Version.of(StoragePlugin.class);

    @Override
    public void register() {
        StoragePluginManager.deplayedRegister(this);
    }

    protected abstract IStorageService initStorageService(String id, App app, Map<String, String> conf);

    protected static Map<String, String> calibrate(Map<String, String> conf, String prefix) {
        Map<String, String> map = C.newMap();
        for (String key : conf.keySet()) {
            String val = conf.get(key);
            if (!key.startsWith("storage")) {
                key = prefix + key;
            }
            map.put(key, val);
        }
        return map;
    }

}
