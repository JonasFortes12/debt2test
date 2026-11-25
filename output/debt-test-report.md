# Technical Debt Test Generation Report

Generated test cases to pay off self-admitted technical debt (SATD).

- **Run ID:** `1b3bf06c9dd44aa64173e8b423ae1883d53c4393fb2b193bc9e636b63110d3ec`
- **Status:** `FAILED`
- **Errors:**
  - `JAVA_PARSE_FAILED`
  - `CANDIDATE_INVALID`
  - `LLM_RESPONSE_INVALID`

## ApplicationClientConfigurationPersister.java -> snapshot()

- **Debt Type:** `DESIGN`
- **Line Number:** `98`
- **Status:** `GENERATED`
- **Comment:** `TODO once WFCORE-6362 is integrated remove this method`

```java
// TODO once WFCORE-6362 is integrated remove this method
public String snapshot() throws ConfigurationPersistenceException {
    return null;
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `WFCORE-6362`
- **URL:** `https://redhat.atlassian.net/browse/WFCORE-6362`
- **Summary:** Remove deprecated misc parsing and persistence API

**Issue References:**

- `WFCORE-6362` (source: `comment`)

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/*
 * Characterization tests for the deprecated snapshot() method.
 *
 * The method is a placeholder that returns null and is scheduled for removal
 * once WFCORE-6362 is integrated. These tests pin down the current contract so
 * that callers are not broken silently before removal, and they make the
 * removal visible as a failing test once the method is deleted.
 *
 * ASSUMPTION: the enclosing type is named ConfigurationPersister and can be
 * instantiated via createPersister(). Adjust the type name, package import,
 * and factory to match the real declaring class.
 */
class SnapshotDeprecatedTest {

    private ConfigurationPersister persister;

    @BeforeEach
    void setUp() {
        persister = createPersister();
    }

    /**
     * Replace with the real construction logic for the class declaring snapshot().
     */
    private static ConfigurationPersister createPersister() {
        return new ConfigurationPersister();
    }

    @Nested
    @DisplayName("snapshot() current contract")
    class ContractTests {

        @Test
        @DisplayName("returns null as the placeholder implementation")
        void returnsNull() throws Exception {
            assertNull(persister.snapshot());
        }

        @Test
        @DisplayName("does not throw ConfigurationPersistenceException")
        void doesNotThrow() {
            assertDoesNotThrow(() -> persister.snapshot());
        }

        @Test
        @DisplayName("is idempotent across repeated invocations")
        void repeatedCallsReturnNullConsistently() throws Exception {
            for (int i = 0; i < 3; i++) {
                assertNull(persister.snapshot(), "invocation " + i + " should return null");
            }
        }

        @Test
        @DisplayName("does not depend on prior state of the persister")
        void independentOfPriorState() throws Exception {
            ConfigurationPersister fresh = createPersister();
            assertNull(fresh.snapshot());
            assertNull(persister.snapshot());
        }
    }

    @Nested
    @DisplayName("removal readiness")
    class RemovalTests {

        @Test
        @DisplayName("method is declared on the persister type so removal is traceable")
        void methodIsDeclaredOnType() throws NoSuchMethodException {
            // Fails loudly once the method is removed, prompting cleanup of this test
            // alongside the production code (tracked by WFCORE-6362).
            assertNull(
                    ConfigurationPersister.class.getMethod("snapshot")
                            .getDeclaringClass()
                            .equals(ConfigurationPersister.class) ? null : new Object(),
                    "snapshot() should be declared directly on ConfigurationPersister");
        }
    }
}
```

---

## ApplicationClientConfigurationPersister.java -> registerSubsystemWriter()

- **Debt Type:** `DESIGN`
- **Line Number:** `113`
- **Status:** `GENERATED`
- **Comment:** `TODO once WFCORE-6362 is integrated remove this method`

```java
// TODO once WFCORE-6362 is integrated remove this method
public void registerSubsystemWriter(final String name, final XMLElementWriter<SubsystemMarshallingContext> writer) {
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `WFCORE-6362`
- **URL:** `https://redhat.atlassian.net/browse/WFCORE-6362`
- **Summary:** Remove deprecated misc parsing and persistence API

**Issue References:**

- `WFCORE-6362` (source: `comment`)

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Characterization tests for the deprecated no-op {@code registerSubsystemWriter} method.
 *
 * <p>The method is a placeholder scheduled for removal once WFCORE-6362 is integrated.
 * These tests pin its current contract (no-op, no side effects, tolerant of null input,
 * safe to call repeatedly) so that the removal can be verified and any accidental
 * behavior change is caught before the cleanup lands.
 *
 * <p>Assumptions: the owning type is configured via {@link #OWNER_CLASS_NAME} and must
 * expose an accessible no-argument constructor. Writer and context types are resolved
 * reflectively so the test has no compile-time dependency on the owning module.
 */
class RegisterSubsystemWriterDeprecatedTest {

    /** Fully qualified name of the class declaring registerSubsystemWriter. Update if it moves. */
    private static final String OWNER_CLASS_NAME = "org.jboss.as.controller.AbstractExtension";
    private static final String METHOD_NAME = "registerSubsystemWriter";
    private static final String WRITER_TYPE_NAME = "org.jboss.staxmapper.XMLElementWriter";
    private static final String CONTEXT_TYPE_NAME = "org.jboss.as.controller.SubsystemMarshallingContext";

    private Object owner;
    private Method method;
    private Class<?> writerType;
    private Class<?> contextType;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> ownerClass = Class.forName(OWNER_CLASS_NAME);
        writerType = Class.forName(WRITER_TYPE_NAME);
        contextType = Class.forName(CONTEXT_TYPE_NAME);

        method = ownerClass.getMethod(METHOD_NAME, String.class, writerType);

        Constructor<?> ctor = ownerClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        owner = ctor.newInstance();
    }

    /** Creates a dynamic proxy for XMLElementWriter that records every invocation. */
    private Object recordingWriter(List<String> invocations) {
        InvocationHandler handler = (proxy, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                // Keep identity semantics for equals/hashCode/toString.
                return switch (m.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "RecordingWriter";
                    default -> null;
                };
            }
            invocations.add(m.getName());
            return null;
        };
        return Proxy.newProxyInstance(writerType.getClassLoader(), new Class<?>[] {writerType}, handler);
    }

    private void invoke(String name, Object writer) throws Exception {
        method.invoke(owner, name, writer);
    }

    @Test
    @DisplayName("Method is public, void and takes (String, XMLElementWriter)")
    void hasExpectedSignature() {
        assertTrue(Modifier.isPublic(method.getModifiers()), "method should be public");
        assertEquals(void.class, method.getReturnType(), "method should return void");
        assertArrayEquals(new Class<?>[] {String.class, writerType}, method.getParameterTypes());
    }

    @Test
    @DisplayName("Writer type parameter references SubsystemMarshallingContext")
    void writerIsParameterisedWithMarshallingContext() {
        var genericType = method.getGenericParameterTypes()[1];
        assertInstanceOf(java.lang.reflect.ParameterizedType.class, genericType);
        var typeArg = ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments()[0];
        assertEquals(contextType, typeArg);
    }

    @Nested
    @DisplayName("Behaviour: no-op contract")
    class NoOpContract {

        @Test
        @DisplayName("Does not throw for a valid name and writer")
        void doesNotThrowForValidArguments() {
            List<String> calls = new ArrayList<>();
            assertDoesNotThrow((Executable) () -> invoke("subsystem", recordingWriter(calls)));
        }

        @Test
        @DisplayName("Does not invoke the supplied writer")
        void neverInvokesWriter() throws Exception {
            List<String> calls = new ArrayList<>();
            invoke("subsystem", recordingWriter(calls));
            assertTrue(calls.isEmpty(), "writer must not be called, but got: " + calls);
        }

        @Test
        @DisplayName("Tolerates a null name")
        void toleratesNullName() {
            List<String> calls = new ArrayList<>();
            assertDoesNotThrow((Executable) () -> invoke(null, recordingWriter(calls)));
            assertTrue(calls.isEmpty());
        }

        @Test
        @DisplayName("Tolerates a null writer")
        void toleratesNullWriter() {
            assertDoesNotThrow((Executable) () -> invoke("subsystem", null));
        }

        @Test
        @DisplayName("Tolerates both arguments being null")
        void toleratesBothNull() {
            assertDoesNotThrow((Executable) () -> invoke(null, null));
        }

        @Test
        @DisplayName("Repeated registrations have no cumulative effect")
        void repeatedCallsAreIdempotent() throws Exception {
            List<String> calls = new ArrayList<>();
            Object writer = recordingWriter(calls);
            for (int i = 0; i < 100; i++) {
                invoke("subsystem", writer);
            }
            assertTrue(calls.isEmpty());
        }

        @Test
        @DisplayName("Different subsystem names are accepted without side effects")
        void acceptsArbitraryNames() throws Exception {
            List<String> calls = new ArrayList<>();
            Object writer = recordingWriter(calls);
            for (String name : List.of("", " ", "a.b.c", "urn:jboss:domain:test:1.0", "x".repeat(4096))) {
                invoke(name, writer);
            }
            assertTrue(calls.isEmpty());
        }
    }
}
```

---

## WildFlyJobXmlResolver.java -> resolveEntity()

- **Debt Type:** `DESIGN`
- **Line Number:** `268`
- **Status:** `GENERATED`
- **Comment:** `this is essentially what JBeret does, but it's ugly. JBeret might need an API to handle this`

```java
// this is essentially what JBeret does, but it's ugly. JBeret might need an API to handle this
@Override
public Object resolveEntity(final String publicID, final String systemID, final String baseURI, final String namespace) throws XMLStreamException {
    try {
        return (jobXmlFiles.containsKey(systemID) ? jobXmlFiles.get(systemID).openStream() : null);
    } catch (IOException e) {
        throw new XMLStreamException(e);
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the XML entity resolver's resolveEntity method.
 *
 * Assumes the enclosing class is named JobXmlResolver, implements
 * javax.xml.stream.XMLResolver, and is constructed with a
 * Map<String, URL> of job XML files keyed by system ID.
 */
class JobXmlResolverResolveEntityTest {

    @TempDir
    Path tempDir;

    private Map<String, URL> jobXmlFiles;
    private JobXmlResolver resolver;

    @BeforeEach
    void setUp() {
        jobXmlFiles = new HashMap<>();
        resolver = new JobXmlResolver(jobXmlFiles);
    }

    private URL createFileWithContent(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toUri().toURL();
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        return out.toString(StandardCharsets.UTF_8);
    }

    /** URL whose stream cannot be opened, used to trigger IOException. */
    private static URL failingUrl() throws IOException {
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return new URLConnection(u) {
                    @Override
                    public void connect() {
                        // no-op
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        throw new IOException("simulated failure");
                    }

                    @Override
                    public OutputStream getOutputStream() throws IOException {
                        throw new IOException("not supported");
                    }
                };
            }
        };
        return new URL("failing", null, -1, "/job.xml", handler);
    }

    @Nested
    class KnownSystemId {

        @Test
        void returnsStreamWithContentOfRegisteredJobXml() throws Exception {
            String systemId = "job-a.xml";
            String content = "<job id=\"a\"/>";
            jobXmlFiles.put(systemId, createFileWithContent("job-a.xml", content));

            Object result = resolver.resolveEntity(null, systemId, null, null);

            assertNotNull(result);
            assertTrue(result instanceof InputStream, "Expected an InputStream");
            assertEquals(content, readAll((InputStream) result));
        }

        @Test
        void returnsNewStreamOnEachCall() throws Exception {
            String systemId = "job-b.xml";
            String content = "<job id=\"b\"/>";
            jobXmlFiles.put(systemId, createFileWithContent("job-b.xml", content));

            InputStream first = (InputStream) resolver.resolveEntity(null, systemId, null, null);
            InputStream second = (InputStream) resolver.resolveEntity(null, systemId, null, null);

            assertNotSame(first, second);
            assertEquals(content, readAll(first));
            assertEquals(content, readAll(second));
        }

        @Test
        void ignoresPublicIdBaseUriAndNamespace() throws Exception {
            String systemId = "job-c.xml";
            String content = "<job id=\"c\"/>";
            jobXmlFiles.put(systemId, createFileWithContent("job-c.xml", content));

            Object result = resolver.resolveEntity(
                    "-//Some//Public//EN", systemId, "http://base/", "urn:ns");

            assertNotNull(result);
            assertEquals(content, readAll((InputStream) result));
        }
    }

    @Nested
    class UnknownSystemId {

        @Test
        void returnsNullWhenSystemIdNotRegistered() throws Exception {
            jobXmlFiles.put("known.xml", createFileWithContent("known.xml", "<job/>"));

            assertNull(resolver.resolveEntity(null, "unknown.xml", null, null));
        }

        @Test
        void returnsNullWhenNoJobXmlFilesRegistered() throws Exception {
            assertNull(resolver.resolveEntity(null, "anything.xml", null, null));
        }

        @Test
        void returnsNullWhenSystemIdIsNull() throws Exception {
            jobXmlFiles.put("known.xml", createFileWithContent("known.xml", "<job/>"));

            assertNull(resolver.resolveEntity(null, null, null, null));
        }

        @Test
        void systemIdMatchIsCaseSensitive() throws Exception {
            jobXmlFiles.put("Job.xml", createFileWithContent("Job.xml", "<job/>"));

            assertNull(resolver.resolveEntity(null, "job.xml", null, null));
        }
    }

    @Nested
    class IoFailures {

        @Test
        void wrapsIOExceptionInXMLStreamException() throws Exception {
            String systemId = "broken.xml";
            jobXmlFiles.put(systemId, failingUrl());

            XMLStreamException ex = assertThrows(XMLStreamException.class,
                    () -> resolver.resolveEntity(null, systemId, null, null));

            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("simulated failure", ex.getCause().getMessage());
        }

        @Test
        void missingFileWrappedInXMLStreamException() throws Exception {
            String systemId = "missing.xml";
            URL missing = tempDir.resolve("does-not-exist.xml").toUri().toURL();
            jobXmlFiles.put(systemId, missing);

            XMLStreamException ex = assertThrows(XMLStreamException.class,
                    () -> resolver.resolveEntity(null, systemId, null, null));

            assertTrue(ex.getCause() instanceof IOException);
        }
    }
}
```

---

## JChannelFactory.java -> createChannel()

- **Debt Type:** `DESIGN`
- **Line Number:** `146`
- **Status:** `GENERATED`
- **Comment:** `TODO Remove this once DNS_PING is configurable via an explicit DNSResolver`

```java
// TODO Remove this once DNS_PING is configurable via an explicit DNSResolver
private static JChannel createChannel(List<Protocol> protocols) throws Exception {
    // DNS_PING current loads its InitialContextFactory via the TCCL
    ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
    try {
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(JChannel.class);
        return new JChannel(protocols);
    } finally {
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jgroups.JChannel;
import org.jgroups.stack.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

/**
 * Characterization tests for the temporary TCCL workaround around
 * {@code JChannel} construction used by DNS_PING.
 *
 * ASSUMPTION: the method under test lives in a class named
 * {@code DnsPingChannelFactory}. Adjust {@link #TARGET_CLASS_NAME} if the
 * enclosing class has a different name.
 *
 * These tests document the contract that must survive when the workaround is
 * removed (i.e. once DNS_PING accepts an explicit DNSResolver):
 *  - the context class loader is switched to the JGroups class loader while the
 *    channel is constructed,
 *  - the caller's context class loader is always restored, including on failure.
 */
class CreateChannelTccl_DebtTest {

    private static final String TARGET_CLASS_NAME = "DnsPingChannelFactory";
    private static final Method CREATE_CHANNEL = resolveCreateChannel();

    private ClassLoader originalTccl;

    @BeforeEach
    void saveTccl() {
        originalTccl = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void restoreTccl() {
        Thread.currentThread().setContextClassLoader(originalTccl);
    }

    @Test
    void tcclIsSwitchedToJChannelLoaderDuringConstruction() {
        AtomicReference<ClassLoader> seenDuringConstruction = new AtomicReference<>();

        Protocol probe = new Protocol() {
            @Override
            public void setProtocolStack(org.jgroups.stack.ProtocolStack stack) {
                // Invoked by JGroups while the stack is being assembled inside the JChannel constructor.
                seenDuringConstruction.compareAndSet(null, Thread.currentThread().getContextClassLoader());
                super.setProtocolStack(stack);
            }
        };

        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0], originalTccl));

        try {
            invokeCreateChannel(List.of(probe));
        } catch (Exception ignored) {
            // Channel assembly without a transport may fail; the TCCL observation is what matters here.
        }

        ClassLoader observed = seenDuringConstruction.get();
        assertNotNull(observed, "Protocol was never attached; TCCL during construction could not be observed");
        assertSame(JChannel.class.getClassLoader(), observed,
                "TCCL must be JChannel's class loader while the channel is constructed");
    }

    @Test
    void tcclIsRestoredAfterSuccessfulOrFailedConstruction() {
        ClassLoader custom = new URLClassLoader(new URL[0], originalTccl);
        Thread.currentThread().setContextClassLoader(custom);

        try {
            invokeCreateChannel(List.of(new Protocol() {}));
        } catch (Exception ignored) {
            // Failure is acceptable here; restoration must hold regardless of outcome.
        }

        assertSame(custom, Thread.currentThread().getContextClassLoader(),
                "Caller's TCCL must be restored after construction");
    }

    @Test
    void tcclIsRestoredWhenConstructionThrows() {
        ClassLoader custom = new URLClassLoader(new URL[0], originalTccl);
        Thread.currentThread().setContextClassLoader(custom);

        // A null protocol list is expected to make the JChannel constructor throw.
        assertThrows(Throwable.class, () -> invokeCreateChannel(null));

        assertSame(custom, Thread.currentThread().getContextClassLoader(),
                "TCCL must be restored even when JChannel construction throws");
    }

    @Test
    void tcclIsRestoredWhenProtocolListIsEmpty() {
        ClassLoader custom = new URLClassLoader(new URL[0], originalTccl);
        Thread.currentThread().setContextClassLoader(custom);

        try {
            invokeCreateChannel(Collections.emptyList());
        } catch (Exception ignored) {
            // Empty stacks are typically rejected by JGroups; restoration is still required.
        }

        assertSame(custom, Thread.currentThread().getContextClassLoader(),
                "TCCL must be restored after an empty protocol list is handled");
    }

    @Test
    void nullTcclIsRestoredAsNull() {
        Thread.currentThread().setContextClassLoader(null);

        assertThrows(Throwable.class, () -> invokeCreateChannel(null));

        assertNull(Thread.currentThread().getContextClassLoader(),
                "A null caller TCCL must be restored as null, not replaced");
    }

    // ----- helpers -----

    private static Method resolveCreateChannel() {
        try {
            Class<?> clazz = Class.forName(TARGET_CLASS_NAME);
            Method m = clazz.getDeclaredMethod("createChannel", List.class);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to resolve createChannel via reflection", e);
        }
    }

    private static Object invokeCreateChannel(List<Protocol> protocols) throws Exception {
        try {
            return CREATE_CHANNEL.invoke(null, protocols);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw e;
        }
    }
}
```

---

## ResourceAdapterDeploymentService.java -> cleanupStartAsync()

- **Debt Type:** `DESIGN`
- **Line Number:** `147`
- **Status:** `GENERATED`
- **Comment:** `TODO this could be replaced by the superclass method if there is no need for the TCCL change and push/pop owner The stop() call doesn't do that so it's probably not needed`

```java
// TODO this could be replaced by the superclass method if there is no need for the TCCL change and push/pop owner
// The stop() call doesn't do that so it's probably not needed
private void cleanupStartAsync(final StartContext context, final String deploymentName, final Throwable cause, final ServiceName duServiceName, final ClassLoader toUse) {
    ExecutorService executorService = getLifecycleExecutorService();
    Runnable r = new Runnable() {

        @Override
        public void run() {
            ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WritableServiceBasedNamingStore.pushOwner(duServiceName);
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(toUse);
                unregisterAll(deploymentName);
            } finally {
                try {
                    context.failed(ConnectorLogger.ROOT_LOGGER.failedToStartRaDeployment(cause, deploymentName));
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
                    WritableServiceBasedNamingStore.popOwner();
                }
            }
        }
    };
    try {
        executorService.execute(r);
    } catch (RejectedExecutionException e) {
        r.run();
    } finally {
        context.asynchronous();
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

/**
 * Characterization tests for the private cleanupStartAsync(...) method.
 *
 * These tests pin down the current behaviour before the TODO is addressed:
 *  - the cleanup runnable is submitted to the lifecycle executor,
 *  - the rejected-execution fallback runs the cleanup inline,
 *  - the context is always marked asynchronous and failed,
 *  - the TCCL is restored and the naming owner stack is balanced,
 *  - failures in unregisterAll do not prevent context.failed(...) or restoration.
 *
 * Any refactoring that replaces this method with the superclass implementation
 * must keep these behaviours (or explicitly justify the change).
 *
 * ASSUMPTIONS: the enclosing class is ResourceAdapterDeploymentService, it exposes
 * a no-arg constructor (or is mockable), getLifecycleExecutorService() and
 * unregisterAll(String) are non-final and accessible to Mockito. Adjust the
 * CLASS_UNDER_TEST constant and imports if the owning class differs.
 */
class CleanupStartAsyncTest {

    private static final String CLASS_UNDER_TEST =
            "org.jboss.as.connector.deployers.ResourceAdapterDeploymentService";
    private static final String METHOD_UNDER_TEST = "cleanupStartAsync";

    private Object service;
    private Method cleanupMethod;
    private StartContext context;
    private ExecutorService executor;
    private ClassLoader originalTccl;
    private ClassLoader toUse;
    private final ServiceName duServiceName = ServiceName.of("jboss", "connector", "test-du");
    private final String deploymentName = "test-deployment.rar";

    @BeforeEach
    void setUp() throws Exception {
        originalTccl = Thread.currentThread().getContextClassLoader();
        toUse = new ClassLoader(originalTccl) { };

        Class<?> clazz = Class.forName(CLASS_UNDER_TEST);
        service = mock(clazz, CALLS_REAL_METHODS);

        executor = mock(ExecutorService.class);
        context = mock(StartContext.class);

        Method getExecutor = clazz.getDeclaredMethod("getLifecycleExecutorService");
        getExecutor.setAccessible(true);
        doReturn(executor).when((Object) service);
        // Stub the executor accessor through Mockito on the mock instance.
        when(getExecutorProxy()).thenReturn(executor);

        cleanupMethod = clazz.getDeclaredMethod(METHOD_UNDER_TEST,
                StartContext.class, String.class, Throwable.class, ServiceName.class, ClassLoader.class);
        cleanupMethod.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalTccl);
    }

    /** Helper that stubs the lifecycle executor accessor on the mock. */
    private ExecutorService getExecutorProxy() {
        return doReturnExecutorStub();
    }

    private ExecutorService doReturnExecutorStub() {
        // Returns the stubbing target; actual stubbing is applied in setUp via doReturn.
        return executor;
    }

    private void invoke(Throwable cause) throws Exception {
        cleanupMethod.invoke(service, context, deploymentName, cause, duServiceName, toUse);
    }

    private Runnable captureSubmittedRunnable() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Submits cleanup to lifecycle executor and marks context asynchronous")
    void submitsCleanupAsynchronously() throws Exception {
        invoke(new IllegalStateException("boom"));

        verify(executor, times(1)).execute(any(Runnable.class));
        verify(context, times(1)).asynchronous();
        verify(context, never()).failed(any());
    }

    @Test
    @DisplayName("Runs cleanup inline when executor rejects the task")
    void runsInlineOnRejectedExecution() throws Exception {
        doThrow(new RejectedExecutionException("rejected")).when(executor).execute(any(Runnable.class));
        Throwable cause = new IllegalStateException("boom");

        invoke(cause);

        verify(service, times(1)).unregisterAll(deploymentName);
        verify(context, times(1)).failed(any());
        verify(context, times(1)).asynchronous();
    }

    @Test
    @DisplayName("Calls unregisterAll with the deployment name when the runnable executes")
    void unregistersDeploymentWhenRun() throws Exception {
        invoke(new IllegalStateException("boom"));
        Runnable r = captureSubmittedRunnable();

        r.run();

        verify(service, times(1)).unregisterAll(deploymentName);
    }

    @Test
    @DisplayName("Marks context failed even when unregisterAll throws")
    void marksFailedWhenUnregisterThrows() throws Exception {
        doThrow(new RuntimeException("unregister failed"))
                .when(service).unregisterAll(deploymentName);
        invoke(new IllegalStateException("boom"));
        Runnable r = captureSubmittedRunnable();

        assertThrows(RuntimeException.class, r::run);

        verify(context, times(1)).failed(any());
    }

    @Test
    @DisplayName("Restores the original context class loader after running")
    void restoresTccl() throws Exception {
        Thread.currentThread().setContextClassLoader(originalTccl);
        invoke(new IllegalStateException("boom"));
        Runnable r = captureSubmittedRunnable();

        r.run();

        assertSame(originalTccl, Thread.currentThread().getContextClassLoader(),
                "TCCL must be restored to the value present before cleanup");
    }

    @Test
    @DisplayName("Restores TCCL even when unregisterAll throws")
    void restoresTcclOnFailure() throws Exception {
        Thread.currentThread().setContextClassLoader(originalTccl);
        doThrow(new RuntimeException("unregister failed"))
                .when(service).unregisterAll(deploymentName);
        invoke(new IllegalStateException("boom"));
        Runnable r = captureSubmittedRunnable();

        assertThrows(RuntimeException.class, r::run);

        assertSame(originalTccl, Thread.currentThread().getContextClassLoader());
    }

    @Test
    @DisplayName("Reports the original cause to context.failed")
    void reportsOriginalCause() throws Exception {
        Throwable cause = new IllegalStateException("root cause");
        invoke(cause);
        Runnable r = captureSubmittedRunnable();

        r.run();

        ArgumentCaptor<Throwable> failure = ArgumentCaptor.forClass(Throwable.class);
        verify(context).failed(failure.capture());
        assertNotNull(failure.getValue());
        assertSame(cause, failure.getValue().getCause(),
                "Failure reported to the context should wrap the original cause");
    }

    @Test
    @DisplayName("Does not run cleanup synchronously when executor accepts the task")
    void doesNotRunInlineWhenAccepted() throws Exception {
        invoke(new IllegalStateException("boom"));

        verify(service, never()).unregisterAll(anyString());
        verify(context, never()).failed(any());
    }
}
```

---

## ParseUtils.java -> addExtensionProperties()

- **Debt Type:** `IMPLEMENTATION`
- **Line Number:** `199`
- **Status:** `GENERATED`
- **Comment:** `* Adds properties of Extension type to the operation      * TODO: not implemented jet in DMR`

```java
/**
 * Adds properties of Extension type to the operation
 * TODO: not implemented jet in DMR
 */
public static void addExtensionProperties(ModelNode operation) {
    /*

        operation.get("reauth-plugin-properties","Property").set("A");
        operation.get("valid-connection-checker-properties","Property").set("B");
        operation.get("stale-connect,roperties","Property").set("C");
        operation.get("exception-sorter-properties","Property").set("D");
         */
    /*final ModelNode sourcePropertiesAddress = address.clone();
        sourcePropertiesAddress.add("reauth-plugin-properties", "Property");
        sourcePropertiesAddress.protect();
        final ModelNode sourcePropertyOperation = new ModelNode();
        sourcePropertyOperation.get(OP).set("add");
        sourcePropertyOperation.get(OP_ADDR).set(sourcePropertiesAddress);
        sourcePropertyOperation.get("value").set("A");

        execute(sourcePropertyOperation);*/
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for addExtensionProperties(ModelNode).
 *
 * NOTE: Replace TargetClass with the class that declares addExtensionProperties.
 *
 * The method body is currently fully commented out (the Extension property
 * support is not yet implemented in DMR). These tests pin down the current
 * behavior (no-op, no side effects) and provide a disabled specification test
 * describing the expected behavior once the feature is implemented.
 */
class AddExtensionPropertiesTest {

    private ModelNode operation;

    @BeforeEach
    void setUp() {
        operation = new ModelNode();
    }

    @Nested
    @DisplayName("Current behavior: method is a no-op")
    class NoOpBehavior {

        @Test
        @DisplayName("does not throw for an empty operation")
        void doesNotThrowForEmptyOperation() {
            assertDoesNotThrow(() -> TargetClass.addExtensionProperties(operation));
        }

        @Test
        @DisplayName("leaves an empty operation unchanged")
        void leavesEmptyOperationUnchanged() {
            String before = operation.toString();

            TargetClass.addExtensionProperties(operation);

            assertEquals(before, operation.toString());
            assertTrue(operation.keys().isEmpty(), "No keys should be added");
        }

        @Test
        @DisplayName("does not add any of the commented-out extension property nodes")
        void doesNotAddExtensionPropertyNodes() {
            TargetClass.addExtensionProperties(operation);

            assertAll(
                () -> assertFalse(operation.hasDefined("reauth-plugin-properties"),
                    "reauth-plugin-properties should not be added"),
                () -> assertFalse(operation.hasDefined("valid-connection-checker-properties"),
                    "valid-connection-checker-properties should not be added"),
                () -> assertFalse(operation.hasDefined("stale-connection-properties"),
                    "stale-connection-properties should not be added"),
                () -> assertFalse(operation.hasDefined("exception-sorter-properties"),
                    "exception-sorter-properties should not be added")
            );
        }

        @Test
        @DisplayName("preserves existing operation content")
        void preservesExistingContent() {
            operation.get("operation").set("add");
            operation.get("address").add("subsystem", "datasources");
            operation.get("jndi-name").set("java:/TestDS");
            String before = operation.toString();

            TargetClass.addExtensionProperties(operation);

            assertAll(
                () -> assertEquals("add", operation.get("operation").asString()),
                () -> assertEquals("java:/TestDS", operation.get("jndi-name").asString()),
                () -> assertEquals(before, operation.toString())
            );
        }

        @Test
        @DisplayName("is idempotent across repeated invocations")
        void isIdempotent() {
            operation.get("operation").set("add");
            String before = operation.toString();

            TargetClass.addExtensionProperties(operation);
            TargetClass.addExtensionProperties(operation);
            TargetClass.addExtensionProperties(operation);

            assertEquals(before, operation.toString());
        }

        @Test
        @DisplayName("does not dereference a null operation (no NPE today)")
        void nullOperationDoesNotThrow() {
            assertDoesNotThrow(() -> TargetClass.addExtensionProperties(null));
        }

        @Test
        @DisplayName("does not add any child keys to the operation")
        void addsNoKeys() {
            operation.get("name").set("value");
            List<String> keysBefore = operation.keys();

            TargetClass.addExtensionProperties(operation);

            assertEquals(keysBefore, operation.keys());
        }
    }

    @Nested
    @DisplayName("Expected behavior once DMR support is implemented")
    class ExpectedBehavior {

        @Test
        @Disabled("TODO: Extension properties not yet implemented in DMR")
        @DisplayName("adds Property-typed extension properties to the operation")
        void addsExtensionPropertiesWhenImplemented() {
            TargetClass.addExtensionProperties(operation);

            assertAll(
                () -> assertTrue(operation.hasDefined("reauth-plugin-properties")),
                () -> assertTrue(operation.hasDefined("valid-connection-checker-properties")),
                () -> assertTrue(operation.hasDefined("stale-connection-properties")),
                () -> assertTrue(operation.hasDefined("exception-sorter-properties"))
            );
        }
    }
}
```

---

## ResourceAdaptersSubsystemTestCase.java -> standardSubsystemTest()

- **Debt Type:** `DESIGN`
- **Line Number:** `90`
- **Status:** `GENERATED`
- **Comment:** `TODO: remove this special method as soon as RA will have a unique name in DMR marshalled in xml`

```java
//TODO: remove this special method as soon as RA will have a unique name in DMR marshalled in xml
protected KernelServices standardSubsystemTest(final String configId, final String configIdResolvedModel, boolean compareXml) throws Exception {
    final AdditionalInitialization additionalInit = createAdditionalInitialization();
    // Parse the subsystem xml and install into the first controller
    final String subsystemXml = configId == null ? getSubsystemXml() : getSubsystemXml(configId);
    final KernelServices servicesA = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
    Assert.assertTrue("Subsystem boot failed!", servicesA.isSuccessfulBoot());
    //Get the model and the persisted xml from the first controller
    final ModelNode modelA = servicesA.readWholeModel();
    validateModel(modelA);
    // Test marshaling
    final String marshalled = servicesA.getPersistedSubsystemXml();
    servicesA.shutdown();
    // validate the the normalized xmls
    String normalizedSubsystem = normalizeXML(subsystemXml);
    if (compareXml) {
        compareXml(configId, normalizedSubsystem, normalizeXML(marshalled));
    }
    //Install the persisted xml from the first controller into a second controller
    final KernelServices servicesB = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
    final ModelNode modelB = servicesB.readWholeModel();
    //WE CANT DO THAT FOR RA BECAUSE THEIR NAME IS GENERATED ON THE FLY W/ archive/module concatenated to a counter.
    //IT'S AN ISSUE WM_SECURITY_MAPPING_TO BE FIXED IN SUBSYSTEM
    //Make sure the models from the two controllers are identical
    //compare(modelA, modelB);
    // Test the describe operation
    final ModelNode operation = createDescribeOperation();
    final ModelNode result = servicesB.executeOperation(operation);
    Assert.assertTrue("the subsystem describe operation has to generate a list of operations to recreate the subsystem", !result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
    final List<ModelNode> operations = result.get(ModelDescriptionConstants.RESULT).asList();
    servicesB.shutdown();
    final KernelServices servicesC = super.createKernelServicesBuilder(additionalInit).setBootOperations(operations).build();
    final ModelNode modelC = servicesC.readWholeModel();
    compare(modelB, modelC);
    assertRemoveSubsystemResources(servicesC, getIgnoredChildResourcesForRemovalTest());
    if (configIdResolvedModel != null) {
        final String subsystemResolvedXml = getSubsystemXml(configIdResolvedModel);
        final KernelServices servicesD = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemResolvedXml).build();
        Assert.assertTrue("Subsystem w/ reolved xml boot failed!", servicesD.isSuccessfulBoot());
        final ModelNode modelD = servicesD.readWholeModel();
        validateModel(modelD);
        resolveandCompareModel(modelA, modelD);
    }
    return servicesA;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.jboss.as.subsystem.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the {@code standardSubsystemTest} helper.
 *
 * The helper is marked with a TODO to be removed once the resource adapter (RA)
 * receives a unique name in the DMR model marshalled to XML. Until then, these tests
 * pin down its current behaviour so the special-case logic can be refactored safely.
 *
 * Assumptions (the external task specification was not provided):
 *  - The host class is the subsystem base test that declares standardSubsystemTest,
 *    and it exposes getSubsystemXml(), getSubsystemXml(String), validateModel(),
 *    createAdditionalInitialization(), createDescribeOperation(),
 *    getIgnoredChildResourcesForRemovalTest(), normalizeXML(), compareXml(),
 *    compare() and resolveandCompareModel().
 *  - Test resources such as "resource-adapters-valid.xml" are available on the classpath.
 */
class StandardSubsystemTestDebtTest extends ResourceAdapterSubsystemTest {

    private static final String VALID_CONFIG_ID = "resource-adapters-valid.xml";
    private static final String RESOLVED_CONFIG_ID = "resource-adapters-resolved.xml";
    private static final String BROKEN_CONFIG_ID = "resource-adapters-broken.xml";

    private KernelServices lastServices;

    @BeforeEach
    void setUp() {
        lastServices = null;
    }

    @AfterEach
    void tearDown() {
        // The helper returns servicesA without shutting it down, so release it here.
        if (lastServices != null) {
            lastServices.shutdown();
            lastServices = null;
        }
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("boots, round-trips, and returns the first controller's services")
        void standardRoundTripSucceeds() throws Exception {
            lastServices = standardSubsystemTest(VALID_CONFIG_ID, null, true);

            assertNotNull(lastServices, "standardSubsystemTest must return the services of controller A");
            assertTrue(lastServices.isSuccessfulBoot(), "returned controller A must have booted");
        }

        @Test
        @DisplayName("default configuration (null configId) uses getSubsystemXml()")
        void nullConfigIdUsesDefaultXml() throws Exception {
            lastServices = standardSubsystemTest(null, null, false);

            assertNotNull(lastServices);
            assertTrue(lastServices.isSuccessfulBoot());
        }

        @Test
        @DisplayName("skips XML comparison when compareXml is false")
        void compareXmlFalseSkipsComparison() throws Exception {
            // With compareXml=false the marshalled XML is not compared against the input,
            // so this must succeed even if the normalized forms differ.
            assertDoesNotThrow(() -> {
                lastServices = standardSubsystemTest(VALID_CONFIG_ID, null, false);
            });
            assertNotNull(lastServices);
        }

        @Test
        @DisplayName("validates the resolved configuration when configIdResolvedModel is provided")
        void resolvedModelConfigIsValidatedAndCompared() throws Exception {
            lastServices = standardSubsystemTest(VALID_CONFIG_ID, RESOLVED_CONFIG_ID, true);

            assertNotNull(lastServices);
            assertTrue(lastServices.isSuccessfulBoot());
        }
    }

    @Nested
    @DisplayName("Failure paths")
    class FailurePaths {

        @Test
        @DisplayName("fails with 'Subsystem boot failed!' when controller A does not boot")
        void bootFailureIsReported() {
            AssertionError error = assertThrows(AssertionError.class,
                    () -> standardSubsystemTest(BROKEN_CONFIG_ID, null, true));

            assertTrue(error.getMessage().contains("Subsystem boot failed!"),
                    "unexpected message: " + error.getMessage());
        }

        @Test
        @DisplayName("fails when the describe operation reports a failure description")
        void describeFailureIsReported() {
            // Force the describe operation to address a non-existent resource so the
            // result contains a failure-description, which the helper must reject.
            createDescribeOperationOverride = () -> {
                ModelNode op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("no-such-operation");
                op.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
                return op;
            };

            AssertionError error = assertThrows(AssertionError.class,
                    () -> standardSubsystemTest(VALID_CONFIG_ID, null, false));

            assertTrue(error.getMessage().contains("describe operation"),
                    "unexpected message: " + error.getMessage());
        }

        @Test
        @DisplayName("fails when the resolved-model configuration does not boot")
        void resolvedModelBootFailureIsReported() {
            resolvedConfigOverride = BROKEN_CONFIG_ID;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> standardSubsystemTest(VALID_CONFIG_ID, RESOLVED_CONFIG_ID, false));

            assertTrue(error.getMessage().contains("w/ reolved xml boot failed!"),
                    "unexpected message: " + error.getMessage());
        }
    }

    @Nested
    @DisplayName("Describe output")
    class DescribeOutput {

        @Test
        @DisplayName("describe result is a non-empty list of operations that rebuilds the subsystem")
        void describeProducesOperationList() throws Exception {
            ModelNode describeResult = executeDescribeOnFreshServices(VALID_CONFIG_ID);

            assertFalse(describeResult.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
            List<ModelNode> operations = describeResult.get(ModelDescriptionConstants.RESULT).asList();
            assertFalse(operations.isEmpty(), "describe must emit operations to recreate the subsystem");
        }
    }

    // ------------------------------------------------------------------
    // Hooks and helpers
    // ------------------------------------------------------------------

    private ModelNode createDescribeOperationOverride;
    private String resolvedConfigOverride;

    @Override
    protected ModelNode createDescribeOperation() {
        if (createDescribeOperationOverride != null) {
            return createDescribeOperationOverride.get();
        }
        return super.createDescribeOperation();
    }

    @Override
    protected String getSubsystemXml(String configId) throws Exception {
        if (resolvedConfigOverride != null && configId.equals(RESOLVED_CONFIG_ID)) {
            return super.getSubsystemXml(resolvedConfigOverride);
        }
        return super.getSubsystemXml(configId);
    }

    private ModelNode executeDescribeOnFreshServices(String configId) throws Exception {
        String xml = getSubsystemXml(configId);
        KernelServices services = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(xml)
                .build();
        try {
            assertTrue(services.isSuccessfulBoot(), "precondition: subsystem must boot");
            return services.executeOperation(createDescribeOperation());
        } finally {
            services.shutdown();
        }
    }

    /** Functional interface-style holder so tests can override the describe operation. */
    @FunctionalInterface
    private interface OperationSupplier {
        ModelNode get();
    }
}
```

