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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class ReentrantReadWriteLockTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.ReentrantReadWriteLockTest");
    }
    // public static Test suite() {
    //     return new TestSuite(ReentrantReadWriteLockTest.class);
    // }

    /**
     * A runnable calling lockInterruptibly
     */
    class InterruptibleLockRunnable extends CheckedRunnable {
        final ReentrantReadWriteLock lock;
        InterruptibleLockRunnable(ReentrantReadWriteLock l) { lock = l; }
        public void realRun() throws InterruptedException {
            lock.writeLock().lockInterruptibly();
        }
    }

    /**
     * A runnable calling lockInterruptibly that expects to be
     * interrupted
     */
    class InterruptedLockRunnable extends CheckedInterruptedRunnable {
        final ReentrantReadWriteLock lock;
        InterruptedLockRunnable(ReentrantReadWriteLock l) { lock = l; }
        public void realRun() throws InterruptedException {
            lock.writeLock().lockInterruptibly();
        }
    }

    /**
     * Subclass to expose protected methods
     */
    static class PublicReentrantReadWriteLock extends ReentrantReadWriteLock {
        PublicReentrantReadWriteLock() { super(); }
        PublicReentrantReadWriteLock(boolean fair) { super(fair); }
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
    void releaseWriteLock(PublicReentrantReadWriteLock lock) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        assertWriteLockedByMoi(lock);
        assertEquals(1, lock.getWriteHoldCount());
        writeLock.unlock();
        assertNotWriteLocked(lock);
    }

    /**
     * Spin-waits until lock.hasQueuedThread(t) becomes true.
     */
    void waitForQueuedThread(PublicReentrantReadWriteLock lock, Thread t) {
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
     * Checks that lock is not write-locked.
     */
    void assertNotWriteLocked(PublicReentrantReadWriteLock lock) {
        assertFalse(lock.isWriteLocked());
        assertFalse(lock.isWriteLockedByCurrentThread());
        assertFalse(lock.writeLock().isHeldByCurrentThread());
        assertEquals(0, lock.getWriteHoldCount());
        assertEquals(0, lock.writeLock().getHoldCount());
        assertNull(lock.getOwner());
    }

    /**
     * Checks that lock is write-locked by the given thread.
     */
    void assertWriteLockedBy(PublicReentrantReadWriteLock lock, Thread t) {
        assertTrue(lock.isWriteLocked());
        assertSame(t, lock.getOwner());
        assertEquals(t == Thread.currentThread(),
                     lock.isWriteLockedByCurrentThread());
        assertEquals(t == Thread.currentThread(),
                     lock.writeLock().isHeldByCurrentThread());
        assertEquals(t == Thread.currentThread(),
                     lock.getWriteHoldCount() > 0);
        assertEquals(t == Thread.currentThread(),
                     lock.writeLock().getHoldCount() > 0);
        assertEquals(0, lock.getReadLockCount());
    }

    /**
     * Checks that lock is write-locked by the current thread.
     */
    void assertWriteLockedByMoi(PublicReentrantReadWriteLock lock) {
        assertWriteLockedBy(lock, Thread.currentThread());
    }

    /**
     * Checks that condition c has no waiters.
     */
    void assertHasNoWaiters(PublicReentrantReadWriteLock lock, Condition c) {
        assertHasWaiters(lock, c, new Thread[] {});
    }

    /**
     * Checks that condition c has exactly the given waiter threads.
     */
    void assertHasWaiters(PublicReentrantReadWriteLock lock, Condition c,
                          Thread... threads) {
        lock.writeLock().lock();
        assertEquals(threads.length > 0, lock.hasWaiters(c));
        assertEquals(threads.length, lock.getWaitQueueLength(c));
        assertEquals(threads.length == 0, lock.getWaitingThreads(c).isEmpty());
        assertEquals(threads.length, lock.getWaitingThreads(c).size());
        assertEquals(new HashSet<Thread>(lock.getWaitingThreads(c)),
                     new HashSet<Thread>(Arrays.asList(threads)));
        lock.writeLock().unlock();
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
        PublicReentrantReadWriteLock lock;

        lock = new PublicReentrantReadWriteLock();
        assertFalse(lock.isFair());
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());

        lock = new PublicReentrantReadWriteLock(true);
        assertTrue(lock.isFair());
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());

        lock = new PublicReentrantReadWriteLock(false);
        assertFalse(lock.isFair());
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());
    }

    /**
     * write-locking and read-locking an unlocked lock succeed
     */
    @Test
    public void testLock()      { testLock(false); }
    @Test
    public void testLock_fair() { testLock(true); }
    private void testLock(boolean fair) {
        PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        assertNotWriteLocked(lock);
        lock.writeLock().lock();
        assertWriteLockedByMoi(lock);
        lock.writeLock().unlock();
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());
        lock.readLock().lock();
        assertNotWriteLocked(lock);
        assertEquals(1, lock.getReadLockCount());
        lock.readLock().unlock();
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());
    }

    /**
     * getWriteHoldCount returns number of recursive holds
     */
    @Test
    public void testGetWriteHoldCount()      { testGetWriteHoldCount(false); }
    @Test
    public void testGetWriteHoldCount_fair() { testGetWriteHoldCount(true); }
    private void testGetWriteHoldCount(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.writeLock().lock();
            assertEquals(i,lock.getWriteHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.writeLock().unlock();
            assertEquals(i - 1,lock.getWriteHoldCount());
        }
    }

    /**
     * writelock.getHoldCount returns number of recursive holds
     */
    @Test
    public void testGetHoldCount()      { testGetHoldCount(false); }
    @Test
    public void testGetHoldCount_fair() { testGetHoldCount(true); }
    private void testGetHoldCount(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.writeLock().lock();
            assertEquals(i,lock.writeLock().getHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.writeLock().unlock();
            assertEquals(i - 1,lock.writeLock().getHoldCount());
        }
    }

    /**
     * getReadHoldCount returns number of recursive holds
     */
    @Test
    public void testGetReadHoldCount()      { testGetReadHoldCount(false); }
    @Test
    public void testGetReadHoldCount_fair() { testGetReadHoldCount(true); }
    private void testGetReadHoldCount(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.readLock().lock();
            assertEquals(i,lock.getReadHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.readLock().unlock();
            assertEquals(i - 1,lock.getReadHoldCount());
        }
    }

    /**
     * write-unlocking an unlocked lock throws IllegalMonitorStateException
     */
    @Test
    public void testWriteUnlock_IMSE()      { testWriteUnlock_IMSE(false); }
    @Test
    public void testWriteUnlock_IMSE_fair() { testWriteUnlock_IMSE(true); }
    private void testWriteUnlock_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.writeLock().unlock();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * read-unlocking an unlocked lock throws IllegalMonitorStateException
     */
    @Test
    public void testReadUnlock_IMSE()      { testReadUnlock_IMSE(false); }
    @Test
    public void testReadUnlock_IMSE_fair() { testReadUnlock_IMSE(true); }
    private void testReadUnlock_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.readLock().unlock();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * write-lockInterruptibly is interruptible
     */
    @Test
    public void testWriteLockInterruptibly_Interruptible()      { testWriteLockInterruptibly_Interruptible(false); }
    @Test
    public void testWriteLockInterruptibly_Interruptible_fair() { testWriteLockInterruptibly_Interruptible(true); }
    private void testWriteLockInterruptibly_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * timed write-tryLock is interruptible
     */
    @Test
    public void testWriteTryLock_Interruptible()      { testWriteTryLock_Interruptible(false); }
    @Test
    public void testWriteTryLock_Interruptible_fair() { testWriteTryLock_Interruptible(true); }
    private void testWriteTryLock_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().tryLock(2 * LONG_DELAY_MS, MILLISECONDS);
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read-lockInterruptibly is interruptible
     */
    @Test
    public void testReadLockInterruptibly_Interruptible()      { testReadLockInterruptibly_Interruptible(false); }
    @Test
    public void testReadLockInterruptibly_Interruptible_fair() { testReadLockInterruptibly_Interruptible(true); }
    private void testReadLockInterruptibly_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.readLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * timed read-tryLock is interruptible
     */
    @Test
    public void testReadTryLock_Interruptible()      { testReadTryLock_Interruptible(false); }
    @Test
    public void testReadTryLock_Interruptible_fair() { testReadTryLock_Interruptible(true); }
    private void testReadTryLock_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.readLock().tryLock(2 * LONG_DELAY_MS, MILLISECONDS);
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * write-tryLock on an unlocked lock succeeds
     */
    @Test
    public void testWriteTryLock()      { testWriteTryLock(false); }
    @Test
    public void testWriteTryLock_fair() { testWriteTryLock(true); }
    private void testWriteTryLock(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        assertTrue(lock.writeLock().tryLock());
        assertWriteLockedByMoi(lock);
        assertTrue(lock.writeLock().tryLock());
        assertWriteLockedByMoi(lock);
        lock.writeLock().unlock();
        releaseWriteLock(lock);
    }

    /**
     * write-tryLock fails if locked
     */
    @Test
    public void testWriteTryLockWhenLocked()      { testWriteTryLockWhenLocked(false); }
    @Test
    public void testWriteTryLockWhenLocked_fair() { testWriteTryLockWhenLocked(true); }
    private void testWriteTryLockWhenLocked(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.writeLock().tryLock());
            }});

        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read-tryLock fails if locked
     */
    @Test
    public void testReadTryLockWhenLocked()      { testReadTryLockWhenLocked(false); }
    @Test
    public void testReadTryLockWhenLocked_fair() { testReadTryLockWhenLocked(true); }
    private void testReadTryLockWhenLocked(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.readLock().tryLock());
            }});

        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * Multiple threads can hold a read lock when not write-locked
     */
    @Test
    public void testMultipleReadLocks()      { testMultipleReadLocks(false); }
    @Test
    public void testMultipleReadLocks_fair() { testMultipleReadLocks(true); }
    private void testMultipleReadLocks(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(lock.readLock().tryLock());
                lock.readLock().unlock();
                assertTrue(lock.readLock().tryLock(LONG_DELAY_MS, MILLISECONDS));
                lock.readLock().unlock();
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        awaitTermination(t);
        lock.readLock().unlock();
    }

    /**
     * A writelock succeeds only after a reading thread unlocks
     */
    @Test
    public void testWriteAfterReadLock()      { testWriteAfterReadLock(false); }
    @Test
    public void testWriteAfterReadLock_fair() { testWriteAfterReadLock(true); }
    private void testWriteAfterReadLock(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(1, lock.getReadLockCount());
                lock.writeLock().lock();
                assertEquals(0, lock.getReadLockCount());
                lock.writeLock().unlock();
            }});
        waitForQueuedThread(lock, t);
        assertNotWriteLocked(lock);
        assertEquals(1, lock.getReadLockCount());
        lock.readLock().unlock();
        assertEquals(0, lock.getReadLockCount());
        awaitTermination(t);
        assertNotWriteLocked(lock);
    }

    /**
     * A writelock succeeds only after reading threads unlock
     */
    @Test
    public void testWriteAfterMultipleReadLocks()      { testWriteAfterMultipleReadLocks(false); }
    @Test
    public void testWriteAfterMultipleReadLocks_fair() { testWriteAfterMultipleReadLocks(true); }
    private void testWriteAfterMultipleReadLocks(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.readLock().lock();
        lock.readLock().lock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                assertEquals(3, lock.getReadLockCount());
                lock.readLock().unlock();
            }});
        awaitTermination(t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(2, lock.getReadLockCount());
                lock.writeLock().lock();
                assertEquals(0, lock.getReadLockCount());
                lock.writeLock().unlock();
            }});
        waitForQueuedThread(lock, t2);
        assertNotWriteLocked(lock);
        assertEquals(2, lock.getReadLockCount());
        lock.readLock().unlock();
        lock.readLock().unlock();
        assertEquals(0, lock.getReadLockCount());
        awaitTermination(t2);
        assertNotWriteLocked(lock);
    }

    /**
     * A thread that tries to acquire a fair read lock (non-reentrantly)
     * will block if there is a waiting writer thread
     */
    @Test
    public void testReaderWriterReaderFairFifo() {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(true);
        final AtomicBoolean t1GotLock = new AtomicBoolean(false);

        lock.readLock().lock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(1, lock.getReadLockCount());
                lock.writeLock().lock();
                assertEquals(0, lock.getReadLockCount());
                t1GotLock.set(true);
                lock.writeLock().unlock();
            }});
        waitForQueuedThread(lock, t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(1, lock.getReadLockCount());
                lock.readLock().lock();
                assertEquals(1, lock.getReadLockCount());
                assertTrue(t1GotLock.get());
                lock.readLock().unlock();
            }});
        waitForQueuedThread(lock, t2);
        assertTrue(t1.isAlive());
        assertNotWriteLocked(lock);
        assertEquals(1, lock.getReadLockCount());
        lock.readLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
        assertNotWriteLocked(lock);
    }

    /**
     * Readlocks succeed only after a writing thread unlocks
     */
    @Test
    public void testReadAfterWriteLock()      { testReadAfterWriteLock(false); }
    @Test
    public void testReadAfterWriteLock_fair() { testReadAfterWriteLock(true); }
    private void testReadAfterWriteLock(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        releaseWriteLock(lock);
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read trylock succeeds if write locked by current thread
     */
    @Test
    public void testReadHoldingWriteLock()      { testReadHoldingWriteLock(false); }
    @Test
    public void testReadHoldingWriteLock_fair() { testReadHoldingWriteLock(true); }
    private void testReadHoldingWriteLock(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        assertTrue(lock.readLock().tryLock());
        lock.readLock().unlock();
        lock.writeLock().unlock();
    }

    /**
     * Read trylock succeeds (barging) even in the presence of waiting
     * readers and/or writers
     */
    @Test
    public void testReadTryLockBarging()      { testReadTryLockBarging(false); }
    @Test
    public void testReadTryLockBarging_fair() { testReadTryLockBarging(true); }
    private void testReadTryLockBarging(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.readLock().lock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});

        waitForQueuedThread(lock, t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        if (fair)
            waitForQueuedThread(lock, t2);

        Thread t3 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().tryLock();
                lock.readLock().unlock();
            }});

        assertTrue(lock.getReadLockCount() > 0);
        awaitTermination(t3);
        assertTrue(t1.isAlive());
        if (fair) assertTrue(t2.isAlive());
        lock.readLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read lock succeeds if write locked by current thread even if
     * other threads are waiting for readlock
     */
    @Test
    public void testReadHoldingWriteLock2()      { testReadHoldingWriteLock2(false); }
    @Test
    public void testReadHoldingWriteLock2_fair() { testReadHoldingWriteLock2(true); }
    private void testReadHoldingWriteLock2(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.readLock().lock();
        lock.readLock().unlock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        assertWriteLockedByMoi(lock);
        lock.readLock().lock();
        lock.readLock().unlock();
        releaseWriteLock(lock);
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read lock succeeds if write locked by current thread even if
     * other threads are waiting for writelock
     */
    @Test
    public void testReadHoldingWriteLock3()      { testReadHoldingWriteLock3(false); }
    @Test
    public void testReadHoldingWriteLock3_fair() { testReadHoldingWriteLock3(true); }
    private void testReadHoldingWriteLock3(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.readLock().lock();
        lock.readLock().unlock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        assertWriteLockedByMoi(lock);
        lock.readLock().lock();
        lock.readLock().unlock();
        assertWriteLockedByMoi(lock);
        lock.writeLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Write lock succeeds if write locked by current thread even if
     * other threads are waiting for writelock
     */
    @Test
    public void testWriteHoldingWriteLock4()      { testWriteHoldingWriteLock4(false); }
    @Test
    public void testWriteHoldingWriteLock4_fair() { testWriteHoldingWriteLock4(true); }
    private void testWriteHoldingWriteLock4(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.writeLock().lock();
        lock.writeLock().unlock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        assertWriteLockedByMoi(lock);
        assertEquals(1, lock.getWriteHoldCount());
        lock.writeLock().lock();
        assertWriteLockedByMoi(lock);
        assertEquals(2, lock.getWriteHoldCount());
        lock.writeLock().unlock();
        assertWriteLockedByMoi(lock);
        assertEquals(1, lock.getWriteHoldCount());
        lock.writeLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read tryLock succeeds if readlocked but not writelocked
     */
    @Test
    public void testTryLockWhenReadLocked()      { testTryLockWhenReadLocked(false); }
    @Test
    public void testTryLockWhenReadLocked_fair() { testTryLockWhenReadLocked(true); }
    private void testTryLockWhenReadLocked(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertTrue(lock.readLock().tryLock());
                lock.readLock().unlock();
            }});

        awaitTermination(t);
        lock.readLock().unlock();
    }

    /**
     * write tryLock fails when readlocked
     */
    @Test
    public void testWriteTryLockWhenReadLocked()      { testWriteTryLockWhenReadLocked(false); }
    @Test
    public void testWriteTryLockWhenReadLocked_fair() { testWriteTryLockWhenReadLocked(true); }
    private void testWriteTryLockWhenReadLocked(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.writeLock().tryLock());
            }});

        awaitTermination(t);
        lock.readLock().unlock();
    }

    /**
     * write timed tryLock times out if locked
     */
    @Test
    public void testWriteTryLock_Timeout()      { testWriteTryLock_Timeout(false); }
    @Test
    public void testWriteTryLock_Timeout_fair() { testWriteTryLock_Timeout(true); }
    private void testWriteTryLock_Timeout(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final long timeoutMillis = timeoutMillis();
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertFalse(lock.writeLock().tryLock(timeoutMillis, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
            }});

        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read timed tryLock times out if write-locked
     */
    @Test
    public void testReadTryLock_Timeout()      { testReadTryLock_Timeout(false); }
    @Test
    public void testReadTryLock_Timeout_fair() { testReadTryLock_Timeout(true); }
    private void testReadTryLock_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                long timeoutMillis = timeoutMillis();
                assertFalse(lock.readLock().tryLock(timeoutMillis, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
            }});

        awaitTermination(t);
        assertTrue(lock.writeLock().isHeldByCurrentThread());
        lock.writeLock().unlock();
    }

    /**
     * write lockInterruptibly succeeds if unlocked, else is interruptible
     */
    @Test
    public void testWriteLockInterruptibly()      { testWriteLockInterruptibly(false); }
    @Test
    public void testWriteLockInterruptibly_fair() { testWriteLockInterruptibly(true); }
    private void testWriteLockInterruptibly(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        try {
            lock.writeLock().lockInterruptibly();
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        assertTrue(lock.writeLock().isHeldByCurrentThread());
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read lockInterruptibly succeeds if lock free else is interruptible
     */
    @Test
    public void testReadLockInterruptibly()      { testReadLockInterruptibly(false); }
    @Test
    public void testReadLockInterruptibly_fair() { testReadLockInterruptibly(true); }
    private void testReadLockInterruptibly(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        try {
            lock.readLock().lockInterruptibly();
            lock.readLock().unlock();
            lock.writeLock().lockInterruptibly();
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.readLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * Calling await without holding lock throws IllegalMonitorStateException
     */
    @Test
    public void testAwait_IMSE()      { testAwait_IMSE(false); }
    @Test
    public void testAwait_IMSE_fair() { testAwait_IMSE(true); }
    private void testAwait_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        for (AwaitMethod awaitMethod : AwaitMethod.values()) {
            long startTime = System.nanoTime();
            try {
                await(c, awaitMethod);
                shouldThrow();
            } catch (IllegalMonitorStateException success) {
            } catch (InterruptedException fail) {
                threadUnexpectedException(fail);
            }
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            c.signal();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * Calling signalAll without holding lock throws IllegalMonitorStateException
     */
    @Test
    public void testSignalAll_IMSE()      { testSignalAll_IMSE(false); }
    @Test
    public void testSignalAll_IMSE_fair() { testSignalAll_IMSE(true); }
    private void testSignalAll_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            c.signalAll();
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final long timeoutMillis = timeoutMillis();
        lock.writeLock().lock();
        final long startTime = System.nanoTime();
        final long timeoutNanos = MILLISECONDS.toNanos(timeoutMillis);
        try {
            long nanosRemaining = c.awaitNanos(timeoutNanos);
            assertTrue(nanosRemaining <= 0);
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
        lock.writeLock().unlock();
    }

    /**
     * timed await without a signal times out
     */
    @Test
    public void testAwait_Timeout()      { testAwait_Timeout(false); }
    @Test
    public void testAwait_Timeout_fair() { testAwait_Timeout(true); }
    private void testAwait_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final long timeoutMillis = timeoutMillis();
        lock.writeLock().lock();
        final long startTime = System.nanoTime();
        try {
            assertFalse(c.await(timeoutMillis, MILLISECONDS));
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
        lock.writeLock().unlock();
    }

    /**
     * awaitUntil without a signal times out
     */
    @Test
    public void testAwaitUntil_Timeout()      { testAwaitUntil_Timeout(false); }
    @Test
    public void testAwaitUntil_Timeout_fair() { testAwaitUntil_Timeout(true); }
    private void testAwaitUntil_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        lock.writeLock().lock();
        // We shouldn't assume that nanoTime and currentTimeMillis
        // use the same time source, so don't use nanoTime here.
        final java.util.Date delayedDate = delayedDate(timeoutMillis());
        try {
            assertFalse(c.awaitUntil(delayedDate));
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(new java.util.Date().getTime() >= delayedDate.getTime());
        lock.writeLock().unlock();
    }

    /**
     * await returns when signalled
     */
    @Test
    public void testAwait()      { testAwait(false); }
    @Test
    public void testAwait_fair() { testAwait(true); }
    private void testAwait(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                locked.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t);
        c.signal();
        assertHasNoWaiters(lock, c);
        assertTrue(t.isAlive());
        lock.writeLock().unlock();
        awaitTermination(t);
    }

    /**
     * awaitUninterruptibly is uninterruptible
     */
    @Test
    public void testAwaitUninterruptibly()      { testAwaitUninterruptibly(false); }
    @Test
    public void testAwaitUninterruptibly_fair() { testAwaitUninterruptibly(true); }
    private void testAwaitUninterruptibly(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch pleaseInterrupt = new CountDownLatch(2);

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt before awaitUninterruptibly
                lock.writeLock().lock();
                pleaseInterrupt.countDown();
                Thread.currentThread().interrupt();
                c.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                lock.writeLock().unlock();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt during awaitUninterruptibly
                lock.writeLock().lock();
                pleaseInterrupt.countDown();
                c.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                lock.writeLock().unlock();
            }});

        await(pleaseInterrupt);
        lock.writeLock().lock();
        lock.writeLock().unlock();
        t2.interrupt();

        assertThreadStaysAlive(t1);
        assertTrue(t2.isAlive());

        lock.writeLock().lock();
        c.signalAll();
        lock.writeLock().unlock();

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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertWriteLockedByMoi(lock);
                assertHasNoWaiters(lock, c);
                locked.countDown();
                try {
                    await(c, awaitMethod);
                } finally {
                    assertWriteLockedByMoi(lock);
                    assertHasNoWaiters(lock, c);
                    lock.writeLock().unlock();
                    assertFalse(Thread.interrupted());
                }
            }});

        await(locked);
        assertHasWaiters(lock, c, t);
        t.interrupt();
        awaitTermination(t);
        assertNotWriteLocked(lock);
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(2);
        final Lock writeLock = lock.writeLock();
        class Awaiter extends CheckedRunnable {
            public void realRun() throws InterruptedException {
                writeLock.lock();
                locked.countDown();
                await(c, awaitMethod);
                writeLock.unlock();
            }
        }

        Thread t1 = newStartedThread(new Awaiter());
        Thread t2 = newStartedThread(new Awaiter());

        await(locked);
        writeLock.lock();
        assertHasWaiters(lock, c, t1, t2);
        c.signalAll();
        assertHasNoWaiters(lock, c);
        writeLock.unlock();
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        final Lock writeLock = lock.writeLock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                writeLock.lock();
                locked1.countDown();
                c.await();
                writeLock.unlock();
            }});

        await(locked1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                writeLock.lock();
                locked2.countDown();
                c.await();
                writeLock.unlock();
            }});

        await(locked2);

        writeLock.lock();
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
        writeLock.unlock();
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(2);
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertWriteLockedByMoi(lock);
                assertEquals(1, lock.writeLock().getHoldCount());
                locked.countDown();
                c.await();
                assertWriteLockedByMoi(lock);
                assertEquals(1, lock.writeLock().getHoldCount());
                lock.writeLock().unlock();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                lock.writeLock().lock();
                assertWriteLockedByMoi(lock);
                assertEquals(2, lock.writeLock().getHoldCount());
                locked.countDown();
                c.await();
                assertWriteLockedByMoi(lock);
                assertEquals(2, lock.writeLock().getHoldCount());
                lock.writeLock().unlock();
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t1, t2);
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.writeLock().unlock();
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.readLock().lock();

        ReentrantReadWriteLock clone = serialClone(lock);
        assertEquals(lock.isFair(), clone.isFair());
        assertTrue(lock.isWriteLocked());
        assertFalse(clone.isWriteLocked());
        assertEquals(1, lock.getReadLockCount());
        assertEquals(0, clone.getReadLockCount());
        clone.writeLock().lock();
        clone.readLock().lock();
        assertTrue(clone.isWriteLocked());
        assertEquals(1, clone.getReadLockCount());
        clone.readLock().unlock();
        clone.writeLock().unlock();
        assertFalse(clone.isWriteLocked());
        assertEquals(1, lock.getReadLockCount());
        assertEquals(0, clone.getReadLockCount());
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    @Test
    public void testHasQueuedThreads()      { testHasQueuedThreads(false); }
    @Test
    public void testHasQueuedThreads_fair() { testHasQueuedThreads(true); }
    private void testHasQueuedThreads(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertFalse(lock.hasQueuedThreads());
        lock.writeLock().lock();
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
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThreads());
    }

    /**
     * hasQueuedThread(null) throws NPE
     */
    @Test
    public void testHasQueuedThreadNPE()      { testHasQueuedThreadNPE(false); }
    @Test
    public void testHasQueuedThreadNPE_fair() { testHasQueuedThreadNPE(true); }
    private void testHasQueuedThreadNPE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertFalse(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        lock.writeLock().lock();
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
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
    }

    /**
     * getQueueLength reports number of waiting threads
     */
    @Test
    public void testGetQueueLength()      { testGetQueueLength(false); }
    @Test
    public void testGetQueueLength_fair() { testGetQueueLength(true); }
    private void testGetQueueLength(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertEquals(0, lock.getQueueLength());
        lock.writeLock().lock();
        t1.start();
        waitForQueuedThread(lock, t1);
        assertEquals(1, lock.getQueueLength());
        t2.start();
        waitForQueuedThread(lock, t2);
        assertEquals(2, lock.getQueueLength());
        t1.interrupt();
        awaitTermination(t1);
        assertEquals(1, lock.getQueueLength());
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertEquals(0, lock.getQueueLength());
    }

    /**
     * getQueuedThreads includes waiting threads
     */
    @Test
    public void testGetQueuedThreads()      { testGetQueuedThreads(false); }
    @Test
    public void testGetQueuedThreads_fair() { testGetQueuedThreads(true); }
    private void testGetQueuedThreads(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertTrue(lock.getQueuedThreads().isEmpty());
        lock.writeLock().lock();
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
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertTrue(lock.getQueuedThreads().isEmpty());
    }

    /**
     * hasWaiters throws NPE if null
     */
    @Test
    public void testHasWaitersNPE()      { testHasWaitersNPE(false); }
    @Test
    public void testHasWaitersNPE_fair() { testHasWaitersNPE(true); }
    private void testHasWaitersNPE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
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
        final PublicReentrantReadWriteLock lock = new PublicReentrantReadWriteLock(fair);
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock(fair);
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock(fair);
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
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final PublicReentrantReadWriteLock lock2 =
            new PublicReentrantReadWriteLock(fair);
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertHasNoWaiters(lock, c);
                assertFalse(lock.hasWaiters(c));
                locked.countDown();
                c.await();
                assertHasNoWaiters(lock, c);
                assertFalse(lock.hasWaiters(c));
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t);
        assertTrue(lock.hasWaiters(c));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertFalse(lock.hasWaiters(c));
        lock.writeLock().unlock();
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
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertEquals(0, lock.getWaitQueueLength(c));
                locked.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t);
        assertEquals(1, lock.getWaitQueueLength(c));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertEquals(0, lock.getWaitQueueLength(c));
        lock.writeLock().unlock();
        awaitTermination(t);
    }

    /**
     * getWaitingThreads returns only and all waiting threads
     */
    @Test
    public void testGetWaitingThreads()      { testGetWaitingThreads(false); }
    @Test
    public void testGetWaitingThreads_fair() { testGetWaitingThreads(true); }
    private void testGetWaitingThreads(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertTrue(lock.getWaitingThreads(c).isEmpty());
                locked1.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertFalse(lock.getWaitingThreads(c).isEmpty());
                locked2.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        lock.writeLock().lock();
        assertTrue(lock.getWaitingThreads(c).isEmpty());
        lock.writeLock().unlock();

        t1.start();
        await(locked1);
        t2.start();
        await(locked2);

        lock.writeLock().lock();
        assertTrue(lock.hasWaiters(c));
        assertTrue(lock.getWaitingThreads(c).contains(t1));
        assertTrue(lock.getWaitingThreads(c).contains(t2));
        assertEquals(2, lock.getWaitingThreads(c).size());
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.writeLock().unlock();

        awaitTermination(t1);
        awaitTermination(t2);

        assertHasNoWaiters(lock, c);
    }

    /**
     * toString indicates current lock state
     */
    @Test
    public void testToString()      { testToString(false); }
    @Test
    public void testToString_fair() { testToString(true); }
    private void testToString(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        assertTrue(lock.toString().contains("Write locks = 0"));
        assertTrue(lock.toString().contains("Read locks = 0"));
        lock.writeLock().lock();
        assertTrue(lock.toString().contains("Write locks = 1"));
        assertTrue(lock.toString().contains("Read locks = 0"));
        lock.writeLock().lock();
        assertTrue(lock.toString().contains("Write locks = 2"));
        assertTrue(lock.toString().contains("Read locks = 0"));
        lock.writeLock().unlock();
        lock.writeLock().unlock();
        lock.readLock().lock();
        assertTrue(lock.toString().contains("Write locks = 0"));
        assertTrue(lock.toString().contains("Read locks = 1"));
        lock.readLock().lock();
        assertTrue(lock.toString().contains("Write locks = 0"));
        assertTrue(lock.toString().contains("Read locks = 2"));
    }

    /**
     * readLock.toString indicates current lock state
     */
    @Test
    public void testReadLockToString()      { testReadLockToString(false); }
    @Test
    public void testReadLockToString_fair() { testReadLockToString(true); }
    private void testReadLockToString(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        assertTrue(lock.readLock().toString().contains("Read locks = 0"));
        lock.readLock().lock();
        assertTrue(lock.readLock().toString().contains("Read locks = 1"));
        lock.readLock().lock();
        assertTrue(lock.readLock().toString().contains("Read locks = 2"));
        lock.readLock().unlock();
        assertTrue(lock.readLock().toString().contains("Read locks = 1"));
        lock.readLock().unlock();
        assertTrue(lock.readLock().toString().contains("Read locks = 0"));
    }

    /**
     * writeLock.toString indicates current lock state
     */
    @Test
    public void testWriteLockToString()      { testWriteLockToString(false); }
    @Test
    public void testWriteLockToString_fair() { testWriteLockToString(true); }
    private void testWriteLockToString(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        assertTrue(lock.writeLock().toString().contains("Unlocked"));
        lock.writeLock().lock();
        assertTrue(lock.writeLock().toString().contains("Locked by"));
        lock.writeLock().unlock();
        assertTrue(lock.writeLock().toString().contains("Unlocked"));
    }

}
