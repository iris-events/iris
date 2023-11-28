package org.iris_events.deployment.scanner;

import java.util.Collection;
import java.util.List;

import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;
import org.iris_events.deployment.builditem.MessageInfoBuildItem;
import org.iris_events.deployment.validation.MessageHandlerAnnotationInstanceValidator;
import org.iris_events.deployment.validation.MessageHandlerCompatibilityValidator;
import org.iris_events.deployment.validation.SnapshotHandlerAnnotationInstanceValidator;
import org.iris_events.producer.EventProducer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scanner {
    private final static Logger log = LoggerFactory.getLogger(Scanner.class);

    private final IndexView indexView;
    private final List<HandlerAnnotationScanner> messageHandlerAnnotationScanners;
    private final MessageAnnotationScanner messageAnnotationScanner;
    private final IrisGeneratedAnnotationScanner irisGeneratedAnnotationScanner;

    public Scanner(IndexView indexView, String serviceName) {
        this.indexView = indexView;
        this.messageHandlerAnnotationScanners = getAnnotationScanners(serviceName);
        this.messageAnnotationScanner = new MessageAnnotationScanner();
        this.irisGeneratedAnnotationScanner = new IrisGeneratedAnnotationScanner();
    }

    public List<MessageHandlerInfoBuildItem> scanEventHandlerAnnotations() {
        final var messageHandlerInfoBuildItems = messageHandlerAnnotationScanners.stream()
                .map(messageHandlerScanner -> messageHandlerScanner.scanHandlerAnnotations(indexView))
                .flatMap(Collection::stream)
                .toList();

        MessageHandlerCompatibilityValidator.validate(messageHandlerInfoBuildItems);

        return messageHandlerInfoBuildItems;
    }

    public List<MessageInfoBuildItem> scanMessageAnnotations() {
        return messageAnnotationScanner.scanMessageAnnotations(indexView);
    }

    public List<MessageInfoBuildItem> scanIrisGeneratedAnnotations() {
        return irisGeneratedAnnotationScanner.scanIrisGeneratedAnnotations(indexView);
    }

    private List<HandlerAnnotationScanner> getAnnotationScanners(String serviceName) {
        final var snapshotMessageHandlerAnnotationScanner = getSnapshotMessageHandlerAnnotationScanner(serviceName);
        final var messageHandlerAnnotationScanner = getMessageHandlerAnnotationScanner(serviceName);

        return List.of(snapshotMessageHandlerAnnotationScanner, messageHandlerAnnotationScanner);
    }

    private MessageHandlerAnnotationScanner getMessageHandlerAnnotationScanner(String serviceName) {
        final var messageHandlerAnnotationInstanceValidator = new MessageHandlerAnnotationInstanceValidator(indexView,
                serviceName);
        return new MessageHandlerAnnotationScanner(messageHandlerAnnotationInstanceValidator);
    }

    private SnapshotMessageHandlerAnnotationScanner getSnapshotMessageHandlerAnnotationScanner(String serviceName) {
        final var snapshotHandlerAnnotationInstanceValidator = new SnapshotHandlerAnnotationInstanceValidator(indexView,
                serviceName);
        return new SnapshotMessageHandlerAnnotationScanner(snapshotHandlerAnnotationInstanceValidator);
    }

    public void scanRpcInvocations() {
        final var eventProducer = this.indexView.getClassByName(EventProducer.class);
        Type objectType = Type.create(DotName.createSimple(Object.class), Type.Kind.CLASS);
        Type responseClassType = Type.create(DotName.createSimple(Class.class), Type.Kind.PARAMETERIZED_TYPE);
        final var sendAndReceiveMethodInfo = eventProducer.method("sendAndReceive", objectType, responseClassType);

        for (ClassInfo classInfo : this.indexView.getKnownClasses()) {
            for (MethodInfo methodInfo : classInfo.methods()) {
                final var calls = callsMethod(methodInfo, sendAndReceiveMethodInfo);
                log.info("Method {} calls sendAndReceive: {}", methodInfo.name(), calls);
            }
        }
    }

    private boolean callsMethod(MethodInfo method, MethodInfo targetMethod) {
        String className = method.declaringClass().name().toString();
        ClassInfo classInfo = this.indexView.getClassByName(DotName.createSimple(className));

        // Use ASM to analyze bytecode of the method
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ClassReader classReader = new ClassReader(classLoader.getResourceAsStream(className.replace('.', '/') + ".class"));
            MethodCallsVisitor methodVisitor = new MethodCallsVisitor(targetMethod);
            classReader.accept(methodVisitor, ClassReader.EXPAND_FRAMES);

            return methodVisitor.callsTargetMethod();
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }

        return false;

    }
}
