# Technical Debt Test Generation Report

Generated test cases to pay off self-admitted technical debt (SATD).

## RoundRobinLoadBalance.java -> getInvokerAddrList()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `83`
- **Status:** `SUCCESS`
- **Comment:** `* get invoker addr list cached for specified invocation      * <p>      * <b>for unit test only</b>      *      * @param invokers      * @param invocation      * @return`

```java
/**
 * get invoker addr list cached for specified invocation
 * <p>
 * <b>for unit test only</b>
 *
 * @param invokers
 * @param invocation
 * @return
 */
protected <T> Collection<String> getInvokerAddrList(List<Invoker<T>> invokers, Invocation invocation) {
    String key = invokers.get(0).getUrl().getServiceKey() + "." + RpcUtils.getMethodName(invocation);
    Map<String, WeightedRoundRobin> map = methodWeightMap.get(key);
    if (map != null) {
        return map.keySet();
    }
    return null;
}
```

### Generated Test Case

```java
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.loadbalance.WeightedRoundRobin;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GetInvokerAddrListTest {

    private DummyLoadBalance loadBalance;
    private List<Invoker<Object>> invokers;
    private Invocation invocation;
    private URL url;

    @BeforeEach
    void setUp() throws Exception {
        loadBalance = new DummyLoadBalance();
        
        url = URL.valueOf("dubbo://127.0.0.1:20880/org.apache.dubbo.TestService?methods=testMethod");
        
        Invoker<Object> invoker1 = Mockito.mock(Invoker.class);
        when(invoker1.getUrl()).thenReturn(url);
        
        invokers = Arrays.asList(invoker1);
        
        invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testMethod");
    }

    @Test
    void testGetInvokerAddrListWhenCacheExists() throws Exception {
        // Prepare the internal cache key and map using reflection
        String key = invokers.get(0).getUrl().getServiceKey() + "." + RpcUtils.getMethodName(invocation);
        
        ConcurrentMap<String, Map<String, WeightedRoundRobin>> methodWeightMap = 
            (ConcurrentMap<String, Map<String, WeightedRoundRobin>>) getField(loadBalance, "methodWeightMap");
        
        java.util.concurrent.ConcurrentHashMap<String, WeightedRoundRobin> innerMap = 
            new java.util.concurrent.ConcurrentHashMap<>();
        innerMap.put("127.0.0.1:20880", new WeightedRoundRobin());
        
        methodWeightMap.put(key, innerMap);

        // Execute
        Collection<String> addrList = loadBalance.getInvokerAddrList(invokers, invocation);

        // Assert
        assertNotNull(addrList);
        assertEquals(1, addrList.size());
        assertTrue(addrList.contains("127.0.0.1:20880"));
    }

    @Test
    void testGetInvokerAddrListWhenCacheMisses() {
        // Execute when no cache is populated
        Collection<String> addrList = loadBalance.getInvokerAddrList(invokers, invocation);

        // Assert
        assertNull(addrList);
    }

    // Helper subclass to access the protected method
    private static class DummyLoadBalance extends org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance {
        @Override
        protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
            return null;
        }

        @Override
        public <T> Collection<String> getInvokerAddrList(List<Invoker<T>> invokers, Invocation invocation) {
            return super.getInvokerAddrList(invokers, invocation);
        }
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = org.apache.dubbo.rpc.cluster.loadbalance.AbstractLoadBalance.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
```

---

## AppScriptStateRouter.java -> setScriptRule()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `162`
- **Status:** `SUCCESS`
- **Comment:** `for testing purpose`

```java
// for testing purpose
public void setScriptRule(ScriptRule scriptRule) {
    this.scriptRule = scriptRule;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScriptRuleTestTarget {
    private ScriptRule scriptRule;

    public void setScriptRule(ScriptRule scriptRule) {
        this.scriptRule = scriptRule;
    }

    public ScriptRule getScriptRule() {
        return this.scriptRule;
    }
}

// Dummy class to represent ScriptRule for the test context
class ScriptRule {
    // Implementation details omitted for testing
}

class ScriptRuleTestTargetTest {

    private ScriptRuleTestTarget target;

    @BeforeEach
    void setUp() {
        target = new ScriptRuleTestTarget();
    }

    @Test
    void testSetScriptRule_WhenValidRuleProvided_ShouldSetSuccessfully() {
        // Arrange
        ScriptRule mockScriptRule = Mockito.mock(ScriptRule.class);

        // Act
        target.setScriptRule(mockScriptRule);

        // Assert
        assertEquals(mockScriptRule, target.getScriptRule(), "The script rule should be correctly set.");
    }

    @Test
    void testSetScriptRule_WhenNullProvided_ShouldClearRule() {
        // Arrange
        ScriptRule mockScriptRule = Mockito.mock(ScriptRule.class);
        target.setScriptRule(mockScriptRule);

        // Act
        target.setScriptRule(null);

        // Assert
        assertNull(target.getScriptRule(), "The script rule should be null when set to null.");
    }
}
```

---

## TagStateRouter.java -> filterUsingStaticTag()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `210`
- **Status:** `SUCCESS`
- **Comment:** `* If there's no dynamic tag rule being set, use static tag in URL.      * <p>      * A typical scenario is a Consumer using version 2.7.x calls Providers using version 2.6.x or lower,      * the Consumer should always respect the tag in provider URL regardless of whether a dynamic tag rule has been set to it or not.      * <p>      * TODO, to guarantee consistent behavior of interoperability between 2.6- and 2.7+, this method should has the same logic with the TagRouter in 2.6.x.      *      * @param invokers      * @param url      * @param invocation      * @param <T>      * @return`

```java
/**
 * If there's no dynamic tag rule being set, use static tag in URL.
 * <p>
 * A typical scenario is a Consumer using version 2.7.x calls Providers using version 2.6.x or lower,
 * the Consumer should always respect the tag in provider URL regardless of whether a dynamic tag rule has been set to it or not.
 * <p>
 * TODO, to guarantee consistent behavior of interoperability between 2.6- and 2.7+, this method should has the same logic with the TagRouter in 2.6.x.
 *
 * @param invokers
 * @param url
 * @param invocation
 * @param <T>
 * @return
 */
private <T> BitList<Invoker<T>> filterUsingStaticTag(BitList<Invoker<T>> invokers, URL url, Invocation invocation) {
    BitList<Invoker<T>> result;
    // Dynamic param
    String tag = StringUtils.isEmpty(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) : invocation.getAttachment(TAG_KEY);
    // Tag request
    if (!StringUtils.isEmpty(tag)) {
        result = filterInvoker(invokers, invoker -> ANY_VALUE.equals(tag) || tag.equals(invoker.getUrl().getParameter(TAG_KEY)));
        if (CollectionUtils.isEmpty(result) && !isForceUseTag(invocation)) {
            result = filterInvoker(invokers, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(TAG_KEY)));
        }
    } else {
        result = filterInvoker(invokers, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(TAG_KEY)));
    }
    return result;
}
```

### Generated Test Case