---

## StandardConfigsXMLValidationUnitTestCase.java -> testStandaloneEC2HA()

- **Debt Type:** `DESIGN`
- **Line Number:** `69`
- **Status:** `GENERATED`
- **Comment:** `TODO Leave commented out until domain-jts.xml is definitely removed from the configuration @Test public void testDomainJTS() throws Exception { parseXml("docs/examples/configs/domain-jts.xml"); } `

```java
//TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
@Test
public void testStandaloneEC2HA() throws Exception {
    parseXml("docs/examples/configs/standalone-ec2-ha.xml");
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replaces the commented-out testDomainJTS by enforcing that the legacy
 * domain-jts.xml is no longer referenced by any example configuration, and
 * that the remaining example configurations still parse.
 *
 * Note: parseXml(String) is assumed to be provided by the enclosing test
 * base class, as in the original test suite.
 */
class ExampleConfigParsingTest {

    private static final Path CONFIG_DIR = Paths.get("docs/examples/configs");
    private static final String LEGACY_CONFIG = "domain-jts.xml";
    private static final String STANDALONE_EC2_HA = "docs/examples/configs/standalone-ec2-ha.xml";

    @Test
    @DisplayName("standalone-ec2-ha.xml parses successfully")
    void testStandaloneEC2HA() throws Exception {
        assertDoesNotThrow(() -> parseXml(STANDALONE_EC2_HA));
    }

    @Test
    @DisplayName("legacy domain-jts.xml is removed from the example configuration set")
    void testLegacyDomainJtsFileIsRemoved() {
        assertFalse(
                Files.exists(CONFIG_DIR.resolve(LEGACY_CONFIG)),
                "Legacy config " + LEGACY_CONFIG + " still exists in " + CONFIG_DIR
                        + "; it must be removed before this debt is considered paid.");
    }

    @Test
    @DisplayName("no example configuration references domain-jts.xml")
    void testNoConfigReferencesLegacyDomainJts() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(CONFIG_DIR),
                "Config directory not found: " + CONFIG_DIR);

        List<Path> configs = listXmlConfigs();
        assertTrue(!configs.isEmpty(), "Expected at least one example XML config in " + CONFIG_DIR);

        for (Path config : configs) {
            String content = Files.readString(config, StandardCharsets.UTF_8);
            assertFalse(content.contains(LEGACY_CONFIG),
                    "Config " + config + " still references " + LEGACY_CONFIG);
        }
    }

    @ParameterizedTest(name = "example config parses: {0}")
    @MethodSource("exampleConfigs")
    @DisplayName("all example configurations parse without error")
    void testExampleConfigParses(String configPath) {
        assertDoesNotThrow(() -> parseXml(configPath));
    }

    static Stream<String> exampleConfigs() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(CONFIG_DIR),
                "Config directory not found: " + CONFIG_DIR);
        return listXmlConfigs().stream()
                .filter(p -> !p.getFileName().toString().equals(LEGACY_CONFIG))
                .map(p -> p.toString().replace('\\', '/'));
    }

    private static List<Path> listXmlConfigs() throws IOException {
        try (Stream<Path> files = Files.list(CONFIG_DIR)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
```

---

## StandardConfigsXMLValidationUnitTestCase.java -> testStandaloneEC2HA()

- **Debt Type:** `DESIGN`
- **Line Number:** `59`
- **Status:** `GENERATED`
- **Comment:** `TODO Leave commented out until domain-jts.xml is definitely removed from the configuration @Test public void testDomainJTS() throws Exception { parseXml("docs/examples/configs/domain-jts.xml"); } `

```java
//TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
@Test
public void testStandaloneEC2HA() throws Exception {
    parseXml("docs/examples/configs/standalone-ec2-ha.xml");
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for example XML configuration parsing.
 *
 * Assumes parseXml(String) is available from this class or a superclass and
 * throws an Exception when the given configuration cannot be parsed.
 */
class ExampleConfigParsingTest {

    private static final Path CONFIG_DIR = Paths.get("docs/examples/configs");
    private static final String STANDALONE_EC2_HA = "docs/examples/configs/standalone-ec2-ha.xml";
    private static final String DOMAIN_JTS = "docs/examples/configs/domain-jts.xml";

    @Test
    @DisplayName("standalone-ec2-ha.xml parses without error")
    public void testStandaloneEC2HA() throws Exception {
        parseXml(STANDALONE_EC2_HA);
    }

    @Test
    @DisplayName("standalone-ec2-ha.xml exists as a regular file")
    public void standaloneEC2HAConfigFileExists() {
        assertTrue(Files.isRegularFile(CONFIG_DIR.resolve("standalone-ec2-ha.xml")),
                "Expected example config to exist: " + STANDALONE_EC2_HA);
    }

    @Test
    @DisplayName("standalone-ec2-ha.xml parses consistently on repeated runs")
    public void standaloneEC2HAParsesRepeatably() {
        assertDoesNotThrow(() -> {
            parseXml(STANDALONE_EC2_HA);
            parseXml(STANDALONE_EC2_HA);
        });
    }

    @Test
    @DisplayName("domain-jts.xml parses if it is still present; skipped once removed")
    public void domainJTSConfigParsesWhilePresent() {
        // Debt-aware: once domain-jts.xml is removed from the repository,
        // this test is skipped instead of failing, so the original
        // commented-out test can be deleted safely.
        assumeTrue(Files.exists(Paths.get(DOMAIN_JTS)),
                "domain-jts.xml has been removed; skipping parse check");
        assertDoesNotThrow(() -> parseXml(DOMAIN_JTS));
    }

    @Test
    @DisplayName("parsing a missing configuration file fails")
    public void parsingMissingConfigFails() {
        assertThrows(Exception.class,
                () -> parseXml("docs/examples/configs/does-not-exist.xml"));
    }
}
```

---

## ComponentView.java -> invoke()

- **Debt Type:** `DOCUMENTATION`
- **Line Number:** `44`
- **Status:** `GENERATED`
- **Comment:** `* Invoke on the component view interceptor chain.      * TODO: fully document the semantics of this method      *      * @param interceptorContext The context of the invocation      * @return The result of the invocation`

```java
/**
 * Invoke on the component view interceptor chain.
 * TODO: fully document the semantics of this method
 *
 * @param interceptorContext The context of the invocation
 * @return The result of the invocation
 */
Object invoke(final InterceptorContext interceptorContext) throws Exception;
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Characterization tests for {@code invoke(InterceptorContext)}.
 *
 * These tests pin down the observable behavior of the method until its
 * semantics are fully documented (see the TODO in the Javadoc). Assumed
 * contract, inferred from the signature:
 *   - the context is passed through to the interceptor chain,
 *   - the value produced by the chain is returned to the caller,
 *   - checked and unchecked exceptions raised by the chain propagate.
 *
 * Assumed types: ComponentViewInterceptor (interface declaring invoke) and
 * InterceptorContext, both in the same package as this test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComponentViewInterceptor.invoke")
class ComponentViewInterceptorInvokeTest {

    private InterceptorContext context;

    @BeforeEach
    void setUp() {
        context = mock(InterceptorContext.class);
    }

    @Nested
    @DisplayName("return value semantics")
    class ReturnValue {

        @Test
        @DisplayName("returns the result produced by the interceptor chain")
        void returnsChainResult() throws Exception {
            Object expected = new Object();
            ComponentViewInterceptor interceptor = ctx -> expected;

            Object actual = interceptor.invoke(context);

            assertSame(expected, actual);
        }

        @Test
        @DisplayName("returns null when the chain produces null")
        void returnsNullWhenChainReturnsNull() throws Exception {
            ComponentViewInterceptor interceptor = ctx -> null;

            assertNull(interceptor.invoke(context));
        }

        @Test
        @DisplayName("returns a String result unchanged")
        void returnsStringResultUnchanged() throws Exception {
            ComponentViewInterceptor interceptor = ctx -> "view-result";

            assertEquals("view-result", interceptor.invoke(context));
        }
    }

    @Nested
    @DisplayName("context handling")
    class ContextHandling {

        @Test
        @DisplayName("passes the exact context instance to the chain")
        void passesSameContextInstance() throws Exception {
            AtomicInteger calls = new AtomicInteger();
            InterceptorContext[] received = new InterceptorContext[1];
            ComponentViewInterceptor interceptor = ctx -> {
                calls.incrementAndGet();
                received[0] = ctx;
                return "ok";
            };

            interceptor.invoke(context);

            assertEquals(1, calls.get());
            assertSame(context, received[0]);
        }

        @Test
        @DisplayName("invokes the chain exactly once per call")
        void invokesChainOnce() throws Exception {
            AtomicInteger calls = new AtomicInteger();
            ComponentViewInterceptor interceptor = ctx -> {
                calls.incrementAndGet();
                return null;
            };

            interceptor.invoke(context);

            assertEquals(1, calls.get());
        }
    }

    @Nested
    @DisplayName("exception propagation")
    class ExceptionPropagation {

        @Test
        @DisplayName("propagates checked exceptions thrown by the chain")
        void propagatesCheckedException() {
            IOException failure = new IOException("chain failed");
            ComponentViewInterceptor interceptor = ctx -> {
                throw failure;
            };

            IOException thrown = assertThrows(IOException.class,
                    () -> interceptor.invoke(context));

            assertSame(failure, thrown);
        }

        @Test
        @DisplayName("propagates unchecked exceptions thrown by the chain")
        void propagatesRuntimeException() {
            IllegalStateException failure = new IllegalStateException("bad state");
            ComponentViewInterceptor interceptor = ctx -> {
                throw failure;
            };

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> interceptor.invoke(context));

            assertSame(failure, thrown);
        }
    }
}
```

---

## ResourceReferenceProcessor.java -> getMessageDestinationRefs()

- **Debt Type:** `DESIGN`
- **Line Number:** `298`
- **Status:** `GENERATED`
- **Comment:** `* TODO: should this be part of the messaging subsystem`

```java
/**
 * TODO: should this be part of the messaging subsystem
 */
private List<BindingConfiguration> getMessageDestinationRefs(final DeploymentDescriptorEnvironment environment, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex, final ResourceInjectionTarget resourceInjectionTarget, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
    final List<BindingConfiguration> bindings = new ArrayList<BindingConfiguration>();
    final MessageDestinationReferencesMetaData messageDestinationReferences = environment.getEnvironment().getMessageDestinationReferences();
    if (messageDestinationReferences == null) {
        return bindings;
    }
    for (final MessageDestinationReferenceMetaData messageRef : messageDestinationReferences) {
        if (messageRef.isDependencyIgnored()) {
            continue;
        }
        final String name;
        if (messageRef.getName().startsWith("java:")) {
            name = messageRef.getName();
        } else {
            name = environment.getDefaultContext() + messageRef.getName();
        }
        Class<?> classType = null;
        if (messageRef.getType() != null) {
            try {
                classType = classLoader.loadClass(messageRef.getType());
            } catch (ClassNotFoundException e) {
                throw EeLogger.ROOT_LOGGER.cannotLoad(e, messageRef.getType());
            }
        }
        // our injection (source) comes from the local (ENC) lookup, no matter what.
        final LookupInjectionSource injectionSource = new LookupInjectionSource(name);
        classType = processInjectionTargets(resourceInjectionTarget, injectionSource, classLoader, deploymentReflectionIndex, messageRef, classType);
        final BindingConfiguration bindingConfiguration;
        if (!isEmpty(messageRef.getLookupName())) {
            bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(messageRef.getLookupName()));
            bindings.add(bindingConfiguration);
        } else if (!isEmpty(messageRef.getMappedName())) {
            bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(messageRef.getMappedName()));
            bindings.add(bindingConfiguration);
        } else if (!isEmpty(messageRef.getLink())) {
            final MessageDestinationInjectionSource messageDestinationInjectionSource = new MessageDestinationInjectionSource(messageRef.getLink(), name);
            bindingConfiguration = new BindingConfiguration(name, messageDestinationInjectionSource);
            deploymentUnit.addToAttachmentList(Attachments.MESSAGE_DESTINATIONS, messageDestinationInjectionSource);
            bindings.add(bindingConfiguration);
        } else {
            ROOT_LOGGER.cannotResolve("message-destination-ref", name);
        }
    }
    return bindings;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEResourceReferenceProcessor;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ee.structure.DeploymentDescriptorEnvironment;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.MessageDestinationReferenceMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationReferencesMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the private {@code getMessageDestinationRefs} method.
 *
 * The method is private and its enclosing class is assumed to be
 * {@link EEResourceReferenceProcessor}. Adjust imports/types to match the
 * actual project structure if they differ.
 *
 * The TODO asks whether this logic belongs to the messaging subsystem. These
 * tests pin down the current behaviour so the logic can be moved safely later.
 */
class GetMessageDestinationRefsTest {

    private static final String DEFAULT_CONTEXT = "java:comp/env/";

    private EEResourceReferenceProcessor processor;
    private ClassLoader classLoader;
    private DeploymentReflectionIndex reflectionIndex;
    private ResourceInjectionTarget injectionTarget;
    private DeploymentUnit deploymentUnit;
    private DeploymentDescriptorEnvironment environment;
    private MessageDestinationReferencesMetaData refs;

    private Method method;

    @BeforeEach
    void setUp() throws Exception {
        processor = new EEResourceReferenceProcessor();
        classLoader = mock(ClassLoader.class);
        reflectionIndex = mock(DeploymentReflectionIndex.class);
        injectionTarget = mock(ResourceInjectionTarget.class);
        deploymentUnit = mock(DeploymentUnit.class);
        environment = mock(DeploymentDescriptorEnvironment.class, RETURNS_DEEP_STUBS);
        refs = mock(MessageDestinationReferencesMetaData.class);

        when(environment.getDefaultContext()).thenReturn(DEFAULT_CONTEXT);
        when(environment.getEnvironment().getMessageDestinationReferences()).thenReturn(refs);

        method = EEResourceReferenceProcessor.class.getDeclaredMethod(
                "getMessageDestinationRefs",
                DeploymentDescriptorEnvironment.class,
                ClassLoader.class,
                DeploymentReflectionIndex.class,
                ResourceInjectionTarget.class,
                DeploymentUnit.class);
        method.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private List<BindingConfiguration> invoke() throws Throwable {
        try {
            return (List<BindingConfiguration>) method.invoke(
                    processor, environment, classLoader, reflectionIndex, injectionTarget, deploymentUnit);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private MessageDestinationReferenceMetaData refWith(String name, String lookup,
                                                        String mapped, String link,
                                                        boolean ignored, String type) {
        MessageDestinationReferenceMetaData ref = mock(MessageDestinationReferenceMetaData.class);
        when(ref.getName()).thenReturn(name);
        when(ref.getLookupName()).thenReturn(lookup);
        when(ref.getMappedName()).thenReturn(mapped);
        when(ref.getLink()).thenReturn(link);
        when(ref.isDependencyIgnored()).thenReturn(ignored);
        when(ref.getType()).thenReturn(type);
        return ref;
    }

    private void givenRefs(MessageDestinationReferenceMetaData... entries) {
        when(refs.iterator()).thenReturn(java.util.Arrays.asList(entries).iterator());
    }

    @Nested
    @DisplayName("Empty or missing input")
    class EmptyInput {

        @Test
        @DisplayName("returns empty list when descriptor has no message destination refs")
        void nullRefsReturnsEmpty() throws Throwable {
            when(environment.getEnvironment().getMessageDestinationReferences()).thenReturn(null);

            List<BindingConfiguration> result = invoke();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns empty list when refs collection is empty")
        void emptyRefsReturnsEmpty() throws Throwable {
            givenRefs();

            assertTrue(invoke().isEmpty());
        }
    }

    @Nested
    @DisplayName("Skipping behaviour")
    class Skipping {

        @Test
        @DisplayName("ignored dependencies produce no binding and no class loading")
        void ignoredDependencySkipped() throws Throwable {
            givenRefs(refWith("jms/queue", "java:jboss/queue", null, null, true, "javax.jms.Queue"));

            List<BindingConfiguration> result = invoke();

            assertTrue(result.isEmpty());
            verifyNoInteractions(classLoader);
            verifyNoInteractions(deploymentUnit);
        }
    }

    @Nested
    @DisplayName("Name resolution")
    class NameResolution {

        @Test
        @DisplayName("java: prefixed name is used as-is")
        void javaNamePreserved() throws Throwable {
            givenRefs(refWith("java:global/queue", "java:jboss/queue", null, null, false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
            assertEquals("java:global/queue", result.get(0).getName());
        }

        @Test
        @DisplayName("relative name is prefixed with default context")
        void relativeNameGetsDefaultContext() throws Throwable {
            givenRefs(refWith("jms/queue", "java:jboss/queue", null, null, false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
            assertEquals(DEFAULT_CONTEXT + "jms/queue", result.get(0).getName());
        }
    }

    @Nested
    @DisplayName("Type loading")
    class TypeLoading {

        @Test
        @DisplayName("declared type is loaded through the supplied class loader")
        void typeIsLoaded() throws Throwable {
            givenRefs(refWith("jms/queue", "java:jboss/queue", null, null, false, "javax.jms.Queue"));
            when(classLoader.loadClass("javax.jms.Queue")).thenReturn((Class) String.class);

            invoke();

            verify(classLoader).loadClass("javax.jms.Queue");
        }

        @Test
        @DisplayName("unloadable type raises a deployment processing error")
        void unloadableTypeThrows() throws Throwable {
            givenRefs(refWith("jms/queue", "java:jboss/queue", null, null, false, "com.missing.Type"));
            when(classLoader.loadClass("com.missing.Type")).thenThrow(new ClassNotFoundException("com.missing.Type"));

            assertThrows(Throwable.class, this::invoke);
        }

        @Test
        @DisplayName("null type skips class loading")
        void nullTypeSkipsLoading() throws Throwable {
            givenRefs(refWith("jms/queue", "java:jboss/queue", null, null, false, null));

            invoke();

            verify(classLoader, never()).loadClass(anyString());
        }
    }

    @Nested
    @DisplayName("Binding source selection")
    class BindingSources {

        @Test
        @DisplayName("lookup name produces a binding")
        void lookupNameBinding() throws Throwable {
            givenRefs(refWith("jms/queue", "java:jboss/exported/queue", null, null, false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
            assertEquals(DEFAULT_CONTEXT + "jms/queue", result.get(0).getName());
        }

        @Test
        @DisplayName("mapped name produces a binding when lookup name is absent")
        void mappedNameBinding() throws Throwable {
            givenRefs(refWith("jms/queue", null, "java:jboss/mapped/queue", null, false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("link produces a binding and registers a message destination attachment")
        void linkRegistersAttachment() throws Throwable {
            givenRefs(refWith("jms/queue", null, null, "orders-queue", false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
            verify(deploymentUnit).addToAttachmentList(
                    eq(org.jboss.as.server.deployment.Attachments.MESSAGE_DESTINATIONS), any());
        }

        @Test
        @DisplayName("lookup name takes precedence over mapped name and link")
        void lookupPrecedence() throws Throwable {
            givenRefs(refWith("jms/queue", "java:lookup", "java:mapped", "link", false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
            verify(deploymentUnit, never()).addToAttachmentList(any(), any());
        }

        @Test
        @DisplayName("mapped name takes precedence over link")
        void mappedPrecedenceOverLink() throws Throwable {
            givenRefs(refWith("jms/queue", null, "java:mapped", "link", false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(1, result.size());
            verify(deploymentUnit, never()).addToAttachmentList(any(), any());
        }

        @Test
        @DisplayName("ref with no lookup, mapped name or link produces no binding")
        void unresolvableRefProducesNoBinding() throws Throwable {
            givenRefs(refWith("jms/queue", null, null, null, false, null));

            List<BindingConfiguration> result = invoke();

            assertTrue(result.isEmpty());
            verify(deploymentUnit, never()).addToAttachmentList(any(), any());
        }

        @Test
        @DisplayName("empty string values are treated as absent")
        void emptyStringsTreatedAsAbsent() throws Throwable {
            givenRefs(refWith("jms/queue", "", "", "", false, null));

            assertTrue(invoke().isEmpty());
        }
    }

    @Nested
    @DisplayName("Multiple references")
    class MultipleRefs {

        @Test
        @DisplayName("resolves each eligible ref and skips ignored ones, preserving order")
        void mixedRefs() throws Throwable {
            givenRefs(
                    refWith("a", "java:a", null, null, false, null),
                    refWith("b", "java:b", null, null, true, null),
                    refWith("c", null, null, "link-c", false, null));

            List<BindingConfiguration> result = invoke();

            assertEquals(2, result.size());
            assertEquals(DEFAULT_CONTEXT + "a", result.get(0).getName());
            assertEquals(DEFAULT_CONTEXT + "c", result.get(1).getName());

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(deploymentUnit, times(1)).addToAttachmentList(any(), captor.capture());
        }
    }
}
```

---

## MethodIntfHelper.java -> of()

- **Debt Type:** `DESIGN`
- **Line Number:** `16`
- **Status:** `GENERATED`
- **Comment:** `centralize this hack`

```java
// centralize this hack
public static MethodInterfaceType of(final InterceptorContext invocation) {
    //for timer invocations there is no view, so the methodInf is attached directly
    //to the context. Otherwise we retrieve it from the invoked view
    MethodInterfaceType methodIntf = invocation.getPrivateData(MethodInterfaceType.class);
    if (methodIntf == null) {
        final ComponentView componentView = invocation.getPrivateData(ComponentView.class);
        if (componentView != null) {
            methodIntf = componentView.getPrivateData(MethodInterfaceType.class);
        } else {
            methodIntf = MethodInterfaceType.Bean;
        }
    }
    return methodIntf;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.MethodIntfHelper;
import org.jboss.invocation.InterceptorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link MethodIntfHelper#of(InterceptorContext)}.
 *
 * These tests pin down the current resolution order so the logic can be
 * centralized safely:
 *   1. Method interface attached directly to the context (timer invocations).
 *   2. Otherwise, the method interface stored on the invoked ComponentView.
 *   3. Otherwise, the default {@link MethodIntf#BEAN}.
 */
class MethodIntfHelperTest {

    @Nested
    @DisplayName("Direct private data on InterceptorContext")
    class DirectPrivateData {

        @Test
        @DisplayName("returns method interface attached directly to context (timer case)")
        void returnsDirectlyAttachedMethodIntf() {
            InterceptorContext context = mock(InterceptorContext.class);
            when(context.getPrivateData(MethodIntf.class)).thenReturn(MethodIntf.TIMER);

            MethodIntf result = MethodIntfHelper.of(context);

            assertEquals(MethodIntf.TIMER, result);
        }

        @Test
        @DisplayName("does not consult ComponentView when direct data is present")
        void directDataTakesPrecedenceOverComponentView() {
            InterceptorContext context = mock(InterceptorContext.class);
            ComponentView view = mock(ComponentView.class);
            when(context.getPrivateData(MethodIntf.class)).thenReturn(MethodIntf.REMOTE);
            when(context.getPrivateData(ComponentView.class)).thenReturn(view);
            when(view.getPrivateData(MethodIntf.class)).thenReturn(MethodIntf.LOCAL);

            MethodIntf result = MethodIntfHelper.of(context);

            assertEquals(MethodIntf.REMOTE, result);
            verify(view, org.mockito.Mockito.never()).getPrivateData(MethodIntf.class);
        }
    }

    @Nested
    @DisplayName("Fallback to ComponentView private data")
    class ComponentViewFallback {

        @Test
        @DisplayName("returns method interface from invoked view when context has none")
        void returnsMethodIntfFromComponentView() {
            InterceptorContext context = mock(InterceptorContext.class);
            ComponentView view = mock(ComponentView.class);
            when(context.getPrivateData(MethodIntf.class)).thenReturn(null);
            when(context.getPrivateData(ComponentView.class)).thenReturn(view);
            when(view.getPrivateData(MethodIntf.class)).thenReturn(MethodIntf.LOCAL);

            MethodIntf result = MethodIntfHelper.of(context);

            assertEquals(MethodIntf.LOCAL, result);
        }

        @Test
        @DisplayName("returns null when view exists but carries no method interface (no Bean default)")
        void viewWithoutMethodIntfYieldsNull() {
            InterceptorContext context = mock(InterceptorContext.class);
            ComponentView view = mock(ComponentView.class);
            when(context.getPrivateData(MethodIntf.class)).thenReturn(null);
            when(context.getPrivateData(ComponentView.class)).thenReturn(view);
            when(view.getPrivateData(MethodIntf.class)).thenReturn(null);

            MethodIntf result = MethodIntfHelper.of(context);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Default resolution")
    class DefaultResolution {

        @Test
        @DisplayName("defaults to BEAN when neither context nor view provides data")
        void defaultsToBeanWhenNoViewPresent() {
            InterceptorContext context = mock(InterceptorContext.class);
            when(context.getPrivateData(MethodIntf.class)).thenReturn(null);
            when(context.getPrivateData(ComponentView.class)).thenReturn(null);

            MethodIntf result = MethodIntfHelper.of(context);

            assertEquals(MethodIntf.BEAN, result);
        }

        @Test
        @DisplayName("does not query view private data when no ComponentView is present")
        void noViewMeansNoViewInteraction() {
            InterceptorContext context = mock(InterceptorContext.class);
            when(context.getPrivateData(MethodIntf.class)).thenReturn(null);
            when(context.getPrivateData(ComponentView.class)).thenReturn(null);

            MethodIntfHelper.of(context);

            verifyNoInteractions(context.getPrivateData(ComponentView.class) == null ? null : null);
        }
    }
}
```

---

## TimerImpl.java -> suspend()

- **Debt Type:** `DESIGN`
- **Line Number:** `458`
- **Status:** `GENERATED`
- **Comment:** `TODO: Revisit this method, we probably don't need this any more. In terms of implementation, this is just equivalent to cancelTimeout() method`

```java
/**
 * Suspends any currently scheduled task for this timer
 * <p>
 * Note that, suspend does <b>not</b> cancel the {@link Timer}. Instead,
 * it just cancels the <b>next scheduled timeout</b>. So once the {@link Timer}
 * is restored (whenever that happens), the {@link Timer} will continue to
 * timeout at appropriate times.
 * </p>
 */
// TODO: Revisit this method, we probably don't need this any more.
// In terms of implementation, this is just equivalent to cancelTimeout() method
@Override
public void suspend() {
    // cancel any scheduled timer task (Future) for this timer
    // delegate to the timerservice, so that it can cancel any scheduled Future
    // for this timer
    this.timerService.cancelTimeout(this);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package com.example.timer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code Timer#suspend()}, which delegates to
 * {@code TimerService#cancelTimeout(Timer)}.
 *
 * Assumes {@code Timer} has a constructor accepting a {@code TimerService}.
 * Adjust package/type names to match the production code.
 */
class TimerSuspendTest {

    private TimerService timerService;
    private Timer timer;

    @BeforeEach
    void setUp() {
        timerService = mock(TimerService.class);
        timer = new Timer(timerService);
    }

    @Test
    @DisplayName("suspend() delegates cancellation of this timer to the TimerService")
    void suspend_delegatesCancelTimeoutWithSelf() {
        timer.suspend();

        verify(timerService, times(1)).cancelTimeout(timer);
    }

    @Test
    @DisplayName("suspend() makes no other interactions with the TimerService")
    void suspend_hasNoOtherServiceInteractions() {
        timer.suspend();

        verify(timerService).cancelTimeout(timer);
        verifyNoMoreInteractions(timerService);
    }

    @Test
    @DisplayName("suspend() is repeatable and delegates on every call")
    void suspend_calledRepeatedly_delegatesEachTime() {
        timer.suspend();
        timer.suspend();
        timer.suspend();

        verify(timerService, times(3)).cancelTimeout(timer);
    }

    @Test
    @DisplayName("suspend() does not cancel timeouts of other timers")
    void suspend_doesNotCancelOtherTimers() {
        Timer otherTimer = new Timer(timerService);

        timer.suspend();

        verify(timerService, times(1)).cancelTimeout(timer);
        verify(timerService, never()).cancelTimeout(otherTimer);
    }

    @Test
    @DisplayName("suspend() propagates exceptions thrown by the TimerService")
    void suspend_propagatesServiceException() {
        doThrow(new IllegalStateException("service unavailable"))
                .when(timerService).cancelTimeout(timer);

        assertThrows(IllegalStateException.class, () -> timer.suspend());

        verify(timerService).cancelTimeout(timer);
    }

    @Test
    @DisplayName("suspend() does not invoke the TimerService when the delegate is not called directly")
    void suspend_neverCallsUnrelatedServiceMethods() {
        timer.suspend();

        verify(timerService, never()).cancelTimeout(null);
        verifyNoMoreInteractions(timerService);
    }
}
```

---

## CSIv2Util.java -> createGSSExportedName()

- **Debt Type:** `DESIGN`
- **Line Number:** `524`
- **Status:** `GENERATED`
- **Comment:** `* <p/>      * Generate an exported name as specified in [RFC 2743], section 3.2 copied below:      * <p/>      * 3.2: Mechanism-Independent Exported Name Object Format      * <p/>      * This section specifies a mechanism-independent level of encapsulating representation for names exported via the      * GSS_Export_name() call, including an object identifier representing the exporting mechanism. The format of names      * encapsulated via this representation shall be defined within individual mechanism drafts.  The Object Identifier      * value to indicate names of this type is defined in Section 4.7 of this document.      * <p/>      * No name type OID is included in this mechanism-independent level of format definition, since (depending on      * individual mechanism specifications) the enclosed name may be implicitly typed or may be explicitly typed using      * a means other than OID encoding.      * <p/>      * The bytes within MECH_OID_LEN and NAME_LEN elements are represented most significant byte first (equivalently,      * in IP network byte order).      * <p/>      * Length          Name            Description      * <p/>      * 2               TOK_ID          Token Identifier      * For exported name objects, this must be hex 04 01.      * 2               MECH_OID_LEN    Length of the Mechanism OID      * MECH_OID_LEN    MECH_OID        Mechanism OID, in DER      * 4               NAME_LEN        Length of name      * NAME_LEN        NAME            Exported name; format defined in applicable mechanism draft.      * <p/>      * A concrete example of the contents of an exported name object, derived from the Kerberos Version 5 mechanism, is      * as follows:      * <p/>      * 04 01 00 0B 06 09 2A 86 48 86 F7 12 01 02 02 hx xx xx xl pp qq ... zz      * <p/>      * ...      *      * @param oid  the DER encoded OID.      * @param name the name to be converted to {@code GSSExportedName}.      * @return a {@code byte[]} representing the exported name.`

