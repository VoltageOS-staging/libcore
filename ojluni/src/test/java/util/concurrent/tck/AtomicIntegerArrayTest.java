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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class AtomicIntegerArrayTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.AtomicIntegerArrayTest");
    }
    // public static Test suite() {
    //     return new TestSuite(AtomicIntegerArrayTest.class);
    // }

    /**
     * constructor creates array of given size with all elements zero
     */
    @Test
    public void testConstructor() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++)
            assertEquals(0, aa.get(i));
    }

    /**
     * constructor with null array throws NPE
     */
    @Test
    public void testConstructor2NPE() {
        try {
            int[] a = null;
            new AtomicIntegerArray(a);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * constructor with array is of same size and has all elements
     */
    @Test
    public void testConstructor2() {
        int[] a = { 17, 3, -42, 99, -7 };
        AtomicIntegerArray aa = new AtomicIntegerArray(a);
        assertEquals(a.length, aa.length());
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], aa.get(i));
    }

    /**
     * get and set for out of bound indices throw IndexOutOfBoundsException
     */
    @Test
    public void testIndexing() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int index : new int[] { -1, SIZE }) {
            try {
                aa.get(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.set(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.lazySet(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndSet(index, 1, 2);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSet(index, 1, 2);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.getAndAdd(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.addAndGet(index, 1);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
        }
    }

    /**
     * get returns the last value set at index
     */
    @Test
    public void testGetSet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.get(i));
            aa.set(i, 2);
            assertEquals(2, aa.get(i));
            aa.set(i, -3);
            assertEquals(-3, aa.get(i));
        }
    }

    /**
     * get returns the last value lazySet at index by same thread
     */
    @Test
    public void testGetLazySet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.lazySet(i, 1);
            assertEquals(1, aa.get(i));
            aa.lazySet(i, 2);
            assertEquals(2, aa.get(i));
            aa.lazySet(i, -3);
            assertEquals(-3, aa.get(i));
        }
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    @Test
    public void testCompareAndSet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertTrue(aa.compareAndSet(i, 1, 2));
            assertTrue(aa.compareAndSet(i, 2, -4));
            assertEquals(-4, aa.get(i));
            assertFalse(aa.compareAndSet(i, -5, 7));
            assertEquals(-4, aa.get(i));
            assertTrue(aa.compareAndSet(i, -4, 7));
            assertEquals(7, aa.get(i));
        }
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    @Test
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!a.compareAndSet(0, 2, 3))
                    Thread.yield();
            }});

        t.start();
        assertTrue(a.compareAndSet(0, 1, 2));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertEquals(3, a.get(0));
    }

    /**
     * repeated weakCompareAndSet succeeds in changing value when equal
     * to expected
     */
    @Test
    public void testWeakCompareAndSet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            do {} while (!aa.weakCompareAndSet(i, 1, 2));
            do {} while (!aa.weakCompareAndSet(i, 2, -4));
            assertEquals(-4, aa.get(i));
            do {} while (!aa.weakCompareAndSet(i, -4, 7));
            assertEquals(7, aa.get(i));
        }
    }

    /**
     * getAndSet returns previous value and sets to given value at given index
     */
    @Test
    public void testGetAndSet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndSet(i, 0));
            assertEquals(0, aa.getAndSet(i, -10));
            assertEquals(-10, aa.getAndSet(i, 1));
        }
    }

    /**
     * getAndAdd returns previous value and adds given value
     */
    @Test
    public void testGetAndAdd() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndAdd(i, 2));
            assertEquals(3, aa.get(i));
            assertEquals(3, aa.getAndAdd(i, -4));
            assertEquals(-1, aa.get(i));
        }
    }

    /**
     * getAndDecrement returns previous value and decrements
     */
    @Test
    public void testGetAndDecrement() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndDecrement(i));
            assertEquals(0, aa.getAndDecrement(i));
            assertEquals(-1, aa.getAndDecrement(i));
        }
    }

    /**
     * getAndIncrement returns previous value and increments
     */
    @Test
    public void testGetAndIncrement() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(1, aa.getAndIncrement(i));
            assertEquals(2, aa.get(i));
            aa.set(i, -2);
            assertEquals(-2, aa.getAndIncrement(i));
            assertEquals(-1, aa.getAndIncrement(i));
            assertEquals(0, aa.getAndIncrement(i));
            assertEquals(1, aa.get(i));
        }
    }

    /**
     * addAndGet adds given value to current, and returns current value
     */
    @Test
    public void testAddAndGet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(3, aa.addAndGet(i, 2));
            assertEquals(3, aa.get(i));
            assertEquals(-1, aa.addAndGet(i, -4));
            assertEquals(-1, aa.get(i));
        }
    }

    /**
     * decrementAndGet decrements and returns current value
     */
    @Test
    public void testDecrementAndGet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(0, aa.decrementAndGet(i));
            assertEquals(-1, aa.decrementAndGet(i));
            assertEquals(-2, aa.decrementAndGet(i));
            assertEquals(-2, aa.get(i));
        }
    }

    /**
     * incrementAndGet increments and returns current value
     */
    @Test
    public void testIncrementAndGet() {
        AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, 1);
            assertEquals(2, aa.incrementAndGet(i));
            assertEquals(2, aa.get(i));
            aa.set(i, -2);
            assertEquals(-1, aa.incrementAndGet(i));
            assertEquals(0, aa.incrementAndGet(i));
            assertEquals(1, aa.incrementAndGet(i));
            assertEquals(1, aa.get(i));
        }
    }

    class Counter extends CheckedRunnable {
        final AtomicIntegerArray aa;
        int decs;
        Counter(AtomicIntegerArray a) { aa = a; }
        public void realRun() {
            for (;;) {
                boolean done = true;
                for (int i = 0; i < aa.length(); i++) {
                    int v = aa.get(i);
                    assertTrue(v >= 0);
                    if (v != 0) {
                        done = false;
                        if (aa.compareAndSet(i, v, v - 1))
                            decs++;
                    }
                }
                if (done)
                    break;
            }
        }
    }

    /**
     * Multiple threads using same array of counters successfully
     * update a number of times equal to total count
     */
    @Test
    public void testCountingInMultipleThreads() throws InterruptedException {
        final AtomicIntegerArray aa = new AtomicIntegerArray(SIZE);
        int countdown = 10000;
        for (int i = 0; i < SIZE; i++)
            aa.set(i, countdown);
        Counter c1 = new Counter(aa);
        Counter c2 = new Counter(aa);
        Thread t1 = newStartedThread(c1);
        Thread t2 = newStartedThread(c2);
        t1.join();
        t2.join();
        assertEquals(c1.decs + c2.decs, SIZE * countdown);
    }

    /**
     * a deserialized serialized array holds same values
     */
    @Test
    public void testSerialization() throws Exception {
        AtomicIntegerArray x = new AtomicIntegerArray(SIZE);
        for (int i = 0; i < SIZE; i++)
            x.set(i, -i);
        AtomicIntegerArray y = serialClone(x);
        assertNotSame(x, y);
        assertEquals(x.length(), y.length());
        for (int i = 0; i < SIZE; i++) {
            assertEquals(x.get(i), y.get(i));
        }
    }

    /**
     * toString returns current value.
     */
    @Test
    public void testToString() {
        int[] a = { 17, 3, -42, 99, -7 };
        AtomicIntegerArray aa = new AtomicIntegerArray(a);
        assertEquals(Arrays.toString(a), aa.toString());
    }

}
