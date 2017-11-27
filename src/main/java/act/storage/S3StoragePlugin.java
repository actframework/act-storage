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
import act.util.UploadFileStorageService;
import org.osgl.storage.IStorageService;
import org.osgl.storage.impl.S3Service;

import java.util.Map;

/**
 * Support set up {@link S3Service}
 */
public class S3StoragePlugin extends StoragePlugin {

    @Override
    protected IStorageService initStorageService(String id, App app, Map<String, String> conf) {
        S3Service ss = new S3Service(calibrate(conf, "storage.s3."));
        ss.setKeyNameProvider(UploadFileStorageService.ACT_STORAGE_KEY_NAME_PROVIDER);
        return ss;
    }

}
