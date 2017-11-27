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
import act.cli.Command;
import act.util.PropertySpec;
import org.osgl.storage.IStorageService;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

public class StorageServiceAdmin {

    public class FieldServiceInfo implements Comparable<FieldServiceInfo> {
        private String className;
        private String fieldName;
        private UpdatePolicy updatePolicy;
        private IStorageService ss;

        public FieldServiceInfo(String classField) {
            this.className = S.before(classField, ":");
            this.fieldName = S.after(classField, ":");
            this.ss = ssm.storageService(className, fieldName);
            this.updatePolicy = ssm.updatePolicy(className, fieldName);
        }

        public String getClassName() {
            return className;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getServiceId() {
            return ss.id();
        }

        public String getContextPath() {
            return ss.getContextPath();
        }

        public UpdatePolicy getUpdatePolicy() {
            return updatePolicy;
        }

        @Override
        public int compareTo(FieldServiceInfo o) {
            int n = o.className.compareTo(className);
            return 0 != n ? n : o.fieldName.compareTo(fieldName);
        }
    }

    private StorageServiceManager ssm;

    @Inject
    public StorageServiceAdmin(App app) {
        ssm = app.singleton(StorageServiceManager.class);
    }

    @Command(value = "act.ss.list, act.ss.print", help = "List all storage field info")
    @PropertySpec("className,fieldName,updatePolicy,serviceId,contextPath")
    public Collection<FieldServiceInfo> listSObjectFields() {
        Set<String> fields = ssm.storageFields();
        C.List<FieldServiceInfo> l = C.newList();
        for (String s : fields) {
            l.add(new FieldServiceInfo(s));
        }
        return l.sorted();
    }

}
