/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

/*
 * @test
 * @summary JSR-166 tck tests (conformance testing mode)
 * @build *
 * @modules java.management
 * @run junit/othervm/timeout=1000 JSR166TestCase
 */

/*
 * @test
 * @summary JSR-166 tck tests (whitebox tests allowed)
 * @build *
 * @modules java.base/java.util.concurrent:open
 *          java.base/java.lang:open
 *          java.management
 * @run junit/othervm/timeout=1000
 *      -Djsr166.testImplementationDetails=true
 *      JSR166TestCase
 * @run junit/othervm/timeout=1000
 *      -Djsr166.testImplementationDetails=true
 *      -Djava.util.concurrent.ForkJoinPool.common.parallelism=0
 *      JSR166TestCase
 * @run junit/othervm/timeout=1000
 *      -Djsr166.testImplementationDetails=true
 *      -Djava.util.concurrent.ForkJoinPool.common.parallelism=1
 *      -Djava.util.secureRandomSeed=true
 *      JSR166TestCase
 * @run junit/othervm/timeout=1000/policy=tck.policy
 *      -Djsr166.testImplementationDetails=true
 *      JSR166TestCase
 */

package test.java.util.concurrent.tck;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.lang.management.ManagementFactory;
//import java.lang.management.ThreadInfo;
//import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PropertyPermission;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Base class for JSR166 Junit TCK tests.  Defines some constants,
 * utility methods and classes, as well as a simple framework for
 * helping to make sure that assertions failing in generated threads
 * cause the associated test that generated them to itself fail (which
 * JUnit does not otherwise arrange).  The rules for creating such
 * tests are:
 *
 * <ol>
 *
 * <li>All assertions in code running in generated threads must use
 * the forms {@link #threadFail}, {@link #threadAssertTrue}, {@link
 * #threadAssertEquals}, or {@link #threadAssertNull}, (not
 * {@code fail}, {@code assertTrue}, etc.) It is OK (but not
 * particularly recommended) for other code to use these forms too.
 * Only the most typically used JUnit assertion methods are defined
 * this way, but enough to live with.
 *
 * <li>If you override {@link #setUp} or {@link #tearDown}, make sure
 * to invoke {@code super.setUp} and {@code super.tearDown} within
 * them. These methods are used to clear and check for thread
 * assertion failures.
 *
 * <li>All delays and timeouts must use one of the constants {@code
 * SHORT_DELAY_MS}, {@code SMALL_DELAY_MS}, {@code MEDIUM_DELAY_MS},
 * {@code LONG_DELAY_MS}. The idea here is that a SHORT is always
 * discriminable from zero time, and always allows enough time for the
 * small amounts of computation (creating a thread, calling a few
 * methods, etc) needed to reach a timeout point. Similarly, a SMALL
 * is always discriminable as larger than SHORT and smaller than
 * MEDIUM.  And so on. These constants are set to conservative values,
 * but even so, if there is ever any doubt, they can all be increased
 * in one spot to rerun tests on slower platforms.
 *
 * <li>All threads generated must be joined inside each test case
 * method (or {@code fail} to do so) before returning from the
 * method. The {@code joinPool} method can be used to do this when
 * using Executors.
 *
 * </ol>
 *
 * <p><b>Other notes</b>
 * <ul>
 *
 * <li>Usually, there is one testcase method per JSR166 method
 * covering "normal" operation, and then as many exception-testing
 * methods as there are exceptions the method can throw. Sometimes
 * there are multiple tests per JSR166 method when the different
 * "normal" behaviors differ significantly. And sometimes testcases
 * cover multiple methods when they cannot be tested in isolation.
 *
 * <li>The documentation style for testcases is to provide as javadoc
 * a simple sentence or two describing the property that the testcase
 * method purports to test. The javadocs do not say anything about how
 * the property is tested. To find out, read the code.
 *
 * <li>These tests are "conformance tests", and do not attempt to
 * test throughput, latency, scalability or other performance factors
 * (see the separate "jtreg" tests for a set intended to check these
 * for the most central aspects of functionality.) So, most tests use
 * the smallest sensible numbers of threads, collection sizes, etc
 * needed to check basic conformance.
 *
 * <li>The test classes currently do not declare inclusion in
 * any particular package to simplify things for people integrating
 * them in TCK test suites.
 *
 * <li>As a convenience, the {@code main} of this class (JSR166TestCase)
 * runs all JSR166 unit tests.
 *
 * </ul>
 */
// Android-changed: Use JUnit4.
@RunWith(Suite.class)
@SuiteClasses({
    ForkJoinPoolTest.class,
    ForkJoinTaskTest.class,
    RecursiveActionTest.class,
    RecursiveTaskTest.class,
    LinkedTransferQueueTest.class,
    LinkedTransferQueueTest.Generic.class,
    PhaserTest.class,
    ThreadLocalRandomTest.class,
    AbstractExecutorServiceTest.class,
    AbstractQueueTest.class,
    AbstractQueuedSynchronizerTest.class,
    AbstractQueuedLongSynchronizerTest.class,
    ArrayBlockingQueueTest.class,
    ArrayBlockingQueueTest.Fair.class,
    ArrayBlockingQueueTest.NonFair.class,
    ArrayDequeTest.class,
    // ArrayListTest.class,
    AtomicBooleanTest.class,
    AtomicIntegerArrayTest.class,
    AtomicIntegerFieldUpdaterTest.class,
    AtomicIntegerTest.class,
    AtomicLongArrayTest.class,
    AtomicLongFieldUpdaterTest.class,
    AtomicLongTest.class,
    AtomicMarkableReferenceTest.class,
    AtomicReferenceArrayTest.class,
    AtomicReferenceFieldUpdaterTest.class,
    AtomicReferenceTest.class,
    AtomicStampedReferenceTest.class,
    ConcurrentHashMapTest.class,
    ConcurrentLinkedDequeTest.class,
    ConcurrentLinkedQueueTest.class,
    ConcurrentSkipListMapTest.class,
    ConcurrentSkipListSubMapTest.class,
    ConcurrentSkipListSetTest.class,
    ConcurrentSkipListSubSetTest.class,
    CopyOnWriteArrayListTest.class,
    CopyOnWriteArraySetTest.class,
    CountDownLatchTest.class,
    CountedCompleterTest.class,
    CyclicBarrierTest.class,
    DelayQueueTest.class,
    DelayQueueTest.Generic.class,
    EntryTest.class,
    ExchangerTest.class,
    ExecutorsTest.class,
    ExecutorCompletionServiceTest.class,
    FutureTaskTest.class,
    LinkedBlockingDequeTest.class,
    LinkedBlockingDequeTest.Unbounded.class,
    LinkedBlockingDequeTest.Bounded.class,
    LinkedBlockingQueueTest.class,
    LinkedBlockingQueueTest.Unbounded.class,
    LinkedBlockingQueueTest.Bounded.class,
    LinkedListTest.class,
    LockSupportTest.class,
    PriorityBlockingQueueTest.class,
    PriorityBlockingQueueTest.Generic.class,
    PriorityBlockingQueueTest.InitialCapacity.class,
    PriorityQueueTest.class,
    ReentrantLockTest.class,
    ReentrantReadWriteLockTest.class,
    ScheduledExecutorTest.class,
    ScheduledExecutorSubclassTest.class,
    SemaphoreTest.class,
    SynchronousQueueTest.class,
    SynchronousQueueTest.Fair.class,
    SynchronousQueueTest.NonFair.class,
    SystemTest.class,
    ThreadLocalTest.class,
    ThreadPoolExecutorTest.class,
    ThreadPoolExecutorSubclassTest.class,
    ThreadTest.class,
    TimeUnitTest.class,
    TreeMapTest.class,
    TreeSetTest.class,
    TreeSubMapTest.class,
    TreeSubSetTest.class,
    VectorTest.class,
    ArrayDeque8Test.class,
    Atomic8Test.class,
    CompletableFutureTest.class,
    ConcurrentHashMap8Test.class,
    CountedCompleter8Test.class,
    DoubleAccumulatorTest.class,
    DoubleAdderTest.class,
    ForkJoinPool8Test.class,
    ForkJoinTask8Test.class,
    LinkedBlockingDeque8Test.class,
    LinkedBlockingQueue8Test.class,
    LongAccumulatorTest.class,
    LongAdderTest.class,
    SplittableRandomTest.class,
    StampedLockTest.class,
    SubmissionPublisherTest.class,
    ThreadLocalRandom8Test.class,
    TimeUnit8Test.class,
    // AtomicBoolean9Test.class,
    // AtomicInteger9Test.class,
    // AtomicIntegerArray9Test.class,
    // AtomicLong9Test.class,
    // AtomicLongArray9Test.class,
    // AtomicReference9Test.class,
    // AtomicReferenceArray9Test.class,
    // ExecutorCompletionService9Test.class,
    // ForkJoinPool9Test.class,
    SubmissionPublisherTest.class,
})
public class JSR166TestCase {
    private static final boolean useSecurityManager =
        Boolean.getBoolean("jsr166.useSecurityManager");

    protected static final boolean expensiveTests =
        Boolean.getBoolean("jsr166.expensiveTests");

    /**
     * If true, also run tests that are not part of the official tck
     * because they test unspecified implementation details.
     */
    protected static final boolean testImplementationDetails =
        Boolean.getBoolean("jsr166.testImplementationDetails");

    /**
     * If true, report on stdout all "slow" tests, that is, ones that
     * take more than profileThreshold milliseconds to execute.
     */
    private static final boolean profileTests =
        Boolean.getBoolean("jsr166.profileTests");

    /**
     * The number of milliseconds that tests are permitted for
     * execution without being reported, when profileTests is set.
     */
    private static final long profileThreshold =
        Long.getLong("jsr166.profileThreshold", 100);

    /**
     * The number of repetitions per test (for tickling rare bugs).
     */
    private static final int runsPerTest =
        Integer.getInteger("jsr166.runsPerTest", 1);

    /**
     * The number of repetitions of the test suite (for finding leaks?).
     */
    private static final int suiteRuns =
        Integer.getInteger("jsr166.suiteRuns", 1);

    /**
     * Returns the value of the system property, or NaN if not defined.
     */
    private static float systemPropertyValue(String name) {
        String floatString = System.getProperty(name);
        if (floatString == null)
            return Float.NaN;
        try {
            return Float.parseFloat(floatString);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                String.format("Bad float value in system property %s=%s",
                              name, floatString));
        }
    }

    /**
     * The scaling factor to apply to standard delays used in tests.
     * May be initialized from any of:
     * - the "jsr166.delay.factor" system property
     * - the "test.timeout.factor" system property (as used by jtreg)
     *   See: http://openjdk.java.net/jtreg/tag-spec.html
     * - hard-coded fuzz factor when using a known slowpoke VM
     */
    private static final float delayFactor = delayFactor();

    private static float delayFactor() {
        float x;
        if (!Float.isNaN(x = systemPropertyValue("jsr166.delay.factor")))
            return x;
        if (!Float.isNaN(x = systemPropertyValue("test.timeout.factor")))
            return x;
        String prop = System.getProperty("java.vm.version");
        if (prop != null && prop.matches(".*debug.*"))
            return 4.0f; // How much slower is fastdebug than product?!
        return 1.0f;
    }

    /**
     * A filter for tests to run, matching strings of the form
     * methodName(className), e.g. "testInvokeAll5(ForkJoinPoolTest)"
     * Usefully combined with jsr166.runsPerTest.
     */
    private static final Pattern methodFilter = methodFilter();

    private static Pattern methodFilter() {
        String regex = System.getProperty("jsr166.methodFilter");
        return (regex == null) ? null : Pattern.compile(regex);
    }

    // BEGIN Android-removed: Usage of TestCase.
    /*
    // Instrumentation to debug very rare, but very annoying hung test runs.
    static volatile TestCase currentTestCase;
    // static volatile int currentRun = 0;
    static {
        Runnable checkForWedgedTest = new Runnable() { public void run() {
            // Avoid spurious reports with enormous runsPerTest.
            // A single test case run should never take more than 1 second.
            // But let's cap it at the high end too ...
            final int timeoutMinutes =
                Math.min(15, Math.max(runsPerTest / 60, 1));
            for (TestCase lastTestCase = currentTestCase;;) {
                try { MINUTES.sleep(timeoutMinutes); }
                catch (InterruptedException unexpected) { break; }
                if (lastTestCase == currentTestCase) {
                    System.err.printf(
                        "Looks like we're stuck running test: %s%n",
                        lastTestCase);
//                     System.err.printf(
//                         "Looks like we're stuck running test: %s (%d/%d)%n",
//                         lastTestCase, currentRun, runsPerTest);
//                     System.err.println("availableProcessors=" +
//                         Runtime.getRuntime().availableProcessors());
//                     System.err.printf("cpu model = %s%n", cpuModel());
                    dumpTestThreads();
                    // one stack dump is probably enough; more would be spam
                    break;
                }
                lastTestCase = currentTestCase;
            }}};
        Thread thread = new Thread(checkForWedgedTest, "checkForWedgedTest");
        thread.setDaemon(true);
        thread.start();
    }
     */
    // END Android-removed: Usage of TestCase.

//     public static String cpuModel() {
//         try {
//             Matcher matcher = Pattern.compile("model name\\s*: (.*)")
//                 .matcher(new String(
//                      Files.readAllBytes(Paths.get("/proc/cpuinfo")), "UTF-8"));
//             matcher.find();
//             return matcher.group(1);
//         } catch (Exception ex) { return null; }
//     }

    // BEGIN Android-removed: Not used for JUnit4.
    /*
    public void runBare() throws Throwable {
        currentTestCase = this;
        if (methodFilter == null
            || methodFilter.matcher(toString()).find())
            super.runBare();
    }

    protected void runTest() throws Throwable {
        for (int i = 0; i < runsPerTest; i++) {
            // currentRun = i;
            if (profileTests)
                runTestProfiled();
            else
                super.runTest();
        }
    }

    protected void runTestProfiled() throws Throwable {
        for (int i = 0; i < 2; i++) {
            long startTime = System.nanoTime();
            super.runTest();
            long elapsedMillis = millisElapsedSince(startTime);
            if (elapsedMillis < profileThreshold)
                break;
            // Never report first run of any test; treat it as a
            // warmup run, notably to trigger all needed classloading,
            if (i > 0)
                System.out.printf("%n%s: %d%n", toString(), elapsedMillis);
        }
    }
    */
    // END Android-removed: Not used for JUnit4.

    /**
     * Runs all JSR166 unit tests using junit.textui.TestRunner.
     */
    public static void main(String[] args) {
        // Android-changed: Use JUnitCore.main.
        // main(suite(), args);
        // org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.JSR166TestCase");
    }

    // BEGIN Android-removed: Not needed for JUnit4.
    /*
    static class PithyResultPrinter extends junit.textui.ResultPrinter {
        PithyResultPrinter(java.io.PrintStream writer) { super(writer); }
        long runTime;
        public void startTest(Test test) {}
        protected void printHeader(long runTime) {
            this.runTime = runTime; // defer printing for later
        }
        protected void printFooter(TestResult result) {
            if (result.wasSuccessful()) {
                getWriter().println("OK (" + result.runCount() + " tests)"
                    + "  Time: " + elapsedTimeAsString(runTime));
            } else {
                getWriter().println("Time: " + elapsedTimeAsString(runTime));
                super.printFooter(result);
            }
        }
    }

    /**
     * Returns a TestRunner that doesn't bother with unnecessary
     * fluff, like printing a "." for each test case.
     * /
    static junit.textui.TestRunner newPithyTestRunner() {
        junit.textui.TestRunner runner = new junit.textui.TestRunner();
        runner.setPrinter(new PithyResultPrinter(System.out));
        return runner;
    }

    /**
     * Runs all unit tests in the given test suite.
     * Actual behavior influenced by jsr166.* system properties.
     * /
    static void main(Test suite, String[] args) {
        if (useSecurityManager) {
            System.err.println("Setting a permissive security manager");
            Policy.setPolicy(permissivePolicy());
            System.setSecurityManager(new SecurityManager());
        }
        for (int i = 0; i < suiteRuns; i++) {
            TestResult result = newPithyTestRunner().doRun(suite);
            if (!result.wasSuccessful())
                System.exit(1);
            System.gc();
            System.runFinalization();
        }
    }

    public static TestSuite newTestSuite(Object... suiteOrClasses) {
        TestSuite suite = new TestSuite();
        for (Object suiteOrClass : suiteOrClasses) {
            // Android-added: Ignore null and CollectionTest. http://b/285113029
            if (suiteOrClass == null) {
                continue;
            }

            if (suiteOrClass instanceof TestSuite)
                suite.addTest((TestSuite) suiteOrClass);
            else if (suiteOrClass instanceof Class)
                suite.addTest(new TestSuite((Class<?>) suiteOrClass));
            else
                throw new ClassCastException("not a test suite or class");
        }
        return suite;
    }

    public static void addNamedTestClasses(TestSuite suite,
                                           String... testClassNames) {
        for (String testClassName : testClassNames) {
            try {
                Class<?> testClass = Class.forName(testClassName);
                Method m = testClass.getDeclaredMethod("suite",
                                                       new Class<?>[0]);
                suite.addTest(newTestSuite((Test)m.invoke(null)));
            } catch (Exception e) {
                throw new Error("Missing test class", e);
            }
        }
    }
    */
    // END Android-removed: Not needed for JUnit4.

    public static final double JAVA_CLASS_VERSION;
    public static final String JAVA_SPECIFICATION_VERSION;
    static {
        try {
            JAVA_CLASS_VERSION = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Double>() {
                public Double run() {
                    return Double.valueOf(System.getProperty("java.class.version"));}});
            JAVA_SPECIFICATION_VERSION = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("java.specification.version");}});
        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    public static boolean atLeastJava6() { return JAVA_CLASS_VERSION >= 50.0; }
    public static boolean atLeastJava7() { return JAVA_CLASS_VERSION >= 51.0; }
    public static boolean atLeastJava8() { return JAVA_CLASS_VERSION >= 52.0; }
    public static boolean atLeastJava9() {
        return JAVA_CLASS_VERSION >= 53.0
            // As of 2015-09, java9 still uses 52.0 class file version
            || JAVA_SPECIFICATION_VERSION.matches("^(1\\.)?(9|[0-9][0-9])$");
    }
    public static boolean atLeastJava10() {
        return JAVA_CLASS_VERSION >= 54.0
            || JAVA_SPECIFICATION_VERSION.matches("^(1\\.)?[0-9][0-9]$");
    }

    // BEGIN Android-removed: Use JUnit4's SuiteClasses annotation.
    /*
    /**
     * Collects all JSR166 unit tests as one suite.
     * /
    public static Test suite() {
        // Java7+ test classes
        TestSuite suite = newTestSuite(
            ForkJoinPoolTest.suite(),
            ForkJoinTaskTest.suite(),
            RecursiveActionTest.suite(),
            RecursiveTaskTest.suite(),
            LinkedTransferQueueTest.suite(),
            PhaserTest.suite(),
            ThreadLocalRandomTest.suite(),
            AbstractExecutorServiceTest.suite(),
            AbstractQueueTest.suite(),
            AbstractQueuedSynchronizerTest.suite(),
            AbstractQueuedLongSynchronizerTest.suite(),
            ArrayBlockingQueueTest.suite(),
            ArrayDequeTest.suite(),
            ArrayListTest.suite(),
            AtomicBooleanTest.suite(),
            AtomicIntegerArrayTest.suite(),
            AtomicIntegerFieldUpdaterTest.suite(),
            AtomicIntegerTest.suite(),
            AtomicLongArrayTest.suite(),
            AtomicLongFieldUpdaterTest.suite(),
            AtomicLongTest.suite(),
            AtomicMarkableReferenceTest.suite(),
            AtomicReferenceArrayTest.suite(),
            AtomicReferenceFieldUpdaterTest.suite(),
            AtomicReferenceTest.suite(),
            AtomicStampedReferenceTest.suite(),
            ConcurrentHashMapTest.suite(),
            ConcurrentLinkedDequeTest.suite(),
            ConcurrentLinkedQueueTest.suite(),
            ConcurrentSkipListMapTest.suite(),
            ConcurrentSkipListSubMapTest.suite(),
            ConcurrentSkipListSetTest.suite(),
            ConcurrentSkipListSubSetTest.suite(),
            CopyOnWriteArrayListTest.suite(),
            CopyOnWriteArraySetTest.suite(),
            CountDownLatchTest.suite(),
            CountedCompleterTest.suite(),
            CyclicBarrierTest.suite(),
            DelayQueueTest.suite(),
            EntryTest.suite(),
            ExchangerTest.suite(),
            ExecutorsTest.suite(),
            ExecutorCompletionServiceTest.suite(),
            FutureTaskTest.suite(),
            LinkedBlockingDequeTest.suite(),
            LinkedBlockingQueueTest.suite(),
            LinkedListTest.suite(),
            LockSupportTest.suite(),
            PriorityBlockingQueueTest.suite(),
            PriorityQueueTest.suite(),
            ReentrantLockTest.suite(),
            ReentrantReadWriteLockTest.suite(),
            ScheduledExecutorTest.suite(),
            ScheduledExecutorSubclassTest.suite(),
            SemaphoreTest.suite(),
            SynchronousQueueTest.suite(),
            SystemTest.suite(),
            ThreadLocalTest.suite(),
            ThreadPoolExecutorTest.suite(),
            ThreadPoolExecutorSubclassTest.suite(),
            ThreadTest.suite(),
            TimeUnitTest.suite(),
            TreeMapTest.suite(),
            TreeSetTest.suite(),
            TreeSubMapTest.suite(),
            TreeSubSetTest.suite(),
            VectorTest.suite());

        // Java8+ test classes
        if (atLeastJava8()) {
            String[] java8TestClassNames = {
                "ArrayDeque8Test",
                "Atomic8Test",
                "CompletableFutureTest",
                "ConcurrentHashMap8Test",
                "CountedCompleter8Test",
                "DoubleAccumulatorTest",
                "DoubleAdderTest",
                "ForkJoinPool8Test",
                "ForkJoinTask8Test",
                "LinkedBlockingDeque8Test",
                "LinkedBlockingQueue8Test",
                "LongAccumulatorTest",
                "LongAdderTest",
                "SplittableRandomTest",
                "StampedLockTest",
                "SubmissionPublisherTest",
                "ThreadLocalRandom8Test",
                "TimeUnit8Test",
            };
            addNamedTestClasses(suite, java8TestClassNames);
        }

        // Java9+ test classes
        if (atLeastJava9()) {
            String[] java9TestClassNames = {
                "AtomicBoolean9Test",
                "AtomicInteger9Test",
                "AtomicIntegerArray9Test",
                "AtomicLong9Test",
                "AtomicLongArray9Test",
                "AtomicReference9Test",
                "AtomicReferenceArray9Test",
                "ExecutorCompletionService9Test",
                "ForkJoinPool9Test",
                "SubmissionPublisherTest",
            };
            addNamedTestClasses(suite, java9TestClassNames);
        }

        return suite;
    }

    /** Returns list of junit-style test method names in given class. * /
    public static ArrayList<String> testMethodNames(Class<?> testClass) {
        Method[] methods = testClass.getDeclaredMethods();
        ArrayList<String> names = new ArrayList<>(methods.length);
        for (Method method : methods) {
            if (method.getName().startsWith("test")
                && Modifier.isPublic(method.getModifiers())
                // method.getParameterCount() requires jdk8+
                && method.getParameterTypes().length == 0) {
                names.add(method.getName());
            }
        }
        return names;
    }

    /**
     * Returns junit-style testSuite for the given test class, but
     * parameterized by passing extra data to each test.
     * /
    public static <ExtraData> Test parameterizedTestSuite
        (Class<? extends JSR166TestCase> testClass,
         Class<ExtraData> dataClass,
         ExtraData data) {
        try {
            TestSuite suite = new TestSuite();
            Constructor c =
                testClass.getDeclaredConstructor(dataClass, String.class);
            for (String methodName : testMethodNames(testClass))
                suite.addTest((Test) c.newInstance(data, methodName));
            return suite;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Returns junit-style testSuite for the jdk8 extension of the
     * given test class, but parameterized by passing extra data to
     * each test.  Uses reflection to allow compilation in jdk7.
     * /
    public static <ExtraData> Test jdk8ParameterizedTestSuite
        (Class<? extends JSR166TestCase> testClass,
         Class<ExtraData> dataClass,
         ExtraData data) {
        if (atLeastJava8()) {
            String name = testClass.getName();
            String name8 = name.replaceAll("Test$", "8Test");
            if (name.equals(name8)) throw new Error(name);
            try {
                return (Test)
                    Class.forName(name8)
                    .getMethod("testSuite", new Class[] { dataClass })
                    .invoke(null, data);
            } catch (Exception e) {
                throw new Error(e);
            }
        } else {
            return new TestSuite();
        }
    }
    */
    // END Android-removed: Use JUnit4's SuiteClasses annotation.

    // Delays for timing-dependent tests, in milliseconds.

    public static long SHORT_DELAY_MS;
    public static long SMALL_DELAY_MS;
    public static long MEDIUM_DELAY_MS;
    public static long LONG_DELAY_MS;

    /**
     * A delay significantly longer than LONG_DELAY_MS.
     * Use this in a thread that is waited for via awaitTermination(Thread).
     */
    public static long LONGER_DELAY_MS;

    private static final long RANDOM_TIMEOUT;
    private static final long RANDOM_EXPIRED_TIMEOUT;
    private static final TimeUnit RANDOM_TIMEUNIT;
    static {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long[] timeouts = { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE };
        RANDOM_TIMEOUT = timeouts[rnd.nextInt(timeouts.length)];
        RANDOM_EXPIRED_TIMEOUT = timeouts[rnd.nextInt(3)];
        TimeUnit[] timeUnits = TimeUnit.values();
        RANDOM_TIMEUNIT = timeUnits[rnd.nextInt(timeUnits.length)];
    }

    /**
     * Returns a timeout for use when any value at all will do.
     */
    static long randomTimeout() { return RANDOM_TIMEOUT; }

    /**
     * Returns a timeout that means "no waiting", i.e. not positive.
     */
    static long randomExpiredTimeout() { return RANDOM_EXPIRED_TIMEOUT; }

    /**
     * Returns a random non-null TimeUnit.
     */
    static TimeUnit randomTimeUnit() { return RANDOM_TIMEUNIT; }

    /**
     * Returns the shortest timed delay. This can be scaled up for
     * slow machines using the jsr166.delay.factor system property,
     * or via jtreg's -timeoutFactor: flag.
     * http://openjdk.java.net/jtreg/command-help.html
     */
    protected long getShortDelay() {
        return (long) (50 * delayFactor);
    }

    /**
     * Sets delays as multiples of SHORT_DELAY.
     */
    protected void setDelays() {
        SHORT_DELAY_MS = getShortDelay();
        SMALL_DELAY_MS  = SHORT_DELAY_MS * 5;
        MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
        LONG_DELAY_MS   = SHORT_DELAY_MS * 200;
    }

    /**
     * Returns a timeout in milliseconds to be used in tests that
     * verify that operations block or time out.
     */
    long timeoutMillis() {
        return SHORT_DELAY_MS / 4;
    }

    /**
     * Returns a new Date instance representing a time at least
     * delayMillis milliseconds in the future.
     */
    Date delayedDate(long delayMillis) {
        // Add 1 because currentTimeMillis is known to round into the past.
        return new Date(System.currentTimeMillis() + delayMillis + 1);
    }

    /**
     * The first exception encountered if any threadAssertXXX method fails.
     */
    private final AtomicReference<Throwable> threadFailure
        = new AtomicReference<>(null);

    /**
     * Records an exception so that it can be rethrown later in the test
     * harness thread, triggering a test case failure.  Only the first
     * failure is recorded; subsequent calls to this method from within
     * the same test have no effect.
     */
    public void threadRecordFailure(Throwable t) {
        System.err.println(t);
        dumpTestThreads();
        threadFailure.compareAndSet(null, t);
    }

    @Before
    public void setUp() {
        setDelays();
    }

    void tearDownFail(String format, Object... args) {
        String msg = toString() + ": " + String.format(format, args);
        System.err.println(msg);
        dumpTestThreads();
        fail(msg);
    }

    /**
     * Extra checks that get done for all test cases.
     *
     * Triggers test case failure if any thread assertions have failed,
     * by rethrowing, in the test harness thread, any exception recorded
     * earlier by threadRecordFailure.
     *
     * Triggers test case failure if interrupt status is set in the main thread.
     */
    @After
    public void tearDown() throws Exception {
        Throwable t = threadFailure.getAndSet(null);
        if (t != null) {
            if (t instanceof Error)
                throw (Error) t;
            else if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            else if (t instanceof Exception)
                throw (Exception) t;
            else {
                failWithCause(t);
            }
        }

        if (Thread.interrupted())
            tearDownFail("interrupt status set in main thread");

        checkForkJoinPoolThreadLeaks();
    }

    // Android-added: Mechanism for throwing a failure with a specified cause
    // This is needed since AssertionFailedError has been removed in JUnit 4.
    public static void failWithCause(Throwable cause, String msg) {
        try {
            fail(msg);
        } catch (Throwable afe) {
            afe.initCause(cause);
            throw afe;
        }
    }
    public static void failWithCause(Throwable cause) {
        failWithCause(cause, cause.toString());
    }

    /**
     * Finds missing PoolCleaners
     */
    void checkForkJoinPoolThreadLeaks() throws InterruptedException {
        Thread[] survivors = new Thread[7];
        int count = Thread.enumerate(survivors);
        for (int i = 0; i < count; i++) {
            Thread thread = survivors[i];
            String name = thread.getName();
            if (name.startsWith("ForkJoinPool-")) {
                // give thread some time to terminate
                thread.join(LONG_DELAY_MS);
                if (thread.isAlive())
                    tearDownFail("Found leaked ForkJoinPool thread thread=%s",
                                 thread);
            }
        }

        if (!ForkJoinPool.commonPool()
            .awaitQuiescence(LONG_DELAY_MS, MILLISECONDS))
            tearDownFail("ForkJoin common pool thread stuck");
    }

    /**
     * Just like fail(reason), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadFail(String reason) {
        try {
            fail(reason);
        } catch (AssertionError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Just like assertTrue(b), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertTrue(boolean b) {
        try {
            assertTrue(b);
        } catch (AssertionError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Just like assertFalse(b), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertFalse(boolean b) {
        try {
            assertFalse(b);
        } catch (AssertionError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Just like assertNull(x), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertNull(Object x) {
        try {
            assertNull(x);
        } catch (AssertionError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Just like assertEquals(x, y), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertEquals(long x, long y) {
        try {
            assertEquals(x, y);
        } catch (AssertionError t) {
            threadRecordFailure(t);
            throw t;
        }
    }

    /**
     * Just like assertEquals(x, y), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertEquals(Object x, Object y) {
        try {
            assertEquals(x, y);
        } catch (AssertionError fail) {
            threadRecordFailure(fail);
            throw fail;
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
        }
    }

    /**
     * Just like assertSame(x, y), but additionally recording (using
     * threadRecordFailure) any AssertionError thrown, so that
     * the current testcase will fail.
     */
    public void threadAssertSame(Object x, Object y) {
        try {
            assertSame(x, y);
        } catch (AssertionError fail) {
            threadRecordFailure(fail);
            throw fail;
        }
    }

    /**
     * Calls threadFail with message "should throw exception".
     */
    public void threadShouldThrow() {
        threadFail("should throw exception");
    }

    /**
     * Calls threadFail with message "should throw" + exceptionName.
     */
    public void threadShouldThrow(String exceptionName) {
        threadFail("should throw " + exceptionName);
    }

    /**
     * Records the given exception using {@link #threadRecordFailure},
     * then rethrows the exception, wrapping it in an
     * AssertionError if necessary.
     */
    public void threadUnexpectedException(Throwable t) {
        threadRecordFailure(t);
        t.printStackTrace();
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else {
            failWithCause(t, "unexpected exception: " + t);
        }
    }

    /**
     * Delays, via Thread.sleep, for the given millisecond delay, but
     * if the sleep is shorter than specified, may re-sleep or yield
     * until time elapses.  Ensures that the given time, as measured
     * by System.nanoTime(), has elapsed.
     */
    static void delay(long millis) throws InterruptedException {
        long nanos = millis * (1000 * 1000);
        final long wakeupTime = System.nanoTime() + nanos;
        do {
            if (millis > 0L)
                Thread.sleep(millis);
            else // too short to sleep
                Thread.yield();
            nanos = wakeupTime - System.nanoTime();
            millis = nanos / (1000 * 1000);
        } while (nanos >= 0L);
    }

    /**
     * Allows use of try-with-resources with per-test thread pools.
     */
    class PoolCleaner implements AutoCloseable {
        private final ExecutorService pool;
        public PoolCleaner(ExecutorService pool) { this.pool = pool; }
        public void close() { joinPool(pool); }
    }

    /**
     * An extension of PoolCleaner that has an action to release the pool.
     */
    class PoolCleanerWithReleaser extends PoolCleaner {
        private final Runnable releaser;
        public PoolCleanerWithReleaser(ExecutorService pool, Runnable releaser) {
            super(pool);
            this.releaser = releaser;
        }
        public void close() {
            try {
                releaser.run();
            } finally {
                super.close();
            }
        }
    }

    PoolCleaner cleaner(ExecutorService pool) {
        return new PoolCleaner(pool);
    }

    PoolCleaner cleaner(ExecutorService pool, Runnable releaser) {
        return new PoolCleanerWithReleaser(pool, releaser);
    }

    PoolCleaner cleaner(ExecutorService pool, CountDownLatch latch) {
        return new PoolCleanerWithReleaser(pool, releaser(latch));
    }

    Runnable releaser(final CountDownLatch latch) {
        return new Runnable() { public void run() {
            do { latch.countDown(); }
            while (latch.getCount() > 0);
        }};
    }

    PoolCleaner cleaner(ExecutorService pool, AtomicBoolean flag) {
        return new PoolCleanerWithReleaser(pool, releaser(flag));
    }

    Runnable releaser(final AtomicBoolean flag) {
        return new Runnable() { public void run() { flag.set(true); }};
    }

    /**
     * Waits out termination of a thread pool or fails doing so.
     */
    void joinPool(ExecutorService pool) {
        try {
            pool.shutdown();
            if (!pool.awaitTermination(2 * LONG_DELAY_MS, MILLISECONDS)) {
                try {
                    threadFail("ExecutorService " + pool +
                               " did not terminate in a timely manner");
                } finally {
                    // last resort, for the benefit of subsequent tests
                    pool.shutdownNow();
                    pool.awaitTermination(MEDIUM_DELAY_MS, MILLISECONDS);
                }
            }
        } catch (SecurityException ok) {
            // Allowed in case test doesn't have privs
        } catch (InterruptedException fail) {
            threadFail("Unexpected InterruptedException");
        }
    }

    /**
     * Like Runnable, but with the freedom to throw anything.
     * junit folks had the same idea:
     * http://junit.org/junit5/docs/snapshot/api/org/junit/gen5/api/Executable.html
     */
    interface Action { public void run() throws Throwable; }

    /**
     * Runs all the given actions in parallel, failing if any fail.
     * Useful for running multiple variants of tests that are
     * necessarily individually slow because they must block.
     */
    protected void testInParallel(Action ... actions) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(pool)) {
            ArrayList<Future<?>> futures = new ArrayList<>(actions.length);
            for (final Action action : actions)
                futures.add(pool.submit(new CheckedRunnable() {
                    public void realRun() throws Throwable { action.run();}}));
            for (Future<?> future : futures)
                try {
                    assertNull(future.get(LONG_DELAY_MS, MILLISECONDS));
                } catch (ExecutionException ex) {
                    threadUnexpectedException(ex.getCause());
                } catch (Exception ex) {
                    threadUnexpectedException(ex);
                }
        }
    }

    /**
     * A debugging tool to print stack traces of most threads, as jstack does.
     * Uninteresting threads are filtered out.
     */
    static void dumpTestThreads() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                System.setSecurityManager(null);
            } catch (SecurityException giveUp) {
                return;
            }
        }

        // Android-removed: Android doesn't have ManagementFactory.
        /*
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        System.err.println("------ stacktrace dump start ------");
        for (ThreadInfo info : threadMXBean.dumpAllThreads(true, true)) {
            final String name = info.getThreadName();
            String lockName;
            if ("Signal Dispatcher".equals(name))
                continue;
            if ("Reference Handler".equals(name)
                && (lockName = info.getLockName()) != null
                && lockName.startsWith("java.lang.ref.Reference$Lock"))
                continue;
            if ("Finalizer".equals(name)
                && (lockName = info.getLockName()) != null
                && lockName.startsWith("java.lang.ref.ReferenceQueue$Lock"))
                continue;
            if ("checkForWedgedTest".equals(name))
                continue;
            System.err.print(info);
        }
        System.err.println("------ stacktrace dump end ------");
        */

        if (sm != null) System.setSecurityManager(sm);
    }

    /**
     * Checks that thread does not terminate within the default
     * millisecond delay of {@code timeoutMillis()}.
     */
    void assertThreadStaysAlive(Thread thread) {
        assertThreadStaysAlive(thread, timeoutMillis());
    }

    /**
     * Checks that thread does not terminate within the given millisecond delay.
     */
    void assertThreadStaysAlive(Thread thread, long millis) {
        try {
            // No need to optimize the failing case via Thread.join.
            delay(millis);
            assertTrue(thread.isAlive());
        } catch (InterruptedException fail) {
            threadFail("Unexpected InterruptedException");
        }
    }

    /**
     * Checks that the threads do not terminate within the default
     * millisecond delay of {@code timeoutMillis()}.
     */
    void assertThreadsStayAlive(Thread... threads) {
        assertThreadsStayAlive(timeoutMillis(), threads);
    }

    /**
     * Checks that the threads do not terminate within the given millisecond delay.
     */
    void assertThreadsStayAlive(long millis, Thread... threads) {
        try {
            // No need to optimize the failing case via Thread.join.
            delay(millis);
            for (Thread thread : threads)
                assertTrue(thread.isAlive());
        } catch (InterruptedException fail) {
            threadFail("Unexpected InterruptedException");
        }
    }

    /**
     * Checks that future.get times out, with the default timeout of
     * {@code timeoutMillis()}.
     */
    void assertFutureTimesOut(Future future) {
        assertFutureTimesOut(future, timeoutMillis());
    }

    /**
     * Checks that future.get times out, with the given millisecond timeout.
     */
    void assertFutureTimesOut(Future future, long timeoutMillis) {
        long startTime = System.nanoTime();
        try {
            future.get(timeoutMillis, MILLISECONDS);
            shouldThrow();
        } catch (TimeoutException success) {
        } catch (Exception fail) {
            threadUnexpectedException(fail);
        } finally { future.cancel(true); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
    }

    /**
     * Fails with message "should throw exception".
     */
    public void shouldThrow() {
        fail("Should throw exception");
    }

    /**
     * Fails with message "should throw " + exceptionName.
     */
    public void shouldThrow(String exceptionName) {
        fail("Should throw " + exceptionName);
    }

    /**
     * The number of elements to place in collections, arrays, etc.
     */
    public static final int SIZE = 20;

    // Some convenient Integer constants

    public static final Integer zero  = new Integer(0);
    public static final Integer one   = new Integer(1);
    public static final Integer two   = new Integer(2);
    public static final Integer three = new Integer(3);
    public static final Integer four  = new Integer(4);
    public static final Integer five  = new Integer(5);
    public static final Integer six   = new Integer(6);
    public static final Integer seven = new Integer(7);
    public static final Integer eight = new Integer(8);
    public static final Integer nine  = new Integer(9);
    public static final Integer m1  = new Integer(-1);
    public static final Integer m2  = new Integer(-2);
    public static final Integer m3  = new Integer(-3);
    public static final Integer m4  = new Integer(-4);
    public static final Integer m5  = new Integer(-5);
    public static final Integer m6  = new Integer(-6);
    public static final Integer m10 = new Integer(-10);

    static Item[] seqItems(int size) {
        Item[] s = new Item[size];
        for (int i = 0; i < size; ++i)
            s[i] = new Item(i);
        return s;
    }
    static Item[] negativeSeqItems(int size) {
        Item[] s = new Item[size];
        for (int i = 0; i < size; ++i)
            s[i] = new Item(-i);
        return s;
    }

    // Many tests rely on defaultItems all being sequential nonnegative
    public static final Item[] defaultItems = seqItems(SIZE);

    static Item itemFor(int i) { // check cache for defaultItems
        Item[] items = defaultItems;
        return (i >= 0 && i < items.length) ? items[i] : new Item(i);
    }

    public static final Item itemZero  = defaultItems[0];
    public static final Item itemOne   = defaultItems[1];
    public static final Item itemTwo   = defaultItems[2];
    public static final Item itemThree = defaultItems[3];
    public static final Item itemFour  = defaultItems[4];
    public static final Item itemFive  = defaultItems[5];
    public static final Item itemSix   = defaultItems[6];
    public static final Item itemSeven = defaultItems[7];
    public static final Item itemEight = defaultItems[8];
    public static final Item itemNine  = defaultItems[9];
    public static final Item itemTen   = defaultItems[10];

    public static final Item[] negativeItems = negativeSeqItems(SIZE);

    public static final Item minusOne   = negativeItems[1];
    public static final Item minusTwo   = negativeItems[2];
    public static final Item minusThree = negativeItems[3];
    public static final Item minusFour  = negativeItems[4];
    public static final Item minusFive  = negativeItems[5];
    public static final Item minusSix   = negativeItems[6];
    public static final Item minusSeven = negativeItems[7];
    public static final Item minusEight = negativeItems[8];
    public static final Item minusNone  = negativeItems[9];
    public static final Item minusTen   = negativeItems[10];

    // elements expected to be missing
    public static final Item fortytwo = new Item(42);
    public static final Item eightysix = new Item(86);
    public static final Item ninetynine = new Item(99);

    // Interop across Item, int

    static void mustEqual(Item x, Item y) {
        if (x != y)
            assertEquals(x.value, y.value);
    }
    static void mustEqual(Item x, int y) {
        assertEquals(x.value, y);
    }
    static void mustEqual(int x, Item y) {
        assertEquals(x, y.value);
    }
    static void mustEqual(int x, int y) {
        assertEquals(x, y);
    }
    static void mustEqual(Object x, Object y) {
        if (x != y)
            assertEquals(x, y);
    }
    static void mustEqual(int x, Object y) {
        if (y instanceof Item)
            assertEquals(x, ((Item)y).value);
        else fail();
    }
    static void mustEqual(Object x, int y) {
        if (x instanceof Item)
            assertEquals(((Item)x).value, y);
        else fail();
    }
    static void mustEqual(boolean x, boolean y) {
        assertEquals(x, y);
    }
    static void mustEqual(long x, long y) {
        assertEquals(x, y);
    }
    static void mustEqual(double x, double y) {
        assertEquals(x, y);
    }
    static void mustContain(Collection<Item> c, int i) {
        assertTrue(c.contains(itemFor(i)));
    }
    static void mustContain(Collection<Item> c, Item i) {
        assertTrue(c.contains(i));
    }
    static void mustNotContain(Collection<Item> c, int i) {
        assertFalse(c.contains(itemFor(i)));
    }
    static void mustNotContain(Collection<Item> c, Item i) {
        assertFalse(c.contains(i));
    }
    static void mustRemove(Collection<Item> c, int i) {
        assertTrue(c.remove(itemFor(i)));
    }
    static void mustRemove(Collection<Item> c, Item i) {
        assertTrue(c.remove(i));
    }
    static void mustNotRemove(Collection<Item> c, int i) {
        Item[] items = defaultItems;
        Item x = (i >= 0 && i < items.length) ? items[i] : new Item(i);
        assertFalse(c.remove(x));
    }
    static void mustNotRemove(Collection<Item> c, Item i) {
        assertFalse(c.remove(i));
    }
    static void mustAdd(Collection<Item> c, int i) {
        assertTrue(c.add(itemFor(i)));
    }
    static void mustAdd(Collection<Item> c, Item i) {
        assertTrue(c.add(i));
    }


    /**
     * Runs Runnable r with a security policy that permits precisely
     * the specified permissions.  If there is no current security
     * manager, the runnable is run twice, both with and without a
     * security manager.  We require that any security manager permit
     * getPolicy/setPolicy.
     */
    public void runWithPermissions(Runnable r, Permission... permissions) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            r.run();
        }
        runWithSecurityManagerWithPermissions(r, permissions);
    }

    /**
     * Runs Runnable r with a security policy that permits precisely
     * the specified permissions.  If there is no current security
     * manager, a temporary one is set for the duration of the
     * Runnable.  We require that any security manager permit
     * getPolicy/setPolicy.
     */
    public void runWithSecurityManagerWithPermissions(Runnable r,
                                                      Permission... permissions) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            Policy savedPolicy = Policy.getPolicy();
            try {
                Policy.setPolicy(permissivePolicy());
                System.setSecurityManager(new SecurityManager());
                runWithSecurityManagerWithPermissions(r, permissions);
            } finally {
                System.setSecurityManager(null);
                Policy.setPolicy(savedPolicy);
            }
        } else {
            Policy savedPolicy = Policy.getPolicy();
            AdjustablePolicy policy = new AdjustablePolicy(permissions);
            Policy.setPolicy(policy);

            try {
                r.run();
            } finally {
                policy.addPermission(new SecurityPermission("setPolicy"));
                Policy.setPolicy(savedPolicy);
            }
        }
    }

    /**
     * Runs a runnable without any permissions.
     */
    public void runWithoutPermissions(Runnable r) {
        runWithPermissions(r);
    }

    /**
     * A security policy where new permissions can be dynamically added
     * or all cleared.
     */
    public static class AdjustablePolicy extends java.security.Policy {
        Permissions perms = new Permissions();
        AdjustablePolicy(Permission... permissions) {
            for (Permission permission : permissions)
                perms.add(permission);
        }
        void addPermission(Permission perm) { perms.add(perm); }
        void clearPermissions() { perms = new Permissions(); }
        public PermissionCollection getPermissions(CodeSource cs) {
            return perms;
        }
        public PermissionCollection getPermissions(ProtectionDomain pd) {
            return perms;
        }
        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p);
        }
        public void refresh() {}
        public String toString() {
            List<Permission> ps = new ArrayList<>();
            for (Enumeration<Permission> e = perms.elements(); e.hasMoreElements();)
                ps.add(e.nextElement());
            return "AdjustablePolicy with permissions " + ps;
        }
    }

    /**
     * Returns a policy containing all the permissions we ever need.
     */
    public static Policy permissivePolicy() {
        return new AdjustablePolicy
            // Permissions j.u.c. needs directly
            (new RuntimePermission("modifyThread"),
             new RuntimePermission("getClassLoader"),
             new RuntimePermission("setContextClassLoader"),
             // Permissions needed to change permissions!
             new SecurityPermission("getPolicy"),
             new SecurityPermission("setPolicy"),
             new RuntimePermission("setSecurityManager"),
             // Permissions needed by the junit test harness
             new RuntimePermission("accessDeclaredMembers"),
             new PropertyPermission("*", "read"),
             new java.io.FilePermission("<<ALL FILES>>", "read"));
    }

    /**
     * Sleeps until the given time has elapsed.
     * Throws AssertionError if interrupted.
     */
    static void sleep(long millis) {
        try {
            delay(millis);
        } catch (InterruptedException fail) {
            failWithCause(fail, "Unexpected InterruptedException");
        }
    }

    /**
     * Spin-waits up to the specified number of milliseconds for the given
     * thread to enter a wait state: BLOCKED, WAITING, or TIMED_WAITING.
     */
    void waitForThreadToEnterWaitState(Thread thread, long timeoutMillis) {
        long startTime = 0L;
        for (;;) {
            Thread.State s = thread.getState();
            if (s == Thread.State.BLOCKED ||
                s == Thread.State.WAITING ||
                s == Thread.State.TIMED_WAITING)
                return;
            else if (s == Thread.State.TERMINATED)
                fail("Unexpected thread termination");
            else if (startTime == 0L)
                startTime = System.nanoTime();
            else if (millisElapsedSince(startTime) > timeoutMillis) {
                threadAssertTrue(thread.isAlive());
                fail("timed out waiting for thread to enter wait state");
            }
            Thread.yield();
        }
    }

    /**
     * Spin-waits up to the specified number of milliseconds for the given
     * thread to enter a wait state: BLOCKED, WAITING, or TIMED_WAITING,
     * and additionally satisfy the given condition.
     */
    void waitForThreadToEnterWaitState(
        Thread thread, long timeoutMillis, Callable<Boolean> waitingForGodot) {
        long startTime = 0L;
        for (;;) {
            Thread.State s = thread.getState();
            if (s == Thread.State.BLOCKED ||
                s == Thread.State.WAITING ||
                s == Thread.State.TIMED_WAITING) {
                try {
                    if (waitingForGodot.call())
                        return;
                } catch (Throwable fail) { threadUnexpectedException(fail); }
            }
            else if (s == Thread.State.TERMINATED)
                fail("Unexpected thread termination");
            else if (startTime == 0L)
                startTime = System.nanoTime();
            else if (millisElapsedSince(startTime) > timeoutMillis) {
                threadAssertTrue(thread.isAlive());
                fail("timed out waiting for thread to enter wait state");
            }
            Thread.yield();
        }
    }

    /**
     * Spin-waits up to LONG_DELAY_MS milliseconds for the given thread to
     * enter a wait state: BLOCKED, WAITING, or TIMED_WAITING.
     */
    void waitForThreadToEnterWaitState(Thread thread) {
        waitForThreadToEnterWaitState(thread, LONG_DELAY_MS);
    }

    /**
     * Spin-waits up to LONG_DELAY_MS milliseconds for the given thread to
     * enter a wait state: BLOCKED, WAITING, or TIMED_WAITING,
     * and additionally satisfy the given condition.
     */
    void waitForThreadToEnterWaitState(
        Thread thread, Callable<Boolean> waitingForGodot) {
        waitForThreadToEnterWaitState(thread, LONG_DELAY_MS, waitingForGodot);
    }

    /**
     * Returns the number of milliseconds since time given by
     * startNanoTime, which must have been previously returned from a
     * call to {@link System#nanoTime()}.
     */
    static long millisElapsedSince(long startNanoTime) {
        return NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
    }

