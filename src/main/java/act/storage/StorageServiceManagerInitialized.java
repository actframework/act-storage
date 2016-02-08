package act.storage;

import act.event.ActEvent;

/**
 * Event triggered immediately when {@link StorageServiceManager} is initialized
 */
public class StorageServiceManagerInitialized extends ActEvent<StorageServiceManager> {

    public StorageServiceManagerInitialized(StorageServiceManager source) {
        super(source);
    }

}
