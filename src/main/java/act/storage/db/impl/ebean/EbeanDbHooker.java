package act.storage.db.impl.ebean;

import act.app.App;
import act.db.ebean.PreEbeanCreation;
import act.db.morphia.MorphiaService;
import act.event.ActEventListenerBase;
import act.storage.StorageServiceManager;
import act.storage.UpdatePolicy;
import act.storage.db.DbHooker;
import act.storage.db.util.Setter;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.event.BeanPersistAdapter;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanPersistRequest;
import org.osgl.$;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.util.S;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.EventObject;
import java.util.List;

/**
 * hook to {@link act.db.morphia.MorphiaPlugin morphia db layer}
 */
public class EbeanDbHooker implements DbHooker {

    private StorageServiceManager ssm;

    public EbeanDbHooker() {
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
        App.instance().eventBus().bind(PreEbeanCreation.class, new ActEventListenerBase<PreEbeanCreation>("storage:hook-ebean-lifecycle-interceptor") {
            @Override
            public void on(PreEbeanCreation event) throws Exception {
                ServerConfig config = event.source();
                config.add(new StorageFieldConverter(ssm()));
            }
        });
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || null != obj && EbeanDbHooker.class.getName().equals(obj.getClass().getName());
    }

    private synchronized StorageServiceManager ssm() {
        if (null == ssm) {
            ssm = App.instance().singleton(StorageServiceManager.class);
        }
        return ssm;
    }
}

class StorageFieldConverter extends BeanPersistAdapter implements BeanPersistController {

    private StorageServiceManager ssm;

    StorageFieldConverter(StorageServiceManager ssm) {
        this.ssm = $.notNull(ssm);
    }

    @Override
    public boolean isRegisterFor(Class<?> cls) {
        return false;
    }

    @Override
    public boolean preDelete(BeanPersistRequest<?> request) {
        return super.preDelete(request);
    }

    @Override
    public boolean preInsert(BeanPersistRequest<?> request) {
        return super.preInsert(request);
    }

    @Override
    public boolean preUpdate(BeanPersistRequest<?> request) {
        return super.preUpdate(request);
    }

}
