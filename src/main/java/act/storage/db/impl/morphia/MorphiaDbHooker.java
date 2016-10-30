package act.storage.db.impl.morphia;

import act.Act;
import act.app.App;
import act.db.DeleteEvent;
import act.db.morphia.MorphiaService;
import act.event.ActEventListenerBase;
import act.storage.StorageServiceManager;
import act.storage.UpdatePolicy;
import act.storage.db.DbHooker;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.util.S;

import java.util.EventObject;
import java.util.List;

/**
 * hook to {@link act.db.morphia.MorphiaPlugin morphia db layer}
 */
public class MorphiaDbHooker implements DbHooker {

    private static Logger logger = LogManager.get(MorphiaDbHooker.class);

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

    private static Logger logger = LogManager.get(MorphiaDbHooker.class);

    private StorageServiceManager ssm;

    private CacheService cacheService;

    StorageFieldConverter(StorageServiceManager ssm) {
        this.ssm = $.notNull(ssm);
        cacheService = Act.cache();
        Act.app().eventBus().bindAsync(DeleteEvent.class, new ActEventListenerBase<DeleteEvent>() {
            @Override
            public void on(DeleteEvent eventObject) throws Exception {
                onDelete(eventObject.getSource());
            }
        });
    }

    private void onDelete(Object entity) {
        Class c = entity.getClass();
        List<String> storageFields = ssm.managedFields(c);
        for (String fieldName : storageFields) {
            String keyCacheField = StorageServiceManager.keyCacheField(fieldName);;
            String key = $.getProperty(cacheService, entity, keyCacheField);
            if (S.blank(key)) {
                continue;
            }
            IStorageService ss = ssm.storageService(c, fieldName);
            try {
                ss.remove(key);
            } catch (Exception e) {
                logger.warn(e, "Error deleting sobject by key: %s", key);
            }
        }
    }

    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        Class c = ent.getClass();
        List<String> storageFields = ssm.managedFields(c);
        for (String fieldName : storageFields) {
            String key = ((BasicDBObject) dbObj).getString(fieldName);
            if (S.blank(key)) {
                continue;
            }
            IStorageService ss = ssm.storageService(c, fieldName);
            try {
                ISObject sobj = ss.get(key);
                $.setProperty(ent, sobj, fieldName);
                $.setProperty(ent, key, StorageServiceManager.keyCacheField(fieldName));
            } catch (Exception e) {
                logger.warn(e, "Error loading sobject by key: %s", key);
            }
        }
    }

    @Override
    public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
        Class c = ent.getClass();
        List<String> storageFields = ssm.managedFields(c);
        for (String fieldName : storageFields) {
            UpdatePolicy updatePolicy = ssm.updatePolicy(c, fieldName);
            IStorageService ss = ssm.storageService(c, fieldName);
            String keyCacheField = StorageServiceManager.keyCacheField(fieldName);
            ISObject sobj = $.getProperty(cacheService, ent, fieldName);
            String prevKey = $.getProperty(cacheService, ent, keyCacheField);
            updatePolicy.handleUpdate(prevKey, sobj, ss);
            if (null != sobj) {
                String newKey = sobj.getKey();
                if (S.blank(newKey) || !ss.isManaged(sobj)) {
                    newKey = ss.getKey();
                }
                if (S.neq(newKey, prevKey)) {
                    try {
                        sobj = ss.put(newKey, sobj);
                        $.setProperty(cacheService, ent, newKey, keyCacheField);
                        $.setProperty(cacheService, ent, sobj, fieldName);
                    } catch (Exception e) {
                        logger.warn(e, "Error loading sobject by key: %s", newKey);
                    }
                }
                dbObj.put(fieldName, sobj.getKey());
            }
        }
    }

}
