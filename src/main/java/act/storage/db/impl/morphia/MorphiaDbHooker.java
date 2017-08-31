package act.storage.db.impl.morphia;

import act.Act;
import act.app.App;
import act.app.DbServiceManager;
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
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.util.*;

/**
 * hook to {@link act.db.morphia.MorphiaPlugin morphia db layer}
 */
public class MorphiaDbHooker implements DbHooker {

    private static Logger logger = LogManager.get(MorphiaDbHooker.class);

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
        DbServiceManager dbServiceManager = Act.app().dbServiceManager();
        List<MorphiaService> morphiaServices = dbServiceManager.dbServicesByClass(MorphiaService.class);
        StorageFieldConverter storageFieldConverter = new StorageFieldConverter(ssm());
        for (MorphiaService morphiaService : morphiaServices) {
            morphiaService.mapper().addInterceptor(storageFieldConverter);
        }
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || null != obj && MorphiaDbHooker.class.getName().equals(obj.getClass().getName());
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

    private Map<$.T2<Class, String>, Class> fieldCache = new HashMap<>();

    StorageFieldConverter(StorageServiceManager ssm) {
        this.ssm = $.notNull(ssm);
        this.cacheService = Act.app().cache("storage-morphia");
        Act.app().eventBus().bindAsync(DeleteEvent.class, new ActEventListenerBase<DeleteEvent>() {
            @Override
            public void on(DeleteEvent eventObject) throws Exception {
                onDelete(eventObject.getSource());
            }
        });
    }

    private void onDelete(Object entity) {
        Class c = entity.getClass();
        String cn = c.getName();
        List<String> storageFields = ssm.managedFields(c);
        for (String fieldName : storageFields) {
            boolean isCollection = ssm.isCollection(cn, fieldName);
            String keyCacheField = StorageServiceManager.keyCacheField(fieldName);
            if (!isCollection) {
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
            } else {
                Collection<String> keys = $.getProperty(cacheService, entity, keyCacheField);
                if (null != keys) {
                    for (String key : keys) {
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
            }
        }
    }

    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        Class c = ent.getClass();
        String cn = c.getName();
        List<String> storageFields = ssm.managedFields(c);
        for (String fieldName : storageFields) {
            boolean isCollection = ssm.isCollection(cn, fieldName);
            if (!isCollection) {
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
            } else {
                IStorageService ss = ssm.storageService(c, fieldName);
                Class fieldType = fieldType(c, fieldName);
                Collection<ISObject> sobjs = $.cast(Act.app().getInstance(fieldType));
                Collection<String> keys = (Collection<String>) ((BasicDBObject) dbObj).get(fieldName);
                if (null == keys) {
                    keys = C.list();
                }
                for (String key : keys) {
                    try {
                        ISObject sobj = ss.get(key);
                        sobjs.add(sobj);
                    } catch (Exception e) {
                        logger.warn(e, "Error loading sobject by key: %s", key);
                    }
                }
                $.setProperty(ent, sobjs, fieldName);
                $.setProperty(ent, keys, StorageServiceManager.keyCacheField(fieldName));
            }
        }
    }

    @Override
    public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
        Class c = ent.getClass();
        String cn = c.getName();
        List<String> storageFields = ssm.managedFields(c);
        for (String fieldName : storageFields) {
            UpdatePolicy updatePolicy = ssm.updatePolicy(c, fieldName);
            IStorageService ss = ssm.storageService(c, fieldName);
            String keyCacheField = StorageServiceManager.keyCacheField(fieldName);
            boolean isCollection = ssm.isCollection(cn, fieldName);
            if (!isCollection) {
                ISObject sobj = $.getProperty(cacheService, ent, fieldName);
                String newKey = null == sobj ? null : sobj.getKey();
                String prevKey = $.getProperty(cacheService, ent, keyCacheField);
                updatePolicy.handleUpdate(prevKey, newKey, ss);
                if (null != sobj) {
                    if (S.blank(newKey) || !ss.isManaged(sobj)) {
                        newKey = ss.getKey();
                    }
                    if (S.neq(newKey, prevKey)) {
                        try {
                            sobj = ss.put(newKey, sobj);
                            $.setProperty(cacheService, ent, newKey, keyCacheField);
                            $.setProperty(cacheService, ent, sobj, fieldName);
                        } catch (Exception e) {
                            logger.warn(e, "Error persist sobject by key: %s", newKey);
                        }
                    }
                    dbObj.put(fieldName, sobj.getKey());
                }
            } else {
                // The field is a collection of ISObject.
                // 1. handle obsolete sobject items
                Collection<ISObject> col = $.getProperty(cacheService, ent, fieldName);
                if (null == col) {
                    col = C.newList();
                }
                Set<String> newKeys = C.newSet();
                for (ISObject sobj : col) {
                    if (null == sobj) {
                        continue;
                    }
                    newKeys.add(sobj.getKey());
                }
                Set<String> oldKeys = $.getProperty(cacheService, ent, keyCacheField);
                if (null == oldKeys) {
                    oldKeys = C.newSet();
                }
                Set<String> oldKeysCopy = C.newSet(oldKeys);
                oldKeysCopy.removeAll(newKeys);
                for (String toBeRemoved : oldKeysCopy) {
                    if (S.isBlank(toBeRemoved)) {
                        continue;
                    }
                    updatePolicy.handleUpdate(toBeRemoved, null, ss);
                }
                // 2. persist all new sobject items
                Class fieldType = fieldType(c, fieldName);
                Collection<ISObject> updatedCol = $.cast(Act.app().getInstance(fieldType));
                newKeys.clear();
                for (ISObject sobj : col) {
                    if (null == sobj) {
                        continue;
                    }
                    String newKey = sobj.getKey();
                    if (S.blank(newKey) || !ss.isManaged(sobj)) {
                        newKey = ss.getKey();
                    }
                    if (!oldKeys.contains(newKey)) {
                        try {
                            sobj = ss.put(newKey, sobj);
                            updatedCol.add(sobj);
                            newKeys.add(sobj.getKey());
                        } catch (Exception e) {
                            logger.warn(e, "Error persist sobject by key: %s", newKey);
                        }
                    }
                }
                $.setProperty(cacheService, ent, newKeys, keyCacheField);
                $.setProperty(cacheService, ent, updatedCol, fieldName);
                dbObj.put(fieldName, newKeys);
            }
        }
    }

    private Class<?> fieldType(Class hostClass, String fieldName) {
        $.T2<Class, String> fieldCacheKey = $.T2(hostClass, fieldName);
        Class fieldType = fieldCache.get(fieldCacheKey);
        if (null == fieldType) {
            Field field = $.fieldOf(hostClass, fieldName, false);
            fieldType = field.getType();
            fieldCache.put(fieldCacheKey, fieldType);
        }
        return fieldType;
    }
}
