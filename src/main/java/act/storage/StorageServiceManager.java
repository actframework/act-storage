package act.storage;

import act.Destroyable;
import act.app.App;
import act.app.AppService;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import act.plugin.AppServicePlugin;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.IStorageService;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;
import java.util.Set;

/**
 * Manage {@link org.osgl.storage.IStorageService} instances
 */
public class StorageServiceManager extends AppServicePlugin implements AppService<StorageServiceManager>, Destroyable {

    private static Logger logger = LogManager.get(StorageServiceManager.class);

    /**
     * The key to fetch default {@link org.osgl.storage.IStorageService storage service}
     */
    public static final String DEFAULT = IStorageService.DEFAULT;

    /**
     * map service id to storage service instance
     */
    private Map<String, IStorageService> serviceMap = C.newMap();

    /**
     * map class name plus field name to storage service instance
     */
    private Map<String, IStorageService> serviceIndex = C.newMap();

    /**
     * map class name plus field name to {@link UpdatePolicy}
     */
    private Map<String, UpdatePolicy> updatePolicyIndex = C.newMap();

    private App app;

    private boolean isDestroyed;

    public StorageServiceManager() {
    }

    @Override
    protected void applyTo(App app) {
        this.app = app;
        initServices(app.config());
        app.singletonRegistry().register(getClass(), this);
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
        IStorageService ss = serviceMap.get(serviceId);
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
        serviceIndex.put(key, ss);

        if (null != updatePolicy) {
            updatePolicyIndex.put(key, updatePolicy);
        }
    }

    Set<String> storageFields() {
        return serviceIndex.keySet();
    }

    public IStorageService storageService(String className, String fieldName) {
        String key = ssKey(className, fieldName);
        IStorageService svc = serviceIndex.get(key);
        if (null == svc) {
            svc = serviceIndex.get(className);
        }
        if (null == svc) {
            svc = app().uploadFileStorageService();
        }
        return svc;
    }

    public UpdatePolicy updatePolicy(String className, String fieldName) {
        return updatePolicyIndex.get(ssKey(className, fieldName));
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
        Destroyable.Util.tryDestroyAll(serviceMap.values());
        serviceMap.clear();
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
                serviceMap.put(DEFAULT, svc);
                return;
            }
        }

        if (storageConfig.containsKey("ss.instances")) {
            String instances = storageConfig.get("ss.instances").toString();
            for (String dbId: instances.split("[,\\s;:]+")) {
                initService(dbId, storageConfig);
            }
        }
        if (serviceMap.containsKey(DEFAULT)) return;
        // try init default service if conf found
        String ssId = null;
        if (storageConfig.containsKey("ss." + DEFAULT +".impl")) {
            ssId = DEFAULT;
        } else if (storageConfig.containsKey("ss.impl")) {
            ssId = "";
        }
        if (null != ssId) {
            initService(ssId, storageConfig);
        } else if (serviceMap.size() == 1) {
            IStorageService svc = serviceMap.values().iterator().next();
            serviceMap.put(DEFAULT, svc);
            logger.warn("Storage service configuration not found. Use the sole one storage service[%s] as default service", svc.id());
        } else {
            if (serviceMap.isEmpty()) {
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
                    serviceMap.put(DEFAULT, svc);
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
        svcConf.put(IStorageService.CONF_ID, ssId);
        IStorageService svc = plugin.initStorageService(ssId, app(), svcConf);
        serviceMap.put(svcId, svc);
        logger.info("storage service[%s] initialized", svcId);
    }
}
