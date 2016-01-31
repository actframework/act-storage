package act.storage;

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
            this.ss = ssm.storageService(classField, fieldName);
            this.updatePolicy = ssm.updatePolicy(classField, fieldName);
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

    @Command(value = "act.ss.list", help = "List all storage field info")
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
