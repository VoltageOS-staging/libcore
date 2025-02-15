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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class ReentrantLockTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.ReentrantLockTest");
    }
    // public static Test suite() {
    //     return new TestSuite(ReentrantLockTest.class);
    // }

    /**
     * A checked runnable calling lockInterruptibly
     */
    class InterruptibleLockRunnable extends CheckedRunnable {
        final ReentrantLock lock;
        InterruptibleLockRunnable(ReentrantLock lock) { this.lock = lock; }
        public void realRun() throws InterruptedException {
            lock.lockInterruptibly();
        }
    }

    /**
     * A checked runnable calling lockInterruptibly that expects to be
     * interrupted
     */
    class InterruptedLockRunnable extends CheckedInterruptedRunnable {
        final ReentrantLock lock;
        InterruptedLockRunnable(ReentrantLock lock) { this.lock = lock; }
        public void realRun() throws InterruptedException {
            lock.lockInterruptibly();
        }
    }

    /**
     * Subclass to expose protected methods
     */
    static class PublicReentrantLock extends ReentrantLock {
        PublicReentrantLock() { super(); }
        PublicReentrantLock(boolean fair) { super(fair); }
        public Thread getOwner() {
            return super.getOwner();
        }
        public Collection<Thread> getQueuedThreads() {
            return super.getQueuedThreads();
        }
        public Collection<Thread> getWaitingThreads(Condition c) {
            return super.getWaitingThreads(c);
        }
    }

    /**
     * Releases write lock, checking that it had a hold count of 1.
     */
    void releaseLock(PublicReentrantLock lock) {
        assertLockedByMoi(lock);
        lock.unlock();
        assertFalse(lock.isHeldByCurrentThread());
        assertNotLocked(lock);
    }

    /**
     * Spin-waits until lock.hasQueuedThread(t) becomes true.
     */
    void waitForQueuedThread(PublicReentrantLock lock, Thread t) {
        long startTime = System.nanoTime();
        while (!lock.hasQueuedThread(t)) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out");
            Thread.yield();
        }
        assertTrue(t.isAlive());
        assertNotSame(t, lock.getOwner());
    }

    /**
     * Checks that lock is not locked.
     */
    void assertNotLocked(PublicReentrantLock lock) {
        assertFalse(lock.isLocked());
        assertFalse(lock.isHeldByCurrentThread());
        assertNull(lock.getOwner());
        assertEquals(0, lock.getHoldCount());
    }

    /**
     * Checks that lock is locked by the given thread.
     */
    void assertLockedBy(PublicReentrantLock lock, Thread t) {
        assertTrue(lock.isLocked());
        assertSame(t, lock.getOwner());
        assertEquals(t == Thread.currentThread(),
                     lock.isHeldByCurrentThread());
        assertEquals(t == Thread.currentThread(),
                     lock.getHoldCount() > 0);
    }

    /**
     * Checks that lock is locked by the current thread.
     */
    void assertLockedByMoi(PublicReentrantLock lock) {
        assertLockedBy(lock, Thread.currentThread());
    }

    /**
     * Checks that condition c has no waiters.
     */
    void assertHasNoWaiters(PublicReentrantLock lock, Condition c) {
        assertHasWaiters(lock, c, new Thread[] {});
    }

    /**
     * Checks that condition c has exactly the given waiter threads.
     */
    void assertHasWaiters(PublicReentrantLock lock, Condition c,
                          Thread... threads) {
        lock.lock();
        assertEquals(threads.length > 0, lock.hasWaiters(c));
        assertEquals(threads.length, lock.getWaitQueueLength(c));
        assertEquals(threads.length == 0, lock.getWaitingThreads(c).isEmpty());
        assertEquals(threads.length, lock.getWaitingThreads(c).size());
        assertEquals(new HashSet<Thread>(lock.getWaitingThreads(c)),
                     new HashSet<Thread>(Arrays.asList(threads)));
        lock.unlock();
    }

    enum AwaitMethod { await, awaitTimed, awaitNanos, awaitUntil }

    /**
     * Awaits condition "indefinitely" using the specified AwaitMethod.
     */
    void await(Condition c, AwaitMethod awaitMethod)
            throws InterruptedException {
        long timeoutMillis = 2 * LONG_DELAY_MS;
        switch (awaitMethod) {
        case await:
            c.await();
            break;
        case awaitTimed:
            assertTrue(c.await(timeoutMillis, MILLISECONDS));
            break;
        case awaitNanos:
            long timeoutNanos = MILLISECONDS.toNanos(timeoutMillis);
            long nanosRemaining = c.awaitNanos(timeoutNanos);
            assertTrue(nanosRemaining > timeoutNanos / 2);
            assertTrue(nanosRemaining <= timeoutNanos);
            break;
        case awaitUntil:
            assertTrue(c.awaitUntil(delayedDate(timeoutMillis)));
            break;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Constructor sets given fairness, and is in unlocked state
     */
    @Test
    public void testConstructor() {
        PublicReentrantLock lock;

        lock = new PublicReentrantLock();
        assertFalse(lock.isFair());
        assertNotLocked(lock);

        lock = new PublicReentrantLock(true);
        assertTrue(lock.isFair());
        assertNotLocked(lock);

        lock = new PublicReentrantLock(false);
        assertFalse(lock.isFair());
        assertNotLocked(lock);
    }

    /**
     * locking an unlocked lock succeeds
     */
    @Test
    public void testLock()      { testLock(false); }
    @Test
    public void testLock_fair() { testLock(true); }
    private void testLock(boolean fair) {
        PublicReentrantLock lock = new PublicReentrantLock(fair);
        lock.lock();
        assertLockedByMoi(lock);
        releaseLock(lock);
    }

    /**
     * Unlocking an unlocked lock throws IllegalMonitorStateException
     */
    @Test
    public void testUnlock_IMSE()      { testUnlock_IMSE(false); }
    @Test
    public void testUnlock_IMSE_fair() { testUnlock_IMSE(true); }
    private void testUnlock_IMSE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        try {
            lock.unlock();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * tryLock on an unlocked lock succeeds
     */
    @Test
    public void testTryLock()      { testTryLock(false); }
    @Test
    public void testTryLock_fair() { testTryLock(true); }
    private void testTryLock(boolean fair) {
        PublicReentrantLock lock = new PublicReentrantLock(fair);
        assertTrue(lock.tryLock());
        assertLockedByMoi(lock);
        assertTrue(lock.tryLock());
        assertLockedByMoi(lock);
        lock.unlock();
        releaseLock(lock);
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    @Test
    public void testHasQueuedThreads()      { testHasQueuedThreads(false); }
    @Test
    public void testHasQueuedThreads_fair() { testHasQueuedThreads(true); }
    private void testHasQueuedThreads(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertFalse(lock.hasQueuedThreads());
        lock.lock();
        assertFalse(lock.hasQueuedThreads());
        t1.start();
        waitForQueuedThread(lock, t1);
        assertTrue(lock.hasQueuedThreads());
        t2.start();
        waitForQueuedThread(lock, t2);
        assertTrue(lock.hasQueuedThreads());
        t1.interrupt();
        awaitTermination(t1);
        assertTrue(lock.hasQueuedThreads());
        lock.unlock();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThreads());
    }

    /**
     * getQueueLength reports number of waiting threads
     */
    @Test
    public void testGetQueueLength()      { testGetQueueLength(false); }
    @Test
    public void testGetQueueLength_fair() { testGetQueueLength(true); }
    private void testGetQueueLength(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertEquals(0, lock.getQueueLength());
        lock.lock();
        t1.start();
        waitForQueuedThread(lock, t1);
        assertEquals(1, lock.getQueueLength());
        t2.start();
        waitForQueuedThread(lock, t2);
        assertEquals(2, lock.getQueueLength());
        t1.interrupt();
        awaitTermination(t1);
        assertEquals(1, lock.getQueueLength());
        lock.unlock();
        awaitTermination(t2);
        assertEquals(0, lock.getQueueLength());
    }

    /**
     * hasQueuedThread(null) throws NPE
     */
    @Test
    public void testHasQueuedThreadNPE()      { testHasQueuedThreadNPE(false); }
    @Test
    public void testHasQueuedThreadNPE_fair() { testHasQueuedThreadNPE(true); }
    private void testHasQueuedThreadNPE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        try {
            lock.hasQueuedThread(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * hasQueuedThread reports whether a thread is queued
     */
    @Test
    public void testHasQueuedThread()      { testHasQueuedThread(false); }
    @Test
    public void testHasQueuedThread_fair() { testHasQueuedThread(true); }
    private void testHasQueuedThread(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertFalse(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        lock.lock();
        t1.start();
        waitForQueuedThread(lock, t1);
        assertTrue(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        t2.start();
        waitForQueuedThread(lock, t2);
        assertTrue(lock.hasQueuedThread(t1));
        assertTrue(lock.hasQueuedThread(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertFalse(lock.hasQueuedThread(t1));
        assertTrue(lock.hasQueuedThread(t2));
        lock.unlock();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
    }

    /**
     * getQueuedThreads includes waiting threads
     */
    @Test
    public void testGetQueuedThreads()      { testGetQueuedThreads(false); }
    @Test
    public void testGetQueuedThreads_fair() { testGetQueuedThreads(true); }
    private void testGetQueuedThreads(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertTrue(lock.getQueuedThreads().isEmpty());
        lock.lock();
        assertTrue(lock.getQueuedThreads().isEmpty());
        t1.start();
        waitForQueuedThread(lock, t1);
        assertEquals(1, lock.getQueuedThreads().size());
        assertTrue(lock.getQueuedThreads().contains(t1));
        t2.start();
        waitForQueuedThread(lock, t2);
        assertEquals(2, lock.getQueuedThreads().size());
        assertTrue(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertFalse(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        assertEquals(1, lock.getQueuedThreads().size());
        lock.unlock();
        awaitTermination(t2);
        assertTrue(lock.getQueuedThreads().isEmpty());
    }

    /**
     * timed tryLock is interruptible
     */
    @Test
    public void testTryLock_Interruptible()      { testTryLock_Interruptible(false); }
    @Test
    public void testTryLock_Interruptible_fair() { testTryLock_Interruptible(true); }
    private void testTryLock_Interruptible(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        lock.lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.tryLock(2 * LONG_DELAY_MS, MILLISECONDS);
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseLock(lock);
    }

    /**
     * tryLock on a locked lock fails
     */
    @Test
    public void testTryLockWhenLocked()      { testTryLockWhenLocked(false); }
    @Test
    public void testTryLockWhenLocked_fair() { testTryLockWhenLocked(true); }
    private void testTryLockWhenLocked(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        lock.lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.tryLock());
            }});

        awaitTermination(t);
        releaseLock(lock);
    }

    /**
     * Timed tryLock on a locked lock times out
     */
    @Test
    public void testTryLock_Timeout()      { testTryLock_Timeout(false); }
    @Test
    public void testTryLock_Timeout_fair() { testTryLock_Timeout(true); }
    private void testTryLock_Timeout(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final long timeoutMillis = timeoutMillis();
        lock.lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertFalse(lock.tryLock(timeoutMillis, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
            }});

        awaitTermination(t);
        releaseLock(lock);
    }

    /**
     * getHoldCount returns number of recursive holds
     */
    @Test
    public void testGetHoldCount()      { testGetHoldCount(false); }
    @Test
    public void testGetHoldCount_fair() { testGetHoldCount(true); }
    private void testGetHoldCount(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.lock();
            assertEquals(i, lock.getHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.unlock();
            assertEquals(i - 1, lock.getHoldCount());
        }
    }

    /**
     * isLocked is true when locked and false when not
     */
    @Test
    public void testIsLocked()      { testIsLocked(false); }
    @Test
    public void testIsLocked_fair() { testIsLocked(true); }
    private void testIsLocked(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        try {
            assertFalse(lock.isLocked());
            lock.lock();
            assertTrue(lock.isLocked());
            lock.lock();
            assertTrue(lock.isLocked());
            lock.unlock();
            assertTrue(lock.isLocked());
            lock.unlock();
            assertFalse(lock.isLocked());
            final CyclicBarrier barrier = new CyclicBarrier(2);
            Thread t = newStartedThread(new CheckedRunnable() {
                    public void realRun() throws Exception {
                        lock.lock();
                        assertTrue(lock.isLocked());
                        barrier.await();
                        barrier.await();
                        lock.unlock();
                    }});

            barrier.await();
            assertTrue(lock.isLocked());
            barrier.await();
            awaitTermination(t);
            assertFalse(lock.isLocked());
        } catch (Exception fail) { threadUnexpectedException(fail); }
    }

    /**
     * lockInterruptibly succeeds when unlocked, else is interruptible
     */
    @Test
    public void testLockInterruptibly()      { testLockInterruptibly(false); }
    @Test
    public void testLockInterruptibly_fair() { testLockInterruptibly(true); }
    private void testLockInterruptibly(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertLockedByMoi(lock);
        Thread t = newStartedThread(new InterruptedLockRunnable(lock));
        waitForQueuedThread(lock, t);
        t.interrupt();
        assertTrue(lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());
        awaitTermination(t);
        releaseLock(lock);
    }

    /**
     * Calling await without holding lock throws IllegalMonitorStateException
     */
    @Test
    public void testAwait_IMSE()      { testAwait_IMSE(false); }
    @Test
    public void testAwait_IMSE_fair() { testAwait_IMSE(true); }
    private void testAwait_IMSE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        for (AwaitMethod awaitMethod : AwaitMethod.values()) {
            long startTime = System.nanoTime();
            try {
                await(c, awaitMethod);
                shouldThrow();
            } catch (IllegalMonitorStateException success) {
            } catch (InterruptedException e) { threadUnexpectedException(e); }
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * Calling signal without holding lock throws IllegalMonitorStateException
     */
    @Test
    public void testSignal_IMSE()      { testSignal_IMSE(false); }
    @Test
    public void testSignal_IMSE_fair() { testSignal_IMSE(true); }
    private void testSignal_IMSE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        try {
            c.signal();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * awaitNanos without a signal times out
     */
    @Test
    public void testAwaitNanos_Timeout()      { testAwaitNanos_Timeout(false); }
    @Test
    public void testAwaitNanos_Timeout_fair() { testAwaitNanos_Timeout(true); }
    private void testAwaitNanos_Timeout(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        final long timeoutMillis = timeoutMillis();
        final long timeoutNanos = MILLISECONDS.toNanos(timeoutMillis);
        lock.lock();
        final long startTime = System.nanoTime();
        try {
            long nanosRemaining = c.awaitNanos(timeoutNanos);
            assertTrue(nanosRemaining <= 0);
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
        lock.unlock();
    }

    /**
     * timed await without a signal times out
     */
    @Test
    public void testAwait_Timeout()      { testAwait_Timeout(false); }
    @Test
    public void testAwait_Timeout_fair() { testAwait_Timeout(true); }
    private void testAwait_Timeout(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        final long timeoutMillis = timeoutMillis();
        lock.lock();
        final long startTime = System.nanoTime();
        try {
            assertFalse(c.await(timeoutMillis, MILLISECONDS));
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
        lock.unlock();
    }

    /**
     * awaitUntil without a signal times out
     */
    @Test
    public void testAwaitUntil_Timeout()      { testAwaitUntil_Timeout(false); }
    @Test
    public void testAwaitUntil_Timeout_fair() { testAwaitUntil_Timeout(true); }
    private void testAwaitUntil_Timeout(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        lock.lock();
        // We shouldn't assume that nanoTime and currentTimeMillis
        // use the same time source, so don't use nanoTime here.
        final java.util.Date delayedDate = delayedDate(timeoutMillis());
        try {
            assertFalse(c.awaitUntil(delayedDate));
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(new java.util.Date().getTime() >= delayedDate.getTime());
        lock.unlock();
    }

    /**
     * await returns when signalled
     */
    @Test
    public void testAwait()      { testAwait(false); }
    @Test
    public void testAwait_fair() { testAwait(true); }
    private void testAwait(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                locked.countDown();
                c.await();
                lock.unlock();
            }});

        await(locked);
        lock.lock();
        assertHasWaiters(lock, c, t);
        c.signal();
        assertHasNoWaiters(lock, c);
        assertTrue(t.isAlive());
        lock.unlock();
        awaitTermination(t);
    }

    /**
     * hasWaiters throws NPE if null
     */
    @Test
    public void testHasWaitersNPE()      { testHasWaitersNPE(false); }
    @Test
    public void testHasWaitersNPE_fair() { testHasWaitersNPE(true); }
    private void testHasWaitersNPE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        try {
            lock.hasWaiters(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitQueueLength throws NPE if null
     */
    @Test
    public void testGetWaitQueueLengthNPE()      { testGetWaitQueueLengthNPE(false); }
    @Test
    public void testGetWaitQueueLengthNPE_fair() { testGetWaitQueueLengthNPE(true); }
    private void testGetWaitQueueLengthNPE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        try {
            lock.getWaitQueueLength(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitingThreads throws NPE if null
     */
    @Test
    public void testGetWaitingThreadsNPE()      { testGetWaitingThreadsNPE(false); }
    @Test
    public void testGetWaitingThreadsNPE_fair() { testGetWaitingThreadsNPE(true); }
    private void testGetWaitingThreadsNPE(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        try {
            lock.getWaitingThreads(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * hasWaiters throws IllegalArgumentException if not owned
     */
    @Test
    public void testHasWaitersIAE()      { testHasWaitersIAE(false); }
    @Test
    public void testHasWaitersIAE_fair() { testHasWaitersIAE(true); }
    private void testHasWaitersIAE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        final ReentrantLock lock2 = new ReentrantLock(fair);
        try {
            lock2.hasWaiters(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * hasWaiters throws IllegalMonitorStateException if not locked
     */
    @Test
    public void testHasWaitersIMSE()      { testHasWaitersIMSE(false); }
    @Test
    public void testHasWaitersIMSE_fair() { testHasWaitersIMSE(true); }
    private void testHasWaitersIMSE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        try {
            lock.hasWaiters(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * getWaitQueueLength throws IllegalArgumentException if not owned
     */
    @Test
    public void testGetWaitQueueLengthIAE()      { testGetWaitQueueLengthIAE(false); }
    @Test
    public void testGetWaitQueueLengthIAE_fair() { testGetWaitQueueLengthIAE(true); }
    private void testGetWaitQueueLengthIAE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        final ReentrantLock lock2 = new ReentrantLock(fair);
        try {
            lock2.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getWaitQueueLength throws IllegalMonitorStateException if not locked
     */
    @Test
    public void testGetWaitQueueLengthIMSE()      { testGetWaitQueueLengthIMSE(false); }
    @Test
    public void testGetWaitQueueLengthIMSE_fair() { testGetWaitQueueLengthIMSE(true); }
    private void testGetWaitQueueLengthIMSE(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        try {
            lock.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * getWaitingThreads throws IllegalArgumentException if not owned
     */
    @Test
    public void testGetWaitingThreadsIAE()      { testGetWaitingThreadsIAE(false); }
    @Test
    public void testGetWaitingThreadsIAE_fair() { testGetWaitingThreadsIAE(true); }
    private void testGetWaitingThreadsIAE(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final PublicReentrantLock lock2 = new PublicReentrantLock(fair);
        try {
            lock2.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getWaitingThreads throws IllegalMonitorStateException if not locked
     */
    @Test
    public void testGetWaitingThreadsIMSE()      { testGetWaitingThreadsIMSE(false); }
    @Test
    public void testGetWaitingThreadsIMSE_fair() { testGetWaitingThreadsIMSE(true); }
    private void testGetWaitingThreadsIMSE(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        try {
            lock.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * hasWaiters returns true when a thread is waiting, else false
     */
    @Test
    public void testHasWaiters()      { testHasWaiters(false); }
    @Test
    public void testHasWaiters_fair() { testHasWaiters(true); }
    private void testHasWaiters(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch pleaseSignal = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertHasNoWaiters(lock, c);
                assertFalse(lock.hasWaiters(c));
                pleaseSignal.countDown();
                c.await();
                assertHasNoWaiters(lock, c);
                assertFalse(lock.hasWaiters(c));
                lock.unlock();
            }});

        await(pleaseSignal);
        lock.lock();
        assertHasWaiters(lock, c, t);
        assertTrue(lock.hasWaiters(c));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertFalse(lock.hasWaiters(c));
        lock.unlock();
        awaitTermination(t);
        assertHasNoWaiters(lock, c);
    }

    /**
     * getWaitQueueLength returns number of waiting threads
     */
    @Test
    public void testGetWaitQueueLength()      { testGetWaitQueueLength(false); }
    @Test
    public void testGetWaitQueueLength_fair() { testGetWaitQueueLength(true); }
    private void testGetWaitQueueLength(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertFalse(lock.hasWaiters(c));
                assertEquals(0, lock.getWaitQueueLength(c));
                locked1.countDown();
                c.await();
                lock.unlock();
            }});

        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertTrue(lock.hasWaiters(c));
                assertEquals(1, lock.getWaitQueueLength(c));
                locked2.countDown();
                c.await();
                lock.unlock();
            }});

        lock.lock();
        assertEquals(0, lock.getWaitQueueLength(c));
        lock.unlock();

        t1.start();
        await(locked1);

        lock.lock();
        assertHasWaiters(lock, c, t1);
        assertEquals(1, lock.getWaitQueueLength(c));
        lock.unlock();

        t2.start();
        await(locked2);

        lock.lock();
        assertHasWaiters(lock, c, t1, t2);
        assertEquals(2, lock.getWaitQueueLength(c));
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.unlock();

        awaitTermination(t1);
        awaitTermination(t2);

        assertHasNoWaiters(lock, c);
    }

    /**
     * getWaitingThreads returns only and all waiting threads
     */
    @Test
    public void testGetWaitingThreads()      { testGetWaitingThreads(false); }
    @Test
    public void testGetWaitingThreads_fair() { testGetWaitingThreads(true); }
    private void testGetWaitingThreads(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertTrue(lock.getWaitingThreads(c).isEmpty());
                locked1.countDown();
                c.await();
                lock.unlock();
            }});

        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertFalse(lock.getWaitingThreads(c).isEmpty());
                locked2.countDown();
                c.await();
                lock.unlock();
            }});

        lock.lock();
        assertTrue(lock.getWaitingThreads(c).isEmpty());
        lock.unlock();

        t1.start();
        await(locked1);

        lock.lock();
        assertHasWaiters(lock, c, t1);
        assertTrue(lock.getWaitingThreads(c).contains(t1));
        assertFalse(lock.getWaitingThreads(c).contains(t2));
        assertEquals(1, lock.getWaitingThreads(c).size());
        lock.unlock();

        t2.start();
        await(locked2);

        lock.lock();
        assertHasWaiters(lock, c, t1, t2);
        assertTrue(lock.getWaitingThreads(c).contains(t1));
        assertTrue(lock.getWaitingThreads(c).contains(t2));
        assertEquals(2, lock.getWaitingThreads(c).size());
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.unlock();

        awaitTermination(t1);
        awaitTermination(t2);

        assertHasNoWaiters(lock, c);
    }

    /**
     * awaitUninterruptibly is uninterruptible
     */
    @Test
    public void testAwaitUninterruptibly()      { testAwaitUninterruptibly(false); }
    @Test
    public void testAwaitUninterruptibly_fair() { testAwaitUninterruptibly(true); }
    private void testAwaitUninterruptibly(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch pleaseInterrupt = new CountDownLatch(2);

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt before awaitUninterruptibly
                lock.lock();
                pleaseInterrupt.countDown();
                Thread.currentThread().interrupt();
                c.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                lock.unlock();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt during awaitUninterruptibly
                lock.lock();
                pleaseInterrupt.countDown();
                c.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                lock.unlock();
            }});

        await(pleaseInterrupt);
        lock.lock();
        lock.unlock();
        t2.interrupt();

        assertThreadStaysAlive(t1);
        assertTrue(t2.isAlive());

        lock.lock();
        c.signalAll();
        lock.unlock();

        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * await/awaitNanos/awaitUntil is interruptible
     */
    @Test
    public void testInterruptible_await()           { testInterruptible(false, AwaitMethod.await); }
    @Test
    public void testInterruptible_await_fair()      { testInterruptible(true,  AwaitMethod.await); }
    @Test
    public void testInterruptible_awaitTimed()      { testInterruptible(false, AwaitMethod.awaitTimed); }
    @Test
    public void testInterruptible_awaitTimed_fair() { testInterruptible(true,  AwaitMethod.awaitTimed); }
    @Test
    public void testInterruptible_awaitNanos()      { testInterruptible(false, AwaitMethod.awaitNanos); }
    @Test
    public void testInterruptible_awaitNanos_fair() { testInterruptible(true,  AwaitMethod.awaitNanos); }
    @Test
    public void testInterruptible_awaitUntil()      { testInterruptible(false, AwaitMethod.awaitUntil); }
    @Test
    public void testInterruptible_awaitUntil_fair() { testInterruptible(true,  AwaitMethod.awaitUntil); }
    private void testInterruptible(boolean fair, final AwaitMethod awaitMethod) {
        final PublicReentrantLock lock =
            new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertLockedByMoi(lock);
                assertHasNoWaiters(lock, c);
                pleaseInterrupt.countDown();
                try {
                    await(c, awaitMethod);
                } finally {
                    assertLockedByMoi(lock);
                    assertHasNoWaiters(lock, c);
                    lock.unlock();
                    assertFalse(Thread.interrupted());
                }
            }});

        await(pleaseInterrupt);
        assertHasWaiters(lock, c, t);
        t.interrupt();
        awaitTermination(t);
        assertNotLocked(lock);
    }

    /**
     * signalAll wakes up all threads
     */
    @Test
    public void testSignalAll_await()           { testSignalAll(false, AwaitMethod.await); }
    @Test
    public void testSignalAll_await_fair()      { testSignalAll(true,  AwaitMethod.await); }
    @Test
    public void testSignalAll_awaitTimed()      { testSignalAll(false, AwaitMethod.awaitTimed); }
    @Test
    public void testSignalAll_awaitTimed_fair() { testSignalAll(true,  AwaitMethod.awaitTimed); }
    @Test
    public void testSignalAll_awaitNanos()      { testSignalAll(false, AwaitMethod.awaitNanos); }
    @Test
    public void testSignalAll_awaitNanos_fair() { testSignalAll(true,  AwaitMethod.awaitNanos); }
    @Test
    public void testSignalAll_awaitUntil()      { testSignalAll(false, AwaitMethod.awaitUntil); }
    @Test
    public void testSignalAll_awaitUntil_fair() { testSignalAll(true,  AwaitMethod.awaitUntil); }
    private void testSignalAll(boolean fair, final AwaitMethod awaitMethod) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch pleaseSignal = new CountDownLatch(2);
        class Awaiter extends CheckedRunnable {
            public void realRun() throws InterruptedException {
                lock.lock();
                pleaseSignal.countDown();
                await(c, awaitMethod);
                lock.unlock();
            }
        }

        Thread t1 = newStartedThread(new Awaiter());
        Thread t2 = newStartedThread(new Awaiter());

        await(pleaseSignal);
        lock.lock();
        assertHasWaiters(lock, c, t1, t2);
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * signal wakes up waiting threads in FIFO order
     */
    @Test
    public void testSignalWakesFifo()      { testSignalWakesFifo(false); }
    @Test
    public void testSignalWakesFifo_fair() { testSignalWakesFifo(true); }
    private void testSignalWakesFifo(boolean fair) {
        final PublicReentrantLock lock =
            new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                locked1.countDown();
                c.await();
                lock.unlock();
            }});

        await(locked1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                locked2.countDown();
                c.await();
                lock.unlock();
            }});

        await(locked2);

        lock.lock();
        assertHasWaiters(lock, c, t1, t2);
        assertFalse(lock.hasQueuedThreads());
        c.signal();
        assertHasWaiters(lock, c, t2);
        assertTrue(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertTrue(lock.hasQueuedThread(t1));
        assertTrue(lock.hasQueuedThread(t2));
        lock.unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * await after multiple reentrant locking preserves lock count
     */
    @Test
    public void testAwaitLockCount()      { testAwaitLockCount(false); }
    @Test
    public void testAwaitLockCount_fair() { testAwaitLockCount(true); }
    private void testAwaitLockCount(boolean fair) {
        final PublicReentrantLock lock = new PublicReentrantLock(fair);
        final Condition c = lock.newCondition();
        final CountDownLatch pleaseSignal = new CountDownLatch(2);
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                assertLockedByMoi(lock);
                assertEquals(1, lock.getHoldCount());
                pleaseSignal.countDown();
                c.await();
                assertLockedByMoi(lock);
                assertEquals(1, lock.getHoldCount());
                lock.unlock();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.lock();
                lock.lock();
                assertLockedByMoi(lock);
                assertEquals(2, lock.getHoldCount());
                pleaseSignal.countDown();
                c.await();
                assertLockedByMoi(lock);
                assertEquals(2, lock.getHoldCount());
                lock.unlock();
                lock.unlock();
            }});

        await(pleaseSignal);
        lock.lock();
        assertHasWaiters(lock, c, t1, t2);
        assertEquals(1, lock.getHoldCount());
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * A serialized lock deserializes as unlocked
     */
    @Test
    public void testSerialization()      { testSerialization(false); }
    @Test
    public void testSerialization_fair() { testSerialization(true); }
    private void testSerialization(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        lock.lock();

        ReentrantLock clone = serialClone(lock);
        assertEquals(lock.isFair(), clone.isFair());
        assertTrue(lock.isLocked());
        assertFalse(clone.isLocked());
        assertEquals(1, lock.getHoldCount());
        assertEquals(0, clone.getHoldCount());
        clone.lock();
        clone.lock();
        assertTrue(clone.isLocked());
        assertEquals(2, clone.getHoldCount());
        assertEquals(1, lock.getHoldCount());
        clone.unlock();
        clone.unlock();
        assertTrue(lock.isLocked());
        assertFalse(clone.isLocked());
    }

    /**
     * toString indicates current lock state
     */
    @Test
    public void testToString()      { testToString(false); }
    @Test
    public void testToString_fair() { testToString(true); }
    private void testToString(boolean fair) {
        final ReentrantLock lock = new ReentrantLock(fair);
        assertTrue(lock.toString().contains("Unlocked"));
        lock.lock();
        assertTrue(lock.toString().contains("Locked by"));
        lock.unlock();
        assertTrue(lock.toString().contains("Unlocked"));
    }
}