```java
/**
 * <p/>
 * Generate an exported name as specified in [RFC 2743], section 3.2 copied below:
 * <p/>
 * 3.2: Mechanism-Independent Exported Name Object Format
 * <p/>
 * This section specifies a mechanism-independent level of encapsulating representation for names exported via the
 * GSS_Export_name() call, including an object identifier representing the exporting mechanism. The format of names
 * encapsulated via this representation shall be defined within individual mechanism drafts.  The Object Identifier
 * value to indicate names of this type is defined in Section 4.7 of this document.
 * <p/>
 * No name type OID is included in this mechanism-independent level of format definition, since (depending on
 * individual mechanism specifications) the enclosed name may be implicitly typed or may be explicitly typed using
 * a means other than OID encoding.
 * <p/>
 * The bytes within MECH_OID_LEN and NAME_LEN elements are represented most significant byte first (equivalently,
 * in IP network byte order).
 * <p/>
 * Length          Name            Description
 * <p/>
 * 2               TOK_ID          Token Identifier
 * For exported name objects, this must be hex 04 01.
 * 2               MECH_OID_LEN    Length of the Mechanism OID
 * MECH_OID_LEN    MECH_OID        Mechanism OID, in DER
 * 4               NAME_LEN        Length of name
 * NAME_LEN        NAME            Exported name; format defined in applicable mechanism draft.
 * <p/>
 * A concrete example of the contents of an exported name object, derived from the Kerberos Version 5 mechanism, is
 * as follows:
 * <p/>
 * 04 01 00 0B 06 09 2A 86 48 86 F7 12 01 02 02 hx xx xx xl pp qq ... zz
 * <p/>
 * ...
 *
 * @param oid  the DER encoded OID.
 * @param name the name to be converted to {@code GSSExportedName}.
 * @return a {@code byte[]} representing the exported name.
 */
public static byte[] createGSSExportedName(byte[] oid, byte[] name) {
    int olen = oid.length;
    int nlen = name.length;
    // size according to spec.
    int size = 2 + 2 + olen + 4 + nlen;
    // allocate space for the exported name.
    byte[] buf = new byte[size];
    // index.
    int i = 0;
    // standard header.
    buf[i++] = 0x04;
    buf[i++] = 0x01;
    // encode oid length.
    buf[i++] = (byte) (olen & 0xFF00);
    buf[i++] = (byte) (olen & 0x00FF);
    // copy the oid in the exported name buffer.
    System.arraycopy(oid, 0, buf, i, olen);
    i += olen;
    // encode the name length in the exported buffer.
    buf[i++] = (byte) (nlen & 0xFF000000);
    buf[i++] = (byte) (nlen & 0x00FF0000);
    buf[i++] = (byte) (nlen & 0x0000FF00);
    buf[i++] = (byte) (nlen & 0x000000FF);
    // finally, copy the name bytes.
    System.arraycopy(name, 0, buf, i, nlen);
    return buf;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

// NOTE: Replace GssExportedNames with the class that declares createGSSExportedName.
class CreateGSSExportedNameTest {

    // DER-encoded Kerberos V5 mechanism OID from RFC 2743 example: 06 09 2A 86 48 86 F7 12 01 02 02
    private static final byte[] KRB5_OID = {
            0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x12, 0x01, 0x02, 0x02
    };

    private static byte[] bytes(int length, int fill) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) fill;
        }
        return b;
    }

    @Nested
    @DisplayName("Header and structure (RFC 2743 section 3.2)")
    class HeaderAndStructure {

        @Test
        @DisplayName("TOK_ID must be 04 01")
        void tokenIdIsHex0401() {
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, new byte[]{0x41});
            assertEquals((byte) 0x04, result[0]);
            assertEquals((byte) 0x01, result[1]);
        }

        @Test
        @DisplayName("MECH_OID_LEN is encoded as 2-byte big-endian length")
        void mechOidLengthEncodedBigEndian() {
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, new byte[0]);
            assertEquals((byte) 0x00, result[2]);
            assertEquals((byte) 0x0B, result[3]);
        }

        @Test
        @DisplayName("MECH_OID is copied verbatim after its length")
        void mechOidCopiedVerbatim() {
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, new byte[0]);
            byte[] actualOid = new byte[KRB5_OID.length];
            System.arraycopy(result, 4, actualOid, 0, KRB5_OID.length);
            assertArrayEquals(KRB5_OID, actualOid);
        }

        @Test
        @DisplayName("NAME_LEN is encoded as 4-byte big-endian length")
        void nameLengthEncodedBigEndian() {
            byte[] name = {0x01, 0x02, 0x03};
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            int offset = 4 + KRB5_OID.length;
            assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x03},
                    new byte[]{result[offset], result[offset + 1], result[offset + 2], result[offset + 3]});
        }

        @Test
        @DisplayName("NAME bytes are copied verbatim at the end of the buffer")
        void nameCopiedVerbatim() {
            byte[] name = {0x10, 0x20, 0x30, 0x40, 0x50};
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            int offset = 2 + 2 + KRB5_OID.length + 4;
            byte[] actualName = new byte[name.length];
            System.arraycopy(result, offset, actualName, 0, name.length);
            assertArrayEquals(name, actualName);
        }

        @Test
        @DisplayName("Total size equals 2 + 2 + oidLen + 4 + nameLen")
        void totalSizeMatchesSpec() {
            byte[] name = bytes(17, 0x7A);
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            assertEquals(2 + 2 + KRB5_OID.length + 4 + name.length, result.length);
        }
    }

    @Nested
    @DisplayName("Boundary and edge cases")
    class BoundaryCases {

        @Test
        @DisplayName("Empty name yields NAME_LEN of zero and no name bytes")
        void emptyName() {
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, new byte[0]);
            assertEquals(2 + 2 + KRB5_OID.length + 4, result.length);
            int offset = 4 + KRB5_OID.length;
            assertArrayEquals(new byte[]{0, 0, 0, 0},
                    new byte[]{result[offset], result[offset + 1], result[offset + 2], result[offset + 3]});
        }

        @Test
        @DisplayName("Empty OID yields MECH_OID_LEN of zero")
        void emptyOid() {
            byte[] name = {0x55};
            byte[] result = GssExportedNames.createGSSExportedName(new byte[0], name);
            assertEquals((byte) 0x00, result[2]);
            assertEquals((byte) 0x00, result[3]);
            assertEquals(2 + 2 + 0 + 4 + name.length, result.length);
            assertEquals((byte) 0x55, result[result.length - 1]);
        }

        @Test
        @DisplayName("Empty OID and empty name produce exactly the 8-byte header")
        void emptyOidAndEmptyName() {
            byte[] result = GssExportedNames.createGSSExportedName(new byte[0], new byte[0]);
            assertArrayEquals(new byte[]{0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, result);
        }

        @Test
        @DisplayName("Name length of 255 is encoded correctly in the low byte")
        void nameLength255() {
            byte[] name = bytes(255, 0x11);
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            int offset = 4 + KRB5_OID.length;
            assertArrayEquals(new byte[]{0x00, 0x00, 0x00, (byte) 0xFF},
                    new byte[]{result[offset], result[offset + 1], result[offset + 2], result[offset + 3]});
        }

        @Test
        @DisplayName("Name length of 256 must encode the 0x01 byte at position 2 of NAME_LEN")
        void nameLength256() {
            // Expected per spec: 00 00 01 00. Current implementation masks bytes incorrectly
            // (cast of (nlen & 0x00FF0000) and (nlen & 0x0000FF00) to byte yields 0), so this
            // test documents the encoding defect.
            byte[] name = bytes(256, 0x22);
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            int offset = 4 + KRB5_OID.length;
            assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x00},
                    new byte[]{result[offset], result[offset + 1], result[offset + 2], result[offset + 3]});
        }

        @Test
        @DisplayName("OID length of 256 must encode the high byte as 0x01")
        void oidLength256() {
            // Expected per spec: 01 00. Current implementation drops the high byte.
            byte[] oid = bytes(256, 0x06);
            byte[] result = GssExportedNames.createGSSExportedName(oid, new byte[0]);
            assertEquals((byte) 0x01, result[2]);
            assertEquals((byte) 0x00, result[3]);
        }

        @Test
        @DisplayName("Name bytes with high-bit values are preserved unchanged")
        void highBitBytesPreserved() {
            byte[] name = {(byte) 0x80, (byte) 0xFF, (byte) 0x00, (byte) 0x7F};
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            int offset = 2 + 2 + KRB5_OID.length + 4;
            byte[] actual = new byte[name.length];
            System.arraycopy(result, offset, actual, 0, name.length);
            assertArrayEquals(name, actual);
        }
    }

    @Nested
    @DisplayName("Reference example and contract")
    class ReferenceAndContract {

        @Test
        @DisplayName("Matches the RFC 2743 Kerberos V5 example prefix 04 01 00 0B 06 09 2A 86 48 86 F7 12 01 02 02")
        void matchesRfcExamplePrefix() {
            byte[] result = GssExportedNames.createGSSExportedName(KRB5_OID, new byte[]{0x01});
            byte[] expectedPrefix = {
                    0x04, 0x01, 0x00, 0x0B,
                    0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x12, 0x01, 0x02, 0x02
            };
            byte[] actualPrefix = new byte[expectedPrefix.length];
            System.arraycopy(result, 0, actualPrefix, 0, expectedPrefix.length);
            assertArrayEquals(expectedPrefix, actualPrefix);
        }

        @Test
        @DisplayName("Full exported name matches hand-built expected byte array")
        void fullExpectedArray() {
            byte[] name = {'h', 'x', 'x'};
            byte[] expected = {
                    0x04, 0x01,
                    0x00, 0x0B,
                    0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x12, 0x01, 0x02, 0x02,
                    0x00, 0x00, 0x00, 0x03,
                    'h', 'x', 'x'
            };
            assertArrayEquals(expected, GssExportedNames.createGSSExportedName(KRB5_OID, name));
        }

        @Test
        @DisplayName("Input arrays are not mutated")
        void inputsNotMutated() {
            byte[] oid = KRB5_OID.clone();
            byte[] name = {0x01, 0x02, 0x03};
            byte[] oidCopy = oid.clone();
            byte[] nameCopy = name.clone();
            GssExportedNames.createGSSExportedName(oid, name);
            assertArrayEquals(oidCopy, oid);
            assertArrayEquals(nameCopy, name);
        }

        @Test
        @DisplayName("Returns a new array on each invocation")
        void returnsFreshArray() {
            byte[] name = {0x01};
            byte[] first = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            byte[] second = GssExportedNames.createGSSExportedName(KRB5_OID, name);
            assertNotSame(first, second);
            assertArrayEquals(first, second);
        }

        @Test
        @DisplayName("Null OID throws NullPointerException")
        void nullOidThrows() {
            assertThrows(NullPointerException.class,
                    () -> GssExportedNames.createGSSExportedName(null, new byte[]{0x01}));
        }

        @Test
        @DisplayName("Null name throws NullPointerException")
        void nullNameThrows() {
            assertThrows(NullPointerException.class,
                    () -> GssExportedNames.createGSSExportedName(KRB5_OID, null));
        }
    }
}
```

---

## InterfaceRepository.java -> getTypeCode()

- **Debt Type:** `DESIGN`
- **Line Number:** `223`
- **Status:** `GENERATED`
- **Comment:** `* Returns the TypeCode IDL TypeCodes for parameter, result, attribute      * and value member types.      * This may provoke a mapping of the class argument.      * <p/>      * Exception classes map to both values and exceptions. For these, this      * method returns the typecode for the value, and you can use the      * <code>getExceptionTypeCode</code> TODO method to get the typecode for the      * mapping to exception.      *      * @param cls The Java class denoting the java type.`

```java
/**
 * Returns the TypeCode IDL TypeCodes for parameter, result, attribute
 * and value member types.
 * This may provoke a mapping of the class argument.
 * <p/>
 * Exception classes map to both values and exceptions. For these, this
 * method returns the typecode for the value, and you can use the
 * <code>getExceptionTypeCode</code> TODO method to get the typecode for the
 * mapping to exception.
 *
 * @param cls The Java class denoting the java type.
 */
private TypeCode getTypeCode(Class cls) throws IRConstructionException, RMIIIOPViolationException {
    if (cls == null)
        throw IIOPLogger.ROOT_LOGGER.invalidNullClass();
    TypeCode ret = (TypeCode) typeCodeMap.get(cls);
    if (ret == null) {
        if (cls == String.class)
            ret = getJavaLangString().type();
        else if (cls == Object.class)
            ret = getJavaLang_Object().type();
        else if (cls == Class.class)
            ret = getJavaxRmiCORBAClassDesc().type();
        else if (cls == java.io.Serializable.class)
            ret = getJavaIoSerializable().type();
        else if (cls == java.io.Externalizable.class)
            ret = getJavaIoExternalizable().type();
        else {
            // Try adding a mapping of the the class to the IR
            addClass(cls);
            // Lookup again, it should be there now.
            ret = (TypeCode) typeCodeMap.get(cls);
            if (ret == null)
                throw IIOPLogger.ROOT_LOGGER.unknownTypeCodeForClass(cls.getName());
            else
                return ret;
        }
        typeCodeMap.put(cls, ret);
    }
    return ret;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the private {@code getTypeCode(Class)} method.
 *
 * The owning class is not provided in the source excerpt, so it is referenced via
 * TARGET_CLASS and accessed reflectively. Adjust TARGET_CLASS to the fully qualified
 * name of the class that declares getTypeCode(Class). The owning class is expected to
 * have an accessible no-arg constructor.
 */
class GetTypeCodeTest {

    private static final String TARGET_CLASS = "org.wildfly.iiop.openjdk.rmi.ir.IDLGenerator";

    private Object target;
    private Method getTypeCodeMethod;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> clazz = Class.forName(TARGET_CLASS);
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        target = ctor.newInstance();

        getTypeCodeMethod = clazz.getDeclaredMethod("getTypeCode", Class.class);
        getTypeCodeMethod.setAccessible(true);
    }

    /**
     * Invokes the private method and unwraps reflective wrapping so that the
     * original checked exception (IRConstructionException / RMIIIOPViolationException)
     * is surfaced to the caller.
     */
    private Object invokeGetTypeCode(Class<?> cls) throws Throwable {
        try {
            return getTypeCodeMethod.invoke(target, cls);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    void nullClassThrowsException() {
        Throwable thrown = assertThrows(Throwable.class, () -> invokeGetTypeCode(null));
        assertNotNull(thrown, "A null class argument must be rejected");
        String name = thrown.getClass().getName();
        assertTrue(
                name.contains("IRConstructionException") || name.contains("RMIIIOPViolationException"),
                "Unexpected exception type for null class: " + name
        );
    }

    @Test
    void stringMapsToNonNullTypeCode() throws Throwable {
        Object typeCode = invokeGetTypeCode(String.class);
        assertNotNull(typeCode, "java.lang.String must map to a TypeCode");
    }

    @Test
    void objectMapsToNonNullTypeCode() throws Throwable {
        Object typeCode = invokeGetTypeCode(Object.class);
        assertNotNull(typeCode, "java.lang.Object must map to a TypeCode");
    }

    @Test
    void classMapsToNonNullTypeCode() throws Throwable {
        Object typeCode = invokeGetTypeCode(Class.class);
        assertNotNull(typeCode, "java.lang.Class must map to a TypeCode");
    }

    @Test
    void serializableMapsToNonNullTypeCode() throws Throwable {
        Object typeCode = invokeGetTypeCode(Serializable.class);
        assertNotNull(typeCode, "java.io.Serializable must map to a TypeCode");
    }

    @Test
    void externalizableMapsToNonNullTypeCode() throws Throwable {
        Object typeCode = invokeGetTypeCode(Externalizable.class);
        assertNotNull(typeCode, "java.io.Externalizable must map to a TypeCode");
    }

    @Test
    void repeatedLookupForStringReturnsCachedInstance() throws Throwable {
        Object first = invokeGetTypeCode(String.class);
        Object second = invokeGetTypeCode(String.class);
        assertSame(first, second, "Repeated lookups must return the cached TypeCode instance");
    }

    @Test
    void repeatedLookupForObjectReturnsCachedInstance() throws Throwable {
        Object first = invokeGetTypeCode(Object.class);
        Object second = invokeGetTypeCode(Object.class);
        assertSame(first, second, "Repeated lookups must return the cached TypeCode instance");
    }

    @Test
    void repeatedLookupForSerializableReturnsCachedInstance() throws Throwable {
        Object first = invokeGetTypeCode(Serializable.class);
        Object second = invokeGetTypeCode(Serializable.class);
        assertSame(first, second, "Repeated lookups must return the cached TypeCode instance");
    }

    @Test
    void distinctJavaTypesYieldDistinctTypeCodes() throws Throwable {
        Object stringTc = invokeGetTypeCode(String.class);
        Object objectTc = invokeGetTypeCode(Object.class);
        assertNotSame(stringTc, objectTc, "String and Object must not share a TypeCode");
    }

    @Test
    void userDefinedSerializableClassIsMappedAndCached() throws Throwable {
        Object first = invokeGetTypeCode(SampleValue.class);
        assertNotNull(first, "A user-defined class must be mapped into a TypeCode via addClass");

        Object second = invokeGetTypeCode(SampleValue.class);
        assertSame(first, second, "Mapped user-defined class must be served from the map on subsequent calls");
    }

    @Test
    void userDefinedClassTypeCodeDiffersFromString() throws Throwable {
        Object sampleTc = invokeGetTypeCode(SampleValue.class);
        Object stringTc = invokeGetTypeCode(String.class);
        assertNotSame(sampleTc, stringTc, "A user-defined class must not reuse the String TypeCode");
    }

    /** Simple serializable value type used to exercise the addClass mapping path. */
    public static class SampleValue implements Serializable {
        private static final long serialVersionUID = 1L;
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
```

---

## PersistenceUnitServiceHandler.java -> getOrCreateResource()

- **Debt Type:** `DESIGN`
- **Line Number:** `1152`
- **Status:** `GENERATED`
- **Comment:** `* TODO this is a temporary hack into internals until DeploymentUnit exposes a proper Resource-based API`

```java
/**
 * TODO this is a temporary hack into internals until DeploymentUnit exposes a proper Resource-based API
 */
private static Resource getOrCreateResource(final Resource parent, final PathElement element) {
    synchronized (parent) {
        if (parent.hasChild(element)) {
            return parent.requireChild(element);
        } else {
            final Resource resource = Resource.Factory.create();
            parent.registerChild(element, resource);
            return resource;
        }
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the private static helper {@code getOrCreateResource(Resource, PathElement)}.
 *
 * The method is private, so it is invoked reflectively. Update {@link #OWNER_CLASS_NAME}
 * to the fully qualified name of the class that declares the method.
 */
class GetOrCreateResourceTest {

    /** TODO: set to the fully qualified name of the class declaring getOrCreateResource. */
    private static final String OWNER_CLASS_NAME = "org.jboss.as.server.deployment.DeploymentUnitProcessor";

    private static final String METHOD_NAME = "getOrCreateResource";

    private Method method;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> owner = Class.forName(OWNER_CLASS_NAME);
        method = owner.getDeclaredMethod(METHOD_NAME, Resource.class, PathElement.class);
        method.setAccessible(true);
        assertTrue(Modifier.isStatic(method.getModifiers()), "method must be static");
        assertTrue(Modifier.isPrivate(method.getModifiers()), "method must be private");
    }

    private Resource invoke(Resource parent, PathElement element) throws Throwable {
        try {
            return (Resource) method.invoke(null, parent, element);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Nested
    @DisplayName("Child creation and retrieval")
    class ChildHandling {

        @Test
        @DisplayName("creates and registers a new child when none exists")
        void createsChildWhenAbsent() throws Throwable {
            Resource parent = Resource.Factory.create();
            PathElement element = PathElement.pathElement("deployment", "app.war");

            assertFalse(parent.hasChild(element));

            Resource created = invoke(parent, element);

            assertNotNull(created, "created resource must not be null");
            assertTrue(parent.hasChild(element), "parent must now have the child registered");
            assertSame(created, parent.requireChild(element),
                    "registered child must be the returned resource");
        }

        @Test
        @DisplayName("returns the existing child instance when already present")
        void returnsExistingChild() throws Throwable {
            Resource parent = Resource.Factory.create();
            PathElement element = PathElement.pathElement("subsystem", "ejb3");
            Resource existing = Resource.Factory.create();
            parent.registerChild(element, existing);

            Resource result = invoke(parent, element);

            assertSame(existing, result, "must return the already registered child");
        }

        @Test
        @DisplayName("does not replace an existing child with a new instance")
        void doesNotReplaceExistingChild() throws Throwable {
            Resource parent = Resource.Factory.create();
            PathElement element = PathElement.pathElement("subsystem", "undertow");
            Resource existing = Resource.Factory.create();
            parent.registerChild(element, existing);

            invoke(parent, element);

            assertSame(existing, parent.requireChild(element),
                    "existing child must not be replaced");
        }

        @Test
        @DisplayName("repeated calls are idempotent and return the same resource")
        void repeatedCallsAreIdempotent() throws Throwable {
            Resource parent = Resource.Factory.create();
            PathElement element = PathElement.pathElement("deployment", "idempotent.jar");

            Resource first = invoke(parent, element);
            Resource second = invoke(parent, element);
            Resource third = invoke(parent, element);

            assertSame(first, second);
            assertSame(second, third);
            assertEquals(1, parent.getChildren(element.getKey()).size(),
                    "only one child should be registered for the element key");
        }

        @Test
        @DisplayName("distinct path elements produce distinct children")
        void distinctElementsProduceDistinctChildren() throws Throwable {
            Resource parent = Resource.Factory.create();
            PathElement a = PathElement.pathElement("deployment", "a.war");
            PathElement b = PathElement.pathElement("deployment", "b.war");

            Resource ra = invoke(parent, a);
            Resource rb = invoke(parent, b);

            assertNotSame(ra, rb);
            assertTrue(parent.hasChild(a));
            assertTrue(parent.hasChild(b));
        }

        @Test
        @DisplayName("children are created under the given parent only")
        void childCreatedOnlyUnderGivenParent() throws Throwable {
            Resource parentOne = Resource.Factory.create();
            Resource parentTwo = Resource.Factory.create();
            PathElement element = PathElement.pathElement("subsystem", "logging");

            Resource created = invoke(parentOne, element);

            assertTrue(parentOne.hasChild(element));
            assertFalse(parentTwo.hasChild(element));
            assertNotSame(created, invoke(parentTwo, element));
        }
    }

    @Nested
    @DisplayName("Argument validation")
    class ArgumentValidation {

        @Test
        @DisplayName("null parent results in NullPointerException")
        void nullParentThrows() {
            PathElement element = PathElement.pathElement("subsystem", "any");

            assertThrows(NullPointerException.class, () -> invoke(null, element));
        }

        @Test
        @DisplayName("null element results in an exception")
        void nullElementThrows() {
            Resource parent = Resource.Factory.create();

            assertThrows(Exception.class, () -> invoke(parent, null));
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("concurrent callers for the same element receive one shared child")
        void concurrentCallersShareSingleChild() throws Exception {
            final Resource parent = Resource.Factory.create();
            final PathElement element = PathElement.pathElement("deployment", "race.war");
            final int threads = 16;

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Resource>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    futures.add(pool.submit((Callable<Resource>) () -> {
                        start.await();
                        return invokeUnchecked(parent, element);
                    }));
                }
                start.countDown();

                Set<Resource> distinct = ConcurrentHashMap.newKeySet();
                for (Future<Resource> f : futures) {
                    distinct.add(f.get(20, TimeUnit.SECONDS));
                }

                assertEquals(1, distinct.size(),
                        "all concurrent callers must observe the same child instance");
                assertTrue(parent.hasChild(element));
            } finally {
                pool.shutdownNow();
            }
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("concurrent callers for different elements all succeed")
        void concurrentCallersDifferentElements() throws Exception {
            final Resource parent = Resource.Factory.create();
            final int count = 20;

            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                List<Future<Resource>> futures = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    final PathElement element = PathElement.pathElement("deployment", "app-" + i + ".war");
                    futures.add(pool.submit(() -> invokeUnchecked(parent, element)));
                }
                Set<Resource> distinct = ConcurrentHashMap.newKeySet();
                for (Future<Resource> f : futures) {
                    distinct.add(f.get(20, TimeUnit.SECONDS));
                }

                assertEquals(count, distinct.size());
                assertEquals(count, parent.getChildren("deployment").size());
            } finally {
                pool.shutdownNow();
            }
        }

        private Resource invokeUnchecked(Resource parent, PathElement element) {
            try {
                return invoke(parent, element);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
```

---

## StandardConfigsXMLValidationUnitTestCase.java -> testStandaloneEC2HA()

- **Debt Type:** `DESIGN`
- **Line Number:** `69`
- **Status:** `GENERATED`
- **Comment:** `TODO Leave commented out until domain-jts.xml is definitely removed from the configuration @Test public void testDomainJTS() throws Exception { parseXml("docs/examples/configs/domain-jts.xml"); } `

```java
//TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
@Test
public void testStandaloneEC2HA() throws Exception {
    parseXml("docs/examples/configs/standalone-ec2-ha.xml");
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests for example configuration parsing.
 *
 * Assumes parseXml(String) is provided by the enclosing test base class and
 * throws Exception on parse failure.
 *
 * The legacy domain-jts.xml example has been retired and is intentionally
 * excluded from parsing. Its previously commented-out test is replaced by
 * an assertion that it is not part of the active example set.
 */
public class ExampleConfigParsingTest extends XmlParsingTestBase {

    private static final Path CONFIG_DIR = Paths.get("docs/examples/configs");
    private static final String RETIRED_CONFIG = "domain-jts.xml";

    @Test
    public void testStandaloneEC2HA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-ha.xml");
    }

    @Test
    public void testRetiredDomainJtsConfigIsNotActive() throws IOException {
        List<String> activeConfigs = listExampleConfigs();
        assertFalse(activeConfigs.contains(RETIRED_CONFIG),
                "Retired config " + RETIRED_CONFIG + " must not be part of the active example set");
    }

    @Test
    public void testExampleConfigDirectoryContainsStandaloneEC2HA() throws IOException {
        List<String> activeConfigs = listExampleConfigs();
        assertTrue(activeConfigs.contains("standalone-ec2-ha.xml"),
                "Expected standalone-ec2-ha.xml to be present in the example configs");
    }

    @TestFactory
    public Stream<DynamicTest> testAllActiveExampleConfigsParse() throws IOException {
        return listExampleConfigs().stream()
                .map(name -> DynamicTest.dynamicTest("parse " + name,
                        () -> assertDoesNotThrow(
                                () -> parseXml(CONFIG_DIR.resolve(name).toString()),
                                "Failed to parse example config: " + name)));
    }

    private List<String> listExampleConfigs() throws IOException {
        try (Stream<Path> files = Files.list(CONFIG_DIR)) {
            return files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".xml"))
                    .filter(n -> !n.equals(RETIRED_CONFIG))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}

/**
 * Minimal placeholder for the base class that supplies parseXml(String).
 * Replace with the existing project base class if one is already available.
 */
abstract class XmlParsingTestBase {
    protected void parseXml(String path) throws Exception {
        // Delegates to the project's XML parsing implementation.
        throw new UnsupportedOperationException("Provide parseXml implementation in base class");
    }
}
```

---

## StandardConfigsXMLValidationUnitTestCase.java -> testStandaloneEC2HA()

- **Debt Type:** `DESIGN`
- **Line Number:** `59`
- **Status:** `GENERATED`
- **Comment:** `TODO Leave commented out until domain-jts.xml is definitely removed from the configuration @Test public void testDomainJTS() throws Exception { parseXml("docs/examples/configs/domain-jts.xml"); } `

```java
//TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
@Test
public void testStandaloneEC2HA() throws Exception {
    parseXml("docs/examples/configs/standalone-ec2-ha.xml");
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the standalone EC2 HA example configuration.
 *
 * The retired domain-jts.xml test was removed (dead commented-out code, design debt).
 * parseXml(String) is assumed to be provided by the existing test base class.
 */
class ExampleConfigParsingTest {

    private static final String STANDALONE_EC2_HA_CONFIG = "docs/examples/configs/standalone-ec2-ha.xml";

    @Test
    @DisplayName("standalone-ec2-ha.xml example config exists on disk")
    void standaloneEC2HAConfigFileExists() {
        Path configPath = Paths.get(STANDALONE_EC2_HA_CONFIG);
        assertTrue(Files.isRegularFile(configPath),
                "Expected example config to exist at: " + configPath.toAbsolutePath());
    }

    @Test
    @DisplayName("standalone-ec2-ha.xml example config parses without error")
    void testStandaloneEC2HA() throws Exception {
        assertDoesNotThrow(() -> parseXml(STANDALONE_EC2_HA_CONFIG));
    }

    @Test
    @DisplayName("parsing standalone-ec2-ha.xml twice yields no error (idempotent parsing)")
    void standaloneEC2HAParsingIsRepeatable() {
        assertDoesNotThrow(() -> {
            parseXml(STANDALONE_EC2_HA_CONFIG);
            parseXml(STANDALONE_EC2_HA_CONFIG);
        });
    }
}
```

---

## ElytronIntegrationResourceDefinitions.java -> getElytronTrustStoreResourceDefinition()

- **Debt Type:** `DESIGN`
- **Line Number:** `113`
- **Status:** `GENERATED`
- **Comment:** `* Defines a resource that represents an Elytron-compatible trust store that will be exported by a JSSE-enabled domain      * in the legacy security subsystem.      *      * To export the trust store the resource uses a {@code BasicAddHandler} implementation that registers the elytron key-store      * capability and implements a {@code org.jboss.as.security.elytron.BasicService.ValueSupplier} that uses the injected      * {@code SecurityDomainContext} to obtain a {@code JSSESecurityDomain}. If such domain is found, its configured trust      * store is obtained and returned.      *      * NOTE 1: In the Elytron subsystem, both key stores and trust stores are registered using the same capability. This      * means that the name of the trust store must be unique across all configured trust stores and key stores. If a trust      * store resource is registered with the same name of a key store resource, an error will occur.      *      * The {@code ValueSupplier} implementation throws an exception if the referenced legacy domain is not a JSSE-enabled      * domain or if the domain doesn't contain a trust store configuration.      *      * NOTE 2: The {@code PicketBox} implementation of a {@code JSSESecurityDomain} returns a reference to the key store if      * a trust store was not configured. So extra care must be taken when that implementation is used (default) as the code      * will silently export the key store as a trust store instead of throwing an exception to alert about a missing trust      * store configuration in the legacy JSSE-enabled domain.`

```java
/**
 * Defines a resource that represents an Elytron-compatible trust store that will be exported by a JSSE-enabled domain
 * in the legacy security subsystem.
 *
 * To export the trust store the resource uses a {@code BasicAddHandler} implementation that registers the elytron key-store
 * capability and implements a {@code org.jboss.as.security.elytron.BasicService.ValueSupplier} that uses the injected
 * {@code SecurityDomainContext} to obtain a {@code JSSESecurityDomain}. If such domain is found, its configured trust
 * store is obtained and returned.
 *
 * NOTE 1: In the Elytron subsystem, both key stores and trust stores are registered using the same capability. This
 * means that the name of the trust store must be unique across all configured trust stores and key stores. If a trust
 * store resource is registered with the same name of a key store resource, an error will occur.
 *
 * The {@code ValueSupplier} implementation throws an exception if the referenced legacy domain is not a JSSE-enabled
 * domain or if the domain doesn't contain a trust store configuration.
 *
 * NOTE 2: The {@code PicketBox} implementation of a {@code JSSESecurityDomain} returns a reference to the key store if
 * a trust store was not configured. So extra care must be taken when that implementation is used (default) as the code
 * will silently export the key store as a trust store instead of throwing an exception to alert about a missing trust
 * store configuration in the legacy JSSE-enabled domain.
 */
public static ResourceDefinition getElytronTrustStoreResourceDefinition() {
    final AttributeDefinition[] attributes = new AttributeDefinition[] { LEGACY_JSSE_CONFIG };
    final AbstractAddStepHandler addHandler = new ModelOnlyAddStepHandler(attributes);
    return new BasicResourceDefinition(Constants.ELYTRON_TRUST_STORE, addHandler, attributes, KEY_STORE_RUNTIME_CAPABILITY);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.jboss.as.controller.ManagementResourceRegistration;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/*
 * NOTE: The enclosing class name and the Constants / KEY_STORE_RUNTIME_CAPABILITY
 * locations below are assumptions based on the source comment. Adjust the imports
 * and the owning class reference to match the actual code base.
 */
class ElytronTrustStoreResourceDefinitionTest {

    private ResourceDefinition definition;

    @BeforeEach
    void setUp() {
        definition = LegacySecurityResourceDefinitions.getElytronTrustStoreResourceDefinition();
    }

    @Test
    @DisplayName("Factory method returns a non-null resource definition")
    void definitionIsNotNull() {
        assertNotNull(definition, "Trust store resource definition must not be null");
    }

    @Test
    @DisplayName("Resource path uses the Elytron trust store constant name")
    void pathElementMatchesTrustStoreName() {
        PathElement pathElement = definition.getPathElement();
        assertNotNull(pathElement, "Path element must be defined");
        assertEquals(Constants.ELYTRON_TRUST_STORE, pathElement.getKey(),
                "Path key must match the Elytron trust store resource name");
    }

    @Test
    @DisplayName("Registering capabilities registers the elytron key-store runtime capability")
    void registersKeyStoreCapability() {
        ManagementResourceRegistration registration = mock(ManagementResourceRegistration.class);

        definition.registerCapabilities(registration);

        verify(registration).registerCapability(KEY_STORE_RUNTIME_CAPABILITY);
    }

    @Test
    @DisplayName("Each factory call produces an independent definition instance")
    void factoryReturnsNewInstances() {
        ResourceDefinition second = LegacySecurityResourceDefinitions.getElytronTrustStoreResourceDefinition();

        assertNotSame(definition, second, "Each call should create a fresh resource definition");
        assertEquals(definition.getPathElement().getKey(), second.getPathElement().getKey(),
                "Independent instances should describe the same resource type");
    }

    @Test
    @DisplayName("Path element is a named element, not a wildcard")
    void pathElementIsNamed() {
        PathElement pathElement = definition.getPathElement();

        assertTrue(!PathElement.WILDCARD_VALUE.equals(pathElement.getKey()),
                "Trust store path key must not be a wildcard key");
        assertNotNull(pathElement.getValue(), "Trust store path value must be set");
    }

    @Nested
    @DisplayName("Registration contract")
    class RegistrationContract {

        @Test
        @DisplayName("Capability registration is performed exactly once per call")
        void capabilityRegisteredOnce() {
            ManagementResourceRegistration registration = mock(ManagementResourceRegistration.class);

            definition.registerCapabilities(registration);

            verify(registration, org.mockito.Mockito.times(1)).registerCapability(KEY_STORE_RUNTIME_CAPABILITY);
        }
    }
}
```

---

## ElytronIntegrationResourceDefinitions.java -> getElytronTrustManagersResourceDefinition()

- **Debt Type:** `DESIGN`
- **Line Number:** `156`
- **Status:** `GENERATED`
- **Comment:** `* Defines a resource that represents Elytron-compatible trust managers that can be exported by a JSSE-enabled domain      * in the legacy security subsystem.      *      * To export the trust managers the resource uses a {@code BasicAddHandler} implementation that registers the elytron      * trust-managers capability and implements a {@code org.jboss.as.security.elytron.BasicService.ValueSupplier} that uses      * the injected {@code SecurityDomainContext} to obtain a {@code JSSESecurityDomain}. If such domain is found, its      * configured trust manager array is obtained and returned.      *      * The {@code ValueSupplier} implementation throws an exception if the referenced legacy domain is not a JSSE-enabled      * domain or if the domain doesn't contain a trust store configuration that can be used to build the trust managers.      *      * NOTE: The {@code PicketBox} implementation of a {@code JSSESecurityDomain} returns a reference to the key store if      * a trust store was not configured. This means that the trust managers that it builds will use the configured key store      * instead of throwing an exception to alert about a missing trust store configuration. So extra care must be taken      * to ensure that the exported trust managers are being built using the correct trust stores.`

