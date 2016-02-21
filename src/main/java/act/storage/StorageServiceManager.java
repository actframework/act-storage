package act.storage;

import act.Destroyable;
import act.app.App;
import act.app.AppService;
import act.app.event.AppEventId;
import act.conf.AppConfig;
import act.di.DiBinder;
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

import javax.inject.Singleton;
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

    /**
     * map class name plus field name to {@link UpdatePolicy}
     */
    private Map<String, UpdatePolicy> updatePolicyByClassField = C.newMap();

    /**
     * Map a list of managed sobject fields to class name
     */
    private Map<String, List<String>> managedFieldByClass = C.newMap();

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
                app().eventBus().emit(new DiBinder<StorageServiceManager>(this, StorageServiceManager.class) {
                    @Override
                    public StorageServiceManager resolve(App app) {
                        return StorageServiceManager.this;
                    }
                });
            }
        });
        app.eventBus().emit(new StorageServiceManagerInitialized(this));
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

    public IStorageService storageService(String serviceId) {
        return serviceById.get(serviceId);
    }

    public UpdatePolicy updatePolicy(String className, String fieldName) {
        return updatePolicyByClassField.get(ssKey(className, fieldName));
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
        Destroyable.Util.tryDestroyAll(serviceById.values());
        Destroyable.Util.tryDestroyAll(dbHookers);
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
                if (s.startsWith(className)) {
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

    public Setter setter(Class c, String fieldName) {
        $.T2<Class, String> classFieldNamePair = $.T2(c, fieldName);
        Setter setter = setterByClass.get(classFieldNamePair);
        if (null == setter) {
            setter = Setter.probe(c, fieldName);
            setterByClass.put(classFieldNamePair, setter);
        }
        return setter;
    }

    public void addDbHooker(DbHooker dbHooker) {
        if (!this.dbHookers.contains(dbHooker)) {
            this.dbHookers.add(dbHooker);
            dbHooker.hookLifecycleInterceptors();
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
                logger.warn("Storage service not intialized: need to specify default storage service implementation");
                return;
            } else {
                logger.warn("Storage configuration not found. Will try to init default service with the sole storage plugin: %s", storagePlugin);
                IStorageService svc = storagePlugin.initStorageService(DEFAULT, app(), C.<String, String>newMap());
                serviceById.put(DEFAULT, svc);
                return;
            }
        }

        if (storageConfig.containsKey("ss.instances")) {
            String instances = storageConfig.get("ss.instances").toString();
            for (String dbId: instances.split("[,\\s;:]+")) {
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
                throw E.invalidConfiguration("Default db service for the application needs to be specified");
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
            Setter setter = setter(cls, keyCacheField);
            if (null != setter) {
                setter.set(to, $.getProperty(from, keyCacheField));
            }
            setter = setter(cls, fieldName);
            if (null != setter) {
                setter.set(to, $.getProperty(from, fieldName));
            }
        }
    }
}
