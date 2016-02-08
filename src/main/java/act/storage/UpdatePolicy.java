package act.storage;

import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.util.S;

/**
 * Defines how to deal with updating to {@link org.osgl.storage.ISObject} typed field
 */
public enum UpdatePolicy {
    /**
     * If a field is specified as {@code REJECT} and the framework
     * detect the sobject has been updated, the framework will throw
     * out an exception to reject the update when persist the entity
     */
    REJECT () {
        @Override
        public void handleUpdate(String prevKey, ISObject updatedObject, IStorageService storageService) {
            if (null == updatedObject) {
                if (S.blank(prevKey)) {
                    return;
                }
                throw new IllegalStateException("sobject is read only");
            } else if (S.neq(prevKey, updatedObject.getKey())) {
                throw new IllegalStateException("sobject is read only");
            }
        }
    },

    /**
     * If a field is specified as {@code DELETE_OLD_DATA} and framework
     * detect the sobject field has been updated, the framework will
     * delete old sobject and then save the updated one when persist
     * the entity
     */
    DELETE_OLD_DATA () {
        @Override
        public void handleUpdate(String prevKey, ISObject updatedObject, IStorageService storageService) {
            if (S.blank(prevKey)) {
                return;
            }
            if (null != updatedObject && S.neq(prevKey, updatedObject.getKey())) {
                storageService.remove(prevKey);
            }
        }
    },

    /**
     * If a field is specified as {@code KEEP_OLD_DATA} and framework
     * detect the sobject field has been updated, the framework will
     * NOT delete old sobject. However the updated one will be saved as well
     */
    KEEP_OLD_DATA;

    public void handleUpdate(String prevKey, ISObject updatedObject, IStorageService storageService) {}
}
