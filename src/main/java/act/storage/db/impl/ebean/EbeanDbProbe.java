package act.storage.db.impl.ebean;

import act.Act;
import act.storage.db.DbProbe;

/**
 * Check if Ebean plugin is loaded
 */
public class EbeanDbProbe extends DbProbe {
    @Override
    public boolean exists() {
        return null != Act.dbManager().plugin("act.db.ebean.EbeanPlugin");
    }

    @Override
    public String dbHookerClass() {
        return "act.storage.db.impl.ebean.EbeanDbHooker";
    }
}