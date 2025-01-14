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

package test.java.util.concurrent.tck;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class ExecutorsTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.ExecutorsTest");
    }
    // public static Test suite() {
    //     return new TestSuite(ExecutorsTest.class);
    // }

    /**
     * A newCachedThreadPool can execute runnables
     */
    @Test
    public void testNewCachedThreadPool1() {
        final ExecutorService e = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * A newCachedThreadPool with given ThreadFactory can execute runnables
     */
    @Test
    public void testNewCachedThreadPool2() {
        final ExecutorService e = Executors.newCachedThreadPool(new SimpleThreadFactory());
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * A newCachedThreadPool with null ThreadFactory throws NPE
     */
    @Test
    public void testNewCachedThreadPool3() {
        try {
            ExecutorService e = Executors.newCachedThreadPool(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A new SingleThreadExecutor can execute runnables
     */
    @Test
    public void testNewSingleThreadExecutor1() {
        final ExecutorService e = Executors.newSingleThreadExecutor();
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * A new SingleThreadExecutor with given ThreadFactory can execute runnables
     */
    @Test
    public void testNewSingleThreadExecutor2() {
        final ExecutorService e = Executors.newSingleThreadExecutor(new SimpleThreadFactory());
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * A new SingleThreadExecutor with null ThreadFactory throws NPE
     */
    @Test
    public void testNewSingleThreadExecutor3() {
        try {
            ExecutorService e = Executors.newSingleThreadExecutor(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A new SingleThreadExecutor cannot be casted to concrete implementation
     */
    @Test
    public void testCastNewSingleThreadExecutor() {
        final ExecutorService e = Executors.newSingleThreadExecutor();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor)e;
                shouldThrow();
            } catch (ClassCastException success) {}
        }
    }

    /**
     * A new newFixedThreadPool can execute runnables
     */
    @Test
    public void testNewFixedThreadPool1() {
        final ExecutorService e = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * A new newFixedThreadPool with given ThreadFactory can execute runnables
     */
    @Test
    public void testNewFixedThreadPool2() {
        final ExecutorService e = Executors.newFixedThreadPool(2, new SimpleThreadFactory());
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * A new newFixedThreadPool with null ThreadFactory throws NPE
     */
    @Test
    public void testNewFixedThreadPool3() {
        try {
            ExecutorService e = Executors.newFixedThreadPool(2, null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A new newFixedThreadPool with 0 threads throws IAE
     */
    @Test
    public void testNewFixedThreadPool4() {
        try {
            ExecutorService e = Executors.newFixedThreadPool(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * An unconfigurable newFixedThreadPool can execute runnables
     */
    @Test
    public void testUnconfigurableExecutorService() {
        final ExecutorService e = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
            e.execute(new NoOpRunnable());
        }
    }

    /**
     * unconfigurableExecutorService(null) throws NPE
     */
    @Test
    public void testUnconfigurableExecutorServiceNPE() {
        try {
            ExecutorService e = Executors.unconfigurableExecutorService(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * unconfigurableScheduledExecutorService(null) throws NPE
     */
    @Test
    public void testUnconfigurableScheduledExecutorServiceNPE() {
        try {
            ExecutorService e = Executors.unconfigurableScheduledExecutorService(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * a newSingleThreadScheduledExecutor successfully runs delayed task
     */
    @Test
    public void testNewSingleThreadScheduledExecutor() throws Exception {
        final ScheduledExecutorService p = Executors.newSingleThreadScheduledExecutor();
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch proceed = new CountDownLatch(1);
            final Runnable task = new CheckedRunnable() {
                public void realRun() {
                    await(proceed);
                }};
            long startTime = System.nanoTime();
            Future f = p.schedule(Executors.callable(task, Boolean.TRUE),
                                  timeoutMillis(), MILLISECONDS);
            assertFalse(f.isDone());
            proceed.countDown();
            assertSame(Boolean.TRUE, f.get(LONG_DELAY_MS, MILLISECONDS));
            assertSame(Boolean.TRUE, f.get());
            assertTrue(f.isDone());
            assertFalse(f.isCancelled());
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        }
    }

    /**
     * a newScheduledThreadPool successfully runs delayed task
     */
    @Test
    public void testNewScheduledThreadPool() throws Exception {
        final ScheduledExecutorService p = Executors.newScheduledThreadPool(2);
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch proceed = new CountDownLatch(1);
            final Runnable task = new CheckedRunnable() {
                public void realRun() {
                    await(proceed);
                }};
            long startTime = System.nanoTime();
            Future f = p.schedule(Executors.callable(task, Boolean.TRUE),
                                  timeoutMillis(), MILLISECONDS);
            assertFalse(f.isDone());
            proceed.countDown();
            assertSame(Boolean.TRUE, f.get(LONG_DELAY_MS, MILLISECONDS));
            assertSame(Boolean.TRUE, f.get());
            assertTrue(f.isDone());
            assertFalse(f.isCancelled());
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        }
    }

    /**
     * an unconfigurable newScheduledThreadPool successfully runs delayed task
     */
    @Test
    public void testUnconfigurableScheduledExecutorService() throws Exception {
        final ScheduledExecutorService p =
            Executors.unconfigurableScheduledExecutorService
            (Executors.newScheduledThreadPool(2));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch proceed = new CountDownLatch(1);
            final Runnable task = new CheckedRunnable() {
                public void realRun() {
                    await(proceed);
                }};
            long startTime = System.nanoTime();
            Future f = p.schedule(Executors.callable(task, Boolean.TRUE),
                                  timeoutMillis(), MILLISECONDS);
            assertFalse(f.isDone());
            proceed.countDown();
            assertSame(Boolean.TRUE, f.get(LONG_DELAY_MS, MILLISECONDS));
            assertSame(Boolean.TRUE, f.get());
            assertTrue(f.isDone());
            assertFalse(f.isCancelled());
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        }
    }

    /**
     * Future.get on submitted tasks will time out if they compute too long.
     */
    @Test
    public void testTimedCallable() throws Exception {
        final ExecutorService[] executors = {
            Executors.newSingleThreadExecutor(),
            Executors.newCachedThreadPool(),
            Executors.newFixedThreadPool(2),
            Executors.newScheduledThreadPool(2),
        };

        final Runnable sleeper = new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                delay(LONG_DELAY_MS);
            }};

        List<Thread> threads = new ArrayList<>();
        for (final ExecutorService executor : executors) {
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() {
                    Future future = executor.submit(sleeper);
                    assertFutureTimesOut(future);
                }}));
        }
        for (Thread thread : threads)
            awaitTermination(thread);
        for (ExecutorService executor : executors)
            joinPool(executor);
    }

    /**
     * ThreadPoolExecutor using defaultThreadFactory has
     * specified group, priority, daemon status, and name
     */
    @Test
    public void testDefaultThreadFactory() throws Exception {
        final ThreadGroup egroup = Thread.currentThread().getThreadGroup();
        final CountDownLatch done = new CountDownLatch(1);
        Runnable r = new CheckedRunnable() {
            public void realRun() {
                try {
                    Thread current = Thread.currentThread();
                    assertTrue(!current.isDaemon());
                    assertTrue(current.getPriority() <= Thread.NORM_PRIORITY);
                    ThreadGroup g = current.getThreadGroup();
                    SecurityManager s = System.getSecurityManager();
                    if (s != null)
                        assertTrue(g == s.getThreadGroup());
                    else
                        assertTrue(g == egroup);
                    String name = current.getName();
                    assertTrue(name.endsWith("thread-1"));
                } catch (SecurityException ok) {
                    // Also pass if not allowed to change setting
                }
                done.countDown();
            }};
        ExecutorService e = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory());
        try (PoolCleaner cleaner = cleaner(e)) {
            e.execute(r);
            await(done);
        }
    }

    /**
     * ThreadPoolExecutor using privilegedThreadFactory has
     * specified group, priority, daemon status, name,
     * access control context and context class loader
     */
    @Test
    @Ignore("Not run in Android")
    public void testPrivilegedThreadFactory() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                final ThreadGroup egroup = Thread.currentThread().getThreadGroup();
                final ClassLoader thisccl = Thread.currentThread().getContextClassLoader();
                final AccessControlContext thisacc = AccessController.getContext();
                Runnable r = new CheckedRunnable() {
                    public void realRun() {
                        Thread current = Thread.currentThread();
                        assertTrue(!current.isDaemon());
                        assertTrue(current.getPriority() <= Thread.NORM_PRIORITY);
                        ThreadGroup g = current.getThreadGroup();
                        SecurityManager s = System.getSecurityManager();
                        if (s != null)
                            assertTrue(g == s.getThreadGroup());
                        else
                            assertTrue(g == egroup);
                        String name = current.getName();
                        assertTrue(name.endsWith("thread-1"));
                        assertSame(thisccl, current.getContextClassLoader());
                        // Android-removed: Android doesn't support real AccessControlContext.
                        // assertEquals(thisacc, AccessController.getCossntext());
                        done.countDown();
                    }};
                ExecutorService e = Executors.newSingleThreadExecutor(Executors.privilegedThreadFactory());
                try (PoolCleaner cleaner = cleaner(e)) {
                    e.execute(r);
                    await(done);
                }
            }};

        runWithPermissions(r,
                           new RuntimePermission("getClassLoader"),
                           new RuntimePermission("setContextClassLoader"),
                           new RuntimePermission("modifyThread"));
    }

    boolean haveCCLPermissions() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new RuntimePermission("setContextClassLoader"));
                sm.checkPermission(new RuntimePermission("getClassLoader"));
            } catch (AccessControlException e) {
                return false;
            }
        }
        return true;
    }

    void checkCCL() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        }
    }

    class CheckCCL implements Callable<Object> {
        public Object call() {
            checkCCL();
            return null;
        }
    }

    /**
     * Without class loader permissions, creating
     * privilegedCallableUsingCurrentClassLoader throws ACE
     */
    @Test
    @Ignore("Not run in Android")
    public void testCreatePrivilegedCallableUsingCCLWithNoPrivs() {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                if (System.getSecurityManager() == null)
                    return;
                try {
                    Executors.privilegedCallableUsingCurrentClassLoader(new NoOpCallable());
                    shouldThrow();
                } catch (AccessControlException success) {}
            }};

        runWithoutPermissions(r);
    }

    /**
     * With class loader permissions, calling
     * privilegedCallableUsingCurrentClassLoader does not throw ACE
     */
    @Test
    @Ignore("Not run in Android")
    public void testPrivilegedCallableUsingCCLWithPrivs() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                Executors.privilegedCallableUsingCurrentClassLoader
                    (new NoOpCallable())
                    .call();
            }};

        runWithPermissions(r,
                           new RuntimePermission("getClassLoader"),
                           new RuntimePermission("setContextClassLoader"));
    }

    /**
     * Without permissions, calling privilegedCallable throws ACE
     */
    @Test
    @Ignore("Not run in Android")
    public void testPrivilegedCallableWithNoPrivs() throws Exception {
        // Avoid classloader-related SecurityExceptions in swingui.TestRunner
        Executors.privilegedCallable(new CheckCCL());

        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                if (System.getSecurityManager() == null)
                    return;
                Callable task = Executors.privilegedCallable(new CheckCCL());
                try {
                    task.call();
                    shouldThrow();
                } catch (AccessControlException success) {}
            }};

        runWithoutPermissions(r);

        // It seems rather difficult to test that the
        // AccessControlContext of the privilegedCallable is used
        // instead of its caller.  Below is a failed attempt to do
        // that, which does not work because the AccessController
        // cannot capture the internal state of the current Policy.
        // It would be much more work to differentiate based on,
        // e.g. CodeSource.

