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

import static org.osgl.storage.impl.FileSystemService.CONF_HOME_DIR;

import act.app.App;
import act.handler.builtin.FileGetter;
import act.route.RouteSource;
import act.util.UploadFileStorageService;
import org.osgl.http.H;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.FileSystemService;
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
        FileSystemService ss = new FileSystemService(conf);
        ss.setKeyNameProvider(UploadFileStorageService.ACT_STORAGE_KEY_NAME_PROVIDER);
        String home = conf.get(CONF_HOME_DIR);
        String url = ss.getStaticWebEndpoint();
        if (null != url) {
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            if (S.notBlank(url) && !url.startsWith("http") && !url.startsWith("//")) {
                App.instance().router().addMapping(H.Method.GET, url, new FileGetter(new File(home)), RouteSource.BUILD_IN);
            }
        }
        return ss;
    }

}
