package act.storage.db.impl.morphia;

import act.asm.Type;
import act.db.morphia.MorphiaService;
import act.storage.db.DbHooker;
import com.mongodb.DBObject;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;

/**
 * hook to {@link act.db.morphia.MorphiaPlugin morphia db layer}
 */
public class MorphiaDbHooker implements DbHooker {

    private Mapper mapper;

    public MorphiaDbHooker() {
        this.mapper = MorphiaService.mapper();
    }

    @Override
    public Type entityAnnotation() {
        return Type.getType(Entity.class);
    }

    @Override
    public Type transientAnnotationType() {
        return Type.getType(Transient.class);
    }

    @Override
    public void hookLifecycleInterceptors() {
        mapper.addInterceptor(new StorageFieldConverter());
    }
}

class StorageFieldConverter extends AbstractEntityInterceptor implements EntityInterceptor {
    @Override
    public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
        super.postLoad(ent, dbObj, mapper);
    }

    @Override
    public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
        super.prePersist(ent, dbObj, mapper);
    }
}