//         final AccessControlContext[] noprivAcc = new AccessControlContext[1];
//         final Callable[] task = new Callable[1];

//         runWithPermissions
//             (new CheckedRunnable() {
//                 public void realRun() {
//                     if (System.getSecurityManager() == null)
//                         return;
//                     noprivAcc[0] = AccessController.getContext();
//                     task[0] = Executors.privilegedCallable(new CheckCCL());
//                     try {
//                         AccessController.doPrivileged(new PrivilegedAction<Void>() {
//                                                           public Void run() {
//                                                               checkCCL();
//                                                               return null;
//                                                           }}, noprivAcc[0]);
//                         shouldThrow();
//                     } catch (AccessControlException success) {}
//                 }});

//         runWithPermissions
//             (new CheckedRunnable() {
//                 public void realRun() throws Exception {
//                     if (System.getSecurityManager() == null)
//                         return;
//                     // Verify that we have an underprivileged ACC
//                     try {
//                         AccessController.doPrivileged(new PrivilegedAction<Void>() {
//                                                           public Void run() {
//                                                               checkCCL();
//                                                               return null;
//                                                           }}, noprivAcc[0]);
//                         shouldThrow();
//                     } catch (AccessControlException success) {}

//                     try {
//                         task[0].call();
//                         shouldThrow();
//                     } catch (AccessControlException success) {}
//                 }},
//              new RuntimePermission("getClassLoader"),
//              new RuntimePermission("setContextClassLoader"));
    }

    /**
     * With permissions, calling privilegedCallable succeeds
     */
    @Test
    @Ignore("Not run in Android")
    public void testPrivilegedCallableWithPrivs() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                Executors.privilegedCallable(new CheckCCL()).call();
            }};

        runWithPermissions(r,
                           new RuntimePermission("getClassLoader"),
                           new RuntimePermission("setContextClassLoader"));
    }

    /**
     * callable(Runnable) returns null when called
     */
    @Test
    public void testCallable1() throws Exception {
        Callable c = Executors.callable(new NoOpRunnable());
        assertNull(c.call());
    }

    /**
     * callable(Runnable, result) returns result when called
     */
    @Test
    public void testCallable2() throws Exception {
        Callable c = Executors.callable(new NoOpRunnable(), one);
        assertSame(one, c.call());
    }

    /**
     * callable(PrivilegedAction) returns its result when called
     */
    @Test
    public void testCallable3() throws Exception {
        Callable c = Executors.callable(new PrivilegedAction() {
                public Object run() { return one; }});
        assertSame(one, c.call());
    }

    /**
     * callable(PrivilegedExceptionAction) returns its result when called
     */
    @Test
    public void testCallable4() throws Exception {
        Callable c = Executors.callable(new PrivilegedExceptionAction() {
                public Object run() { return one; }});
        assertSame(one, c.call());
    }

    /**
     * callable(null Runnable) throws NPE
     */
    @Test
    public void testCallableNPE1() {
        try {
            Callable c = Executors.callable((Runnable) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * callable(null, result) throws NPE
     */
    @Test
    public void testCallableNPE2() {
        try {
            Callable c = Executors.callable((Runnable) null, one);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * callable(null PrivilegedAction) throws NPE
     */
    @Test
    public void testCallableNPE3() {
        try {
            Callable c = Executors.callable((PrivilegedAction) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * callable(null PrivilegedExceptionAction) throws NPE
     */
    @Test
    public void testCallableNPE4() {
        try {
            Callable c = Executors.callable((PrivilegedExceptionAction) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

}
