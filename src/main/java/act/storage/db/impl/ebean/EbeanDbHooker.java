package act.storage.db.impl.ebean;

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

import act.Act;
import act.app.App;
import act.db.DeleteEvent;
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
import com.avaje.ebean.event.BeanPostLoad;
import org.osgl.$;
import org.osgl.cache.CacheService;
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
                BeanPostLoad postLoad = new StorageFieldConverter(ssm());
                config.add(postLoad);
                BeanPersistController persistController = (BeanPersistController) postLoad;
                config.add(persistController);
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

class StorageFieldConverter extends BeanPersistAdapter implements BeanPersistController, BeanPostLoad {

    private StorageServiceManager ssm;
    private CacheService cacheService;

    StorageFieldConverter(StorageServiceManager ssm) {
        this.ssm = $.notNull(ssm);
        this.cacheService = Act.app().cache("storage-ebean");
    }

    @Override
    public boolean isRegisterFor(Class<?> cls) {
        return ssm.managedFields(cls) != null;
    }

    @Override
    public boolean preDelete(BeanPersistRequest<?> request) {
        System.out.println(">>> about to delete " + request);
        return super.preDelete(request);
    }

    @Override
    public boolean preInsert(BeanPersistRequest<?> request) {
        System.out.println(">>> about to insert " + request);
        return super.preInsert(request);
    }

    @Override
    public boolean preUpdate(BeanPersistRequest<?> request) {
        System.out.println(">>> about to update " + request);
        return super.preUpdate(request);
    }

    @Override
    public void postLoad(Object bean) {

    }
}