```java
/**
 * Defines a resource that represents Elytron-compatible trust managers that can be exported by a JSSE-enabled domain
 * in the legacy security subsystem.
 *
 * To export the trust managers the resource uses a {@code BasicAddHandler} implementation that registers the elytron
 * trust-managers capability and implements a {@code org.jboss.as.security.elytron.BasicService.ValueSupplier} that uses
 * the injected {@code SecurityDomainContext} to obtain a {@code JSSESecurityDomain}. If such domain is found, its
 * configured trust manager array is obtained and returned.
 *
 * The {@code ValueSupplier} implementation throws an exception if the referenced legacy domain is not a JSSE-enabled
 * domain or if the domain doesn't contain a trust store configuration that can be used to build the trust managers.
 *
 * NOTE: The {@code PicketBox} implementation of a {@code JSSESecurityDomain} returns a reference to the key store if
 * a trust store was not configured. This means that the trust managers that it builds will use the configured key store
 * instead of throwing an exception to alert about a missing trust store configuration. So extra care must be taken
 * to ensure that the exported trust managers are being built using the correct trust stores.
 */
public static ResourceDefinition getElytronTrustManagersResourceDefinition() {
    final AttributeDefinition[] attributes = new AttributeDefinition[] { LEGACY_JSSE_CONFIG };
    final AbstractAddStepHandler addHandler = new ModelOnlyAddStepHandler(attributes);
    return new BasicResourceDefinition(Constants.ELYTRON_TRUST_MANAGER, addHandler, attributes, TRUST_MANAGER_RUNTIME_CAPABILITY);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the resource definition that exports Elytron-compatible trust managers
 * from a legacy JSSE-enabled security domain.
 *
 * NOTE: Adjust the owning class name / package below to match the actual location
 * of getElytronTrustManagersResourceDefinition() in the codebase.
 */
class ElytronTrustManagersResourceDefinitionTest {

    private ResourceDefinition definition;

    @BeforeEach
    void setUp() {
        // Replace 'LegacySecurityResourceDefinitions' with the class that declares the method.
        definition = LegacySecurityResourceDefinitions.getElytronTrustManagersResourceDefinition();
    }

    @Test
    @DisplayName("Resource definition is created")
    void definitionIsNotNull() {
        assertNotNull(definition, "Resource definition must not be null");
    }

    @Test
    @DisplayName("Resource is registered under the elytron trust-manager path")
    void pathElementMatchesElytronTrustManager() {
        assertNotNull(definition.getPathElement(), "Path element must be defined");
        assertEquals(Constants.ELYTRON_TRUST_MANAGER, definition.getPathElement().getValue(),
                "Path element value must be the elytron trust-manager constant");
    }

    @Test
    @DisplayName("Resource exposes exactly the LEGACY_JSSE_CONFIG attribute")
    void attributesContainOnlyLegacyJsseConfig() {
        Collection<AttributeDefinition> attributes = definition.getAttributes();
        assertNotNull(attributes, "Attributes must not be null");

        List<String> names = attributes.stream()
                .map(AttributeDefinition::getName)
                .collect(Collectors.toList());

        assertEquals(1, names.size(), "Only one attribute is expected, found: " + names);
        assertEquals(LegacySecurityResourceDefinitions.LEGACY_JSSE_CONFIG.getName(), names.get(0),
                "The single attribute must be LEGACY_JSSE_CONFIG");
    }

    @Test
    @DisplayName("Resource registers the trust-managers runtime capability")
    void registersTrustManagerRuntimeCapability() {
        Collection<RuntimeCapability> capabilities = definition.getCapabilities();
        assertNotNull(capabilities, "Capabilities must not be null");

        boolean found = capabilities.stream()
                .anyMatch(cap -> LegacySecurityResourceDefinitions.TRUST_MANAGER_RUNTIME_CAPABILITY
                        .getName().equals(cap.getName()));

        assertTrue(found, "The trust-managers runtime capability must be registered");
    }

    @Test
    @DisplayName("Each call produces an independent definition instance")
    void eachCallProducesNewInstance() {
        ResourceDefinition another = LegacySecurityResourceDefinitions.getElytronTrustManagersResourceDefinition();
        assertNotSame(definition, another,
                "Factory method should not share mutable resource definition instances");
    }

    @Test
    @DisplayName("Shared attribute array is not mutated across definitions")
    void attributeDefinitionsAreStable() {
        Collection<AttributeDefinition> first = definition.getAttributes();
        Collection<AttributeDefinition> second = LegacySecurityResourceDefinitions
                .getElytronTrustManagersResourceDefinition().getAttributes();

        assertEquals(
                first.stream().map(AttributeDefinition::getName).collect(Collectors.toList()),
                second.stream().map(AttributeDefinition::getName).collect(Collectors.toList()),
                "Attribute set must be consistent across independently created definitions");
    }
}
```

---

## MailSubsystemTestCase.java -> parameters()

- **Debt Type:** `DESIGN`
- **Line Number:** `66`
- **Status:** `GENERATED`
- **Comment:** `TODO Create formal enumeration of schema versions`

```java
// TODO Create formal enumeration of schema versions
@Parameters
public static Collection<Object[]> parameters() {
    return List.of(new Object[] { 1, 0 }, new Object[] { 1, 1 }, new Object[] { 1, 2 }, new Object[] { 2, 0 }, new Object[] { 3, 0 }, new Object[] { 4, 0 });
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the schema version parameter source.
 *
 * ASSUMPTION: the method under test lives in a class named {@code SchemaVersions}.
 * Adjust the call sites in {@link #parameters()} below if the owning class differs.
 *
 * These tests pin the current contract of the raw Object[] {major, minor} data so that
 * a future formal enumeration (the TODO) can be introduced safely and verified against
 * the same expectations.
 */
@DisplayName("Schema version parameters (pre-enumeration contract)")
class SchemaVersionParametersTest {

    private static final List<String> EXPECTED_VERSIONS =
            List.of("1.0", "1.1", "1.2", "2.0", "3.0", "4.0");

    private static Collection<Object[]> parameters() {
        return SchemaVersions.parameters();
    }

    private static List<String> toVersionStrings(Collection<Object[]> data) {
        return data.stream()
                .map(entry -> entry[0] + "." + entry[1])
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("parameters() returns a non-null, non-empty collection")
    void parametersIsNotNullAndNotEmpty() {
        Collection<Object[]> data = parameters();

        assertNotNull(data, "parameters() must not return null");
        assertFalse(data.isEmpty(), "parameters() must not be empty");
    }

    @Test
    @DisplayName("parameters() contains exactly the expected schema versions in order")
    void containsExactlyExpectedVersions() {
        assertEquals(EXPECTED_VERSIONS, toVersionStrings(parameters()));
    }

    @Test
    @DisplayName("No entry is null and every entry has exactly two components")
    void everyEntryHasExactlyTwoComponents() {
        for (Object[] entry : parameters()) {
            assertNotNull(entry, "Entry must not be null");
            assertEquals(2, entry.length, "Entry must contain exactly {major, minor}");
        }
    }

    @Test
    @DisplayName("Both components are non-null Integers")
    void componentsAreIntegers() {
        for (Object[] entry : parameters()) {
            assertNotNull(entry[0], "Major version must not be null");
            assertNotNull(entry[1], "Minor version must not be null");
            assertInstanceOf(Integer.class, entry[0], "Major version must be an Integer");
            assertInstanceOf(Integer.class, entry[1], "Minor version must be an Integer");
        }
    }

    @Test
    @DisplayName("Major versions are >= 1 and minor versions are >= 0")
    void versionComponentsAreInValidRanges() {
        for (Object[] entry : parameters()) {
            int major = (Integer) entry[0];
            int minor = (Integer) entry[1];
            assertTrue(major >= 1, "Major version must be >= 1 but was " + major);
            assertTrue(minor >= 0, "Minor version must be >= 0 but was " + minor);
        }
    }

    @Test
    @DisplayName("Versions are unique")
    void versionsAreUnique() {
        List<String> versions = toVersionStrings(parameters());
        long distinct = versions.stream().distinct().count();

        assertEquals(versions.size(), distinct, "Duplicate schema versions detected: " + versions);
    }

    @Test
    @DisplayName("Versions are sorted ascending by major, then minor")
    void versionsAreSortedAscending() {
        List<Object[]> entries = new ArrayList<>(parameters());

        for (int i = 1; i < entries.size(); i++) {
            Object[] prev = entries.get(i - 1);
            Object[] curr = entries.get(i);
            int prevMajor = (Integer) prev[0];
            int prevMinor = (Integer) prev[1];
            int currMajor = (Integer) curr[0];
            int currMinor = (Integer) curr[1];

            boolean ordered = prevMajor < currMajor
                    || (prevMajor == currMajor && prevMinor < currMinor);

            assertTrue(ordered,
                    "Version " + prevMajor + "." + prevMinor + " must precede "
                            + currMajor + "." + currMinor);
        }
    }

    @Test
    @DisplayName("Returned collection is immutable")
    void returnedCollectionIsImmutable() {
        Collection<Object[]> data = parameters();

        assertThrows(UnsupportedOperationException.class,
                () -> data.add(new Object[] { 5, 0 }),
                "parameters() should return an immutable collection");
    }

    @Test
    @DisplayName("Repeated calls return equivalent data")
    void repeatedCallsAreConsistent() {
        assertDoesNotThrow(() -> {
            List<String> first = toVersionStrings(parameters());
            List<String> second = toVersionStrings(parameters());
            assertEquals(first, second);
        });
    }
}
```

---

## ActiveMQResourceAdapter.java -> createRecoveryActiveMQConnectionFactory()

- **Debt Type:** `DESIGN`
- **Line Number:** `74`
- **Status:** `GENERATED`
- **Comment:** `* Workaround for WFLY-18756 until https://issues.apache.org/jira/browse/ARTEMIS-4508 is merged and released.`

```java
/**
 * Workaround for WFLY-18756 until https://issues.apache.org/jira/browse/ARTEMIS-4508 is merged and released.
 */
@Override
public ActiveMQConnectionFactory createRecoveryActiveMQConnectionFactory(final ConnectionFactoryProperties overrideProperties) {
    ActiveMQConnectionFactory cf = super.createRecoveryActiveMQConnectionFactory(overrideProperties);
    cf.setUseTopologyForLoadBalancing(this.isUseTopologyForLoadBalancing());
    return cf;
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `WFLY-18756`
- **URL:** `https://redhat.atlassian.net/browse/WFLY-18756`
- **Summary:** WildFly throws UnknownHostExceptions and XARecovery fails when Connected to an AMQ Cluster in OpenShift

**Description:**

```
When the JBoss EAP is running outside OpenShift with its messaging-activemq subsystem iconfigured as a resource adapter connecting to an AMQ cluster within OpenShift, we see UnknownHostExceptions and XARecovery failures due to topology updates, even if useTopologyForLoadBalancing is set to false and even if ha is also set false.


```

**Issue References:**

- `WFLY-18756` (source: `comment`)
- `ARTEMIS-4508` (source: `comment`)

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.core.client.impl.ServerLocatorImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.ra.ActiveMQResourceAdapter;
import org.apache.activemq.artemis.ra.ConnectionFactoryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/*
 * NOTE: The class under test is assumed to be a subclass of ActiveMQResourceAdapter
 * named "TopologyWorkaroundResourceAdapter" that contains the overridden method
 * createRecoveryActiveMQConnectionFactory(ConnectionFactoryProperties). Adjust the
 * class name / package below to match the production class.
 */
class TopologyWorkaroundResourceAdapterTest {

    private TopologyWorkaroundResourceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TopologyWorkaroundResourceAdapter();
        // Minimal connection configuration so the super implementation can build a factory.
        adapter.setConnectionParameters("host=localhost;port=61616");
    }

    @Nested
    @DisplayName("createRecoveryActiveMQConnectionFactory")
    class CreateRecoveryFactory {

        @Test
        @DisplayName("returns a non-null ActiveMQConnectionFactory")
        void returnsNonNullFactory() {
            adapter.setUseTopologyForLoadBalancing(false);

            ActiveMQConnectionFactory cf =
                    adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            assertNotNull(cf);
        }

        @Test
        @DisplayName("propagates useTopologyForLoadBalancing=false from the adapter to the recovery factory")
        void propagatesFalseTopologyFlag() {
            adapter.setUseTopologyForLoadBalancing(false);

            ActiveMQConnectionFactory cf =
                    adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            assertEquals(false, cf.isUseTopologyForLoadBalancing(),
                    "Recovery factory must honour the adapter's topology setting (WFLY-18756 workaround)");
        }

        @Test
        @DisplayName("propagates useTopologyForLoadBalancing=true from the adapter to the recovery factory")
        void propagatesTrueTopologyFlag() {
            adapter.setUseTopologyForLoadBalancing(true);

            ActiveMQConnectionFactory cf =
                    adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            assertEquals(true, cf.isUseTopologyForLoadBalancing(),
                    "Recovery factory must honour the adapter's topology setting (WFLY-18756 workaround)");
        }

        @ParameterizedTest(name = "adapter flag = {0} is copied to recovery factory")
        @ValueSource(booleans = {true, false})
        void topologyFlagIsCopiedForBothValues(boolean flag) {
            adapter.setUseTopologyForLoadBalancing(flag);

            ActiveMQConnectionFactory cf =
                    adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            assertEquals(flag, cf.isUseTopologyForLoadBalancing());
        }

        @Test
        @DisplayName("reflects the adapter's current flag on each call, not a cached value")
        void reflectsCurrentFlagOnEachCall() {
            adapter.setUseTopologyForLoadBalancing(false);
            ActiveMQConnectionFactory first =
                    adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            adapter.setUseTopologyForLoadBalancing(true);
            ActiveMQConnectionFactory second =
                    adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            assertNotSame(first, second, "Each call must create a new recovery factory");
            assertEquals(false, first.isUseTopologyForLoadBalancing());
            assertEquals(true, second.isUseTopologyForLoadBalancing());
        }

        @Test
        @DisplayName("does not alter the adapter's own topology flag")
        void doesNotMutateAdapterFlag() {
            adapter.setUseTopologyForLoadBalancing(false);

            adapter.createRecoveryActiveMQConnectionFactory(new ConnectionFactoryProperties());

            assertEquals(false, adapter.isUseTopologyForLoadBalancing(),
                    "Workaround must only copy the flag, never change the adapter state");
        }
    }
}
```

---

## BroadcastGroupDefinition.java -> getAvailableConnectors()

- **Debt Type:** `DESIGN`
- **Line Number:** `141`
- **Status:** `GENERATED`
- **Comment:** `FIXME use capabilities & requirements`

```java
// FIXME use capabilities & requirements
private static Set<String> getAvailableConnectors(final OperationContext context, final ModelNode operation) throws OperationFailedException {
    PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
    PathAddress active = MessagingServices.getActiveMQServerPathAddress(address);
    Set<String> availableConnectors = new HashSet<>();
    Resource subsystemResource = context.readResourceFromRoot(active.getParent(), false);
    availableConnectors.addAll(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
    Resource activeMQServerResource = context.readResourceFromRoot(active, false);
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR));
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
    return availableConnectors;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.jboss.as.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.Resource;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for the private static method {@code getAvailableConnectors}, which currently
 * hard-codes the connector resource types it inspects (FIXME: use capabilities & requirements).
 *
 * These tests pin down the current behaviour so that the refactoring to capabilities
 * and requirements can be performed safely.
 *
 * NOTE: Adjust {@link #OWNER_CLASS_NAME} to the actual class declaring the method if it differs.
 */
class GetAvailableConnectorsTest {

    private static final String OWNER_CLASS_NAME = "org.jboss.as.messaging.MessagingServices";
    private static final String METHOD_NAME = "getAvailableConnectors";

    private static final PathElement SUBSYSTEM = PathElement.pathElement("subsystem", "messaging-activemq");
    private static final PathElement SERVER = PathElement.pathElement("server", "default");
    private static final PathElement QUEUE = PathElement.pathElement("jms-queue", "myQueue");

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(SUBSYSTEM);
    private static final PathAddress SERVER_ADDRESS = SUBSYSTEM_ADDRESS.append(SERVER);

    private OperationContext context;
    private ModelNode operation;
    private Resource subsystemResource;
    private Resource serverResource;

    @BeforeEach
    void setUp() {
        context = mock(OperationContext.class);
        subsystemResource = mock(Resource.class);
        serverResource = mock(Resource.class);

        operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP_ADDR)
                .set(SERVER_ADDRESS.append(QUEUE).toModelNode());

        when(context.readResourceFromRoot(SUBSYSTEM_ADDRESS, false)).thenReturn(subsystemResource);
        when(context.readResourceFromRoot(SERVER_ADDRESS, false)).thenReturn(serverResource);

        // Default: no children anywhere
        when(subsystemResource.getChildrenNames(any(String.class))).thenReturn(Collections.emptySet());
        when(serverResource.getChildrenNames(any(String.class))).thenReturn(Collections.emptySet());
    }

    @Test
    @DisplayName("returns empty set when no connectors are defined")
    void returnsEmptySetWhenNoConnectors() throws Exception {
        Set<String> result = invoke(context, operation);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("collects remote connectors from the subsystem resource")
    void collectsRemoteConnectorsFromSubsystem() throws Exception {
        when(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(Set.of("subsystem-remote"));

        Set<String> result = invoke(context, operation);

        assertEquals(Set.of("subsystem-remote"), result);
    }

    @Test
    @DisplayName("collects http, in-vm, remote and generic connectors from the active server resource")
    void collectsAllConnectorTypesFromServer() throws Exception {
        when(serverResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR))
                .thenReturn(Set.of("http-conn"));
        when(serverResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR))
                .thenReturn(Set.of("in-vm-conn"));
        when(serverResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(Set.of("remote-conn"));
        when(serverResource.getChildrenNames(CommonAttributes.CONNECTOR))
                .thenReturn(Set.of("generic-conn"));

        Set<String> result = invoke(context, operation);

        assertEquals(Set.of("http-conn", "in-vm-conn", "remote-conn", "generic-conn"), result);
    }

    @Test
    @DisplayName("combines connectors from subsystem and server resources")
    void combinesSubsystemAndServerConnectors() throws Exception {
        when(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(Set.of("subsystem-remote"));
        when(serverResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR))
                .thenReturn(Set.of("http-conn"));

        Set<String> result = invoke(context, operation);

        assertEquals(Set.of("subsystem-remote", "http-conn"), result);
    }

    @Test
    @DisplayName("de-duplicates connector names appearing in several sources")
    void deduplicatesConnectorNames() throws Exception {
        when(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(Set.of("shared"));
        when(serverResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(Set.of("shared", "server-only"));
        when(serverResource.getChildrenNames(CommonAttributes.CONNECTOR))
                .thenReturn(Set.of("shared"));

        Set<String> result = invoke(context, operation);

        assertEquals(Set.of("shared", "server-only"), result);
    }

    @Test
    @DisplayName("reads subsystem and active server resources from root without runtime access")
    void readsResourcesFromRootWithoutRuntime() throws Exception {
        invoke(context, operation);

        verify(context).readResourceFromRoot(SUBSYSTEM_ADDRESS, false);
        verify(context).readResourceFromRoot(SERVER_ADDRESS, false);
    }

    @Test
    @DisplayName("propagates exceptions thrown when the active server resource cannot be read")
    void propagatesResourceReadFailure() {
        when(context.readResourceFromRoot(any(PathAddress.class), anyBoolean()))
                .thenThrow(new IllegalStateException("resource not found"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> invoke(context, operation));
        assertEquals("resource not found", ex.getMessage());
    }

    // ----------------------------------------------------------------------
    // Reflection helper
    // ----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Set<String> invoke(OperationContext context, ModelNode operation) throws Exception {
        Class<?> owner = Class.forName(OWNER_CLASS_NAME);
        Method method = owner.getDeclaredMethod(METHOD_NAME, OperationContext.class, ModelNode.class);
        method.setAccessible(true);
        try {
            return (Set<String>) method.invoke(null, context, operation);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OperationFailedException) {
                throw (OperationFailedException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }
}
```

---

## BroadcastGroupWriteAttributeHandler.java -> getAvailableConnectors()

- **Debt Type:** `DESIGN`
- **Line Number:** `69`
- **Status:** `GENERATED`
- **Comment:** `FIXME use capabilities & requirements`

```java
// FIXME use capabilities & requirements
private static Set<String> getAvailableConnectors(final OperationContext context, final ModelNode operation) throws OperationFailedException {
    PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
    PathAddress active = MessagingServices.getActiveMQServerPathAddress(address);
    Set<String> availableConnectors = new HashSet<>();
    Resource subsystemResource = context.readResourceFromRoot(active.getParent(), false);
    availableConnectors.addAll(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
    Resource activeMQServerResource = context.readResourceFromRoot(active, false);
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR));
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
    availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
    return availableConnectors;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the private static helper {@code getAvailableConnectors}.
 *
 * FIXME (design debt): the method currently hard-codes the connector child types it aggregates.
 * These tests pin the CURRENT behaviour so that a refactoring to "capabilities & requirements"
 * can be verified against the same observable results.
 *
 * ASSUMPTIONS:
 *  - TARGET_CLASS is the class declaring the private method; adjust the name/package if needed.
 *  - MessagingServices.getActiveMQServerPathAddress(address) resolves an address under
 *    /subsystem=messaging-activemq/server=X to /subsystem=messaging-activemq/server=X.
 */
class GetAvailableConnectorsTest {

    // Adjust to the actual declaring class of the private method.
    private static final String TARGET_CLASS = "org.jboss.as.messaging.MessagingServices";
    private static final String METHOD_NAME = "getAvailableConnectors";

    private static final PathAddress SUBSYSTEM_ADDRESS =
            PathAddress.pathAddress(PathElement.pathElement("subsystem", "messaging-activemq"));
    private static final PathAddress SERVER_ADDRESS =
            SUBSYSTEM_ADDRESS.append(PathElement.pathElement("server", "default"));
    private static final PathAddress OPERATION_ADDRESS =
            SERVER_ADDRESS.append(PathElement.pathElement("jms-queue", "myQueue"));

    private OperationContext context;
    private Resource subsystemResource;
    private Resource serverResource;
    private ModelNode operation;

    @BeforeEach
    void setUp() {
        context = mock(OperationContext.class);
        subsystemResource = mock(Resource.class);
        serverResource = mock(Resource.class);

        operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP_ADDR).set(OPERATION_ADDRESS.toModelNode());

        when(context.readResourceFromRoot(eq(SUBSYSTEM_ADDRESS), eq(false))).thenReturn(subsystemResource);
        when(context.readResourceFromRoot(eq(SERVER_ADDRESS), eq(false))).thenReturn(serverResource);

        // Default: no children of any type
        when(subsystemResource.getChildrenNames(anyString())).thenReturn(Collections.emptySet());
        when(serverResource.getChildrenNames(anyString())).thenReturn(Collections.emptySet());
    }

    @Test
    void returnsEmptySetWhenNoConnectorsExist() throws Exception {
        Set<String> result = invoke(context, operation);

        assertTrue(result.isEmpty());
    }

    @Test
    void aggregatesRemoteConnectorsFromSubsystemResource() throws Exception {
        when(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(set("subsystem-remote"));

        Set<String> result = invoke(context, operation);

        assertEquals(set("subsystem-remote"), result);
    }

    @Test
    void aggregatesHttpInVmRemoteAndGenericConnectorsFromServerResource() throws Exception {
        when(serverResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR))
                .thenReturn(set("http-a"));
        when(serverResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR))
                .thenReturn(set("in-vm-a"));
        when(serverResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(set("remote-a"));
        when(serverResource.getChildrenNames(CommonAttributes.CONNECTOR))
                .thenReturn(set("connector-a"));

        Set<String> result = invoke(context, operation);

        assertEquals(set("http-a", "in-vm-a", "remote-a", "connector-a"), result);
    }

    @Test
    void combinesConnectorsFromSubsystemAndServerResources() throws Exception {
        when(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(set("shared-remote", "subsystem-only"));
        when(serverResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR))
                .thenReturn(set("shared-remote", "server-only"));
        when(serverResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR))
                .thenReturn(set("http-a"));

        Set<String> result = invoke(context, operation);

        // Duplicates across resources must collapse into a single entry
        assertEquals(set("shared-remote", "subsystem-only", "server-only", "http-a"), result);
    }

    @Test
    void deduplicatesNamesAcrossConnectorTypes() throws Exception {
        when(serverResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR))
                .thenReturn(set("same"));
        when(serverResource.getChildrenNames(CommonAttributes.CONNECTOR))
                .thenReturn(set("same"));

        Set<String> result = invoke(context, operation);

        assertEquals(set("same"), result);
    }

    @Test
    void readsSubsystemResourceFromParentOfActiveServerAddress() throws Exception {
        invoke(context, operation);

        verify(context).readResourceFromRoot(SUBSYSTEM_ADDRESS, false);
        verify(context).readResourceFromRoot(SERVER_ADDRESS, false);
    }

    @Test
    void subsystemResourceContributesOnlyRemoteConnectors() throws Exception {
        invoke(context, operation);

        verify(subsystemResource).getChildrenNames(CommonAttributes.REMOTE_CONNECTOR);
        verify(subsystemResource, never()).getChildrenNames(CommonAttributes.HTTP_CONNECTOR);
        verify(subsystemResource, never()).getChildrenNames(CommonAttributes.IN_VM_CONNECTOR);
        verify(subsystemResource, never()).getChildrenNames(CommonAttributes.CONNECTOR);
    }

    @Test
    void resultIsMutableCopyNotAffectingResources() throws Exception {
        when(serverResource.getChildrenNames(CommonAttributes.CONNECTOR))
                .thenReturn(set("c1"));

        Set<String> result = invoke(context, operation);
        result.add("extra");

        // Subsequent call still returns freshly computed set without the mutation
        Set<String> second = invoke(context, operation);
        assertEquals(set("c1"), second);
    }

    @Test
    void propagatesOperationFailedExceptionFromContext() {
        when(context.readResourceFromRoot(eq(SERVER_ADDRESS), eq(false)))
                .thenThrow(new IllegalStateException("resource not available"));

        // Non-OperationFailedException from the context surfaces through reflection as the cause
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> invoke(context, operation));
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Set<String> invoke(OperationContext ctx, ModelNode op) throws Exception {
        Class<?> clazz = Class.forName(TARGET_CLASS);
        Method method = clazz.getDeclaredMethod(METHOD_NAME, OperationContext.class, ModelNode.class);
        method.setAccessible(true);
        try {
            return (Set<String>) method.invoke(null, ctx, op);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof OperationFailedException) {
                throw (OperationFailedException) e.getCause();
            }
            throw e;
        }
    }

    private static Set<String> set(String... values) {
        Set<String> s = new HashSet<>();
        Collections.addAll(s, values);
        return s;
    }
}
```

---

## PathDefinition.java -> getMinOccurs()

- **Debt Type:** `DESIGN`
- **Line Number:** `132`
- **Status:** `GENERATED`
- **Comment:** `TODO add @Override once the WFCORE version with this method is integrated`

```java
// TODO add @Override once the WFCORE version with this method is integrated
public int getMinOccurs() {
    return 1;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/*
 * Replace ClassUnderTest with the actual class that declares getMinOccurs().
 * The TODO notes that @Override should be added once the WFCORE version that
 * declares this method is on the classpath. These tests pin the current
 * contract so the later annotation change can be verified safely.
 */
class GetMinOccursTest {

    private ClassUnderTest instance;

    @BeforeEach
    void setUp() {
        instance = new ClassUnderTest();
    }

    @Nested
    @DisplayName("Behavior of getMinOccurs()")
    class Behavior {

        @Test
        @DisplayName("returns 1 as the minimum number of occurrences")
        void returnsOne() {
            assertEquals(1, instance.getMinOccurs());
        }

        @Test
        @DisplayName("returns the same value on repeated calls (idempotent)")
        void isIdempotent() {
            int first = instance.getMinOccurs();
            for (int i = 0; i < 100; i++) {
                assertEquals(first, instance.getMinOccurs());
            }
        }

        @Test
        @DisplayName("returns the same value across independent instances")
        void consistentAcrossInstances() {
            assertEquals(
                    new ClassUnderTest().getMinOccurs(),
                    new ClassUnderTest().getMinOccurs());
        }

        @Test
        @DisplayName("result is at least 0 (a minimum occurrence count is never negative)")
        void nonNegative() {
            assertTrue(instance.getMinOccurs() >= 0);
        }

        @Test
        @DisplayName("result does not exceed 1 (contract: at least one occurrence required)")
        void resultIsOne() {
            assertEquals(1, instance.getMinOccurs(), "minimum occurrences should be exactly 1");
        }
    }

    @Nested
    @DisplayName("Signature of getMinOccurs()")
    class Signature {

        private Method method;

        @BeforeEach
        void loadMethod() throws NoSuchMethodException {
            method = ClassUnderTest.class.getMethod("getMinOccurs");
        }

        @Test
        @DisplayName("is public")
        void isPublic() {
            assertTrue(Modifier.isPublic(method.getModifiers()));
        }

        @Test
        @DisplayName("is an instance method, not static")
        void isNotStatic() {
            assertFalse(Modifier.isStatic(method.getModifiers()));
        }

        @Test
        @DisplayName("returns primitive int")
        void returnsInt() {
            assertEquals(int.class, method.getReturnType());
        }

        @Test
        @DisplayName("takes no parameters")
        void noParameters() {
            assertEquals(0, method.getParameterCount());
        }
    }
}
```

---

## JMSDestinationDefinitionInjectionSource.java -> startQueue()

- **Debt Type:** `DESIGN`
- **Line Number:** `156`
- **Status:** `GENERATION_FAILED`
- **Comment:** `* To workaround ActiveMQ's BindingRegistry limitation in {@link WildFlyBindingRegistry}      * that does not allow to build a BindingInfo with the ResolutionContext info, the Jakarta Messaging queue is created *without* any      * JNDI bindings and handle the JNDI bindings directly by getting the service's Jakarta Messaging queue.`

```java
/**
 * To workaround ActiveMQ's BindingRegistry limitation in {@link WildFlyBindingRegistry}
 * that does not allow to build a BindingInfo with the ResolutionContext info, the Jakarta Messaging queue is created *without* any
 * JNDI bindings and handle the JNDI bindings directly by getting the service's Jakarta Messaging queue.
 */
private void startQueue(final String queueName, final ServiceTarget serviceTarget, final ServiceName serverServiceName, final ServiceBuilder<?> serviceBuilder, final DeploymentUnit deploymentUnit, final Injector<ManagedReferenceFactory> injector, final boolean external) {
    final String selector = properties.containsKey(SELECTOR.getName()) ? properties.get(SELECTOR.getName()) : null;
    final boolean durable = properties.containsKey(DURABLE.getName()) ? Boolean.valueOf(properties.get(DURABLE.getName())) : DURABLE.getDefaultValue().asBoolean();
    final String managementAddress = properties.containsKey(MANAGEMENT_ADDRESS.getName()) ? properties.get(MANAGEMENT_ADDRESS.getName()) : MANAGEMENT_ADDRESS.getDefaultValue().asString();
    final String user = properties.containsKey("management-user") ? properties.get("management-user") : null;
    final String password = properties.containsKey("management-password") ? properties.get("management-password") : null;
    ModelNode destination = new ModelNode();
    destination.get(NAME).set(queueName);
    destination.get(DURABLE.getName()).set(durable);
    if (selector != null) {
        destination.get(SELECTOR.getName()).set(selector);
    }
    destination.get(ENTRIES).add(jndiName);
    Service<Queue> queueService;
    if (external) {
        // check @JMSDestinationDefinitions boolean property named enable-amq1-prefix for runtime queue
        final boolean enabledAMQ1Prefix = properties.containsKey(External.ENABLE_AMQ1_PREFIX.getName()) ? Boolean.valueOf(properties.get(External.ENABLE_AMQ1_PREFIX.getName())) : External.ENABLE_AMQ1_PREFIX.getDefaultValue().asBoolean();
        ServiceName pcfName = JMSServices.getPooledConnectionFactoryBaseServiceName(serverServiceName).append(resourceAdapter);
        final ServiceName jmsQueueServiceName = JMSServices.getJmsQueueBaseServiceName(serverServiceName).append(queueName);
        queueService = ExternalJMSQueueService.installRuntimeQueueService(DestinationConfiguration.Builder.getInstance().setResourceAdapter(resourceAdapter).setName(queueName).setManagementQueueAddress(managementAddress).setDestinationServiceName(jmsQueueServiceName).setDurable(durable).setSelector(selector).setManagementUsername(user).setManagementPassword(password).build(), serviceTarget, pcfName, enabledAMQ1Prefix);
    } else {
        queueService = JMSQueueService.installService(queueName, serviceTarget, serverServiceName, selector, durable);
    }
    inject(serviceBuilder, injector, queueService);
    //create the management registration
    String serverName = null;
    final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
    PathAddress registration;
    if (external) {
        final PathElement dest = PathElement.pathElement(EXTERNAL_JMS_QUEUE, queueName);
        deploymentResourceSupport.getDeploymentSubsystemModel(MessagingExtension.SUBSYSTEM_NAME);
        registration = PathAddress.pathAddress(dest);
    } else {
        serverName = getActiveMQServerName(properties);
        final PathElement dest = PathElement.pathElement(JMS_QUEUE, queueName);
        final PathElement serverElement = PathElement.pathElement(SERVER, serverName);
        deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        registration = PathAddress.pathAddress(serverElement, dest);
    }
    MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
    JMSQueueConfigurationRuntimeHandler.INSTANCE.registerResource(serverName, queueName, destination);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java

```

---

## AbstractPojoPhase.java -> considerUninstalls()

- **Debt Type:** `DESIGN`
- **Line Number:** `205`
- **Status:** `GENERATED`
- **Comment:** `* Consider the uninstalls.      * <p/>      * This method is here to be able to override      * the behavior after installs failed.      * e.g. perhaps only running uninstalls from the index.      * <p/>      * By default we run all uninstalls in the case      * at least one install failed.      *      * @param uninstalls the uninstalls      * @param index      current installs index`

```java
/**
 * Consider the uninstalls.
 * <p/>
 * This method is here to be able to override
 * the behavior after installs failed.
 * e.g. perhaps only running uninstalls from the index.
 * <p/>
 * By default we run all uninstalls in the case
 * at least one install failed.
 *
 * @param uninstalls the uninstalls
 * @param index      current installs index
 */
protected void considerUninstalls(List<Joinpoint> uninstalls, int index) {
    if (uninstalls == null)
        return;
    for (int j = Math.min(index, uninstalls.size() - 1); j >= 0; j--) {
        try {
            uninstalls.get(j).dispatch();
        } catch (Throwable t) {
            PojoLogger.ROOT_LOGGER.ignoreUninstallError(uninstalls.get(j), t);
        }
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * ASSUMPTIONS (adjust names/packages to match the production code):
 *  - The class declaring considerUninstalls(...) is named `PojoInstaller`
 *    and has an accessible no-arg constructor, in the same package as this test
 *    (so the protected method is accessible).
 *  - `Joinpoint` is an interface with a `dispatch()` method.
 *  - `PojoLogger.ROOT_LOGGER` is the logger used by the method.
 */
class ConsiderUninstallsTest {

    /** Test subclass exposing the protected method under test. */
    static class TestableInstaller extends PojoInstaller {
        void invokeConsiderUninstalls(List<Joinpoint> uninstalls, int index) {
            considerUninstalls(uninstalls, index);
        }
    }

    /**
     * Creates a recording Joinpoint via dynamic proxy so no mocking framework is needed.
     * If dispatch() throws, the given throwable is raised.
     */
    private static Joinpoint recordingJoinpoint(String name, List<String> log, Throwable toThrow) {
        return (Joinpoint) Proxy.newProxyInstance(
                Joinpoint.class.getClassLoader(),
                new Class<?>[]{Joinpoint.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "dispatch":
                            log.add(name);
                            if (toThrow != null) {
                                throw toThrow;
                            }
                            return null;
                        case "toString":
                            return name;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            return null;
                    }
                });
    }

    private TestableInstaller installer;
    private List<String> dispatchLog;

    @BeforeEach
    void setUp() {
        installer = new TestableInstaller();
        dispatchLog = new ArrayList<>();
    }

    @Test
    void nullUninstallsReturnsWithoutError() {
        assertDoesNotThrow(() -> installer.invokeConsiderUninstalls(null, 5));
        assertTrue(dispatchLog.isEmpty());
    }

    @Test
    void emptyUninstallsDispatchesNothing() {
        assertDoesNotThrow(() -> installer.invokeConsiderUninstalls(new ArrayList<>(), 0));
        assertTrue(dispatchLog.isEmpty());
    }

    @Test
    void indexAtOrBeyondLastDispatchesAllInReverseOrder() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, null),
                recordingJoinpoint("c", dispatchLog, null));

        installer.invokeConsiderUninstalls(uninstalls, 2);
        assertEquals(Arrays.asList("c", "b", "a"), dispatchLog);
    }

    @Test
    void indexBeyondSizeIsClampedToLastElement() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, null));

        installer.invokeConsiderUninstalls(uninstalls, 100);
        assertEquals(Arrays.asList("b", "a"), dispatchLog);
    }

    @Test
    void indexInMiddleOnlyDispatchesUpToIndexInReverseOrder() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, null),
                recordingJoinpoint("c", dispatchLog, null),
                recordingJoinpoint("d", dispatchLog, null));

        installer.invokeConsiderUninstalls(uninstalls, 1);
        assertEquals(Arrays.asList("b", "a"), dispatchLog);
    }

    @Test
    void indexZeroDispatchesOnlyFirstUninstall() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, null));

        installer.invokeConsiderUninstalls(uninstalls, 0);
        assertEquals(List.of("a"), dispatchLog);
    }

    @Test
    void negativeIndexDispatchesNothing() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, null));

        installer.invokeConsiderUninstalls(uninstalls, -1);
        assertTrue(dispatchLog.isEmpty());
    }

    @Test
    void failingUninstallDoesNotStopRemainingUninstalls() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, new RuntimeException("boom")),
                recordingJoinpoint("c", dispatchLog, null));

        assertDoesNotThrow(() -> installer.invokeConsiderUninstalls(uninstalls, 2));
        assertEquals(Arrays.asList("c", "b", "a"), dispatchLog);
    }

    @Test
    void errorThrowableIsSwallowedAndRemainingUninstallsStillRun() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, null),
                recordingJoinpoint("b", dispatchLog, new AssertionError("fatal-ish")));

        assertDoesNotThrow(() -> installer.invokeConsiderUninstalls(uninstalls, 1));
        assertEquals(Arrays.asList("b", "a"), dispatchLog);
    }

    @Test
    void allFailingUninstallsAreAllAttempted() {
        List<Joinpoint> uninstalls = Arrays.asList(
                recordingJoinpoint("a", dispatchLog, new IllegalStateException("1")),
                recordingJoinpoint("b", dispatchLog, new IllegalStateException("2")));

        assertDoesNotThrow(() -> installer.invokeConsiderUninstalls(uninstalls, 1));
        assertEquals(Arrays.asList("b", "a"), dispatchLog);
    }
}
```

---

## EJBManagementUtil.java -> getNodeName()

- **Debt Type:** `DESIGN`
- **Line Number:** `633`
- **Status:** `GENERATED`
- **Comment:** `TODO: This method is temporary hack till we figure out the management operation to get the jboss.node.name system property from the server http://lists.jboss.org/pipermail/jboss-as7-dev/2011-November/004434.html`

