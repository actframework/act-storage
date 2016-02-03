package act.storage.db.impl.morphia;

import act.Act;
import act.storage.db.DbProbe;

/**
 * Check if Morphia plugin is loaded
 */
public class MorphiaDbProbe implements DbProbe {
    @Override
    public boolean exists() {
        return null != Act.dbManager().plugin("act.db.morphia.MorphiaPlugin");
    }

    @Override
    public String dbHookerClass() {
        return "act.storage.db.impl.morphia.MorphiaDbHooker";
    }
}
