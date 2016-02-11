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
 * Enhance entity class that has fields with managed sobject fields
 */
@ActComponent
public class EntityClassEnhancer extends AppByteCodeEnhancer<EntityClassEnhancer> {

    private boolean hasManagedFields = false;
    private String cn;
    private StorageServiceManager ssm;
    private List<DbHooker> dbHookers = C.list();
    //private Map<DbHooker, Boolean> entityAnnotationState = C.newMap();
    private Map<DbHooker, Set<String>> transientAnnotationState = C.newMap();
    private List<String> managedFields = C.newList();

    public EntityClassEnhancer() {
        super($.F.<String>yes());
    }

    @Override
    protected Class<EntityClassEnhancer> subClass() {
        return EntityClassEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        String className = name.replace('/', '.').replace('$', '.');
        if (className.contains("Image")) {
            $.nil();
        }
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

//    @Override
//    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//        Type type = Type.getType(desc);
//        for (DbHooker hooker : dbHookers) {
//            if (Type.getType(hooker.entityAnnotation()).equals(type)) {
//                entityAnnotationState.put(hooker, true);
//            }
//        }
//        return super.visitAnnotation(desc, visible);
//    }
//
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
                    for (DbHooker hooker : dbHookers) {
                        if (shouldEnhance(hooker)) {
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
        for (DbHooker hooker : dbHookers) {
            if (shouldEnhance(hooker)) {
                for (String fn : ssm.managedFields(cn)) {
                    doEnhanceOn(fn, hooker);
                }
            }
        }
        super.visitEnd();
    }

    private void doEnhanceOn(String sobjField, DbHooker hooker) {
        addKeyField(sobjField, hooker);
    }

    private void addKeyField(String sobjField, DbHooker hooker) {
        String fieldName = S.builder(sobjField).append("Key").toString();
        FieldVisitor fv = cv.visitField(ACC_PRIVATE, fieldName, AsmTypes.STRING_DESC, null, null);
        AnnotationVisitor av = fv.visitAnnotation(Type.getType(hooker.transientAnnotationType()).getDescriptor(), true);
        av.visitEnd();
        fv.visitEnd();
    }

    private void addTransientAnnotation(String sobjField, DbHooker hooker) {
        $.nil();
    }

    private boolean shouldAddTransientAnnotation(String sobjField, DbHooker hooker) {
        return !transientAnnotationState.get(hooker).contains(sobjField);
    }

    private boolean shouldEnhance(DbHooker hooker) {
        return hasManagedFields /* && entityAnnotationState.containsKey(hooker) && entityAnnotationState.get(hooker)*/;
    }

    private synchronized StorageServiceManager ssm() {
        if (null == ssm) {
            ssm = App.instance().singleton(StorageServiceManager.class);
        }
        return ssm;
    }

}