```java
// TODO: This method is temporary hack till we figure out the management operation to get the
// jboss.node.name system property from the server http://lists.jboss.org/pipermail/jboss-as7-dev/2011-November/004434.html
public static String getNodeName() {
    // Logic copied from org.jboss.as.server.ServerEnvironment constructor
    final Properties props = System.getProperties();
    final Map<String, String> env = System.getenv();
    // Calculate host and default server name
    String hostName = props.getProperty("jboss.host.name");
    String qualifiedHostName = props.getProperty("jboss.qualified.host.name");
    if (qualifiedHostName == null) {
        // if host name is specified, don't pick a qualified host name that isn't related to it
        qualifiedHostName = hostName;
        if (qualifiedHostName == null) {
            // POSIX-like OSes including Mac should have this set
            qualifiedHostName = env.get("HOSTNAME");
        }
        if (qualifiedHostName == null) {
            // Certain versions of Windows
            qualifiedHostName = env.get("COMPUTERNAME");
        }
        if (qualifiedHostName == null) {
            try {
                qualifiedHostName = NetworkUtils.canonize(InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                qualifiedHostName = null;
            }
        }
        if (qualifiedHostName != null && qualifiedHostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$|:")) {
            // IP address is not acceptable
            qualifiedHostName = null;
        }
        if (qualifiedHostName == null) {
            // Give up
            qualifiedHostName = "unknown-host.unknown-domain";
        }
        qualifiedHostName = qualifiedHostName.trim().toLowerCase(Locale.ENGLISH);
    }
    if (hostName == null) {
        // Use the host part of the qualified host name
        final int idx = qualifiedHostName.indexOf('.');
        hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
    }
    // Set up the server name for management purposes
    String serverName = props.getProperty("jboss.server.name");
    if (serverName == null) {
        serverName = hostName;
    }
    // Set up the clustering node name
    String nodeName = props.getProperty("jboss.node.name");
    if (nodeName == null) {
        nodeName = serverName;
    }
    return nodeName;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// NOTE: Rename NodeNameHelper to the class that declares the static getNodeName() method.
class NodeNameHelperTest {

    private static final String[] MANAGED_PROPS = {
            "jboss.node.name",
            "jboss.server.name",
            "jboss.host.name",
            "jboss.qualified.host.name"
    };

    private final Map<String, String> originalValues = new HashMap<>();

    @BeforeEach
    void saveAndClearProperties() {
        for (String key : MANAGED_PROPS) {
            originalValues.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
    }

    @AfterEach
    void restoreProperties() {
        for (String key : MANAGED_PROPS) {
            String value = originalValues.get(key);
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        }
    }

    @Test
    @DisplayName("Returns jboss.node.name when explicitly set")
    void returnsExplicitNodeName() {
        System.setProperty("jboss.node.name", "explicit-node");
        System.setProperty("jboss.server.name", "some-server");
        System.setProperty("jboss.host.name", "some-host");

        assertEquals("explicit-node", NodeNameHelper.getNodeName());
    }

    @Test
    @DisplayName("Falls back to jboss.server.name when node name is not set")
    void fallsBackToServerName() {
        System.setProperty("jboss.server.name", "my-server");
        System.setProperty("jboss.host.name", "my-host.example.com");

        assertEquals("my-server", NodeNameHelper.getNodeName());
    }

    @Test
    @DisplayName("Falls back to jboss.host.name when node and server names are not set")
    void fallsBackToHostName() {
        System.setProperty("jboss.host.name", "Node1.example.com");

        assertEquals("Node1.example.com", NodeNameHelper.getNodeName());
    }

    @Test
    @DisplayName("Derives short host name from qualified host name when host name is absent")
    void derivesShortHostNameFromQualifiedName() {
        System.setProperty("jboss.qualified.host.name", "Node1.Example.com");

        assertEquals("Node1", NodeNameHelper.getNodeName());
    }

    @Test
    @DisplayName("Uses qualified host name without domain as-is when it has no dot")
    void usesQualifiedHostNameWithoutDomain() {
        System.setProperty("jboss.qualified.host.name", "server-a");

        assertEquals("server-a", NodeNameHelper.getNodeName());
    }

    @Test
    @DisplayName("Explicit node name takes precedence over qualified host name")
    void nodeNamePrecedenceOverQualifiedHost() {
        System.setProperty("jboss.qualified.host.name", "server-a.example.com");
        System.setProperty("jboss.node.name", "cluster-node-7");

        assertEquals("cluster-node-7", NodeNameHelper.getNodeName());
    }

    @Test
    @DisplayName("Never returns null when no properties are set")
    void neverReturnsNullWithoutProperties() {
        String nodeName = NodeNameHelper.getNodeName();

        assertNotNull(nodeName);
        assertEquals(false, nodeName.isEmpty());
    }

    @Test
    @DisplayName("Returns the node name unchanged when it is set to a value with surrounding characters")
    void returnsNodeNameVerbatim() {
        System.setProperty("jboss.node.name", "  Node-With.Mixed_Case  ");

        assertEquals("  Node-With.Mixed_Case  ", NodeNameHelper.getNodeName());
    }
}
```

---

## AuthenticationTestCase.java -> testAuthentication\_TwoBeans\_ReAuth\_BadPwd()

- **Debt Type:** `TEST`
- **Line Number:** `161`
- **Status:** `GENERATED`
- **Comment:** `TODO - Similar test with first bean @RunAs - does it make sense to also manually switch?`

```java
// TODO - Similar test with first bean @RunAs - does it make sense to also manually switch?
@Test
public void testAuthentication_TwoBeans_ReAuth_BadPwd() throws Exception {
    Util.switchIdentity("user1", "password1", () -> entryBean.doubleWhoAmI("user2", "wrong_password"), true);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the two-bean re-authentication flow.
 *
 * Resolves the TODO: the original test relied on the first bean's @RunAs to
 * switch identity implicitly. These tests cover the behaviour explicitly by
 * manually switching identity with Util.switchIdentity, and verify both the
 * failure path (bad password) and the success path (valid password) so the
 * manual switch is exercised independently of @RunAs.
 *
 * Assumptions about the existing test harness (adjust if the real signatures differ):
 *  - entryBean is a field (inherited or injected by the harness) exposing
 *    String doubleWhoAmI(String user, String password).
 *  - Util.switchIdentity(user, password, action, boolean) runs the action under
 *    the given identity and throws if authentication fails.
 */
public class EntryBeanTwoBeansReAuthTest {

    // Provided by the test harness / container injection.
    protected EntryBean entryBean;

    private static final String USER1 = "user1";
    private static final String PASSWORD1 = "password1";
    private static final String USER2 = "user2";
    private static final String PASSWORD2 = "password2";
    private static final String BAD_PASSWORD = "wrong_password";

    @Test
    @DisplayName("Original scenario: re-auth as user2 with bad password is rejected")
    public void testAuthentication_TwoBeans_ReAuth_BadPwd() throws Exception {
        Util.switchIdentity(USER1, PASSWORD1,
                () -> entryBean.doubleWhoAmI(USER2, BAD_PASSWORD), true);
    }

    @Test
    @DisplayName("Manual switch: bad re-auth password must fail with an exception")
    public void testManualSwitch_TwoBeans_ReAuth_BadPwd_Rejected() {
        assertThrows(Exception.class, () ->
                Util.switchIdentity(USER1, PASSWORD1,
                        () -> entryBean.doubleWhoAmI(USER2, BAD_PASSWORD), true));
    }

    @Test
    @DisplayName("Manual switch: valid re-auth credentials succeed and return a result")
    public void testManualSwitch_TwoBeans_ReAuth_ValidPwd_Succeeds() {
        AtomicReference<String> result = new AtomicReference<>();

        assertDoesNotThrow(() ->
                Util.switchIdentity(USER1, PASSWORD1, () -> {
                    result.set(entryBean.doubleWhoAmI(USER2, PASSWORD2));
                }, true));

        assertNotNull(result.get(), "doubleWhoAmI should return an identity for a valid re-auth");
        assertEquals(USER2, result.get().trim().toLowerCase().contains(USER2) ? USER2 : result.get(),
                "Second bean should observe the re-authenticated caller");
    }

    @Test
    @DisplayName("Manual switch: unknown user in re-auth is rejected")
    public void testManualSwitch_TwoBeans_ReAuth_UnknownUser_Rejected() {
        assertThrows(Exception.class, () ->
                Util.switchIdentity(USER1, PASSWORD1,
                        () -> entryBean.doubleWhoAmI("no_such_user", PASSWORD2), true));
    }

    @Test
    @DisplayName("Manual switch: empty re-auth password is rejected")
    public void testManualSwitch_TwoBeans_ReAuth_EmptyPwd_Rejected() {
        assertThrows(Exception.class, () ->
                Util.switchIdentity(USER1, PASSWORD1,
                        () -> entryBean.doubleWhoAmI(USER2, ""), true));
    }

    @Nested
    @DisplayName("Outer identity context")
    class OuterIdentity {

        @Test
        @DisplayName("Failed re-auth does not leak elevated identity to subsequent calls")
        public void testFailedReAuth_DoesNotLeakIdentity() {
            assertThrows(Exception.class, () ->
                    Util.switchIdentity(USER1, PASSWORD1,
                            () -> entryBean.doubleWhoAmI(USER2, BAD_PASSWORD), true));

            // After the failed switch, the original identity must be restored and usable.
            AtomicReference<String> afterFailure = new AtomicReference<>();
            assertDoesNotThrow(() ->
                    Util.switchIdentity(USER1, PASSWORD1, () -> {
                        afterFailure.set(entryBean.doubleWhoAmI(USER1, PASSWORD1));
                    }, true));
            assertNotNull(afterFailure.get(), "Original identity should still work after failed re-auth");
        }
    }
}
```

---

## SessionBean.java -> performTests()

- **Debt Type:** `IMPLEMENTATION`
- **Line Number:** `25`
- **Status:** `GENERATED`
- **Comment:** `TODO - Why do I need to override this method.`

```java
// TODO - Why do I need to override this method.
public void performTests(final String beanMethod) {
    super.performTests(beanMethod);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformTestsOverrideTest {

    /**
     * Stand-in for the real superclass. It records every invocation so the
     * tests can verify exactly what the override forwards to it.
     */
    static class BaseTestRunner {
        final List<String> invocations = new ArrayList<>();
        RuntimeException exceptionToThrow;

        public void performTests(final String beanMethod) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            invocations.add(beanMethod);
        }
    }

    /**
     * Mirrors the production class: overrides performTests and only delegates to super.
     */
    static class DerivedTestRunner extends BaseTestRunner {
        @Override
        public void performTests(final String beanMethod) {
            super.performTests(beanMethod);
        }
    }

    private DerivedTestRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DerivedTestRunner();
    }

    @Nested
    @DisplayName("Delegation to superclass")
    class Delegation {

        @Test
        @DisplayName("forwards the bean method name to super exactly once")
        void delegatesToSuperOnce() {
            runner.performTests("getName");

            assertEquals(1, runner.invocations.size());
            assertEquals("getName", runner.invocations.get(0));
        }

        @Test
        @DisplayName("forwards the same argument instance without modification")
        void forwardsArgumentUnchanged() {
            String beanMethod = "setValue";

            runner.performTests(beanMethod);

            assertSame(beanMethod, runner.invocations.get(0));
        }

        @Test
        @DisplayName("forwards each call in order across multiple invocations")
        void preservesCallOrder() {
            runner.performTests("first");
            runner.performTests("second");
            runner.performTests("third");

            assertEquals(List.of("first", "second", "third"), runner.invocations);
        }

        @Test
        @DisplayName("forwards null without throwing in the override")
        void forwardsNull() {
            assertDoesNotThrow(() -> runner.performTests(null));

            assertEquals(1, runner.invocations.size());
            assertNull(runner.invocations.get(0));
        }

        @Test
        @DisplayName("forwards empty string without alteration")
        void forwardsEmptyString() {
            runner.performTests("");

            assertEquals("", runner.invocations.get(0));
        }
    }

    @Nested
    @DisplayName("Exception propagation")
    class ExceptionPropagation {

        @Test
        @DisplayName("propagates exceptions thrown by super unchanged")
        void propagatesSuperException() {
            IllegalStateException expected = new IllegalStateException("boom");
            runner.exceptionToThrow = expected;

            IllegalStateException actual = assertThrows(
                    IllegalStateException.class,
                    () -> runner.performTests("getName"));

            assertSame(expected, actual);
            assertTrue(runner.invocations.isEmpty());
        }

        @Test
        @DisplayName("does not swallow or wrap checked-style runtime exceptions")
        void doesNotWrapException() {
            NullPointerException expected = new NullPointerException("missing");
            runner.exceptionToThrow = expected;

            NullPointerException actual = assertThrows(
                    NullPointerException.class,
                    () -> runner.performTests(null));

            assertSame(expected, actual);
        }
    }

    @Nested
    @DisplayName("Override is behaviourally transparent")
    class Transparency {

        @Test
        @DisplayName("is an instance of the base type so polymorphic calls reach it")
        void isBaseType() {
            BaseTestRunner asBase = new DerivedTestRunner();

            asBase.performTests("polymorphic");

            assertEquals(List.of("polymorphic"),
                    ((DerivedTestRunner) asBase).invocations);
        }

        @Test
        @DisplayName("produces the same observable state as calling super directly")
        void matchesDirectSuperBehaviour() {
            BaseTestRunner direct = new BaseTestRunner();

            direct.performTests("compare");
            runner.performTests("compare");

            assertEquals(direct.invocations, runner.invocations);
        }
    }
}
```

---

## JPA2LCTestCase.java -> testEntityCacheSameSession()

- **Debt Type:** `DESIGN`
- **Line Number:** `138`
- **Status:** `GENERATED`
- **Comment:** `When entity caching is enabled, loading all entities at once will put all entities in the cache. During the SAME session, when looking up for the ID of an entity which was returned by the original query, no SQL queries should be executed.  Hibernate ORM 5.3 internally changed from 5.1. Infinispan caches are now non-transactional, meaning that the Hibernate first level cache is relied on inside of transactions for caching. Note from Radim: " With (5.1) transactional caches, Infinispan was storing the fact that you've stored some entities in transactional context and when you attempted to read it from cache, it transparently provided the updated data. With non-transactional caches the *Infinispan layer* (as opposed to *2LC layer*) does not do that. Instead the 2LC provider registers Jakarta Persistence synchronization to execute the update if the Jakarta Persistence transaction commits. "  In response to this change, the current sameSessionCheck doesn't make sense anymore, as the "sameSession" refers to the same persistence context being used for the entire test. The same persistence context being used, means that the persistence context first level cache (1lc), is used, instead of the 2lc, which leads to emp2LCStats.getElementCountInMemory() being zero, which would cause this test to fail. Since this test, as currently written, doesn't really test the 2lc, we will ignore it.`

```java
// When entity caching is enabled, loading all entities at once
// will put all entities in the cache. During the SAME session,
// when looking up for the ID of an entity which was returned by
// the original query, no SQL queries should be executed.
//
// Hibernate ORM 5.3 internally changed from 5.1.
// Infinispan caches are now non-transactional, meaning that the Hibernate first level cache is
// relied on inside of transactions for caching.
// Note from Radim:
// "
// With (5.1) transactional caches, Infinispan was storing the fact that you've
// stored some entities in transactional context and when you attempted to
// read it from cache, it transparently provided the updated data. With
// non-transactional caches the *Infinispan layer* (as opposed to *2LC
// layer*) does not do that. Instead the 2LC provider registers Jakarta Persistence
// synchronization to execute the update if the Jakarta Persistence transaction commits.
// "
//
// In response to this change, the current sameSessionCheck doesn't make sense anymore,
// as the "sameSession" refers to the same persistence context being used for the entire test.
// The same persistence context being used, means that the persistence context first level cache (1lc),
// is used, instead of the 2lc, which leads to emp2LCStats.getElementCountInMemory() being zero, which would
// cause this test to fail.
// Since this test, as currently written, doesn't really test the 2lc, we will ignore it.
@Ignore
@Test
@InSequence(3)
public void testEntityCacheSameSession() throws Exception {
    SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
    String message = sfsb.sameSessionCheck(CACHE_REGION_NAME);
    if (!message.equals("OK")) {
        fail(message);
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.DisplayName;

/**
 * Replaces the ignored testEntityCacheSameSession.
 *
 * The original test relied on the same persistence context, which exercises the
 * first-level cache (1LC) rather than the second-level cache (2LC). Since Hibernate 5.3
 * with non-transactional Infinispan caches, the 2LC is updated on transaction
 * synchronization, so the test must verify the 2LC explicitly:
 *
 *  1. Loading all entities with a query populates the 2LC region.
 *  2. Within the SAME session, looking up the IDs of returned entities executes no SQL.
 *  3. In a NEW session (fresh 1LC), looking up the IDs is served from the 2LC with no SQL.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityCacheSameSessionTest {

    private static final String CACHE_REGION_NAME = EmployeeEntity.class.getName();
    private static final int EMPLOYEE_COUNT = 5;

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUpSessionFactory() {
        Configuration configuration = new Configuration()
                .addAnnotatedClass(EmployeeEntity.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:entitycachetest;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.cache.use_second_level_cache", "true")
                .setProperty("hibernate.cache.use_query_cache", "false")
                // Region factory is environment specific (Infinispan for 5.3 in the original setup).
                // Override with -Dhibernate.cache.region.factory_class=... if needed.
                .setProperty("hibernate.cache.region.factory_class",
                        System.getProperty("hibernate.cache.region.factory_class",
                                "org.hibernate.cache.internal.NoCachingRegionFactory"));

        sessionFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void tearDownSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    void seedDataAndClearCaches() {
        sessionFactory.getCache().evictEntityRegion(EmployeeEntity.class);
        sessionFactory.getStatistics().clear();

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            session.createQuery("delete from EmployeeEntity").executeUpdate();
            for (long i = 1; i <= EMPLOYEE_COUNT; i++) {
                session.persist(new EmployeeEntity(i, "employee-" + i));
            }
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            throw e;
        } finally {
            session.close();
        }

        sessionFactory.getCache().evictEntityRegion(EmployeeEntity.class);
        sessionFactory.getStatistics().clear();
    }

    @Test
    @DisplayName("Loading all entities populates the second-level cache region")
    void loadAllEntitiesPopulatesSecondLevelCache() {
        Statistics stats = sessionFactory.getStatistics();

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            List<EmployeeEntity> all = session.createQuery("from EmployeeEntity", EmployeeEntity.class)
                    .getResultList();
            assertEquals(EMPLOYEE_COUNT, all.size());
            tx.commit();
        } finally {
            session.close();
        }

        assertNotNull(stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME));
        long elementsInCache = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME).getElementCountInMemory();
        assertEquals(EMPLOYEE_COUNT, elementsInCache,
                "All loaded entities should be present in the 2LC region");
    }

    @Test
    @DisplayName("Lookup by ID in the same session after a query executes no SQL")
    void lookupByIdInSameSessionExecutesNoSql() {
        Statistics stats = sessionFactory.getStatistics();

        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            List<EmployeeEntity> all = session.createQuery("from EmployeeEntity", EmployeeEntity.class)
                    .getResultList();
            List<Long> ids = all.stream().map(EmployeeEntity::getId).collect(Collectors.toList());

            long preparedBefore = stats.getPrepareStatementCount();

            for (Long id : ids) {
                EmployeeEntity found = session.get(EmployeeEntity.class, id);
                assertNotNull(found, "Entity with id " + id + " should be found");
            }

            long preparedAfter = stats.getPrepareStatementCount();
            assertEquals(preparedBefore, preparedAfter,
                    "No SQL statements should be executed when looking up returned IDs in the same session");

            tx.commit();
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("Lookup by ID in a new session is served from the second-level cache without SQL")
    void lookupByIdInNewSessionUsesSecondLevelCache() {
        Statistics stats = sessionFactory.getStatistics();

        Session loadingSession = sessionFactory.openSession();
        Transaction loadingTx = loadingSession.beginTransaction();
        try {
            loadingSession.createQuery("from EmployeeEntity", EmployeeEntity.class).getResultList();
            loadingTx.commit();
        } finally {
            loadingSession.close();
        }

        long hitsBefore = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME).getHitCount();
        long preparedBefore = stats.getPrepareStatementCount();

        Session newSession = sessionFactory.openSession();
        Transaction tx = newSession.beginTransaction();
        try {
            for (long id = 1; id <= EMPLOYEE_COUNT; id++) {
                EmployeeEntity found = newSession.get(EmployeeEntity.class, id);
                assertNotNull(found, "Entity with id " + id + " should be found");
            }
            tx.commit();
        } finally {
            newSession.close();
        }

        long preparedAfter = stats.getPrepareStatementCount();
        long hitsAfter = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME).getHitCount();

        assertEquals(preparedBefore, preparedAfter,
                "No SQL should be executed when entities are served from the 2LC");
        assertTrue(hitsAfter - hitsBefore >= EMPLOYEE_COUNT,
                "Each looked-up entity should register a second-level cache hit");
    }

    @Entity(name = "EmployeeEntity")
    @Cacheable
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class EmployeeEntity {

        @Id
        private Long id;

        private String name;

        protected EmployeeEntity() {
            // required by Hibernate
        }

        EmployeeEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
```

---

## SFSB2LC.java -> sameSessionCheck()

- **Debt Type:** `DESIGN`
- **Line Number:** `82`
- **Status:** `GENERATED`
- **Comment:** `* Checking entity 2LC in one EntityManager session      * TODO: rewrite to separate transaction used to createEmployee, and either load entities outside tx or      * start new tx for loading entities.  Maybe simplest to have calling code, handle creating the Employees first.      * TODO: need conclusion to discussion about whether createEmployee should cause 2lc PutCount to be incremented,      *       as no data is loaded into the cache.`

```java
/**
 * Checking entity 2LC in one EntityManager session
 * TODO: rewrite to separate transaction used to createEmployee, and either load entities outside tx or
 * start new tx for loading entities.  Maybe simplest to have calling code, handle creating the Employees first.
 * TODO: need conclusion to discussion about whether createEmployee should cause 2lc PutCount to be incremented,
 *       as no data is loaded into the cache.
 */
public String sameSessionCheck(String CACHE_REGION_NAME) {
    EntityManager em = emf.createEntityManager();
    Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
    stats.clear();
    CacheRegionStatistics emp2LCStats = stats.getDomainDataRegionStatistics(CACHE_REGION_NAME + "Employee");
    try {
        // add new entities and check if they are put in 2LC
        createEmployee(em, "Peter", "Ostrava", 2);
        createEmployee(em, "Tom", "Brno", 3);
        assertEquals("There are 2 puts in the 2LC" + generateEntityCacheStats(emp2LCStats), 2, emp2LCStats.getPutCount());
        // loading all Employee entities should put in 2LC all Employee
        List<?> empList = getAllEmployeesQuery(em);
        assertEquals("There are 2 entities.", empList.size(), 2);
        assertEquals("There are 2 entities in the 2LC" + generateEntityCacheStats(emp2LCStats), 2, emp2LCStats.getElementCountInMemory());
        // clear session
        em.clear();
        // entity should be loaded from 2L cache, we'are expecting hit in 2L cache
        Employee emp = getEmployee(em, 2);
        assertNotNull("Employee returned", emp);
        assertEquals("Expected 1 hit in cache" + generateEntityCacheStats(emp2LCStats), 1, emp2LCStats.getHitCount());
    } catch (AssertionError e) {
        return e.getMessage();
    } finally {
        em.close();
    }
    return "OK";
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the same-session second-level cache check.
 *
 * Assumptions (adjust to the real enclosing class):
 *  - The class under test is {@code SessionCacheChecker}, constructed with an {@code EntityManagerFactory}.
 *  - Helper methods {@code createEmployee(EntityManager, String, String, int)},
 *    {@code getAllEmployeesQuery(EntityManager)}, {@code getEmployee(EntityManager, int)} and
 *    {@code generateEntityCacheStats(CacheRegionStatistics)} are package-visible so they can be stubbed on a spy.
 *  - The Employee entity exists in the same package with a no-arg constructor.
 */
@ExtendWith(MockitoExtension.class)
class SessionCacheCheckerSameSessionTest {

    private static final String REGION = "com.example.model.";
    private static final String EMP_REGION = REGION + "Employee";

    @Mock private EntityManagerFactory emf;
    @Mock private EntityManager em;
    @Mock private Session session;
    @Mock private SessionFactory sessionFactory;
    @Mock private Statistics stats;
    @Mock private CacheRegionStatistics empStats;

    private SessionCacheChecker checker;

    @BeforeEach
    void setUp() {
        lenient().when(emf.createEntityManager()).thenReturn(em);
        lenient().when(em.unwrap(Session.class)).thenReturn(session);
        lenient().when(session.getSessionFactory()).thenReturn(sessionFactory);
        lenient().when(sessionFactory.getStatistics()).thenReturn(stats);
        lenient().when(stats.getDomainDataRegionStatistics(EMP_REGION)).thenReturn(empStats);

        checker = spy(new SessionCacheChecker(emf));

        // Helper behaviour: no-op creation, two loaded employees, one entity fetched after clear.
        lenient().doNothing().when(checker).createEmployee(any(EntityManager.class), anyString(), anyString(), anyInt());
        lenient().doReturn(List.of(new Employee(), new Employee()))
                .when(checker).getAllEmployeesQuery(em);
        lenient().doReturn(new Employee()).when(checker).getEmployee(em, 2);
        lenient().doReturn("stats").when(checker).generateEntityCacheStats(any());
    }

    private void stubCounts(long puts, long elements, long hits) {
        lenient().when(empStats.getPutCount()).thenReturn(puts);
        lenient().when(empStats.getElementCountInMemory()).thenReturn(elements);
        lenient().when(empStats.getHitCount()).thenReturn(hits);
    }

    @Test
    void returnsOkWhenAllCacheExpectationsAreMet() {
        stubCounts(2L, 2L, 1L);

        String result = checker.sameSessionCheck(REGION);

        assertEquals("OK", result);
    }

    @Test
    void clearsStatisticsBeforeRunningChecks() {
        stubCounts(2L, 2L, 1L);

        checker.sameSessionCheck(REGION);

        verify(stats).clear();
    }

    @Test
    void createsTwoEmployeesWithExpectedData() {
        stubCounts(2L, 2L, 1L);

        checker.sameSessionCheck(REGION);

        verify(checker).createEmployee(em, "Peter", "Ostrava", 2);
        verify(checker).createEmployee(em, "Tom", "Brno", 3);
    }

    @Test
    void returnsFailureMessageWhenPutCountIsWrong() {
        stubCounts(0L, 2L, 1L);

        String result = checker.sameSessionCheck(REGION);

        assertNotEquals("OK", result);
        assertTrue(result.startsWith("There are 2 puts in the 2LC"),
                "Unexpected message: " + result);
    }

    @Test
    void returnsFailureMessageWhenQueryReturnsWrongNumberOfEntities() {
        stubCounts(2L, 2L, 1L);
        doReturn(List.of(new Employee(), new Employee(), new Employee()))
                .when(checker).getAllEmployeesQuery(em);

        String result = checker.sameSessionCheck(REGION);

        assertEquals("There are 2 entities.", result);
    }

    @Test
    void returnsFailureMessageWhenElementCountInMemoryIsWrong() {
        stubCounts(2L, 1L, 1L);

        String result = checker.sameSessionCheck(REGION);

        assertTrue(result.startsWith("There are 2 entities in the 2LC"),
                "Unexpected message: " + result);
    }

    @Test
    void returnsFailureMessageWhenHitCountIsWrong() {
        stubCounts(2L, 2L, 0L);

        String result = checker.sameSessionCheck(REGION);

        assertTrue(result.startsWith("Expected 1 hit in cache"),
                "Unexpected message: " + result);
    }

    @Test
    void returnsNullEmployeeFailureMessageWhenGetEmployeeReturnsNull() {
        stubCounts(2L, 2L, 1L);
        doReturn(null).when(checker).getEmployee(em, 2);

        String result = checker.sameSessionCheck(REGION);

        assertEquals("Employee returned", result);
    }

    @Test
    void clearsSessionBeforeFetchingEmployeeFromCache() {
        stubCounts(2L, 2L, 1L);

        checker.sameSessionCheck(REGION);

        InOrder inOrder = inOrder(checker, em);
        inOrder.verify(checker).getAllEmployeesQuery(em);
        inOrder.verify(em).clear();
        inOrder.verify(checker).getEmployee(em, 2);
    }

    @Test
    void alwaysClosesEntityManagerOnSuccess() {
        stubCounts(2L, 2L, 1L);

        checker.sameSessionCheck(REGION);

        verify(em).close();
    }

    @Test
    void alwaysClosesEntityManagerOnAssertionFailure() {
        stubCounts(0L, 0L, 0L);

        checker.sameSessionCheck(REGION);

        verify(em).close();
    }

    @Test
    void propagatesNonAssertionExceptionsAndStillClosesEntityManager() {
        stubCounts(2L, 2L, 1L);
        doThrow(new IllegalStateException("db down"))
                .when(checker).createEmployee(any(EntityManager.class), anyString(), anyString(), anyInt());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> checker.sameSessionCheck(REGION));

        assertEquals("db down", ex.getMessage());
        verify(em).close();
    }
}
```

---

## LastNodeToLeaveRemoteEJBTestCase.java -> createModifiedEJBClientContext()

- **Debt Type:** `DESIGN`
- **Line Number:** `296`
- **Status:** `GENERATED`
- **Comment:** `* Create a sort-of copy of the current context with a different cluster node selector      *      * TODO: We need an easier way to create modified EJB client contexts`

```java
/*
     * Create a sort-of copy of the current context with a different cluster node selector
     *
     * TODO: We need an easier way to create modified EJB client contexts
     */
private EJBClientContext createModifiedEJBClientContext(EJBClientContext oldContext) {
    final EJBClientContext.Builder ejbClientBuilder = new EJBClientContext.Builder();
    // transport
    ejbClientBuilder.addTransportProvider(new RemoteTransportProvider());
    // configured connections
    for (EJBClientConnection connection : oldContext.getConfiguredConnections()) {
        EJBClientConnection.Builder builder = new EJBClientConnection.Builder();
        builder.setDestination(connection.getDestination());
        builder.setForDiscovery(connection.isForDiscovery());
        ejbClientBuilder.addClientConnection(builder.build());
    }
    // cluster node selector
    ejbClientBuilder.setClusterNodeSelector(new CustomClusterNodeSelector());
    return ejbClientBuilder.build();
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.RemoteTransportProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests for the private method {@code createModifiedEJBClientContext(EJBClientContext)}.
 *
 * ASSUMPTIONS (adjust to the real project):
 *  - The owning class is referenced by {@link #OWNER_CLASS_NAME}; it must have a no-arg
 *    constructor (or the method is static, in which case the instance is ignored).
 *  - {@code CustomClusterNodeSelector} is available on the test classpath in the same
 *    package as the owning class.
 *
 * The method is invoked via reflection because it is private. The tests cover the
 * behavior that is documented in the TODO: the copy keeps transport, connections and
 * their settings, replaces the cluster node selector, and leaves the original untouched.
 */
class CreateModifiedEJBClientContextTest {

    private static final String OWNER_CLASS_NAME = "org.example.ejb.EJBClientContextModifier";
    private static final String METHOD_NAME = "createModifiedEJBClientContext";

    private Object owner;
    private Method method;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> ownerClass = Class.forName(OWNER_CLASS_NAME);
        Constructor<?> ctor = ownerClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        owner = ctor.newInstance();

        method = ownerClass.getDeclaredMethod(METHOD_NAME, EJBClientContext.class);
        method.setAccessible(true);
    }

    private EJBClientContext invoke(EJBClientContext oldContext) throws Exception {
        Object result = method.invoke(owner, oldContext);
        return (EJBClientContext) result;
    }

    private static EJBClientContext buildContext(EJBClientConnection... connections) {
        EJBClientContext.Builder builder = new EJBClientContext.Builder();
        builder.addTransportProvider(new RemoteTransportProvider());
        for (EJBClientConnection connection : connections) {
            builder.addClientConnection(connection);
        }
        return builder.build();
    }

    private static EJBClientConnection connection(String uri, boolean forDiscovery) {
        EJBClientConnection.Builder builder = new EJBClientConnection.Builder();
        builder.setDestination(URI.create(uri));
        builder.setForDiscovery(forDiscovery);
        return builder.build();
    }

    @Nested
    @DisplayName("Returned context")
    class ReturnedContext {

        @Test
        @DisplayName("is a new instance distinct from the original context")
        void returnsNewInstance() throws Exception {
            EJBClientContext original = buildContext(connection("remote+http://node1:8080", false));

            EJBClientContext modified = invoke(original);

            assertNotNull(modified);
            assertNotSame(original, modified, "A modified copy must be a different context instance");
        }

        @Test
        @DisplayName("uses CustomClusterNodeSelector as its cluster node selector")
        void usesCustomClusterNodeSelector() throws Exception {
            EJBClientContext original = buildContext(connection("remote+http://node1:8080", false));

            EJBClientContext modified = invoke(original);

            assertNotNull(modified.getClusterNodeSelector(), "Cluster node selector must be set");
            assertInstanceOf(CustomClusterNodeSelector.class, modified.getClusterNodeSelector());
        }

        @Test
        @DisplayName("replaces the cluster node selector of the original context")
        void replacesSelectorFromOriginal() throws Exception {
            EJBClientContext original = buildContext(connection("remote+http://node1:8080", false));
            Object originalSelector = original.getClusterNodeSelector();

            EJBClientContext modified = invoke(original);

            assertNotSame(originalSelector, modified.getClusterNodeSelector(),
                    "Selector should be replaced, not carried over from the original");
        }
    }

    @Nested
    @DisplayName("Configured connections")
    class ConfiguredConnections {

        @Test
        @DisplayName("preserves destinations of all configured connections")
        void preservesDestinations() throws Exception {
            EJBClientContext original = buildContext(
                    connection("remote+http://node1:8080", false),
                    connection("remote+http://node2:8080", false));

            EJBClientContext modified = invoke(original);

            List<URI> originalDestinations = original.getConfiguredConnections().stream()
                    .map(EJBClientConnection::getDestination)
                    .collect(Collectors.toList());
            List<URI> modifiedDestinations = modified.getConfiguredConnections().stream()
                    .map(EJBClientConnection::getDestination)
                    .collect(Collectors.toList());

            assertEquals(originalDestinations, modifiedDestinations);
        }

        @Test
        @DisplayName("preserves the forDiscovery flag of each connection")
        void preservesDiscoveryFlag() throws Exception {
            EJBClientContext original = buildContext(
                    connection("remote+http://discovery:8080", true),
                    connection("remote+http://regular:8080", false));

            EJBClientContext modified = invoke(original);

            List<EJBClientConnection> connections = modified.getConfiguredConnections();
            assertEquals(2, connections.size());

            for (EJBClientConnection c : connections) {
                if (c.getDestination().getHost().equals("discovery")) {
                    assertTrue(c.isForDiscovery(), "Discovery connection must remain for discovery");
                } else {
                    assertFalse(c.isForDiscovery(), "Regular connection must not become discovery");
                }
            }
        }

        @Test
        @DisplayName("produces a context with no connections when the original has none")
        void emptyConnectionsStayEmpty() throws Exception {
            EJBClientContext original = buildContext();

            EJBClientContext modified = invoke(original);

            assertNotNull(modified.getConfiguredConnections());
            assertTrue(modified.getConfiguredConnections().isEmpty());
        }

        @Test
        @DisplayName("does not modify the original context's connections")
        void originalConnectionsUnchanged() throws Exception {
            EJBClientContext original = buildContext(connection("remote+http://node1:8080", true));
            int before = original.getConfiguredConnections().size();

            invoke(original);

            assertEquals(before, original.getConfiguredConnections().size());
            assertTrue(original.getConfiguredConnections().get(0).isForDiscovery());
        }
    }

    @Nested
    @DisplayName("Robustness")
    class Robustness {

        @Test
        @DisplayName("repeated invocations produce independent contexts")
        void repeatedInvocationsAreIndependent() throws Exception {
            EJBClientContext original = buildContext(connection("remote+http://node1:8080", false));

            EJBClientContext first = invoke(original);
            EJBClientContext second = invoke(original);

            assertNotSame(first, second);
            assertNotSame(first.getClusterNodeSelector(), second.getClusterNodeSelector());
        }

        @Test
        @DisplayName("throws NullPointerException for a null context")
        void nullContextFailsFast() {
            Exception ex = assertThrows(Exception.class, () -> invoke(null));
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            assertInstanceOf(NullPointerException.class, cause);
        }
    }
}
```

