package act.storage.db.impl.morphia;

import act.app.App;
import act.db.morphia.MorphiaService;
import act.storage.StorageServiceManager;
import act.storage.UpdatePolicy;
import act.storage.db.DbHooker;
import act.storage.db.util.Setter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.List;

/**
 * hook to {@link act.db.morphia.MorphiaPlugin morphia db layer}
 */
public class MorphiaDbHooker implements DbHooker {

    private Mapper mapper;
    private StorageServiceManager ssm;

    public MorphiaDbHooker() {
    }

    @Override
    public Class entityAnnotation() {
        return (Entity.class);
    }

    @Override
    public Class transientAnnotationType() {
        return (Transient.class);
    }

    @Override
    public void hookLifecycleInterceptors() {
        mapper().addInterceptor(new StorageFieldConverter(ssm()));
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || null != obj && MorphiaDbHooker.class.getName().equals(obj.getClass().getName());
    }

    private synchronized Mapper mapper() {
        if (null == mapper) {
            mapper = MorphiaService.mapper();
        }
        return mapper;
    }

    private synchronized StorageServiceManager ssm() {
        if (null == ssm) {
            ssm = App.instance().singleton(StorageServiceManager.class);
        }
        return ssm;
    }
}

class StorageFieldConverter extends AbstractEntityInterceptor implements EntityInterceptor {

    private StorageServiceManager ssm;

    StorageFieldConverter(StorageServiceManager ssm) {
        this.ssm = $.notNull(ssm);
    }

    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        Class c = ent.getClass();
        String cn = c.getName();
        List<String> storageFields = ssm.managedFields(cn);
        for (String fieldName : storageFields) {
            String key = ((BasicDBObject) dbObj).getString(fieldName);
            if (S.blank(key)) {
                continue;
            }
            IStorageService ss = ssm.storageService(cn, fieldName);
            ISObject sobj = ss.get(key);
            if (null != sobj) {
                Setter setter = ssm.setter(c, fieldName);
                setter.set(ent, sobj);
            }
        }
    }

    @Override
    public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
        Class c = ent.getClass();
        String cn = c.getName();
        List<String> storageFields = ssm.managedFields(cn);
        for (String fieldName : storageFields) {
            UpdatePolicy updatePolicy = ssm.updatePolicy(cn, fieldName);
            IStorageService ss = ssm.storageService(cn, fieldName);
            String keyCacheField = S.builder(fieldName).append("Key").toString();
            ISObject sobj = $.getProperty(ent, fieldName);
            String prevKey = $.getProperty(ent, keyCacheField);
            updatePolicy.handleUpdate(prevKey, sobj, ss);
            if (null != sobj) {
                String newKey = sobj.getKey();
                if (S.blank(newKey)) {
                    newKey = ss.getKey();
                } else if (S.eq(newKey, prevKey)) {
                    continue;
                }
                sobj = ss.put(newKey, sobj);
                Setter setter = ssm.setter(c, keyCacheField);
                setter.set(ent, newKey);
                setter = ssm.setter(c, fieldName);
                setter.set(ent, sobj);
                dbObj.put(fieldName, sobj.getKey());
            }
        }
    }





}
