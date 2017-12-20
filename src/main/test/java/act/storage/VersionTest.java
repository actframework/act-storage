package act.storage;

import org.junit.Test;
import osgl.ut.TestBase;

public class VersionTest extends TestBase {

    @Test
    public void versionShallContainsStorage() {
        yes(StoragePlugin.VERSION.toString().contains("storage"));
    }
}