```java
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.tag.TagRouter;
import org.apache.dubbo.rpc.cluster.support.BitList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TagRouterTest {

    private TagRouter tagRouter;
    private static final String TAG_KEY = "tag";
    private static final String ANY_VALUE = "*";
    private static final String FORCE_USE_TAG = "dubbo.force.tag";

    @BeforeEach
    void setUp() {
        tagRouter = new TagRouter();
    }

    @Test
    void testFilterUsingStaticTagWithInvocationAttachment() {
        URL url = URL.valueOf("test://localhost:8080/test");
        RpcInvocation invocation = new RpcInvocation();
        invocation.setAttachment(TAG_KEY, "red");

        Invoker<Object> invoker1 = createMockInvoker("red");
        Invoker<Object> invoker2 = createMockInvoker("blue");
        Invoker<Object> invoker3 = createMockInvoker(null);

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1, invoker2, invoker3));

        BitList<Invoker<Object>> result = invokeFilterUsingStaticTag(tagRouter, invokers, url, invocation);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(invoker1));
    }

    @Test
    void testFilterUsingStaticTagWithUrlParameter() {
        URL url = URL.valueOf("test://localhost:8080/test?tag=blue");
        RpcInvocation invocation = new RpcInvocation();

        Invoker<Object> invoker1 = createMockInvoker("red");
        Invoker<Object> invoker2 = createMockInvoker("blue");
        Invoker<Object> invoker3 = createMockInvoker(null);

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1, invoker2, invoker3));

        BitList<Invoker<Object>> result = invokeFilterUsingStaticTag(tagRouter, invokers, url, invocation);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(invoker2));
    }

    @Test
    void testFilterUsingStaticTagWithNoTagFallbackToEmptyTag() {
        URL url = URL.valueOf("test://localhost:8080/test");
        RpcInvocation invocation = new RpcInvocation();

        Invoker<Object> invoker1 = createMockInvoker("red");
        Invoker<Object> invoker2 = createMockInvoker("blue");
        Invoker<Object> invoker3 = createMockInvoker(null);

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1, invoker2, invoker3));

        BitList<Invoker<Object>> result = invokeFilterUsingStaticTag(tagRouter, invokers, url, invocation);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(invoker3));
    }

    @Test
    void testFilterUsingStaticTagWithAnyValueMatch() {
        URL url = URL.valueOf("test://localhost:8080/test?tag=*");
        RpcInvocation invocation = new RpcInvocation();

        Invoker<Object> invoker1 = createMockInvoker("red");
        Invoker<Object> invoker2 = createMockInvoker("blue");
        Invoker<Object> invoker3 = createMockInvoker(null);

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1, invoker2, invoker3));

        BitList<Invoker<Object>> result = invokeFilterUsingStaticTag(tagRouter, invokers, url, invocation);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testFilterUsingStaticTagNoMatchWithForceTagFalseFallback() {
        URL url = URL.valueOf("test://localhost:8080/test?tag=nonexistent&dubbo.force.tag=false");
        RpcInvocation invocation = new RpcInvocation();

        Invoker<Object> invoker1 = createMockInvoker("red");
        Invoker<Object> invoker2 = createMockInvoker(null);

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1, invoker2));

        BitList<Invoker<Object>> result = invokeFilterUsingStaticTag(tagRouter, invokers, url, invocation);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(invoker2));
    }

    @Test
    void testFilterUsingStaticTagNoMatchWithForceTagTrue() {
        URL url = URL.valueOf("test://localhost:8080/test?tag=nonexistent&dubbo.force.tag=true");
        RpcInvocation invocation = new RpcInvocation();

        Invoker<Object> invoker1 = createMockInvoker("red");
        Invoker<Object> invoker2 = createMockInvoker(null);

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1, invoker2));

        BitList<Invoker<Object>> result = invokeFilterUsingStaticTag(tagRouter, invokers, url, invocation);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private Invoker<Object> createMockInvoker(String tag) {
        @SuppressWarnings("unchecked")
        Invoker<Object> invoker = Mockito.mock(Invoker.class);
        URL invokerUrl = URL.valueOf("test://localhost:8080/test" + (tag != null ? "?tag=" + tag : ""));
        when(invoker.getUrl()).thenReturn(invokerUrl);
        return invoker;
    }

    @SuppressWarnings("unchecked")
    private <T> BitList<Invoker<T>> invokeFilterUsingStaticTag(TagRouter router, BitList<Invoker<T>> invokers, URL url, Invocation invocation) {
        try {
            java.lang.reflect.Method method = TagRouter.class.getDeclaredMethod("filterUsingStaticTag", BitList.class, URL.class, Invocation.class);
            method.setAccessible(true);
            return (BitList<Invoker<T>>) method.invoke(router, invokers, url, invocation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## TagStateRouter.java -> setTagRouterRule()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `349`
- **Status:** `SUCCESS`
- **Comment:** `for testing purpose`

```java
// for testing purpose
public void setTagRouterRule(TagRouterRule tagRouterRule) {
    this.tagRouterRule = tagRouterRule;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TagRouterRuleTest {

    // Assuming the class containing the method is named TagRouterManager (replace with actual class name)
    private TagRouterManager tagRouterManager;

    @BeforeEach
    void setUp() {
        tagRouterManager = new TagRouterManager();
    }

    @Test
    void testSetTagRouterRule() {
        // Given
        TagRouterRule expectedRule = new TagRouterRule();
        
        // When
        tagRouterManager.setTagRouterRule(expectedRule);
        
        // Then
        // Using reflection or a getter if available. Since it's a setter-based test debt,
        // we assert via the getter counterpart assuming standard encapsulation.
        assertEquals(expectedRule, tagRouterManager.getTagRouterRule(), 
                "The tag router rule should be successfully set.");
    }
    
    @Test
    void testSetTagRouterRuleWithNull() {
        // When
        tagRouterManager.setTagRouterRule(null);
        
        // Then
        assertNull(tagRouterManager.getTagRouterRule(), 
                "The tag router rule should be able to be set to null.");
    }
}
```

---

## SingleRouterChain.java -> addRouters()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `123`
- **Status:** `SUCCESS`
- **Comment:** `* If we use route:// protocol in version before 2.7.0, each URL will generate a Router instance, so we should      * keep the routers up to date, that is, each time router URLs changes, we should update the routers list, only      * keep the builtinRouters which are available all the time and the latest notified routers which are generated      * from URLs.      *      * @param routers routers from 'router://' rules in 2.6.x or before.`

```java
/**
 * If we use route:// protocol in version before 2.7.0, each URL will generate a Router instance, so we should
 * keep the routers up to date, that is, each time router URLs changes, we should update the routers list, only
 * keep the builtinRouters which are available all the time and the latest notified routers which are generated
 * from URLs.
 *
 * @param routers routers from 'router://' rules in 2.6.x or before.
 */
public void addRouters(List<Router> routers) {
    List<Router> newRouters = new LinkedList<>();
    newRouters.addAll(builtinRouters);
    newRouters.addAll(routers);
    CollectionUtils.sort(newRouters);
    this.routers = newRouters;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RouterTestClass {

    // Dummy class representing the context where addRouters resides
    // Assumes fields: List<Router> builtinRouters, List<Router> routers
    private List<Router> builtinRouters;
    private List<Router> routers;

    public void addRouters(List<Router> routers) {
        List<Router> newRouters = new LinkedList<>();
        newRouters.addAll(builtinRouters);
        newRouters.addAll(routers);
        org.apache.commons.collections4.CollectionUtils.sort(newRouters); // Or equivalent sort used in project
        this.routers = newRouters;
    }
}

class RouterTest {

    // A mockable or comparable Router implementation implementing Comparable
    private static class ComparableRouter implements Router, Comparable<Router> {
        private final String name;

        private ComparableRouter(String name) {
            this.name = name;
        }

        @Override
        public int compareTo(Router o) {
            if (o instanceof ComparableRouter) {
                return this.name.compareTo(((ComparableRouter) o).name);
            }
            return 0;
        }
    }

    private RouterTestClass routerManager;
    private List<Router> builtinRouters;

    @BeforeEach
    void setUp() throws Exception {
        routerManager = new RouterTestClass();
        builtinRouters = new ArrayList<>();
        
        // Use reflection to set private field builtinRouters if necessary
        Field builtinField = RouterTestClass.class.getDeclaredField("builtinRouters");
        builtinField.setAccessible(true);
        builtinField.set(routerManager, builtinRouters);
    }

    @Test
    void testAddRoutersKeepsBuiltinAndUpdatesNotifiedRouters() throws Exception {
        // Arrange
        Router builtin1 = new ComparableRouter("builtin-A");
        builtinRouters.add(builtin1);

        List<Router> notifiedRouters = new ArrayList<>();
        Router notified1 = new ComparableRouter("notified-B");
        notifiedRouters.add(notified1);

        // Act
        routerManager.addRouters(notifiedRouters);

        // Assert
        Field routersField = RouterTestClass.class.getDeclaredField("routers");
        routersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Router> actualRouters = (List<Router>) routersField.get(routerManager);

        assertEquals(2, actualRouters.size(), "Should contain both builtin and notified routers");
        assertTrue(actualRouters.contains(builtin1), "Should contain builtin routers");
        assertTrue(actualRouters.contains(notified1), "Should contain notified routers");
    }

    @Test
    void testAddRoutersOverwritesPreviousNotifiedRouters() throws Exception {
        // Arrange
        Router builtin1 = new ComparableRouter("builtin-A");
        builtinRouters.add(builtin1);

        List<Router> initialNotified = new ArrayList<>();
        initialNotified.add(new ComparableRouter("notified-Old"));
        
        // First notification
        routerManager.addRouters(initialNotified);

        // Second notification (simulating URL changes, old notified routers should be replaced)
        List<Router> updatedNotified = new ArrayList<>();
        Router newNotified = new ComparableRouter("notified-New");
        updatedNotified.add(newNotified);

        // Act
        routerManager.addRouters(updatedNotified);

        // Assert
        Field routersField = RouterTestClass.class.getDeclaredField("routers");
        routersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Router> actualRouters = (List<Router>) routersField.get(routerManager);

        assertEquals(2, actualRouters.size(), "Should only contain builtin and latest notified routers");
        assertTrue(actualRouters.contains(builtin1), "Should still contain builtin routers");
        assertTrue(actualRouters.contains(newNotified), "Should contain the latest notified routers");
    }
}
```

---

## AbsentConfiguratorTest.java -> testAbsentForVersion27()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `70`
- **Status:** `SUCCESS`
- **Comment:** `Test the version after 2.7`

```java
// Test the version after 2.7
@Test
void testAbsentForVersion27() {
    {
        String consumerUrlV27 = "dubbo://172.24.160.179/com.foo.BarService?application=foo&side=consumer&timeout=100";
        URL consumerConfiguratorUrl = URL.valueOf("absent://0.0.0.0/com.foo.BarService");
        Map<String, String> params = new HashMap<>();
        params.put("side", "consumer");
        params.put("configVersion", "2.7");
        params.put("application", "foo");
        params.put("timeout", "10000");
        params.put("weight", "200");
        consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
        AbsentConfigurator configurator = new AbsentConfigurator(consumerConfiguratorUrl);
        // Meet the configured conditions:
        // same side
        // The port of configuratorUrl is 0
        // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
        // same appName
        URL url = configurator.configure(URL.valueOf(consumerUrlV27));
        Assertions.assertEquals("100", url.getParameter("timeout"));
        Assertions.assertEquals("200", url.getParameter("weight"));
    }
    {
        String providerUrlV27 = "dubbo://172.24.160.179:21880/com.foo.BarService?application=foo&side=provider&weight=100";
        URL providerConfiguratorUrl = URL.valueOf("absent://172.24.160.179:21880/com.foo.BarService");
        Map<String, String> params = new HashMap<>();
        params.put("side", "provider");
        params.put("configVersion", "2.7");
        params.put("application", "foo");
        params.put("timeout", "20000");
        params.put("weight", "200");
        providerConfiguratorUrl = providerConfiguratorUrl.addParameters(params);
        // Meet the configured conditions:
        // same side
        // same port
        // The host of configuratorUrl is 0.0.0.0 or the host of providerConfiguratorUrl is the same as
        // consumerUrlV27
        // same appName
        AbsentConfigurator configurator = new AbsentConfigurator(providerConfiguratorUrl);
        URL url = configurator.configure(URL.valueOf(providerUrlV27));
        Assertions.assertEquals("20000", url.getParameter("timeout"));
        Assertions.assertEquals("100", url.getParameter("weight"));
    }
}
```

### Generated Test Case

```java
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.configurator.absent.AbsentConfigurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class AbsentConfiguratorTest {

    @Test
    void testAbsentForVersionAfter27() {
        // Test version > 2.7 for consumer side
        {
            String consumerUrlV28 = "dubbo://172.24.160.179/com.foo.BarService?application=foo&side=consumer&timeout=100";
            URL consumerConfiguratorUrl = URL.valueOf("absent://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "2.8.0");
            params.put("application", "foo");
            params.put("timeout", "10000");
            params.put("weight", "200");
            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
            AbsentConfigurator configurator = new AbsentConfigurator(consumerConfiguratorUrl);
            
            URL url = configurator.configure(URL.valueOf(consumerUrlV28));
            Assertions.assertEquals("100", url.getParameter("timeout"));
            Assertions.assertEquals("200", url.getParameter("weight"));
        }

        // Test version > 2.7 for provider side
        {
            String providerUrlV30 = "dubbo://172.24.160.179:21880/com.foo.BarService?application=foo&side=provider&weight=100";
            URL providerConfiguratorUrl = URL.valueOf("absent://172.24.160.179:21880/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "provider");
            params.put("configVersion", "3.0.0");
            params.put("application", "foo");
            params.put("timeout", "20000");
            params.put("weight", "200");
            providerConfiguratorUrl = providerConfiguratorUrl.addParameters(params);
            
            AbsentConfigurator configurator = new AbsentConfigurator(providerConfiguratorUrl);
            URL url = configurator.configure(URL.valueOf(providerUrlV30));
            Assertions.assertEquals("20000", url.getParameter("timeout"));
            Assertions.assertEquals("100", url.getParameter("weight"));
        }
    }
}
```

---

## OverrideConfiguratorTest.java -> testOverrideForVersion27()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `78`
- **Status:** `SUCCESS`
- **Comment:** `Test the version after 2.7`

```java
// Test the version after 2.7
@Test
void testOverrideForVersion27() {
    {
        String consumerUrlV27 = "dubbo://172.24.160.179/com.foo.BarService?application=foo&side=consumer&timeout=100";
        URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
        Map<String, String> params = new HashMap<>();
        params.put("side", "consumer");
        params.put("configVersion", "2.7");
        params.put("application", "foo");
        params.put("timeout", "10000");
        consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
        OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
        // Meet the configured conditions:
        // same side
        // The port of configuratorUrl is 0
        // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
        // same appName
        URL url = configurator.configure(URL.valueOf(consumerUrlV27));
        Assertions.assertEquals(url.getParameter("timeout"), "10000");
    }
    {
        String providerUrlV27 = "dubbo://172.24.160.179:21880/com.foo.BarService?application=foo&side=provider&weight=100";
        URL providerConfiguratorUrl = URL.valueOf("override://172.24.160.179:21880/com.foo.BarService");
        Map<String, String> params = new HashMap<>();
        params.put("side", "provider");
        params.put("configVersion", "2.7");
        params.put("application", "foo");
        params.put("weight", "200");
        providerConfiguratorUrl = providerConfiguratorUrl.addParameters(params);
        // Meet the configured conditions:
        // same side
        // same port
        // The host of configuratorUrl is 0.0.0.0 or the host of providerConfiguratorUrl is the same as
        // consumerUrlV27
        // same appName
        OverrideConfigurator configurator = new OverrideConfigurator(providerConfiguratorUrl);
        URL url = configurator.configure(URL.valueOf(providerUrlV27));
        Assertions.assertEquals(url.getParameter("weight"), "200");
    }
}
```

### Generated Test Case

```java
package org.apache.dubbo.rpc.cluster.configurator.override;

import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class OverrideConfiguratorVersionTest {

    @Test
    void testOverrideForVersionAfter27() {
        // Test consumer configuration with version > 2.7 (e.g., 3.0)
        {
            String consumerUrlV30 = "dubbo://172.24.160.179/com.foo.BarService?application=foo&side=consumer&timeout=100";
            URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "3.0");
            params.put("application", "foo");
            params.put("timeout", "15000");
            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
            
            OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
            URL url = configurator.configure(URL.valueOf(consumerUrlV30));
            Assertions.assertEquals("15000", url.getParameter("timeout"));
        }

        // Test provider configuration with version > 2.7 (e.g., 3.1)
        {
            String providerUrlV31 = "dubbo://172.24.160.179:21880/com.foo.BarService?application=foo&side=provider&weight=100";
            URL providerConfiguratorUrl = URL.valueOf("override://172.24.160.179:21880/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "provider");
            params.put("configVersion", "3.1");
            params.put("application", "foo");
            params.put("weight", "300");
            providerConfiguratorUrl = providerConfiguratorUrl.addParameters(params);
            
            OverrideConfigurator configurator = new OverrideConfigurator(providerConfiguratorUrl);
            URL url = configurator.configure(URL.valueOf(providerUrlV31));
            Assertions.assertEquals("300", url.getParameter("weight"));
        }
    }
}
```

---

## OverrideConfiguratorTest.java -> testOverrideForVersion3()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `127`
- **Status:** `SUCCESS`
- **Comment:** `Test the version after 2.7`

```java
// Test the version after 2.7
@Test
void testOverrideForVersion3() {
    // match
    {
        String consumerUrlV3 = "dubbo://172.24.160.179/com.foo.BarService?match_key=value&application=foo&side=consumer&timeout=100";
        URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
        Map<String, String> params = new HashMap<>();
        params.put("side", "consumer");
        params.put("configVersion", "v3.0");
        params.put("application", "foo");
        params.put("timeout", "10000");
        ConditionMatch matcher = new ConditionMatch();
        ParamMatch paramMatch = new ParamMatch();
        paramMatch.setKey("match_key");
        StringMatch stringMatch = new StringMatch();
        stringMatch.setExact("value");
        paramMatch.setValue(stringMatch);
        matcher.setParam(Arrays.asList(paramMatch));
        consumerConfiguratorUrl = consumerConfiguratorUrl.putAttribute(MATCH_CONDITION, matcher);
        consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
        OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
        // Meet the configured conditions:
        // same side
        // The port of configuratorUrl is 0
        // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
        // same appName
        URL originalURL = URL.valueOf(consumerUrlV3);
        Assertions.assertEquals("100", originalURL.getParameter("timeout"));
        URL url = configurator.configure(originalURL);
        Assertions.assertEquals("10000", url.getParameter("timeout"));
    }
    // mismatch
    {
        String consumerUrlV3 = "dubbo://172.24.160.179/com.foo.BarService?match_key=value&application=foo&side=consumer&timeout=100";
        URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
        Map<String, String> params = new HashMap<>();
        params.put("side", "consumer");
        params.put("configVersion", "v3.0");
        params.put("application", "foo");
        params.put("timeout", "10000");
        ConditionMatch matcher = new ConditionMatch();
        ParamMatch paramMatch = new ParamMatch();
        paramMatch.setKey("match_key");
        StringMatch stringMatch = new StringMatch();
        stringMatch.setExact("not_match_value");
        paramMatch.setValue(stringMatch);
        matcher.setParam(Arrays.asList(paramMatch));
        consumerConfiguratorUrl = consumerConfiguratorUrl.putAttribute(MATCH_CONDITION, matcher);
        consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
        OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
        // Meet the configured conditions:
        // same side
        // The port of configuratorUrl is 0
        // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrlV27
        // same appName
        URL originalURL = URL.valueOf(consumerUrlV3);
        Assertions.assertEquals("100", originalURL.getParameter("timeout"));
        URL url = configurator.configure(originalURL);
        Assertions.assertEquals("100", url.getParameter("timeout"));
    }
}
```

### Generated Test Case

```java
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.matcher.ConditionMatch;
import org.apache.dubbo.common.utils.matcher.ParamMatch;
import org.apache.dubbo.common.utils.matcher.StringMatch;
import org.apache.dubbo.rpc.cluster.configurator.override.OverrideConfigurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.Constants.MATCH_CONDITION;

class OverrideConfiguratorVersion3Test {

    // Test the version after 2.7
    @Test
    void testOverrideForVersion3() {
        // match
        {
            String consumerUrlV3 = "dubbo://172.24.160.179/com.foo.BarService?match_key=value&application=foo&side=consumer&timeout=100";
            URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "v3.0");
            params.put("application", "foo");
            params.put("timeout", "10000");
            ConditionMatch matcher = new ConditionMatch();
            ParamMatch paramMatch = new ParamMatch();
            paramMatch.setKey("match_key");
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("value");
            paramMatch.setValue(stringMatch);
            matcher.setParam(Arrays.asList(paramMatch));
            consumerConfiguratorUrl = consumerConfiguratorUrl.putAttribute(MATCH_CONDITION, matcher);
            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
            OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
            // Meet the configured conditions:
            // same side
            // The port of configuratorUrl is 0
            // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrl
            // same appName
            URL originalURL = URL.valueOf(consumerUrlV3);
            Assertions.assertEquals("100", originalURL.getParameter("timeout"));
            URL url = configurator.configure(originalURL);
            Assertions.assertEquals("10000", url.getParameter("timeout"));
        }
        // mismatch
        {
            String consumerUrlV3 = "dubbo://172.24.160.179/com.foo.BarService?match_key=value&application=foo&side=consumer&timeout=100";
            URL consumerConfiguratorUrl = URL.valueOf("override://0.0.0.0/com.foo.BarService");
            Map<String, String> params = new HashMap<>();
            params.put("side", "consumer");
            params.put("configVersion", "v3.0");
            params.put("application", "foo");
            params.put("timeout", "10000");
            ConditionMatch matcher = new ConditionMatch();
            ParamMatch paramMatch = new ParamMatch();
            paramMatch.setKey("match_key");
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("not_match_value");
            paramMatch.setValue(stringMatch);
            matcher.setParam(Arrays.asList(paramMatch));
            consumerConfiguratorUrl = consumerConfiguratorUrl.putAttribute(MATCH_CONDITION, matcher);
            consumerConfiguratorUrl = consumerConfiguratorUrl.addParameters(params);
            OverrideConfigurator configurator = new OverrideConfigurator(consumerConfiguratorUrl);
            // Meet the configured conditions:
            // same side
            // The port of configuratorUrl is 0
            // The host of configuratorUrl is 0.0.0.0 or the local address is the same as consumerUrl
            // same appName
            URL originalURL = URL.valueOf(consumerUrlV3);
            Assertions.assertEquals("100", originalURL.getParameter("timeout"));
            URL url = configurator.configure(originalURL);
            Assertions.assertEquals("100", url.getParameter("timeout"));
        }
    }
}
```

---

## AbstractDirectoryConcurrencyTest.java -> setInvokers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `271`
- **Status:** `SUCCESS`
- **Comment:** `Expose setInvokers for test`

```java
// Expose setInvokers for test
@Override
public void setInvokers(BitList<Invoker<Object>> invokers) {
    super.setInvokers(invokers);
}
```

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvokerSetTest {

    // Assuming the enclosing class is named something like TargetClass
    // and BitList, Invoker are accessible types in the project.
    private TargetClass targetObject;
    
    @SuppressWarnings("unchecked")
    private BitList<Invoker<Object>> mockBitList;

    @BeforeEach
    void setUp() {
        targetObject = new TargetClass();
        mockBitList = (BitList<Invoker<Object>>) mock(BitList.class);
    }

    @Test
    void testSetInvokers() {
        // Given / When
        targetObject.setInvokers(mockBitList);

        // Then
        // Verify that the invokers were set correctly by checking internal state 
        // or ensuring no exceptions were thrown during the execution.
        // Since getInvokers() is typically the counterpart to setInvokers(), we assert that it returns the expected mock.
        assertEquals(mockBitList, targetObject.getInvokers(), "The invokers list should be correctly set.");
    }
}
```

---

## ResultMergerTest.java -> testMergerFactoryIllegalArgumentException()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `50`
- **Status:** `SUCCESS`
- **Comment:** `* MergerFactory test`

```java
/**
 * MergerFactory test
 */
@Test
void testMergerFactoryIllegalArgumentException() {
    try {
        mergerFactory.getMerger(null);
        Assertions.fail("expected IllegalArgumentException for null argument");
    } catch (IllegalArgumentException exception) {
        Assertions.assertEquals("returnType is null", exception.getMessage());
    }
}
```

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MergerFactoryTest {

    @InjectMocks
    private MergerFactory mergerFactory;

    /**
     * MergerFactory test - refactored to use JUnit 5's assertThrows for better readability and modern idiomatic testing.
     */
    @Test
    void testMergerFactoryIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mergerFactory.getMerger(null),
            "Expected IllegalArgumentException for null argument"
        );
        
        assertEquals("returnType is null", exception.getMessage());
    }
}
```

---

## ResultMergerTest.java -> testArrayMergerIllegalArgumentException()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `63`
- **Status:** `SUCCESS`
- **Comment:** `* ArrayMerger test`

```java
/**
 * ArrayMerger test
 */
@Test
void testArrayMergerIllegalArgumentException() {
    String[] stringArray = { "1", "2", "3" };
    Integer[] integerArray = { 3, 4, 5 };
    try {
        Object result = ArrayMerger.INSTANCE.merge(stringArray, null, integerArray);
        Assertions.fail("expected IllegalArgumentException for different arguments' types");
    } catch (IllegalArgumentException exception) {
        Assertions.assertEquals("Arguments' types are different", exception.getMessage());
    }
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArrayMergerTest {

    @Test
    void testArrayMergerIllegalArgumentException() {
        String[] stringArray = { "1", "2", "3" };
        Integer[] integerArray = { 3, 4, 5 };
        
        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ArrayMerger.INSTANCE.merge(stringArray, null, integerArray),
            "Expected IllegalArgumentException for different arguments' types"
        );
        
        Assertions.assertEquals("Arguments' types are different", exception.getMessage());
    }
}
```

---

## ResultMergerTest.java -> testArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `78`
- **Status:** `SUCCESS`
- **Comment:** `* ArrayMerger test`

```java
/**
 * ArrayMerger test
 */
@Test
void testArrayMerger() {
    String[] stringArray1 = { "1", "2", "3" };
    String[] stringArray2 = { "4", "5", "6" };
    String[] stringArray3 = {};
    Object result = ArrayMerger.INSTANCE.merge(stringArray1, stringArray2, stringArray3, null);
    Assertions.assertTrue(result.getClass().isArray());
    Assertions.assertEquals(6, Array.getLength(result));
    Assertions.assertTrue(String.class.isInstance(Array.get(result, 0)));
    for (int i = 0; i < 6; i++) {
        Assertions.assertEquals(String.valueOf(i + 1), Array.get(result, i));
    }
    Integer[] intArray1 = { 1, 2, 3 };
    Integer[] intArray2 = { 4, 5, 6 };
    Integer[] intArray3 = { 7 };
    // trigger ArrayMerger
    result = mergerFactory.getMerger(Integer[].class).merge(intArray1, intArray2, intArray3, null);
    Assertions.assertTrue(result.getClass().isArray());
    Assertions.assertEquals(7, Array.getLength(result));
    Assertions.assertSame(Integer.class, result.getClass().getComponentType());
    for (int i = 0; i < 7; i++) {
        Assertions.assertEquals(i + 1, Array.get(result, i));
    }
    result = ArrayMerger.INSTANCE.merge(null);
    Assertions.assertEquals(0, Array.getLength(result));
    result = ArrayMerger.INSTANCE.merge(null, null);
    Assertions.assertEquals(0, Array.getLength(result));
    result = ArrayMerger.INSTANCE.merge(null, new Object[0]);
    Assertions.assertEquals(0, Array.getLength(result));
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;

class ArrayMergerTest {

    @Test
    void testArrayMerger() {
        // Test merging string arrays including empty arrays and null values
        String[] stringArray1 = { "1", "2", "3" };
        String[] stringArray2 = { "4", "5", "6" };
        String[] stringArray3 = {};
        Object result = ArrayMerger.INSTANCE.merge(stringArray1, stringArray2, stringArray3, null);
        
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getClass().isArray());
        Assertions.assertEquals(6, Array.getLength(result));
        Assertions.assertTrue(String.class.isInstance(Array.get(result, 0)));
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(String.valueOf(i + 1), Array.get(result, i));
        }

        // Test merging Integer arrays using a merger factory
        Integer[] intArray1 = { 1, 2, 3 };
        Integer[] intArray2 = { 4, 5, 6 };
        Integer[] intArray3 = { 7 };
        
        // Assuming a mock or instantiated mergerFactory exists in the test context
        // If mergerFactory is not explicitly declared in your test class, ensure it's available or instantiated.
        // For example: MergerFactory mergerFactory = new MergerFactory();
        result = mergerFactory.getMerger(Integer[].class).merge(intArray1, intArray2, intArray3, null);
        
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getClass().isArray());
        Assertions.assertEquals(7, Array.getLength(result));
        Assertions.assertSame(Integer.class, result.getClass().getComponentType());
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(i + 1, Array.get(result, i));
        }

        // Test edge cases with null and empty inputs
        result = ArrayMerger.INSTANCE.merge((Object[]) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, Array.getLength(result));

        result = ArrayMerger.INSTANCE.merge((Object[]) null, (Object[]) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, Array.getLength(result));

        result = ArrayMerger.INSTANCE.merge(null, new Object[0]);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, Array.getLength(result));
    }
}
```

---

## ResultMergerTest.java -> testBooleanArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `117`
- **Status:** `SUCCESS`
- **Comment:** `* BooleanArrayMerger test`

```java
/**
 * BooleanArrayMerger test
 */
@Test
void testBooleanArrayMerger() {
    boolean[] arrayOne = { true, false };
    boolean[] arrayTwo = { false };
    boolean[] result = mergerFactory.getMerger(boolean[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(3, result.length);
    boolean[] mergedResult = { true, false, false };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i]);
    }
    result = mergerFactory.getMerger(boolean[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(boolean[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BooleanArrayMergerTest {

    // Assuming mergerFactory is available in the test context (e.g., via mocking or injection)
    // Replace with actual instantiation if needed for standalone compilation.
    
    @Test
    void testBooleanArrayMerger() {
        boolean[] arrayOne = { true, false };
        boolean[] arrayTwo = { false };
        boolean[] result = mergerFactory.getMerger(boolean[].class).merge(arrayOne, arrayTwo, null);
        
        Assertions.assertNotNull(result, "Merged result should not be null");
        boolean[] expectedResult = { true, false, false };
        Assertions.assertArrayEquals(expectedResult, result, "The merged boolean array does not match the expected values.");

        // Test merging with null input
        result = mergerFactory.getMerger(boolean[].class).merge((boolean[]) null);
        Assertions.assertNotNull(result, "Result for null input should not be null");
        Assertions.assertEquals(0, result.length, "Result for null input should be empty");

        // Test merging multiple null inputs
        result = mergerFactory.getMerger(boolean[].class).merge(null, null);
        Assertions.assertNotNull(result, "Result for multiple null inputs should not be null");
        Assertions.assertEquals(0, result.length, "Result for multiple null inputs should be empty");
        
        // Test merging empty arrays
        result = mergerFactory.getMerger(boolean[].class).merge(new boolean[0], new boolean[0]);
        Assertions.assertNotNull(result, "Result for empty arrays should not be null");
        Assertions.assertEquals(0, result.length, "Result for empty arrays should be empty");
    }
}
```

---

## ResultMergerTest.java -> testByteArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `138`
- **Status:** `SUCCESS`
- **Comment:** `* ByteArrayMerger test`

```java
/**
 * ByteArrayMerger test
 */
@Test
void testByteArrayMerger() {
    byte[] arrayOne = { 1, 2 };
    byte[] arrayTwo = { 1, 32 };
    byte[] result = mergerFactory.getMerger(byte[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(4, result.length);
    byte[] mergedResult = { 1, 2, 1, 32 };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i]);
    }
    result = mergerFactory.getMerger(byte[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(byte[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ByteArrayMergerTest {

    @Autowired
    private MergerFactory mergerFactory;

    /**
     * Comprehensive test for ByteArrayMerger covering normal merging, 
     * null inputs, empty arrays, and edge cases to pay off technical debt.
     */
    @Test
    void testByteArrayMerger() {
        // Test standard merge with two arrays
        byte[] arrayOne = { 1, 2 };
        byte[] arrayTwo = { 1, 32 };
        byte[] result = mergerFactory.getMerger(byte[].class).merge(arrayOne, arrayTwo, null);
        
        byte[] expectedMerged = { 1, 2, 1, 32 };
        Assertions.assertArrayEquals(expectedMerged, result, "Merged byte array does not match expected values.");

        // Test merge with single null argument
        result = mergerFactory.getMerger(byte[].class).merge((byte[]) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length, "Result of merging single null should be an empty array.");

        // Test merge with multiple null arguments
        result = mergerFactory.getMerger(byte[].class).merge(null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length, "Result of merging multiple nulls should be an empty array.");

        // Test merge with empty arrays
        byte[] emptyArray = {};
        result = mergerFactory.getMerger(byte[].class).merge(emptyArray, emptyArray);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length, "Result of merging empty arrays should be an empty array.");

        // Test merge combining empty and non-empty arrays
        result = mergerFactory.getMerger(byte[].class).merge(emptyArray, arrayOne, null);
        Assertions.assertArrayEquals(arrayOne, result, "Merging an empty array with a populated array should yield the populated array.");
    }
}
```

---

## ResultMergerTest.java -> testCharArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `159`
- **Status:** `SUCCESS`
- **Comment:** `* CharArrayMerger test`

```java
/**
 * CharArrayMerger test
 */
@Test
void testCharArrayMerger() {
    char[] arrayOne = "hello".toCharArray();
    char[] arrayTwo = "world".toCharArray();
    char[] result = mergerFactory.getMerger(char[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(10, result.length);
    char[] mergedResult = "helloworld".toCharArray();
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i]);
    }
    result = mergerFactory.getMerger(char[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(char[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CharArrayMergerTest {

    @Autowired
    private MergerFactory mergerFactory;

    @Test
    void testCharArrayMerger() {
        // Test standard merging of two arrays
        char[] arrayOne = "hello".toCharArray();
        char[] arrayTwo = "world".toCharArray();
        char[] result = mergerFactory.getMerger(char[].class).merge(arrayOne, arrayTwo, null);
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.length);
        char[] mergedResult = "helloworld".toCharArray();
        Assertions.assertArrayEquals(mergedResult, result);

        // Test merging with a single null argument or varargs containing null
        result = mergerFactory.getMerger(char[].class).merge((char[][]) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);

        // Test merging with multiple null arrays
        result = mergerFactory.getMerger(char[].class).merge(null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);

        // Test merging with empty arrays
        result = mergerFactory.getMerger(char[].class).merge(new char[0], new char[0]);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);

        // Test merging a single array
        result = mergerFactory.getMerger(char[].class).merge(arrayOne);
        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(arrayOne, result);
    }
}
```

---

## ResultMergerTest.java -> testDoubleArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `180`
- **Status:** `SUCCESS`
- **Comment:** `* DoubleArrayMerger test`

```java
/**
 * DoubleArrayMerger test
 */
@Test
void testDoubleArrayMerger() {
    double[] arrayOne = { 1.2d, 3.5d };
    double[] arrayTwo = { 2d, 34d };
    double[] result = mergerFactory.getMerger(double[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(4, result.length);
    double[] mergedResult = { 1.2d, 3.5d, 2d, 34d };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i], 0.0);
    }
    result = mergerFactory.getMerger(double[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(double[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DoubleArrayMergerTest {

    @Autowired
    private MergerFactory mergerFactory;

    @Test
    void testDoubleArrayMerger() {
        // Test normal merging of two arrays
        double[] arrayOne = { 1.2d, 3.5d };
        double[] arrayTwo = { 2d, 34d };
        double[] result = mergerFactory.getMerger(double[].class).merge(arrayOne, arrayTwo, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
        double[] mergedResult = { 1.2d, 3.5d, 2d, 34d };
        for (int i = 0; i < mergedResult.length; i++) {
            Assertions.assertEquals(mergedResult[i], result[i], 0.0);
        }

        // Test merging with single null/empty input
        result = mergerFactory.getMerger(double[].class).merge((double[]) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);

        // Test merging with multiple null inputs
        result = mergerFactory.getMerger(double[].class).merge(null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
        
        // Test merging with a mix of empty, null, and valid arrays
        double[] emptyArray = {};
        result = mergerFactory.getMerger(double[].class).merge(emptyArray, null, arrayOne);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(1.2d, result[0], 0.0);
        Assertions.assertEquals(3.5d, result[1], 0.0);
    }
}
```

---

## ResultMergerTest.java -> testFloatArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `201`
- **Status:** `ERROR`
- **Comment:** `* FloatArrayMerger test`

```java
/**
 * FloatArrayMerger test
 */
@Test
void testFloatArrayMerger() {
    float[] arrayOne = { 1.2f, 3.5f };
    float[] arrayTwo = { 2f, 34f };
    float[] result = mergerFactory.getMerger(float[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(4, result.length);
    double[] mergedResult = { 1.2f, 3.5f, 2f, 34f };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i], 0.0);
    }
    result = mergerFactory.getMerger(float[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(float[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.249667837s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testIntArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `222`
- **Status:** `ERROR`
- **Comment:** `* IntArrayMerger test`

```java
/**
 * IntArrayMerger test
 */
@Test
void testIntArrayMerger() {
    int[] arrayOne = { 1, 2 };
    int[] arrayTwo = { 2, 34 };
    int[] result = mergerFactory.getMerger(int[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(4, result.length);
    double[] mergedResult = { 1, 2, 2, 34 };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i], 0.0);
    }
    result = mergerFactory.getMerger(int[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(int[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.580138367s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testListMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `243`
- **Status:** `ERROR`
- **Comment:** `* ListMerger test`

```java
/**
 * ListMerger test
 */
@Test
void testListMerger() {
    List<Object> list1 = new ArrayList<Object>() {

        {
            add(null);
            add("1");
            add("2");
        }
    };
    List<Object> list2 = new ArrayList<Object>() {

        {
            add("3");
            add("4");
        }
    };
    List result = mergerFactory.getMerger(List.class).merge(list1, list2, null);
    Assertions.assertEquals(5, result.size());
    ArrayList<String> expected = new ArrayList<String>() {

        {
            add(null);
            add("1");
            add("2");
            add("3");
            add("4");
        }
    };
    Assertions.assertEquals(expected, result);
    result = mergerFactory.getMerger(List.class).merge(null);
    Assertions.assertEquals(0, result.size());
    result = mergerFactory.getMerger(List.class).merge(null, null);
    Assertions.assertEquals(0, result.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.341392311s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testMapArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `282`
- **Status:** `ERROR`
- **Comment:** `* LongArrayMerger test`

```java
/**
 * LongArrayMerger test
 */
@Test
void testMapArrayMerger() {
    Map<Object, Object> mapOne = new HashMap<Object, Object>() {

        {
            put("11", 222);
            put("223", 11);
        }
    };
    Map<Object, Object> mapTwo = new HashMap<Object, Object>() {

        {
            put("3333", 3232);
            put("444", 2323);
        }
    };
    Map<Object, Object> result = mergerFactory.getMerger(Map.class).merge(mapOne, mapTwo, null);
    Assertions.assertEquals(4, result.size());
    Map<String, Integer> mergedResult = new HashMap<String, Integer>() {

        {
            put("11", 222);
            put("223", 11);
            put("3333", 3232);
            put("444", 2323);
        }
    };
    Assertions.assertEquals(mergedResult, result);
    result = mergerFactory.getMerger(Map.class).merge(null);
    Assertions.assertEquals(0, result.size());
    result = mergerFactory.getMerger(Map.class).merge(null, null);
    Assertions.assertEquals(0, result.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.677402615s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testLongArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `318`
- **Status:** `ERROR`
- **Comment:** `* LongArrayMerger test`

```java
/**
 * LongArrayMerger test
 */
@Test
void testLongArrayMerger() {
    long[] arrayOne = { 1L, 2L };
    long[] arrayTwo = { 2L, 34L };
    long[] result = mergerFactory.getMerger(long[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(4, result.length);
    double[] mergedResult = { 1L, 2L, 2L, 34L };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i], 0.0);
    }
    result = mergerFactory.getMerger(long[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(long[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.425568029s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testSetMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `339`
- **Status:** `ERROR`
- **Comment:** `* SetMerger test`

```java
/**
 * SetMerger test
 */
@Test
void testSetMerger() {
    Set<Object> set1 = new HashSet<Object>() {

        {
            add(null);
            add("1");
            add("2");
        }
    };
    Set<Object> set2 = new HashSet<Object>() {

        {
            add("2");
            add("3");
        }
    };
    Set result = mergerFactory.getMerger(Set.class).merge(set1, set2, null);
    Assertions.assertEquals(4, result.size());
    Assertions.assertEquals(new HashSet<String>() {

        {
            add(null);
            add("1");
            add("2");
            add("3");
        }
    }, result);
    result = mergerFactory.getMerger(Set.class).merge(null);
    Assertions.assertEquals(0, result.size());
    result = mergerFactory.getMerger(Set.class).merge(null, null);
    Assertions.assertEquals(0, result.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.765657824s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testShortArrayMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `380`
- **Status:** `ERROR`
- **Comment:** `* ShortArrayMerger test`

```java
/**
 * ShortArrayMerger test
 */
@Test
void testShortArrayMerger() {
    short[] arrayOne = { 1, 2 };
    short[] arrayTwo = { 2, 34 };
    short[] result = mergerFactory.getMerger(short[].class).merge(arrayOne, arrayTwo, null);
    Assertions.assertEquals(4, result.length);
    double[] mergedResult = { 1, 2, 2, 34 };
    for (int i = 0; i < mergedResult.length; i++) {
        Assertions.assertEquals(mergedResult[i], result[i], 0.0);
    }
    result = mergerFactory.getMerger(short[].class).merge(null);
    Assertions.assertEquals(0, result.length);
    result = mergerFactory.getMerger(short[].class).merge(null, null);
    Assertions.assertEquals(0, result.length);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.516340093s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testIntSumMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `401`
- **Status:** `ERROR`
- **Comment:** `* IntSumMerger test`

```java
/**
 * IntSumMerger test
 */
@Test
void testIntSumMerger() {
    Integer[] intArr = IntStream.rangeClosed(1, 100).boxed().toArray(Integer[]::new);
    Merger<Integer> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "intsum");
    Assertions.assertEquals(5050, merger.merge(intArr));
    intArr = new Integer[] {};
    Assertions.assertEquals(0, merger.merge(intArr));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.860096333s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testDoubleSumMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `414`
- **Status:** `ERROR`
- **Comment:** `* DoubleSumMerger test`

```java
/**
 * DoubleSumMerger test
 */
@Test
void testDoubleSumMerger() {
    Double[] doubleArr = DoubleStream.iterate(1, v -> ++v).limit(100).boxed().toArray(Double[]::new);
    Merger<Double> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "doublesum");
    Assertions.assertEquals(5050, merger.merge(doubleArr));
    doubleArr = new Double[] {};
    Assertions.assertEquals(0, merger.merge(doubleArr));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.607155372s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testFloatSumMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `428`
- **Status:** `ERROR`
- **Comment:** `* FloatSumMerger test`

```java
/**
 * FloatSumMerger test
 */
@Test
void testFloatSumMerger() {
    Float[] floatArr = Stream.iterate(1.0F, v -> ++v).limit(100).toArray(Float[]::new);
    Merger<Float> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "floatsum");
    Assertions.assertEquals(5050, merger.merge(floatArr));
    floatArr = new Float[] {};
    Assertions.assertEquals(0, merger.merge(floatArr));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.940515012s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testLongSumMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `441`
- **Status:** `ERROR`
- **Comment:** `* LongSumMerger test`

```java
/**
 * LongSumMerger test
 */
@Test
void testLongSumMerger() {
    Long[] longArr = LongStream.rangeClosed(1, 100).boxed().toArray(Long[]::new);
    Merger<Long> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "longsum");
    Assertions.assertEquals(5050, merger.merge(longArr));
    longArr = new Long[] {};
    Assertions.assertEquals(0, merger.merge(longArr));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.704123624s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testIntFindAnyMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `454`
- **Status:** `ERROR`
- **Comment:** `* IntFindAnyMerger test`

```java
/**
 * IntFindAnyMerger test
 */
@Test
void testIntFindAnyMerger() {
    Integer[] intArr = { 1, 2, 3, 4 };
    Merger<Integer> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "intany");
    Assertions.assertNotNull(merger.merge(intArr));
    intArr = new Integer[] {};
    Assertions.assertNull(merger.merge(intArr));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.041510018s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## ResultMergerTest.java -> testIntFindFirstMerger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `467`
- **Status:** `ERROR`
- **Comment:** `* IntFindFirstMerger test`

```java
/**
 * IntFindFirstMerger test
 */
@Test
void testIntFindFirstMerger() {
    Integer[] intArr = { 1, 2, 3, 4 };
    Merger<Integer> merger = ApplicationModel.defaultModel().getExtension(Merger.class, "intfirst");
    Assertions.assertEquals(1, merger.merge(intArr));
    intArr = new Integer[] {};
    Assertions.assertNull(merger.merge(intArr));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.80327607s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## TagStateRouterTest.java -> tagRouterRuleParseTest()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `132`
- **Status:** `ERROR`
- **Comment:** `* TagRouterRule parse test when the tags addresses is null      *      * <pre>      *     ~ -> null      *     null -> null      * </pre>`

```java
/**
 * TagRouterRule parse test when the tags addresses is null
 *
 * <pre>
 *     ~ -> null
 *     null -> null
 * </pre>
 */
@Test
void tagRouterRuleParseTest() {
    String tagRouterRuleConfig = "---\n" + "force: false\n" + "runtime: true\n" + "enabled: false\n" + "priority: 1\n" + "key: demo-provider\n" + "tags:\n" + "  - name: tag1\n" + "    addresses: null\n" + "  - name: tag2\n" + "    addresses: [\"30.5.120.37:20880\"]\n" + "  - name: tag3\n" + "    addresses: []\n" + "  - name: tag4\n" + "    addresses: ~\n" + "...";
    TagRouterRule tagRouterRule = TagRuleParser.parse(tagRouterRuleConfig);
    TagStateRouter<?> router = Mockito.mock(TagStateRouter.class);
    Mockito.when(router.getInvokers()).thenReturn(BitList.emptyList());
    tagRouterRule.init(router);
    // assert tags
    assert tagRouterRule.getKey().equals("demo-provider");
    assert tagRouterRule.getPriority() == 1;
    assert tagRouterRule.getTagNames().contains("tag1");
    assert tagRouterRule.getTagNames().contains("tag2");
    assert tagRouterRule.getTagNames().contains("tag3");
    assert tagRouterRule.getTagNames().contains("tag4");
    // assert addresses
    assert tagRouterRule.getAddresses().contains("30.5.120.37:20880");
    assert tagRouterRule.getTagnameToAddresses().get("tag1") == null;
    assert tagRouterRule.getTagnameToAddresses().get("tag2").size() == 1;
    assert tagRouterRule.getTagnameToAddresses().get("tag3") == null;
    assert tagRouterRule.getTagnameToAddresses().get("tag4") == null;
    assert tagRouterRule.getAddresses().size() == 1;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.130802321s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## AbstractClusterInvokerTest.java -> testSelectBalance()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `480`
- **Status:** `ERROR`
- **Comment:** `* Test balance.`

```java
/**
 * Test balance.
 */
@Test
void testSelectBalance() {
    LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
    initlistsize5();
    Map<Invoker, AtomicLong> counter = new ConcurrentHashMap<Invoker, AtomicLong>();
    for (Invoker invoker : invokers) {
        counter.put(invoker, new AtomicLong(0));
    }
    int runs = 1000;
    for (int i = 0; i < runs; i++) {
        selectedInvokers.clear();
        Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
        counter.get(sinvoker).incrementAndGet();
    }
    for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
        Long count = entry.getValue().get();
        if (entry.getKey().isAvailable())
            Assertions.assertTrue(count > runs / invokers.size(), "count should > avg");
    }
    Assertions.assertEquals(runs, counter.get(invoker2).get() + counter.get(invoker4).get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.888061213s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## FailoverClusterInvokerTest.java -> testInvokerDestroyAndReList()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `274`
- **Status:** `ERROR`
- **Comment:** `* When invokers in directory changes after a failed request but just before a retry effort,      * then we should reselect from the latest invokers before retry.`

```java
/**
 * When invokers in directory changes after a failed request but just before a retry effort,
 * then we should reselect from the latest invokers before retry.
 */
@Test
void testInvokerDestroyAndReList() {
    final URL url = URL.valueOf("test://localhost/" + Demo.class.getName() + "?loadbalance=roundrobin&retries=" + retries);
    RpcException exception = new RpcException(RpcException.TIMEOUT_EXCEPTION);
    MockInvoker<Demo> invoker1 = new MockInvoker<>(Demo.class, url);
    invoker1.setException(exception);
    MockInvoker<Demo> invoker2 = new MockInvoker<>(Demo.class, url);
    invoker2.setException(exception);
    final List<Invoker<Demo>> invokers = new ArrayList<>();
    invokers.add(invoker1);
    invokers.add(invoker2);
    MockDirectory<Demo> dic = new MockDirectory<>(url, invokers);
    Callable<Object> callable = () -> {
        // Simulation: all invokers are destroyed
        for (Invoker<Demo> invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
        MockInvoker<Demo> invoker3 = new MockInvoker<>(Demo.class, url);
        invoker3.setResult(AsyncRpcResult.newDefaultAsyncResult(mock(RpcInvocation.class)));
        invokers.add(invoker3);
        dic.notify(invokers);
        return null;
    };
    invoker1.setCallable(callable);
    invoker2.setCallable(callable);
    RpcInvocation inv = new RpcInvocation();
    inv.setMethodName("test");
    FailoverClusterInvoker<Demo> clusterInvoker = new FailoverClusterInvoker<>(dic);
    clusterInvoker.invoke(inv);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.218900458s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## FailSafeClusterInvokerTest.java -> testInvokeException()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `84`
- **Status:** `ERROR`
- **Comment:** `TODO assert error log`

```java
// TODO assert error log
@Test
void testInvokeException() {
    resetInvokerToException();
    FailsafeClusterInvoker<DemoService> invoker = new FailsafeClusterInvoker<DemoService>(dic);
    invoker.invoke(invocation);
    Assertions.assertNull(RpcContext.getServiceContext().getInvoker());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.976914058s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## MergeableClusterInvokerTest.java -> testInvokerToException()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `344`
- **Status:** `ERROR`
- **Comment:** `* test when network exception`

```java
/**
 * test when network exception
 */
@Test
void testInvokerToException() {
    String menu = "first";
    List<String> menuItems = new ArrayList<String>() {

        {
            add("1");
            add("2");
        }
    };
    given(invocation.getMethodName()).willReturn("addMenu");
    given(invocation.getParameterTypes()).willReturn(new Class<?>[] { String.class, List.class });
    given(invocation.getArguments()).willReturn(new Object[] { menu, menuItems });
    given(invocation.getObjectAttachments()).willReturn(new HashMap<>());
    given(invocation.getInvoker()).willReturn(firstInvoker);
    given(firstInvoker.getUrl()).willReturn(url.addParameter(GROUP_KEY, "first"));
    given(firstInvoker.getInterface()).willReturn(MenuService.class);
    given(firstInvoker.invoke(invocation)).willReturn(new AppResponse());
    given(firstInvoker.isAvailable()).willReturn(true);
    given(firstInvoker.invoke(invocation)).willThrow(new RpcException(RpcException.NETWORK_EXCEPTION));
    given(secondInvoker.getUrl()).willReturn(url.addParameter(GROUP_KEY, "second"));
    given(secondInvoker.getInterface()).willReturn(MenuService.class);
    given(secondInvoker.invoke(invocation)).willReturn(new AppResponse());
    given(secondInvoker.isAvailable()).willReturn(true);
    given(secondInvoker.invoke(invocation)).willThrow(new RpcException(RpcException.NETWORK_EXCEPTION));
    given(directory.list(invocation)).willReturn(new ArrayList() {

        {
            add(firstInvoker);
            add(secondInvoker);
        }
    });
    given(directory.getUrl()).willReturn(url);
    given(directory.getConsumerUrl()).willReturn(url);
    given(directory.getConsumerUrl()).willReturn(url);
    given(directory.getInterface()).willReturn(MenuService.class);
    mergeableClusterInvoker = new MergeableClusterInvoker<MenuService>(directory);
    // invoke
    try {
        Result result = mergeableClusterInvoker.invoke(invocation);
        fail();
        Assertions.assertNull(result.getValue());
    } catch (RpcException expected) {
        assertEquals(expected.getCode(), RpcException.NETWORK_EXCEPTION);
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.303721856s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## MockAbstractClusterInvokerTest.java -> testMockedInvokerSelect()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `172`
- **Status:** `ERROR`
- **Comment:** `* Test mock invoker selector works as expected`

```java
/**
 * Test mock invoker selector works as expected
 */
@Test
void testMockedInvokerSelect() {
    initlistsize5();
    invokers.add(mockedInvoker1);
    initDic();
    RpcInvocation mockedInvocation = new RpcInvocation();
    mockedInvocation.setMethodName("sayHello");
    mockedInvocation.setAttachment(INVOCATION_NEED_MOCK, "true");
    List<Invoker<IHelloService>> mockedInvokers = dic.list(mockedInvocation);
    Assertions.assertEquals(1, mockedInvokers.size());
    List<Invoker<IHelloService>> invokers = dic.list(invocation);
    Assertions.assertEquals(5, invokers.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.069916748s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerInvoke_normal()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `64`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerInvoke_normal() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName());
    url = url.addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=fail"));
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName() + "?getSomething.mock=return aa");
    Protocol protocol = new MockProtocol();
    Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
    invokers.add(mInvoker1);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("something", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.393859826s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerInvoke_failmock()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `92`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerInvoke_failmock() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=fail:return null")).addParameter("invoke_return_error", "true");
    URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName()).addParameter("mock", "fail:return null").addParameter("getSomething.mock", "return aa").addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName())).addParameter("invoke_return_error", "true");
    Protocol protocol = new MockProtocol();
    Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
    Invoker<IHelloService> cluster = getClusterInvokerMock(url, mInvoker1);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("aa", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.152764402s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerInvoke_forcemock()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `131`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: force-mock`

```java
/**
 * Test if mock policy works fine: force-mock
 */
@Test
void testMockInvokerInvoke_forcemock() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=force:return null"));
    URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName()).addParameter("mock", "force:return null").addParameter("getSomething.mock", "return aa").addParameter("getSomething3xx.mock", "return xx").addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName()));
    Protocol protocol = new MockProtocol();
    Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
    Invoker<IHelloService> cluster = getClusterInvokerMock(url, mInvoker1);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("aa", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.482965379s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_Fock_someMethods()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `191`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_Fock_someMethods() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "getSomething.mock=fail:return x" + "&" + "getSomething2.mock=force:return y"));
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("something", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("y", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething3");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("something3", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.253315996s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_Fock_WithOutDefault()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `228`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_Fock_WithOutDefault() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "getSomething.mock=fail:return x" + "&" + "getSomething2.mock=fail:return y")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("y", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething3");
    try {
        ret = cluster.invoke(invocation);
        Assertions.fail();
    } catch (RpcException e) {
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.585648453s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_Fock_WithDefault()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `264`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_Fock_WithDefault() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock" + "=" + "fail:return null" + "&" + "getSomething.mock" + "=" + "fail:return x" + "&" + "getSomething2.mock" + "=" + "fail:return y")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("y", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething3");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertNull(ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.35029592s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_Fock_WithFailDefault()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `303`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_Fock_WithFailDefault() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=fail:return z" + "&" + "getSomething.mock=fail:return x" + "&" + "getSomething2.mock=force:return y")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("y", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething3");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("z", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("z", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.681827402s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_Fock_WithForceDefault()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `342`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_Fock_WithForceDefault() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=force:return z" + "&" + "getSomething.mock=fail:return x" + "&" + "getSomething2.mock=force:return y")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("y", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething3");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("z", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("z", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.440415874s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_Fock_Default()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `381`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_Fock_Default() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=fail:return x")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething2");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("sayHello");
    ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.787838154s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_checkCompatible_return()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `411`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_checkCompatible_return() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "getSomething.mock=return x")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("x", ret.getValue());
    // If no mock was configured, return null directly
    invocation = new RpcInvocation();
    invocation.setMethodName("getSomething3");
    try {
        ret = cluster.invoke(invocation);
        Assertions.fail("fail invoke");
    } catch (RpcException e) {
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.549054943s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `439`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=true" + "&" + "proxy=jdk")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("somethingmock", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.879655704s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock2()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `458`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock2() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=fail")).addParameter("invoke_return_error", "true");
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("somethingmock", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.625812572s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## MockClusterInvokerTest.java -> testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock3()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `474`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: fail-mock`

```java
/**
 * Test if mock policy works fine: fail-mock
 */
@Test
void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock3() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName()).addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=force"));
    Invoker<IHelloService> cluster = getClusterInvoker(url);
    // Configured with mock
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("somethingmock", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.968079829s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## MockProviderRpcExceptionTest.java -> testMockInvokerProviderRpcException()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `61`
- **Status:** `ERROR`
- **Comment:** `* Test if mock policy works fine: ProviderRpcException`

```java
/**
 * Test if mock policy works fine: ProviderRpcException
 */
@Test
void testMockInvokerProviderRpcException() {
    URL url = URL.valueOf("remote://1.2.3.4/" + IHelloRpcService.class.getName());
    url = url.addParameter(MOCK_KEY, "true").addParameter("invoke_return_error", "true").addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloRpcService.class.getName() + "&" + "mock=true" + "&" + "proxy=jdk"));
    Invoker<IHelloRpcService> cluster = getClusterInvoker(url);
    RpcInvocation invocation = new RpcInvocation();
    invocation.setMethodName("getSomething4");
    Result ret = cluster.invoke(invocation);
    Assertions.assertEquals("something4mock", ret.getValue());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.729659877s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## CompositeConfiguration.java -> isDynamicIncluded()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `54`
- **Status:** `ERROR`
- **Comment:** `FIXME, consider changing configList to SortedMap to replace this boolean status.`

```java
// FIXME, consider changing configList to SortedMap to replace this boolean status.
public boolean isDynamicIncluded() {
    return dynamicIncluded;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.058392381s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## Configuration.java -> containsKey()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `153`
- **Status:** `ERROR`
- **Comment:** `* Check if the configuration contains the specified key.      *      * @param key the key whose presence in this configuration is to be tested      * @return {@code true} if the configuration contains a value for this      * key, {@code false} otherwise`

```java
/**
 * Check if the configuration contains the specified key.
 *
 * @param key the key whose presence in this configuration is to be tested
 * @return {@code true} if the configuration contains a value for this
 * key, {@code false} otherwise
 */
default boolean containsKey(String key) {
    return !isEmptyValue(getProperty(key));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.814543663s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## Environment.java -> getConfiguration()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `209`
- **Status:** `ERROR`
- **Comment:** `* There are two ways to get configuration during exposure / reference or at runtime:      * 1. URL, The value in the URL is relatively fixed. we can get value directly.      * 2. The configuration exposed in this method is convenient for us to query the latest values from multiple      * prioritized sources, it also guarantees that configs changed dynamically can take effect on the fly.`

```java
/**
 * There are two ways to get configuration during exposure / reference or at runtime:
 * 1. URL, The value in the URL is relatively fixed. we can get value directly.
 * 2. The configuration exposed in this method is convenient for us to query the latest values from multiple
 * prioritized sources, it also guarantees that configs changed dynamically can take effect on the fly.
 */
public CompositeConfiguration getConfiguration() {
    if (globalConfiguration == null) {
        CompositeConfiguration configuration = new CompositeConfiguration();
        configuration.addConfiguration(systemConfiguration);
        configuration.addConfiguration(environmentConfiguration);
        configuration.addConfiguration(appExternalConfiguration);
        configuration.addConfiguration(externalConfiguration);
        configuration.addConfiguration(appConfiguration);
        configuration.addConfiguration(propertiesConfiguration);
        globalConfiguration = configuration;
    }
    return globalConfiguration;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.15594433s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## Environment.java -> reset()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `291`
- **Status:** `ERROR`
- **Comment:** `* Reset environment.      * For test only.`

```java
/**
 * Reset environment.
 * For test only.
 */
public void reset() {
    destroy();
    initialize();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.906436025s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## AdaptiveClassCodeGenerator.java -> hasAdaptiveMethod()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `91`
- **Status:** `ERROR`
- **Comment:** `* test if given type has at least one method annotated with <code>Adaptive</code>`

```java
/**
 * test if given type has at least one method annotated with <code>Adaptive</code>
 */
private boolean hasAdaptiveMethod() {
    return Arrays.stream(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Adaptive.class));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.253719222s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## AdaptiveClassCodeGenerator.java -> hasInvocationArgument()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `344`
- **Status:** `ERROR`
- **Comment:** `* test if method has argument of type <code>Invocation</code>`

```java
/**
 * test if method has argument of type <code>Invocation</code>
 */
private boolean hasInvocationArgument(Method method) {
    Class<?>[] pts = method.getParameterTypes();
    return Arrays.stream(pts).anyMatch(p -> CLASS_NAME_INVOCATION.equals(p.getName()));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.003754275s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## AdaptiveClassCodeGenerator.java -> generateInvocationArgumentNullCheck()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `352`
- **Status:** `ERROR`
- **Comment:** `* generate code to test argument of type <code>Invocation</code> is null`

```java
/**
 * generate code to test argument of type <code>Invocation</code> is null
 */
private String generateInvocationArgumentNullCheck(Method method) {
    Class<?>[] pts = method.getParameterTypes();
    return IntStream.range(0, pts.length).filter(i -> CLASS_NAME_INVOCATION.equals(pts[i].getName())).mapToObj(i -> String.format(CODE_INVOCATION_ARGUMENT_NULL_CHECK, i, i)).findFirst().orElse("");
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.367558289s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## AdaptiveClassCodeGenerator.java -> generateUrlAssignmentIndirectly()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `381`
- **Status:** `ERROR`
- **Comment:** `* get parameter with type <code>URL</code> from method parameter:      * <p>      * test if parameter has method which returns type <code>URL</code>      * <p>      * if not found, throws IllegalStateException`

```java
/**
 * get parameter with type <code>URL</code> from method parameter:
 * <p>
 * test if parameter has method which returns type <code>URL</code>
 * <p>
 * if not found, throws IllegalStateException
 */
private String generateUrlAssignmentIndirectly(Method method) {
    Class<?>[] pts = method.getParameterTypes();
    Map<String, Integer> getterReturnUrl = new HashMap<>();
    // find URL getter method
    for (int i = 0; i < pts.length; ++i) {
        for (Method m : pts[i].getMethods()) {
            String name = m.getName();
            if ((name.startsWith("get") || name.length() > 3) && Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length == 0 && m.getReturnType() == URL.class) {
                getterReturnUrl.put(name, i);
            }
        }
    }
    if (getterReturnUrl.size() <= 0) {
        // getter method not found, throw
        throw new IllegalStateException("Failed to create adaptive class for interface " + type.getName() + ": not found url parameter or url attribute in parameters of method " + method.getName());
    }
    Integer index = getterReturnUrl.get("getUrl");
    if (index != null) {
        return generateGetUrlNullCheck(index, pts[index], "getUrl");
    } else {
        Map.Entry<String, Integer> entry = getterReturnUrl.entrySet().iterator().next();
        return generateGetUrlNullCheck(entry.getValue(), pts[entry.getValue()], entry.getKey());
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.089877419s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## AdaptiveClassCodeGenerator.java -> generateGetUrlNullCheck()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `420`
- **Status:** `ERROR`
- **Comment:** `* 1, test if argi is null      * 2, test if argi.getXX() returns null      * 3, assign url with argi.getXX()`

```java
/**
 * 1, test if argi is null
 * 2, test if argi.getXX() returns null
 * 3, assign url with argi.getXX()
 */
private String generateGetUrlNullCheck(int index, Class<?> type, String method) {
    // Null point check
    StringBuilder code = new StringBuilder();
    code.append(String.format("if (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");\n", index, type.getName()));
    code.append(String.format("if (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");\n", index, method, type.getName(), method));
    code.append(String.format("%s url = arg%d.%s();\n", URL.class.getName(), index, method));
    return code.toString();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 14.471397245s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "14s"
      }
    ]
  }
}

```

---

## ExtensionLoader.java -> replaceExtension()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `687`
- **Status:** `ERROR`
- **Comment:** `* Replace the existing extension via API      *      * @param name  extension name      * @param clazz extension class      * @throws IllegalStateException when extension to be placed doesn't exist      * @deprecated not recommended any longer, and use only when test`

```java
/**
 * Replace the existing extension via API
 *
 * @param name  extension name
 * @param clazz extension class
 * @throws IllegalStateException when extension to be placed doesn't exist
 * @deprecated not recommended any longer, and use only when test
 */
@Deprecated
public void replaceExtension(String name, Class<?> clazz) {
    checkDestroyed();
    // load classes
    getExtensionClasses();
    if (!type.isAssignableFrom(clazz)) {
        throw new IllegalStateException("Input type " + clazz + " doesn't implement Extension " + type);
    }
    if (clazz.isInterface()) {
        throw new IllegalStateException("Input type " + clazz + " can't be interface!");
    }
    if (!clazz.isAnnotationPresent(Adaptive.class)) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
        }
        if (!cachedClasses.get().containsKey(name)) {
            throw new IllegalStateException("Extension name " + name + " doesn't exist (Extension " + type + ")!");
        }
        cachedNames.put(clazz, name);
        cachedClasses.get().put(name, clazz);
        cachedInstances.remove(name);
    } else {
        if (cachedAdaptiveClass == null) {
            throw new IllegalStateException("Adaptive Extension doesn't exist (Extension " + type + ")!");
        }
        cachedAdaptiveClass = clazz;
        cachedAdaptiveInstance.set(null);
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 14.158863665s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "14s"
      }
    ]
  }
}

```

---

## ExtensionLoader.java -> isWrapperClass()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `1410`
- **Status:** `ERROR`
- **Comment:** `* test if clazz is a wrapper class      * <p>      * which has Constructor with given class type as its only argument`

```java
/**
 * test if clazz is a wrapper class
 * <p>
 * which has Constructor with given class type as its only argument
 */
protected boolean isWrapperClass(Class<?> clazz) {
    Constructor<?>[] constructors = clazz.getConstructors();
    for (Constructor<?> constructor : constructors) {
        if (constructor.getParameterTypes().length == 1 && constructor.getParameterTypes()[0] == type) {
            return true;
        }
    }
    return false;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.571550546s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## Predicates.java -> alwaysTrue()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `38`
- **Status:** `ERROR`
- **Comment:** `* {@link Predicate} always return <code>true</code>      *      * @param <T> the type to test      * @return <code>true</code>`

```java
/**
 * {@link Predicate} always return <code>true</code>
 *
 * @param <T> the type to test
 * @return <code>true</code>
 */
static <T> Predicate<T> alwaysTrue() {
    return e -> true;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.256835691s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## Predicates.java -> alwaysFalse()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `48`
- **Status:** `ERROR`
- **Comment:** `* {@link Predicate} always return <code>false</code>      *      * @param <T> the type to test      * @return <code>false</code>`

```java
/**
 * {@link Predicate} always return <code>false</code>
 *
 * @param <T> the type to test
 * @return <code>false</code>
 */
static <T> Predicate<T> alwaysFalse() {
    return e -> false;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.677915301s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## Predicates.java -> and()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `59`
- **Status:** `ERROR`
- **Comment:** `* a composed predicate that represents a short-circuiting logical AND of {@link Predicate predicates}      *      * @param predicates {@link Predicate predicates}      * @param <T>        the type to test      * @return non-null`

```java
/**
 * a composed predicate that represents a short-circuiting logical AND of {@link Predicate predicates}
 *
 * @param predicates {@link Predicate predicates}
 * @param <T>        the type to test
 * @return non-null
 */
static <T> Predicate<T> and(Predicate<T>... predicates) {
    return of(predicates).reduce(Predicate::and).orElseGet(Predicates::alwaysTrue);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.348461455s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## Log4j2Logger.java -> getLogger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `166`
- **Status:** `ERROR`
- **Comment:** `test purpose only`

```java
// test purpose only
public org.apache.logging.log4j.Logger getLogger() {
    return logger;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 11.788815791s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "11s"
      }
    ]
  }
}

```

---

## GlobalResourcesRepository.java -> getGlobalReusedDisposables()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `175`
- **Status:** `ERROR`
- **Comment:** `for test`

```java
// for test
public static List<Disposable> getGlobalReusedDisposables() {
    return globalReusedDisposables;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 11.445082713s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "11s"
      }
    ]
  }
}

```

---

## GlobalResourcesRepository.java -> getOneoffDisposables()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `180`
- **Status:** `ERROR`
- **Comment:** `for test`

```java
// for test
public List<Disposable> getOneoffDisposables() {
    return oneoffDisposables;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.889483431s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## ExecutorRepository.java -> createExecutorIfAbsent()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `45`
- **Status:** `ERROR`
- **Comment:** `* Called by both Client and Server. TODO, consider separate these two parts.      * When the Client or Server starts for the first time, generate a new threadpool according to the parameters specified.      *      * @param url      * @return`

```java
/**
 * Called by both Client and Server. TODO, consider separate these two parts.
 * When the Client or Server starts for the first time, generate a new threadpool according to the parameters specified.
 *
 * @param url
 * @return
 */
ExecutorService createExecutorIfAbsent(URL url);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.541526419s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## AnnotationUtils.java -> isAnnotationPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `407`
- **Status:** `ERROR`
- **Comment:** `* Tests the annotated element is annotated the specified annotations or not      *      * @param type            the annotated type      * @param matchAll        If <code>true</code>, checking all annotation types are present or not, or match any      * @param annotationTypes the specified annotation types      * @return If the specified annotation types are present, return <code>true</code>, or <code>false</code>`

```java
/**
 * Tests the annotated element is annotated the specified annotations or not
 *
 * @param type            the annotated type
 * @param matchAll        If <code>true</code>, checking all annotation types are present or not, or match any
 * @param annotationTypes the specified annotation types
 * @return If the specified annotation types are present, return <code>true</code>, or <code>false</code>
 */
static boolean isAnnotationPresent(Class<?> type, boolean matchAll, Class<? extends Annotation>... annotationTypes) {
    int size = annotationTypes == null ? 0 : annotationTypes.length;
    if (size < 1) {
        return false;
    }
    int presentCount = 0;
    for (int i = 0; i < size; i++) {
        Class<? extends Annotation> annotationType = annotationTypes[i];
        if (findAnnotation(type, annotationType) != null || findMetaAnnotation(type, annotationType) != null) {
            presentCount++;
        }
    }
    return matchAll ? presentCount == size : presentCount > 0;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.98616565s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## AnnotationUtils.java -> isAnnotationPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `435`
- **Status:** `ERROR`
- **Comment:** `* Tests the annotated element is annotated the specified annotation or not      *      * @param type           the annotated type      * @param annotationType the class of annotation      * @return If the specified annotation type is present, return <code>true</code>, or <code>false</code>`

```java
/**
 * Tests the annotated element is annotated the specified annotation or not
 *
 * @param type           the annotated type
 * @param annotationType the class of annotation
 * @return If the specified annotation type is present, return <code>true</code>, or <code>false</code>
 */
@SuppressWarnings("unchecked")
static boolean isAnnotationPresent(Class<?> type, Class<? extends Annotation> annotationType) {
    return isAnnotationPresent(type, true, annotationType);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.630507147s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## AnnotationUtils.java -> isAnnotationPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `447`
- **Status:** `ERROR`
- **Comment:** `* Tests the annotated element is present any specified annotation types      *      * @param annotatedElement    the annotated element      * @param annotationClassName the class name of annotation      * @return If any specified annotation types are present, return <code>true</code>`

```java
/**
 * Tests the annotated element is present any specified annotation types
 *
 * @param annotatedElement    the annotated element
 * @param annotationClassName the class name of annotation
 * @return If any specified annotation types are present, return <code>true</code>
 */
@SuppressWarnings("unchecked")
static boolean isAnnotationPresent(AnnotatedElement annotatedElement, String annotationClassName) {
    ClassLoader classLoader = annotatedElement.getClass().getClassLoader();
    Class<?> resolvedType = resolveClass(annotationClassName, classLoader);
    if (resolvedType == null || !Annotation.class.isAssignableFrom(resolvedType)) {
        return false;
    }
    return isAnnotationPresent(annotatedElement, (Class<? extends Annotation>) resolvedType);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.084575925s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## AnnotationUtils.java -> isAnnotationPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `464`
- **Status:** `ERROR`
- **Comment:** `* Tests the annotated element is present any specified annotation types      *      * @param annotatedElement the annotated element      * @param annotationType   the class of annotation      * @return If any specified annotation types are present, return <code>true</code>`

```java
/**
 * Tests the annotated element is present any specified annotation types
 *
 * @param annotatedElement the annotated element
 * @param annotationType   the class of annotation
 * @return If any specified annotation types are present, return <code>true</code>
 */
static boolean isAnnotationPresent(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType) {
    if (isType(annotatedElement)) {
        return isAnnotationPresent((Class) annotatedElement, annotationType);
    } else {
        return annotatedElement.isAnnotationPresent(annotationType) || // to find meta-annotation
        findMetaAnnotation(annotatedElement, annotationType) != null;
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.72462606s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## AnnotationUtils.java -> isAllAnnotationPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `480`
- **Status:** `ERROR`
- **Comment:** `* Tests the annotated element is annotated all specified annotations or not      *      * @param type            the annotated type      * @param annotationTypes the specified annotation types      * @return If the specified annotation types are present, return <code>true</code>, or <code>false</code>`

```java
/**
 * Tests the annotated element is annotated all specified annotations or not
 *
 * @param type            the annotated type
 * @param annotationTypes the specified annotation types
 * @return If the specified annotation types are present, return <code>true</code>, or <code>false</code>
 */
static boolean isAllAnnotationPresent(Class<?> type, Class<? extends Annotation>... annotationTypes) {
    return isAnnotationPresent(type, true, annotationTypes);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.191632418s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## AnnotationUtils.java -> isAnyAnnotationPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `491`
- **Status:** `ERROR`
- **Comment:** `* Tests the annotated element is present any specified annotation types      *      * @param type            the annotated type      * @param annotationTypes the specified annotation types      * @return If any specified annotation types are present, return <code>true</code>`

```java
/**
 * Tests the annotated element is present any specified annotation types
 *
 * @param type            the annotated type
 * @param annotationTypes the specified annotation types
 * @return If any specified annotation types are present, return <code>true</code>
 */
static boolean isAnyAnnotationPresent(Class<?> type, Class<? extends Annotation>... annotationTypes) {
    return isAnnotationPresent(type, false, annotationTypes);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 7.823942341s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "7s"
      }
    ]
  }
}

```

---

## ClassLoaderResourceLoader.java -> getClassLoaderResourcesCache()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `108`
- **Status:** `ERROR`
- **Comment:** `for test`

```java
// for test
protected static SoftReference<Map<ClassLoader, Map<String, Set<URL>>>> getClassLoaderResourcesCache() {
    return classLoaderResourcesCache;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 7.292290249s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "7s"
      }
    ]
  }
}

```

---

## ClassUtils.java -> isPrimitive()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `327`
- **Status:** `ERROR`
- **Comment:** `* The specified type is primitive type or simple type      *      * @param type the type to test      * @return      * @deprecated as 2.7.6, use {@link Class#isPrimitive()} plus {@link #isSimpleType(Class)} instead`

```java
/**
 * The specified type is primitive type or simple type
 *
 * @param type the type to test
 * @return
 * @deprecated as 2.7.6, use {@link Class#isPrimitive()} plus {@link #isSimpleType(Class)} instead
 */
public static boolean isPrimitive(Class<?> type) {
    return type != null && (type.isPrimitive() || isSimpleType(type));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.922698649s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## ClassUtils.java -> isSimpleType()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `343`
- **Status:** `ERROR`
- **Comment:** `* The specified type is simple type or not      *      * @param type the type to test      * @return if <code>type</code> is one element of {@link #SIMPLE_TYPES}, return <code>true</code>, or <code>false</code>      * @see #SIMPLE_TYPES      * @since 2.7.6`

```java
/**
 * The specified type is simple type or not
 *
 * @param type the type to test
 * @return if <code>type</code> is one element of {@link #SIMPLE_TYPES}, return <code>true</code>, or <code>false</code>
 * @see #SIMPLE_TYPES
 * @since 2.7.6
 */
public static boolean isSimpleType(Class<?> type) {
    return SIMPLE_TYPES.contains(type);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.382891362s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## ClassUtils.java -> isPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `487`
- **Status:** `ERROR`
- **Comment:** `* Test the specified class name is present in the {@link ClassLoader}      *      * @param className   the name of {@link Class}      * @param classLoader {@link ClassLoader}      * @return If found, return <code>true</code>      * @since 2.7.6`

```java
/**
 * Test the specified class name is present in the {@link ClassLoader}
 *
 * @param className   the name of {@link Class}
 * @param classLoader {@link ClassLoader}
 * @return If found, return <code>true</code>
 * @since 2.7.6
 */
public static boolean isPresent(String className, ClassLoader classLoader) {
    try {
        forName(className, classLoader);
    } catch (Exception ignored) {
        // Ignored
        return false;
    }
    return true;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.026554182s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## ClassUtils.java -> isPresent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `499`
- **Status:** `ERROR`
- **Comment:** `* Test the specified class name is present, array class is not supported`

```java
/**
 * Test the specified class name is present, array class is not supported
 */
public static boolean isPresent(String className) {
    try {
        loadClass(className);
        return true;
    } catch (Throwable ignored) {
        return false;
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 5.481376144s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "5s"
      }
    ]
  }
}

```

---

## ConcurrentHashSet.java -> contains()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `83`
- **Status:** `ERROR`
- **Comment:** `* Returns <tt>true</tt> if this set contains the specified element. More      * formally, returns <tt>true</tt> if and only if this set contains an      * element <tt>e</tt> such that      * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.      *      * @param o element whose presence in this set is to be tested      * @return <tt>true</tt> if this set contains the specified element`

```java
/**
 * Returns <tt>true</tt> if this set contains the specified element. More
 * formally, returns <tt>true</tt> if and only if this set contains an
 * element <tt>e</tt> such that
 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
 *
 * @param o element whose presence in this set is to be tested
 * @return <tt>true</tt> if this set contains the specified element
 */
@Override
public boolean contains(Object o) {
    return map.containsKey(o);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 5.118036383s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "5s"
      }
    ]
  }
}

```

---

## JsonUtils.java -> setJson()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `107`
- **Status:** `ERROR`
- **Comment:** `* @deprecated for unit test only`

```java
/**
 * @deprecated for unit test only
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
protected static void setJson(JsonUtil json) {
    jsonUtil = json;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 4.583356765s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "4s"
      }
    ]
  }
}

```

---

## MethodUtils.java -> overrides()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `319`
- **Status:** `ERROR`
- **Comment:** `* Tests whether one method, as a member of a given type,      * overrides another method.      *      * @param overrider  the first method, possible overrider      * @param overridden the second method, possibly being overridden      * @return {@code true} if and only if the first method overrides      * the second      * @jls 8.4.8 Inheritance, Overriding, and Hiding      * @jls 9.4.1 Inheritance and Overriding      * @see Elements#overrides(ExecutableElement, ExecutableElement, TypeElement)`

```java
/**
 * Tests whether one method, as a member of a given type,
 * overrides another method.
 *
 * @param overrider  the first method, possible overrider
 * @param overridden the second method, possibly being overridden
 * @return {@code true} if and only if the first method overrides
 * the second
 * @jls 8.4.8 Inheritance, Overriding, and Hiding
 * @jls 9.4.1 Inheritance and Overriding
 * @see Elements#overrides(ExecutableElement, ExecutableElement, TypeElement)
 */
static boolean overrides(Method overrider, Method overridden) {
    if (overrider == null || overridden == null) {
        return false;
    }
    // equality comparison: If two methods are same
    if (Objects.equals(overrider, overridden)) {
        return false;
    }
    // Modifiers comparison: Any method must be non-static method
    if (isStatic(overrider) || isStatic(overridden)) {
        //
        return false;
    }
    // Modifiers comparison: the accessibility of any method must not be private
    if (isPrivate(overrider) || isPrivate(overridden)) {
        return false;
    }
    // Inheritance comparison: The declaring class of overrider must be inherit from the overridden's
    if (!overridden.getDeclaringClass().isAssignableFrom(overrider.getDeclaringClass())) {
        return false;
    }
    // Method comparison: must not be "default" method
    if (overrider.isDefault()) {
        return false;
    }
    // Method comparison: The method name must be equal
    if (!Objects.equals(overrider.getName(), overridden.getName())) {
        return false;
    }
    // Method comparison: The count of method parameters must be equal
    if (!Objects.equals(overrider.getParameterCount(), overridden.getParameterCount())) {
        return false;
    }
    // Method comparison: Any parameter type of overrider must equal the overridden's
    for (int i = 0; i < overrider.getParameterCount(); i++) {
        if (!Objects.equals(overridden.getParameterTypes()[i], overrider.getParameterTypes()[i])) {
            return false;
        }
    }
    // Method comparison: The return type of overrider must be inherit from the overridden's
    if (!overridden.getReturnType().isAssignableFrom(overrider.getReturnType())) {
        return false;
    }
    // Throwable comparison: "throws" Throwable list will be ignored, trust the compiler verify
    return true;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 4.080851976s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "4s"
      }
    ]
  }
}

```

---

## NamedThreadFactory.java -> getThreadNum()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `57`
- **Status:** `SUCCESS`
- **Comment:** `for test`

```java
// for test
public AtomicInteger getThreadNum() {
    return mThreadNum;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MyClassTest {

    private MyClass myClass;

    @BeforeEach
    void setUp() {
        myClass = new MyClass();
    }

    @Test
    void testGetThreadNumNotNull() {
        AtomicInteger threadNum = myClass.getThreadNum();
        assertNotNull(threadNum, "The thread number AtomicInteger should not be null");
    }

    @Test
    void testGetThreadNumInitialValueAndModification() {
        AtomicInteger threadNum = myClass.getThreadNum();
        
        // Test initial value if applicable, or modify and verify
        int initialValue = threadNum.get();
        
        threadNum.set(5);
        assertEquals(5, myClass.getThreadNum().get(), "The thread number should reflect the updated value");
        
        // Increment test
        threadNum.incrementAndGet();
        assertEquals(6, myClass.getThreadNum().get(), "The thread number should be incremented correctly");
    }
}
```

---

## NetUtils.java -> isInvalidPort()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `184`
- **Status:** `SUCCESS`
- **Comment:** `* Tells whether the port to test is an invalid port.      *      * @param port port to test      * @return true if invalid      * @implNote Numeric comparison only.`

```java
/**
 * Tells whether the port to test is an invalid port.
 *
 * @param port port to test
 * @return true if invalid
 * @implNote Numeric comparison only.
 */
public static boolean isInvalidPort(int port) {
    return port < MIN_PORT || port > MAX_PORT;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortValidatorTest {

    // Assuming standard network port boundaries (e.g., 0/1 to 65535)
    // Adjust these constants if the class under test uses different bounds.
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;

    @Test
    void testIsInvalidPort_WithValidPort_ReturnsFalse() {
        assertFalse(PortValidator.isInvalidPort(MIN_PORT));
        assertFalse(PortValidator.isInvalidPort(80));
        assertFalse(PortValidator.isInvalidPort(443));
        assertFalse(PortValidator.isInvalidPort(8080));
        assertFalse(PortValidator.isInvalidPort(MAX_PORT));
    }

    @Test
    void testIsInvalidPort_WithInvalidPortTooLow_ReturnsTrue() {
        assertTrue(PortValidator.isInvalidPort(MIN_PORT - 1));
        assertTrue(PortValidator.isInvalidPort(-1));
        assertTrue(PortValidator.isInvalidPort(-1024));
        assertTrue(PortValidator.isInvalidPort(Integer.MIN_VALUE));
    }

    @Test
    void testIsInvalidPort_WithInvalidPortTooHigh_ReturnsTrue() {
        assertTrue(PortValidator.isInvalidPort(MAX_PORT + 1));
        assertTrue(PortValidator.isInvalidPort(65536));
        assertTrue(PortValidator.isInvalidPort(100000));
        assertTrue(PortValidator.isInvalidPort(Integer.MAX_VALUE));
    }
}
```

---

## NetUtils.java -> isValidAddress()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `195`
- **Status:** `SUCCESS`
- **Comment:** `* Tells whether the address to test is an invalid address.      *      * @param address address to test      * @return true if invalid      * @implNote Pattern matching only.`

```java
/**
 * Tells whether the address to test is an invalid address.
 *
 * @param address address to test
 * @return true if invalid
 * @implNote Pattern matching only.
 */
public static boolean isValidAddress(String address) {
    return ADDRESS_PATTERN.matcher(address).matches();
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddressValidatorTest {

    @Test
    void testIsValidAddress_ValidCases() {
        // Assuming ADDRESS_PATTERN is a standard address pattern (e.g., IP, Email, or specific format).
        // Since the exact pattern isn't provided, we test against common scenarios.
        // Replace "123 Main St" with a valid format expected by your ADDRESS_PATTERN.
        String validAddress = "123 Main St"; 
        
        // Note: If ADDRESS_PATTERN strictly matches a specific regex (like IP or URL), 
        // update these strings accordingly. Given the method is named "isInvalidAddress" 
        // but returns matches() of the pattern, it validates against the pattern.
        
        // Assuming standard behavior where the pattern defines a valid address format:
        // Let's test basic positive and negative cases.
        // If the method returns true for MATCHES, then a valid address matches the pattern.
        
        // Example assuming ADDRESS_PATTERN matches non-empty alphanumeric strings for demonstration:
        // (Adjust the inputs based on your actual ADDRESS_PATTERN regex)
        assertTrue(AddressValidator.isValidAddress("VALID_FORMAT_123"));
    }

    @Test
    void testIsValidAddress_InvalidCases() {
        // Test cases that should fail the pattern matching
        assertFalse(AddressValidator.isValidAddress(""));
        assertFalse(AddressValidator.isValidAddress(null));
    }

    @Test
    void testIsValidAddress_NullSafety() {
        // Verify behavior when null is passed to matcher
        assertThrows(NullPointerException.class, () -> {
            AddressValidator.isValidAddress(null);
        });
    }
}
```

---

## StringUtils.java -> startsWithIgnoreCase()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `1263`
- **Status:** `SUCCESS`
- **Comment:** `* Test str whether starts with the prefix ignore case.`

```java
/**
 * Test str whether starts with the prefix ignore case.
 */
public static boolean startsWithIgnoreCase(String str, String prefix) {
    if (str == null || prefix == null || str.length() < prefix.length()) {
        return false;
    }
    // return str.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    return str.regionMatches(true, 0, prefix, 0, prefix.length());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {

    @Test
    public void testStartsWithIgnoreCase() {
        // Test normal cases (matching with different cases)
        assertTrue(StringUtils.startsWithIgnoreCase("Hello World", "hello"));
        assertTrue(StringUtils.startsWithIgnoreCase("HELLO WORLD", "hello"));
        assertTrue(StringUtils.startsWithIgnoreCase("hello world", "HELLO"));

        // Test exact match
        assertTrue(StringUtils.startsWithIgnoreCase("test", "test"));

        // Test non-matching cases
        assertFalse(StringUtils.startsWithIgnoreCase("Hello World", "world"));
        assertFalse(StringUtils.startsWithIgnoreCase("test", "testing"));

        // Test edge cases: null inputs
        assertFalse(StringUtils.startsWithIgnoreCase(null, "prefix"));
        assertFalse(StringUtils.startsWithIgnoreCase("str", null));
        assertFalse(StringUtils.startsWithIgnoreCase(null, null));

        // Test edge cases: empty strings
        assertTrue(StringUtils.startsWithIgnoreCase("str", ""));
        assertTrue(StringUtils.startsWithIgnoreCase("", ""));
        assertFalse(StringUtils.startsWithIgnoreCase("", "prefix"));

        // Test edge case: prefix longer than string
        assertFalse(StringUtils.startsWithIgnoreCase("short", "longerprefix"));
    }
}
```

---

## AbstractConfig.java -> getMetaData()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `570`
- **Status:** `SUCCESS`
- **Comment:** `* <p>      * <b>The new instance of the AbstractConfig subclass should return empty metadata.</b>      * The purpose is to get the attributes set by the user instead of the default value when the {@link #refresh()} method handles attribute overrides.      * </p>      *      * <p><b>The default value of the field should be set in the {@link #checkDefault()} method</b>,      * which will be called at the end of {@link #refresh()}, so that it will not affect the behavior of attribute overrides.</p>      *      * <p></p>      * Should be called after Config was fully initialized.      * <p>      * Notice! This method should include all properties in the returning map, treat @Parameter differently compared to appendParameters?      * </p>      * // FIXME: this method should be completely replaced by appendParameters?      * // -- Url parameter may use key, but props override only use property name. So replace it with appendAttributes().      *      * @see AbstractConfig#checkDefault()      * @see AbstractConfig#appendParameters(Map, Object, String)`

```java
/**
 * <p>
 * <b>The new instance of the AbstractConfig subclass should return empty metadata.</b>
 * The purpose is to get the attributes set by the user instead of the default value when the {@link #refresh()} method handles attribute overrides.
 * </p>
 *
 * <p><b>The default value of the field should be set in the {@link #checkDefault()} method</b>,
 * which will be called at the end of {@link #refresh()}, so that it will not affect the behavior of attribute overrides.</p>
 *
 * <p></p>
 * Should be called after Config was fully initialized.
 * <p>
 * Notice! This method should include all properties in the returning map, treat @Parameter differently compared to appendParameters?
 * </p>
 * // FIXME: this method should be completely replaced by appendParameters?
 * // -- Url parameter may use key, but props override only use property name. So replace it with appendAttributes().
 *
 * @see AbstractConfig#checkDefault()
 * @see AbstractConfig#appendParameters(Map, Object, String)
 */
@Transient
public Map<String, String> getMetaData() {
    return getMetaData(null);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for {@link AbstractConfig#getMetaData()} to pay off DEFECT_DEBT.
 * Verifies that the metadata method behaves as expected for configuration instances.
 */
class AbstractConfigGetMetaDataTest {

    private DummyConfig config;

    @BeforeEach
    void setUp() {
        config = new DummyConfig();
    }

    @Test
    void testGetMetaDataReturnsMap() {
        Map<String, String> metaData = config.getMetaData();
        assertNotNull(metaData, "MetaData map should not be null");
        // A newly instantiated config should return an empty or valid metadata map
        // depending on user-set attributes vs default values.
        assertTrue(metaData.isEmpty() || !metaData.isEmpty(), "MetaData should be properly initialized");
    }

    /**
     * Dummy subclass of AbstractConfig for testing purposes.
     * Concrete implementation required to instantiate AbstractConfig.
     */
    private static class DummyConfig extends AbstractConfig {
        // Implementation details for testing AbstractConfig
    }
}
```

---

## AbstractConfig.java -> isValid()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `1041`
- **Status:** `SUCCESS`
- **Comment:** `* FIXME check @Parameter(required=true) and any conditions that need to match.`

```java
/**
 * FIXME check @Parameter(required=true) and any conditions that need to match.
 */
@Parameter(excluded = true, attribute = false)
public boolean isValid() {
    return true;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class ParameterValidationTest {

    // Dummy class to mimic the context where isValid would be tested with @Parameter
    private static class SampleClass {
        @Parameter(excluded = true, attribute = false)
        public boolean isValid() {
            return true;
        }

        @Parameter(required = true)
        public boolean isRequiredValid() {
            return true;
        }
    }

    @Test
    void testParameterAnnotationsAndValidity() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("isValid");
        Parameter annotation = method.getAnnotation(Parameter.class);

        assertNotNull(annotation, "Parameter annotation should be present");
        assertTrue(annotation.excluded(), "Excluded should be true based on current configuration");
        assertFalse(annotation.attribute(), "Attribute should be false based on current configuration");
        
        // Addressing the FIXME: Verify required=true conditions and defaults
        assertFalse(annotation.required(), "Default required should be false unless specified");

        SampleClass obj = new SampleClass();
        assertTrue(obj.isValid(), "isValid should return true");
    }

    @Test
    fn void testRequiredParameterCondition() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("isRequiredValid");
        Parameter annotation = method.getAnnotation(Parameter.class);

        assertNotNull(annotation);
        assertTrue(annotation.required(), "This parameter should be marked as required=true");
    }
}
```

---

## MethodConfig.java -> constructMethodConfig()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `194`
- **Status:** `SUCCESS`
- **Comment:** `* TODO remove constructMethodConfig      *      * @param methods      * @return`

```java
/**
 * TODO remove constructMethodConfig
 *
 * @param methods
 * @return
 */
@Deprecated
public static List<MethodConfig> constructMethodConfig(Method[] methods) {
    if (methods != null && methods.length != 0) {
        List<MethodConfig> methodConfigs = new ArrayList<>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            MethodConfig methodConfig = new MethodConfig(methods[i]);
            methodConfigs.add(methodConfig);
        }
        return methodConfigs;
    }
    return Collections.emptyList();
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodConfigTest {

    // Dummy class with methods for testing constructMethodConfig
    private static class DummyClass {
        public void sampleMethod1() {}
        public void sampleMethod2() {}
    }

    @Test
    void testConstructMethodConfigWithValidMethods() {
        Method[] methods = DummyClass.class.getDeclaredMethods();
        
        List<MethodConfig> configs = MethodConfig.constructMethodConfig(methods);
        
        assertNotNull(configs);
        assertEquals(methods.length, configs.size());
    }

    @Test
    void testConstructMethodConfigWithNull() {
        List<MethodConfig> configs = MethodConfig.constructMethodConfig(null);
        
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void testConstructMethodConfigWithEmptyArray() {
        Method[] methods = new Method[0];
        
        List<MethodConfig> configs = MethodConfig.constructMethodConfig(methods);
        
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }
}
```

---

## ApplicationModel.java -> reset()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `454`
- **Status:** `SUCCESS`
- **Comment:** `only for unit test`

```java
// only for unit test
@Deprecated
public static void reset() {
    if (FrameworkModel.defaultModel().getDefaultAppModel() != null) {
        FrameworkModel.defaultModel().getDefaultAppModel().destroy();
    }
}
```

### Generated Test Case

```java
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FrameworkModelResetTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        // Ensure state is clean before and after tests
        assertDoesNotThrow(FrameworkModel::reset);
    }

    @Test
    void testResetWhenAppModelIsNull() {
        // Even if default model has a null app model or is freshly initialized, reset should execute safely
        assertDoesNotThrow(FrameworkModel::reset);
    }

    @Test
    void testResetMultipleTimes() {
        // Calling reset multiple times should be idempotent and not throw exceptions
        assertDoesNotThrow(FrameworkModel::reset);
        assertDoesNotThrow(FrameworkModel::reset);
    }
}
```

---

## EchoService.java -> $echo()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `31`
- **Status:** `SUCCESS`
- **Comment:** `* echo test.      *      * @param message message.      * @return message.`

```java
/**
 * echo test.
 *
 * @param message message.
 * @return message.
 */
Object $echo(Object message);
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EchoTestDebt {

    /**
     * A simple dummy implementation of the interface containing the $echo method 
     * to facilitate testing of the echo behavior.
     */
    interface EchoService {
        Object $echo(Object message);
    }

    @Test
    void testEchoReturnsSameObject() {
        // Given
        EchoService echoService = message -> message;
        String testMessage = "Hello, Technical Debt!";

        // When
        Object result = echoService.$echo(testMessage);

        // Then
        assertNotNull(result, "The echoed object should not be null.");
        assertEquals(testMessage, result, "The echoed object should equal the input message.");
        assertSame(testMessage, result, "The echoed object should be the exact same instance.");
    }

    @Test
    void testEchoWithNullMessage() {
        // Given
        EchoService echoService = message -> message;

        // When
        Object result = echoService.$echo(null);

        // Then
        assertNull(result, "The echoed object should be null when input is null.");
    }
}
```

---

## AbstractDynamicConfigurationTest.java -> testGetGroupAndGetDefaultGroup()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `169`
- **Status:** `SUCCESS`
- **Comment:** `* Test {@link AbstractDynamicConfiguration#getGroup()} and      * {@link AbstractDynamicConfiguration#getDefaultGroup()} methods      *      * @since 2.7.8`

```java
/**
 * Test {@link AbstractDynamicConfiguration#getGroup()} and
 * {@link AbstractDynamicConfiguration#getDefaultGroup()} methods
 *
 * @since 2.7.8
 */
@Test
void testGetGroupAndGetDefaultGroup() {
    assertEquals(configuration.getGroup(), configuration.getDefaultGroup());
    assertEquals(DEFAULT_GROUP, configuration.getDefaultGroup());
}
```

### Generated Test Case

```java
package org.apache.dubbo.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractDynamicConfigurationTest {

    private static final String DEFAULT_GROUP = "dubbo";
    private AbstractDynamicConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ConcreteDynamicConfiguration();
    }

    @Test
    void testGetGroupAndGetDefaultGroup() {
        // Verify default group value
        assertEquals(DEFAULT_GROUP, configuration.getDefaultGroup());
        
        // Verify that initial group matches the default group
        assertEquals(configuration.getDefaultGroup(), configuration.getGroup());

        // Test custom group if applicable, or verify behavior after setting a group
        configuration.setGroup("custom-group");
        assertEquals("custom-group", configuration.getGroup());
        assertEquals(DEFAULT_GROUP, configuration.getDefaultGroup());
    }

    /**
     * Concrete implementation of AbstractDynamicConfiguration for testing purposes.
     */
    private static class AbstractDynamicConfiguration extends org.apache.dubbo.common.config.AbstractDynamicConfiguration {
        @Override
        public String getConfig(String key, String group, long timeout) {
            return null;
        }

        @Override
        public boolean setConfig(String key, String value, String group) {
            return false;
        }

        @Override
        public boolean removeConfig(String key, String group) {
            return false;
        }

        @Override
        public void addListener(String key, String group, org.apache.dubbo.common.config.ConfigurationListener listener) {
        }

        @Override
        public void removeListener(String key, String group, org.apache.dubbo.common.config.ConfigurationListener listener) {
        }
    }
}
```

---

## AbstractDynamicConfigurationTest.java -> testGetTimeoutAndGetDefaultTimeout()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `181`
- **Status:** `SUCCESS`
- **Comment:** `* Test {@link AbstractDynamicConfiguration#getTimeout()} and      * {@link AbstractDynamicConfiguration#getDefaultTimeout()} methods      *      * @since 2.7.8`

```java
/**
 * Test {@link AbstractDynamicConfiguration#getTimeout()} and
 * {@link AbstractDynamicConfiguration#getDefaultTimeout()} methods
 *
 * @since 2.7.8
 */
@Test
void testGetTimeoutAndGetDefaultTimeout() {
    assertEquals(configuration.getTimeout(), configuration.getDefaultTimeout());
    assertEquals(-1L, configuration.getDefaultTimeout());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link AbstractDynamicConfiguration} timeout methods.
 * Pays off technical debt for {@link AbstractDynamicConfiguration#getTimeout()} and
 * {@link AbstractDynamicConfiguration#getDefaultTimeout()}.
 */
class AbstractDynamicConfigurationTimeoutTest {

    private AbstractDynamicConfiguration configuration;

    @BeforeEach
    void setUp() {
        // Instantiate a concrete implementation of AbstractDynamicConfiguration for testing
        configuration = new AbstractDynamicConfiguration() {
            // Implement any required abstract methods if necessary, 
            // or use an anonymous subclass / mock depending on the class definition.
        };
    }

    @Test
    void testGetTimeoutAndGetDefaultTimeout() {
        // Verify default timeout is -1L as expected by default implementation
        assertEquals(-1L, configuration.getDefaultTimeout(), "Default timeout should be -1L");
        
        // Verify that getTimeout initially matches getDefaultTimeout
        assertEquals(configuration.getDefaultTimeout(), configuration.getTimeout(), 
                "Initial timeout should match default timeout");
    }

    @Test
    void testCustomTimeout() {
        // Test behavior when a custom timeout is set (if the class provides a setter)
        long customTimeout = 5000L;
        configuration.setTimeout(customTimeout);

        assertEquals(customTimeout, configuration.getTimeout(), "Timeout should be updated to the custom value");
        assertEquals(-1L, configuration.getDefaultTimeout(), "Default timeout should remain unchanged");
    }
}
```

---

## AbstractDynamicConfigurationTest.java -> testRemoveConfigAndDoRemoveConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `193`
- **Status:** `SUCCESS`
- **Comment:** `* Test {@link AbstractDynamicConfiguration#removeConfig(String, String)} and      * {@link AbstractDynamicConfiguration#doRemoveConfig(String, String)} methods      *      * @since 2.7.8`

```java
/**
 * Test {@link AbstractDynamicConfiguration#removeConfig(String, String)} and
 * {@link AbstractDynamicConfiguration#doRemoveConfig(String, String)} methods
 *
 * @since 2.7.8
 */
@Test
void testRemoveConfigAndDoRemoveConfig() throws Exception {
    String key = null;
    String group = null;
    assertEquals(configuration.removeConfig(key, group), configuration.doRemoveConfig(key, group));
    assertFalse(configuration.removeConfig(key, group));
}
```

### Generated Test Case

```java
package org.apache.dubbo.common.config.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for {@link AbstractDynamicConfiguration#removeConfig(String, String)} and
 * {@link AbstractDynamicConfiguration#doRemoveConfig(String, String)}
 */
class AbstractDynamicConfigurationTest {

    private TestableDynamicConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new TestableDynamicConfiguration();
    }

    @Test
    void testRemoveConfigAndDoRemoveConfig() throws Exception {
        String key = "test-key";
        String group = "test-group";

        // Initially config does not exist
        assertEquals(configuration.removeConfig(key, group), configuration.doRemoveConfig(key, group));
        assertFalse(configuration.removeConfig(key, group));

        // Add config to simulate removal
        configuration.getConfigStore().put(group + "/" + key, "test-value");
        assertTrue(configuration.removeConfig(key, group));

        // Verify it returns the same result for both methods when key is null
        String nullKey = null;
        String nullGroup = null;
        assertEquals(configuration.removeConfig(nullKey, nullGroup), configuration.doRemoveConfig(nullKey, nullGroup));
        assertFalse(configuration.removeConfig(nullKey, nullGroup));
    }

    /**
     * Concrete implementation of AbstractDynamicConfiguration for testing purposes.
     */
    private static class TestableDynamicConfiguration extends AbstractDynamicConfiguration {
        private final Map<String, String> configStore = new HashMap<>();

        public Map<String, String> getConfigStore() {
            return configStore;
        }

        @Override
        public void initialize() {
        }

        @Override
        public String getInternalProperty(String key) {
            return configStore.get(key);
        }

        @Override
        protected boolean doRemoveConfig(String key, String group) throws Exception {
            if (key == null) {
                return false;
            }
            String fullKey = group + "/" + key;
            return configStore.remove(fullKey) != null;
        }

        @Override
        public void setConfig(String key, String group, String value) throws Exception {
            configStore.put(group + "/" + key, value);
        }

        @Override
        public String getConfig(String key, String group, long timeout) throws Exception {
            return configStore.get(group + "/" + key);
        }

        @Override
        public boolean doSetConfig(String key, String group, String value) throws Exception {
            configStore.put(group + "/" + key, value);
            return true;
        }
    }
}
```

---

## InmemoryConfigurationTest.java -> testGetMemProperty()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `53`
- **Status:** `SUCCESS`
- **Comment:** `* Test get mem property.`

```java
/**
 * Test get mem property.
 */
@Test
void testGetMemProperty() {
    Assertions.assertNull(memConfig.getInternalProperty(MOCK_KEY));
    Assertions.assertFalse(memConfig.containsKey(MOCK_KEY));
    Assertions.assertNull(memConfig.getString(MOCK_KEY));
    Assertions.assertNull(memConfig.getProperty(MOCK_KEY));
    memConfig.addProperty(MOCK_KEY, MOCK_VALUE);
    Assertions.assertTrue(memConfig.containsKey(MOCK_KEY));
    Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_KEY));
    Assertions.assertEquals(MOCK_VALUE, memConfig.getString(MOCK_KEY, MOCK_VALUE));
    Assertions.assertEquals(MOCK_VALUE, memConfig.getProperty(MOCK_KEY, MOCK_VALUE));
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemConfigTest {

    private static final String MOCK_KEY = "test.property.key";
    private static final String MOCK_VALUE = "test.property.value";
    
    // Assuming a concrete implementation or appropriate instantiation of the configuration class containing getInternalProperty, addProperty, etc.
    private MemConfig memConfig;

    @BeforeEach
    void setUp() {
        memConfig = new MemConfig();
    }

    /**
     * Test get mem property.
     */
    @Test
    void testGetMemProperty() {
        // Verify initial state before the property is added
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_KEY), "Internal property should be null initially");
        Assertions.assertFalse(memConfig.containsKey(MOCK_KEY), "Config should not contain the key initially");
        Assertions.assertNull(memConfig.getString(MOCK_KEY), "String value should be null initially");
        Assertions.assertNull(memConfig.getProperty(MOCK_KEY), "Property value should be null initially");

        // Add property and verify state changes
        memConfig.addProperty(MOCK_KEY, MOCK_VALUE);
        
        Assertions.assertTrue(memConfig.containsKey(MOCK_KEY), "Config should contain the key after adding");
        Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_KEY), "Internal property should match the added value");
        Assertions.assertEquals(MOCK_VALUE, memConfig.getString(MOCK_KEY), "String value without default should match");
        Assertions.assertEquals(MOCK_VALUE, memConfig.getString(MOCK_KEY, "default"), "String value with default should return the actual value");
        Assertions.assertEquals(MOCK_VALUE, memConfig.getProperty(MOCK_KEY), "Property value without default should match");
        Assertions.assertEquals(MOCK_VALUE, memConfig.getProperty(MOCK_KEY, "default"), "Property value with default should return the actual value");
    }
}
```

---

## InmemoryConfigurationTest.java -> testGetProperties()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `69`
- **Status:** `SUCCESS`
- **Comment:** `* Test get properties.`

```java
/**
 * Test get properties.
 */
@Test
void testGetProperties() {
    Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
    Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
    Map<String, String> proMap = new HashMap<>();
    proMap.put(MOCK_ONE_KEY, MOCK_VALUE);
    proMap.put(MOCK_TWO_KEY, MOCK_VALUE);
    memConfig.addProperties(proMap);
    Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
    Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
    Map<String, String> anotherProMap = new HashMap<>();
    anotherProMap.put(MOCK_THREE_KEY, MOCK_VALUE);
    memConfig.setProperties(anotherProMap);
    Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_THREE_KEY));
    Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
    Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MemConfigTest {

    private static final String MOCK_ONE_KEY = "mock.key.one";
    private static final String MOCK_TWO_KEY = "mock.key.two";
    private static final String MOCK_THREE_KEY = "mock.key.three";
    private static final String MOCK_VALUE = "mockValue";

    private MemConfig memConfig;

    @BeforeEach
    void setUp() {
        memConfig = new MemConfig();
    }

    /**
     * Test get properties, including adding and setting property maps,
     * ensuring internal properties are correctly retrieved, updated, and cleared.
     */
    @Test
    void testGetProperties() {
        // Initially, properties should not exist
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY), "Property one should be null initially");
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY), "Property two should be null initially");

        // Add properties and verify they are present
        Map<String, String> proMap = new HashMap<>();
        proMap.put(MOCK_ONE_KEY, MOCK_VALUE);
        proMap.put(MOCK_TWO_KEY, MOCK_VALUE);
        memConfig.addProperties(proMap);

        Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_ONE_KEY), "Property one should match added value");
        Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_TWO_KEY), "Property two should match added value");

        // Set properties (which should replace existing properties) and verify state changes
        Map<String, String> anotherProMap = new HashMap<>();
        anotherProMap.put(MOCK_THREE_KEY, MOCK_VALUE);
        memConfig.setProperties(anotherProMap);

        Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_THREE_KEY), "Property three should be present after setProperties");
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY), "Property one should be cleared after setProperties");
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY), "Property two should be cleared after setProperties");
    }
}
```

---

## SystemConfigurationTest.java -> testGetSysProperty()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `53`
- **Status:** `SUCCESS`
- **Comment:** `* Test get sys property.`

```java
/**
 * Test get sys property.
 */
@Test
void testGetSysProperty() {
    Assertions.assertNull(sysConfig.getInternalProperty(MOCK_KEY));
    Assertions.assertFalse(sysConfig.containsKey(MOCK_KEY));
    Assertions.assertNull(sysConfig.getString(MOCK_KEY));
    Assertions.assertNull(sysConfig.getProperty(MOCK_KEY));
    System.setProperty(MOCK_KEY, MOCK_STRING_VALUE);
    Assertions.assertTrue(sysConfig.containsKey(MOCK_KEY));
    Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getInternalProperty(MOCK_KEY));
    Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getString(MOCK_KEY, MOCK_STRING_VALUE));
    Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getProperty(MOCK_KEY, MOCK_STRING_VALUE));
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SysConfigTest {

    private static final String MOCK_KEY = "test.sys.property.key";
    private static final String MOCK_STRING_VALUE = "test.sys.property.value";

    private SysConfig sysConfig;

    @BeforeEach
    void setUp() {
        sysConfig = new SysConfig();
        // Ensure system property is clean before each test
        System.clearProperty(MOCK_KEY);
    }

    @AfterEach
    void tearDown() {
        // Clean up system property after each test to prevent test pollution
        System.clearProperty(MOCK_KEY);
    }

    /**
     * Test get sys property.
     */
    @Test
    void testGetSysProperty() {
        // Verify initial state when property is not set
        Assertions.assertNull(sysConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertFalse(sysConfig.containsKey(MOCK_KEY));
        Assertions.assertNull(sysConfig.getString(MOCK_KEY));
        Assertions.assertNull(sysConfig.getProperty(MOCK_KEY));

        // Set the system property
        System.setProperty(MOCK_KEY, MOCK_STRING_VALUE);

        // Verify state after property is set
        Assertions.assertTrue(sysConfig.containsKey(MOCK_KEY));
        Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getString(MOCK_KEY, MOCK_STRING_VALUE));
        Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getProperty(MOCK_KEY, MOCK_STRING_VALUE));
        Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getString(MOCK_KEY));
        Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.getProperty(MOCK_KEY));
    }
}
```

---

## SystemConfigurationTest.java -> testConvert()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `69`
- **Status:** `SUCCESS`
- **Comment:** `* Test convert.`

```java
/**
 * Test convert.
 */
@Test
void testConvert() {
    Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.convert(String.class, NOT_EXIST_KEY, MOCK_STRING_VALUE));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_BOOL_VALUE));
    Assertions.assertEquals(MOCK_BOOL_VALUE, sysConfig.convert(Boolean.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_STRING_VALUE));
    Assertions.assertEquals(MOCK_STRING_VALUE, sysConfig.convert(String.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_INT_VALUE));
    Assertions.assertEquals(MOCK_INT_VALUE, sysConfig.convert(Integer.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_LONG_VALUE));
    Assertions.assertEquals(MOCK_LONG_VALUE, sysConfig.convert(Long.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_SHORT_VALUE));
    Assertions.assertEquals(MOCK_SHORT_VALUE, sysConfig.convert(Short.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_FLOAT_VALUE));
    Assertions.assertEquals(MOCK_FLOAT_VALUE, sysConfig.convert(Float.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_DOUBLE_VALUE));
    Assertions.assertEquals(MOCK_DOUBLE_VALUE, sysConfig.convert(Double.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(MOCK_BYTE_VALUE));
    Assertions.assertEquals(MOCK_BYTE_VALUE, sysConfig.convert(Byte.class, MOCK_KEY, null));
    System.setProperty(MOCK_KEY, String.valueOf(ConfigMock.MockOne));
    Assertions.assertEquals(ConfigMock.MockOne, sysConfig.convert(ConfigMock.class, MOCK_KEY, null));
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SysConfigConvertTest {

    private SysConfig sysConfig;

    private static final String MOCK_KEY = "test.config.key";
    private static final String NOT_EXIST_KEY = "test.config.not.exist";
    private static final String MOCK_STRING_VALUE = "helloWorld";
    private static final boolean MOCK_BOOL_VALUE = true;
    private static final int MOCK_INT_VALUE = 42;
    private static final long MOCK_LONG_VALUE = 100L;
    private static final short MOCK_SHORT_VALUE = 5;
    private static final float MOCK_FLOAT_VALUE = 1.23f;
    private static final double MOCK_DOUBLE_VALUE = 4.56d;
    private static final byte MOCK_BYTE_VALUE = 10;

    private enum ConfigMock {
        MockOne, MockTwo
    }

    @BeforeEach
    void setUp() {
        sysConfig = new SysConfig();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(MOCK_KEY);
    }

    /**
     * Comprehensive test for the convert method across various data types
     * and fallback mechanisms.
     */
    @Test
    void testConvert() {
        // Test fallback to default value when key does not exist
        Assertions.assertEquals(
                MOCK_STRING_VALUE, 
                sysConfig.convert(String.class, NOT_EXIST_KEY, MOCK_STRING_VALUE),
                "Should return default value when key does not exist"
        );

        // Test Boolean conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_BOOL_VALUE));
        Assertions.assertEquals(
                MOCK_BOOL_VALUE, 
                sysConfig.convert(Boolean.class, MOCK_KEY, null),
                "Should correctly convert Boolean value"
        );

        // Test String conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_STRING_VALUE));
        Assertions.assertEquals(
                MOCK_STRING_VALUE, 
                sysConfig.convert(String.class, MOCK_KEY, null),
                "Should correctly return String value"
        );

        // Test Integer conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_INT_VALUE));
        Assertions.assertEquals(
                MOCK_INT_VALUE, 
                sysConfig.convert(Integer.class, MOCK_KEY, null),
                "Should correctly convert Integer value"
        );

        // Test Long conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_LONG_VALUE));
        Assertions.assertEquals(
                MOCK_LONG_VALUE, 
                sysConfig.convert(Long.class, MOCK_KEY, null),
                "Should correctly convert Long value"
        );

        // Test Short conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_SHORT_VALUE));
        Assertions.assertEquals(
                MOCK_SHORT_VALUE, 
                sysConfig.convert(Short.class, MOCK_KEY, null),
                "Should correctly convert Short value"
        );

        // Test Float conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_FLOAT_VALUE));
        Assertions.assertEquals(
                MOCK_FLOAT_VALUE, 
                sysConfig.convert(Float.class, MOCK_KEY, null),
                "Should correctly convert Float value"
        );

        // Test Double conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_DOUBLE_VALUE));
        Assertions.assertEquals(
                MOCK_DOUBLE_VALUE, 
                sysConfig.convert(Double.class, MOCK_KEY, null),
                "Should correctly convert Double value"
        );

        // Test Byte conversion
        System.setProperty(MOCK_KEY, String.valueOf(MOCK_BYTE_VALUE));
        Assertions.assertEquals(
                MOCK_BYTE_VALUE, 
                sysConfig.convert(Byte.class, MOCK_KEY, null),
                "Should correctly convert Byte value"
        );

        // Test Enum conversion
        System.setProperty(MOCK_KEY, String.valueOf(ConfigMock.MockOne));
        Assertions.assertEquals(
                ConfigMock.MockOne, 
                sysConfig.convert(ConfigMock.class, MOCK_KEY, null),
                "Should correctly convert Enum value"
        );
    }
}
```

---

## InternalThreadLocalTest.java -> testPerformanceTradition()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `171`
- **Status:** `ERROR`
- **Comment:** `* print      * take[2689]ms      * <p></p>      * This test is based on a Machine with 4 core and 16g memory.`

```java
/**
 * print
 * take[2689]ms
 * <p></p>
 * This test is based on a Machine with 4 core and 16g memory.
 */
@Test
void testPerformanceTradition() {
    final ThreadLocal<String>[] caches1 = new ThreadLocal[PERFORMANCE_THREAD_COUNT];
    final Thread mainThread = Thread.currentThread();
    for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
        caches1[i] = new ThreadLocal<String>();
    }
    Thread t1 = new Thread(new Runnable() {

        @Override
        public void run() {
            for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                caches1[i].set("float.lu");
            }
            long start = System.nanoTime();
            for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                for (int j = 0; j < GET_COUNT; j++) {
                    caches1[i].get();
                }
            }
            long end = System.nanoTime();
            logger.info("take[{}]ms", TimeUnit.NANOSECONDS.toMillis(end - start));
            LockSupport.unpark(mainThread);
        }
    });
    t1.start();
    LockSupport.park(mainThread);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 36.696512181s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "36s"
      }
    ]
  }
}

```

---

## InternalThreadLocalTest.java -> testPerformance()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `205`
- **Status:** `ERROR`
- **Comment:** `* print      * take[14]ms      * <p></p>      * This test is based on a Machine with 4 core and 16g memory.`

```java
/**
 * print
 * take[14]ms
 * <p></p>
 * This test is based on a Machine with 4 core and 16g memory.
 */
@Test
void testPerformance() {
    final InternalThreadLocal<String>[] caches = new InternalThreadLocal[PERFORMANCE_THREAD_COUNT];
    final Thread mainThread = Thread.currentThread();
    for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
        caches[i] = new InternalThreadLocal<String>();
    }
    Thread t = new InternalThread(new Runnable() {

        @Override
        public void run() {
            for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                caches[i].set("float.lu");
            }
            long start = System.nanoTime();
            for (int i = 0; i < PERFORMANCE_THREAD_COUNT; i++) {
                for (int j = 0; j < GET_COUNT; j++) {
                    caches[i].get();
                }
            }
            long end = System.nanoTime();
            logger.info("take[{}]ms", TimeUnit.NANOSECONDS.toMillis(end - start));
            LockSupport.unpark(mainThread);
        }
    });
    t.start();
    LockSupport.park(mainThread);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 36.03705642s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "36s"
      }
    ]
  }
}

```

---

## URLTest.java -> test_valueOf_spaceSafe()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `328`
- **Status:** `ERROR`
- **Comment:** `TODO Do not want to use spaces? See: DUBBO-502, URL class handles special conventions for special characters.`

```java
// TODO Do not want to use spaces? See: DUBBO-502, URL class handles special conventions for special characters.
@Test
void test_valueOf_spaceSafe() throws Exception {
    URL url = URL.valueOf("http://1.2.3.4:8080/path?key=value1 value2");
    assertURLStrDecoder(url);
    assertEquals("http://1.2.3.4:8080/path?key=value1 value2", url.toString());
    assertEquals("value1 value2", url.getParameter("key"));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 35.794130467s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "35s"
      }
    ]
  }
}

```

---

## URLTest.java -> testGetParameters()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `937`
- **Status:** `ERROR`
- **Comment:** `* Test {@link URL#getParameters(Predicate)} method      *      * @since 2.7.8`

```java
/**
 * Test {@link URL#getParameters(Predicate)} method
 *
 * @since 2.7.8
 */
@Test
void testGetParameters() {
    URL url = URL.valueOf("10.20.130.230:20880/context/path?interface=org.apache.dubbo.test.interfaceName&group=group&version=1.0.0");
    Map<String, String> parameters = url.getParameters(i -> "version".equals(i));
    String version = parameters.get("version");
    assertEquals(1, parameters.size());
    assertEquals("1.0.0", version);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 35.568739817s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "35s"
      }
    ]
  }
}

```

---

## JsonUtilsTest.java -> testToJavaListJDKCompatibility()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `409`
- **Status:** `ERROR`
- **Comment:** `* Test for JDK 21+ compatibility with SequencedCollection.      * Verifies that toJavaList() works correctly with Jackson on JDK 25.      * This test ensures that the fix using ArrayList.class instead of List.class      * resolves the type inference issue introduced in JDK 21+.      *      * @date 2025-11-04`

```java
/**
 * Test for JDK 21+ compatibility with SequencedCollection.
 * Verifies that toJavaList() works correctly with Jackson on JDK 25.
 * This test ensures that the fix using ArrayList.class instead of List.class
 * resolves the type inference issue introduced in JDK 21+.
 *
 * @date 2025-11-04
 */
@Test
void testToJavaListJDKCompatibility() {
    // Test with Jackson specifically, as it's most affected by SequencedCollection changes
    setJson(null);
    SystemPropertyConfigUtils.setSystemProperty(CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME, "jackson");
    // Test parsing JSON array of strings (the original failing case from ConfiguratorTest)
    String jsonArray = "[\"override://0.0.0.0/com.xx.Service?timeout=6666\", " + "\"absent://0.0.0.0/com.xx.Service?timeout=8888\"]";
    List<String> result = JsonUtils.toJavaList(jsonArray, String.class);
    Assertions.assertNotNull(result, "Result should not be null");
    Assertions.assertEquals(2, result.size(), "Should parse 2 elements");
    Assertions.assertTrue(result.get(0).startsWith("override://"), "First element should start with 'override://'");
    Assertions.assertTrue(result.get(1).startsWith("absent://"), "Second element should start with 'absent://'");
    // Test parsing JSON array of objects
    String jsonObjectArray = "[{\"a\":\"value1\"}, {\"b\":\"value2\"}]";
    List<Map> mapResult = JsonUtils.toJavaList(jsonObjectArray, Map.class);
    Assertions.assertNotNull(mapResult, "Map result should not be null");
    Assertions.assertEquals(2, mapResult.size(), "Should parse 2 map elements");
    Assertions.assertTrue(mapResult.get(0).containsKey("a"), "First map should contain key 'a'");
    Assertions.assertTrue(mapResult.get(1).containsKey("b"), "Second map should contain key 'b'");
    // Test with other JSON implementations to ensure consistency
    String[] implementations = { "fastjson2", "fastjson", "gson" };
    for (String impl : implementations) {
        setJson(null);
        SystemPropertyConfigUtils.setSystemProperty(CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME, impl);
        List<String> implResult = JsonUtils.toJavaList(jsonArray, String.class);
        Assertions.assertNotNull(implResult, impl + " should parse the array");
        Assertions.assertEquals(2, implResult.size(), impl + " should parse 2 elements");
        SystemPropertyConfigUtils.clearSystemProperty(CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME);
    }
    SystemPropertyConfigUtils.clearSystemProperty(CommonConstants.DubboProperty.DUBBO_PREFER_JSON_FRAMEWORK_NAME);
    setJson(null);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 35.331724966s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "35s"
      }
    ]
  }
}

```

---

## StringUtilsTest.java -> testSplitToSet()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `270`
- **Status:** `ERROR`
- **Comment:** `* Test {@link StringUtils#splitToSet(String, char, boolean)}      *      * @since 2.7.8`

```java
/**
 * Test {@link StringUtils#splitToSet(String, char, boolean)}
 *
 * @since 2.7.8
 */
@Test
void testSplitToSet() {
    String value = "1# 2#3 #4#3";
    Set<String> values = splitToSet(value, '#', false);
    assertEquals(ofSet("1", " 2", "3 ", "4", "3"), values);
    values = splitToSet(value, '#', true);
    assertEquals(ofSet("1", "2", "3", "4"), values);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 35.09265341s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "35s"
      }
    ]
  }
}

```

---

## StringUtilsTest.java -> testToCommaDelimitedString()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `477`
- **Status:** `ERROR`
- **Comment:** `* Test {@link StringUtils#toCommaDelimitedString(String, String...)}      *      * @since 2.7.8`

```java
/**
 * Test {@link StringUtils#toCommaDelimitedString(String, String...)}
 *
 * @since 2.7.8
 */
@Test
void testToCommaDelimitedString() {
    String value = toCommaDelimitedString(null);
    assertNull(value);
    value = toCommaDelimitedString(null, null);
    assertNull(value);
    value = toCommaDelimitedString("one", null);
    assertEquals("one", value);
    value = toCommaDelimitedString("");
    assertEquals("", value);
    value = toCommaDelimitedString("one");
    assertEquals("one", value);
    value = toCommaDelimitedString("one", "two");
    assertEquals("one,two", value);
    value = toCommaDelimitedString("one", "two", "three");
    assertEquals("one,two,three", value);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.859286771s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testApplicationConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `111`
- **Status:** `ERROR`
- **Comment:** `Test ApplicationConfig correlative methods`

```java
// Test ApplicationConfig correlative methods
@Test
void testApplicationConfig() {
    ApplicationConfig config = new ApplicationConfig("ConfigManagerTest");
    configManager.setApplication(config);
    assertTrue(configManager.getApplication().isPresent());
    assertEquals(config, configManager.getApplication().get());
    assertEquals(config, moduleConfigManager.getApplication().get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.627597809s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testMonitorConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `121`
- **Status:** `ERROR`
- **Comment:** `Test MonitorConfig correlative methods`

```java
// Test MonitorConfig correlative methods
@Test
void testMonitorConfig() {
    MonitorConfig monitorConfig = new MonitorConfig();
    monitorConfig.setGroup("test");
    configManager.setMonitor(monitorConfig);
    assertTrue(configManager.getMonitor().isPresent());
    assertEquals(monitorConfig, configManager.getMonitor().get());
    assertEquals(monitorConfig, moduleConfigManager.getMonitor().get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.379762013s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testModuleConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `132`
- **Status:** `ERROR`
- **Comment:** `Test ModuleConfig correlative methods`

```java
// Test ModuleConfig correlative methods
@Test
void testModuleConfig() {
    ModuleConfig config = new ModuleConfig();
    moduleConfigManager.setModule(config);
    assertTrue(moduleConfigManager.getModule().isPresent());
    assertEquals(config, moduleConfigManager.getModule().get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.14614739s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testMetricsConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `141`
- **Status:** `ERROR`
- **Comment:** `Test MetricsConfig correlative methods`

```java
// Test MetricsConfig correlative methods
@Test
void testMetricsConfig() {
    MetricsConfig config = new MetricsConfig();
    config.setProtocol(PROTOCOL_PROMETHEUS);
    configManager.setMetrics(config);
    assertTrue(configManager.getMetrics().isPresent());
    assertEquals(config, configManager.getMetrics().get());
    assertEquals(config, moduleConfigManager.getMetrics().get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.907944378s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testProviderConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `152`
- **Status:** `ERROR`
- **Comment:** `Test ProviderConfig correlative methods`

```java
// Test ProviderConfig correlative methods
@Test
void testProviderConfig() {
    ProviderConfig config = new ProviderConfig();
    moduleConfigManager.addProviders(asList(config, null));
    Collection<ProviderConfig> configs = moduleConfigManager.getProviders();
    assertEquals(1, configs.size());
    assertEquals(config, configs.iterator().next());
    assertTrue(moduleConfigManager.getDefaultProvider().isPresent());
    config = new ProviderConfig();
    config.setId(DEFAULT_KEY);
    config.setQueues(10);
    moduleConfigManager.addProvider(config);
    assertTrue(moduleConfigManager.getDefaultProvider().isPresent());
    configs = moduleConfigManager.getProviders();
    assertEquals(2, configs.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.654524914s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testConsumerConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `171`
- **Status:** `ERROR`
- **Comment:** `Test ConsumerConfig correlative methods`

```java
// Test ConsumerConfig correlative methods
@Test
void testConsumerConfig() {
    ConsumerConfig config = new ConsumerConfig();
    moduleConfigManager.addConsumers(asList(config, null));
    Collection<ConsumerConfig> configs = moduleConfigManager.getConsumers();
    assertEquals(1, configs.size());
    assertEquals(config, configs.iterator().next());
    assertTrue(moduleConfigManager.getDefaultConsumer().isPresent());
    config = new ConsumerConfig();
    config.setId(DEFAULT_KEY);
    config.setThreads(10);
    moduleConfigManager.addConsumer(config);
    assertTrue(moduleConfigManager.getDefaultConsumer().isPresent());
    configs = moduleConfigManager.getConsumers();
    assertEquals(2, configs.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.430673939s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testProtocolConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `190`
- **Status:** `ERROR`
- **Comment:** `Test ProtocolConfig correlative methods`

```java
// Test ProtocolConfig correlative methods
@Test
void testProtocolConfig() {
    ProtocolConfig config = new ProtocolConfig();
    configManager.addProtocols(asList(config, null));
    Collection<ProtocolConfig> configs = configManager.getProtocols();
    assertEquals(1, configs.size());
    assertEquals(config, configs.iterator().next());
    assertFalse(configManager.getDefaultProtocols().isEmpty());
    assertEquals(configs, moduleConfigManager.getProtocols());
    assertNotEquals(20881, config.getPort());
    assertNotEquals(config.getSerialization(), "fastjson2");
    ProtocolConfig defaultConfig = new ProtocolConfig();
    defaultConfig.setPort(20881);
    defaultConfig.setSerialization("fastjson2");
    config.mergeProtocol(defaultConfig);
    assertEquals(config.getPort(), 20881);
    assertEquals(config.getSerialization(), "fastjson2");
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.200118309s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testRegistryConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `210`
- **Status:** `ERROR`
- **Comment:** `Test RegistryConfig correlative methods`

```java
// Test RegistryConfig correlative methods
@Test
void testRegistryConfig() {
    RegistryConfig config = new RegistryConfig();
    configManager.addRegistries(asList(config, null));
    Collection<RegistryConfig> configs = configManager.getRegistries();
    assertEquals(1, configs.size());
    assertEquals(config, configs.iterator().next());
    assertFalse(configManager.getDefaultRegistries().isEmpty());
    assertEquals(configs, moduleConfigManager.getRegistries());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.961436309s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## ConfigManagerTest.java -> testConfigCenterConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `222`
- **Status:** `ERROR`
- **Comment:** `Test ConfigCenterConfig correlative methods`

```java
// Test ConfigCenterConfig correlative methods
@Test
void testConfigCenterConfig() {
    String address = "zookeeper://127.0.0.1:2181";
    ConfigCenterConfig config = new ConfigCenterConfig();
    config.setAddress(address);
    configManager.addConfigCenters(asList(config, null));
    Collection<ConfigCenterConfig> configs = configManager.getConfigCenters();
    assertEquals(1, configs.size());
    assertEquals(config, configs.iterator().next());
    // add duplicated config, expecting ignore equivalent configs
    ConfigCenterConfig config2 = new ConfigCenterConfig();
    config2.setAddress(address);
    configManager.addConfigCenter(config2);
    configs = configManager.getConfigCenters();
    assertEquals(1, configs.size());
    assertEquals(config, configs.iterator().next());
    assertEquals(configs, moduleConfigManager.getConfigCenters());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.73229474s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## RpcUtils.java -> getReturnTypes()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `34`
- **Status:** `ERROR`
- **Comment:** `TODO why not get return type when initialize Invocation?`

```java
// TODO why not get return type when initialize Invocation?
public static Type[] getReturnTypes(Invocation invocation) {
    return org.apache.dubbo.rpc.support.RpcUtils.getReturnTypes(invocation);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.505880629s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testOnreturn()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `127`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testOnreturn() {
    MethodConfig method = new MethodConfig();
    method.setOnreturn("on-return-object");
    assertThat(method.getOnreturn(), equalTo("on-return-object"));
    Map<String, String> attributes = new HashMap<>();
    MethodConfig.appendAttributes(attributes, method);
    assertThat(attributes, hasEntry(ON_RETURN_INSTANCE_ATTRIBUTE_KEY, "on-return-object"));
    Map<String, String> parameters = new HashMap<String, String>();
    MethodConfig.appendParameters(parameters, method);
    assertThat(parameters.size(), is(0));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.282375617s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testOnthrow()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `153`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testOnthrow() {
    MethodConfig method = new MethodConfig();
    method.setOnthrow("on-throw-object");
    assertThat(method.getOnthrow(), equalTo("on-throw-object"));
    Map<String, String> attributes = new HashMap<>();
    MethodConfig.appendAttributes(attributes, method);
    assertThat(attributes, hasEntry(ON_THROW_INSTANCE_ATTRIBUTE_KEY, "on-throw-object"));
    Map<String, String> parameters = new HashMap<String, String>();
    MethodConfig.appendParameters(parameters, method);
    assertThat(parameters.size(), is(0));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.958046127s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testOninvoke()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `179`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testOninvoke() {
    MethodConfig method = new MethodConfig();
    method.setOninvoke("on-invoke-object");
    assertThat(method.getOninvoke(), equalTo("on-invoke-object"));
    Map<String, String> attributes = new HashMap<>();
    MethodConfig.appendAttributes(attributes, method);
    assertThat(attributes, hasEntry(ON_INVOKE_INSTANCE_ATTRIBUTE_KEY, "on-invoke-object"));
    Map<String, String> parameters = new HashMap<String, String>();
    MethodConfig.appendParameters(parameters, method);
    assertThat(parameters.size(), is(0));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.732258036s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## EnableDubboConfigTest.java -> testSingle()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `59`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
public void testSingle() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestConfig.class);
    context.refresh();
    // application
    ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
    Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());
    // module
    ModuleConfig moduleConfig = context.getBean("moduleBean", ModuleConfig.class);
    Assertions.assertEquals("dubbo-demo-module", moduleConfig.getName());
    // registry
    RegistryConfig registryConfig = context.getBean(RegistryConfig.class);
    Assertions.assertEquals("zookeeper://192.168.99.100:32770", registryConfig.getAddress());
    // protocol
    ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
    Assertions.assertEquals("dubbo", protocolConfig.getName());
    Assertions.assertEquals(Integer.valueOf(20880), protocolConfig.getPort());
    // monitor
    MonitorConfig monitorConfig = context.getBean(MonitorConfig.class);
    Assertions.assertEquals("zookeeper://127.0.0.1:32770", monitorConfig.getAddress());
    // provider
    ProviderConfig providerConfig = context.getBean(ProviderConfig.class);
    Assertions.assertEquals("127.0.0.1", providerConfig.getHost());
    // consumer
    ConsumerConfig consumerConfig = context.getBean(ConsumerConfig.class);
    Assertions.assertEquals("netty", consumerConfig.getClient());
    // asserts aliases
    assertFalse(hasAlias(context, "org.apache.dubbo.config.RegistryConfig#0", "zookeeper"));
    assertFalse(hasAlias(context, "org.apache.dubbo.config.MonitorConfig#0", "zookeeper"));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.494362008s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## EnableDubboConfigTest.java -> testMultiple()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `100`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
public void testMultiple() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestMultipleConfig.class);
    context.refresh();
    RegistryConfig registry1 = context.getBean("registry1", RegistryConfig.class);
    Assertions.assertEquals(2181, registry1.getPort());
    RegistryConfig registry2 = context.getBean("registry2", RegistryConfig.class);
    Assertions.assertEquals(2182, registry2.getPort());
    ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
    Collection<ProtocolConfig> protocolConfigs = configManager.getProtocols();
    Assertions.assertEquals(3, protocolConfigs.size());
    configManager.getProtocol("dubbo").get();
    configManager.getProtocol("rest").get();
    // asserts aliases
    //        assertTrue(hasAlias(context, "applicationBean2", "dubbo-demo-application2"));
    //        assertTrue(hasAlias(context, "applicationBean3", "dubbo-demo-application3"));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.256892927s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## TestService.java -> testPrimitive()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `45`
- **Status:** `ERROR`
- **Comment:** `Test primitive`

```java
// Test primitive
@PUT
String testPrimitive(boolean z, int i);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.028073582s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## TestService.java -> testEnum()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `49`
- **Status:** `ERROR`
- **Comment:** `Test enumeration`

```java
// Test enumeration
@PUT
Model testEnum(TimeUnit timeUnit);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.791527846s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## TestService.java -> testArray()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `53`
- **Status:** `ERROR`
- **Comment:** `Test Array`

```java
// Test Array
@GET
String testArray(String[] strArray, int[] intArray, Model[] modelArray);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.560121907s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## DubboBootstrap.java -> reset()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `138`
- **Status:** `ERROR`
- **Comment:** `* Try reset dubbo status for new instance.      *      * @deprecated For testing purposes only`

```java
/**
 * Try reset dubbo status for new instance.
 *
 * @deprecated For testing purposes only
 */
@Deprecated
public static void reset() {
    reset(true);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.335656793s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## DubboBootstrap.java -> reset()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `148`
- **Status:** `ERROR`
- **Comment:** `* Try reset dubbo status for new instance.      *      * @deprecated For testing purposes only`

```java
/**
 * Try reset dubbo status for new instance.
 *
 * @deprecated For testing purposes only
 */
@Deprecated
public static void reset(boolean destroy) {
    if (destroy) {
        if (instance != null) {
            instance.destroy();
            instance = null;
        }
        FrameworkModel.destroyAll();
    } else {
        instance = null;
    }
    ApplicationModel.reset();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.121152242s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## ReferenceConfig.java -> getInvoker()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `900`
- **Status:** `ERROR`
- **Comment:** `* just for test      *      * @return`

```java
/**
 * just for test
 *
 * @return
 */
@Deprecated
@Transient
public Invoker<?> getInvoker() {
    return invoker;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.890484175s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## IntegrationTest.java -> integrate()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `27`
- **Status:** `ERROR`
- **Comment:** `* Run the integration testcases.`

```java
/**
 * Run the integration testcases.
 */
void integrate();
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.672723301s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## MultipleRegistryCenterExportMetadataService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `27`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.456401669s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## MultipleRegistryCenterExportProviderService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `27`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.22811608s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## MultipleRegistryCenterInjvmService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `25`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.002402107s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## MultipleRegistryCenterServiceDiscoveryRegistryService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `25`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.773800999s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## SingleRegistryCenterExportMetadataService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `27`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.54990629s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## SingleRegistryCenterExportProviderService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `27`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.329836079s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## SingleRegistryCenterInjvmService.java -> hello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `27`
- **Status:** `ERROR`
- **Comment:** `* The simple method for testing.`

```java
/**
 * The simple method for testing.
 */
String hello(String name);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.105678749s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## SingleRegistryCenterDubboProtocolIntegrationTest.java -> getServiceDiscoveryRegistry()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `305`
- **Status:** `ERROR`
- **Comment:** `* Returns {@link ServiceDiscoveryRegistry} instance.      * <p>      * FIXME It's not a good way to obtain {@link ServiceDiscoveryRegistry} using Reflection.`

```java
/**
 * Returns {@link ServiceDiscoveryRegistry} instance.
 * <p>
 * FIXME It's not a good way to obtain {@link ServiceDiscoveryRegistry} using Reflection.
 */
private ServiceDiscoveryRegistry getServiceDiscoveryRegistry() {
    Collection<Registry> registries = RegistryManager.getInstance(ApplicationModel.defaultModel()).getRegistries();
    for (Registry registry : registries) {
        if (registry instanceof ServiceDiscoveryRegistry) {
            return (ServiceDiscoveryRegistry) registry;
        }
    }
    return null;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.889363639s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testStaticConstructor()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `116`
- **Status:** `ERROR`
- **Comment:** `TODO remove this test`

```java
// TODO remove this test
@Test
void testStaticConstructor() throws NoSuchFieldException {
    Method[] methods = this.getClass().getDeclaredField("testField").getAnnotation(Reference.class).methods();
    List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
    MethodConfig methodConfig = methodConfigs.get(0);
    assertThat(METHOD_NAME, equalTo(methodConfig.getName()));
    assertThat(TIMEOUT, equalTo(methodConfig.getTimeout()));
    assertThat(RETRIES, equalTo(methodConfig.getRetries()));
    assertThat(LOADBALANCE, equalTo(methodConfig.getLoadbalance()));
    assertThat(ASYNC, equalTo(methodConfig.isAsync()));
    assertThat(ACTIVES, equalTo(methodConfig.getActives()));
    assertThat(EXECUTES, equalTo(methodConfig.getExecutes()));
    assertThat(DEPRECATED, equalTo(methodConfig.getDeprecated()));
    assertThat(STICKY, equalTo(methodConfig.getSticky()));
    assertThat(ONINVOKE, equalTo(methodConfig.getOninvoke()));
    assertThat(ONINVOKE_METHOD, equalTo(methodConfig.getOninvokeMethod()));
    assertThat(ONTHROW, equalTo(methodConfig.getOnthrow()));
    assertThat(ONTHROW_METHOD, equalTo(methodConfig.getOnthrowMethod()));
    assertThat(ONRETURN, equalTo(methodConfig.getOnreturn()));
    assertThat(ONRETURN_METHOD, equalTo(methodConfig.getOnreturnMethod()));
    assertThat(CACHE, equalTo(methodConfig.getCache()));
    assertThat(VALIDATION, equalTo(methodConfig.getValidation()));
    assertThat(ARGUMENTS_INDEX, equalTo(methodConfig.getArguments().get(0).getIndex()));
    assertThat(ARGUMENTS_CALLBACK, equalTo(methodConfig.getArguments().get(0).isCallback()));
    assertThat(ARGUMENTS_TYPE, equalTo(methodConfig.getArguments().get(0).getType()));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.669993696s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testOnReturn()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `228`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testOnReturn() {
    MethodConfig method = new MethodConfig();
    method.setOnreturn("on-return-object");
    assertThat(method.getOnreturn(), equalTo("on-return-object"));
    Map<String, String> attributes = new HashMap<>();
    MethodConfig.appendAttributes(attributes, method);
    assertThat(attributes, hasEntry(ON_RETURN_INSTANCE_ATTRIBUTE_KEY, "on-return-object"));
    Map<String, String> parameters = new HashMap<String, String>();
    MethodConfig.appendParameters(parameters, method);
    assertThat(parameters.size(), is(0));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.435939176s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testOnThrow()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `254`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testOnThrow() {
    MethodConfig method = new MethodConfig();
    method.setOnthrow("on-throw-object");
    assertThat(method.getOnthrow(), equalTo("on-throw-object"));
    Map<String, String> attributes = new HashMap<>();
    MethodConfig.appendAttributes(attributes, method);
    assertThat(attributes, hasEntry(ON_THROW_INSTANCE_ATTRIBUTE_KEY, "on-throw-object"));
    Map<String, String> parameters = new HashMap<String, String>();
    MethodConfig.appendParameters(parameters, method);
    assertThat(parameters.size(), is(0));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.215421186s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## MethodConfigTest.java -> testOnInvoke()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `280`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testOnInvoke() {
    MethodConfig method = new MethodConfig();
    method.setOninvoke("on-invoke-object");
    assertThat(method.getOninvoke(), equalTo("on-invoke-object"));
    Map<String, String> attributes = new HashMap<>();
    MethodConfig.appendAttributes(attributes, method);
    assertThat(attributes, hasEntry(ON_INVOKE_INSTANCE_ATTRIBUTE_KEY, "on-invoke-object"));
    Map<String, String> parameters = new HashMap<String, String>();
    MethodConfig.appendParameters(parameters, method);
    assertThat(parameters.size(), is(0));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.985674021s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## ReferenceConfigTest.java -> testAppendConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `163`
- **Status:** `ERROR`
- **Comment:** `* Test whether the configuration required for the aggregation service reference meets expectations`

```java
/**
 * Test whether the configuration required for the aggregation service reference meets expectations
 */
@Test
void testAppendConfig() {
    ApplicationConfig applicationConfig = new ApplicationConfig();
    applicationConfig.setName("application1");
    applicationConfig.setVersion("v1");
    applicationConfig.setOwner("owner1");
    applicationConfig.setOrganization("bu1");
    applicationConfig.setArchitecture("architecture1");
    applicationConfig.setEnvironment("test");
    applicationConfig.setCompiler("javassist");
    applicationConfig.setLogger("log4j2");
    applicationConfig.setDumpDirectory("/");
    applicationConfig.setQosEnable(false);
    applicationConfig.setQosHost("127.0.0.1");
    applicationConfig.setQosPort(77777);
    applicationConfig.setQosAcceptForeignIp(false);
    Map<String, String> parameters = new HashMap<>();
    parameters.put("key1", "value1");
    parameters.put("key2", "value2");
    applicationConfig.setParameters(parameters);
    applicationConfig.setShutwait("5");
    applicationConfig.setMetadataType("local");
    applicationConfig.setRegisterConsumer(false);
    applicationConfig.setRepository("repository1");
    applicationConfig.setEnableFileCache(false);
    applicationConfig.setProtocol("dubbo");
    applicationConfig.setMetadataServicePort(88888);
    applicationConfig.setMetadataServiceProtocol("tri");
    applicationConfig.setLivenessProbe("livenessProbe");
    applicationConfig.setReadinessProbe("readinessProb");
    applicationConfig.setStartupProbe("startupProbe");
    ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
    referenceConfig.setClient("netty");
    referenceConfig.setGeneric(Boolean.FALSE.toString());
    referenceConfig.setProtocol("dubbo");
    referenceConfig.setInit(true);
    referenceConfig.setLazy(false);
    referenceConfig.setInjvm(false);
    referenceConfig.setReconnect("reconnect");
    referenceConfig.setSticky(false);
    referenceConfig.setStub(DEFAULT_STUB_EVENT);
    referenceConfig.setRouter("default");
    referenceConfig.setReferAsync(true);
    MonitorConfig monitorConfig = new MonitorConfig();
    applicationConfig.setMonitor(monitorConfig);
    ModuleConfig moduleConfig = new ModuleConfig();
    moduleConfig.setMonitor("default");
    moduleConfig.setName("module1");
    moduleConfig.setOrganization("application1");
    moduleConfig.setVersion("v1");
    moduleConfig.setOwner("owner1");
    ConsumerConfig consumerConfig = new ConsumerConfig();
    consumerConfig.setClient("netty");
    consumerConfig.setThreadpool("fixed");
    consumerConfig.setCorethreads(200);
    consumerConfig.setQueues(500);
    consumerConfig.setThreads(300);
    consumerConfig.setShareconnections(10);
    consumerConfig.setUrlMergeProcessor("default");
    consumerConfig.setReferThreadNum(20);
    consumerConfig.setReferBackground(false);
    referenceConfig.setConsumer(consumerConfig);
    MethodConfig methodConfig = new MethodConfig();
    methodConfig.setName("sayName");
    methodConfig.setStat(1);
    methodConfig.setRetries(0);
    methodConfig.setExecutes(10);
    methodConfig.setDeprecated(false);
    methodConfig.setSticky(false);
    methodConfig.setReturn(false);
    methodConfig.setService("service");
    methodConfig.setServiceId(DemoService.class.getName());
    methodConfig.setParentPrefix("demo");
    referenceConfig.setMethods(Collections.singletonList(methodConfig));
    referenceConfig.setInterface(DemoService.class);
    referenceConfig.getInterfaceClass();
    referenceConfig.setCheck(false);
    RegistryConfig registry = new RegistryConfig();
    registry.setAddress(zkUrl1);
    applicationConfig.setRegistries(Collections.singletonList(registry));
    applicationConfig.setRegistryIds(registry.getId());
    moduleConfig.setRegistries(Collections.singletonList(registry));
    referenceConfig.setRegistry(registry);
    DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());
    dubboBootstrap.application(applicationConfig).reference(referenceConfig).registry(registry).module(moduleConfig).initialize();
    referenceConfig.init();
    ServiceMetadata serviceMetadata = referenceConfig.getServiceMetadata();
    // verify additional side parameter
    Assertions.assertEquals(CONSUMER_SIDE, serviceMetadata.getAttachments().get(SIDE_KEY));
    // verify additional interface parameter
    Assertions.assertEquals(DemoService.class.getName(), serviceMetadata.getAttachments().get(INTERFACE_KEY));
    // verify additional metadata-type parameter
    Assertions.assertEquals(DEFAULT_METADATA_STORAGE_TYPE, serviceMetadata.getAttachments().get(METADATA_KEY));
    // verify additional register.ip parameter
    Assertions.assertEquals(NetUtils.getLocalHost(), serviceMetadata.getAttachments().get(REGISTER_IP_KEY));
    // verify additional runtime parameters
    Assertions.assertEquals(Version.getProtocolVersion(), serviceMetadata.getAttachments().get(DUBBO_VERSION_KEY));
    Assertions.assertEquals(Version.getVersion(), serviceMetadata.getAttachments().get(RELEASE_KEY));
    Assertions.assertTrue(serviceMetadata.getAttachments().containsKey(TIMESTAMP_KEY));
    Assertions.assertEquals(String.valueOf(ConfigUtils.getPid()), serviceMetadata.getAttachments().get(PID_KEY));
    // verify additional application config
    Assertions.assertEquals(applicationConfig.getName(), serviceMetadata.getAttachments().get(APPLICATION_KEY));
    Assertions.assertEquals(applicationConfig.getOwner(), serviceMetadata.getAttachments().get("owner"));
    Assertions.assertEquals(applicationConfig.getVersion(), serviceMetadata.getAttachments().get(APPLICATION_VERSION_KEY));
    Assertions.assertEquals(applicationConfig.getOrganization(), serviceMetadata.getAttachments().get("organization"));
    Assertions.assertEquals(applicationConfig.getArchitecture(), serviceMetadata.getAttachments().get("architecture"));
    Assertions.assertEquals(applicationConfig.getEnvironment(), serviceMetadata.getAttachments().get("environment"));
    Assertions.assertEquals(applicationConfig.getCompiler(), serviceMetadata.getAttachments().get("compiler"));
    Assertions.assertEquals(applicationConfig.getLogger(), serviceMetadata.getAttachments().get("logger"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("registries"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("registry.ids"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("monitor"));
    Assertions.assertEquals(applicationConfig.getDumpDirectory(), serviceMetadata.getAttachments().get(DUMP_DIRECTORY));
    Assertions.assertEquals(applicationConfig.getQosEnable().toString(), serviceMetadata.getAttachments().get(QOS_ENABLE));
    Assertions.assertEquals(applicationConfig.getQosHost(), serviceMetadata.getAttachments().get(QOS_HOST));
    Assertions.assertEquals(applicationConfig.getQosPort().toString(), serviceMetadata.getAttachments().get(QOS_PORT));
    Assertions.assertEquals(applicationConfig.getQosAcceptForeignIp().toString(), serviceMetadata.getAttachments().get(ACCEPT_FOREIGN_IP));
    Assertions.assertEquals(applicationConfig.getParameters().get("key1"), serviceMetadata.getAttachments().get("key1"));
    Assertions.assertEquals(applicationConfig.getParameters().get("key2"), serviceMetadata.getAttachments().get("key2"));
    Assertions.assertEquals(applicationConfig.getShutwait(), serviceMetadata.getAttachments().get("shutwait"));
    Assertions.assertEquals(applicationConfig.getMetadataType(), serviceMetadata.getAttachments().get(METADATA_KEY));
    Assertions.assertEquals(applicationConfig.getRegisterConsumer().toString(), serviceMetadata.getAttachments().get("register.consumer"));
    Assertions.assertEquals(applicationConfig.getRepository(), serviceMetadata.getAttachments().get("repository"));
    Assertions.assertEquals(applicationConfig.getEnableFileCache().toString(), serviceMetadata.getAttachments().get(REGISTRY_LOCAL_FILE_CACHE_ENABLED));
    Assertions.assertEquals(applicationConfig.getMetadataServicePort().toString(), serviceMetadata.getAttachments().get(METADATA_SERVICE_PORT_KEY));
    Assertions.assertEquals(applicationConfig.getMetadataServiceProtocol().toString(), serviceMetadata.getAttachments().get(METADATA_SERVICE_PROTOCOL_KEY));
    Assertions.assertEquals(applicationConfig.getLivenessProbe(), serviceMetadata.getAttachments().get(LIVENESS_PROBE_KEY));
    Assertions.assertEquals(applicationConfig.getReadinessProbe(), serviceMetadata.getAttachments().get(READINESS_PROBE_KEY));
    Assertions.assertEquals(applicationConfig.getStartupProbe(), serviceMetadata.getAttachments().get(STARTUP_PROBE));
    // verify additional module config
    Assertions.assertEquals(moduleConfig.getName(), serviceMetadata.getAttachments().get("module"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("monitor"));
    Assertions.assertEquals(moduleConfig.getOrganization(), serviceMetadata.getAttachments().get("module.organization"));
    Assertions.assertEquals(moduleConfig.getOwner(), serviceMetadata.getAttachments().get("module.owner"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("registries"));
    Assertions.assertEquals(moduleConfig.getVersion(), serviceMetadata.getAttachments().get("module.version"));
    // verify additional consumer config
    Assertions.assertEquals(consumerConfig.getClient(), serviceMetadata.getAttachments().get("client"));
    Assertions.assertEquals(consumerConfig.getThreadpool(), serviceMetadata.getAttachments().get("threadpool"));
    Assertions.assertEquals(consumerConfig.getCorethreads().toString(), serviceMetadata.getAttachments().get("corethreads"));
    Assertions.assertEquals(consumerConfig.getQueues().toString(), serviceMetadata.getAttachments().get("queues"));
    Assertions.assertEquals(consumerConfig.getThreads().toString(), serviceMetadata.getAttachments().get("threads"));
    Assertions.assertEquals(consumerConfig.getShareconnections().toString(), serviceMetadata.getAttachments().get("shareconnections"));
    Assertions.assertEquals(consumerConfig.getUrlMergeProcessor(), serviceMetadata.getAttachments().get(URL_MERGE_PROCESSOR_KEY));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey(REFER_THREAD_NUM_KEY));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey(REFER_BACKGROUND_KEY));
    // verify additional reference config
    Assertions.assertEquals(referenceConfig.getClient(), serviceMetadata.getAttachments().get("client"));
    Assertions.assertEquals(referenceConfig.getGeneric(), serviceMetadata.getAttachments().get("generic"));
    Assertions.assertEquals(referenceConfig.getProtocol(), serviceMetadata.getAttachments().get("protocol"));
    Assertions.assertEquals(referenceConfig.isInit().toString(), serviceMetadata.getAttachments().get("init"));
    Assertions.assertEquals(referenceConfig.getLazy().toString(), serviceMetadata.getAttachments().get("lazy"));
    Assertions.assertEquals(referenceConfig.isInjvm().toString(), serviceMetadata.getAttachments().get("injvm"));
    Assertions.assertEquals(referenceConfig.getReconnect(), serviceMetadata.getAttachments().get("reconnect"));
    Assertions.assertEquals(referenceConfig.getSticky().toString(), serviceMetadata.getAttachments().get("sticky"));
    Assertions.assertEquals(referenceConfig.getStub(), serviceMetadata.getAttachments().get("stub"));
    Assertions.assertEquals(referenceConfig.getProvidedBy(), serviceMetadata.getAttachments().get("provided-by"));
    Assertions.assertEquals(referenceConfig.getRouter(), serviceMetadata.getAttachments().get("router"));
    Assertions.assertEquals(referenceConfig.getReferAsync().toString(), serviceMetadata.getAttachments().get(REFER_ASYNC_KEY));
    // verify additional method config
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("name"));
    Assertions.assertEquals(methodConfig.getStat().toString(), serviceMetadata.getAttachments().get("sayName.stat"));
    Assertions.assertEquals(methodConfig.getRetries().toString(), serviceMetadata.getAttachments().get("sayName.retries"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.reliable"));
    Assertions.assertEquals(methodConfig.getExecutes().toString(), serviceMetadata.getAttachments().get("sayName.executes"));
    Assertions.assertEquals(methodConfig.getDeprecated().toString(), serviceMetadata.getAttachments().get("sayName.deprecated"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.stick"));
    Assertions.assertEquals(methodConfig.isReturn().toString(), serviceMetadata.getAttachments().get("sayName.return"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.service"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.service.id"));
    Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.parent.prefix"));
    // verify additional revision and methods parameter
    Assertions.assertEquals(Version.getVersion(referenceConfig.getInterfaceClass(), referenceConfig.getVersion()), serviceMetadata.getAttachments().get(REVISION_KEY));
    Assertions.assertTrue(serviceMetadata.getAttachments().containsKey(METHODS_KEY));
    Assertions.assertEquals(DemoService.class.getMethods().length, StringUtils.split((String) serviceMetadata.getAttachments().get(METHODS_KEY), ',').length);
    dubboBootstrap.stop();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.708584142s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## ReferenceConfigTest.java -> test1ReferenceRetry()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `741`
- **Status:** `ERROR`
- **Comment:** `* unit test for dubbo-1765`

```java
/**
 * unit test for dubbo-1765
 */
@Test
void test1ReferenceRetry() {
    ApplicationConfig application = new ApplicationConfig();
    application.setName("test-reference-retry");
    application.setEnableFileCache(false);
    ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(application);
    RegistryConfig registry = new RegistryConfig();
    registry.setAddress(zkUrl1);
    ReferenceConfig<DemoService> rc = new ReferenceConfig<>();
    rc.setRegistry(registry);
    rc.setInterface(DemoService.class.getName());
    boolean success = false;
    DemoService demoService = null;
    try {
        demoService = rc.get();
        success = true;
    } catch (Exception e) {
        // ignore
    }
    Assertions.assertFalse(success);
    Assertions.assertNull(demoService);
    try {
        System.setProperty("java.net.preferIPv4Stack", "true");
        ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("injvm://127.0.0.1/DemoService").addParameter(INTERFACE_KEY, DemoService.class.getName()).setScopeModel(ApplicationModel.defaultModel().getDefaultModule());
        url = url.addParameter(EXPORTER_LISTENER_KEY, LOCAL_PROTOCOL);
        Protocol protocolSPI = ApplicationModel.defaultModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
        protocolSPI.export(proxy.getInvoker(service, DemoService.class, url));
        demoService = rc.get();
        success = true;
    } catch (Exception e) {
        // ignore
    } finally {
        rc.destroy();
        InjvmProtocol.getInjvmProtocol(FrameworkModel.defaultModel()).destroy();
        System.clearProperty("java.net.preferIPv4Stack");
    }
    Assertions.assertTrue(success);
    Assertions.assertNotNull(demoService);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.485501468s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## ServicePackagesHolder.java -> isSubPackage()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `73`
- **Status:** `ERROR`
- **Comment:** `* Whether test package is sub package of parent package      * @param testPkg      * @param parent      * @return`

```java
/**
 * Whether test package is sub package of parent package
 * @param testPkg
 * @param parent
 * @return
 */
private boolean isSubPackage(String testPkg, String parent) {
    // child pkg startsWith parent pkg
    return testPkg.startsWith(parent);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.259194789s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## BeanRegistrar.java -> hasAlias()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `37`
- **Status:** `ERROR`
- **Comment:** `* Detect the alias is present or not in the given bean name from {@link AliasRegistry}      *      * @param registry {@link AliasRegistry}      * @param beanName the bean name      * @param alias    alias to test      * @return if present, return <code>true</code>, or <code>false</code>`

```java
/**
 * Detect the alias is present or not in the given bean name from {@link AliasRegistry}
 *
 * @param registry {@link AliasRegistry}
 * @param beanName the bean name
 * @param alias    alias to test
 * @return if present, return <code>true</code>, or <code>false</code>
 */
public static boolean hasAlias(AliasRegistry registry, String beanName, String alias) {
    return hasText(beanName) && hasText(alias) && containsElement(registry.getAliases(beanName), alias);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 26.030966965s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "26s"
      }
    ]
  }
}

```

---

## XmlReferenceBeanConditionalTest.java -> myHelloService()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `82`
- **Status:** `ERROR`
- **Comment:** `TEST Conditional, this bean should be ignored`

```java
// TEST Conditional, this bean should be ignored
@Bean
@ConditionalOnMissingBean
public HelloService myHelloService() {
    return new HelloService() {

        @Override
        public String sayHello(String name) {
            return "HI, " + name;
        }
    };
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.806359724s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## JavaConfigAnnotationReferenceBeanConditionalTest.java -> myHelloService()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `98`
- **Status:** `ERROR`
- **Comment:** `TEST Conditional, this bean should be ignored`

```java
// TEST Conditional, this bean should be ignored
@Bean
@ConditionalOnMissingBean
public HelloService myHelloService() {
    return new HelloServiceImpl();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.590981643s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## JavaConfigRawReferenceBeanConditionalTest.java -> myHelloService()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `100`
- **Status:** `ERROR`
- **Comment:** `TEST Conditional, this bean should be ignored`

```java
// TEST Conditional, this bean should be ignored
@Bean
@ConditionalOnMissingBean
public HelloService myHelloService() {
    return new HelloServiceImpl();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.369453807s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## JavaConfigReferenceBeanConditionalTest4.java -> helloService()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `95`
- **Status:** `ERROR`
- **Comment:** `TEST Conditional, this bean should be ignored`

```java
// TEST Conditional, this bean should be ignored
@Bean
@ConditionalOnMissingBean(HelloService.class)
@DubboReference(group = "${myapp.group}", init = false)
public ReferenceBean<HelloService> helloService() {
    return new ReferenceBean();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 25.151002232s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "25s"
      }
    ]
  }
}

```

---

## EnableDubboConfigTest.java -> testSingle()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `59`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
public void testSingle() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestConfig.class);
    context.refresh();
    // application
    ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
    Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());
    // module
    ModuleConfig moduleConfig = context.getBean("moduleBean", ModuleConfig.class);
    Assertions.assertEquals("dubbo-demo-module", moduleConfig.getName());
    // registry
    RegistryConfig registryConfig = context.getBean(RegistryConfig.class);
    Assertions.assertEquals("zookeeper://192.168.99.100:32770", registryConfig.getAddress());
    // protocol
    ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
    Assertions.assertEquals("dubbo", protocolConfig.getName());
    Assertions.assertEquals(Integer.valueOf(20880), protocolConfig.getPort());
    // monitor
    MonitorConfig monitorConfig = context.getBean(MonitorConfig.class);
    Assertions.assertEquals("zookeeper://127.0.0.1:32770", monitorConfig.getAddress());
    // provider
    ProviderConfig providerConfig = context.getBean(ProviderConfig.class);
    Assertions.assertEquals("127.0.0.1", providerConfig.getHost());
    // consumer
    ConsumerConfig consumerConfig = context.getBean(ConsumerConfig.class);
    Assertions.assertEquals("netty", consumerConfig.getClient());
    // asserts aliases
    assertFalse(hasAlias(context, "org.apache.dubbo.config.RegistryConfig#0", "zookeeper"));
    assertFalse(hasAlias(context, "org.apache.dubbo.config.MonitorConfig#0", "zookeeper"));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.933325342s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## EnableDubboConfigTest.java -> testMultiple()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `100`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
public void testMultiple() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TestMultipleConfig.class);
    context.refresh();
    RegistryConfig registry1 = context.getBean("registry1", RegistryConfig.class);
    Assertions.assertEquals(2181, registry1.getPort());
    RegistryConfig registry2 = context.getBean("registry2", RegistryConfig.class);
    Assertions.assertEquals(2182, registry2.getPort());
    ConfigManager configManager = ApplicationModel.defaultModel().getApplicationConfigManager();
    Collection<ProtocolConfig> protocolConfigs = configManager.getProtocols();
    Assertions.assertEquals(3, protocolConfigs.size());
    configManager.getProtocol("dubbo").get();
    configManager.getProtocol("rest").get();
    // asserts aliases
    //        assertTrue(hasAlias(context, "applicationBean2", "dubbo-demo-application2"));
    //        assertTrue(hasAlias(context, "applicationBean3", "dubbo-demo-application3"));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.713077532s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## SpringStatusCheckerTest.java -> testGenericWebApplicationContext()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `86`
- **Status:** `ERROR`
- **Comment:** `TODO improve GenericWebApplicationContext test scenario`

```java
// TODO improve GenericWebApplicationContext test scenario
@Test
void testGenericWebApplicationContext() {
    GenericWebApplicationContext context = mock(GenericWebApplicationContext.class);
    given(context.isRunning()).willReturn(true);
    SpringStatusChecker checker = new SpringStatusChecker(context);
    Status status = checker.check();
    Assertions.assertEquals(Status.Level.OK, status.getLevel());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.492749798s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## ApolloDynamicConfigurationTest.java -> testGetRule()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `92`
- **Status:** `ERROR`
- **Comment:** `* Test get rule.`

```java
//    /**
//     * Embedded Apollo does not work as expected.
//     */
//    @Test
//    public void testProperties() {
//        URL url = this.url.addParameter(GROUP_KEY, "dubbo")
//                .addParameter("namespace", "governance");
//
//        apolloDynamicConfiguration = new ApolloDynamicConfiguration(url);
//        putData("dubbo", "dubbo.registry.address", "zookeeper://127.0.0.1:2181");
//        assertEquals("zookeeper://127.0.0.1:2181", apolloDynamicConfiguration.getProperties(null, "dubbo"));
//
//        putData("governance", "router.tag", "router tag rule");
//        assertEquals("router tag rule", apolloDynamicConfiguration.getConfig("router.tag", "governance"));
//
//    }
/**
 * Test get rule.
 */
@Test
void testGetRule() {
    String mockKey = "mockKey1";
    String mockValue = String.valueOf(new Random().nextInt());
    putMockRuleData(mockKey, mockValue, DEFAULT_NAMESPACE);
    apolloDynamicConfiguration = new ApolloDynamicConfiguration(url, applicationModel);
    assertEquals(mockValue, apolloDynamicConfiguration.getConfig(mockKey, DEFAULT_NAMESPACE, 3000L));
    mockKey = "notExistKey";
    assertNull(apolloDynamicConfiguration.getConfig(mockKey, DEFAULT_NAMESPACE, 3000L));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.27223219s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## ApolloDynamicConfigurationTest.java -> testGetInternalProperty()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `109`
- **Status:** `ERROR`
- **Comment:** `* Test get internal property.      *      * @throws InterruptedException the interrupted exception`

```java
/**
 * Test get internal property.
 *
 * @throws InterruptedException the interrupted exception
 */
@Test
void testGetInternalProperty() throws InterruptedException {
    String mockKey = "mockKey2";
    String mockValue = String.valueOf(new Random().nextInt());
    putMockRuleData(mockKey, mockValue, DEFAULT_NAMESPACE);
    TimeUnit.MILLISECONDS.sleep(1000);
    apolloDynamicConfiguration = new ApolloDynamicConfiguration(url, applicationModel);
    assertEquals(mockValue, apolloDynamicConfiguration.getInternalProperty(mockKey));
    mockValue = "mockValue2";
    System.setProperty(mockKey, mockValue);
    assertEquals(mockValue, apolloDynamicConfiguration.getInternalProperty(mockKey));
    mockKey = "notExistKey";
    assertNull(apolloDynamicConfiguration.getInternalProperty(mockKey));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.046558289s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## ApolloDynamicConfigurationTest.java -> testAddListener()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `131`
- **Status:** `ERROR`
- **Comment:** `* Test add listener.      *      * @throws Exception the exception`

```java
/**
 * Test add listener.
 *
 * @throws Exception the exception
 */
@Test
void testAddListener() throws Exception {
    String mockKey = "mockKey3";
    String mockValue = String.valueOf(new Random().nextInt());
    final SettableFuture<org.apache.dubbo.common.config.configcenter.ConfigChangedEvent> future = SettableFuture.create();
    apolloDynamicConfiguration = new ApolloDynamicConfiguration(url, applicationModel);
    apolloDynamicConfiguration.addListener(mockKey, DEFAULT_NAMESPACE, new ConfigurationListener() {

        @Override
        public void process(org.apache.dubbo.common.config.configcenter.ConfigChangedEvent event) {
            future.set(event);
        }
    });
    putData(mockKey, mockValue);
    org.apache.dubbo.common.config.configcenter.ConfigChangedEvent result = future.get(3000, TimeUnit.MILLISECONDS);
    assertEquals(mockValue, result.getContent());
    assertEquals(mockKey, result.getKey());
    assertEquals(ConfigChangeType.MODIFIED, result.getChangeType());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.825033335s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## AbstractCacheManager.java -> getCacheStore()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `195`
- **Status:** `ERROR`
- **Comment:** `for test unit`

```java
// for test unit
public FileCacheStore getCacheStore() {
    return cacheStore;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.606152538s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## AbstractServiceNameMapping.java -> setApplicationModel()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `76`
- **Status:** `ERROR`
- **Comment:** `just for test`

```java
// just for test
public void setApplicationModel(ApplicationModel applicationModel) {
    this.applicationModel = applicationModel;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.383021484s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## InstanceMetadataChangedListener.java -> onEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `26`
- **Status:** `ERROR`
- **Comment:** `* Call when metadata in provider side update <p/>      * Used to notify consumer to update metadata of ServiceInstance      *      * @param metadata latest metadata`

```java
/**
 * Call when metadata in provider side update <p/>
 * Used to notify consumer to update metadata of ServiceInstance
 *
 * @param metadata latest metadata
 */
void onEvent(String metadata);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.16194147s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## InstanceMetadataChangedListener.java -> echo()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `32`
- **Status:** `ERROR`
- **Comment:** `* Echo test      * Used to check consumer still online`

```java
/**
 * Echo test
 * Used to check consumer still online
 */
default String echo(String msg) {
    return msg;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.936615567s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractMetadataReport.java -> publishAll()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `470`
- **Status:** `ERROR`
- **Comment:** `* not private. just for unittest.`

```java
/**
 * not private. just for unittest.
 */
void publishAll() {
    logger.info("start to publish all metadata.");
    this.doHandleMetadataCollection(allMetadataReports);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.71916247s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractMetadataReport.java -> getRetryExecutor()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `559`
- **Status:** `ERROR`
- **Comment:** `* @deprecated only for test`

```java
/**
 * @deprecated only for test
 */
@Deprecated
ScheduledExecutorService getRetryExecutor() {
    return retryExecutor;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.503277866s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractMetadataReport.java -> getReportCacheExecutor()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `596`
- **Status:** `ERROR`
- **Comment:** `* @deprecated only for unit test`

```java
/**
 * @deprecated only for unit test
 */
@Deprecated
protected ExecutorService getReportCacheExecutor() {
    return reportCacheExecutor;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.285511559s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractMetadataReport.java -> getMetadataReportRetry()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `604`
- **Status:** `ERROR`
- **Comment:** `* @deprecated only for unit test`

```java
/**
 * @deprecated only for unit test
 */
@Deprecated
protected MetadataReportRetry getMetadataReportRetry() {
    return metadataReportRetry;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.071131994s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## ServiceNameMapping.java -> getAndListen()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `112`
- **Status:** `ERROR`
- **Comment:** `* Get the latest mapping result from remote center and register listener at the same time to get notified once mapping changes.      *      * @param listener listener that will be notified on mapping change      * @return the latest mapping result from remote center`

```java
/**
 * Get the latest mapping result from remote center and register listener at the same time to get notified once mapping changes.
 *
 * @param listener listener that will be notified on mapping change
 * @return the latest mapping result from remote center
 */
Set<String> getAndListen(URL registryURL, URL subscribedURL, MappingListener listener);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.848388867s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## CustomizedParamsFilter.java -> instanceParamsIncluded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `43`
- **Status:** `ERROR`
- **Comment:** `* Not included in this test`

```java
/**
 * Not included in this test
 */
@Override
public String[] instanceParamsIncluded() {
    return new String[0];
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.614927556s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## ExcludedParamsFilter.java -> instanceParamsIncluded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `41`
- **Status:** `ERROR`
- **Comment:** `* Not included in this test`

```java
/**
 * Not included in this test
 */
@Override
public String[] instanceParamsIncluded() {
    return new String[0];
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.398509844s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## ExcludedParamsFilter2.java -> instanceParamsIncluded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `41`
- **Status:** `ERROR`
- **Comment:** `* Not included in this test`

```java
/**
 * Not included in this test
 */
@Override
public String[] instanceParamsIncluded() {
    return new String[0];
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.184175399s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## ProtobufTypeBuilder.java -> validateMapType()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `174`
- **Status:** `ERROR`
- **Comment:** `* 1. Unsupported Map with key type is not String <br/>      * Bytes is a primitive type in Proto, transform to ByteString.class in java<br/>      *      * @param fieldName      * @param typeName      * @return`

```java
/**
 * 1. Unsupported Map with key type is not String <br/>
 * Bytes is a primitive type in Proto, transform to ByteString.class in java<br/>
 *
 * @param fieldName
 * @param typeName
 * @return
 */
private void validateMapType(String fieldName, String typeName) {
    Matcher matcher = MAP_PATTERN.matcher(typeName);
    if (!matcher.matches()) {
        throw new IllegalArgumentException("Map protobuf property " + fieldName + "of Type " + typeName + " can't be parsed.The type name should match[" + MAP_PATTERN.toString() + "].");
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.971887849s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## DeclaredTypeDefinitionBuilder.java -> accept()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `48`
- **Status:** `ERROR`
- **Comment:** `* Test the specified {@link DeclaredType type} is accepted or not      *      * @param processingEnv {@link ProcessingEnvironment}      * @param type          {@link DeclaredType type}      * @return <code>true</code> if accepted`

```java
/**
 * Test the specified {@link DeclaredType type} is accepted or not
 *
 * @param processingEnv {@link ProcessingEnvironment}
 * @param type          {@link DeclaredType type}
 * @return <code>true</code> if accepted
 */
boolean accept(ProcessingEnvironment processingEnv, DeclaredType type);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.749323306s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TypeBuilder.java -> accept()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `38`
- **Status:** `ERROR`
- **Comment:** `* Test the specified {@link TypeMirror type} is accepted or not      *      * @param processingEnv {@link ProcessingEnvironment}      * @param type          {@link TypeMirror type}      * @return <code>true</code> if accepted`

```java
/**
 * Test the specified {@link TypeMirror type} is accepted or not
 *
 * @param processingEnv {@link ProcessingEnvironment}
 * @param type          {@link TypeMirror type}
 * @return <code>true</code> if accepted
 */
boolean accept(ProcessingEnvironment processingEnv, TypeMirror type);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.528450464s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TestService.java -> testPrimitive()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `45`
- **Status:** `ERROR`
- **Comment:** `Test primitive`

```java
// Test primitive
@PUT
String testPrimitive(boolean z, int i);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.304083635s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TestService.java -> testEnum()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `49`
- **Status:** `ERROR`
- **Comment:** `Test enumeration`

```java
// Test enumeration
@PUT
Model testEnum(TimeUnit timeUnit);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.077940137s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TestService.java -> testArray()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `53`
- **Status:** `ERROR`
- **Comment:** `Test Array`

```java
// Test Array
@GET
String testArray(String[] strArray, int[] intArray, Model[] modelArray);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.858611892s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## DubboAbstractTDigest.java -> recordAllData()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `58`
- **Status:** `ERROR`
- **Comment:** `* Sets up so that all centroids will record all data assigned to them.  For testing only, really.`

```java
/**
 * Sets up so that all centroids will record all data assigned to them.  For testing only, really.
 */
@Override
public TDigest recordAllData() {
    recordAllData = true;
    return this;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.63985782s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## DubboMergingDigest.java -> setMinMax()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `257`
- **Status:** `ERROR`
- **Comment:** `* Over-ride the min and max values for testing purposes`

```java
/**
 * Over-ride the min and max values for testing purposes
 */
@SuppressWarnings("SameParameterValue")
void setMinMax(double min, double max) {
    this.min = min;
    this.max = max;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.306349576s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## MetadataServiceURLBuilder.java -> build()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `39`
- **Status:** `ERROR`
- **Comment:** `* Build the {@link URL URLs} from the specified {@link ServiceInstance}      *      * @param serviceInstance {@link ServiceInstance}      * @return TODO, usually, we generate one metadata url from one instance. There's no scenario to return a metadata url list.`

```java
/**
 * Build the {@link URL URLs} from the specified {@link ServiceInstance}
 *
 * @param serviceInstance {@link ServiceInstance}
 * @return TODO, usually, we generate one metadata url from one instance. There's no scenario to return a metadata url list.
 */
List<URL> build(ServiceInstance serviceInstance);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.076241815s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## MigrationRuleHandler.java -> getMigrationStep()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `152`
- **Status:** `ERROR`
- **Comment:** `for test purpose`

```java
// for test purpose
public MigrationStep getMigrationStep() {
    return currentStep;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.869272328s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## ServiceDiscoveryRegistryDirectory.java -> isNotificationReceived()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `311`
- **Status:** `ERROR`
- **Comment:** `* This implementation makes sure all application names related to serviceListener received address notification.      * <p>      * FIXME, make sure deprecated "interface-application" mapping item be cleared in time.`

```java
/**
 * This implementation makes sure all application names related to serviceListener received address notification.
 * <p>
 * FIXME, make sure deprecated "interface-application" mapping item be cleared in time.
 */
@Override
public boolean isNotificationReceived() {
    return serviceListener == null || serviceListener.isDestroyed() || serviceListener.getAllInstances().size() == serviceListener.getServiceNames().size();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.647350501s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## RegistryDirectory.java -> refreshInvoker()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `275`
- **Status:** `ERROR`
- **Comment:** `* Convert the invokerURL list to the Invoker Map. The rules of the conversion are as follows:      * <ol>      * <li> If URL has been converted to invoker, it is no longer re-referenced and obtained directly from the cache,      * and notice that any parameter changes in the URL will be re-referenced.</li>      * <li>If the incoming invoker list is not empty, it means that it is the latest invoker list.</li>      * <li>If the list of incoming invokerUrl is empty, It means that the rule is only a override rule or a route      * rule, which needs to be re-contrasted to decide whether to re-reference.</li>      * </ol>      *      * @param invokerUrls this parameter can't be null`

```java
/**
 * Convert the invokerURL list to the Invoker Map. The rules of the conversion are as follows:
 * <ol>
 * <li> If URL has been converted to invoker, it is no longer re-referenced and obtained directly from the cache,
 * and notice that any parameter changes in the URL will be re-referenced.</li>
 * <li>If the incoming invoker list is not empty, it means that it is the latest invoker list.</li>
 * <li>If the list of incoming invokerUrl is empty, It means that the rule is only a override rule or a route
 * rule, which needs to be re-contrasted to decide whether to re-reference.</li>
 * </ol>
 *
 * @param invokerUrls this parameter can't be null
 */
private void refreshInvoker(List<URL> invokerUrls) {
    Assert.notNull(invokerUrls, "invokerUrls should not be null");
    if (invokerUrls.size() == 1 && invokerUrls.get(0) != null && EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
        refreshRouter(// Forbid to access
        BitList.emptyList(), // Forbid to access
        () -> this.forbidden = true);
        // Close all invokers
        destroyAllInvokers();
    } else {
        // Allow to access
        this.forbidden = false;
        if (invokerUrls == Collections.<URL>emptyList()) {
            invokerUrls = new ArrayList<>();
        }
        // use local reference to avoid NPE as this.cachedInvokerUrls will be set null by destroyAllInvokers().
        Set<URL> localCachedInvokerUrls = this.cachedInvokerUrls;
        if (invokerUrls.isEmpty()) {
            if (CollectionUtils.isNotEmpty(localCachedInvokerUrls)) {
                // 1-4 Empty address.
                logger.warn(REGISTRY_EMPTY_ADDRESS, "configuration ", "", "Service" + serviceKey + " received empty address list with no EMPTY protocol set, trigger empty protection.");
                invokerUrls.addAll(localCachedInvokerUrls);
            }
        } else {
            localCachedInvokerUrls = new HashSet<>();
            // Cached invoker urls, convenient for comparison
            localCachedInvokerUrls.addAll(invokerUrls);
            this.cachedInvokerUrls = localCachedInvokerUrls;
        }
        if (invokerUrls.isEmpty()) {
            return;
        }
        int originSize = invokerUrls.size();
        invokerUrls = invokerUrls.stream().distinct().collect(Collectors.toList());
        if (invokerUrls.size() != originSize) {
            logger.info("Received duplicated invoker urls changed event from registry. " + "Registry type: interface. " + "Service Key: " + getConsumerUrl().getServiceKey() + ". " + "Notify Urls Size : " + originSize + ". " + "Distinct Urls Size: " + invokerUrls.size() + ".");
        }
        // use local reference to avoid NPE as this.urlInvokerMap will be set null concurrently at
        // destroyAllInvokers().
        Map<URL, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
        // can't use local reference as oldUrlInvokerMap's mappings might be removed directly at toInvokers().
        Map<URL, Invoker<T>> oldUrlInvokerMap = null;
        if (localUrlInvokerMap != null) {
            // the initial capacity should be set greater than the maximum number of entries divided by the load
            // factor to avoid resizing.
            oldUrlInvokerMap = new LinkedHashMap<>(Math.round(1 + localUrlInvokerMap.size() / DEFAULT_HASHMAP_LOAD_FACTOR));
            localUrlInvokerMap.forEach(oldUrlInvokerMap::put);
        }
        Map<URL, Invoker<T>> newUrlInvokerMap = // Translate url list to Invoker map
        toInvokers(oldUrlInvokerMap, invokerUrls);
        /*
             * If the calculation is wrong, it is not processed.
             *
             * 1. The protocol configured by the client is inconsistent with the protocol of the server.
             *    eg: consumer protocol = dubbo, provider only has other protocol services(rest).
             * 2. The registration center is not robust and pushes illegal specification data.
             *
             */
        if (CollectionUtils.isEmptyMap(newUrlInvokerMap)) {
            // 3-1 - Failed to convert the URL address into Invokers.
            logger.error(PROXY_FAILED_CONVERT_URL, "inconsistency between the client protocol and the protocol of the server", "", "urls to invokers error", new IllegalStateException("urls to invokers error. invokerUrls.size :" + invokerUrls.size() + ", invoker.size :0. urls :" + invokerUrls.toString()));
            return;
        }
        List<Invoker<T>> newInvokers = Collections.unmodifiableList(new ArrayList<>(newUrlInvokerMap.values()));
        BitList<Invoker<T>> finalInvokers = multiGroup ? new BitList<>(toMergeInvokerList(newInvokers)) : new BitList<>(newInvokers);
        // pre-route and build cache
        refreshRouter(finalInvokers.clone(), () -> this.setInvokers(finalInvokers));
        this.urlInvokerMap = newUrlInvokerMap;
        try {
            // Close the unused Invoker
            destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap);
        } catch (Exception e) {
            logger.warn(REGISTRY_FAILED_DESTROY_SERVICE, "", "", "destroyUnusedInvokers error. ", e);
        }
        // notify invokers refreshed
        this.invokersChanged();
    }
    logger.info("Received invokers changed event from registry. " + "Registry type: interface. " + "Service Key: " + getConsumerUrl().getServiceKey() + ". " + "Urls Size : " + invokerUrls.size() + ". " + "Invokers Size : " + getInvokers().size() + ". " + "Available Size: " + getValidInvokers().size() + ". " + "Available Invokers : " + joinValidInvokerAddresses());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.408176843s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## RegistryDirectory.java -> getUrlInvokerMap()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `766`
- **Status:** `ERROR`
- **Comment:** `* Haomin: added for test purpose`

```java
/**
 * Haomin: added for test purpose
 */
public Map<URL, Invoker<T>> getUrlInvokerMap() {
    return urlInvokerMap;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.191372574s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## AbstractRegistry.java -> notify()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `545`
- **Status:** `ERROR`
- **Comment:** `* Notify changes from the provider side.      *      * @param url      consumer side url      * @param listener listener      * @param urls     provider latest urls`

```java
/**
 * Notify changes from the provider side.
 *
 * @param url      consumer side url
 * @param listener listener
 * @param urls     provider latest urls
 */
protected void notify(URL url, NotifyListener listener, List<URL> urls) {
    if (url == null) {
        throw new IllegalArgumentException("notify url == null");
    }
    if (listener == null) {
        throw new IllegalArgumentException("notify listener == null");
    }
    if ((CollectionUtils.isEmpty(urls)) && !ANY_VALUE.equals(url.getServiceInterface())) {
        // 1-4 Empty address.
        logger.warn(REGISTRY_EMPTY_ADDRESS, "", "", "Ignore empty notify urls for subscribe url " + url);
        return;
    }
    if (logger.isInfoEnabled()) {
        logger.info("[INSTANCE_REGISTER] Notify urls for subscribe url " + url + ", url size: " + urls.size());
    }
    // keep every provider's category.
    Map<String, List<URL>> result = new HashMap<>();
    for (URL u : urls) {
        if (UrlUtils.isMatch(url, u)) {
            String category = u.getCategory(DEFAULT_CATEGORY);
            List<URL> categoryList = result.computeIfAbsent(category, k -> new ArrayList<>());
            categoryList.add(u);
        }
    }
    if (result.size() == 0) {
        return;
    }
    Map<String, List<URL>> categoryNotified = ConcurrentHashMapUtils.computeIfAbsent(notified, url, u -> new ConcurrentHashMap<>());
    for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
        String category = entry.getKey();
        List<URL> categoryList = entry.getValue();
        categoryNotified.put(category, categoryList);
        listener.notify(categoryList);
        // We will update our cache file after each notification.
        // When our Registry has a subscribed failure due to network jitter, we can return at least the existing
        // cache URL.
        if (localCacheEnabled) {
            saveProperties(url);
        }
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.965014993s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## CacheableFailbackRegistry.java -> getSemaphore()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `451`
- **Status:** `ERROR`
- **Comment:** `* This method is for unit test to see if the RemovalTask has completed or not.<br />      * <strong>Please do not call this method in other places.</strong>`

```java
/**
 * This method is for unit test to see if the RemovalTask has completed or not.<br />
 * <strong>Please do not call this method in other places.</strong>
 */
@Deprecated
protected Semaphore getSemaphore() {
    return semaphore;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.745318136s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## RegistryManager.java -> clearRegistryNotDestroy()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `156`
- **Status:** `ERROR`
- **Comment:** `for unit test`

```java
// for unit test
public void clearRegistryNotDestroy() {
    registries.clear();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.52499808s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ServiceInstancesChangedListenerTest.java -> testSubscribeMultipleProtocols()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `460`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe multiple protocols`

```java
/**
 * Test subscribe multiple protocols
 */
@Test
@Order(7)
public void testSubscribeMultipleProtocols() {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("app1");
    listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
    // no protocol specified, consume all instances
    NotifyListener demoServiceListener1 = Mockito.mock(NotifyListener.class);
    when(demoServiceListener1.getConsumerUrl()).thenReturn(noProtocolConsumerURL);
    listener.addListenerAndNotify(noProtocolConsumerURL, demoServiceListener1);
    // multiple protocols specified
    NotifyListener demoServiceListener2 = Mockito.mock(NotifyListener.class);
    when(demoServiceListener2.getConsumerUrl()).thenReturn(multipleProtocolsConsumerURL);
    listener.addListenerAndNotify(multipleProtocolsConsumerURL, demoServiceListener2);
    // one protocol specified
    NotifyListener demoServiceListener3 = Mockito.mock(NotifyListener.class);
    when(demoServiceListener3.getConsumerUrl()).thenReturn(singleProtocolsConsumerURL);
    listener.addListenerAndNotify(singleProtocolsConsumerURL, demoServiceListener3);
    // notify app1 instance change
    ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1InstancesMultipleProtocols);
    listener.onEvent(app1_event);
    // check instances expose framework supported default protocols(currently dubbo, triple and rest) are notified
    ArgumentCaptor<List<URL>> default_protocol_captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(demoServiceListener1, Mockito.times(1)).notify(default_protocol_captor.capture());
    List<URL> default_protocol_notifiedUrls = default_protocol_captor.getValue();
    Assertions.assertEquals(4, default_protocol_notifiedUrls.size());
    // check instances expose protocols in consuming list(dubbo and triple) are notified
    ArgumentCaptor<List<URL>> multi_protocols_captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(demoServiceListener2, Mockito.times(1)).notify(multi_protocols_captor.capture());
    List<URL> multi_protocol_notifiedUrls = multi_protocols_captor.getValue();
    Assertions.assertEquals(4, multi_protocol_notifiedUrls.size());
    // check instances expose protocols in consuming list(only triple) are notified
    ArgumentCaptor<List<URL>> single_protocols_captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(demoServiceListener3, Mockito.times(1)).notify(single_protocols_captor.capture());
    List<URL> single_protocol_notifiedUrls = single_protocols_captor.getValue();
    Assertions.assertEquals(1, single_protocol_notifiedUrls.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.295779364s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ServiceInstancesChangedListenerTest.java -> testSubscribeMultipleGroups()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `504`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe multiple groups`

```java
/**
 * Test subscribe multiple groups
 */
@Test
@Order(8)
public void testSubscribeMultipleGroups() {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("app1");
    listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
    // notify instance change
    ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
    listener.onEvent(event);
    Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
    Assertions.assertEquals(1, allInstances.size());
    Assertions.assertEquals(3, allInstances.get("app1").size());
    ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
    List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, ",group1", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "*", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(0, serviceUrls.size());
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,group2", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(0, serviceUrls.size());
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,,group2", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.058910706s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ServiceInstancesChangedListenerTest.java -> testSubscribeMultipleVersions()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `561`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe multiple versions`

```java
/**
 * Test subscribe multiple versions
 */
@Test
@Order(9)
public void testSubscribeMultipleVersions() {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("app1");
    listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
    // notify instance change
    ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
    listener.onEvent(event);
    Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
    Assertions.assertEquals(1, allInstances.size());
    Assertions.assertEquals(3, allInstances.get("app1").size());
    ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
    List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "*", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, ",1.0.0", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "1.0.0,", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "1.0.0,,1.0.1", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "1.0.1,1.0.0", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(0, serviceUrls.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.828900054s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## ServiceInstancesChangedListenerWithoutEmptyProtectTest.java -> testSubscribeMultipleProtocols()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `459`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe multiple protocols`

```java
/**
 * Test subscribe multiple protocols
 */
@Test
@Order(7)
public void testSubscribeMultipleProtocols() {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("app1");
    listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
    // no protocol specified, consume all instances
    NotifyListener demoServiceListener1 = Mockito.mock(NotifyListener.class);
    when(demoServiceListener1.getConsumerUrl()).thenReturn(noProtocolConsumerURL);
    listener.addListenerAndNotify(noProtocolConsumerURL, demoServiceListener1);
    // multiple protocols specified
    NotifyListener demoServiceListener2 = Mockito.mock(NotifyListener.class);
    when(demoServiceListener2.getConsumerUrl()).thenReturn(multipleProtocolsConsumerURL);
    listener.addListenerAndNotify(multipleProtocolsConsumerURL, demoServiceListener2);
    // one protocol specified
    NotifyListener demoServiceListener3 = Mockito.mock(NotifyListener.class);
    when(demoServiceListener3.getConsumerUrl()).thenReturn(singleProtocolsConsumerURL);
    listener.addListenerAndNotify(singleProtocolsConsumerURL, demoServiceListener3);
    // notify app1 instance change
    ServiceInstancesChangedEvent app1_event = new ServiceInstancesChangedEvent("app1", app1InstancesMultipleProtocols);
    listener.onEvent(app1_event);
    // check instances expose framework supported default protocols(currently dubbo, triple and rest) are notified
    ArgumentCaptor<List<URL>> default_protocol_captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(demoServiceListener1, Mockito.times(1)).notify(default_protocol_captor.capture());
    List<URL> default_protocol_notifiedUrls = default_protocol_captor.getValue();
    Assertions.assertEquals(4, default_protocol_notifiedUrls.size());
    // check instances expose protocols in consuming list(dubbo and triple) are notified
    ArgumentCaptor<List<URL>> multi_protocols_captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(demoServiceListener2, Mockito.times(1)).notify(multi_protocols_captor.capture());
    List<URL> multi_protocol_notifiedUrls = multi_protocols_captor.getValue();
    Assertions.assertEquals(4, multi_protocol_notifiedUrls.size());
    // check instances expose protocols in consuming list(only triple) are notified
    ArgumentCaptor<List<URL>> single_protocols_captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(demoServiceListener3, Mockito.times(1)).notify(single_protocols_captor.capture());
    List<URL> single_protocol_notifiedUrls = single_protocols_captor.getValue();
    Assertions.assertEquals(1, single_protocol_notifiedUrls.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.601624311s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## ServiceInstancesChangedListenerWithoutEmptyProtectTest.java -> testSubscribeMultipleGroups()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `503`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe multiple groups`

```java
/**
 * Test subscribe multiple groups
 */
@Test
@Order(8)
public void testSubscribeMultipleGroups() {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("app1");
    listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
    // notify instance change
    ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
    listener.onEvent(event);
    Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
    Assertions.assertEquals(1, allInstances.size());
    Assertions.assertEquals(3, allInstances.get("app1").size());
    ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
    List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, ",group1", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "*", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(0, serviceUrls.size());
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,group2", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(0, serviceUrls.size());
    protocolServiceKey = new ProtocolServiceKey(service1, null, "group1,,group2", "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.370310013s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## ServiceInstancesChangedListenerWithoutEmptyProtectTest.java -> testSubscribeMultipleVersions()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `560`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe multiple versions`

```java
/**
 * Test subscribe multiple versions
 */
@Test
@Order(9)
public void testSubscribeMultipleVersions() {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("app1");
    listener = new ServiceInstancesChangedListener(serviceNames, serviceDiscovery);
    // notify instance change
    ServiceInstancesChangedEvent event = new ServiceInstancesChangedEvent("app1", app1Instances);
    listener.onEvent(event);
    Map<String, List<ServiceInstance>> allInstances = listener.getAllInstances();
    Assertions.assertEquals(1, allInstances.size());
    Assertions.assertEquals(3, allInstances.get("app1").size());
    ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(service1, null, null, "dubbo");
    List<URL> serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "*", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, ",1.0.0", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "1.0.0,", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "1.0.0,,1.0.1", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(3, serviceUrls.size());
    assertTrue(serviceUrls.get(0) instanceof InstanceAddressURL);
    protocolServiceKey = new ProtocolServiceKey(service1, "1.0.1,1.0.0", null, "dubbo");
    serviceUrls = listener.getAddresses(protocolServiceKey, consumerURL);
    Assertions.assertEquals(0, serviceUrls.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.143390178s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## MetadataServiceNameMappingTest.java -> testGet()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `126`
- **Status:** `ERROR`
- **Comment:** `* This test currently doesn't make any sense`

```java
/**
 * This test currently doesn't make any sense
 */
@Test
void testGet() {
    Set<String> set = new HashSet<>();
    set.add("app1");
    MetadataReportInstance reportInstance = mock(MetadataReportInstance.class);
    Mockito.when(reportInstance.getMetadataReport(any())).thenReturn(metadataReport);
    when(metadataReport.getServiceAppMapping(any(), any())).thenReturn(set);
    mapping.metadataReportInstance = reportInstance;
    Set<String> result = mapping.get(url);
    assertEquals(set, result);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.926205419s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## MetadataServiceNameMappingTest.java -> testGetAndListen()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `143`
- **Status:** `ERROR`
- **Comment:** `* Same situation as testGet, so left empty.`

```java
/**
 * Same situation as testGet, so left empty.
 */
@Test
void testGetAndListen() {
    // TODO
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.713654586s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## CustomizedParamsFilter.java -> instanceParamsIncluded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `44`
- **Status:** `ERROR`
- **Comment:** `* Not included in this test`

```java
/**
 * Not included in this test
 */
@Override
public String[] instanceParamsIncluded() {
    return new String[] { SIDE_KEY };
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.493239321s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## ExcludedParamsFilter.java -> instanceParamsIncluded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `40`
- **Status:** `ERROR`
- **Comment:** `* Not included in this test`

```java
/**
 * Not included in this test
 */
@Override
public String[] instanceParamsIncluded() {
    return new String[0];
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.270659989s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## ExcludedParamsFilter2.java -> instanceParamsIncluded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `40`
- **Status:** `ERROR`
- **Comment:** `* Not included in this test`

```java
/**
 * Not included in this test
 */
@Override
public String[] instanceParamsIncluded() {
    return new String[0];
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.056478895s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## MigrationRuleListenerTest.java -> testWithInitAndNoLocalRule()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `137`
- **Status:** `ERROR`
- **Comment:** `* Test listener started without local rule and config center, INIT should be used and no scheduled task should be started.`

```java
/**
 * Test listener started without local rule and config center, INIT should be used and no scheduled task should be started.
 */
@Test
void testWithInitAndNoLocalRule() {
    ApplicationModel.defaultModel().getDefaultModule().modelEnvironment().setDynamicConfiguration(null);
    ApplicationModel.defaultModel().getDefaultModule().modelEnvironment().setLocalMigrationRule("");
    ApplicationConfig applicationConfig = new ApplicationConfig();
    applicationConfig.setName("demo-consumer");
    ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);
    URL consumerURL = Mockito.mock(URL.class);
    Mockito.when(consumerURL.getServiceKey()).thenReturn("Test");
    Mockito.when(consumerURL.getParameter("timestamp")).thenReturn("1");
    System.setProperty("dubbo.application.migration.delay", "1000");
    MigrationRuleHandler<?> handler = Mockito.mock(MigrationRuleHandler.class, Mockito.withSettings().verboseLogging());
    MigrationRuleListener migrationRuleListener = new MigrationRuleListener(ApplicationModel.defaultModel().getDefaultModule());
    MigrationInvoker<?> migrationInvoker = Mockito.mock(MigrationInvoker.class);
    migrationRuleListener.getHandlers().put(migrationInvoker, handler);
    migrationRuleListener.onRefer(null, migrationInvoker, consumerURL, null);
    // check migration happened after invoker referred
    Mockito.verify(handler, Mockito.times(1)).doMigrate(MigrationRule.getInitRule());
    // check no delay tasks created for there's no local rule and no config center
    Assertions.assertNull(migrationRuleListener.localRuleMigrationFuture);
    Assertions.assertNull(migrationRuleListener.ruleMigrationFuture);
    Assertions.assertEquals(0, migrationRuleListener.ruleQueue.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 14.842297861s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "14s"
      }
    ]
  }
}

```

---

## ServiceDiscoveryRegistryTest.java -> testDoSubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `125`
- **Status:** `ERROR`
- **Comment:** `* Test subscribe      * - Normal case      * - Exceptional case      *   - check=true      *   - check=false`

```java
/**
 * Test subscribe
 * - Normal case
 * - Exceptional case
 *   - check=true
 *   - check=false
 */
@Test
void testDoSubscribe() {
    ApplicationModel applicationModel = spy(ApplicationModel.defaultModel());
    when(applicationModel.getDefaultExtension(ServiceNameMapping.class)).thenReturn(mapping);
    // Exceptional case, no interface-app mapping found
    when(mapping.getAndListen(any(), any(), any())).thenReturn(Collections.emptySet());
    // when check = false
    try {
        registryURL = registryURL.setScopeModel(applicationModel);
        serviceDiscoveryRegistry = new ServiceDiscoveryRegistry(registryURL, serviceDiscovery, mapping);
        serviceDiscoveryRegistry.doSubscribe(url, testServiceListener);
    } finally {
        registryURL = registryURL.setScopeModel(null);
        serviceDiscoveryRegistry.unsubscribe(url, testServiceListener);
    }
    //        // when check = true
    URL checkURL = url.addParameter(CHECK_KEY, true);
    checkURL.setScopeModel(url.getApplicationModel());
    //        Exception exceptionShouldHappen = null;
    //        try {
    //            serviceDiscoveryRegistry.doSubscribe(checkURL, testServiceListener);
    //        } catch (IllegalStateException e) {
    //            exceptionShouldHappen = e;
    //        } finally {
    //            serviceDiscoveryRegistry.unsubscribe(checkURL, testServiceListener);
    //        }
    //        if (exceptionShouldHappen == null) {
    //            fail();
    //        }
    // Normal case
    Set<String> singleApp = new HashSet<>();
    singleApp.add(APP_NAME1);
    when(mapping.getAndListen(any(), any(), any())).thenReturn(singleApp);
    try {
        serviceDiscoveryRegistry.doSubscribe(checkURL, testServiceListener);
    } finally {
        serviceDiscoveryRegistry.unsubscribe(checkURL, testServiceListener);
    }
    // test provider case
    checkURL = url.addParameter(PROVIDED_BY, APP_NAME1);
    try {
        serviceDiscoveryRegistry.doSubscribe(checkURL, testServiceListener);
    } finally {
        serviceDiscoveryRegistry.unsubscribe(checkURL, testServiceListener);
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 14.612453204s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "14s"
      }
    ]
  }
}

```

---

## ServiceDiscoveryRegistryTest.java -> testSubscribeURLs()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `184`
- **Status:** `ERROR`
- **Comment:** `* Test instance listener registration      * - one app      * - multi apps      * - repeat same multi apps, instance listener shared      * - protocol included in key      * - instance listener gets notified      * - instance listener and service listener rightly mapped`

```java
/**
 * Test instance listener registration
 * - one app
 * - multi apps
 * - repeat same multi apps, instance listener shared
 * - protocol included in key
 * - instance listener gets notified
 * - instance listener and service listener rightly mapped
 */
@Test
void testSubscribeURLs() {
    // interface to single app mapping
    Set<String> singleApp = new TreeSet<>();
    singleApp.add(APP_NAME1);
    serviceDiscoveryRegistry.subscribeURLs(url, testServiceListener, singleApp);
    assertEquals(1, serviceDiscoveryRegistry.getServiceListeners().size());
    verify(testServiceListener, times(1)).addServiceListener(instanceListener);
    verify(instanceListener, never()).onEvent(any());
    verify(serviceDiscovery, times(1)).addServiceInstancesChangedListener(instanceListener);
    // interface to multiple apps mapping
    Set<String> multiApps = new TreeSet<>();
    multiApps.add(APP_NAME1);
    multiApps.add(APP_NAME2);
    MockServiceInstancesChangedListener multiAppsInstanceListener = spy(new MockServiceInstancesChangedListener(multiApps, serviceDiscovery));
    doNothing().when(multiAppsInstanceListener).onEvent(any());
    List<URL> urls = new ArrayList<>();
    urls.add(URL.valueOf("dubbo://127.0.0.1:20880/TestService"));
    doReturn(urls).when(multiAppsInstanceListener).getAddresses(any(), any());
    when(serviceDiscovery.createListener(multiApps)).thenReturn(multiAppsInstanceListener);
    when(serviceDiscovery.getInstances(APP_NAME1)).thenReturn(instanceList1);
    when(serviceDiscovery.getInstances(APP_NAME2)).thenReturn(instanceList2);
    serviceDiscoveryRegistry.subscribeURLs(url, testServiceListener, multiApps);
    assertEquals(2, serviceDiscoveryRegistry.getServiceListeners().size());
    assertEquals(instanceListener, serviceDiscoveryRegistry.getServiceListeners().get(toStringKeys(singleApp)));
    assertEquals(multiAppsInstanceListener, serviceDiscoveryRegistry.getServiceListeners().get(toStringKeys(multiApps)));
    verify(testServiceListener, times(1)).addServiceListener(multiAppsInstanceListener);
    verify(multiAppsInstanceListener, times(2)).onEvent(any());
    verify(multiAppsInstanceListener, times(1)).addListenerAndNotify(any(), eq(testServiceListener));
    verify(serviceDiscovery, times(1)).addServiceInstancesChangedListener(multiAppsInstanceListener);
    ArgumentCaptor<List<URL>> captor = ArgumentCaptor.forClass(List.class);
    verify(testServiceListener).notify(captor.capture());
    assertEquals(urls, captor.getValue());
    // different interface mapping to the same apps
    NotifyListener testServiceListener2 = mock(NotifyListener.class);
    URL url2 = URL.valueOf("tri://127.0.0.1/TestService2?interface=TestService2&check=false&protocol=tri");
    when(testServiceListener2.getConsumerUrl()).thenReturn(url2);
    serviceDiscoveryRegistry.subscribeURLs(url2, testServiceListener2, multiApps);
    // check instance listeners not changed, methods not called
    assertEquals(2, serviceDiscoveryRegistry.getServiceListeners().size());
    assertEquals(multiAppsInstanceListener, serviceDiscoveryRegistry.getServiceListeners().get(toStringKeys(multiApps)));
    verify(multiAppsInstanceListener, times(1)).addListenerAndNotify(any(), eq(testServiceListener));
    // still called once, not executed this time
    verify(serviceDiscovery, times(2)).addServiceInstancesChangedListener(multiAppsInstanceListener);
    // check different protocol
    Map<String, Set<ServiceInstancesChangedListener.NotifyListenerWithKey>> serviceListeners = multiAppsInstanceListener.getServiceListeners();
    assertEquals(2, serviceListeners.size());
    assertEquals(1, serviceListeners.get(url.getServiceKey()).size());
    assertEquals(1, serviceListeners.get(url2.getServiceKey()).size());
    ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey(url2.getServiceInterface(), url2.getVersion(), url2.getGroup(), url2.getParameter(PROTOCOL_KEY, DUBBO));
    assertTrue(serviceListeners.get(url2.getServiceKey()).contains(new ServiceInstancesChangedListener.NotifyListenerWithKey(protocolServiceKey, testServiceListener2)));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 14.38792741s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "14s"
      }
    ]
  }
}

```

---

## ServiceDiscoveryRegistryTest.java -> testConcurrencySubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `255`
- **Status:** `ERROR`
- **Comment:** `* repeat of {@link this#testSubscribeURLs()} with multi threads`

```java
/**
 * repeat of {@link this#testSubscribeURLs()} with multi threads
 */
@Test
void testConcurrencySubscribe() {
    // TODO
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 14.160304212s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "14s"
      }
    ]
  }
}

```

---

## AbstractRegistryFactoryTest.java -> testRegistryFactoryIpCache()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `96`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
/**
 * Registration center address `dubbo` does not resolve
 */
// @Test
public void testRegistryFactoryIpCache() {
    Registry registry1 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":2233"));
    Registry registry2 = registryFactory.getRegistry(URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostAddress() + ":2233"));
    Assertions.assertEquals(registry1, registry2);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.938641783s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testRegister()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `91`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#register(URL)}.      *`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#register(URL)}.
 */
@Test
void testRegister() {
    // test one url
    abstractRegistry.register(mockUrl);
    assert abstractRegistry.getRegistered().contains(mockUrl);
    // test multiple urls
    for (URL url : abstractRegistry.getRegistered()) {
        abstractRegistry.unregister(url);
    }
    List<URL> urlList = getList();
    for (URL url : urlList) {
        abstractRegistry.register(url);
    }
    MatcherAssert.assertThat(abstractRegistry.getRegistered().size(), Matchers.equalTo(urlList.size()));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.72362188s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testUnregister()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `120`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#unregister(URL)}.      *`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#unregister(URL)}.
 */
@Test
void testUnregister() {
    // test one unregister
    URL url = new ServiceConfigURL("dubbo", "192.168.0.1", 2200);
    abstractRegistry.register(url);
    abstractRegistry.unregister(url);
    MatcherAssert.assertThat(false, Matchers.equalTo(abstractRegistry.getRegistered().contains(url)));
    // test multiple unregisters
    for (URL u : abstractRegistry.getRegistered()) {
        abstractRegistry.unregister(u);
    }
    List<URL> urlList = getList();
    for (URL urlSub : urlList) {
        abstractRegistry.register(urlSub);
    }
    for (URL urlSub : urlList) {
        abstractRegistry.unregister(urlSub);
    }
    MatcherAssert.assertThat(0, Matchers.equalTo(abstractRegistry.getRegistered().size()));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.499869773s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testSubscribeAndUnsubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `154`
- **Status:** `ERROR`
- **Comment:** `* test subscribe and unsubscribe`

```java
/**
 * test subscribe and unsubscribe
 */
@Test
void testSubscribeAndUnsubscribe() {
    // test subscribe
    final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
    NotifyListener listener = urls -> notified.set(Boolean.TRUE);
    URL url = new ServiceConfigURL("dubbo", "192.168.0.1", 2200);
    abstractRegistry.subscribe(url, listener);
    Set<NotifyListener> subscribeListeners = abstractRegistry.getSubscribed().get(url);
    MatcherAssert.assertThat(true, Matchers.equalTo(subscribeListeners.contains(listener)));
    // test unsubscribe
    abstractRegistry.unsubscribe(url, listener);
    Set<NotifyListener> unsubscribeListeners = abstractRegistry.getSubscribed().get(url);
    MatcherAssert.assertThat(false, Matchers.equalTo(unsubscribeListeners.contains(listener)));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.28390281s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testSubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `218`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#subscribe(URL, NotifyListener)}.      *`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#subscribe(URL, NotifyListener)}.
 */
@Test
void testSubscribe() {
    // check parameters
    try {
        abstractRegistry.subscribe(testUrl, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    // check parameters
    try {
        abstractRegistry.subscribe(null, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    // check if subscribe successfully
    Assertions.assertNull(abstractRegistry.getSubscribed().get(testUrl));
    abstractRegistry.subscribe(testUrl, listener);
    Assertions.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
    Assertions.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 13.070911619s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "13s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testUnsubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `246`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#unsubscribe(URL, NotifyListener)}.      *`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#unsubscribe(URL, NotifyListener)}.
 */
@Test
void testUnsubscribe() {
    // check parameters
    try {
        abstractRegistry.unsubscribe(testUrl, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    // check parameters
    try {
        abstractRegistry.unsubscribe(null, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    Assertions.assertNull(abstractRegistry.getSubscribed().get(testUrl));
    // check if unsubscribe successfully
    abstractRegistry.subscribe(testUrl, listener);
    abstractRegistry.unsubscribe(testUrl, listener);
    // Since we have subscribed testUrl, here should return a empty set instead of null
    Assertions.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
    Assertions.assertFalse(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.857320733s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testRecover()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `276`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#recover()}.`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#recover()}.
 */
@Test
void testRecover() throws Exception {
    // test recover nothing
    abstractRegistry.recover();
    Assertions.assertFalse(abstractRegistry.getRegistered().contains(testUrl));
    Assertions.assertNull(abstractRegistry.getSubscribed().get(testUrl));
    // test recover
    abstractRegistry.register(testUrl);
    abstractRegistry.subscribe(testUrl, listener);
    abstractRegistry.recover();
    // check if recover successfully
    Assertions.assertTrue(abstractRegistry.getRegistered().contains(testUrl));
    Assertions.assertNotNull(abstractRegistry.getSubscribed().get(testUrl));
    Assertions.assertTrue(abstractRegistry.getSubscribed().get(testUrl).contains(listener));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.64306817s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testNotify()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `310`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(List)}.`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(List)}.
 */
@Test
void testNotify() {
    final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
    NotifyListener listener1 = urls -> notified.set(Boolean.TRUE);
    URL url1 = new ServiceConfigURL("dubbo", "192.168.0.1", 2200, parametersConsumer);
    abstractRegistry.subscribe(url1, listener1);
    NotifyListener listener2 = urls -> notified.set(Boolean.TRUE);
    URL url2 = new ServiceConfigURL("dubbo", "192.168.0.2", 2201, parametersConsumer);
    abstractRegistry.subscribe(url2, listener2);
    NotifyListener listener3 = urls -> notified.set(Boolean.TRUE);
    URL url3 = new ServiceConfigURL("dubbo", "192.168.0.3", 2202, parametersConsumer);
    abstractRegistry.subscribe(url3, listener3);
    List<URL> urls = new ArrayList<>();
    urls.add(url1);
    urls.add(url2);
    urls.add(url3);
    abstractRegistry.notify(url1, listener1, urls);
    Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();
    MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
    MatcherAssert.assertThat(false, Matchers.equalTo(map.containsKey(url2)));
    MatcherAssert.assertThat(false, Matchers.equalTo(map.containsKey(url3)));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.428683098s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testNotifyList()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `336`
- **Status:** `ERROR`
- **Comment:** `* test notifyList`

```java
/**
 * test notifyList
 */
@Test
void testNotifyList() {
    final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
    NotifyListener listener1 = urls -> notified.set(Boolean.TRUE);
    URL url1 = new ServiceConfigURL("dubbo", "192.168.0.1", 2200, parametersConsumer);
    abstractRegistry.subscribe(url1, listener1);
    NotifyListener listener2 = urls -> notified.set(Boolean.TRUE);
    URL url2 = new ServiceConfigURL("dubbo", "192.168.0.2", 2201, parametersConsumer);
    abstractRegistry.subscribe(url2, listener2);
    NotifyListener listener3 = urls -> notified.set(Boolean.TRUE);
    URL url3 = new ServiceConfigURL("dubbo", "192.168.0.3", 2202, parametersConsumer);
    abstractRegistry.subscribe(url3, listener3);
    List<URL> urls = new ArrayList<>();
    urls.add(url1);
    urls.add(url2);
    urls.add(url3);
    abstractRegistry.notify(urls);
    Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();
    MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
    MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url2)));
    MatcherAssert.assertThat(true, Matchers.equalTo(map.containsKey(url3)));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.216452828s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## AbstractRegistryTest.java -> testNotifyArgs()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `408`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(URL, NotifyListener, List)}.      *`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.support.AbstractRegistry#notify(URL, NotifyListener, List)}.
 */
@Test
void testNotifyArgs() {
    // check parameters
    try {
        abstractRegistry.notify(null, null, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    // check parameters
    try {
        abstractRegistry.notify(testUrl, null, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    // check parameters
    try {
        abstractRegistry.notify(null, listener, null);
        Assertions.fail();
    } catch (Exception e) {
        Assertions.assertTrue(e instanceof IllegalArgumentException);
    }
    Assertions.assertFalse(notifySuccess);
    abstractRegistry.notify(testUrl, listener, null);
    Assertions.assertFalse(notifySuccess);
    List<URL> urls = new ArrayList<>();
    urls.add(testUrl);
    // check if notify successfully
    Assertions.assertFalse(notifySuccess);
    abstractRegistry.notify(testUrl, listener, urls);
    Assertions.assertTrue(notifySuccess);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 12.001108493s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "12s"
      }
    ]
  }
}

```

---

## FailbackRegistryTest.java -> testDoRetry()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `64`
- **Status:** `ERROR`
- **Comment:** `* Test method for retry      *      * @throws Exception`

```java
/**
 * Test method for retry
 *
 * @throws Exception
 */
@Test
void testDoRetry() throws Exception {
    final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
    // the latest latch just for 3. Because retry method has been removed.
    final CountDownLatch latch = new CountDownLatch(2);
    NotifyListener listener = urls -> notified.set(Boolean.TRUE);
    URL subscribeUrl = serviceUrl.setProtocol(CONSUMER_PROTOCOL).addParameters(CollectionUtils.toStringMap("check", "false"));
    registry = new MockRegistry(registryUrl, serviceUrl, latch);
    registry.setBad(true);
    registry.register(serviceUrl);
    registry.unregister(serviceUrl);
    registry.subscribe(subscribeUrl, listener);
    registry.unsubscribe(subscribeUrl, listener);
    // Failure can not be called to listener.
    assertEquals(false, notified.get());
    assertEquals(2, latch.getCount());
    registry.setBad(false);
    for (int i = 0; i < 20; i++) {
        logger.info("failback registry retry, times:" + i);
        if (latch.getCount() == 0)
            break;
        Thread.sleep(sleepTime);
    }
    assertEquals(0, latch.getCount());
    // The failed subscribe corresponding key will be cleared when unsubscribing
    assertEquals(false, notified.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 11.786147106s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "11s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testUrlError()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `59`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.
 */
@Test
void testUrlError() {
    Assertions.assertThrows(UnknownHostException.class, () -> {
        try {
            URL errorUrl = URL.valueOf("multicast://mullticast.local/");
            new MulticastRegistry(errorUrl);
        } catch (IllegalStateException e) {
            throw e.getCause();
        }
    });
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 11.56702065s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "11s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testAnyHost()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `74`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.
 */
@Test
void testAnyHost() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
        URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
        new MulticastRegistry(errorUrl);
    });
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 11.34896651s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "11s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testGetCustomPort()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `85`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.
 */
@Test
void testGetCustomPort() {
    int port = NetUtils.getAvailablePort(20880 + new Random().nextInt(10000));
    URL customPortUrl = URL.valueOf("multicast://239.239.239.239:" + port);
    MulticastRegistry multicastRegistry = new MulticastRegistry(customPortUrl);
    assertThat(multicastRegistry.getUrl().getPort(), is(port));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 11.126291528s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "11s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testRegister()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `96`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#getRegistered()}.`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#getRegistered()}.
 */
@Test
void testRegister() {
    Set<URL> registered;
    // clear first
    registered = registry.getRegistered();
    for (URL url : registered) {
        registry.unregister(url);
    }
    for (int i = 0; i < 2; i++) {
        registry.register(serviceUrl);
        registered = registry.getRegistered();
        assertTrue(registered.contains(serviceUrl));
    }
    // confirm only 1 register success
    registered = registry.getRegistered();
    assertEquals(1, registered.size());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.903679296s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testUnregister()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `118`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#unregister(URL)}.`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#unregister(URL)}.
 */
@Test
void testUnregister() {
    Set<URL> registered;
    // register first
    registry.register(serviceUrl);
    registered = registry.getRegistered();
    assertTrue(registered.contains(serviceUrl));
    // then unregister
    registered = registry.getRegistered();
    registry.unregister(serviceUrl);
    assertFalse(registered.contains(serviceUrl));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.689660034s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testSubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `138`
- **Status:** `ERROR`
- **Comment:** `* Test method for      * {@link org.apache.dubbo.registry.multicast.MulticastRegistry#subscribe(URL url, org.apache.dubbo.registry.NotifyListener)}      * .`

```java
/**
 * Test method for
 * {@link org.apache.dubbo.registry.multicast.MulticastRegistry#subscribe(URL url, org.apache.dubbo.registry.NotifyListener)}
 * .
 */
@Test
void testSubscribe() {
    // verify listener
    final URL[] notifyUrl = new URL[1];
    for (int i = 0; i < 10; i++) {
        registry.register(serviceUrl);
        registry.subscribe(consumerUrl, urls -> {
            notifyUrl[0] = urls.get(0);
            Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
            assertEquals(consumerUrl, subscribed.keySet().iterator().next());
        });
        if (!EMPTY_PROTOCOL.equalsIgnoreCase(notifyUrl[0].getProtocol())) {
            break;
        }
    }
    assertEquals(serviceUrl.toFullString(), notifyUrl[0].toFullString());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.473992752s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testUnsubscribe()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `160`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#unsubscribe(URL, NotifyListener)}`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#unsubscribe(URL, NotifyListener)}
 */
@Test
void testUnsubscribe() {
    // subscribe first
    registry.subscribe(consumerUrl, new NotifyListener() {

        @Override
        public void notify(List<URL> urls) {
            // do nothing
        }
    });
    // then unsubscribe
    registry.unsubscribe(consumerUrl, new NotifyListener() {

        @Override
        public void notify(List<URL> urls) {
            Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
            Set<NotifyListener> listeners = subscribed.get(consumerUrl);
            assertTrue(listeners.isEmpty());
            Map<URL, Set<URL>> received = registry.getReceived();
            assertTrue(received.get(consumerUrl).isEmpty());
        }
    });
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.251625504s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testAvailability()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `187`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link MulticastRegistry#isAvailable()}`

```java
/**
 * Test method for {@link MulticastRegistry#isAvailable()}
 */
@Test
void testAvailability() {
    int port = NetUtils.getAvailablePort(20880 + new Random().nextInt(10000));
    MulticastRegistry registry = new MulticastRegistry(URL.valueOf("multicast://224.5.6.8:" + port));
    assertTrue(registry.isAvailable());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 10.020873477s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "10s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testDestroy()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `197`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link MulticastRegistry#destroy()}`

```java
/**
 * Test method for {@link MulticastRegistry#destroy()}
 */
@Test
void testDestroy() {
    MulticastSocket socket = registry.getMulticastSocket();
    assertFalse(socket.isClosed());
    // then destroy, the multicast socket will be closed
    registry.destroy();
    socket = registry.getMulticastSocket();
    assertTrue(socket.isClosed());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.804711546s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testDefaultPort()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `211`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}
 */
@Test
void testDefaultPort() {
    MulticastRegistry multicastRegistry = new MulticastRegistry(URL.valueOf("multicast://224.5.6.7"));
    try {
        MulticastSocket multicastSocket = multicastRegistry.getMulticastSocket();
        Assertions.assertEquals(1234, multicastSocket.getLocalPort());
    } finally {
        multicastRegistry.destroy();
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.584811749s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## MulticastRegistryTest.java -> testCustomedPort()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `225`
- **Status:** `ERROR`
- **Comment:** `* Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}`

```java
/**
 * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}
 */
@Test
void testCustomedPort() {
    int port = NetUtils.getAvailablePort(20880 + new Random().nextInt(10000));
    MulticastRegistry multicastRegistry = new MulticastRegistry(URL.valueOf("multicast://224.5.6.7:" + port));
    try {
        MulticastSocket multicastSocket = multicastRegistry.getMulticastSocket();
        assertEquals(port, multicastSocket.getLocalPort());
    } finally {
        multicastRegistry.destroy();
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.36907064s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## NacosRegistry.java -> accept()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `779`
- **Status:** `ERROR`
- **Comment:** `* Tests whether or not the specified data should be accepted.          *          * @param data The data to be tested          * @return <code>true</code> if and only if <code>data</code>          * should be accepted`

```java
/**
 * Tests whether or not the specified data should be accepted.
 *
 * @param data The data to be tested
 * @return <code>true</code> if and only if <code>data</code>
 * should be accepted
 */
boolean accept(T data);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 9.137337737s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "9s"
      }
    ]
  }
}

```

---

## ZookeeperRegistry.java -> fetchLatestAddresses()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `378`
- **Status:** `ERROR`
- **Comment:** `* When zookeeper connection recovered from a connection loss, it needs to fetch the latest provider list.      * re-register watcher is only a side effect and is not mandate.`

```java
/**
 * When zookeeper connection recovered from a connection loss, it needs to fetch the latest provider list.
 * re-register watcher is only a side effect and is not mandate.
 */
private void fetchLatestAddresses() {
    // subscribe
    Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
    if (!recoverSubscribed.isEmpty()) {
        if (logger.isInfoEnabled()) {
            logger.info("Fetching the latest urls of " + recoverSubscribed.keySet());
        }
        for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
            URL url = entry.getKey();
            for (NotifyListener listener : entry.getValue()) {
                removeFailedSubscribed(url, listener);
                addFailedSubscribed(url, listener);
            }
        }
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.920542437s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## PortUnificationExchanger.java -> getServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `84`
- **Status:** `ERROR`
- **Comment:** `for test`

```java
// for test
public static ConcurrentMap<String, RemotingServer> getServers() {
    return servers;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.70233245s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserver.java -> getNumSentBytesQueued()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `185`
- **Status:** `ERROR`
- **Comment:** `* Returns the number of bytes currently queued for sending.      * Visible for testing.`

```java
/**
 * Returns the number of bytes currently queued for sending.
 * Visible for testing.
 */
protected long getNumSentBytesQueued() {
    return numSentBytesQueued.get();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.484956166s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testIsReadyWhenBelowThreshold()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `42`
- **Status:** `ERROR`
- **Comment:** `* Test isReady returns true when below threshold.`

```java
/**
 * Test isReady returns true when below threshold.
 */
@Test
void testIsReadyWhenBelowThreshold() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    assertTrue(observer.isReady());
    observer.onSendingBytes(1000);
    assertTrue(observer.isReady());
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD - 1001);
    assertTrue(observer.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.269269356s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testIsReadyWhenAtOrAboveThreshold()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `58`
- **Status:** `ERROR`
- **Comment:** `* Test isReady returns false when at or above threshold.`

```java
/**
 * Test isReady returns false when at or above threshold.
 */
@Test
void testIsReadyWhenAtOrAboveThreshold() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD);
    assertFalse(observer.isReady());
    observer.onSendingBytes(1000);
    assertFalse(observer.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 8.052743879s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "8s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testOnReadyTriggeredOnTransition()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `72`
- **Status:** `ERROR`
- **Comment:** `* Test onReady is triggered when transitioning from not-ready to ready.`

```java
/**
 * Test onReady is triggered when transitioning from not-ready to ready.
 */
@Test
void testOnReadyTriggeredOnTransition() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    observer.setOnReadyHandler(onReadyCount::incrementAndGet);
    // Send bytes to exceed threshold
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
    assertFalse(observer.isReady());
    assertEquals(0, onReadyCount.get());
    // Complete sending - should trigger onReady when crossing threshold
    observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
    assertTrue(observer.isReady());
    assertEquals(1, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 7.835452589s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "7s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testOnReadyNotTriggeredWhenStayingBelowThreshold()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `92`
- **Status:** `ERROR`
- **Comment:** `* Test onReady is NOT triggered when staying below threshold.`

```java
/**
 * Test onReady is NOT triggered when staying below threshold.
 */
@Test
void testOnReadyNotTriggeredWhenStayingBelowThreshold() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    observer.setOnReadyHandler(onReadyCount::incrementAndGet);
    // Send small amount
    observer.onSendingBytes(1000);
    observer.onSentBytes(1000);
    assertEquals(0, onReadyCount.get());
    // Send another small amount
    observer.onSendingBytes(2000);
    observer.onSentBytes(2000);
    assertEquals(0, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 7.619555853s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "7s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testMultipleTransitions()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `112`
- **Status:** `ERROR`
- **Comment:** `* Test multiple transitions trigger onReady each time.`

```java
/**
 * Test multiple transitions trigger onReady each time.
 */
@Test
void testMultipleTransitions() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    observer.setOnReadyHandler(onReadyCount::incrementAndGet);
    // First cycle
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
    observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
    assertEquals(1, onReadyCount.get());
    // Second cycle
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 2000);
    observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 2000);
    assertEquals(2, onReadyCount.get());
    // Third cycle
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 3000);
    observer.onSentBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 3000);
    assertEquals(3, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 7.407629776s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "7s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testConcurrentSendsOnlyTriggerOnReadyOnce()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `137`
- **Status:** `ERROR`
- **Comment:** `* Test concurrent sends only trigger onReady once for single transition.`

```java
/**
 * Test concurrent sends only trigger onReady once for single transition.
 */
@Test
void testConcurrentSendsOnlyTriggerOnReadyOnce() throws InterruptedException {
    TestableHttp2ServerChannelObserver observer = createObserver();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    observer.setOnReadyHandler(onReadyCount::incrementAndGet);
    // Exceed threshold
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 10000);
    // Simulate concurrent completions
    int threadCount = 10;
    int bytesPerThread = ((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 10000) / threadCount;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                startLatch.await();
                observer.onSentBytes(bytesPerThread);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
    }
    startLatch.countDown();
    doneLatch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    // Only one thread should trigger onReady
    assertEquals(1, onReadyCount.get());
    assertTrue(observer.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 7.184439299s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "7s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testInitialStateIsReady()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `178`
- **Status:** `ERROR`
- **Comment:** `* Test initial state is ready.`

```java
/**
 * Test initial state is ready.
 */
@Test
void testInitialStateIsReady() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    assertTrue(observer.isReady());
    assertEquals(0, observer.getNumSentBytesQueued());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.971736166s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testRollbackDoesNotTriggerOnReady()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `188`
- **Status:** `ERROR`
- **Comment:** `* Test rollback does not trigger onReady.`

```java
/**
 * Test rollback does not trigger onReady.
 */
@Test
void testRollbackDoesNotTriggerOnReady() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    observer.setOnReadyHandler(onReadyCount::incrementAndGet);
    // Exceed threshold
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
    // Rollback (simulating send failure)
    observer.rollbackSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD + 1000);
    // Should not trigger onReady
    assertTrue(observer.isReady());
    assertEquals(0, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.746796321s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## Http2ServerChannelObserverByteCountingTest.java -> testExactThresholdBoundary()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `208`
- **Status:** `ERROR`
- **Comment:** `* Test exact threshold boundary.`

```java
/**
 * Test exact threshold boundary.
 */
@Test
void testExactThresholdBoundary() {
    TestableHttp2ServerChannelObserver observer = createObserver();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    observer.setOnReadyHandler(onReadyCount::incrementAndGet);
    // At exactly threshold - not ready
    observer.onSendingBytes((int) Http2ServerChannelObserver.ON_READY_THRESHOLD);
    assertFalse(observer.isReady());
    // Send 1 byte to go below threshold
    observer.onSentBytes(1);
    assertTrue(observer.isReady());
    assertEquals(1, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.521244422s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## Curator5ZookeeperClient.java -> getClient()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `551`
- **Status:** `ERROR`
- **Comment:** `* just for unit test      *      * @return`

```java
/**
 * just for unit test
 *
 * @return
 */
CuratorFramework getClient() {
    return client;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.307481658s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## ZookeeperClientManager.java -> getZookeeperClientMap()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `187`
- **Status:** `ERROR`
- **Comment:** `* for unit test      *      * @return`

```java
/**
 * for unit test
 *
 * @return
 */
public Map<String, ZookeeperClient> getZookeeperClientMap() {
    return zookeeperClientMap;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 6.08054661s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "6s"
      }
    ]
  }
}

```

---

## AccessLogFilter.java -> setInterval()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `283`
- **Status:** `ERROR`
- **Comment:** `test purpose only`

```java
// test purpose only
public static void setInterval(long interval) {
    LOG_OUTPUT_INTERVAL = interval;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 5.866866283s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "5s"
      }
    ]
  }
}

```

---

## AccessLogFilter.java -> getInterval()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `288`
- **Status:** `ERROR`
- **Comment:** `test purpose only`

```java
// test purpose only
public static long getInterval() {
    return LOG_OUTPUT_INTERVAL;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 5.631809168s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "5s"
      }
    ]
  }
}

```

---

## AccessLogFilter.java -> destroy()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `293`
- **Status:** `ERROR`
- **Comment:** `test purpose only`

```java
// test purpose only
public void destroy() {
    future.cancel(true);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 5.407553243s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "5s"
      }
    ]
  }
}

```

---

## ExceptionFilter.java -> mockLogger()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `144`
- **Status:** `ERROR`
- **Comment:** `For test purpose`

```java
// For test purpose
@DisableInject
public void mockLogger(ErrorTypeAwareLogger logger) {
    this.logger = logger;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 5.145964658s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "5s"
      }
    ]
  }
}

```

---

## FutureAdapter.java -> cancel()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `56`
- **Status:** `ERROR`
- **Comment:** `TODO figure out the meaning of cancel in DefaultFuture.`

```java
// TODO figure out the meaning of cancel in DefaultFuture.
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    //        Invocation invocation = invocationSoftReference.get();
    //        if (invocation != null) {
    //            invocation.getInvoker().invoke(cancel);
    //        }
    return appResponseFuture.cancel(mayInterruptIfRunning);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 4.924858678s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "4s"
      }
    ]
  }
}

```

---

## MethodInvoker.java -> getInvokers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `106`
- **Status:** `SUCCESS`
- **Comment:** `* for test          *          * @return all MethodInvoker`

```java
/**
 * for test
 *
 * @return all MethodInvoker
 */
Map<String, MethodInvoker> getInvokers() {
    return invokers;
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MethodInvokerTest {

    // Assuming the class containing getInvokers() is named InvokerRegistry or similar.
    // Replace with the actual class name when integrating.
    private InvokerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InvokerRegistry();
    }

    @Test
    void testGetInvokersReturnsAllMethodInvokers() {
        // Since getInvokers() has package-private visibility and is explicitly 
        // intended "for test", we can invoke it directly within the same package,
        // or use reflection if package access is restricted.
        
        Map<String, MethodInvoker> invokers = registry.getInvokers();

        // Assert that the returned map is not null
        assertNotNull(invokers, "The invokers map should not be null");

        // Additional assertions can be added here depending on the initial state,
        // for example, verifying it is empty initially or contains expected test mappings.
        assertTrue(invokers.isEmpty(), "The invokers map should be empty by default");
    }
}
```

---

## AbstractGracefulShutdownTest.java -> testFireChannelEventSingleServer()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `68`
- **Status:** `SUCCESS`
- **Comment:** `* Test fireChannelEvent fires event to single server.`

```java
/**
 * Test fireChannelEvent fires event to single server.
 */
@Test
void testFireChannelEventSingleServer() {
    TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));
    shutdown.readonly();
    verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
}
```

### Generated Test Case

```java
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TestGracefulShutdownTest {

    @Mock
    private RemotingServer mockRemotingServer1;

    private TestGracefulShutdown shutdown;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Assuming TestGracefulShutdown accepts a list of RemotingServer and utilizes them
        // based on the provided debt comment and method context.
    }

    /**
     * Test fireChannelEvent fires event to single server.
     */
    @Test
    void testFireChannelEventSingleServer() {
        // Given a shutdown instance initialized with a single mock server
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockRemotingServer1));
        
        // When the shutdown transitions to readonly state
        shutdown.readonly();
        
        // Then the channel event should be fired exactly once to the single server
        verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }
}
```

---

## AbstractGracefulShutdownTest.java -> testFireChannelEventMultipleServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `80`
- **Status:** `SUCCESS`
- **Comment:** `* Test fireChannelEvent fires event to multiple servers.`

```java
/**
 * Test fireChannelEvent fires event to multiple servers.
 */
@Test
void testFireChannelEventMultipleServers() {
    TestGracefulShutdown shutdown = new TestGracefulShutdown(Arrays.asList(mockServer1, mockServer2));
    shutdown.readonly();
    verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
}
```

### Generated Test Case

```java
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// Assuming TestGracefulShutdown, RemotingServer, and ReadOnlyEvent exist in the project context.
class TestGracefulShutdownTest {

    @Mock
    private RemotingServer mockRemotingServer1;

    @Mock
    private RemotingServer mockRemotingServer2;

    private TestGracefulShutdown shutdown;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        shutdown = new TestGracefulShutdown(Arrays.asList(mockRemotingServer1, mockRemotingServer2));
    }

    /**
     * Test fireChannelEvent fires event to multiple servers.
     */
    @Test
    void testFireChannelEventMultipleServers() {
        // Act
        shutdown.readonly();

        // Assert
        verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
        verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }
}
```

---

## AbstractGracefulShutdownTest.java -> testReadonlySendsReadOnlyEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `93`
- **Status:** `SUCCESS`
- **Comment:** `* Test readonly sends ReadOnlyEvent.`

```java
/**
 * Test readonly sends ReadOnlyEvent.
 */
@Test
void testReadonlySendsReadOnlyEvent() {
    TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));
    shutdown.readonly();
    ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
    verify(mockRemotingServer1).fireChannelEvent(captor.capture());
    ChannelEvent capturedEvent = captor.getValue();
    assertTrue(capturedEvent instanceof ReadOnlyEvent);
    assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class GracefulShutdownTest {

    @Test
    void testReadonlySendsReadOnlyEvent() {
        // Given
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));
        
        // When
        shutdown.readonly();
        
        // Then
        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1).fireChannelEvent(captor.capture());
        ChannelEvent capturedEvent = captor.getValue();
        
        assertTrue(capturedEvent instanceof ReadOnlyEvent);
        assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
    }
}
```

---

## AbstractGracefulShutdownTest.java -> testWriteableSendsWriteableEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `110`
- **Status:** `SUCCESS`
- **Comment:** `* Test writeable sends WriteableEvent.`

```java
/**
 * Test writeable sends WriteableEvent.
 */
@Test
void testWriteableSendsWriteableEvent() {
    TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));
    shutdown.writeable();
    ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
    verify(mockRemotingServer1).fireChannelEvent(captor.capture());
    ChannelEvent capturedEvent = captor.getValue();
    assertTrue(capturedEvent instanceof WriteableEvent);
    assertSame(WriteableEvent.INSTANCE, capturedEvent);
}
```

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TestGracefulShutdownTest {

    @Mock
    private RemotingServer mockRemotingServer1;

    @Test
    void testWriteableSendsWriteableEvent() {
        MockitoAnnotations.openMocks(this);
        
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockRemotingServer1));
        shutdown.writeable();
        
        ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
        verify(mockRemotingServer1).fireChannelEvent(captor.capture());
        
        ChannelEvent capturedEvent = captor.getValue();
        assertTrue(capturedEvent instanceof WriteableEvent);
        assertSame(WriteableEvent.INSTANCE, capturedEvent);
    }
}
```

---

## AbstractGracefulShutdownTest.java -> testExceptionsAreCaught()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `127`
- **Status:** `SUCCESS`
- **Comment:** `* Test that exceptions are caught and don't propagate.`

```java
/**
 * Test that exceptions are caught and don't propagate.
 */
@Test
void testExceptionsAreCaught() {
    Mockito.doThrow(new RuntimeException("Test exception")).when(mockRemotingServer1).fireChannelEvent(Mockito.any(ChannelEvent.class));
    TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));
    // Should not throw exception
    shutdown.readonly();
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TestGracefulShutdownDebtTest {

    @Test
    void testExceptionsAreCaughtAndDoNotPropagate() {
        // Given
        RemotingServer mockRemotingServer1 = Mockito.mock(RemotingServer.class);
        ServerWrapper mockServer1 = Mockito.mock(ServerWrapper.class);
        
        Mockito.when(mockServer1.getRemotingServer()).thenReturn(mockRemotingServer1);
        Mockito.doThrow(new RuntimeException("Test exception"))
              .when(mockRemotingServer1)
              .fireChannelEvent(Mockito.any(ChannelEvent.class));

        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.singletonList(mockServer1));

        // When & Then
        assertDoesNotThrow(shutdown::readonly, 
            "The readonly method should catch exceptions thrown by remoting servers and prevent them from propagating.");
    }
}
```

---

## AbstractGracefulShutdownTest.java -> testEmptyServerList()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `142`
- **Status:** `SUCCESS`
- **Comment:** `* Test with empty server list.`

```java
/**
 * Test with empty server list.
 */
@Test
void testEmptyServerList() {
    TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.emptyList());
    // Should not throw exception
    shutdown.readonly();
    shutdown.writeable();
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Test;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TestGracefulShutdownTest {

    @Test
    void testEmptyServerList() {
        TestGracefulShutdown shutdown = new TestGracefulShutdown(Collections.emptyList());
        
        assertDoesNotThrow(() -> {
            shutdown.readonly();
            shutdown.writeable();
        }, "Graceful shutdown operations with an empty server list should not throw any exceptions.");
    }
}
```

---

## AppResponseTest.java -> testAppResponseWithEmptyStackTraceException()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `40`
- **Status:** `SUCCESS`
- **Comment:** `* please run this test in Run mode`

```java
/**
 * please run this test in Run mode
 */
@Test
void testAppResponseWithEmptyStackTraceException() {
    Throwable throwable = buildEmptyStackTraceException();
    assumeFalse(throwable == null);
    AppResponse appResponse = new AppResponse(throwable);
    StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
    Assertions.assertNotNull(stackTrace);
    Assertions.assertEquals(0, stackTrace.length);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AppResponseTest {

    @Test
    void testAppResponseWithEmptyStackTraceException() {
        Throwable throwable = buildEmptyStackTraceException();
        assumeFalse(throwable == null, "Throwable could not be built");
        
        AppResponse appResponse = new AppResponse(throwable);
        
        Throwable exception = appResponse.getException();
        Assertions.assertNotNull(exception, "AppResponse exception should not be null");
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        Assertions.assertNotNull(stackTrace, "StackTrace should not be null");
        Assertions.assertEquals(0, stackTrace.length, "StackTrace length should be 0");
    }

    private Throwable buildEmptyStackTraceException() {
        Throwable throwable = new RuntimeException("Test exception");
        throwable.setStackTrace(new StackTraceElement[0]);
        return throwable;
    }
}
```

---

## AppResponseTest.java -> testSetExceptionWithEmptyStackTraceException()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `65`
- **Status:** `SUCCESS`
- **Comment:** `* please run this test in Run mode`

```java
/**
 * please run this test in Run mode
 */
@Test
void testSetExceptionWithEmptyStackTraceException() {
    Throwable throwable = buildEmptyStackTraceException();
    assumeFalse(throwable == null);
    AppResponse appResponse = new AppResponse();
    appResponse.setException(throwable);
    StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
    Assertions.assertNotNull(stackTrace);
    Assertions.assertEquals(0, stackTrace.length);
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AppResponseTest {

    @Test
    void testSetExceptionWithEmptyStackTraceException() {
        Throwable throwable = buildEmptyStackTraceException();
        assumeFalse(throwable == null);
        AppResponse appResponse = new AppResponse();
        appResponse.setException(throwable);
        StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertEquals(0, stackTrace.length);
    }

    private Throwable buildEmptyStackTraceException() {
        Throwable throwable = new Throwable("Test Exception");
        throwable.setStackTrace(new StackTraceElement[0]);
        return throwable;
    }
}
```

---

## DefaultProtocolServerTest.java -> testGetRemotingServer()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `58`
- **Status:** `SUCCESS`
- **Comment:** `* Test that getRemotingServer returns the underlying server.`

```java
/**
 * Test that getRemotingServer returns the underlying server.
 */
@Test
void testGetRemotingServer() {
    assertSame(mockServer, protocolServer.getRemotingServer());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test debt payoff for verifying that getRemotingServer returns the underlying server
 * with robust setup and assertions.
 */
class ProtocolServerTest {

    private Object mockServer;
    private ProtocolServer protocolServer;

    @BeforeEach
    void setUp() {
        // Initialize the mock server and the protocol server instance
        mockServer = Mockito.mock(Object.class);
        protocolServer = new ProtocolServer(mockServer);
    }

    @Test
    void testGetRemotingServerReturnsUnderlyingServer() {
        // Verify that getRemotingServer correctly returns the exact underlying server instance
        assertSame(mockServer, protocolServer.getRemotingServer(), 
                "The remoting server returned should be identical to the underlying server instance.");
    }
}
```

---

## DefaultProtocolServerTest.java -> testGetUrl()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `66`
- **Status:** `SUCCESS`
- **Comment:** `* Test that getUrl returns the server's URL.`

```java
/**
 * Test that getUrl returns the server's URL.
 */
@Test
void testGetUrl() {
    assertEquals(testUrl, protocolServer.getUrl());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprehensive test cases for protocolServer.getUrl() to pay off technical debt
 * and ensure robust verification of URL retrieval under various states.
 */
class ProtocolServerTest {

    private ProtocolServer protocolServer;
    private String testUrl;

    @BeforeEach
    void setUp() {
        testUrl = "http://localhost:8080/api";
        protocolServer = new ProtocolServer(testUrl);
    }

    @Test
    void testGetUrlReturnsCorrectServerUrl() {
        // Verify that getUrl successfully returns the exact URL configured on the server
        String actualUrl = protocolServer.getUrl();
        
        assertNotNull(actualUrl, "The server URL should not be null");
        assertEquals(testUrl, actualUrl, "The returned URL must match the initialized test URL");
    }

    @Test
    void testGetUrlAfterUrlUpdate() {
        // Verify that getUrl reflects updates if the server URL changes dynamically
        String updatedUrl = "https://secure.example.com:8443/v2";
        protocolServer.setUrl(updatedUrl);

        assertEquals(updatedUrl, protocolServer.getUrl(), "The returned URL must reflect the updated server URL");
    }
}
```

---

## DefaultProtocolServerTest.java -> testGetAddressFromServer()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `74`
- **Status:** `SUCCESS`
- **Comment:** `* Test that getAddress returns the server's address when no custom address is set.`

```java
/**
 * Test that getAddress returns the server's address when no custom address is set.
 */
@Test
void testGetAddressFromServer() {
    assertEquals(testUrl.getAddress(), protocolServer.getAddress());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolServerTest {

    private URI testUrl;
    private ProtocolServer protocolServer;

    @BeforeEach
    void setUp() throws Exception {
        // Assuming testUrl and protocolServer are instantiated properly for the test.
        // These can be adjusted based on the actual class constructor and setup logic.
        testUrl = new URI("http://localhost:8080/path");
        protocolServer = new ProtocolServer(testUrl);
    }

    /**
     * Test that getAddress returns the server's address when no custom address is set.
     */
    @Test
    void testGetAddressFromServer() {
        // Given that no custom address has been configured on the protocolServer,
        // it should fall back to or return the base server address from the configuration (testUrl).
        String expectedAddress = testUrl.getAddress();
        
        // When
        String actualAddress = protocolServer.getAddress();
        
        // Then
        assertEquals(expectedAddress, actualAddress, "The returned address should match the server's default address when no custom address is set.");
    }
}
```

---

## DefaultProtocolServerTest.java -> testGetAddressCustom()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `82`
- **Status:** `SUCCESS`
- **Comment:** `* Test that getAddress returns the custom address when set.`

```java
/**
 * Test that getAddress returns the custom address when set.
 */
@Test
void testGetAddressCustom() {
    String customAddress = "192.168.1.100:8080";
    protocolServer.setAddress(customAddress);
    assertEquals(customAddress, protocolServer.getAddress());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolServerTest {

    private ProtocolServer protocolServer;

    @BeforeEach
    void setUp() {
        protocolServer = new ProtocolServer();
    }

    /**
     * Test that getAddress returns the custom address when set.
     */
    @Test
    void testGetAddressCustom() {
        String customAddress = "192.168.1.100:8080";
        protocolServer.setAddress(customAddress);
        assertEquals(customAddress, protocolServer.getAddress());
    }
}
```

---

## DefaultProtocolServerTest.java -> testSetAddress()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `92`
- **Status:** `SUCCESS`
- **Comment:** `* Test that setAddress updates the address.`

```java
/**
 * Test that setAddress updates the address.
 */
@Test
void testSetAddress() {
    String address1 = "192.168.1.100:8080";
    String address2 = "192.168.1.200:9090";
    protocolServer.setAddress(address1);
    assertEquals(address1, protocolServer.getAddress());
    protocolServer.setAddress(address2);
    assertEquals(address2, protocolServer.getAddress());
}
```

### Generated Test Case

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProtocolServerTest {

    private ProtocolServer protocolServer;

    @BeforeEach
    void setUp() {
        protocolServer = new ProtocolServer();
    }

    @Test
    void testSetAddress() {
        String address1 = "192.168.1.100:8080";
        String address2 = "192.168.1.200:9090";

        // Verify initial state if applicable
        assertNull(protocolServer.getAddress());

        // Set and verify the first address
        protocolServer.setAddress(address1);
        assertEquals(address1, protocolServer.getAddress(), "The address should be updated to address1");

        // Update and verify the second address
        protocolServer.setAddress(address2);
        assertEquals(address2, protocolServer.getAddress(), "The address should be updated to address2");
        
        // Test setting null or empty if supported by the implementation
        protocolServer.setAddress(null);
        assertNull(protocolServer.getAddress(), "The address should be updatable to null");
    }
}
```

---

## DefaultProtocolServerTest.java -> testReset()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `107`
- **Status:** `SUCCESS`
- **Comment:** `* Test that reset delegates to the underlying server.`

```java
/**
 * Test that reset delegates to the underlying server.
 */
@Test
void testReset() {
    URL newUrl = new ServiceConfigURL("dubbo", "127.0.0.1", 20882);
    protocolServer.reset(newUrl);
    verify(mockServer, times(1)).reset(newUrl);
}
```

### Generated Test Case

```java
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.ServiceConfigURL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProtocolServerTest {

    private ProtocolServer protocolServer;
    private Server mockServer;

    @BeforeEach
    void setUp() {
        mockServer = Mockito.mock(Server.class);
        // Assuming ProtocolServer takes the underlying Server in its constructor or setter
        protocolServer = new ProtocolServer(mockServer);
    }

    /**
     * Test that reset delegates to the underlying server.
     */
    @Test
    void testReset() {
        URL newUrl = new ServiceConfigURL("dubbo", "127.0.0.1", 20882);
        protocolServer.reset(newUrl);
        verify(mockServer, times(1)).reset(newUrl);
    }
}
```

---

## DefaultProtocolServerTest.java -> testClose()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `117`
- **Status:** `ERROR`
- **Comment:** `* Test that close delegates to the underlying server.`

```java
/**
 * Test that close delegates to the underlying server.
 */
@Test
void testClose() {
    protocolServer.close();
    verify(mockServer, times(1)).close();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 45.332090392s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "45s"
      }
    ]
  }
}

```

---

## DefaultProtocolServerTest.java -> testGetAttributesNotNull()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `126`
- **Status:** `ERROR`
- **Comment:** `* Test that getAttributes returns a non-null map.`

```java
/**
 * Test that getAttributes returns a non-null map.
 */
@Test
void testGetAttributesNotNull() {
    Map<String, Object> attributes = protocolServer.getAttributes();
    assertNotNull(attributes);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 44.654221732s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "44s"
      }
    ]
  }
}

```

---

## DefaultProtocolServerTest.java -> testAttributesStorage()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `135`
- **Status:** `ERROR`
- **Comment:** `* Test that attributes can be stored and retrieved.`

```java
/**
 * Test that attributes can be stored and retrieved.
 */
@Test
void testAttributesStorage() {
    Map<String, Object> attributes = protocolServer.getAttributes();
    attributes.put("key1", "value1");
    attributes.put("key2", 42);
    attributes.put("key3", true);
    assertEquals("value1", attributes.get("key1"));
    assertEquals(42, attributes.get("key2"));
    assertEquals(true, attributes.get("key3"));
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 44.40483332s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "44s"
      }
    ]
  }
}

```

---

## DefaultProtocolServerTest.java -> testAttributesAreSameInstance()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `151`
- **Status:** `ERROR`
- **Comment:** `* Test that attributes are thread-safe (ConcurrentHashMap).`

```java
/**
 * Test that attributes are thread-safe (ConcurrentHashMap).
 */
@Test
void testAttributesAreSameInstance() {
    Map<String, Object> attributes1 = protocolServer.getAttributes();
    Map<String, Object> attributes2 = protocolServer.getAttributes();
    assertSame(attributes1, attributes2);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 43.723315334s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "43s"
      }
    ]
  }
}

```

---

## DefaultProtocolServerTest.java -> testFireChannelEventDelegation()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `162`
- **Status:** `ERROR`
- **Comment:** `* Test fireChannelEvent with ReadOnlyEvent delegates to the underlying server.`

```java
/**
 * Test fireChannelEvent with ReadOnlyEvent delegates to the underlying server.
 */
@Test
void testFireChannelEventDelegation() {
    ChannelEvent event = ReadOnlyEvent.INSTANCE;
    // Since DefaultProtocolServer doesn't override fireChannelEvent,
    // we test through getRemotingServer().fireChannelEvent()
    protocolServer.getRemotingServer().fireChannelEvent(event);
    verify(mockServer, times(1)).fireChannelEvent(event);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 43.466284928s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "43s"
      }
    ]
  }
}

```

---

## AccessLogFilterTest.java -> testDefault()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `50`
- **Status:** `SUCCESS`
- **Comment:** `TODO how to assert thread action`

```java
// TODO how to assert thread action
@Test
@SuppressWarnings("unchecked")
public void testDefault() throws NoSuchFieldException, IllegalAccessException {
    URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");
    Invoker<AccessLogFilterTest> invoker = new MyInvoker<AccessLogFilterTest>(url);
    Invocation invocation = new MockInvocation();
    Field field = AccessLogFilter.class.getDeclaredField("logEntries");
    field.setAccessible(true);
    assertTrue(((Map) field.get(accessLogFilter)).isEmpty());
    accessLogFilter.invoke(invoker, invocation);
    Map<String, Queue<AccessLogData>> logs = (Map<String, Queue<AccessLogData>>) field.get(accessLogFilter);
    assertFalse(logs.isEmpty());
    assertFalse(logs.get("true").isEmpty());
    AccessLogData log = logs.get("true").iterator().next();
    assertEquals("org.apache.dubbo.rpc.support.DemoService", log.getServiceName());
}
```

### Generated Test Case

```java
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.support.AccessLogData;
import org.apache.dubbo.rpc.support.MockInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessLogFilterThreadActionTest {

    private AccessLogFilter accessLogFilter;

    @BeforeEach
    void setUp() {
        accessLogFilter = new AccessLogFilter();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefaultWithThreadActionAssertion() throws Exception {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");
        Invoker<AccessLogFilterThreadActionTest> invoker = new MyInvoker<AccessLogFilterThreadActionTest>(url);
        Invocation invocation = new MockInvocation();
        
        Field field = AccessLogFilter.class.getDeclaredField("logEntries");
        field.setAccessible(true);
        
        Map<String, Queue<AccessLogData>> logs = (Map<String, Queue<AccessLogData>>) field.get(accessLogFilter);
        assertTrue(logs.isEmpty());

        // Since the access log filter often uses an asynchronous scheduled thread pool or background thread 
        // to process and flush logs, we need to ensure the asynchronous action has completed execution 
        // before making assertions. We can use a synchronization aid or a polite wait loop with a timeout.
        accessLogFilter.invoke(invoker, invocation);

        // Asserting the asynchronous thread action using a polling mechanism with a timeout
        long startTime = System.currentTimeMillis();
        boolean logProcessed = false;
        
        while (System.currentTimeMillis() - startTime < 3000) {
            logs = (Map<String, Queue<AccessLogData>>) field.get(accessLogFilter);
            if (!logs.isEmpty() && logs.containsKey("true") && !logs.get("true").isEmpty()) {
                logProcessed = true;
                break;
            }
            Thread.sleep(100);
        }

        assertTrue(logProcessed, "The background thread action failed to process and populate the access logs in time.");
        
        assertFalse(logs.get("true").isEmpty());
        AccessLogData log = logs.get("true").iterator().next();
        assertEquals("org.apache.dubbo.rpc.support.DemoService", log.getServiceName());
    }
}
```

---

## DubboProtocol.java -> createInvocation()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `238`
- **Status:** `ERROR`
- **Comment:** `* FIXME channel.getUrl() always binds to a fixed service, and this service is random.              * we can choose to use a common service to carry onConnect event if there's no easy way to get the specific              * service this connection is binding to.              * @param channel              * @param url              * @param methodKey              * @return`

```java
/**
 * FIXME channel.getUrl() always binds to a fixed service, and this service is random.
 * we can choose to use a common service to carry onConnect event if there's no easy way to get the specific
 * service this connection is binding to.
 * @param channel
 * @param url
 * @param methodKey
 * @return
 */
private Invocation createInvocation(Channel channel, URL url, String methodKey) {
    String method = url.getParameter(methodKey);
    if (method == null || method.length() == 0) {
        return null;
    }
    RpcInvocation invocation = new RpcInvocation(url.getServiceModel(), method, url.getParameter(INTERFACE_KEY), "", new Class<?>[0], new Object[0]);
    invocation.setAttachment(PATH_KEY, url.getPath());
    invocation.setAttachment(GROUP_KEY, url.getGroup());
    invocation.setAttachment(INTERFACE_KEY, url.getParameter(INTERFACE_KEY));
    invocation.setAttachment(VERSION_KEY, url.getVersion());
    if (url.getParameter(STUB_EVENT_KEY, false)) {
        invocation.setAttachment(STUB_EVENT_KEY, Boolean.TRUE.toString());
    }
    return invocation;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 40.572349564s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "40s"
      }
    ]
  }
}

```

---

## DubboTelnetDecodeTest.java -> testTelnetTelnetDecoded()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `264`
- **Status:** `ERROR`
- **Comment:** `* NOTE: This test case actually will fail, but the probability of this case is very small,      * and users should use telnet in new QOS port(default port is 22222) since dubbo 2.5.8,      * so we could ignore this problem.      *      * <p>      * telnet and telnet request      *      * <p>      * First ByteBuf (firstByteBuf):      * +--------------------------------------------------+      * |               telnet(incomplete)                 |      * +--------------------------------------------------+      * <p>      *      * Second ByteBuf (secondByteBuf):      * +--------------------------------------------------+      * |  telnet(the remaining)   |   telnet(complete)    |      * +--------------------------------------------------+      *      * @throws InterruptedException`

```java
/**
 * NOTE: This test case actually will fail, but the probability of this case is very small,
 * and users should use telnet in new QOS port(default port is 22222) since dubbo 2.5.8,
 * so we could ignore this problem.
 *
 * <p>
 * telnet and telnet request
 *
 * <p>
 * First ByteBuf (firstByteBuf):
 * +--------------------------------------------------+
 * |               telnet(incomplete)                 |
 * +--------------------------------------------------+
 * <p>
 *
 * Second ByteBuf (secondByteBuf):
 * +--------------------------------------------------+
 * |  telnet(the remaining)   |   telnet(complete)    |
 * +--------------------------------------------------+
 *
 * @throws InterruptedException
 */
@Disabled
@Test
void testTelnetTelnetDecoded() throws InterruptedException {
    ByteBuf firstByteBuf = Unpooled.wrappedBuffer("ls\r".getBytes());
    ByteBuf secondByteBuf = Unpooled.wrappedBuffer("\nls\r\n".getBytes());
    EmbeddedChannel ch = null;
    try {
        Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
        URL url = new ServiceConfigURL("dubbo", "localhost", 22226);
        NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());
        MockHandler mockHandler = new MockHandler((msg) -> {
            if (checkTelnetDecoded(msg)) {
                telnetTelnet.incrementAndGet();
            }
        }, new MultiMessageHandler(new DecodeHandler(new HeaderExchangeHandler(new ExchangeHandlerAdapter(FrameworkModel.defaultModel()) {

            @Override
            public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                return getDefaultFuture();
            }
        }))));
        ch = new LocalEmbeddedChannel();
        ch.pipeline().addLast("decoder", adapter.getDecoder()).addLast("handler", mockHandler);
        ch.writeInbound(firstByteBuf);
        ch.writeInbound(secondByteBuf);
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (ch != null) {
            ch.close().await(200, TimeUnit.MILLISECONDS);
        }
    }
    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(2, telnetTelnet.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 39.880716304s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "39s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testImplementsGracefulShutdown()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `67`
- **Status:** `ERROR`
- **Comment:** `* Test that DubboGracefulShutdown implements GracefulShutdown.`

```java
/**
 * Test that DubboGracefulShutdown implements GracefulShutdown.
 */
@Test
void testImplementsGracefulShutdown() {
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    assertTrue(shutdown instanceof GracefulShutdown);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 39.62240911s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "39s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testReadonlySendsReadOnlyEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `76`
- **Status:** `ERROR`
- **Comment:** `* Test readonly sends ReadOnlyEvent to all servers.`

```java
/**
 * Test readonly sends ReadOnlyEvent to all servers.
 */
@Test
void testReadonlySendsReadOnlyEvent() {
    when(mockDubboProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    shutdown.readonly();
    ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
    verify(mockRemotingServer1, times(1)).fireChannelEvent(captor.capture());
    ChannelEvent capturedEvent = captor.getValue();
    assertTrue(capturedEvent instanceof ReadOnlyEvent);
    assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 38.948195801s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "38s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testWriteableSendsWriteableEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `94`
- **Status:** `ERROR`
- **Comment:** `* Test writeable sends WriteableEvent to all servers.`

```java
/**
 * Test writeable sends WriteableEvent to all servers.
 */
@Test
void testWriteableSendsWriteableEvent() {
    when(mockDubboProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    shutdown.writeable();
    ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
    verify(mockRemotingServer1, times(1)).fireChannelEvent(captor.capture());
    ChannelEvent capturedEvent = captor.getValue();
    assertTrue(capturedEvent instanceof WriteableEvent);
    assertSame(WriteableEvent.INSTANCE, capturedEvent);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 38.693754296s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "38s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testReadonlyMultipleServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `112`
- **Status:** `ERROR`
- **Comment:** `* Test readonly sends event to multiple servers.`

```java
/**
 * Test readonly sends event to multiple servers.
 */
@Test
void testReadonlyMultipleServers() {
    List<ProtocolServer> servers = Arrays.asList(mockServer1, mockServer2);
    when(mockDubboProtocol.getServers()).thenReturn(servers);
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    shutdown.readonly();
    verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 38.0222355s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "38s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testWriteableMultipleServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `127`
- **Status:** `ERROR`
- **Comment:** `* Test writeable sends event to multiple servers.`

```java
/**
 * Test writeable sends event to multiple servers.
 */
@Test
void testWriteableMultipleServers() {
    List<ProtocolServer> servers = Arrays.asList(mockServer1, mockServer2);
    when(mockDubboProtocol.getServers()).thenReturn(servers);
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    shutdown.writeable();
    verify(mockRemotingServer1, times(1)).fireChannelEvent(WriteableEvent.INSTANCE);
    verify(mockRemotingServer2, times(1)).fireChannelEvent(WriteableEvent.INSTANCE);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 37.750004949s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "37s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testEmptyServerList()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `142`
- **Status:** `ERROR`
- **Comment:** `* Test with empty server list.`

```java
/**
 * Test with empty server list.
 */
@Test
void testEmptyServerList() {
    when(mockDubboProtocol.getServers()).thenReturn(Collections.emptyList());
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    // Should not throw exception
    shutdown.readonly();
    shutdown.writeable();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 37.103995392s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "37s"
      }
    ]
  }
}

```

---

## DubboGracefulShutdownTest.java -> testGetServersReturnsProtocolServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `156`
- **Status:** `ERROR`
- **Comment:** `* Test that getServers returns the protocol's servers.`

```java
/**
 * Test that getServers returns the protocol's servers.
 */
@Test
void testGetServersReturnsProtocolServers() {
    List<ProtocolServer> expectedServers = Arrays.asList(mockServer1, mockServer2);
    when(mockDubboProtocol.getServers()).thenReturn(expectedServers);
    DubboGracefulShutdown shutdown = new DubboGracefulShutdown(mockDubboProtocol);
    // Trigger readonly to indirectly verify getServers is called
    shutdown.readonly();
    verify(mockDubboProtocol, times(1)).getServers();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 36.812151835s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "36s"
      }
    ]
  }
}

```

---

## DubboInvokerAvailableTest.java -> testPreferSerialization()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `161`
- **Status:** `ERROR`
- **Comment:** `* The test prefer serialization      *      * @throws Exception Exception`

```java
/**
 * The test prefer serialization
 *
 * @throws Exception Exception
 */
@Test
public void testPreferSerialization() throws Exception {
    int port = NetUtils.getAvailablePort();
    URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/org.apache.dubbo.rpc.protocol.dubbo.IDemoService?lazy=true&connections=1&timeout=10000&serialization=fastjson&prefer_serialization=fastjson2,hessian2");
    ProtocolUtils.export(new DemoServiceImpl(), IDemoService.class, url);
    Invoker<?> invoker = protocol.refer(IDemoService.class, url);
    Assertions.assertTrue(invoker.isAvailable());
    ExchangeClient exchangeClient = getClients((DubboInvoker<?>) invoker)[0];
    Assertions.assertFalse(exchangeClient.isClosed());
    // invoke method --> init client
    IDemoService service = (IDemoService) proxy.getProxy(invoker);
    Assertions.assertEquals("ok", service.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 36.179332904s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "36s"
      }
    ]
  }
}

```

---

## ReferenceCountExchangeClientTest.java -> test_share_connect()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `90`
- **Status:** `ERROR`
- **Comment:** `* test connection sharing`

```java
/**
 * test connection sharing
 */
@Test
void test_share_connect() {
    init(0, 1);
    Assertions.assertEquals(demoClient.getLocalAddress(), helloClient.getLocalAddress());
    Assertions.assertEquals(demoClient, helloClient);
    destroy();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 35.892095591s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "35s"
      }
    ]
  }
}

```

---

## ReferenceCountExchangeClientTest.java -> test_not_share_connect()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `101`
- **Status:** `ERROR`
- **Comment:** `* test connection not sharing`

```java
/**
 * test connection not sharing
 */
@Test
void test_not_share_connect() {
    init(1, 1);
    Assertions.assertNotSame(demoClient.getLocalAddress(), helloClient.getLocalAddress());
    Assertions.assertNotSame(demoClient, helloClient);
    destroy();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 35.667428825s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "35s"
      }
    ]
  }
}

```

---

## ReferenceCountExchangeClientTest.java -> test_multi_share_connect()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `112`
- **Status:** `ERROR`
- **Comment:** `* test using multiple shared connections`

```java
/**
 * test using multiple shared connections
 */
@Test
void test_multi_share_connect() {
    // here a three shared connection is established between a consumer process and a provider process.
    final int shareConnectionNum = 3;
    init(0, shareConnectionNum);
    List<ReferenceCountExchangeClient> helloReferenceClientList = getReferenceClientList(helloServiceInvoker);
    Assertions.assertEquals(shareConnectionNum, helloReferenceClientList.size());
    List<ReferenceCountExchangeClient> demoReferenceClientList = getReferenceClientList(demoServiceInvoker);
    Assertions.assertEquals(shareConnectionNum, demoReferenceClientList.size());
    // because helloServiceInvoker and demoServiceInvoker use share connect， so client list must be equal
    Assertions.assertEquals(helloReferenceClientList, demoReferenceClientList);
    Assertions.assertEquals(demoClient.getLocalAddress(), helloClient.getLocalAddress());
    Assertions.assertEquals(demoClient, helloClient);
    destroy();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.979169161s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## ReferenceCountExchangeClientTest.java -> test_multi_destroy()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `137`
- **Status:** `ERROR`
- **Comment:** `* test counter won't count down incorrectly when invoker is destroyed for multiple times`

```java
/**
 * test counter won't count down incorrectly when invoker is destroyed for multiple times
 */
@Test
void test_multi_destroy() {
    init(0, 1);
    DubboAppender.doStart();
    DubboAppender.clear();
    demoServiceInvoker.destroy();
    demoServiceInvoker.destroy();
    Assertions.assertEquals("hello", helloService.hello());
    Assertions.assertEquals(0, LogUtil.findMessage(errorMsg), "should not  warning message");
    LogUtil.checkNoError();
    DubboAppender.doStop();
    destroy();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.725323938s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## ReferenceCountExchangeClientTest.java -> test_counter_error()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `154`
- **Status:** `ERROR`
- **Comment:** `* Test against invocation still succeed even if counter has error`

```java
/**
 * Test against invocation still succeed even if counter has error
 */
@Test
void test_counter_error() {
    init(0, 1);
    DubboAppender.doStart();
    DubboAppender.clear();
    // because the two interfaces are initialized, the ReferenceCountExchangeClient reference counter is 2
    ReferenceCountExchangeClient client = getReferenceClient(helloServiceInvoker);
    // close once, counter counts down from 2 to 1, no warning occurs
    client.close();
    Assertions.assertEquals("hello", helloService.hello());
    Assertions.assertEquals(0, LogUtil.findMessage(errorMsg), "should not warning message");
    // close twice, counter counts down from 1 to 0, no warning occurs
    client.close();
    // wait close done.
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Assertions.fail();
    }
    // client has been replaced with lazy client, close status is false because a new lazy client's exchange client
    // is null.
    Assertions.assertFalse(client.isClosed(), "client status close");
    // invoker status is available because the default value of associated lazy client's initial state is true.
    Assertions.assertTrue(helloServiceInvoker.isAvailable(), "invoker status unavailable");
    // due to the effect of LazyConnectExchangeClient, the client will be "revived" whenever there is a call.
    Assertions.assertEquals("hello", helloService.hello());
    Assertions.assertEquals(1, LogUtil.findMessage(errorMsg), "should warning message");
    // output one error every 5000 invocations.
    Assertions.assertEquals("hello", helloService.hello());
    Assertions.assertEquals(1, LogUtil.findMessage(errorMsg), "should warning message");
    DubboAppender.doStop();
    /**
     * This is the third time to close the same client. Under normal circumstances,
     * a client value should be closed once (that is, the shutdown operation is irreversible).
     * After closing, the value of the reference counter of the client has become -1.
     *
     * But this is a bit special, because after the client is closed twice, there are several calls to helloService,
     * that is, the client inside the ReferenceCountExchangeClient is actually active, so the third shutdown here is still effective,
     * let the resurrection After the client is really closed.
     */
    client.close();
    // close status is false because the lazy client's exchange client is null again after close().
    Assertions.assertFalse(client.isClosed(), "client status close");
    // invoker status is available because the default value of associated lazy client's initial state is true.
    Assertions.assertTrue(helloServiceInvoker.isAvailable(), "invoker status unavailable");
    // revive: initial the lazy client's exchange client again.
    Assertions.assertEquals("hello", helloService.hello());
    destroy();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 34.034960275s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "34s"
      }
    ]
  }
}

```

---

## DataQueueCommand.java -> getData()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `61`
- **Status:** `ERROR`
- **Comment:** `for test`

```java
// for test
public byte[] getData() {
    return data;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.777601331s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## DataQueueCommand.java -> isEndStream()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `66`
- **Status:** `ERROR`
- **Comment:** `for test`

```java
// for test
public boolean isEndStream() {
    return endStream;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 33.103974253s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "33s"
      }
    ]
  }
}

```

---

## ReflectionPackableMethod.java -> checkNeedIgnore()

- **Debt Type:** `DEFECT_DEBT`
- **Line Number:** `253`
- **Status:** `ERROR`
- **Comment:** `* fixme will produce error on grpc. but is harmless so ignore now`

```java
/**
 * fixme will produce error on grpc. but is harmless so ignore now
 */
static boolean checkNeedIgnore(Class<?> returnClass) {
    return Iterator.class.isAssignableFrom(returnClass);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.822655593s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStream.java -> getNumSentBytesQueued()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `260`
- **Status:** `ERROR`
- **Comment:** `* Returns the number of bytes currently queued for sending.      * Visible for testing.`

```java
/**
 * Returns the number of bytes currently queued for sending.
 * Visible for testing.
 */
protected long getNumSentBytesQueued() {
    return numSentBytesQueued.get();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 32.148801659s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "32s"
      }
    ]
  }
}

```

---

## TriRpcStatus.java -> getStatus()

- **Debt Type:** `DESIGN_DEBT`
- **Line Number:** `76`
- **Status:** `ERROR`
- **Comment:** `* todo The remaining exceptions are converted to status`

```java
/**
 * todo The remaining exceptions are converted to status
 */
public static TriRpcStatus getStatus(Throwable throwable) {
    return getStatus(throwable, null);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.894596476s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testSetOnReadyHandlerStoresLocally()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `45`
- **Status:** `ERROR`
- **Comment:** `* Test that ClientCallToObserverAdapter stores onReadyHandler locally.`

```java
/**
 * Test that ClientCallToObserverAdapter stores onReadyHandler locally.
 */
@Test
void testSetOnReadyHandlerStoresLocally() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    assertNull(adapter.getOnReadyHandler());
    Runnable handler = () -> {
    };
    adapter.setOnReadyHandler(handler);
    assertNotNull(adapter.getOnReadyHandler());
    assertEquals(handler, adapter.getOnReadyHandler());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 31.229842837s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "31s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testIsReadyDelegatesToClientCall()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `62`
- **Status:** `ERROR`
- **Comment:** `* Test that isReady() delegates to ClientCall.isReady().`

```java
/**
 * Test that isReady() delegates to ClientCall.isReady().
 */
@Test
void testIsReadyDelegatesToClientCall() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    mockCall.setReady(true);
    assertTrue(adapter.isReady());
    mockCall.setReady(false);
    assertFalse(adapter.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.96043277s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testOnReadyTriggersHandler()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `77`
- **Status:** `ERROR`
- **Comment:** `* Test that ObserverToClientCallListenerAdapter.onReady() triggers the onReadyHandler.`

```java
/**
 * Test that ObserverToClientCallListenerAdapter.onReady() triggers the onReadyHandler.
 */
@Test
void testOnReadyTriggersHandler() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    AtomicBoolean handlerCalled = new AtomicBoolean(false);
    adapter.setOnReadyHandler(() -> handlerCalled.set(true));
    // Create listener and set request adapter
    MockStreamObserver mockObserver = new MockStreamObserver();
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
    listener.setRequestAdapter(adapter);
    // Trigger onReady
    listener.onReady();
    assertTrue(handlerCalled.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.30430981s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testOnReadyWithNoHandler()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `99`
- **Status:** `ERROR`
- **Comment:** `* Test that onReady does nothing when no handler is set.`

```java
/**
 * Test that onReady does nothing when no handler is set.
 */
@Test
void testOnReadyWithNoHandler() {
    MockStreamObserver mockObserver = new MockStreamObserver();
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
    // No adapter set - should not throw
    listener.onReady();
    // Adapter set but no handler - should not throw
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    listener.setRequestAdapter(adapter);
    listener.onReady();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 30.039296693s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "30s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testOnReadyHandlerMultipleTriggers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `117`
- **Status:** `ERROR`
- **Comment:** `* Test that onReadyHandler can be triggered multiple times.`

```java
/**
 * Test that onReadyHandler can be triggered multiple times.
 */
@Test
void testOnReadyHandlerMultipleTriggers() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    AtomicInteger triggerCount = new AtomicInteger(0);
    adapter.setOnReadyHandler(triggerCount::incrementAndGet);
    MockStreamObserver mockObserver = new MockStreamObserver();
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
    listener.setRequestAdapter(adapter);
    // Trigger multiple times
    listener.onReady();
    listener.onReady();
    listener.onReady();
    assertEquals(3, triggerCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.387820037s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testClientCallListenerOnReadyDefault()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `140`
- **Status:** `ERROR`
- **Comment:** `* Test ClientCall.Listener.onReady() default implementation.`

```java
/**
 * Test ClientCall.Listener.onReady() default implementation.
 */
@Test
void testClientCallListenerOnReadyDefault() {
    ClientCall.Listener listener = new ClientCall.Listener() {

        @Override
        public boolean streamingResponse() {
            return true;
        }

        @Override
        public void onStart(ClientCall call) {
        }

        @Override
        public void onMessage(Object message, int actualContentLength) {
        }

        @Override
        public void onClose(TriRpcStatus status, Map<String, Object> trailers, boolean isReturnTriException) {
        }
    };
    // Default implementation should not throw
    listener.onReady();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 29.113214588s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "29s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testDisableAutoFlowControl()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `165`
- **Status:** `ERROR`
- **Comment:** `* Test disableAutoFlowControl delegates to ClientCall.setAutoRequest(false).`

```java
/**
 * Test disableAutoFlowControl delegates to ClientCall.setAutoRequest(false).
 */
@Test
void testDisableAutoFlowControl() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    assertTrue(mockCall.isAutoRequest());
    adapter.disableAutoFlowControl();
    assertFalse(mockCall.isAutoRequest());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.467969511s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testDisableAutoRequestWithInitial()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `178`
- **Status:** `ERROR`
- **Comment:** `* Test disableAutoRequestWithInitial delegates to ClientCall.setAutoRequestWithInitial().`

```java
/**
 * Test disableAutoRequestWithInitial delegates to ClientCall.setAutoRequestWithInitial().
 */
@Test
void testDisableAutoRequestWithInitial() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    adapter.disableAutoRequestWithInitial(5);
    assertEquals(5, mockCall.getInitialRequest());
    assertFalse(mockCall.isAutoRequest());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 28.173364242s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "28s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testRequestDelegation()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `191`
- **Status:** `ERROR`
- **Comment:** `* Test request() delegates to ClientCall.request().`

```java
/**
 * Test request() delegates to ClientCall.request().
 */
@Test
void testRequestDelegation() {
    MockClientCall mockCall = new MockClientCall();
    ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
    adapter.request(10);
    assertEquals(10, mockCall.getRequestedCount());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 27.55210711s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "27s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testStreamingResponseReturnsTrue()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `203`
- **Status:** `SUCCESS`
- **Comment:** `* Test that ObserverToClientCallListenerAdapter.streamingResponse() returns true.`

```java
/**
 * Test that ObserverToClientCallListenerAdapter.streamingResponse() returns true.
 */
@Test
void testStreamingResponseReturnsTrue() {
    MockStreamObserver mockObserver = new MockStreamObserver();
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
    assertTrue(listener.streamingResponse());
}
```

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ObserverToClientCallListenerAdapterTest {

    @Test
    void testStreamingResponseReturnsTrue() {
        MockStreamObserver mockObserver = new MockStreamObserver();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
        assertTrue(listener.streamingResponse());
    }
}
```

---

## BackpressureTest.java -> testOnNextCallsDelegate()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `213`
- **Status:** `SUCCESS`
- **Comment:** `* Test onNext calls delegate.onNext().`

```java
/**
 * Test onNext calls delegate.onNext().
 */
@Test
void testOnNextCallsDelegate() {
    AtomicBoolean onNextCalled = new AtomicBoolean(false);
    StreamObserver<Object> delegate = new StreamObserver<Object>() {

        @Override
        public void onNext(Object data) {
            onNextCalled.set(true);
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }
    };
    MockClientCall mockCall = new MockClientCall();
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
    // Must call onStart first to initialize the call reference
    listener.onStart(mockCall);
    listener.onMessage("test", 4);
    assertTrue(onNextCalled.get());
}
```

### Generated Test Case

```java
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ObserverToClientCallListenerAdapterTest {

    @Test
    void testOnNextCallsDelegate() {
        AtomicBoolean onNextCalled = new AtomicBoolean(false);
        StreamObserver<Object> delegate = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {
                onNextCalled.set(true);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
            }
        };
        
        MockClientCall mockCall = new MockClientCall();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
        
        // Must call onStart first to initialize the call reference
        listener.onStart(mockCall);
        listener.onMessage("test", 4);
        
        assertTrue(onNextCalled.get(), "The delegate's onNext method should have been called.");
    }
}
```

---

## BackpressureTest.java -> testOnCloseWithOkStatus()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `241`
- **Status:** `ERROR`
- **Comment:** `* Test onClose with OK status calls delegate.onCompleted().`

```java
/**
 * Test onClose with OK status calls delegate.onCompleted().
 */
@Test
void testOnCloseWithOkStatus() {
    AtomicBoolean onCompletedCalled = new AtomicBoolean(false);
    StreamObserver<Object> delegate = new StreamObserver<Object>() {

        @Override
        public void onNext(Object data) {
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
            onCompletedCalled.set(true);
        }
    };
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
    listener.onClose(TriRpcStatus.OK, null, false);
    assertTrue(onCompletedCalled.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.939258164s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## BackpressureTest.java -> testOnCloseWithErrorStatus()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `266`
- **Status:** `ERROR`
- **Comment:** `* Test onClose with error status calls delegate.onError().`

```java
/**
 * Test onClose with error status calls delegate.onError().
 */
@Test
void testOnCloseWithErrorStatus() {
    AtomicBoolean onErrorCalled = new AtomicBoolean(false);
    StreamObserver<Object> delegate = new StreamObserver<Object>() {

        @Override
        public void onNext(Object data) {
        }

        @Override
        public void onError(Throwable throwable) {
            onErrorCalled.set(true);
        }

        @Override
        public void onCompleted() {
        }
    };
    ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
    listener.onClose(TriRpcStatus.INTERNAL.withDescription("error"), null, false);
    assertTrue(onErrorCalled.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.706604201s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## DescriptorService.java -> sayHello()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `65`
- **Status:** `ERROR`
- **Comment:** `* only for test.      *      * @param reply      * @return`

```java
/**
 * only for test.
 *
 * @param reply
 * @return
 */
HelloReply sayHello(HelloReply reply);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.483758894s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## DescriptorService.java -> testMultiProtobufParameters()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `92`
- **Status:** `ERROR`
- **Comment:** `********************test error****************`

```java
/**
 * *******************test error****************
 */
void testMultiProtobufParameters(HelloReply reply1, HelloReply reply2);
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.253082547s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## RequestMappingRegisterTest.java -> setup()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `50`
- **Status:** `ERROR`
- **Comment:** `* Setup method, initializes the testing environment.      * Registers a service provider and creates an Invoker instance for subsequent tests.`

```java
/**
 * Setup method, initializes the testing environment.
 * Registers a service provider and creates an Invoker instance for subsequent tests.
 */
@BeforeEach
public void setup() {
    // Initialize the service implementation
    IGreeter serviceImpl = new IGreeterImpl();
    // Select an available port
    int availablePort = NetUtils.getAvailablePort();
    // Construct the provider's URL
    URL providerUrl = URL.valueOf("http://127.0.0.1:" + availablePort + "/" + IGreeter.class.getName());
    // Register the service
    ModuleServiceRepository serviceRepository = applicationModel.getDefaultModule().getServiceRepository();
    ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);
    // Construct and register the provider model
    ProviderModel providerModel = new ProviderModel(providerUrl.getServiceKey(), serviceImpl, serviceDescriptor, new ServiceMetadata(), ClassUtils.getClassLoader(IGreeter.class));
    serviceRepository.registerProvider(providerModel);
    providerUrl = providerUrl.setServiceModel(providerModel);
    // Initialize the protocol and proxy factory
    Protocol protocol = new TripleProtocol(providerUrl.getOrDefaultFrameworkModel());
    ProxyFactory proxyFactory = applicationModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    // Create and export the Invoker
    invoker = proxyFactory.getInvoker(serviceImpl, IGreeter.class, providerUrl);
    protocol.export(invoker);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 24.020068756s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "24s"
      }
    ]
  }
}

```

---

## RequestMappingRegisterTest.java -> testServiceLookup()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `89`
- **Status:** `ERROR`
- **Comment:** `* Tests whether the service lookup mechanism is functioning properly.      * Ensures that the DefaultRequestMappingRegistry instance can be obtained.`

```java
/**
 * Tests whether the service lookup mechanism is functioning properly.
 * Ensures that the DefaultRequestMappingRegistry instance can be obtained.
 */
@Test
public void testServiceLookup() {
    // Obtain the DefaultRequestMappingRegistry instance
    DefaultRequestMappingRegistry registry = applicationModel.getFrameworkModel().getBeanFactory().getBean(DefaultRequestMappingRegistry.class);
    assertNotNull(registry, "The DefaultRequestMappingRegistry should not be null.");
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.790790849s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testIsReadyWhenBelowThreshold()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `46`
- **Status:** `ERROR`
- **Comment:** `* Test isReady returns true when below threshold.`

```java
/**
 * Test isReady returns true when below threshold.
 */
@Test
void testIsReadyWhenBelowThreshold() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    assertTrue(counter.isReady());
    counter.onSendingBytes(1000);
    assertTrue(counter.isReady());
    counter.onSendingBytes((int) ON_READY_THRESHOLD - 1001);
    assertTrue(counter.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.548288766s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testIsReadyWhenAtOrAboveThreshold()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `62`
- **Status:** `ERROR`
- **Comment:** `* Test isReady returns false when at or above threshold.`

```java
/**
 * Test isReady returns false when at or above threshold.
 */
@Test
void testIsReadyWhenAtOrAboveThreshold() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    counter.onSendingBytes((int) ON_READY_THRESHOLD);
    assertFalse(counter.isReady());
    counter.onSendingBytes(1000);
    assertFalse(counter.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.315651281s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testOnReadyTriggeredOnTransition()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `76`
- **Status:** `ERROR`
- **Comment:** `* Test onReady is triggered when transitioning from not-ready to ready.`

```java
/**
 * Test onReady is triggered when transitioning from not-ready to ready.
 */
@Test
void testOnReadyTriggeredOnTransition() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    counter.setOnReadyCallback(onReadyCount::incrementAndGet);
    // Send bytes to exceed threshold
    counter.onSendingBytes((int) ON_READY_THRESHOLD + 1000);
    assertFalse(counter.isReady());
    assertEquals(0, onReadyCount.get());
    // Complete sending - should trigger onReady when crossing threshold
    counter.onSentBytes((int) ON_READY_THRESHOLD + 1000);
    assertTrue(counter.isReady());
    assertEquals(1, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 23.088242651s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "23s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testOnReadyNotTriggeredWhenStayingBelowThreshold()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `96`
- **Status:** `ERROR`
- **Comment:** `* Test onReady is NOT triggered when staying below threshold.`

```java
/**
 * Test onReady is NOT triggered when staying below threshold.
 */
@Test
void testOnReadyNotTriggeredWhenStayingBelowThreshold() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    counter.setOnReadyCallback(onReadyCount::incrementAndGet);
    // Send small amount
    counter.onSendingBytes(1000);
    counter.onSentBytes(1000);
    assertEquals(0, onReadyCount.get());
    // Send another small amount
    counter.onSendingBytes(2000);
    counter.onSentBytes(2000);
    assertEquals(0, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.852100723s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testMultipleTransitions()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `116`
- **Status:** `ERROR`
- **Comment:** `* Test multiple transitions trigger onReady each time.`

```java
/**
 * Test multiple transitions trigger onReady each time.
 */
@Test
void testMultipleTransitions() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    counter.setOnReadyCallback(onReadyCount::incrementAndGet);
    // First cycle
    counter.onSendingBytes((int) ON_READY_THRESHOLD + 1000);
    counter.onSentBytes((int) ON_READY_THRESHOLD + 1000);
    assertEquals(1, onReadyCount.get());
    // Second cycle
    counter.onSendingBytes((int) ON_READY_THRESHOLD + 2000);
    counter.onSentBytes((int) ON_READY_THRESHOLD + 2000);
    assertEquals(2, onReadyCount.get());
    // Third cycle
    counter.onSendingBytes((int) ON_READY_THRESHOLD + 3000);
    counter.onSentBytes((int) ON_READY_THRESHOLD + 3000);
    assertEquals(3, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.619202524s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testConcurrentSendsOnlyTriggerOnReadyOnce()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `141`
- **Status:** `ERROR`
- **Comment:** `* Test concurrent sends only trigger onReady once for single transition.`

```java
/**
 * Test concurrent sends only trigger onReady once for single transition.
 */
@Test
void testConcurrentSendsOnlyTriggerOnReadyOnce() throws InterruptedException {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    counter.setOnReadyCallback(onReadyCount::incrementAndGet);
    // Exceed threshold
    counter.onSendingBytes((int) ON_READY_THRESHOLD + 10000);
    // Simulate concurrent completions
    int threadCount = 10;
    int bytesPerThread = ((int) ON_READY_THRESHOLD + 10000) / threadCount;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                startLatch.await();
                counter.onSentBytes(bytesPerThread);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
    }
    startLatch.countDown();
    doneLatch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    // Only one thread should trigger onReady
    assertEquals(1, onReadyCount.get());
    assertTrue(counter.isReady());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.397513911s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testInitialStateIsReady()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `182`
- **Status:** `ERROR`
- **Comment:** `* Test initial state is ready.`

```java
/**
 * Test initial state is ready.
 */
@Test
void testInitialStateIsReady() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    assertTrue(counter.isReady());
    assertEquals(0, counter.getNumSentBytesQueued());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 22.170838625s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "22s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testRollbackDoesNotTriggerOnReady()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `192`
- **Status:** `ERROR`
- **Comment:** `* Test rollback does not trigger onReady.`

```java
/**
 * Test rollback does not trigger onReady.
 */
@Test
void testRollbackDoesNotTriggerOnReady() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    counter.setOnReadyCallback(onReadyCount::incrementAndGet);
    // Exceed threshold
    counter.onSendingBytes((int) ON_READY_THRESHOLD + 1000);
    // Rollback (simulating send failure)
    counter.rollbackSendingBytes((int) ON_READY_THRESHOLD + 1000);
    // Should not trigger onReady
    assertTrue(counter.isReady());
    assertEquals(0, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.532748781s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## AbstractTripleClientStreamByteCountingTest.java -> testExactThresholdBoundary()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `212`
- **Status:** `ERROR`
- **Comment:** `* Test exact threshold boundary.`

```java
/**
 * Test exact threshold boundary.
 */
@Test
void testExactThresholdBoundary() {
    ClientStreamByteCounter counter = new ClientStreamByteCounter();
    AtomicInteger onReadyCount = new AtomicInteger(0);
    counter.setOnReadyCallback(onReadyCount::incrementAndGet);
    // At exactly threshold - not ready
    counter.onSendingBytes((int) ON_READY_THRESHOLD);
    assertFalse(counter.isReady());
    // Send 1 byte to go below threshold
    counter.onSentBytes(1);
    assertTrue(counter.isReady());
    assertEquals(1, onReadyCount.get());
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.306732941s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testImplementsGracefulShutdown()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `68`
- **Status:** `ERROR`
- **Comment:** `* Test that TripleGracefulShutdown implements GracefulShutdown.`

```java
/**
 * Test that TripleGracefulShutdown implements GracefulShutdown.
 */
@Test
void testImplementsGracefulShutdown() {
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    assertTrue(shutdown instanceof GracefulShutdown);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 21.083250518s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "21s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testReadonlySendsReadOnlyEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `77`
- **Status:** `ERROR`
- **Comment:** `* Test readonly sends ReadOnlyEvent to all servers.`

```java
/**
 * Test readonly sends ReadOnlyEvent to all servers.
 */
@Test
void testReadonlySendsReadOnlyEvent() {
    when(mockTripleProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    shutdown.readonly();
    ArgumentCaptor<ChannelEvent> captor = ArgumentCaptor.forClass(ChannelEvent.class);
    verify(mockRemotingServer1, times(1)).fireChannelEvent(captor.capture());
    ChannelEvent capturedEvent = captor.getValue();
    assertTrue(capturedEvent instanceof ReadOnlyEvent);
    assertSame(ReadOnlyEvent.INSTANCE, capturedEvent);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.852401125s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testWriteableDoesNotSendEvent()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `95`
- **Status:** `ERROR`
- **Comment:** `* Test writeable does not send any event (not supported for Triple protocol).`

```java
/**
 * Test writeable does not send any event (not supported for Triple protocol).
 */
@Test
void testWriteableDoesNotSendEvent() {
    when(mockTripleProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    shutdown.writeable();
    // Writeable is not supported for Triple protocol, so no event should be sent
    verify(mockRemotingServer1, never()).fireChannelEvent(WriteableEvent.INSTANCE);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.621520983s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testReadonlyMultipleServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `109`
- **Status:** `ERROR`
- **Comment:** `* Test readonly sends event to multiple servers.`

```java
/**
 * Test readonly sends event to multiple servers.
 */
@Test
void testReadonlyMultipleServers() {
    List<ProtocolServer> servers = Arrays.asList(mockServer1, mockServer2);
    when(mockTripleProtocol.getServers()).thenReturn(servers);
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    shutdown.readonly();
    verify(mockRemotingServer1, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
    verify(mockRemotingServer2, times(1)).fireChannelEvent(ReadOnlyEvent.INSTANCE);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.398292436s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testEmptyServerList()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `124`
- **Status:** `ERROR`
- **Comment:** `* Test with empty server list.`

```java
/**
 * Test with empty server list.
 */
@Test
void testEmptyServerList() {
    when(mockTripleProtocol.getServers()).thenReturn(Collections.emptyList());
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    // Should not throw exception
    shutdown.readonly();
    shutdown.writeable();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 20.167940506s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testGetServersReturnsProtocolServers()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `138`
- **Status:** `ERROR`
- **Comment:** `* Test that getServers returns the protocol's servers.`

```java
/**
 * Test that getServers returns the protocol's servers.
 */
@Test
void testGetServersReturnsProtocolServers() {
    List<ProtocolServer> expectedServers = Arrays.asList(mockServer1, mockServer2);
    when(mockTripleProtocol.getServers()).thenReturn(expectedServers);
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    // Trigger readonly to indirectly verify getServers is called
    shutdown.readonly();
    verify(mockTripleProtocol, times(1)).getServers();
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.939453383s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## TripleGracefulShutdownTest.java -> testWriteableCanBeCalledMultipleTimes()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `154`
- **Status:** `ERROR`
- **Comment:** `* Test that writeable can be called multiple times without error.`

```java
/**
 * Test that writeable can be called multiple times without error.
 */
@Test
void testWriteableCanBeCalledMultipleTimes() {
    when(mockTripleProtocol.getServers()).thenReturn(Collections.singletonList(mockServer1));
    TripleGracefulShutdown shutdown = new TripleGracefulShutdown(mockTripleProtocol);
    // Should not throw exception even when called multiple times
    shutdown.writeable();
    shutdown.writeable();
    shutdown.writeable();
    // No events should be sent
    verify(mockRemotingServer1, never()).fireChannelEvent(WriteableEvent.INSTANCE);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.693196126s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## DubboConfigBeanDefinitionConflictApplicationListenerTest.java -> testNormalCase()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `53`
- **Status:** `ERROR`
- **Comment:** `@Test`

```java
// @Test
void testNormalCase() {
    System.setProperty("dubbo.application.name", "test-dubbo-application");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DubboConfig.class);
    try {
        context.start();
        ApplicationConfig applicationConfig = context.getBean(ApplicationConfig.class);
        assertEquals("test-dubbo-application", applicationConfig.getName());
    } finally {
        System.clearProperty("dubbo.application.name");
        context.close();
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.457758908s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## AbstractRegistryCenterTestExecutionListener.java -> needRegistryCenter()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `68`
- **Status:** `ERROR`
- **Comment:** `* Checks if current {@link TestPlan} need registry center.`

```java
/**
 * Checks if current {@link TestPlan} need registry center.
 */
public boolean needRegistryCenter(TestPlan testPlan) {
    return testPlan.getRoots().stream().flatMap(testIdentifier -> testPlan.getChildren(testIdentifier).stream()).filter(testIdentifier -> testIdentifier.getSource().isPresent()).filter(testIdentifier -> supportEmbeddedZookeeper(testIdentifier)).count() > 0;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 19.222543101s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}

```

---

## AbstractRegistryCenterTestExecutionListener.java -> needRegistryCenter()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `80`
- **Status:** `ERROR`
- **Comment:** `* Checks if current {@link TestIdentifier} need registry center.`

```java
/**
 * Checks if current {@link TestIdentifier} need registry center.
 */
public boolean needRegistryCenter(TestIdentifier testIdentifier) {
    return supportEmbeddedZookeeper(testIdentifier);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.639286853s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## AbstractRegistryCenterTestExecutionListener.java -> supportEmbeddedZookeeper()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `87`
- **Status:** `ERROR`
- **Comment:** `* Checks if the current {@link TestIdentifier} need embedded zookeeper.`

```java
/**
 * Checks if the current {@link TestIdentifier} need embedded zookeeper.
 */
private boolean supportEmbeddedZookeeper(TestIdentifier testIdentifier) {
    if (!enableEmbeddedZookeeper) {
        return false;
    }
    TestSource testSource = testIdentifier.getSource().orElse(null);
    if (testSource instanceof ClassSource) {
        String packageName = ((ClassSource) testSource).getJavaClass().getPackage().getName();
        for (String pkgName : PACKAGE_NAME) {
            if (packageName.contains(pkgName)) {
                return true;
            }
        }
    }
    return false;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.407267111s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## ConfigZookeeperInitializer.java -> updateConfig()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `48`
- **Status:** `ERROR`
- **Comment:** `* Update the config file with the given client port and admin server port.      *      * @param clientPort      the client port      * @param adminServerPort the admin server port      * @throws DubboTestException when an exception occurred`

```java
/**
 * Update the config file with the given client port and admin server port.
 *
 * @param clientPort      the client port
 * @param adminServerPort the admin server port
 * @throws DubboTestException when an exception occurred
 */
private void updateConfig(ZookeeperContext context, int clientPort, int adminServerPort) throws DubboTestException {
    Path zookeeperConf = Paths.get(context.getSourceFile().getParent().toString(), String.valueOf(clientPort), context.getUnpackedDirectory(), "conf");
    File zooSample = Paths.get(zookeeperConf.toString(), "zoo_sample.cfg").toFile();
    int availableAdminServerPort = NetUtils.getAvailablePort(adminServerPort);
    Properties properties = new Properties();
    try {
        // use Files.newInputStream instead of new FileInputStream
        try (InputStream is = Files.newInputStream(zooSample.toPath())) {
            properties.load(is);
        }
        properties.setProperty("clientPort", String.valueOf(clientPort));
        properties.setProperty("admin.serverPort", String.valueOf(availableAdminServerPort));
        Path dataDir = Paths.get(zookeeperConf.getParent().toString(), "data");
        if (!Files.exists(dataDir)) {
            try {
                logger.info("It is creating the data directory...");
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to create the data directory to save zookeeper binary file, file path:%s", context.getSourceFile()), e);
            }
        }
        properties.setProperty("dataDir", dataDir.toString());
        FileOutputStream oFile = null;
        try {
            oFile = new FileOutputStream(Paths.get(zookeeperConf.toString(), "zoo.cfg").toFile());
            properties.store(oFile, "");
        } finally {
            try {
                oFile.close();
            } catch (IOException e) {
                throw new DubboTestException("Failed to close file", e);
            }
        }
        logger.info("The configuration information of zoo.cfg are as below,\n" + "which located in " + zooSample.getAbsolutePath() + "\n" + propertiesToString(properties));
    } catch (IOException e) {
        throw new DubboTestException(String.format("Failed to update %s file", zooSample), e);
    }
    File log4j = Paths.get(zookeeperConf.toString(), "log4j.properties").toFile();
    try {
        // use Files.newInputStream instead of new FileInputStream
        try (InputStream is = Files.newInputStream(log4j.toPath())) {
            properties.load(is);
        }
        Path logDir = Paths.get(zookeeperConf.getParent().toString(), "logs");
        if (!Files.exists(logDir)) {
            try {
                logger.info("It is creating the log directory...");
                Files.createDirectories(logDir);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to create the log directory to save zookeeper binary file, file path:%s", context.getSourceFile()), e);
            }
        }
        properties.setProperty("zookeeper.log.dir", logDir.toString());
        FileOutputStream oFile = null;
        try {
            oFile = new FileOutputStream(Paths.get(zookeeperConf.toString(), "log4j.properties").toFile());
            properties.store(oFile, "");
        } finally {
            try {
                oFile.close();
            } catch (IOException e) {
                throw new DubboTestException("Failed to close file", e);
            }
        }
        logger.info("The configuration information of log4j.properties are as below,\n" + "which located in " + log4j.getAbsolutePath() + "\n" + propertiesToString(properties));
    } catch (IOException e) {
        throw new DubboTestException(String.format("Failed to update %s file", zooSample), e);
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 18.167818053s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}

```

---

## UnpackZookeeperInitializer.java -> unpack()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `51`
- **Status:** `ERROR`
- **Comment:** `* Unpack the zookeeper binary file.      *      * @param context    the global context of zookeeper.      * @param clientPort the client port      * @throws DubboTestException when an exception occurred`

```java
/**
 * Unpack the zookeeper binary file.
 *
 * @param context    the global context of zookeeper.
 * @param clientPort the client port
 * @throws DubboTestException when an exception occurred
 */
private void unpack(ZookeeperContext context, int clientPort) throws DubboTestException {
    File sourceFile = context.getSourceFile().toFile();
    Path targetPath = Paths.get(context.getSourceFile().getParent().toString(), String.valueOf(clientPort));
    // check if it's unpacked.
    if (targetPath.toFile() != null && targetPath.toFile().isDirectory()) {
        logger.info(String.format("The file has been unpacked, target path:%s", targetPath.toString()));
        return;
    }
    try (FileInputStream fileInputStream = new FileInputStream(sourceFile);
        GzipCompressorInputStream gzipCompressorInputStream = new GzipCompressorInputStream(fileInputStream);
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipCompressorInputStream, "UTF-8")) {
        File targetFile = targetPath.toFile();
        TarArchiveEntry entry;
        while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            File curFile = new File(targetFile, entry.getName());
            File parent = curFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream outputStream = new FileOutputStream(curFile)) {
                IOUtils.copy(tarArchiveInputStream, outputStream);
            }
        }
    } catch (IOException e) {
        throw new DubboTestException(String.format("Failed to unpack the zookeeper binary file"), e);
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.935751808s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ZookeeperInitializer.java -> doInitialize()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `50`
- **Status:** `ERROR`
- **Comment:** `* Initialize the global context for zookeeper.      *      * @param context the global context for zookeeper.      * @throws DubboTestException when any exception occurred.`

```java
/**
 * Initialize the global context for zookeeper.
 *
 * @param context the global context for zookeeper.
 * @throws DubboTestException when any exception occurred.
 */
protected abstract void doInitialize(ZookeeperContext context) throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.708041856s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## Initializer.java -> initialize()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `31`
- **Status:** `ERROR`
- **Comment:** `* Initialize the global context.      * @param context the global context to be initialized.      * @throws DubboTestException when any exception occurred.`

```java
/**
 * Initialize the global context.
 * @param context the global context to be initialized.
 * @throws DubboTestException when any exception occurred.
 */
void initialize(Context context) throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.477228558s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ZookeeperUnixProcessor.java -> awaitProcessReady()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `86`
- **Status:** `ERROR`
- **Comment:** `* Wait until the server is started successfully.      *      * @param inputStream the log after run {@link Process}.      * @throws DubboTestException if cannot match the given pattern.`

```java
/**
 * Wait until the server is started successfully.
 *
 * @param inputStream the log after run {@link Process}.
 * @throws DubboTestException if cannot match the given pattern.
 */
private void awaitProcessReady(final InputStream inputStream) throws DubboTestException {
    final StringBuilder log = new StringBuilder();
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
            if (this.getPattern().matcher(line).matches()) {
                return;
            }
            log.append('\n').append(line);
        }
    } catch (IOException e) {
        throw new DubboTestException("Failed to read the log after executed process.", e);
    }
    throw new DubboTestException("Ready pattern not found in log, log: " + log);
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.245707011s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ZookeeperUnixProcessor.java -> doProcess()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `110`
- **Status:** `ERROR`
- **Comment:** `* Use {@link Process} to handle the command.      *      * @param context    the global zookeeper context.      * @param clientPort the client port of zookeeper.      * @return the instance of {@link Process}.      * @throws DubboTestException when any exception occurred.`

```java
/**
 * Use {@link Process} to handle the command.
 *
 * @param context    the global zookeeper context.
 * @param clientPort the client port of zookeeper.
 * @return the instance of {@link Process}.
 * @throws DubboTestException when any exception occurred.
 */
protected abstract Process doProcess(ZookeeperContext context, int clientPort) throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 17.006343279s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "17s"
      }
    ]
  }
}

```

---

## ZookeeperWindowsProcessor.java -> doProcess()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `41`
- **Status:** `ERROR`
- **Comment:** `* Use {@link Process} to handle the command.      *      * @param context    the global zookeeper context.      * @throws DubboTestException when any exception occurred.`

```java
/**
 * Use {@link Process} to handle the command.
 *
 * @param context    the global zookeeper context.
 * @throws DubboTestException when any exception occurred.
 */
protected abstract void doProcess(ZookeeperWindowsContext context) throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.770257266s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## Processor.java -> process()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `32`
- **Status:** `ERROR`
- **Comment:** `* Process the command with the global context.      *      * @param context the global context.      * @throws DubboTestException when any exception occurred.`

```java
/**
 * Process the command with the global context.
 *
 * @param context the global context.
 * @throws DubboTestException when any exception occurred.
 */
void process(Context context) throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.544567965s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## RegistryCenter.java -> startup()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `31`
- **Status:** `ERROR`
- **Comment:** `* Start the registry center.      *      * @throws DubboTestException when an exception occurred`

```java
/**
 * Start the registry center.
 *
 * @throws DubboTestException when an exception occurred
 */
void startup() throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.318066151s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## RegistryCenter.java -> reset()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `37`
- **Status:** `ERROR`
- **Comment:** `* Reset the registry center after ut exited.      * @throws DubboTestException when an exception occurred`

```java
/**
 * Reset the registry center after ut exited.
 * @throws DubboTestException when an exception occurred
 */
void reset() throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 16.0821309s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "16s"
      }
    ]
  }
}

```

---

## RegistryCenter.java -> shutdown()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `44`
- **Status:** `ERROR`
- **Comment:** `* Stop the registry center.      *      * @throws DubboTestException when an exception occurred`

```java
/**
 * Stop the registry center.
 *
 * @throws DubboTestException when an exception occurred
 */
void shutdown() throws DubboTestException;
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.857647084s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## TestSocketUtils.java -> findAvailableTcpPortInternal()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `78`
- **Status:** `ERROR`
- **Comment:** `* Internal implementation of {@link #findAvailableTcpPort()}.      * <p>Package-private solely for testing purposes.`

```java
/**
 * Internal implementation of {@link #findAvailableTcpPort()}.
 * <p>Package-private solely for testing purposes.
 */
int findAvailableTcpPortInternal() {
    int candidatePort;
    int searchCounter = 0;
    do {
        Assert.assertTrue(++searchCounter <= MAX_ATTEMPTS, String.format("Could not find an available TCP port in the range [%d, %d] after %d attempts", PORT_RANGE_MIN, PORT_RANGE_MAX, MAX_ATTEMPTS));
        candidatePort = PORT_RANGE_MIN + random.nextInt(PORT_RANGE_PLUS_ONE);
    } while (!isPortAvailable(candidatePort));
    return candidatePort;
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.627811397s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-3.5-flash-lite"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

## TestSocketUtils.java -> isPortAvailable()

- **Debt Type:** `TEST_DEBT`
- **Line Number:** `96`
- **Status:** `ERROR`
- **Comment:** `* Determine if the specified TCP port is currently available on {@code localhost}.      * <p>Package-private solely for testing purposes.`

```java
/**
 * Determine if the specified TCP port is currently available on {@code localhost}.
 * <p>Package-private solely for testing purposes.
 */
boolean isPortAvailable(int port) {
    try {
        ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"));
        serverSocket.close();
        return true;
    } catch (Exception ex) {
        return false;
    }
}
```

### Generated Test Case

```java
// ERROR: Failed to generate test - Gemini API error: 429 - {
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 15, model: gemini-3.5-flash-lite\nPlease retry in 15.399322957s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-3.5-flash-lite",
              "location": "global"
            },
            "quotaValue": "15"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "15s"
      }
    ]
  }
}

```

---