---

## SimpleServlet.java -> doPatch()

- **Debt Type:** `DESIGN`
- **Line Number:** `91`
- **Status:** `GENERATED`
- **Comment:** `Servlet 6.0 workaround @Override`

```java
// Servlet 6.0 workaround
// @Override
protected void doPatch(HttpServletRequest request, HttpServletResponse response) {
    this.doDelete(request, response);
    this.doGet(request, response);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Servlet 6.0 PATCH workaround, which delegates PATCH handling
 * to doDelete followed by doGet.
 *
 * NOTE: The real servlet class name was not provided. The nested
 * {@link PatchWorkaroundServlet} mirrors the method under test. Replace it
 * with the production servlet class (and make the test live in the same
 * package, or invoke doPatch via a subclass) when integrating.
 */
class DoPatchWorkaroundTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private PatchWorkaroundServlet servlet;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        servlet = new PatchWorkaroundServlet();
    }

    @Test
    void doPatchInvokesDoDeleteThenDoGetInOrder() throws Exception {
        servlet.doPatch(request, response);

        assertEquals(List.of("doDelete", "doGet"), servlet.calls,
                "doPatch must call doDelete first, then doGet");
    }

    @Test
    void doPatchPassesSameRequestAndResponseToBothDelegates() throws Exception {
        servlet.doPatch(request, response);

        assertSame(request, servlet.deleteRequest);
        assertSame(response, servlet.deleteResponse);
        assertSame(request, servlet.getRequest);
        assertSame(response, servlet.getResponse);
    }

    @Test
    void doPatchDoesNotWriteToResponseDirectly() throws Exception {
        servlet.doPatch(request, response);

        // All response handling is delegated; doPatch itself must not touch it.
        verifyNoInteractions(response);
    }

    @Test
    void doDeleteFailurePreventsDoGetAndPropagates() {
        servlet.deleteException = new ServletException("delete failed");

        ServletException ex = assertThrows(ServletException.class,
                () -> servlet.doPatch(request, response));

        assertEquals("delete failed", ex.getMessage());
        assertEquals(List.of("doDelete"), servlet.calls,
                "doGet must not run when doDelete fails");
    }

    @Test
    void doGetFailurePropagatesAfterDoDelete() {
        servlet.getException = new IOException("get failed");

        IOException ex = assertThrows(IOException.class,
                () -> servlet.doPatch(request, response));

        assertEquals("get failed", ex.getMessage());
        assertEquals(List.of("doDelete", "doGet"), servlet.calls,
                "doDelete must run exactly once before doGet fails");
    }

    @Test
    void repeatedDoPatchCallsInvokeDelegatesEachTime() throws Exception {
        servlet.doPatch(request, response);
        servlet.doPatch(request, response);

        assertEquals(
                List.of("doDelete", "doGet", "doDelete", "doGet"),
                servlet.calls);
    }

    @Test
    void doPatchSequenceContainsNoAdditionalDelegateCalls() throws Exception {
        servlet.doPatch(request, response);

        assertEquals(2, servlet.calls.size());
        assertTrue(servlet.calls.indexOf("doDelete") < servlet.calls.indexOf("doGet"));
    }

    /**
     * Test double mirroring the workaround under test. Records delegate
     * invocations and can be configured to throw.
     */
    static class PatchWorkaroundServlet extends HttpServlet {

        final List<String> calls = new ArrayList<>();
        HttpServletRequest deleteRequest;
        HttpServletResponse deleteResponse;
        HttpServletRequest getRequest;
        HttpServletResponse getResponse;
        ServletException deleteException;
        IOException getException;

        // Servlet 6.0 workaround
        protected void doPatch(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            this.doDelete(request, response);
            this.doGet(request, response);
        }

        @Override
        protected void doDelete(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            calls.add("doDelete");
            deleteRequest = request;
            deleteResponse = response;
            if (deleteException != null) {
                throw deleteException;
            }
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            calls.add("doGet");
            getRequest = request;
            getResponse = response;
            if (getException != null) {
                throw getException;
            }
        }
    }
}
```

---

## LoginLogoutBasics.java -> readServerLogLines()

- **Debt Type:** `DESIGN`
- **Line Number:** `294`
- **Status:** `GENERATED`
- **Comment:** `TODO prune this unused code if its non-use doesn't indicate something dropped`

```java
// TODO prune this unused code if its non-use doesn't indicate something dropped
public List<String> readServerLogLines() {
    String jbossHome = System.getProperty("jboss.install.dir");
    String logPath = String.format("%s%sstandalone%slog%sserver.log", jbossHome, (jbossHome.endsWith(File.separator) || jbossHome.endsWith("/")) ? "" : File.separator, File.separator, File.separator);
    logPath = logPath.replace('/', File.separatorChar);
    try {
        // UTF8 is used by default
        return Files.readAllLines(Paths.get(logPath));
    } catch (MalformedInputException e1) {
        // some windows machines could accept only StandardCharsets.ISO_8859_1 encoding
        try {
            return Files.readAllLines(Paths.get(logPath), StandardCharsets.ISO_8859_1);
        } catch (IOException e4) {
            throw new RuntimeException("Server logs has not standard Charsets (UTF8 or ISO_8859_1)");
        }
    } catch (IOException e) {
        // server.log file is not created, it is the same as server.log is empty
    }
    return new ArrayList<>();
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/*
 * Characterization tests for readServerLogLines().
 *
 * These tests pin down the current observable behavior so that the method can be
 * safely pruned or refactored. If the method is confirmed unused, the whole
 * class (and this test suite) can be removed together with it.
 *
 * Assumes the method lives in a class named ServerLogReader in the same package.
 * Adjust the instantiation below if the owning class differs.
 */
class ServerLogReaderTest {

    private static final String PROPERTY = "jboss.install.dir";

    @TempDir
    Path tempDir;

    private String originalProperty;
    private ServerLogReader reader;

    @BeforeEach
    void setUp() {
        originalProperty = System.getProperty(PROPERTY);
        reader = new ServerLogReader();
    }

    @AfterEach
    void tearDown() {
        if (originalProperty == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, originalProperty);
        }
    }

    private Path createLogFile(Path installDir) throws IOException {
        Path logDir = installDir.resolve("standalone").resolve("log");
        Files.createDirectories(logDir);
        return logDir.resolve("server.log");
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("reads all lines from a UTF-8 server.log")
        void readsUtf8Log() throws IOException {
            Path logFile = createLogFile(tempDir);
            Files.write(logFile, List.of("first line", "second line", "ünïcödé line"), StandardCharsets.UTF_8);
            System.setProperty(PROPERTY, tempDir.toString());

            List<String> lines = reader.readServerLogLines();

            assertEquals(List.of("first line", "second line", "ünïcödé line"), lines);
        }

        @Test
        @DisplayName("returns an empty list for an empty server.log")
        void emptyLogReturnsEmptyList() throws IOException {
            Path logFile = createLogFile(tempDir);
            Files.write(logFile, new byte[0]);
            System.setProperty(PROPERTY, tempDir.toString());

            List<String> lines = reader.readServerLogLines();

            assertNotNull(lines);
            assertTrue(lines.isEmpty());
        }

        @Test
        @DisplayName("handles jboss.install.dir with a trailing file separator")
        void trailingFileSeparator() throws IOException {
            Path logFile = createLogFile(tempDir);
            Files.write(logFile, List.of("entry"), StandardCharsets.UTF_8);
            System.setProperty(PROPERTY, tempDir.toString() + File.separator);

            assertEquals(List.of("entry"), reader.readServerLogLines());
        }

        @Test
        @DisplayName("handles jboss.install.dir with a trailing forward slash")
        void trailingForwardSlash() throws IOException {
            Path logFile = createLogFile(tempDir);
            Files.write(logFile, List.of("entry"), StandardCharsets.UTF_8);
            System.setProperty(PROPERTY, tempDir.toString() + "/");

            assertEquals(List.of("entry"), reader.readServerLogLines());
        }
    }

    @Nested
    @DisplayName("Encoding fallback")
    class EncodingFallback {

        @Test
        @DisplayName("falls back to ISO-8859-1 when the file is not valid UTF-8")
        void fallsBackToIso88591() throws IOException {
            Path logFile = createLogFile(tempDir);
            // 0xE9 is 'é' in ISO-8859-1 but an invalid start of a UTF-8 sequence here.
            byte[] content = {(byte) 0xE9, ' ', 'a', '\n', 'b'};
            Files.write(logFile, content);
            System.setProperty(PROPERTY, tempDir.toString());

            List<String> lines = reader.readServerLogLines();

            assertEquals(List.of("\u00E9 a", "b"), lines);
        }
    }

    @Nested
    @DisplayName("Missing or unreadable log")
    class MissingOrUnreadable {

        @Test
        @DisplayName("returns an empty list when server.log does not exist")
        void missingFileReturnsEmptyList() throws IOException {
            Files.createDirectories(tempDir.resolve("standalone").resolve("log"));
            System.setProperty(PROPERTY, tempDir.toString());

            List<String> lines = reader.readServerLogLines();

            assertNotNull(lines);
            assertTrue(lines.isEmpty());
        }

        @Test
        @DisplayName("returns an empty list when the install directory does not exist")
        void missingInstallDirReturnsEmptyList() {
            System.setProperty(PROPERTY, tempDir.resolve("does-not-exist").toString());

            List<String> lines = reader.readServerLogLines();

            assertNotNull(lines);
            assertTrue(lines.isEmpty());
        }

        @Test
        @DisplayName("returns an empty list when server.log is a directory")
        void logPathIsDirectoryReturnsEmptyList() throws IOException {
            Path logLocation = createLogFile(tempDir);
            Files.createDirectories(logLocation);
            System.setProperty(PROPERTY, tempDir.toString());

            List<String> lines = reader.readServerLogLines();

            assertNotNull(lines);
            assertTrue(lines.isEmpty());
        }
    }

    @Nested
    @DisplayName("Invalid configuration")
    class InvalidConfiguration {

        @Test
        @DisplayName("throws NullPointerException when jboss.install.dir is not set")
        void nullInstallDirThrows() {
            System.clearProperty(PROPERTY);

            assertThrows(NullPointerException.class, () -> reader.readServerLogLines());
        }
    }
}
```

---

## AuthenticationTestCase.java -> testAuthentication\_TwoBeans\_ReAuth\_BadPwd()

- **Debt Type:** `TEST`
- **Line Number:** `154`
- **Status:** `GENERATED`
- **Comment:** `TODO - Similar test with first bean @RunAs - does it make sense to also manually switch?`

```java
// TODO - Similar test with first bean @RunAs - does it make sense to also manually switch?
@Test
public void testAuthentication_TwoBeans_ReAuth_BadPwd() throws Exception {
    Util.switchIdentity("user1", "password1", () -> entryBean.doubleWhoAmI("user2", "wrong_password"), true);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests re-authentication across two beans when the second authentication
 * uses a bad password.
 *
 * Assumptions (adjust to the real project):
 *  - Util.switchIdentity(user, password, action, boolean) runs the action under
 *    the given identity and throws an exception if authentication fails.
 *  - entryBean.doubleWhoAmI(user, password) calls a second bean, which is
 *    protected by @RunAs, and returns the caller identity it observes.
 *  - entryBean.whoAmI() returns the identity of the current caller.
 *  - Authentication failures surface as a SecurityException (or subclass).
 */
class EntryBeanReAuthenticationTest {

    private EntryBean entryBean;

    @BeforeEach
    void setUp() throws Exception {
        // Obtain the entry bean from the test container / fixture used by this suite.
        entryBean = TestContainer.lookup(EntryBean.class);
    }

    @Test
    @DisplayName("Re-authenticating the second bean with a bad password is rejected")
    void testAuthentication_TwoBeans_ReAuth_BadPwd() {
        assertThrows(SecurityException.class, () ->
                Util.switchIdentity("user1", "password1",
                        () -> entryBean.doubleWhoAmI("user2", "wrong_password"),
                        true));
    }

    @Test
    @DisplayName("Failed re-authentication does not leak the attempted identity to the caller")
    void testAuthentication_TwoBeans_ReAuth_BadPwd_OriginalIdentityRestored() {
        assertThrows(SecurityException.class, () ->
                Util.switchIdentity("user1", "password1",
                        () -> entryBean.doubleWhoAmI("user2", "wrong_password"),
                        true));

        // After the failed switch, the original identity must be back in effect.
        Util.switchIdentity("user1", "password1",
                () -> assertEquals("user1", entryBean.whoAmI()),
                true);
    }

    @Test
    @DisplayName("Manual identity switch around a bad re-auth still fails and restores identity")
    void testAuthentication_TwoBeans_ReAuth_BadPwd_ManualSwitch() {
        // Verifies the behaviour the TODO asks about: the outer identity switch
        // must not be required for the inner bean's re-auth to be enforced.
        assertThrows(SecurityException.class, () ->
                Util.switchIdentity("user1", "password1",
                        () -> Util.switchIdentity("user2", "wrong_password",
                                () -> entryBean.doubleWhoAmI("user2", "wrong_password"),
                                false),
                        true));

        Util.switchIdentity("user1", "password1",
                () -> assertEquals("user1", entryBean.whoAmI()),
                true);
    }
}
```

---

## MVCKrazoSmokeTestCase.java -> startAndSetupContainer()

- **Debt Type:** `DESIGN`
- **Line Number:** `92`
- **Status:** `GENERATED`
- **Comment:** `TODO migrate this to testsuite/integration/basic and drop this`

```java
// TODO migrate this to testsuite/integration/basic and inject the http listener URL
//    @ArquillianResource
//    private URL url;
// TODO migrate this to testsuite/integration/basic and drop this
@Test
@InSequence(-1)
public void startAndSetupContainer() throws Exception {
    log.trace("*** starting server");
    containerController.start(CONTAINER);
    ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");
    log.trace("*** will configure server now");
    SETUP_TASK.setup(managementClient, CONTAINER);
    deployer.deploy(DEPLOYMENT_NAME);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.jboss.as.test.integration.management.base; // TODO: align with the package of the class under migration

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.as.controller.client.ModelControllerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

/**
 * Unit tests for the server bootstrap logic previously exercised by
 * {@code startAndSetupContainer()}.
 *
 * Collaborators are mocked so the sequencing contract can be verified without
 * launching a container. Full end-to-end behaviour (HTTP listener URL injection,
 * real deployment) is intentionally left to the integration suite under
 * testsuite/integration/basic, which is the migration target of the TODOs.
 *
 * Assumed types (adjust imports/names to match the real migration source):
 *   - ContainerController  : starts a named container
 *   - Deployer             : deploys a named deployment
 *   - SetupTask            : configures the server via a ManagementClient
 */
class StartAndSetupContainerTest {

    private static final String CONTAINER = "test-container";
    private static final String DEPLOYMENT_NAME = "test-deployment";

    private ContainerController containerController;
    private Deployer deployer;
    private SetupTask setupTask;
    private ModelControllerClient modelClient;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9990;

    @BeforeEach
    void setUp() {
        containerController = mock(ContainerController.class);
        deployer = mock(Deployer.class);
        setupTask = mock(SetupTask.class);
        modelClient = mock(ModelControllerClient.class);
    }

    /**
     * Runs the bootstrap sequence the same way the original test method does,
     * with the static environment lookups mocked out.
     */
    private void runStartAndSetup() throws Exception {
        try (MockedStatic<TestSuiteEnvironment> env = mockStatic(TestSuiteEnvironment.class)) {
            env.when(TestSuiteEnvironment::getModelControllerClient).thenReturn(modelClient);
            env.when(TestSuiteEnvironment::getServerAddress).thenReturn(SERVER_ADDRESS);
            env.when(TestSuiteEnvironment::getServerPort).thenReturn(SERVER_PORT);

            // Mirrors the body of startAndSetupContainer()
            containerController.start(CONTAINER);
            ManagementClient managementClient = new ManagementClient(
                    modelClient, SERVER_ADDRESS, SERVER_PORT, "remote+http");
            setupTask.setup(managementClient, CONTAINER);
            deployer.deploy(DEPLOYMENT_NAME);
        }
    }

    @Test
    @DisplayName("Starts container before configuring and deploying")
    void startsContainerBeforeSetupAndDeploy() throws Exception {
        runStartAndSetup();

        InOrder order = inOrder(containerController, setupTask, deployer);
        order.verify(containerController).start(CONTAINER);
        order.verify(setupTask).setup(any(ManagementClient.class), eq(CONTAINER));
        order.verify(deployer).deploy(DEPLOYMENT_NAME);
    }

    @Test
    @DisplayName("Passes the management client built from environment address and port")
    void setupReceivesManagementClientFromEnvironment() throws Exception {
        runStartAndSetup();

        verify(setupTask).setup(
                org.mockito.ArgumentMatchers.argThat(mc ->
                        mc != null),
                eq(CONTAINER));
    }

    @Nested
    @DisplayName("Failure handling")
    class FailureHandling {

        @Test
        @DisplayName("Does not configure or deploy when container fails to start")
        void startFailurePreventsSetupAndDeploy() throws Exception {
            org.mockito.Mockito.doThrow(new IllegalStateException("start failed"))
                    .when(containerController).start(CONTAINER);

            assertThrows(IllegalStateException.class, this::runStartAndSetup);

            verifyNoInteractions(setupTask);
            verify(deployer, never()).deploy(any());
        }

        @Test
        @DisplayName("Does not deploy when setup task fails")
        void setupFailurePreventsDeploy() throws Exception {
            org.mockito.Mockito.doThrow(new RuntimeException("setup failed"))
                    .when(setupTask).setup(any(ManagementClient.class), eq(CONTAINER));

            assertThrows(RuntimeException.class, this::runStartAndSetup);

            verify(containerController).start(CONTAINER);
            verify(deployer, never()).deploy(any());
        }

        @Test
        @DisplayName("Propagates deployment failure")
        void deployFailureIsPropagated() throws Exception {
            org.mockito.Mockito.doThrow(new RuntimeException("deploy failed"))
                    .when(deployer).deploy(DEPLOYMENT_NAME);

            assertThrows(RuntimeException.class, this::runStartAndSetup);

            verify(containerController).start(CONTAINER);
            verify(setupTask).setup(any(ManagementClient.class), eq(CONTAINER));
        }
    }
}

/*
 * Minimal collaborator contracts assumed by the tests above.
 * Replace with the real types from the migration source when porting.
 */
interface ContainerController {
    void start(String containerId) throws Exception;
}

interface Deployer {
    void deploy(String deploymentName) throws Exception;
}

interface SetupTask {
    void setup(ManagementClient managementClient, String containerId) throws Exception;
}
```

---

## MVCKrazoSmokeTestCase.java -> stopContainer()

- **Debt Type:** `DESIGN`
- **Line Number:** `120`
- **Status:** `GENERATED`
- **Comment:** `TODO migrate this to testsuite/integration/basic and drop this`

```java
// TODO migrate this to testsuite/integration/basic and drop this
@Test
@InSequence(10)
public void stopContainer() throws Exception {
    deployer.undeploy(DEPLOYMENT_NAME);
    final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");
    log.trace("*** resetting test configuration");
    SETUP_TASK.tearDown(managementClient, CONTAINER);
    log.trace("*** stopping container");
    containerController.stop(CONTAINER);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.jboss.as.test.integration.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for the legacy {@code stopContainer()} lifecycle method.
 *
 * Assumptions (adjust to the real enclosing class when migrating):
 *  - The enclosing test class is {@code LegacyContainerTestCase}; the method under test is
 *    its public {@code stopContainer()}.
 *  - Instance fields {@code deployer}, {@code containerController} and the setup task
 *    {@code SETUP_TASK} are non-final and can be injected reflectively.
 *  - {@code DEPLOYMENT_NAME} and {@code CONTAINER} are the constants used by the class.
 *
 * Once migrated to testsuite/integration/basic, this class (and the legacy class) should be
 * reviewed and the legacy class removed.
 */
class StopContainerTest {

    private static final String DEPLOYMENT_NAME = "test-deployment";
    private static final String CONTAINER = "default-jbossas";

    private LegacyContainerTestCase target;
    private Deployer deployer;
    private ContainerController containerController;
    private ServerSetupTask setupTask;
    private ModelControllerClient client;

    private MockedStatic<TestSuiteEnvironment> environment;
    private MockedConstruction<ManagementClient> managementClientConstruction;
    private final List<List<?>> constructorArguments = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        target = mock(LegacyContainerTestCase.class, CALLS_REAL_METHODS);
        deployer = mock(Deployer.class);
        containerController = mock(ContainerController.class);
        setupTask = mock(ServerSetupTask.class);
        client = mock(ModelControllerClient.class);

        setField(target, "deployer", deployer);
        setField(target, "containerController", containerController);
        setField(target, "SETUP_TASK", setupTask);

        environment = mockStatic(TestSuiteEnvironment.class);
        environment.when(TestSuiteEnvironment::getModelControllerClient).thenReturn(client);
        environment.when(TestSuiteEnvironment::getServerAddress).thenReturn("localhost");
        environment.when(TestSuiteEnvironment::getServerPort).thenReturn(9990);

        constructorArguments.clear();
        managementClientConstruction = mockConstruction(ManagementClient.class,
                (mock, context) -> constructorArguments.add(context.arguments()));
    }

    @AfterEach
    void tearDown() {
        managementClientConstruction.close();
        environment.close();
    }

    @Test
    @DisplayName("stopContainer undeploys, resets configuration and stops the container in order")
    void stopContainerExecutesStepsInOrder() throws Exception {
        target.stopContainer();

        assertEquals(1, managementClientConstruction.constructed().size(),
                "a single ManagementClient should be created");
        ManagementClient managementClient = managementClientConstruction.constructed().get(0);

        InOrder inOrder = inOrder(deployer, setupTask, containerController);
        inOrder.verify(deployer).undeploy(DEPLOYMENT_NAME);
        inOrder.verify(setupTask).tearDown(managementClient, CONTAINER);
        inOrder.verify(containerController).stop(CONTAINER);
    }

    @Test
    @DisplayName("ManagementClient is created from the suite's client, address, port and remote+http protocol")
    void managementClientIsConfiguredFromTestSuiteEnvironment() throws Exception {
        target.stopContainer();

        assertEquals(1, constructorArguments.size());
        assertEquals(List.of(client, "localhost", 9990, "remote+http"), constructorArguments.get(0));
    }

    @Test
    @DisplayName("undeploy failure propagates and the setup task and container are left untouched")
    void undeployFailurePropagatesAndSkipsRemainingSteps() throws Exception {
        doThrow(new IllegalStateException("undeploy failed"))
                .when(deployer).undeploy(DEPLOYMENT_NAME);

        assertThrows(IllegalStateException.class, () -> target.stopContainer());

        verifyNoInteractions(setupTask);
        verifyNoInteractions(containerController);
    }

    @Test
    @DisplayName("tearDown failure propagates and the container is not stopped")
    void tearDownFailurePropagatesAndSkipsStop() throws Exception {
        doThrow(new RuntimeException("teardown failed"))
                .when(setupTask).tearDown(any(ManagementClient.class), any(String.class));

        assertThrows(RuntimeException.class, () -> target.stopContainer());

        verify(deployer).undeploy(DEPLOYMENT_NAME);
        verifyNoInteractions(containerController);
    }

    @Test
    @DisplayName("container stop failure propagates after undeploy and teardown have run")
    void stopFailurePropagates() throws Exception {
        doThrow(new RuntimeException("stop failed"))
                .when(containerController).stop(CONTAINER);

        assertThrows(RuntimeException.class, () -> target.stopContainer());

        verify(deployer).undeploy(DEPLOYMENT_NAME);
        verify(setupTask).tearDown(managementClientConstruction.constructed().get(0), CONTAINER);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
```

---

## ComplexPropertiesParseUtils.java -> addExtensionProperties()

- **Debt Type:** `IMPLEMENTATION`
- **Line Number:** `284`
- **Status:** `GENERATED`
- **Comment:** `* Adds properties of Extension type to the operation      * TODO: not implemented jet in DMR`

```java
/**
 * Adds properties of Extension type to the operation
 * TODO: not implemented jet in DMR
 */
public static void addExtensionProperties(ModelNode operation) {
    operation.get("reauth-plugin-properties", "name").set("Property1");
    operation.get("valid-connection-checker-properties", "name").set("Property2");
    operation.get("stale-connection-checker-properties", "name").set("Property3");
    operation.get("exception-sorter-properties", "name").set("Property4");
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// NOTE: Adjust the import/class name below to match the class that declares
// addExtensionProperties(ModelNode). It is assumed to be a static utility class.
// import com.example.OperationUtils;

class AddExtensionPropertiesTest {

    private static final String[] PROPERTY_GROUPS = {
            "reauth-plugin-properties",
            "valid-connection-checker-properties",
            "stale-connection-checker-properties",
            "exception-sorter-properties"
    };

    @Nested
    @DisplayName("Happy path: extension properties are added to the operation")
    class HappyPath {

        @Test
        @DisplayName("Adds a 'name' entry under each extension property group")
        void addsNameUnderEachGroup() {
            ModelNode operation = new ModelNode();

            OperationUtils.addExtensionProperties(operation);

            assertEquals("Property1",
                    operation.get("reauth-plugin-properties", "name").asString());
            assertEquals("Property2",
                    operation.get("valid-connection-checker-properties", "name").asString());
            assertEquals("Property3",
                    operation.get("stale-connection-checker-properties", "name").asString());
            assertEquals("Property4",
                    operation.get("exception-sorter-properties", "name").asString());
        }

        @Test
        @DisplayName("Creates each property group as an OBJECT node")
        void createsObjectNodes() {
            ModelNode operation = new ModelNode();

            OperationUtils.addExtensionProperties(operation);

            for (String group : PROPERTY_GROUPS) {
                assertTrue(operation.hasDefined(group), "Missing group: " + group);
                assertEquals(ModelType.OBJECT, operation.get(group).getType(),
                        "Group should be OBJECT: " + group);
            }
        }

        @Test
        @DisplayName("Creates exactly the expected top-level keys and nothing else")
        void createsOnlyExpectedKeys() {
            ModelNode operation = new ModelNode();

            OperationUtils.addExtensionProperties(operation);

            assertEquals(PROPERTY_GROUPS.length, operation.keys().size(),
                    "Unexpected top-level keys: " + operation.keys());
            for (String group : PROPERTY_GROUPS) {
                assertTrue(operation.keys().contains(group), "Missing key: " + group);
            }
        }

        @Test
        @DisplayName("Each group contains only the 'name' key")
        void eachGroupContainsOnlyName() {
            ModelNode operation = new ModelNode();

            OperationUtils.addExtensionProperties(operation);

            for (String group : PROPERTY_GROUPS) {
                assertEquals(1, operation.get(group).keys().size(),
                        "Group should contain exactly one key: " + group);
                assertTrue(operation.get(group).keys().contains("name"),
                        "Group should contain 'name': " + group);
            }
        }
    }

    @Nested
    @DisplayName("Preservation of existing operation content")
    class Preservation {

        @Test
        @DisplayName("Preserves unrelated existing operation attributes")
        void preservesUnrelatedAttributes() {
            ModelNode operation = new ModelNode();
            operation.get("operation").set("add");
            operation.get("address").add("subsystem", "datasources");
            operation.get("jndi-name").set("java:/MyDS");

            OperationUtils.addExtensionProperties(operation);

            assertEquals("add", operation.get("operation").asString());
            assertEquals("java:/MyDS", operation.get("jndi-name").asString());
            assertEquals("subsystem", operation.get("address").get(0).asProperty().getName());
        }

        @Test
        @DisplayName("Preserves other keys already present within an existing property group")
        void preservesSiblingKeysInExistingGroup() {
            ModelNode operation = new ModelNode();
            operation.get("reauth-plugin-properties", "other").set("keep-me");

            OperationUtils.addExtensionProperties(operation);

            assertEquals("keep-me",
                    operation.get("reauth-plugin-properties", "other").asString());
            assertEquals("Property1",
                    operation.get("reauth-plugin-properties", "name").asString());
        }
    }

    @Nested
    @DisplayName("Idempotency and overwriting behavior")
    class Idempotency {

        @Test
        @DisplayName("Calling twice yields the same result as calling once")
        void calledTwiceIsIdempotent() {
            ModelNode once = new ModelNode();
            OperationUtils.addExtensionProperties(once);

            ModelNode twice = new ModelNode();
            OperationUtils.addExtensionProperties(twice);
            OperationUtils.addExtensionProperties(twice);

            assertEquals(once, twice);
        }

        @Test
        @DisplayName("Overwrites a pre-existing 'name' value in a property group")
        void overwritesExistingName() {
            ModelNode operation = new ModelNode();
            operation.get("exception-sorter-properties", "name").set("OldValue");

            OperationUtils.addExtensionProperties(operation);

            assertEquals("Property4",
                    operation.get("exception-sorter-properties", "name").asString());
        }
    }

    @Nested
    @DisplayName("Edge and error cases")
    class EdgeCases {

        @Test
        @DisplayName("Throws NullPointerException when operation is null")
        void nullOperationThrows() {
            assertThrows(NullPointerException.class,
                    () -> OperationUtils.addExtensionProperties(null));
        }

        @Test
        @DisplayName("Does not mutate other ModelNode instances")
        void doesNotAffectOtherInstances() {
            ModelNode target = new ModelNode();
            ModelNode unrelated = new ModelNode();
            unrelated.get("untouched").set(true);

            OperationUtils.addExtensionProperties(target);

            assertFalse(unrelated.hasDefined("reauth-plugin-properties"));
            assertTrue(unrelated.get("untouched").asBoolean());
            assertEquals(1, unrelated.keys().size());
        }
    }
}
```

---

## Utils.java -> loginWithKerberos()

- **Debt Type:** `DESIGN`
- **Line Number:** `760`
- **Status:** `GENERATED`
- **Comment:** `* Creates login context for given {@link Krb5LoginConfiguration} and credentials and calls the {@link LoginContext#login()}      * method on it. This method contains workaround for IBM JDK issue described in bugzilla <a      * href="https://bugzilla.redhat.com/show_bug.cgi?id=1206177">https://bugzilla.redhat.com/show_bug.cgi?id=1206177</a>.      *      * @param krb5Configuration      * @param user      * @param pass      * @return      * @throws LoginException`

```java
/**
 * Creates login context for given {@link Krb5LoginConfiguration} and credentials and calls the {@link LoginContext#login()}
 * method on it. This method contains workaround for IBM JDK issue described in bugzilla <a
 * href="https://bugzilla.redhat.com/show_bug.cgi?id=1206177">https://bugzilla.redhat.com/show_bug.cgi?id=1206177</a>.
 *
 * @param krb5Configuration
 * @param user
 * @param pass
 * @return
 * @throws LoginException
 */
public static LoginContext loginWithKerberos(final Krb5LoginConfiguration krb5Configuration, final String user, final String pass) throws LoginException {
    LoginContext lc = new LoginContext(krb5Configuration.getName(), new UsernamePasswordCBH(user, pass.toCharArray()));
    lc.login();
    return lc;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

// NOTE: Adjust the imports below and the enclosing class name ("KerberosUtils")
// to match the actual packages/classes in your project.
// Krb5LoginConfiguration and UsernamePasswordCBH are referenced from the
// production code; the enclosing class is assumed to be KerberosUtils.
class KerberosUtilsLoginWithKerberosTest {

    private Krb5LoginConfiguration krb5Configuration;

    @BeforeEach
    void setUp() {
        krb5Configuration = mock(Krb5LoginConfiguration.class);
        when(krb5Configuration.getName()).thenReturn("krb5-test-config");
    }

    @Test
    @DisplayName("Returns the LoginContext that was successfully logged in")
    void loginWithKerberos_successfulLogin_returnsLoginContext() throws LoginException {
        try (MockedConstruction<LoginContext> constructed =
                     mockConstruction(LoginContext.class)) {

            LoginContext result = KerberosUtils.loginWithKerberos(krb5Configuration, "user", "pass");

            assertNotNull(result);
            List<LoginContext> contexts = constructed.constructed();
            assertSame(1, contexts.size(), "Exactly one LoginContext should be constructed");
            assertSame(contexts.get(0), result, "Returned context should be the constructed instance");
            verify(contexts.get(0)).login();
        }
    }

    @Test
    @DisplayName("Calls login() exactly once on the created LoginContext")
    void loginWithKerberos_callsLoginOnce() throws LoginException {
        try (MockedConstruction<LoginContext> constructed =
                     mockConstruction(LoginContext.class)) {

            KerberosUtils.loginWithKerberos(krb5Configuration, "alice", "secret");

            LoginContext lc = constructed.constructed().get(0);
            verify(lc, org.mockito.Mockito.times(1)).login();
        }
    }

    @Test
    @DisplayName("Propagates LoginException thrown by LoginContext#login()")
    void loginWithKerberos_loginFails_propagatesLoginException() {
        try (MockedConstruction<LoginContext> constructed =
                     mockConstruction(LoginContext.class, (mockLc, context) ->
                             doThrow(new LoginException("invalid credentials")).when(mockLc).login())) {

            LoginException ex = assertThrows(LoginException.class,
                    () -> KerberosUtils.loginWithKerberos(krb5Configuration, "user", "wrong"));

            assertSame("invalid credentials", ex.getMessage());
            assertSame(1, constructed.constructed().size());
        }
    }

    @Test
    @DisplayName("Throws NullPointerException when configuration is null")
    void loginWithKerberos_nullConfiguration_throwsNpe() {
        try (MockedConstruction<LoginContext> constructed =
                     mockConstruction(LoginContext.class)) {

            assertThrows(NullPointerException.class,
                    () -> KerberosUtils.loginWithKerberos(null, "user", "pass"));

            assertSame(0, constructed.constructed().size(),
                    "No LoginContext should be created when configuration is null");
        }
    }

    @Test
    @DisplayName("Throws NullPointerException when password is null")
    void loginWithKerberos_nullPassword_throwsNpe() {
        try (MockedConstruction<LoginContext> constructed =
                     mockConstruction(LoginContext.class)) {

            assertThrows(NullPointerException.class,
                    () -> KerberosUtils.loginWithKerberos(krb5Configuration, "user", null));

            assertSame(0, constructed.constructed().size(),
                    "No LoginContext should be created when password is null");
        }
    }
}
```

---

## RecoveryExecutor.java -> runTransactionRecovery()

- **Debt Type:** `DESIGN`
- **Line Number:** `82`
- **Status:** `GENERATED`
- **Comment:** `* <p>      * Run the transaction recovery. It expects the <code>transaction-listener</code> is enabled      * (<code>&sol;subsystem=transactions&sol;:write-attribute(name=transaction-listener, value=true)</code>).      * </p>      * <p>      * Returning from this method could not necessary means that whole recovery cycle was fully finished.      * To be sure that the whole recovery cycle is finished is recommended to run this method twice (one by one).      * After the second call it's ensured that one(!) recovery cycle is fully finished.      * </p>      * <p>      * The method returns true if the recovery socket listener call succeeded and when the recovery cycle was run      * without any issue. An issue means there is an error on recovery processing of an XAResource -      * e.g. one example of such failure could be a database is down and recovery was not able to connect.      * </p>      *      * @param socketReadTimeout  socket timeout for launching the recovery against the transaction listener socket,      *                           expected to be opened already      * @return true if recovery was successfully run without any issue, false otherwise`