//     void assertTerminatesPromptly(long timeoutMillis, Runnable r) {
//         long startTime = System.nanoTime();
//         try {
//             r.run();
//         } catch (Throwable fail) { threadUnexpectedException(fail); }
//         if (millisElapsedSince(startTime) > timeoutMillis/2)
//             throw new AssertionFailedError("did not return promptly");
//     }

//     void assertTerminatesPromptly(Runnable r) {
//         assertTerminatesPromptly(LONG_DELAY_MS/2, r);
//     }

    /**
     * Checks that timed f.get() returns the expected value, and does not
     * wait for the timeout to elapse before returning.
     */
    <T> void checkTimedGet(Future<T> f, T expectedValue, long timeoutMillis) {
        long startTime = System.nanoTime();
        try {
            assertEquals(expectedValue, f.get(timeoutMillis, MILLISECONDS));
        } catch (Throwable fail) { threadUnexpectedException(fail); }
        if (millisElapsedSince(startTime) > timeoutMillis/2)
            fail("timed get did not return promptly");
    }

    <T> void checkTimedGet(Future<T> f, T expectedValue) {
        checkTimedGet(f, expectedValue, LONG_DELAY_MS);
    }

    /**
     * Returns a new started daemon Thread running the given runnable.
     */
    Thread newStartedThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Waits for the specified time (in milliseconds) for the thread
     * to terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t, long timeoutMillis) {
        try {
            t.join(timeoutMillis);
        } catch (InterruptedException fail) {
            threadUnexpectedException(fail);
        } finally {
            if (t.getState() != Thread.State.TERMINATED) {
                t.interrupt();
                threadFail("timed out waiting for thread to terminate");
            }
        }
    }

    /**
     * Waits for LONG_DELAY_MS milliseconds for the thread to
     * terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t) {
        awaitTermination(t, LONG_DELAY_MS);
    }

    // Some convenient Runnable classes

    public abstract class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {
                realRun();
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
            }
        }
    }

    public abstract class RunnableShouldThrow implements Runnable {
        protected abstract void realRun() throws Throwable;

        final Class<?> exceptionClass;

        <T extends Throwable> RunnableShouldThrow(Class<T> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public final void run() {
            try {
                realRun();
                threadShouldThrow(exceptionClass.getSimpleName());
            } catch (Throwable t) {
                if (! exceptionClass.isInstance(t))
                    threadUnexpectedException(t);
            }
        }
    }

    public abstract class ThreadShouldThrow extends Thread {
        protected abstract void realRun() throws Throwable;

        final Class<?> exceptionClass;

        <T extends Throwable> ThreadShouldThrow(Class<T> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public final void run() {
            try {
                realRun();
                threadShouldThrow(exceptionClass.getSimpleName());
            } catch (Throwable t) {
                if (! exceptionClass.isInstance(t))
                    threadUnexpectedException(t);
            }
        }
    }

    public abstract class CheckedInterruptedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {
                realRun();
                threadShouldThrow("InterruptedException");
            } catch (InterruptedException success) {
                threadAssertFalse(Thread.interrupted());
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
            }
        }
    }

    public abstract class CheckedCallable<T> implements Callable<T> {
        protected abstract T realCall() throws Throwable;

        public final T call() {
            try {
                return realCall();
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
                return null;
            }
        }
    }

    public abstract class CheckedInterruptedCallable<T>
        implements Callable<T> {
        protected abstract T realCall() throws Throwable;

        public final T call() {
            try {
                T result = realCall();
                threadShouldThrow("InterruptedException");
                return result;
            } catch (InterruptedException success) {
                threadAssertFalse(Thread.interrupted());
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
            }
            return null;
        }
    }

    public static class NoOpRunnable implements Runnable {
        public void run() {}
    }

    public static class NoOpCallable implements Callable {
        public Object call() { return Boolean.TRUE; }
    }

    public static final String TEST_STRING = "a test string";

    public static class StringTask implements Callable<String> {
        final String value;
        public StringTask() { this(TEST_STRING); }
        public StringTask(String value) { this.value = value; }
        public String call() { return value; }
    }

    public Callable<String> latchAwaitingStringTask(final CountDownLatch latch) {
        return new CheckedCallable<String>() {
            protected String realCall() {
                try {
                    latch.await();
                } catch (InterruptedException quittingTime) {}
                return TEST_STRING;
            }};
    }

    public Runnable countDowner(final CountDownLatch latch) {
        return new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                latch.countDown();
            }};
    }

    class LatchAwaiter extends CheckedRunnable {
        static final int NEW = 0;
        static final int RUNNING = 1;
        static final int DONE = 2;
        final CountDownLatch latch;
        int state = NEW;
        LatchAwaiter(CountDownLatch latch) { this.latch = latch; }
        public void realRun() throws InterruptedException {
            state = 1;
            await(latch);
            state = 2;
        }
    }

    public LatchAwaiter awaiter(CountDownLatch latch) {
        return new LatchAwaiter(latch);
    }

    public void await(CountDownLatch latch, long timeoutMillis) {
        try {
            if (!latch.await(timeoutMillis, MILLISECONDS))
                fail("timed out waiting for CountDownLatch for "
                     + (timeoutMillis/1000) + " sec");
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
        }
    }

    public void await(CountDownLatch latch) {
        await(latch, LONG_DELAY_MS);
    }

    public void await(Semaphore semaphore) {
        try {
            if (!semaphore.tryAcquire(LONG_DELAY_MS, MILLISECONDS))
                fail("timed out waiting for Semaphore for "
                     + (LONG_DELAY_MS/1000) + " sec");
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
        }
    }

//     /**
//      * Spin-waits up to LONG_DELAY_MS until flag becomes true.
//      */
//     public void await(AtomicBoolean flag) {
//         await(flag, LONG_DELAY_MS);
//     }

