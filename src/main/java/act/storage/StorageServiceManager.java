package act.storage;

import act.Destroyable;
import act.app.App;
import act.app.AppService;
import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.inject.DependencyInjectionBinder;
import act.plugin.AppServicePlugin;
import act.storage.db.DbHooker;
import act.storage.db.util.Setter;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manage {@link org.osgl.storage.IStorageService} instances
 */
@Singleton
public class StorageServiceManager extends AppServicePlugin implements AppService<StorageServiceManager>, Destroyable {

    private static Logger logger = LogManager.get(StorageServiceManager.class);

    /**
     * The key to fetch default {@link org.osgl.storage.IStorageService storage service}
     */
    public static final String DEFAULT = IStorageService.DEFAULT;

    /**
     * map service id to storage service instance
     */
    private Map<String, IStorageService> serviceById = C.newMap();

    /**
     * map class name plus field name to storage service instance
     */
    private Map<String, IStorageService> serviceByClassField = C.newMap();

    private Map<$.T2<Class, String>, $.Val<IStorageService>> serviceByClass = C.newMap();

    /**
     * map class name plus field name to {@link UpdatePolicy}
     */
    private Map<String, UpdatePolicy> updatePolicyByClassField = C.newMap();

    private Map<$.T2<Class, String>, $.Val<UpdatePolicy>> updatePolicyByClass = C.newMap();

    /**
     * Map a list of managed sobject fields to class name
     */
    private Map<String, List<String>> managedFieldByClass = C.newMap();

    /**
     * Map a list of managed sobject fields to class name including super class names
     */
    private Map<String, List<String>> managedFieldByClass2 = C.newMap();

    /**
     * Map {@link Setter} instance to (Class, FieldName) pair
     */
    private Map<$.T2<Class, String>, Setter> setterByClass = C.newMap();

    private List<DbHooker> dbHookers = C.newList();

    private App app;

    private boolean isDestroyed;

    public StorageServiceManager() {
    }

