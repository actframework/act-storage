package act.storage;

import act.app.App;
import act.app.event.AppEventId;
import act.storage.db.DbHooker;
import act.storage.db.DbProbe;
import act.util.SubTypeFinder2;
import org.osgl.$;

public class DbProbeFinder extends SubTypeFinder2<DbProbe> {

    public DbProbeFinder() {
        super(DbProbe.class);
    }

    @Override
    protected void found(Class<DbProbe> target, final App app) {
        DbProbe probe = app.newInstance(target);
        if (probe.exists()) {
            final StorageServiceManager storageServiceManager = app.singleton(StorageServiceManager.class);
            final Class dbHookerClass = $.classForName(probe.dbHookerClass(), app.classLoader());
            DbHooker hooker = $.cast(app.newInstance(dbHookerClass));
            storageServiceManager.addDbHooker(hooker);
        }
    }

}