//     /**
//      * Spin-waits up to the specified timeout until flag becomes true.
//      */
//     public void await(AtomicBoolean flag, long timeoutMillis) {
//         long startTime = System.nanoTime();
//         while (!flag.get()) {
//             if (millisElapsedSince(startTime) > timeoutMillis)
//                 throw new AssertionFailedError("timed out");
//             Thread.yield();
//         }
//     }

    public static class NPETask implements Callable<String> {
        public String call() { throw new NullPointerException(); }
    }

    public static class CallableOne implements Callable<Integer> {
        public Integer call() { return one; }
    }

    public class ShortRunnable extends CheckedRunnable {
        protected void realRun() throws Throwable {
            delay(SHORT_DELAY_MS);
        }
    }

    public class ShortInterruptedRunnable extends CheckedInterruptedRunnable {
        protected void realRun() throws InterruptedException {
            delay(SHORT_DELAY_MS);
        }
    }

    public class SmallRunnable extends CheckedRunnable {
        protected void realRun() throws Throwable {
            delay(SMALL_DELAY_MS);
        }
    }

    public class SmallPossiblyInterruptedRunnable extends CheckedRunnable {
        protected void realRun() {
            try {
                delay(SMALL_DELAY_MS);
            } catch (InterruptedException ok) {}
        }
    }

    public class SmallCallable extends CheckedCallable {
        protected Object realCall() throws InterruptedException {
            delay(SMALL_DELAY_MS);
            return Boolean.TRUE;
        }
    }

    public class MediumRunnable extends CheckedRunnable {
        protected void realRun() throws Throwable {
            delay(MEDIUM_DELAY_MS);
        }
    }

    public class MediumInterruptedRunnable extends CheckedInterruptedRunnable {
        protected void realRun() throws InterruptedException {
            delay(MEDIUM_DELAY_MS);
        }
    }

    public Runnable possiblyInterruptedRunnable(final long timeoutMillis) {
        return new CheckedRunnable() {
            protected void realRun() {
                try {
                    delay(timeoutMillis);
                } catch (InterruptedException ok) {}
            }};
    }

    public class MediumPossiblyInterruptedRunnable extends CheckedRunnable {
        protected void realRun() {
            try {
                delay(MEDIUM_DELAY_MS);
            } catch (InterruptedException ok) {}
        }
    }

    public class LongPossiblyInterruptedRunnable extends CheckedRunnable {
        protected void realRun() {
            try {
                delay(LONG_DELAY_MS);
            } catch (InterruptedException ok) {}
        }
    }

    /**
     * For use as ThreadFactory in constructors
     */
    public static class SimpleThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    public interface TrackedRunnable extends Runnable {
        boolean isDone();
    }

    public static TrackedRunnable trackedRunnable(final long timeoutMillis) {
        return new TrackedRunnable() {
                private volatile boolean done = false;
                public boolean isDone() { return done; }
                public void run() {
                    try {
                        delay(timeoutMillis);
                        done = true;
                    } catch (InterruptedException ok) {}
                }
            };
    }

    public static class TrackedShortRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                delay(SHORT_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedSmallRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                delay(SMALL_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedMediumRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                delay(MEDIUM_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedLongRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            try {
                delay(LONG_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
        }
    }

    public static class TrackedNoOpRunnable implements Runnable {
        public volatile boolean done = false;
        public void run() {
            done = true;
        }
    }

    public static class TrackedCallable implements Callable {
        public volatile boolean done = false;
        public Object call() {
            try {
                delay(SMALL_DELAY_MS);
                done = true;
            } catch (InterruptedException ok) {}
            return Boolean.TRUE;
        }
    }

    /**
     * Analog of CheckedRunnable for RecursiveAction
     */
    public abstract class CheckedRecursiveAction extends RecursiveAction {
        protected abstract void realCompute() throws Throwable;

        @Override protected final void compute() {
            try {
                realCompute();
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
            }
        }
    }

    /**
     * Analog of CheckedCallable for RecursiveTask
     */
    public abstract class CheckedRecursiveTask<T> extends RecursiveTask<T> {
        protected abstract T realCompute() throws Throwable;

        @Override protected final T compute() {
            try {
                return realCompute();
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
                return null;
            }
        }
    }

    /**
     * For use as RejectedExecutionHandler in constructors
     */
    public static class NoOpREHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r,
                                      ThreadPoolExecutor executor) {}
    }

    /**
     * A CyclicBarrier that uses timed await and fails with
     * AssertionFailedErrors instead of throwing checked exceptions.
     */
    public static class CheckedBarrier extends CyclicBarrier {
        public CheckedBarrier(int parties) { super(parties); }

        public int await() {
            try {
                return super.await(2 * LONG_DELAY_MS, MILLISECONDS);
            } catch (TimeoutException timedOut) {
                fail("timed out");
            } catch (Exception fail) {
                failWithCause(fail, "Unexpected exception: " + fail);
            }
            return -1;
        }
    }

    void checkEmpty(BlockingQueue q) {
        try {
            assertTrue(q.isEmpty());
            assertEquals(0, q.size());
            assertNull(q.peek());
            assertNull(q.poll());
            assertNull(q.poll(0, MILLISECONDS));
            assertEquals(q.toString(), "[]");
            assertTrue(Arrays.equals(q.toArray(), new Object[0]));
            assertFalse(q.iterator().hasNext());
            try {
                q.element();
                shouldThrow();
            } catch (NoSuchElementException success) {}
            try {
                q.iterator().next();
                shouldThrow();
            } catch (NoSuchElementException success) {}
            try {
                q.remove();
                shouldThrow();
            } catch (NoSuchElementException success) {}
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
    }

    void assertSerialEquals(Object x, Object y) {
        assertTrue(Arrays.equals(serialBytes(x), serialBytes(y)));
    }

    void assertNotSerialEquals(Object x, Object y) {
        assertFalse(Arrays.equals(serialBytes(x), serialBytes(y)));
    }

    byte[] serialBytes(Object o) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            return bos.toByteArray();
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
            return new byte[0];
        }
    }

    void assertImmutable(final Object o) {
        if (o instanceof Collection) {
            assertThrows(
                UnsupportedOperationException.class,
                new Runnable() { public void run() {
                        ((Collection) o).add(null);}});
        }
    }

    @SuppressWarnings("unchecked")
    <T> T serialClone(T o) {
        try {
            ObjectInputStream ois = new ObjectInputStream
                (new ByteArrayInputStream(serialBytes(o)));
            T clone = (T) ois.readObject();
            if (o == clone) assertImmutable(o);
            assertSame(o.getClass(), clone.getClass());
            return clone;
        } catch (Throwable fail) {
            threadUnexpectedException(fail);
            return null;
        }
    }

    /**
     * A version of serialClone that leaves error handling (for
     * e.g. NotSerializableException) up to the caller.
     */
    @SuppressWarnings("unchecked")
    <T> T serialClonePossiblyFailing(T o)
        throws ReflectiveOperationException, java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        oos.close();
        ObjectInputStream ois = new ObjectInputStream
            (new ByteArrayInputStream(bos.toByteArray()));
        T clone = (T) ois.readObject();
        if (o == clone) assertImmutable(o);
        assertSame(o.getClass(), clone.getClass());
        return clone;
    }

    /**
     * If o implements Cloneable and has a public clone method,
     * returns a clone of o, else null.
     */
    @SuppressWarnings("unchecked")
    <T> T cloneableClone(T o) {
        if (!(o instanceof Cloneable)) return null;
        final T clone;
        try {
            clone = (T) o.getClass().getMethod("clone").invoke(o);
        } catch (NoSuchMethodException ok) {
            return null;
        } catch (ReflectiveOperationException unexpected) {
            throw new Error(unexpected);
        }
        assertNotSame(o, clone); // not 100% guaranteed by spec
        assertSame(o.getClass(), clone.getClass());
        return clone;
    }

    public void assertThrows(Class<? extends Throwable> expectedExceptionClass,
                             Runnable... throwingActions) {
        for (Runnable throwingAction : throwingActions) {
            boolean threw = false;
            try { throwingAction.run(); }
            catch (Throwable t) {
                threw = true;
                if (!expectedExceptionClass.isInstance(t)) {
                    failWithCause(t, "Expected " + expectedExceptionClass.getName() +
                            ", got " + t.getClass().getName());
                }
            }
            if (!threw)
                shouldThrow(expectedExceptionClass.getName());
        }
    }

    public void assertIteratorExhausted(Iterator<?> it) {
        try {
            it.next();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        assertFalse(it.hasNext());
    }

    public <T> Callable<T> callableThrowing(final Exception ex) {
        return new Callable<T>() { public T call() throws Exception { throw ex; }};
    }

    public Runnable runnableThrowing(final RuntimeException ex) {
        return new Runnable() { public void run() { throw ex; }};
    }

    /** A reusable thread pool to be shared by tests. */
    static final ExecutorService cachedThreadPool =
        new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                               1000L, MILLISECONDS,
                               new SynchronousQueue<Runnable>());

    static <T> void shuffle(T[] array) {
        Collections.shuffle(Arrays.asList(array), ThreadLocalRandom.current());
    }
}