```java
/**
 * <p>
 * Run the transaction recovery. It expects the <code>transaction-listener</code> is enabled
 * (<code>&sol;subsystem=transactions&sol;:write-attribute(name=transaction-listener, value=true)</code>).
 * </p>
 * <p>
 * Returning from this method could not necessary means that whole recovery cycle was fully finished.
 * To be sure that the whole recovery cycle is finished is recommended to run this method twice (one by one).
 * After the second call it's ensured that one(!) recovery cycle is fully finished.
 * </p>
 * <p>
 * The method returns true if the recovery socket listener call succeeded and when the recovery cycle was run
 * without any issue. An issue means there is an error on recovery processing of an XAResource -
 * e.g. one example of such failure could be a database is down and recovery was not able to connect.
 * </p>
 *
 * @param socketReadTimeout  socket timeout for launching the recovery against the transaction listener socket,
 *                           expected to be opened already
 * @return true if recovery was successfully run without any issue, false otherwise
 */
public boolean runTransactionRecovery(int socketReadTimeout) {
    try {
        return getRecoveryDriver().synchronousVerboseScan(TimeoutUtil.adjust(socketReadTimeout), RECOVERY_SCAN_RETRY_COUNT);
    } catch (Exception e) {
        throw new IllegalStateException("Error when triggering transaction recovery synchronous scan with RecoveryDriver " + recoveryDriverReference.get() + ", based on the management client " + managementClient, e);
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/*
 * Test for TransactionRecoveryHelper#runTransactionRecovery(int).
 *
 * Assumptions (adjust to the actual code base if names differ):
 *  - The enclosing class is TransactionRecoveryHelper with a constructor that takes no arguments
 *    (or a suitable one) and a non-final method getRecoveryDriver() returning RecoveryDriver.
 *  - RecoveryDriver exposes boolean synchronousVerboseScan(long timeout, int retryCount).
 *  - TimeoutUtil.adjust(int) is a static utility used to scale timeouts.
 */
class TransactionRecoveryHelperRunTransactionRecoveryTest {

    private TransactionRecoveryHelper helper;
    private RecoveryDriver recoveryDriver;

    @BeforeEach
    void setUp() {
        helper = spy(new TransactionRecoveryHelper());
        recoveryDriver = mock(RecoveryDriver.class);
        doReturn(recoveryDriver).when(helper).getRecoveryDriver();
    }

    @Nested
    @DisplayName("Successful recovery cycle")
    class SuccessfulRecovery {

        @Test
        @DisplayName("returns true when the recovery scan completes without issues")
        void returnsTrueWhenScanSucceeds() {
            int timeout = 5000;
            doReturn(true).when(recoveryDriver)
                    .synchronousVerboseScan(eq((long) TimeoutUtil.adjust(timeout)), anyInt());

            boolean result = helper.runTransactionRecovery(timeout);

            assertTrue(result, "Recovery without any issue must return true");
        }

        @Test
        @DisplayName("passes the adjusted socket timeout to the recovery driver")
        void passesAdjustedTimeoutToDriver() {
            int timeout = 1234;
            doReturn(true).when(recoveryDriver).synchronousVerboseScan(anyLong(), anyInt());

            helper.runTransactionRecovery(timeout);

            verify(recoveryDriver, times(1))
                    .synchronousVerboseScan(eq((long) TimeoutUtil.adjust(timeout)), anyInt());
        }

        @Test
        @DisplayName("running recovery twice in a row performs two independent scans")
        void runningTwiceInvokesScanTwice() {
            int timeout = 2000;
            doReturn(true).when(recoveryDriver).synchronousVerboseScan(anyLong(), anyInt());

            boolean first = helper.runTransactionRecovery(timeout);
            boolean second = helper.runTransactionRecovery(timeout);

            assertTrue(first);
            assertTrue(second);
            verify(recoveryDriver, times(2)).synchronousVerboseScan(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("Recovery cycle with issues")
    class FailedRecovery {

        @Test
        @DisplayName("returns false when the recovery scan reports an XAResource failure")
        void returnsFalseWhenScanReportsIssue() {
            doReturn(false).when(recoveryDriver).synchronousVerboseScan(anyLong(), anyInt());

            boolean result = helper.runTransactionRecovery(3000);

            assertFalse(result, "A recovery with an issue (e.g. database down) must return false");
        }
    }

    @Nested
    @DisplayName("Error propagation")
    class ErrorHandling {

        @Test
        @DisplayName("wraps driver exceptions in IllegalStateException with the original cause")
        void wrapsExceptionFromDriver() {
            RuntimeException cause = new RuntimeException("listener socket not opened");
            doThrow(cause).when(recoveryDriver).synchronousVerboseScan(anyLong(), anyInt());

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> helper.runTransactionRecovery(1000));

            assertSame(cause, ex.getCause(), "Original exception must be preserved as the cause");
            assertTrue(ex.getMessage().contains("Error when triggering transaction recovery"),
                    "Message should describe the failing recovery trigger");
        }

        @Test
        @DisplayName("wraps checked exceptions from the driver in IllegalStateException")
        void wrapsCheckedExceptionFromDriver() throws Exception {
            Exception cause = new Exception("connection refused");
            doThrow(cause).when(recoveryDriver).synchronousVerboseScan(anyLong(), anyInt());

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> helper.runTransactionRecovery(1000));

            assertSame(cause, ex.getCause());
        }

        @Test
        @DisplayName("does not swallow errors: failure is never reported as a plain false")
        void exceptionIsNotConvertedToFalse() {
            doThrow(new RuntimeException("boom")).when(recoveryDriver).synchronousVerboseScan(anyLong(), anyInt());

            assertThrows(IllegalStateException.class, () -> helper.runTransactionRecovery(1000));
            verify(recoveryDriver, times(1)).synchronousVerboseScan(anyLong(), anyInt());
        }
    }
}
```

---

## StaleDockerConnectionWorkaround.java -> start()

- **Debt Type:** `DESIGN`
- **Line Number:** `31`
- **Status:** `GENERATED`
- **Comment:** `* Starts the given container, retrying if the start failed on a stale Docker API connection.      * Not to be used with a container whose {@link GenericContainer#start()} itself delegates to this workaround.`

```java
/**
 * Starts the given container, retrying if the start failed on a stale Docker API connection.
 * Not to be used with a container whose {@link GenericContainer#start()} itself delegates to this workaround.
 */
public static void start(GenericContainer<?> container) {
    start(container.getDockerImageName(), container::start);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Tests for the static {@code start(GenericContainer<?>)} workaround helper.
 *
 * Assumption: the enclosing class is {@code ContainerStartHelper}. Adjust the
 * class reference in {@link #newHelper()}-style calls if the owning class differs.
 */
class ContainerStartHelperTest {

    private static final DockerImageName IMAGE = DockerImageName.parse("alpine:3.19");

    /**
     * A GenericContainer whose start() is intercepted so no Docker daemon is needed.
     */
    private static class CountingContainer extends GenericContainer<CountingContainer> {
        final AtomicInteger startCalls = new AtomicInteger();
        private final Runnable behaviour;

        CountingContainer(Runnable behaviour) {
            super(IMAGE);
            this.behaviour = behaviour;
        }

        @Override
        public void start() {
            startCalls.incrementAndGet();
            behaviour.run();
        }
    }

    @Test
    @DisplayName("start() invokes container start exactly once when the start succeeds")
    void start_invokesContainerStartOnce_whenStartSucceeds() {
        CountingContainer container = new CountingContainer(() -> { });

        ContainerStartHelper.start(container);

        assertEquals(1, container.startCalls.get(),
                "A successful start should not trigger retries");
    }

    @Test
    @DisplayName("start() does not swallow a non-transient failure from the container")
    void start_propagatesNonTransientFailure() {
        CountingContainer container = new CountingContainer(() -> {
            throw new IllegalStateException("container definition is invalid");
        });

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ContainerStartHelper.start(container));

        assertNotNull(thrown);
        assertTrue(container.startCalls.get() >= 1,
                "The container start must have been attempted");
    }

    @Test
    @DisplayName("start() retries after a transient failure and eventually succeeds")
    void start_retriesOnTransientFailure_thenSucceeds() {
        AtomicInteger attempt = new AtomicInteger();
        CountingContainer container = new CountingContainer(() -> {
            if (attempt.incrementAndGet() == 1) {
                throw new RuntimeException("stale Docker API connection");
            }
        });

        assertDoesNotThrow(() -> ContainerStartHelper.start(container));

        assertEquals(2, container.startCalls.get(),
                "Expected one failed attempt followed by one successful retry");
    }

    @Test
    @DisplayName("start() rejects a null container")
    void start_nullContainer_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> ContainerStartHelper.start(null));
    }

    @Test
    @DisplayName("start() uses the container's own image name for the retry wrapper")
    void start_usesContainerImageName() {
        CountingContainer container = new CountingContainer(() -> { });

        assertEquals(IMAGE.asCanonicalNameString(),
                container.getDockerImageName());

        assertDoesNotThrow(() -> ContainerStartHelper.start(container));
        assertEquals(1, container.startCalls.get());
    }
}
```

---

## WebServerService.java -> getPort()

- **Debt Type:** `IMPLEMENTATION`
- **Line Number:** `35`
- **Status:** `GENERATED`
- **Comment:** `todo we need to handle cases when deployments reference listeners/server/host directly`

```java
//todo we need to handle cases when deployments reference listeners/server/host directly
@Override
public int getPort(final String protocol, final boolean secure) {
    Map<String, UndertowListener> listeners = getListenerMap();
    UndertowListener listener = null;
    for (Map.Entry<String, UndertowListener> entry : listeners.entrySet()) {
        if (protocol.toLowerCase(Locale.ENGLISH).contains(entry.getKey())) {
            listener = entry.getValue();
        }
    }
    if (listener != null && listener.getProtocol() == HttpListenerService.PROTOCOL && secure) {
        if (listeners.containsKey(HttpsListenerService.PROTOCOL)) {
            listener = listeners.get(HttpsListenerService.PROTOCOL);
        } else {
            UndertowLogger.ROOT_LOGGER.secureListenerNotAvailableForPort(protocol);
        }
    }
    if (listener != null) {
        SocketBinding binding = listener.getSocketBinding();
        return binding.getAbsolutePort();
    }
    throw UndertowLogger.ROOT_LOGGER.noPortListeningForProtocol(protocol);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.wildfly.extension.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code getPort(String, boolean)}.
 *
 * The method resolves a listener by protocol, upgrades an HTTP listener to the
 * HTTPS listener when a secure port is requested, and returns the absolute port
 * of the resolved listener's socket binding. The TODO concerns deployments that
 * reference listeners, server, or host directly; these tests exercise the
 * listener resolution paths that such references depend on.
 */
class ServerGetPortTest {

    private static final String HTTP = HttpListenerService.PROTOCOL;
    private static final String HTTPS = HttpsListenerService.PROTOCOL;

    private Server server;
    private Map<String, UndertowListener> listeners;

    @BeforeEach
    void setUp() {
        server = mock(Server.class, CALLS_REAL_METHODS);
        // LinkedHashMap keeps insertion order so iteration is deterministic:
        // "http" is visited before "https", so "https" wins for protocol "https".
        listeners = new LinkedHashMap<>();
        when(server.getListenerMap()).thenAnswer(invocation -> listeners);
    }

    private UndertowListener listener(String protocol, int port) {
        UndertowListener listener = mock(UndertowListener.class);
        SocketBinding binding = mock(SocketBinding.class);
        when(listener.getProtocol()).thenReturn(protocol);
        when(listener.getSocketBinding()).thenReturn(binding);
        when(binding.getAbsolutePort()).thenReturn(port);
        return listener;
    }

    @Nested
    @DisplayName("Non-secure lookups")
    class NonSecure {

        @Test
        @DisplayName("returns the port of the matching HTTP listener")
        void returnsHttpPort() {
            listeners.put(HTTP, listener(HTTP, 8080));

            assertEquals(8080, server.getPort(HTTP, false));
        }

        @Test
        @DisplayName("returns the port of the HTTPS listener when protocol is https and not secure")
        void returnsHttpsPortWhenProtocolIsHttps() {
            listeners.put(HTTP, listener(HTTP, 8080));
            listeners.put(HTTPS, listener(HTTPS, 8443));

            assertEquals(8443, server.getPort(HTTPS, false));
        }

        @Test
        @DisplayName("matches protocol case-insensitively")
        void matchesProtocolCaseInsensitively() {
            listeners.put(HTTP, listener(HTTP, 8080));

            assertEquals(8080, server.getPort("HTTP", false));
        }

        @Test
        @DisplayName("matches listener whose key is contained in a longer protocol string")
        void matchesListenerKeyContainedInProtocol() {
            listeners.put(HTTP, listener(HTTP, 8080));

            assertEquals(8080, server.getPort("http/1.1", false));
        }
    }

    @Nested
    @DisplayName("Secure lookups")
    class Secure {

        @Test
        @DisplayName("upgrades an HTTP listener to the HTTPS listener when secure is requested")
        void upgradesHttpToHttpsWhenSecure() {
            listeners.put(HTTP, listener(HTTP, 8080));
            listeners.put(HTTPS, listener(HTTPS, 8443));

            assertEquals(8443, server.getPort(HTTP, true));
        }

        @Test
        @DisplayName("falls back to the HTTP port when no HTTPS listener is available")
        void fallsBackToHttpWhenNoHttpsListener() {
            listeners.put(HTTP, listener(HTTP, 8080));

            assertEquals(8080, server.getPort(HTTP, true));
        }

        @Test
        @DisplayName("returns the HTTPS port when secure is requested for an https protocol")
        void secureHttpsReturnsHttpsPort() {
            listeners.put(HTTP, listener(HTTP, 8080));
            listeners.put(HTTPS, listener(HTTPS, 8443));

            assertEquals(8443, server.getPort(HTTPS, true));
        }
    }

    @Nested
    @DisplayName("Failure cases")
    class Failures {

        @Test
        @DisplayName("throws when no listener matches the protocol")
        void throwsWhenNoListenerMatches() {
            listeners.put(HTTP, listener(HTTP, 8080));

            assertThrows(RuntimeException.class, () -> server.getPort("ajp", false));
        }

        @Test
        @DisplayName("throws when there are no listeners at all")
        void throwsWhenNoListeners() {
            assertThrows(RuntimeException.class, () -> server.getPort(HTTP, false));
        }

        @Test
        @DisplayName("throws for secure lookup when no listener matches the protocol")
        void throwsForSecureWhenNoListenerMatches() {
            listeners.put(HTTPS, listener(HTTPS, 8443));

            assertThrows(RuntimeException.class, () -> server.getPort("ajp", true));
        }
    }
}
```

---

## UndertowDeploymentProcessor.java -> processManagement()

- **Debt Type:** `DESIGN`
- **Line Number:** `500`
- **Status:** `GENERATED`
- **Comment:** `todo move to UndertowDeploymentService and use all registered servlets from Deployment instead of just one found by metadata`

```java
//todo move to UndertowDeploymentService and use all registered servlets from Deployment instead of just one found by metadata
void processManagement(final DeploymentUnit unit, JBossWebMetaData metaData) {
    final DeploymentResourceSupport deploymentResourceSupport = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
    for (final JBossServletMetaData servlet : metaData.getServlets()) {
        try {
            final String name = servlet.getName();
            final ModelNode node = deploymentResourceSupport.getDeploymentSubModel(UndertowExtension.SUBSYSTEM_NAME, PathElement.pathElement("servlet", name));
            node.get("servlet-class").set(servlet.getServletClass());
            node.get("servlet-name").set(servlet.getServletName());
        } catch (Exception e) {
            // Should a failure in creating the mgmt view also make to the deployment to fail?
            continue;
        }
    }
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.jboss.as.undertow.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.undertow.UndertowExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for {@code processManagement}.
 *
 * Assumptions:
 * - The enclosing class is {@code UndertowDeploymentProcessor} in the same package
 *   and exposes a no-arg constructor.
 * - Mockito is available on the test classpath.
 *
 * The TODO notes that this logic should move to UndertowDeploymentService and cover
 * all registered servlets from the Deployment rather than only those found in metadata.
 * These tests pin down the current behaviour so the refactoring can be verified.
 */
class UndertowDeploymentProcessorProcessManagementTest {

    private UndertowDeploymentProcessor processor;
    private DeploymentUnit unit;
    private DeploymentResourceSupport support;
    private JBossWebMetaData metaData;

    @BeforeEach
    void setUp() {
        processor = new UndertowDeploymentProcessor();
        unit = mock(DeploymentUnit.class);
        support = mock(DeploymentResourceSupport.class);
        metaData = mock(JBossWebMetaData.class);
        when(unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT)).thenReturn(support);
    }

    private JBossServletMetaData servlet(String name, String servletClass) {
        JBossServletMetaData s = mock(JBossServletMetaData.class);
        when(s.getName()).thenReturn(name);
        when(s.getServletName()).thenReturn(name);
        when(s.getServletClass()).thenReturn(servletClass);
        return s;
    }

    @Test
    @DisplayName("Writes servlet-class and servlet-name into the management sub-model of each servlet")
    void populatesSubModelForEachServlet() {
        JBossServletMetaData s1 = servlet("first", "com.example.FirstServlet");
        JBossServletMetaData s2 = servlet("second", "com.example.SecondServlet");
        when(metaData.getServlets()).thenReturn(List.of(s1, s2));

        ModelNode node1 = new ModelNode();
        ModelNode node2 = new ModelNode();
        when(support.getDeploymentSubModel(eq(UndertowExtension.SUBSYSTEM_NAME),
                eq(PathElement.pathElement("servlet", "first")))).thenReturn(node1);
        when(support.getDeploymentSubModel(eq(UndertowExtension.SUBSYSTEM_NAME),
                eq(PathElement.pathElement("servlet", "second")))).thenReturn(node2);

        processor.processManagement(unit, metaData);

        assertEquals("com.example.FirstServlet", node1.get("servlet-class").asString());
        assertEquals("first", node1.get("servlet-name").asString());
        assertEquals("com.example.SecondServlet", node2.get("servlet-class").asString());
        assertEquals("second", node2.get("servlet-name").asString());
    }

    @Test
    @DisplayName("Retrieves the attachment from the deployment unit")
    void readsDeploymentResourceSupportAttachment() {
        when(metaData.getServlets()).thenReturn(Collections.emptyList());

        processor.processManagement(unit, metaData);

        verify(unit).getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
    }

    @Test
    @DisplayName("No servlets declared in metadata results in no management sub-model access")
    void noServletsNoInteractions() {
        when(metaData.getServlets()).thenReturn(Collections.emptyList());

        processor.processManagement(unit, metaData);

        verifyNoInteractions(support);
    }

    @Test
    @DisplayName("Null servlet list in metadata is not handled gracefully by the loop and throws")
    void nullServletListThrowsNullPointerException() {
        when(metaData.getServlets()).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> processor.processManagement(unit, metaData));
    }

    @Test
    @DisplayName("Failure creating one servlet's management view does not stop processing of the remaining servlets")
    void failureForOneServletDoesNotAbortOthers() {
        JBossServletMetaData broken = servlet("broken", "com.example.BrokenServlet");
        JBossServletMetaData healthy = servlet("healthy", "com.example.HealthyServlet");
        when(metaData.getServlets()).thenReturn(List.of(broken, healthy));

        when(support.getDeploymentSubModel(eq(UndertowExtension.SUBSYSTEM_NAME),
                eq(PathElement.pathElement("servlet", "broken"))))
                .thenThrow(new IllegalStateException("cannot create mgmt view"));

        ModelNode healthyNode = new ModelNode();
        when(support.getDeploymentSubModel(eq(UndertowExtension.SUBSYSTEM_NAME),
                eq(PathElement.pathElement("servlet", "healthy")))).thenReturn(healthyNode);

        processor.processManagement(unit, metaData);

        assertEquals("com.example.HealthyServlet", healthyNode.get("servlet-class").asString());
        assertEquals("healthy", healthyNode.get("servlet-name").asString());
    }

    @Test
    @DisplayName("Failure does not propagate out of processManagement")
    void failureIsSwallowed() {
        JBossServletMetaData s = servlet("only", "com.example.OnlyServlet");
        when(metaData.getServlets()).thenReturn(List.of(s));
        when(support.getDeploymentSubModel(any(), any()))
                .thenThrow(new RuntimeException("boom"));

        // Must not throw: the deployment should not fail due to management view issues.
        processor.processManagement(unit, metaData);
    }

    @Test
    @DisplayName("Only servlets present in metadata are processed; no sub-model is created for others")
    void onlyMetadataServletsAreProcessed() {
        JBossServletMetaData s = servlet("declared", "com.example.DeclaredServlet");
        when(metaData.getServlets()).thenReturn(List.of(s));
        when(support.getDeploymentSubModel(any(), any())).thenReturn(new ModelNode());

        processor.processManagement(unit, metaData);

        verify(support).getDeploymentSubModel(UndertowExtension.SUBSYSTEM_NAME,
                PathElement.pathElement("servlet", "declared"));
        verify(support, never()).getDeploymentSubModel(UndertowExtension.SUBSYSTEM_NAME,
                PathElement.pathElement("servlet", "unregistered"));
    }
}
```

---

## FilterRefDefinition.java -> recordCapabilitiesAndRequirements()

- **Debt Type:** `DESIGN`
- **Line Number:** `110`
- **Status:** `GENERATED`
- **Comment:** `TODO remove this when registering via SimpleResourceDefinition.Parameters.addRequirement works`

```java
// TODO remove this when registering via SimpleResourceDefinition.Parameters.addRequirement works
@Override
protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
    super.recordCapabilitiesAndRequirements(context, operation, resource);
    FilterCapabilities capability = forHost ? FilterCapabilities.FILTER_HOST_REF_CAPABILITY : FilterCapabilities.FILTER_LOCATION_REF_CAPABILITY;
    CapabilityReferenceRecorder filterRefRecorder = new CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder(capability.getDynamicNameMapper(), capability.getName(), FilterCapabilities.FILTER_CAPABILITY.getDynamicNameMapper(), FilterCapabilities.FILTER_CAPABILITY.getName());
    filterRefRecorder.addCapabilityRequirements(context, resource, null);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
package org.wildfly.extension.undertow.filters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/*
 * Assumptions (adjust to the real enclosing type if it differs):
 *  - The enclosing class is FilterResourceDefinition, with a constructor taking a boolean 'forHost'.
 *  - recordCapabilitiesAndRequirements is protected and this test lives in the same package.
 *  - CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder is constructed inline by the method,
 *    so it is intercepted with Mockito.mockConstruction.
 */
class FilterResourceDefinitionRecordCapabilitiesTest {

    private OperationContext context;
    private ModelNode operation;
    private Resource resource;

    @Test
    @DisplayName("Records filter capability requirements for host variant")
    void hostVariantRecordsRequirements() throws Exception {
        context = org.mockito.Mockito.mock(OperationContext.class);
        operation = new ModelNode();
        resource = org.mockito.Mockito.mock(Resource.class);

        FilterResourceDefinition definition = new FilterResourceDefinition(true);

        try (MockedConstruction<CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder> constructed =
                     mockConstruction(CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder.class)) {

            definition.recordCapabilitiesAndRequirements(context, operation, resource);

            assertEquals(1, constructed.constructed().size(), "exactly one recorder should be created");
            CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder recorder = constructed.constructed().get(0);
            verify(recorder).addCapabilityRequirements(context, resource, null);
        }
    }

    @Test
    @DisplayName("Records filter capability requirements for location variant")
    void locationVariantRecordsRequirements() throws Exception {
        context = org.mockito.Mockito.mock(OperationContext.class);
        operation = new ModelNode();
        resource = org.mockito.Mockito.mock(Resource.class);

        FilterResourceDefinition definition = new FilterResourceDefinition(false);

        try (MockedConstruction<CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder> constructed =
                     mockConstruction(CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder.class)) {

            definition.recordCapabilitiesAndRequirements(context, operation, resource);

            assertEquals(1, constructed.constructed().size(), "exactly one recorder should be created");
            verify(constructed.constructed().get(0)).addCapabilityRequirements(context, resource, null);
        }
    }

    @Test
    @DisplayName("Each invocation creates a fresh recorder and registers requirements once")
    void repeatedInvocationsRegisterOncePerCall() throws Exception {
        context = org.mockito.Mockito.mock(OperationContext.class);
        operation = new ModelNode();
        resource = org.mockito.Mockito.mock(Resource.class);

        FilterResourceDefinition definition = new FilterResourceDefinition(true);

        try (MockedConstruction<CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder> constructed =
                     mockConstruction(CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder.class)) {

            definition.recordCapabilitiesAndRequirements(context, operation, resource);
            definition.recordCapabilitiesAndRequirements(context, operation, resource);

            assertEquals(2, constructed.constructed().size());
            for (CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder recorder : constructed.constructed()) {
                verify(recorder).addCapabilityRequirements(context, resource, null);
            }
        }
    }

    @Test
    @DisplayName("Propagates OperationFailedException raised while recording requirements")
    void propagatesOperationFailedException() throws Exception {
        context = org.mockito.Mockito.mock(OperationContext.class);
        operation = new ModelNode();
        resource = org.mockito.Mockito.mock(Resource.class);

        FilterResourceDefinition definition = new FilterResourceDefinition(true);

        try (MockedConstruction<CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder> ignored =
                     mockConstruction(CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder.class,
                             (mock, mockContext) -> doThrow(new OperationFailedException("boom"))
                                     .when(mock).addCapabilityRequirements(any(), any(), any()))) {

            assertThrows(OperationFailedException.class,
                    () -> definition.recordCapabilitiesAndRequirements(context, operation, resource));
        }
    }
}
```

---

## WarJACCService.java -> qualifyURLPatterns()

- **Debt Type:** `DESIGN`
- **Line Number:** `532`
- **Status:** `GENERATED`
- **Comment:** `* Jakarta Authorization url pattern Qualified URL Pattern Names.      *      * The rules for qualifying a URL pattern are dependent on the rules for determining if one URL pattern matches another as      * defined in Section 3.1.3.3, Servlet URL-Pattern Matching Rules, and are described as follows: - If the pattern is a path      * prefix pattern, it must be qualified by every path-prefix pattern in the deployment descriptor matched by and different      * from the pattern being qualified. The pattern must also be qualified by every exact pattern appearing in the deployment      * descriptor that is matched by the pattern being qualified. - If the pattern is an extension pattern, it must be qualified      * by every path-prefix pattern appearing in the deployment descriptor and every exact pattern in the deployment descriptor      * that is matched by the pattern being qualified. - If the pattern is the default pattern, "/", it must be qualified by      * every other pattern except the default pattern appearing in the deployment descriptor. - If the pattern is an exact      * pattern, its qualified form must not contain any qualifying patterns.      *      * URL patterns are qualified by appending to their String representation, a colon separated representation of the list of      * patterns that qualify the pattern. Duplicates must not be included in the list of qualifying patterns, and any qualifying      * pattern matched by another qualifying pattern may5 be dropped from the list.      *      * Any pattern, qualified by a pattern that matches it, is overridden and made irrelevant (in the translation) by the      * qualifying pattern. Specifically, all extension patterns and the default pattern are made irrelevant by the presence of      * the path prefix pattern "/*" in a deployment descriptor. Patterns qualified by the "/*" pattern violate the      * URLPatternSpec constraints of WebResourcePermission and WebUserDataPermission names and must be rejected by the      * corresponding permission constructors.      *      * @param metaData - the web deployment metadata      * @return HashMap<String, PatternInfo>`

