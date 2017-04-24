package act.storage;

import act.app.App;
import act.asm.AnnotationVisitor;
import act.asm.FieldVisitor;
import act.asm.Type;
import act.storage.db.DbHooker;
import act.util.AppByteCodeEnhancer;
import act.util.AsmTypes;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhance entity class that has fields with managed sobject fields:
 * - Add @Transient annotation to sobject field
 * - Add corresponding sobject key field
 */
public class EntityClassEnhancer extends AppByteCodeEnhancer<EntityClassEnhancer> {

    private boolean hasManagedFields = false;
    private String cn;
    private volatile StorageServiceManager ssm;
    private List<DbHooker> dbHookers = C.list();
    private Map<DbHooker, Set<String>> transientAnnotationState = C.newMap();
    private List<String> managedFields = C.newList();

    public EntityClassEnhancer() {
        super(S.F.startsWith("act.").negate());
    }

    @Override
    protected Class<EntityClassEnhancer> subClass() {
        return EntityClassEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        String className = name.replace('/', '.').replace('$', '.');
        List<String> l = ssm().managedFields(className);
        hasManagedFields = !l.isEmpty();
        if (hasManagedFields) {
            cn = className;
            dbHookers = ssm().dbHookers();
            managedFields = l;
            for (DbHooker hooker : dbHookers) {
                transientAnnotationState.put(hooker, C.<String>newSet());
            }
        }
    }

    @Override
    public FieldVisitor visitField(int access, final String name, String desc, String signature, Object value) {
        final FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        if (managedFields.contains(name)) {
            return new FieldVisitor(ASM5, fv) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    for (DbHooker hooker : dbHookers) {
                        Type transientType = Type.getType(hooker.transientAnnotationType());
                        if (transientType.equals(Type.getType(desc))) {
                            Set<String> set = transientAnnotationState.get(hooker);
                            set.add(name);
                        }
                    }
                    return super.visitAnnotation(desc, visible);
                }

                @Override
                public void visitEnd() {
                    if (shouldEnhance()) {
                        for (DbHooker hooker : dbHookers) {
                            if (!transientAnnotationState.get(hooker).contains(name)) {
                                AnnotationVisitor av = fv.visitAnnotation(Type.getType(hooker.transientAnnotationType()).getDescriptor(), true);
                                av.visitEnd();
                            }
                        }
                    }
                    super.visitEnd();
                }
            };
        }
        return fv;
    }

    @Override
    public void visitEnd() {
        StorageServiceManager ssm = ssm();
        if (shouldEnhance()) {
            Map<String, FieldVisitor> fvMap = new HashMap<>();
            for (DbHooker hooker : dbHookers) {
                for (String fn : ssm.managedFields(cn)) {
                    FieldVisitor fv = fvMap.get(fn);
                    boolean isCollection = ssm.isCollection(cn, fn);
                    FieldVisitor fv0 = doEnhanceOn(fn, hooker, isCollection, fv);
                    if (null == fv) {
                        fvMap.put(fn, fv0);
                    }
                }
            }
            for (FieldVisitor fv : fvMap.values()) {
                fv.visitEnd();
            }
        }
        super.visitEnd();
    }

    private FieldVisitor doEnhanceOn(String sobjField, DbHooker hooker, boolean isCollection, FieldVisitor fv) {
        return addKeyField(sobjField, isCollection, hooker, fv);
    }

    private FieldVisitor addKeyField(String sobjField, boolean isCollection, DbHooker hooker, FieldVisitor fv) {
        String fieldName = S.builder(sobjField).append("Key").toString();
        String desc = isCollection ? "Ljava/util/Set;" : AsmTypes.STRING_DESC;
        String signature = isCollection ? "Ljava/util/Set<Ljava/lang/String;>;" : null;
        fv = null == fv ? cv.visitField(ACC_PRIVATE, fieldName, desc, signature, null) : fv;
        AnnotationVisitor av = fv.visitAnnotation(Type.getType(hooker.transientAnnotationType()).getDescriptor(), true);
        av.visitEnd();
        return fv;
    }

    private boolean shouldEnhance() {
        return hasManagedFields;
    }

    private StorageServiceManager ssm() {
        if (null == ssm) {
            synchronized (this) {
                if (null != ssm) {
                    ssm = App.instance().singleton(StorageServiceManager.class);
                }
            }
        }
        return ssm;
    }

}
