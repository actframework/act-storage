package act.storage;

import act.ActComponent;
import act.app.App;
import act.asm.AnnotationVisitor;
import act.asm.FieldVisitor;
import act.asm.Type;
import act.asm.tree.FieldNode;
import act.storage.db.DbHooker;
import act.util.AppByteCodeEnhancer;
import act.util.AsmTypes;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhance entity class that has fields with managed sobject fields:
 * - Add @Transient annotation to sobject field
 * - Add corresponding sobject key field
 */
@ActComponent
public class EntityClassEnhancer extends AppByteCodeEnhancer<EntityClassEnhancer> {

    private boolean hasManagedFields = false;
    private String cn;
    private StorageServiceManager ssm;
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
            for (DbHooker hooker : dbHookers) {
                for (String fn : ssm.managedFields(cn)) {
                    boolean isCollection = ssm.isCollection(cn, fn);
                    doEnhanceOn(fn, hooker, isCollection);
                }
            }
        }
        super.visitEnd();
    }

    private void doEnhanceOn(String sobjField, DbHooker hooker, boolean isCollection) {
        addKeyField(sobjField, isCollection, hooker);
    }

    private void addKeyField(String sobjField, boolean isCollection, DbHooker hooker) {
        String fieldName = S.builder(sobjField).append("Key").toString();
        String desc = isCollection ? "Ljava/util/Set;" : AsmTypes.STRING_DESC;
        String signature = isCollection ? "Ljava/util/Set<Ljava/lang/String;>;" : null;
        FieldVisitor fv = cv.visitField(ACC_PRIVATE, fieldName, desc, signature, null);
        AnnotationVisitor av = fv.visitAnnotation(Type.getType(hooker.transientAnnotationType()).getDescriptor(), true);
        av.visitEnd();
        fv.visitEnd();
    }

    private boolean shouldEnhance() {
        return hasManagedFields;
    }

    private synchronized StorageServiceManager ssm() {
        if (null == ssm) {
            ssm = App.instance().singleton(StorageServiceManager.class);
        }
        return ssm;
    }

}