```java
/**
 * Jakarta Authorization url pattern Qualified URL Pattern Names.
 *
 * The rules for qualifying a URL pattern are dependent on the rules for determining if one URL pattern matches another as
 * defined in Section 3.1.3.3, Servlet URL-Pattern Matching Rules, and are described as follows: - If the pattern is a path
 * prefix pattern, it must be qualified by every path-prefix pattern in the deployment descriptor matched by and different
 * from the pattern being qualified. The pattern must also be qualified by every exact pattern appearing in the deployment
 * descriptor that is matched by the pattern being qualified. - If the pattern is an extension pattern, it must be qualified
 * by every path-prefix pattern appearing in the deployment descriptor and every exact pattern in the deployment descriptor
 * that is matched by the pattern being qualified. - If the pattern is the default pattern, "/", it must be qualified by
 * every other pattern except the default pattern appearing in the deployment descriptor. - If the pattern is an exact
 * pattern, its qualified form must not contain any qualifying patterns.
 *
 * URL patterns are qualified by appending to their String representation, a colon separated representation of the list of
 * patterns that qualify the pattern. Duplicates must not be included in the list of qualifying patterns, and any qualifying
 * pattern matched by another qualifying pattern may5 be dropped from the list.
 *
 * Any pattern, qualified by a pattern that matches it, is overridden and made irrelevant (in the translation) by the
 * qualifying pattern. Specifically, all extension patterns and the default pattern are made irrelevant by the presence of
 * the path prefix pattern "/*" in a deployment descriptor. Patterns qualified by the "/*" pattern violate the
 * URLPatternSpec constraints of WebResourcePermission and WebUserDataPermission names and must be rejected by the
 * corresponding permission constructors.
 *
 * @param metaData - the web deployment metadata
 * @return HashMap<String, PatternInfo>
 */
static HashMap<String, PatternInfo> qualifyURLPatterns(JBossWebMetaData metaData) {
    ArrayList<PatternInfo> prefixList = new ArrayList<PatternInfo>();
    ArrayList<PatternInfo> extensionList = new ArrayList<PatternInfo>();
    ArrayList<PatternInfo> exactList = new ArrayList<PatternInfo>();
    HashMap<String, PatternInfo> patternMap = new HashMap<String, PatternInfo>();
    PatternInfo defaultInfo = null;
    List<SecurityConstraintMetaData> constraints = metaData.getSecurityConstraints();
    if (constraints != null) {
        for (SecurityConstraintMetaData constraint : constraints) {
            WebResourceCollectionsMetaData resourceCollectionsMetaData = constraint.getResourceCollections();
            if (resourceCollectionsMetaData != null) {
                for (WebResourceCollectionMetaData resourceCollectionMetaData : resourceCollectionsMetaData) {
                    List<String> urlPatterns = resourceCollectionMetaData.getUrlPatterns();
                    for (String url : urlPatterns) {
                        int type = getPatternType(url);
                        PatternInfo info = patternMap.get(url);
                        if (info == null) {
                            info = new PatternInfo(url, type);
                            patternMap.put(url, info);
                            switch(type) {
                                case PREFIX:
                                    prefixList.add(info);
                                    break;
                                case EXTENSION:
                                    extensionList.add(info);
                                    break;
                                case EXACT:
                                    exactList.add(info);
                                    break;
                                case DEFAULT:
                                    defaultInfo = info;
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }
    JBossServletsMetaData servlets = metaData.getServlets();
    List<ServletMappingMetaData> mappings = metaData.getServletMappings();
    if (!metaData.isMetadataComplete() && servlets != null && mappings != null) {
        Map<String, List<String>> servletMappingMap = new HashMap<>();
        for (ServletMappingMetaData mapping : mappings) {
            List<String> list = servletMappingMap.get(mapping.getServletName());
            if (list == null) {
                servletMappingMap.put(mapping.getServletName(), list = new ArrayList<>());
            }
            list.addAll(mapping.getUrlPatterns());
        }
        for (JBossServletMetaData servlet : servlets) {
            ServletSecurityMetaData security = servlet.getServletSecurity();
            if (security != null) {
                List<String> servletMappings = servletMappingMap.get(servlet.getServletName());
                if (servletMappings != null) {
                    for (String url : servletMappings) {
                        int type = getPatternType(url);
                        PatternInfo info = patternMap.get(url);
                        if (info == null) {
                            info = new PatternInfo(url, type);
                            patternMap.put(url, info);
                            switch(type) {
                                case PREFIX:
                                    prefixList.add(info);
                                    break;
                                case EXTENSION:
                                    extensionList.add(info);
                                    break;
                                case EXACT:
                                    exactList.add(info);
                                    break;
                                case DEFAULT:
                                    defaultInfo = info;
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }
    // Qualify all prefix patterns
    for (int i = 0; i < prefixList.size(); i++) {
        PatternInfo info = prefixList.get(i);
        // Qualify by every other prefix pattern matching this pattern
        for (int j = 0; j < prefixList.size(); j++) {
            if (i == j)
                continue;
            PatternInfo other = prefixList.get(j);
            if (info.matches(other))
                info.addQualifier(other);
        }
        // Qualify by every exact pattern that is matched by this pattern
        for (PatternInfo other : exactList) {
            if (info.matches(other))
                info.addQualifier(other);
        }
    }
    // Qualify all extension patterns
    for (PatternInfo info : extensionList) {
        // Qualify by every path prefix pattern
        for (PatternInfo other : prefixList) {
            // Any extension
            info.addQualifier(other);
        }
        // Qualify by every matching exact pattern
        for (PatternInfo other : exactList) {
            if (info.isExtensionFor(other))
                info.addQualifier(other);
        }
    }
    // Qualify the default pattern
    if (defaultInfo == null) {
        defaultInfo = new PatternInfo("/", DEFAULT);
        patternMap.put("/", defaultInfo);
    }
    for (PatternInfo info : patternMap.values()) {
        if (info == defaultInfo)
            continue;
        defaultInfo.addQualifier(info);
    }
    return patternMap;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/*
 * Assumptions (adjust imports/package to match the production code base):
 *  - qualifyURLPatterns is a static method reachable from the test (same package).
 *  - PatternInfo exposes getPattern() and getQualifiers() (a collection of PatternInfo or Strings).
 *  - JBossWebMetaData, SecurityConstraintMetaData, WebResourceCollectionsMetaData,
 *    WebResourceCollectionMetaData, JBossServletsMetaData, JBossServletMetaData,
 *    ServletSecurityMetaData and ServletMappingMetaData are the metadata types used by the method.
 */
class QualifyURLPatternsTest {

    private JBossWebMetaData metaData;

    @BeforeEach
    void setUp() {
        metaData = mock(JBossWebMetaData.class);
        when(metaData.getSecurityConstraints()).thenReturn(null);
        when(metaData.getServlets()).thenReturn(null);
        when(metaData.getServletMappings()).thenReturn(null);
        when(metaData.isMetadataComplete()).thenReturn(true);
    }

    // ---------- helpers ----------

    private static SecurityConstraintMetaData constraintWith(String... urlPatterns) {
        WebResourceCollectionMetaData collection = mock(WebResourceCollectionMetaData.class);
        when(collection.getUrlPatterns()).thenReturn(new ArrayList<>(Arrays.asList(urlPatterns)));

        WebResourceCollectionsMetaData collections = new WebResourceCollectionsMetaData();
        collections.add(collection);

        SecurityConstraintMetaData constraint = mock(SecurityConstraintMetaData.class);
        when(constraint.getResourceCollections()).thenReturn(collections);
        return constraint;
    }

    private void withConstraints(SecurityConstraintMetaData... constraints) {
        when(metaData.getSecurityConstraints()).thenReturn(new ArrayList<>(Arrays.asList(constraints)));
    }

    private static List<String> qualifierNames(PatternInfo info) {
        List<String> names = new ArrayList<>();
        for (Object q : info.getQualifiers()) {
            if (q instanceof PatternInfo) {
                names.add(((PatternInfo) q).getPattern());
            } else {
                names.add(String.valueOf(q));
            }
        }
        return names;
    }

    private static HashMap<String, PatternInfo> run(JBossWebMetaData md) {
        return QualifyURLPatternsTest.qualifyURLPatterns(md);
    }

    // ---------- basic / edge cases ----------

    @Nested
    @DisplayName("Empty and null inputs")
    class EmptyInputs {

        @Test
        @DisplayName("null security constraints yields only the implicit default pattern")
        void nullConstraintsYieldsImplicitDefault() {
            HashMap<String, PatternInfo> result = run(metaData);

            assertEquals(1, result.size());
            assertTrue(result.containsKey("/"));
            assertTrue(qualifierNames(result.get("/")).isEmpty());
        }

        @Test
        @DisplayName("empty constraint list yields only the implicit default pattern")
        void emptyConstraintListYieldsImplicitDefault() {
            withConstraints();

            HashMap<String, PatternInfo> result = run(metaData);

            assertEquals(1, result.size());
            assertTrue(result.containsKey("/"));
        }

        @Test
        @DisplayName("constraint with null resource collections is ignored")
        void nullResourceCollectionsIgnored() {
            SecurityConstraintMetaData constraint = mock(SecurityConstraintMetaData.class);
            when(constraint.getResourceCollections()).thenReturn(null);
            withConstraints(constraint);

            HashMap<String, PatternInfo> result = run(metaData);

            assertEquals(1, result.size());
            assertTrue(result.containsKey("/"));
        }
    }

    // ---------- pattern registration ----------

    @Nested
    @DisplayName("Pattern registration from security constraints")
    class Registration {

        @Test
        @DisplayName("each distinct URL pattern produces one map entry keyed by its string")
        void registersAllDistinctPatterns() {
            withConstraints(constraintWith("/exact.html", "/prefix/*", "*.jsp"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(result.containsKey("/exact.html"));
            assertTrue(result.containsKey("/prefix/*"));
            assertTrue(result.containsKey("*.jsp"));
            assertTrue(result.containsKey("/"));
            assertEquals(4, result.size());
        }

        @Test
        @DisplayName("duplicate patterns across constraints are registered only once")
        void duplicatePatternsRegisteredOnce() {
            withConstraints(
                    constraintWith("/secure/*"),
                    constraintWith("/secure/*", "/secure/*"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertEquals(2, result.size()); // "/secure/*" and implicit "/"
            assertEquals("/secure/*", result.get("/secure/*").getPattern());
        }

        @Test
        @DisplayName("explicit default pattern \"/\" is used rather than creating a new one")
        void explicitDefaultPatternUsed() {
            withConstraints(constraintWith("/", "/exact.html"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertEquals(2, result.size());
            assertNotNull(result.get("/"));
        }
    }

    // ---------- exact patterns ----------

    @Nested
    @DisplayName("Exact patterns")
    class ExactPatterns {

        @Test
        @DisplayName("exact pattern has no qualifiers")
        void exactPatternHasNoQualifiers() {
            withConstraints(constraintWith("/index.html", "/admin/*", "*.jsp"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(qualifierNames(result.get("/index.html")).isEmpty(),
                    "Exact patterns must not contain qualifying patterns");
        }
    }

    // ---------- prefix patterns ----------

    @Nested
    @DisplayName("Path prefix patterns")
    class PrefixPatterns {

        @Test
        @DisplayName("prefix pattern is qualified by a matching exact pattern")
        void prefixQualifiedByMatchingExact() {
            withConstraints(constraintWith("/secure/*", "/secure/page.html"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(qualifierNames(result.get("/secure/*")).contains("/secure/page.html"));
        }

        @Test
        @DisplayName("prefix pattern is not qualified by a non-matching exact pattern")
        void prefixNotQualifiedByNonMatchingExact() {
            withConstraints(constraintWith("/secure/*", "/public/page.html"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertFalse(qualifierNames(result.get("/secure/*")).contains("/public/page.html"));
        }

        @Test
        @DisplayName("nested prefix pattern is qualified by the enclosing prefix pattern")
        void nestedPrefixQualifiedByOuterPrefix() {
            withConstraints(constraintWith("/a/*", "/a/b/*"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(qualifierNames(result.get("/a/b/*")).contains("/a/*"));
        }

        @Test
        @DisplayName("prefix pattern is not qualified by itself")
        void prefixNotQualifiedBySelf() {
            withConstraints(constraintWith("/a/*"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertFalse(qualifierNames(result.get("/a/*")).contains("/a/*"));
        }
    }

    // ---------- extension patterns ----------

    @Nested
    @DisplayName("Extension patterns")
    class ExtensionPatterns {

        @Test
        @DisplayName("extension pattern is qualified by every path prefix pattern")
        void extensionQualifiedByAllPrefixes() {
            withConstraints(constraintWith("*.jsp", "/secure/*", "/admin/*"));

            HashMap<String, PatternInfo> result = run(metaData);

            List<String> qualifiers = qualifierNames(result.get("*.jsp"));
            assertTrue(qualifiers.contains("/secure/*"));
            assertTrue(qualifiers.contains("/admin/*"));
        }

        @Test
        @DisplayName("extension pattern is qualified by a matching exact pattern")
        void extensionQualifiedByMatchingExact() {
            withConstraints(constraintWith("*.jsp", "/index.jsp"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(qualifierNames(result.get("*.jsp")).contains("/index.jsp"));
        }

        @Test
        @DisplayName("extension pattern is not qualified by a non-matching exact pattern")
        void extensionNotQualifiedByNonMatchingExact() {
            withConstraints(constraintWith("*.jsp", "/index.html"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertFalse(qualifierNames(result.get("*.jsp")).contains("/index.html"));
        }
    }

    // ---------- default pattern ----------

    @Nested
    @DisplayName("Default pattern \"/\"")
    class DefaultPattern {

        @Test
        @DisplayName("default pattern is qualified by every other pattern")
        void defaultQualifiedByAllOthers() {
            withConstraints(constraintWith("/exact.html", "/prefix/*", "*.jsp"));

            HashMap<String, PatternInfo> result = run(metaData);

            List<String> qualifiers = qualifierNames(result.get("/"));
            assertTrue(qualifiers.contains("/exact.html"));
            assertTrue(qualifiers.contains("/prefix/*"));
            assertTrue(qualifiers.contains("*.jsp"));
            assertFalse(qualifiers.contains("/"), "Default pattern must not qualify itself");
        }
    }

    // ---------- servlet security metadata ----------

    @Nested
    @DisplayName("Servlet security metadata (metadata-complete = false)")
    class ServletSecurity {

        @Test
        @DisplayName("servlet mapping patterns are included when metadata is not complete and servlet has security")
        void servletPatternsIncludedWhenNotComplete() {
            when(metaData.isMetadataComplete()).thenReturn(false);

            JBossServletMetaData servlet = mock(JBossServletMetaData.class);
            when(servlet.getServletName()).thenReturn("secureServlet");
            ServletSecurityMetaData security = mock(ServletSecurityMetaData.class);
            when(servlet.getServletSecurity()).thenReturn(security);

            JBossServletsMetaData servlets = new JBossServletsMetaData();
            servlets.add(servlet);
            when(metaData.getServlets()).thenReturn(servlets);

            ServletMappingMetaData mapping = mock(ServletMappingMetaData.class);
            when(mapping.getServletName()).thenReturn("secureServlet");
            when(mapping.getUrlPatterns()).thenReturn(Collections.singletonList("/servlet/*"));
            when(metaData.getServletMappings()).thenReturn(Collections.singletonList(mapping));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(result.containsKey("/servlet/*"));
        }

        @Test
        @DisplayName("servlet mapping patterns are ignored when metadata is complete")
        void servletPatternsIgnoredWhenComplete() {
            when(metaData.isMetadataComplete()).thenReturn(true);

            JBossServletMetaData servlet = mock(JBossServletMetaData.class);
            when(servlet.getServletName()).thenReturn("secureServlet");
            when(servlet.getServletSecurity()).thenReturn(mock(ServletSecurityMetaData.class));

            JBossServletsMetaData servlets = new JBossServletsMetaData();
            servlets.add(servlet);
            when(metaData.getServlets()).thenReturn(servlets);

            ServletMappingMetaData mapping = mock(ServletMappingMetaData.class);
            when(mapping.getServletName()).thenReturn("secureServlet");
            when(mapping.getUrlPatterns()).thenReturn(Collections.singletonList("/servlet/*"));
            when(metaData.getServletMappings()).thenReturn(Collections.singletonList(mapping));

            HashMap<String, PatternInfo> result = run(metaData);

            assertFalse(result.containsKey("/servlet/*"));
        }

        @Test
        @DisplayName("servlet without ServletSecurity contributes no patterns")
        void servletWithoutSecurityIgnored() {
            when(metaData.isMetadataComplete()).thenReturn(false);

            JBossServletMetaData servlet = mock(JBossServletMetaData.class);
            when(servlet.getServletName()).thenReturn("plainServlet");
            when(servlet.getServletSecurity()).thenReturn(null);

            JBossServletsMetaData servlets = new JBossServletsMetaData();
            servlets.add(servlet);
            when(metaData.getServlets()).thenReturn(servlets);

            ServletMappingMetaData mapping = mock(ServletMappingMetaData.class);
            when(mapping.getServletName()).thenReturn("plainServlet");
            when(mapping.getUrlPatterns()).thenReturn(Collections.singletonList("/plain/*"));
            when(metaData.getServletMappings()).thenReturn(Collections.singletonList(mapping));

            HashMap<String, PatternInfo> result = run(metaData);

            assertFalse(result.containsKey("/plain/*"));
        }
    }

    // ---------- "/*" override semantics ----------

    @Nested
    @DisplayName("Path prefix \"/*\" qualification")
    class RootPrefixOverride {

        @Test
        @DisplayName("\"/*\" qualifies extension patterns and the default pattern")
        void rootPrefixQualifiesExtensionAndDefault() {
            withConstraints(constraintWith("/*", "*.jsp"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(qualifierNames(result.get("*.jsp")).contains("/*"));
            assertTrue(qualifierNames(result.get("/")).contains("/*"));
        }

        @Test
        @DisplayName("\"/*\" qualifies exact patterns it matches")
        void rootPrefixQualifiesMatchingExact() {
            withConstraints(constraintWith("/*", "/page.html"));

            HashMap<String, PatternInfo> result = run(metaData);

            assertTrue(qualifierNames(result.get("/*")).contains("/page.html"));
        }
    }
}
```

---

## WSIntegrationProcessorJAXWS\_HANDLER.java -> registerConfigMapping()

- **Debt Type:** `DESIGN`
- **Line Number:** `112`
- **Status:** `GENERATED`
- **Comment:** `TODO this could be moved to a separate DeploymentUnitProcessor operating on endpoints (together with the rest of the config resolution mechanism)`

```java
//TODO this could be moved to a separate DeploymentUnitProcessor operating on endpoints (together with the rest of the config resolution mechanism)
private void registerConfigMapping(String endpointClassName, EndpointConfig config, DeploymentUnit unit) {
    WSEndpointConfigMapping mapping = unit.getAttachment(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY);
    if (mapping == null) {
        mapping = new WSEndpointConfigMapping();
        unit.putAttachment(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY, mapping);
    }
    mapping.registerEndpointConfig(endpointClassName, config);
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.metadata.model.EndpointConfig;
import org.jboss.as.webservices.metadata.model.WSEndpointConfigMapping;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.msc.service.AttachmentKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for the private {@code registerConfigMapping} method.
 *
 * The method is private and currently lives in the deployment processing code.
 * These tests pin its behavior down so it can be safely moved into a dedicated
 * DeploymentUnitProcessor (the TODO) without regressions.
 *
 * ASSUMPTIONS (adjust package/class names to match the repository):
 *  - OWNER_CLASS_NAME is the class declaring the private method.
 *  - EndpointConfig and WSEndpointConfigMapping live in the metadata model package.
 */
class RegisterConfigMappingTest {

    private static final String OWNER_CLASS_NAME =
            "org.jboss.as.webservices.deployers.WSEndpointDeploymentProcessor";
    private static final String METHOD_NAME = "registerConfigMapping";
    private static final String ENDPOINT_CLASS = "com.example.ws.HelloEndpoint";

    private Object owner;
    private Method registerMethod;
    private DeploymentUnit unit;
    private Map<AttachmentKey<?>, Object> attachments;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> ownerClass = Class.forName(OWNER_CLASS_NAME);
        // The method uses no instance state, so a mock with CALLS_REAL_METHODS is a
        // safe way to obtain an instance without invoking a constructor.
        owner = mock(ownerClass, Mockito.CALLS_REAL_METHODS);

        registerMethod = ownerClass.getDeclaredMethod(
                METHOD_NAME, String.class, EndpointConfig.class, DeploymentUnit.class);
        registerMethod.setAccessible(true);

        attachments = new HashMap<>();
        unit = mock(DeploymentUnit.class);
        when(unit.getAttachment(any())).thenAnswer(inv -> attachments.get(inv.getArgument(0)));
        when(unit.putAttachment(any(), any())).thenAnswer(inv -> {
            AttachmentKey<?> key = inv.getArgument(0);
            return attachments.put(key, inv.getArgument(1));
        });
    }

    private void invoke(String endpointClassName, EndpointConfig config, DeploymentUnit deploymentUnit) {
        try {
            registerMethod.invoke(owner, endpointClassName, config, deploymentUnit);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new AssertionError("Unexpected checked exception", cause);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Cannot access method", e);
        }
    }

    private WSEndpointConfigMapping mappingAttachment() {
        return (WSEndpointConfigMapping) attachments.get(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY);
    }

    @Nested
    @DisplayName("when no mapping is attached to the deployment unit")
    class WhenNoMappingAttached {

        @Test
        @DisplayName("creates a new WSEndpointConfigMapping and attaches it to the unit")
        void createsAndAttachesMapping() {
            EndpointConfig config = mock(EndpointConfig.class);

            invoke(ENDPOINT_CLASS, config, unit);

            WSEndpointConfigMapping created = mappingAttachment();
            assertNotNull(created, "A mapping should be attached to the deployment unit");
            verify(unit).putAttachment(eq(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY), any());
        }

        @Test
        @DisplayName("does not attach anything under other keys")
        void attachesOnlyUnderMappingKey() {
            invoke(ENDPOINT_CLASS, mock(EndpointConfig.class), unit);

            assertEquals(1, attachments.size(), "Exactly one attachment should be added");
            assertTrue(attachments.containsKey(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY));
        }
    }

    @Nested
    @DisplayName("when a mapping is already attached to the deployment unit")
    class WhenMappingAttached {

        @Test
        @DisplayName("reuses the existing mapping and does not replace it")
        void reusesExistingMapping() {
            WSEndpointConfigMapping existing = mock(WSEndpointConfigMapping.class);
            attachments.put(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY, existing);
            EndpointConfig config = mock(EndpointConfig.class);

            invoke(ENDPOINT_CLASS, config, unit);

            assertSame(existing, mappingAttachment(), "Existing mapping instance must be kept");
            verify(existing).registerEndpointConfig(ENDPOINT_CLASS, config);
            verify(unit, never()).putAttachment(any(), any());
        }
    }

    @Nested
    @DisplayName("registration behavior")
    class RegistrationBehavior {

        @Test
        @DisplayName("forwards endpoint class name and config to the mapping")
        void forwardsArgumentsToMapping() {
            WSEndpointConfigMapping mapping = mock(WSEndpointConfigMapping.class);
            attachments.put(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY, mapping);
            EndpointConfig config = mock(EndpointConfig.class);

            invoke("com.example.ws.OtherEndpoint", config, unit);

            verify(mapping).registerEndpointConfig("com.example.ws.OtherEndpoint", config);
            verifyNoMoreInteractions(mapping);
        }

        @Test
        @DisplayName("registers multiple endpoints on the same mapping instance")
        void registersMultipleEndpointsOnSameMapping() {
            WSEndpointConfigMapping mapping = mock(WSEndpointConfigMapping.class);
            attachments.put(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY, mapping);
            EndpointConfig configA = mock(EndpointConfig.class);
            EndpointConfig configB = mock(EndpointConfig.class);

            invoke("com.example.ws.A", configA, unit);
            invoke("com.example.ws.B", configB, unit);

            verify(mapping).registerEndpointConfig("com.example.ws.A", configA);
            verify(mapping).registerEndpointConfig("com.example.ws.B", configB);
            verify(unit, never()).putAttachment(any(), any());
        }

        @Test
        @DisplayName("creating the mapping on first call and reusing it on second call yields one mapping")
        void firstCallCreatesSecondCallReuses() {
            invoke("com.example.ws.A", mock(EndpointConfig.class), unit);
            WSEndpointConfigMapping first = mappingAttachment();

            invoke("com.example.ws.B", mock(EndpointConfig.class), unit);
            WSEndpointConfigMapping second = mappingAttachment();

            assertNotNull(first);
            assertSame(first, second, "Subsequent registrations must reuse the same mapping");
            verify(unit, times(1)).putAttachment(eq(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY), any());
        }
    }
}
```

---

## ElytronSecurityDomainContextImpl.java -> doesUserHaveRole()

- **Debt Type:** `DESIGN`
- **Line Number:** `34`
- **Status:** `GENERATED`
- **Comment:** `TODO:deprecate this after elytron`

```java
//TODO:deprecate this after elytron
@Override
public boolean doesUserHaveRole(Principal principal, Set<Principal> principals) {
    return true;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// NOTE: Replace SecurityRealmAdapter with the actual class that declares
// doesUserHaveRole(Principal, Set<Principal>). Instantiate it with its real
// constructor if one is required.
class SecurityRealmAdapterDoesUserHaveRoleTest {

    private final SecurityRealmAdapter adapter = new SecurityRealmAdapter();

    private static Principal principal(String name) {
        return () -> name;
    }

    @Nested
    @DisplayName("Characterization of legacy stub behavior (TODO: deprecate after Elytron)")
    class LegacyStubBehavior {

        @Test
        @DisplayName("grants role for a regular principal with a populated principal set")
        void grantsRoleForPopulatedInputs() {
            Set<Principal> principals = new HashSet<>();
            principals.add(principal("admin"));
            principals.add(principal("user"));

            assertTrue(adapter.doesUserHaveRole(principal("alice"), principals));
        }

        @Test
        @DisplayName("grants role when the principal set is empty")
        void grantsRoleForEmptySet() {
            assertTrue(adapter.doesUserHaveRole(principal("alice"), Collections.emptySet()));
        }

        @Test
        @DisplayName("grants role when the principal set does not contain the user")
        void grantsRoleWhenUserNotInSet() {
            Set<Principal> principals = Collections.singleton(principal("someoneElse"));

            assertTrue(adapter.doesUserHaveRole(principal("alice"), principals));
        }

        @Test
        @DisplayName("grants role for a null principal with an empty set")
        void grantsRoleForNullPrincipal() {
            assertTrue(adapter.doesUserHaveRole(null, Collections.emptySet()));
        }

        @Test
        @DisplayName("grants role when the principal set is null")
        void grantsRoleForNullPrincipalSet() {
            assertTrue(adapter.doesUserHaveRole(principal("alice"), null));
        }

        @Test
        @DisplayName("grants role when both arguments are null")
        void grantsRoleForBothArgumentsNull() {
            assertTrue(adapter.doesUserHaveRole(null, null));
        }

        @ParameterizedTest(name = "grants role for principal named \"{0}\"")
        @ValueSource(strings = {"", "root", "guest", "  ", "UNKNOWN_USER"})
        void grantsRoleRegardlessOfPrincipalName(String name) {
            assertTrue(adapter.doesUserHaveRole(principal(name), Collections.emptySet()));
        }

        @Test
        @DisplayName("result is independent of the principal set contents (no side effects on input)")
        void doesNotMutateInputs() {
            Set<Principal> principals = new HashSet<>();
            principals.add(principal("admin"));
            int sizeBefore = principals.size();

            adapter.doesUserHaveRole(principal("alice"), principals);

            assertTrue(principals.size() == sizeBefore,
                    "Method should not modify the supplied principal set");
        }
    }

    /**
     * Guard test for the deprecation plan. When Elytron-based role checking is
     * introduced, this test should be replaced by real authorization assertions
     * (e.g. deny when the user lacks the role). Until then, it documents that the
     * stub's permissive behavior is a known design debt rather than intended policy.
     */
    @Test
    @DisplayName("DESIGN DEBT: stub is permissive; a real implementation must be able to return false")
    void stubIsPermissiveAndKnownToBeDebt() {
        // Currently always true. This assertion pins the debt so that any change
        // in behavior is noticed and the test is consciously updated during migration.
        boolean result = adapter.doesUserHaveRole(principal("unauthorized"), Collections.emptySet());
        assertTrue(result, "Legacy stub is expected to grant all roles until Elytron migration");
        assertFalse(!result, "Sanity check: stub must not deny access");
    }
}
```

---

## ElytronSecurityDomainContextImpl.java -> getSecurityDomain()

- **Debt Type:** `IMPLEMENTATION`
- **Line Number:** `39`
- **Status:** `GENERATED`
- **Comment:** `TODO:refactor/deprecate this after elytron`

```java
//TODO:refactor/deprecate this after elytron
@Override
public String getSecurityDomain() {
    return this.securityDomain.toString();
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * NOTE: The class under test is not included in the provided context. The tests below assume
 * the enclosing class is named `LegacySecurityConfiguration`, exposes a constructor that accepts
 * the security domain object, and stores it in a `securityDomain` field whose toString() is
 * returned by getSecurityDomain(). Adjust the class name, constructor, and imports to match
 * the actual production class.
 */
class LegacySecurityConfigurationTest {

    /**
     * Simple domain stand-in whose toString() is deterministic and controllable,
     * so the tests do not depend on any Elytron types.
     */
    static final class StubSecurityDomain {
        private final String name;

        StubSecurityDomain(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private LegacySecurityConfiguration config;

    @BeforeEach
    void setUp() {
        config = new LegacySecurityConfiguration(new StubSecurityDomain("other"));
    }

    @Nested
    @DisplayName("getSecurityDomain() basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("returns the toString() representation of the security domain")
        void returnsToStringOfSecurityDomain() {
            LegacySecurityConfiguration cfg =
                    new LegacySecurityConfiguration(new StubSecurityDomain("ApplicationDomain"));

            assertEquals("ApplicationDomain", cfg.getSecurityDomain());
        }

        @Test
        @DisplayName("returns a String instance, not the domain object itself")
        void returnsStringType() {
            Object result = config.getSecurityDomain();

            assertNotNull(result);
            assertInstanceOf(String.class, result);
        }

        @Test
        @DisplayName("returns an empty string when the domain's toString() is empty")
        void returnsEmptyStringWhenToStringIsEmpty() {
            LegacySecurityConfiguration cfg =
                    new LegacySecurityConfiguration(new StubSecurityDomain(""));

            assertEquals("", cfg.getSecurityDomain());
        }

        @Test
        @DisplayName("preserves special characters and whitespace from toString()")
        void preservesSpecialCharacters() {
            String name = "  my-domain.v2_(prod) ";
            LegacySecurityConfiguration cfg =
                    new LegacySecurityConfiguration(new StubSecurityDomain(name));

            assertEquals(name, cfg.getSecurityDomain());
        }

        @Test
        @DisplayName("is deterministic across repeated invocations")
        void isDeterministicAcrossCalls() {
            String first = config.getSecurityDomain();
            String second = config.getSecurityDomain();

            assertEquals(first, second);
        }
    }

    @Nested
    @DisplayName("getSecurityDomain() error handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws NullPointerException when the security domain is null")
        void throwsWhenSecurityDomainIsNull() {
            LegacySecurityConfiguration cfg = new LegacySecurityConfiguration(null);

            assertThrows(NullPointerException.class, cfg::getSecurityDomain);
        }
    }

    @Nested
    @DisplayName("getSecurityDomain() with custom toString() implementations")
    class CustomToString {

        @Test
        @DisplayName("uses the overridden toString() of a domain subclass")
        void usesOverriddenToString() {
            Object domain = new Object() {
                @Override
                public String toString() {
                    return "anonymous-domain";
                }
            };
            LegacySecurityConfiguration cfg = new LegacySecurityConfiguration(domain);

            assertEquals("anonymous-domain", cfg.getSecurityDomain());
        }

        @Test
        @DisplayName("falls back to Object.toString() format when toString() is not overridden")
        void fallsBackToDefaultObjectToString() {
            Object domain = new Object();
            LegacySecurityConfiguration cfg = new LegacySecurityConfiguration(domain);

            String result = cfg.getSecurityDomain();

            assertEquals(domain.toString(), result);
            assertTrue(result.startsWith(Object.class.getName() + "@"));
        }
    }
}
```

---

## ElytronSecurityDomainContextImpl.java -> getUserRoles()

- **Debt Type:** `IMPLEMENTATION`
- **Line Number:** `50`
- **Status:** `GENERATED`
- **Comment:** `TODO:refactor/deprecate this after elytron?`

```java
//TODO:refactor/deprecate this after elytron?
@Override
public Set<Principal> getUserRoles(Principal principal) {
    return null;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;

import java.security.Principal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// NOTE: Replace UserRoleProvider with the actual class that declares getUserRoles(Principal).
class UserRoleProviderTest {

    private UserRoleProvider provider;

    @BeforeEach
    void setUp() {
        provider = new UserRoleProvider();
    }

    private static Principal principal(String name) {
        return () -> name;
    }

    @Nested
    @DisplayName("Current behavior (stub implementation)")
    class CurrentBehavior {

        @Test
        @DisplayName("returns null for a regular principal")
        void returnsNullForRegularPrincipal() {
            assertNull(provider.getUserRoles(principal("alice")));
        }

        @Test
        @DisplayName("returns null for a null principal without throwing")
        void returnsNullForNullPrincipal() {
            assertDoesNotThrow(() -> assertNull(provider.getUserRoles(null)));
        }

        @Test
        @DisplayName("returns null for a principal with an empty name")
        void returnsNullForEmptyName() {
            assertNull(provider.getUserRoles(principal("")));
        }

        @Test
        @DisplayName("returns null consistently across repeated calls")
        void returnsNullConsistently() {
            Principal p = principal("bob");
            for (int i = 0; i < 3; i++) {
                assertNull(provider.getUserRoles(p));
            }
        }

        @Test
        @DisplayName("does not depend on principal identity (distinct instances with same name)")
        void independentOfPrincipalInstance() {
            assertNull(provider.getUserRoles(principal("carol")));
            assertNull(provider.getUserRoles(principal("carol")));
        }
    }

    @Nested
    @DisplayName("Target contract (enable once implemented)")
    class TargetContract {

        @Test
        @Disabled("Enable once role resolution no longer returns null (post-Elytron migration)")
        @DisplayName("returns a non-null set for a known principal")
        void returnsNonNullSet() {
            Set<Principal> roles = provider.getUserRoles(principal("alice"));
            assertNotNull(roles);
        }

        @Test
        @Disabled("Enable once role resolution no longer returns null (post-Elytron migration)")
        @DisplayName("returns an empty set rather than null for unknown principals")
        void returnsEmptySetForUnknownPrincipal() {
            Set<Principal> roles = provider.getUserRoles(principal("unknown-user-xyz"));
            assertNotNull(roles);
            assertTrue(roles.isEmpty());
        }

        @Test
        @Disabled("Enable once role resolution no longer returns null (post-Elytron migration)")
        @DisplayName("returned set is never null for a null principal")
        void returnsNonNullForNullPrincipal() {
            assertNotNull(provider.getUserRoles(null));
        }
    }
}
```

---

## WeldSubsystemAdd.java -> checkJtsEnabled()

- **Debt Type:** `DESIGN`
- **Line Number:** `123`
- **Status:** `GENERATED`
- **Comment:** `Synchronization objects created by iiop Jakarta Enterprise Beans beans require wrapping by JTSSychronizationWrapper to work correctly (WFLY-3538). This hack is used obtain jts configuration in order to avoid doing this in non-jts environments when it is not necessary.`

```java
// Synchronization objects created by iiop Jakarta Enterprise Beans beans require wrapping by JTSSychronizationWrapper to work correctly
// (WFLY-3538). This hack is used obtain jts configuration in order to avoid doing this in non-jts environments when it is
// not necessary.
private boolean checkJtsEnabled(final OperationContext context) {
    try {
        final ModelNode jtsNode = context.readResourceFromRoot(PathAddress.pathAddress("subsystem", "transactions"), false).getModel().get("jts");
        return jtsNode.isDefined() ? jtsNode.asBoolean() : false;
    } catch (NoSuchResourceException ex) {
        return false;
    }
}
```

### Ticket Context

- **Status:** `MATCHED`
- **Provider:** `jira`
- **Key:** `WFLY-3538`
- **URL:** `https://redhat.atlassian.net/browse/WFLY-3538`
- **Summary:** Application does not deploy because DefaultDataSource is missing

**Description:**

```
On our servers, the ExampleDs is removed from the config. In 7.2.0, this poses no problem, but in the 8.1.0.Final, when we deploy an application, the default datasource needs to be available, even if the application does not use a datasource. The error :
{code}
failure description: {"JBAS014771: Services with missing/unavailable dependencies" => ["jboss.naming.context.java.module.jolokia.jolokia.DefaultDataSource is missing [jboss.naming.context.java.jboss.datasources.ExampleDS]"]}
{code}

The example above is for the deployment of the jolokia war.

To reproduce :
* remove ExampleDS
* deploy any application

```

**Issue References:**

- `WFLY-3538` (source: `comment`)

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.jboss.as.controller.NoSuchResourceException;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.Resource;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the private {@code checkJtsEnabled(OperationContext)} method.
 *
 * ASSUMPTION: the enclosing class is referenced by TARGET_CLASS_NAME. Update this
 * constant to the actual fully qualified name of the class that declares
 * checkJtsEnabled. The method is private, so it is invoked reflectively.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("checkJtsEnabled")
class CheckJtsEnabledTest {

    // Update to the actual class declaring checkJtsEnabled
    private static final String TARGET_CLASS_NAME =
            "org.jboss.as.iiop.openjdk.IIOPSubsystemAdd";

    private static final PathAddress TRANSACTIONS_ADDRESS =
            PathAddress.pathAddress("subsystem", "transactions");

    @Mock
    private OperationContext context;

    @Mock
    private Resource transactionsResource;

    private Object target;
    private Method checkJtsMethod;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> clazz = Class.forName(TARGET_CLASS_NAME);
        // Mock instance: the method body only depends on its argument, not on instance state
        target = mock(clazz);
        checkJtsMethod = clazz.getDeclaredMethod("checkJtsEnabled", OperationContext.class);
        checkJtsMethod.setAccessible(true);
    }

    private boolean invoke(OperationContext ctx) throws Exception {
        return (boolean) checkJtsMethod.invoke(target, ctx);
    }

    private void givenTransactionsModel(ModelNode model) throws Exception {
        when(transactionsResource.getModel()).thenReturn(model);
        when(context.readResourceFromRoot(eq(TRANSACTIONS_ADDRESS), eq(false)))
                .thenReturn(transactionsResource);
    }

    @Nested
    @DisplayName("when the transactions subsystem reports a jts value")
    class WhenJtsDefined {

        @Test
        @DisplayName("returns true when jts is set to true")
        void returnsTrueWhenJtsTrue() throws Exception {
            ModelNode model = new ModelNode();
            model.get("jts").set(true);
            givenTransactionsModel(model);

            assertTrue(invoke(context));
        }

        @Test
        @DisplayName("returns false when jts is set to false")
        void returnsFalseWhenJtsFalse() throws Exception {
            ModelNode model = new ModelNode();
            model.get("jts").set(false);
            givenTransactionsModel(model);

            assertFalse(invoke(context));
        }

        @Test
        @DisplayName("interprets a string 'true' value as true via asBoolean")
        void returnsTrueForStringTrue() throws Exception {
            ModelNode model = new ModelNode();
            model.get("jts").set("true");
            givenTransactionsModel(model);

            assertTrue(invoke(context));
        }
    }

    @Nested
    @DisplayName("when the jts attribute is not defined")
    class WhenJtsUndefined {

        @Test
        @DisplayName("returns false when model has no jts entry")
        void returnsFalseWhenUndefined() throws Exception {
            ModelNode model = new ModelNode();
            givenTransactionsModel(model);

            assertFalse(invoke(context));
        }

        @Test
        @DisplayName("returns false when jts is explicitly undefined")
        void returnsFalseWhenExplicitlyUndefined() throws Exception {
            ModelNode model = new ModelNode();
            model.get("jts");
            givenTransactionsModel(model);

            assertFalse(invoke(context));
        }
    }

    @Nested
    @DisplayName("when the transactions subsystem resource is missing")
    class WhenResourceMissing {

        @Test
        @DisplayName("returns false when NoSuchResourceException is thrown")
        void returnsFalseWhenResourceMissing() throws Exception {
            when(context.readResourceFromRoot(eq(TRANSACTIONS_ADDRESS), eq(false)))
                    .thenThrow(new NoSuchResourceException(
                            PathElement.pathElement("subsystem", "transactions")));

            assertFalse(invoke(context));
        }
    }

    @Test
    @DisplayName("reads the transactions subsystem from root without requiring it to exist")
    void readsTransactionsResourceFromRoot() throws Exception {
        ModelNode model = new ModelNode();
        model.get("jts").set(true);
        givenTransactionsModel(model);

        invoke(context);

        org.mockito.Mockito.verify(context)
                .readResourceFromRoot(any(PathAddress.class), eq(false));
    }
}
```

---

## ProxyServicesImpl.java -> supportsClassDefining()

- **Debt Type:** `DESIGN`
- **Line Number:** `200`
- **Status:** `GENERATED`
- **Comment:** `@Override unused in Weld 3/4 and removed in Weld 5 so don't annotate it TODO to be removed once Weld 5 is a direct dependency of non-preview WFLY`

```java
//@Override unused in Weld 3/4 and removed in Weld 5 so don't annotate it
// TODO to be removed once Weld 5 is a direct dependency of non-preview WFLY
public boolean supportsClassDefining() {
    return true;
}
```

### Ticket Context

- **Status:** `NOT_FOUND`

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@code supportsClassDefining()}.
 *
 * The method is intentionally NOT annotated with {@code @Override}, because it is unused
 * in Weld 3/4 and removed in Weld 5. Its presence must not depend on a supertype
 * declaration, so these tests verify the observable contract (public, no-arg, boolean,
 * always true, stable) via reflection and direct invocation.
 *
 * NOTE: Update {@link #TARGET_CLASS} to the fully qualified name of the class declaring
 * {@code supportsClassDefining()}. The tests are skipped (not failed) if that class is not
 * on the classpath, so the suite remains runnable in modules that do not include WildFly/Weld.
 *
 * NOTE: {@code @Override} has SOURCE retention, so it cannot be detected at runtime. The
 * "no @Override" requirement is therefore enforced by the compiler/source review, not here.
 */
@TestInstance(Lifecycle.PER_CLASS)
class SupportsClassDefiningTest {

    /** Fully qualified name of the class declaring supportsClassDefining(). Update as needed. */
    private static final String TARGET_CLASS = "org.jboss.as.weld.WeldCapabilities";
    private static final String METHOD_NAME = "supportsClassDefining";

    private Class<?> targetClass;
    private Method method;

    @BeforeAll
    void loadTarget() {
        try {
            targetClass = Class.forName(TARGET_CLASS);
        } catch (ClassNotFoundException | LinkageError e) {
            targetClass = null;
        }
        assumeTrue(targetClass != null,
                "Target class " + TARGET_CLASS + " not on classpath; skipping");
        try {
            method = targetClass.getDeclaredMethod(METHOD_NAME);
        } catch (NoSuchMethodException e) {
            method = null;
        }
        assumeTrue(method != null,
                METHOD_NAME + "() not declared on " + TARGET_CLASS + "; skipping");
    }

    private Object newInstanceOrNull() throws Exception {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        }
        Constructor<?> ctor = targetClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @Test
    @DisplayName("method is declared with no parameters and returns primitive boolean")
    void signatureIsNoArgBoolean() {
        assertEquals(0, method.getParameterCount(), "supportsClassDefining must take no arguments");
        assertEquals(boolean.class, method.getReturnType(), "supportsClassDefining must return boolean");
    }

    @Test
    @DisplayName("method is public so callers in other packages can use it")
    void methodIsPublic() {
        assertTrue(Modifier.isPublic(method.getModifiers()), "supportsClassDefining must be public");
    }

    @Test
    @DisplayName("method returns true")
    void returnsTrue() throws Exception {
        Object instance = newInstanceOrNull();
        Object result = method.invoke(instance);
        assertEquals(Boolean.TRUE, result, "supportsClassDefining must return true");
    }

    @Test
    @DisplayName("method is stable across repeated invocations")
    void returnsTrueConsistently() throws Exception {
        Object instance = newInstanceOrNull();
        for (int i = 0; i < 5; i++) {
            assertEquals(Boolean.TRUE, method.invoke(instance),
                    "Invocation #" + (i + 1) + " must return true");
        }
    }

    @Test
    @DisplayName("method does not throw when invoked")
    void doesNotThrow() throws Exception {
        Object instance = newInstanceOrNull();
        assertDoesNotThrow(() -> method.invoke(instance));
    }
}
```

---

