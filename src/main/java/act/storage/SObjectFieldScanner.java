package act.storage;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppByteCodeScannerBase;
import act.app.AppSourceCodeScanner;
import act.asm.AnnotationVisitor;
import act.asm.FieldVisitor;
import act.asm.Type;
import act.util.AppCodeScannerPluginBase;
import act.util.ByteCodeVisitor;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Set;

/**
 * Scan classes and find fields with type {@link org.osgl.storage.ISObject}
 */
@ActComponent
public class SObjectFieldScanner extends AppCodeScannerPluginBase {
    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        return null;
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        return new _AppByteCodeScanner(app);
    }

    @Override
    public boolean load() {
        return true;
    }

    private static class _AppByteCodeScanner extends AppByteCodeScannerBase {

        private App app;

        _AppByteCodeScanner(App app) {
            this.app = $.notNull(app);
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public ByteCodeVisitor byteCodeVisitor() {
            return new _ByteCodeVisitor(app);
        }

        @Override
        public void scanFinished(String className) {
        }
    }

    private static class _ByteCodeVisitor extends ByteCodeVisitor {
        private App app;
        private boolean managed;
        private String className;
        private String serviceId;
        private String contextPath;
        private UpdatePolicy updatePolicy = UpdatePolicy.DELETE_OLD_DATA;
        _ByteCodeVisitor(App app) {
            this.app = $.notNull(app);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            className = name.replace('/', '.');
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (isStoreAnno(desc)) {
                managed = true;
                return new AnnotationVisitor(ASM5, av) {
                    @Override
                    public void visit(String name, Object value) {
                        if ("value".equals(name)) {
                            String serviceId, contextPath;
                            String url = S.string(value);
                            if (url.contains(":")) {
                                serviceId = S.beforeFirst(url, ":");
                                contextPath = S.afterFirst(url, ":");
                            } else {
                                serviceId = null;
                                contextPath = url;
                            }
                            _ByteCodeVisitor.this.serviceId = serviceId;
                            _ByteCodeVisitor.this.contextPath = contextPath;
                        }
                    }

                    @Override
                    public void visitEnum(String name, String desc, String value) {
                        super.visitEnum(name, desc, value);
                        if ("updatePolicy".equals(name)) {
                            updatePolicy = UpdatePolicy.valueOf(value);
                        }
                    }
                };
            }
            return av;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (managed) {
                storageServiceManager().registerServiceIndex(className, null, false, serviceId, contextPath, updatePolicy);
            }
        }

        private static final Set<String> SUPPORTED_STORE_FIELD_DESCS = C.set(
                "Lorg/osgl/storage/ISObject;",
                "Lorg/osgl/storage/impl/SObject;");

        private static final Set<String> SUPPORTED_STORE_FIELD_SIGNATURES = C.set(
                "Ljava/util/List<Lorg/osgl/storage/ISObject;>;",
                "Ljava/util/List<Lorg/osgl/storage/SObject;>;",
                "Ljava/util/Set<Lorg/osgl/storage/ISObject;>;",
                "Ljava/util/Set<Lorg/osgl/storage/SObject;>;"
                );

        private static boolean isSObjectField(String desc, String signature) {
            if (null != signature) {
                return SUPPORTED_STORE_FIELD_SIGNATURES.contains(signature);
            } else {
                return SUPPORTED_STORE_FIELD_DESCS.contains(desc);
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, final String signature, Object value) {
            FieldVisitor fv = super.visitField(access, name, desc, signature, value);

            boolean isStatic = ((access & ACC_STATIC) != 0);
            if (isStatic) {
                return fv;
            }
            if (isSObjectField(desc, signature)) {
                final String fieldName = name;
                return new FieldVisitor(ASM5, fv) {

                    private String serviceId = StorageServiceManager.DEFAULT;
                    private String contextPath = "";
                    private boolean managed = false;
                    private UpdatePolicy updatePolicy = UpdatePolicy.DELETE_OLD_DATA;
                    private boolean isCollection = null != signature;

                    @Override
                    public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
                        AnnotationVisitor av = super.visitAnnotation(desc, visible);
                        if (isStoreAnno(desc)) {
                            managed = true;
                            return new AnnotationVisitor(ASM5, av) {
                                @Override
                                public void visit(String name, Object value) {
                                    if ("value".equals(name)) {
                                        String url = S.string(value);
                                        if (url.contains(":")) {
                                            serviceId = S.beforeFirst(url, ":");
                                            contextPath = S.afterFirst(url, ":");
                                        } else {
                                            serviceId = null;
                                            contextPath = url;
                                        }
                                    }
                                }

                                @Override
                                public void visitEnum(String name, String desc, String value) {
                                    super.visitEnum(name, desc, value);
                                    if ("updatePolicy".equals(name)) {
                                        updatePolicy = UpdatePolicy.valueOf(value);
                                    }
                                }
                            };
                        }
                        return av;
                    }

                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (managed) {
                            if (S.blank(serviceId)) {
                                serviceId = _ByteCodeVisitor.this.serviceId;
                            }
                            if (S.blank(contextPath)) {
                                contextPath = _ByteCodeVisitor.this.contextPath;
                            }
                            storageServiceManager().registerServiceIndex(className, fieldName, isCollection, serviceId, contextPath, updatePolicy);
                        }
                    }
                };
            }
            return fv;
        }

        private boolean isStoreAnno(String desc) {
            return Type.getType(Store.class).equals(Type.getType(desc));
        }

        private StorageServiceManager storageServiceManager() {
            return app.singleton(StorageServiceManager.class);
        }

    }
}
