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

import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.util.S;

/**
 * Defines how to deal with updating to {@link org.osgl.storage.ISObject} typed field
 */
public enum UpdatePolicy {
    /**
     * If a field is specified as {@code REJECT} and the framework
     * detect the sobject has been updated, the framework will throw
     * out an exception to reject the update when persist the entity
     */
    REJECT () {
        @Override
        public void handleUpdate(String prevKey, String newKey, IStorageService storageService) {
            if (null == newKey) {
                if (S.blank(prevKey)) {
                    return;
                }
                throw new IllegalStateException("sobject is read only");
            } else if (S.neq(prevKey, newKey)) {
                throw new IllegalStateException("sobject is read only");
            }
        }
    },

    /**
     * If a field is specified as {@code DELETE_OLD_DATA} and framework
     * detect the sobject field has been updated, the framework will
     * delete old sobject and then save the updated one when persist
     * the entity
     */
    DELETE_OLD_DATA () {
        @Override
        public void handleUpdate(String prevKey, String newKey, IStorageService storageService) {
            if (S.blank(prevKey)) {
                return;
            }
            if (null == newKey || S.neq(prevKey, newKey)) {
                storageService.remove(prevKey);
            }
        }
    },

    /**
     * If a field is specified as {@code KEEP_OLD_DATA} and framework
     * detect the sobject field has been updated, the framework will
     * NOT delete old sobject. However the updated one will be saved as well
     */
    KEEP_OLD_DATA;

    public void handleUpdate(String prevKey, String newKey, IStorageService storageService) {}
}