    @Override
    protected void applyTo(App app) {
        this.app = app;
        initServices(app.config());
        app.registerSingleton(this);
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_LOADED, new Runnable() {
            @Override
            public void run() {
                app().eventBus().emit(new DependencyInjectionBinder<StorageServiceManager>(this, StorageServiceManager.class) {
                    @Override
                    public StorageServiceManager resolve(App app) {
                        return StorageServiceManager.this;
                    }
                });
            }
        });
        app.eventBus().emit(new StorageServiceManagerInitialized(this));
    }

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    private static String ssKey(String className, String fieldName) {
        StringBuilder sb = S.builder();
        sb.append($.notNull(className));
        if (S.notBlank(fieldName)) {
            sb.append(":");
            sb.append(fieldName);
        }
        return sb.toString();
    }

    public void registerServiceIndex(String className, String fieldName, String serviceId, String contextPath, UpdatePolicy updatePolicy) {
        if (S.blank(serviceId)) {
            serviceId = DEFAULT;
        }
        IStorageService ss = serviceById.get(serviceId);
        if (null == ss) {
            if (DEFAULT.equals(serviceId)) {
                logger.warn("default storage service not configured. Use the upload storage service");
                ss = app().uploadFileStorageService();
            } else {
                throw E.invalidConfiguration("Cannot find storage engine by id: %s", serviceId);
            }
        }
        assert null != ss;
        if (S.notBlank(contextPath)) {
            ss = ss.subFolder(contextPath);
        }
        String key = ssKey(className, fieldName);
        serviceByClassField.put(key, ss);

        if (null != updatePolicy) {
            updatePolicyByClassField.put(key, updatePolicy);
        }
    }

    public Set<String> storageFields() {
        return serviceByClassField.keySet();
    }

    public IStorageService storageService(String className, String fieldName) {
        String key = ssKey(className, fieldName);
        IStorageService svc = serviceByClassField.get(key);
        if (null == svc) {
            svc = serviceByClassField.get(className);
        }
        if (null == svc) {
            svc = app().uploadFileStorageService();
        }
        return svc;
    }

    public IStorageService storageService(Class c, String fieldName) {
        $.T2<Class, String> key = $.T2(c, fieldName);
        $.Val<IStorageService> val = serviceByClass.get(key);
        if (null == val) {
            while (Object.class != c) {
                IStorageService ss = serviceByClassField.get(ssKey(c.getName(), fieldName));
                if (null == ss) {
                    ss = serviceByClassField.get(c.getName());
                }
                if (null == ss) {
                    c = c.getSuperclass();
                    continue;
                }
                val = $.val(ss);
                serviceByClass.put(key, val);
                return ss;
            }
            val = $.val((IStorageService) null);
            serviceByClass.put(key, val);
        }
        return val.get();
    }

    public IStorageService storageService(String serviceId) {
        return serviceById.get(serviceId);
    }

    public UpdatePolicy updatePolicy(String className, String fieldName) {
        return updatePolicyByClassField.get(ssKey(className, fieldName));
    }

    public UpdatePolicy updatePolicy(Class c, String fieldName) {
        $.T2<Class, String> key = $.T2(c, fieldName);
        $.Val<UpdatePolicy> v = updatePolicyByClass.get(key);
        if (null == v) {
            while (Object.class != c) {
                UpdatePolicy p = updatePolicy(c.getName(), fieldName);
                if (null != p) {
                    updatePolicyByClass.put(key, $.val(p));
                    return p;
                }
                c = c.getSuperclass();
            }
            v = $.val((UpdatePolicy) null);
            updatePolicyByClass.put(key, v);
        }
        return v.get();
    }

    @Override
    public StorageServiceManager app(App app) {
        this.app = app;
        return this;
    }

    @Override
    public App app() {
        return app;
    }

    @Override
    public boolean isDestroyed() {
        return isDestroyed;
    }

    @Override
    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        isDestroyed = true;
        Destroyable.Util.tryDestroyAll(serviceById.values(), ApplicationScoped.class);
        Destroyable.Util.tryDestroyAll(dbHookers, ApplicationScoped.class);
        serviceById.clear();
        serviceByClassField.clear();
        updatePolicyByClassField.clear();
        managedFieldByClass.clear();
        setterByClass.clear();
        dbHookers.clear();
    }

    public List<String> managedFields(String className) {
        List<String> fields = managedFieldByClass.get(className);
        if (null == fields) {
            fields = C.newList();
            Set<String> ss = storageFields();
            for (String s: ss) {
                if (s.startsWith(className + ":")) {
                    String fn = S.after(s, ":");
                    if (S.notEmpty(fn)) {
                        fields.add(fn);
                    }
                }
            }
            managedFieldByClass.put(className, fields);
        }
        return fields;
    }

    public List<String> managedFields(Class<?> c) {
        String cn = c.getName();
        List<String> fields = managedFieldByClass2.get(cn);
        if (null == fields) {
            fields = C.newList();
            while (c != Object.class) {
                fields.addAll(managedFields(c.getName()));
                c = c.getSuperclass();
            }
            managedFieldByClass2.put(cn, fields);
        }
        return fields;
    }

    public void addDbHooker(DbHooker dbHooker) {
        if (!this.dbHookers.contains(dbHooker)) {
            this.dbHookers.add(dbHooker);
            dbHooker.hookLifecycleInterceptors();
            logger.debug("DbHooker[%s] hooked", dbHooker.getClass());
        }
    }

    public List<DbHooker> dbHookers() {
        return C.list(dbHookers);
    }

    public void delete(ISObject sobj) {
        String id = sobj.getAttribute(ISObject.ATTR_SS_ID);
        IStorageService ss = storageService(id);
        String contextPath = sobj.getAttribute(ISObject.ATTR_SS_CTX);
        if (S.notBlank(contextPath)) {
            ss = ss.subFolder(contextPath);
        }
        ss.remove(sobj.getKey());
    }

    private void initServices(AppConfig config) {
        StoragePluginManager pluginManager = StoragePluginManager.instance();
        if (!pluginManager.hasPlugin()) {
            logger.warn("Storage service not initialized: No storage plugin found");
            return;
        }
        StoragePlugin storagePlugin = pluginManager.theSolePlugin();
        Map<String, String> storageConfig = config.subSet("ss.");
        if (storageConfig.isEmpty()) {
            if (null == storagePlugin) {
                logger.warn("Storage service not initialized: need to specify default storage service implementation");
                return;
            } else {
                logger.warn("Storage configuration not found. Will try to init default service with the sole storage plugin: %s", storagePlugin);
                IStorageService svc = storagePlugin.initStorageService(DEFAULT, app(), C.<String, String>newMap());
                serviceById.put(DEFAULT, svc);
                return;
            }
        }

        String instances = null;
        if (storageConfig.containsKey("ss.instances")) {
            instances = storageConfig.get("ss.instances").toString();
            for (String dbId: instances.split(S.COMMON_SEP)) {
                initService(dbId, storageConfig);
            }
        }
        if (serviceById.containsKey(DEFAULT)) return;
        // try init default service if conf found
        String ssId = null;
        if (storageConfig.containsKey("ss." + DEFAULT +".impl")) {
            ssId = DEFAULT;
        } else if (storageConfig.containsKey("ss.impl")) {
            ssId = "";
        }
        if (null != ssId) {
            initService(ssId, storageConfig);
        } else if (serviceById.size() == 1) {
            IStorageService svc = serviceById.values().iterator().next();
            serviceById.put(DEFAULT, svc);
            logger.warn("Storage service configuration not found. Use the sole one storage service[%s] as default service", svc.id());
        } else {
            if (serviceById.isEmpty()) {
                if (null == storagePlugin) {
                    logger.warn("Storage service not intialized: need to specify default storage service implementation");
                } else {
                    logger.warn("Storage configuration not found. Will try to init default service with the sole storage plugin: %s", storagePlugin);
                    Map<String, String> svcConf = C.newMap();
                    String prefix = "ss.";
                    for (String key : storageConfig.keySet()) {
                        if (key.startsWith(prefix)) {
                            String o = storageConfig.get(key);
                            svcConf.put(key.substring(prefix.length()), o);
                        }
                    }
                    IStorageService svc = storagePlugin.initStorageService(DEFAULT, app(), svcConf);
                    serviceById.put(DEFAULT, svc);
                }
            } else {
                if (null != instances) {
                    // use the first instance as the default one
                    IStorageService svc = serviceById.get(instances.split(S.COMMON_SEP)[0]);
                    serviceById.put(DEFAULT, $.notNull(svc));
                } else {
                    throw E.invalidConfiguration("Default db service for the application needs to be specified");
                }
            }
        }
    }

    private void initService(String ssId, Map<String, String> conf) {
        Map<String, String> svcConf = C.newMap();
        String prefix = "ss." + (S.empty(ssId) ? "" : ssId + ".");
        for (String key : conf.keySet()) {
            if (key.startsWith(prefix)) {
                String o = conf.get(key);
                svcConf.put(key.substring(prefix.length()), o);
            }
        }
        String impl = svcConf.remove("impl");
        String svcId = S.empty(ssId) ? DEFAULT : ssId;
        if (null == impl) {
            throw new ConfigurationException("Cannot init storage service[%s]: implementation not specified", svcId);
        }
        StoragePlugin plugin = StoragePluginManager.instance().plugin(impl);
        if (null == plugin) {
            throw new ConfigurationException("Cannot init storage service[%s]: implementation not found", svcId);
        }
        svcConf.put(IStorageService.CONF_ID, "".equals(ssId) ? DEFAULT : ssId);
        IStorageService svc = plugin.initStorageService(ssId, app(), svcConf);
        serviceById.put(svcId, svc);
        logger.info("storage service[%s] initialized", svcId);
    }

    /**
     * Returns {@code StorageServiceManager} instance if the manager has been initialized
     * or {@code null} if not
     * @return the instance or {@code null}
     */
    public static StorageServiceManager instance() {
        return App.instance().singleton(StorageServiceManager.class);
    }

    public static String keyCacheField(String fieldName) {
        return S.builder(fieldName).append("Key").toString();
    }

    public void copyManagedFields(Object from, Object to) {
        Class<?> cls = from.getClass();
        String className = cls.getName();
        List<String> managedFields = managedFields(className);
        if (null == managedFields) {
            return;
        }
        for (String fieldName : managedFields) {
            String keyCacheField = keyCacheField(fieldName);
            $.setProperty(to, $.getProperty(from, keyCacheField), keyCacheField);
            $.setProperty(to, $.getProperty(from, fieldName), fieldName);
        }
    }
}
