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
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of atomic class methods accepting lambdas introduced in JDK8.
 */
// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class Atomic8Test extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.Atomic8Test");
    }
    // public static Test suite() {
    //     return new TestSuite(Atomic8Test.class);
    // }

    static long addLong17(long x) { return x + 17; }
    static int addInt17(int x) { return x + 17; }
    static Integer addInteger17(Integer x) {
        return x.intValue() + 17;
    }
    static Integer sumInteger(Integer x, Integer y) {
        return x.intValue() + y.intValue();
    }

    volatile long aLongField;
    volatile int anIntField;
    volatile Integer anIntegerField;

    AtomicLongFieldUpdater<Atomic8Test> aLongFieldUpdater() {
        return AtomicLongFieldUpdater.newUpdater
            (Atomic8Test.class, "aLongField");
    }

    AtomicIntegerFieldUpdater<Atomic8Test> anIntFieldUpdater() {
        return AtomicIntegerFieldUpdater.newUpdater
            (Atomic8Test.class, "anIntField");
    }

    AtomicReferenceFieldUpdater<Atomic8Test,Integer> anIntegerFieldUpdater() {
        return AtomicReferenceFieldUpdater.newUpdater
            (Atomic8Test.class, Integer.class, "anIntegerField");
    }

    /**
     * AtomicLong getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testLongGetAndUpdate() {
        AtomicLong a = new AtomicLong(1L);
        assertEquals(1L, a.getAndUpdate(Atomic8Test::addLong17));
        assertEquals(18L, a.getAndUpdate(Atomic8Test::addLong17));
        assertEquals(35L, a.get());
    }

    /**
     * AtomicLong updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testLongUpdateAndGet() {
        AtomicLong a = new AtomicLong(1L);
        assertEquals(18L, a.updateAndGet(Atomic8Test::addLong17));
        assertEquals(35L, a.updateAndGet(Atomic8Test::addLong17));
    }

    /**
     * AtomicLong getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testLongGetAndAccumulate() {
        AtomicLong a = new AtomicLong(1L);
        assertEquals(1L, a.getAndAccumulate(2L, Long::sum));
        assertEquals(3L, a.getAndAccumulate(3L, Long::sum));
        assertEquals(6L, a.get());
    }

    /**
     * AtomicLong accumulateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testLongAccumulateAndGet() {
        AtomicLong a = new AtomicLong(1L);
        assertEquals(7L, a.accumulateAndGet(6L, Long::sum));
        assertEquals(10L, a.accumulateAndGet(3L, Long::sum));
        assertEquals(10L, a.get());
    }

    /**
     * AtomicInteger getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testIntGetAndUpdate() {
        AtomicInteger a = new AtomicInteger(1);
        assertEquals(1, a.getAndUpdate(Atomic8Test::addInt17));
        assertEquals(18, a.getAndUpdate(Atomic8Test::addInt17));
        assertEquals(35, a.get());
    }

    /**
     * AtomicInteger updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testIntUpdateAndGet() {
        AtomicInteger a = new AtomicInteger(1);
        assertEquals(18, a.updateAndGet(Atomic8Test::addInt17));
        assertEquals(35, a.updateAndGet(Atomic8Test::addInt17));
        assertEquals(35, a.get());
    }

    /**
     * AtomicInteger getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testIntGetAndAccumulate() {
        AtomicInteger a = new AtomicInteger(1);
        assertEquals(1, a.getAndAccumulate(2, Integer::sum));
        assertEquals(3, a.getAndAccumulate(3, Integer::sum));
        assertEquals(6, a.get());
    }

    /**
     * AtomicInteger accumulateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testIntAccumulateAndGet() {
        AtomicInteger a = new AtomicInteger(1);
        assertEquals(7, a.accumulateAndGet(6, Integer::sum));
        assertEquals(10, a.accumulateAndGet(3, Integer::sum));
        assertEquals(10, a.get());
    }

    /**
     * AtomicReference getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testReferenceGetAndUpdate() {
        AtomicReference<Integer> a = new AtomicReference<>(one);
        assertEquals((Integer) 1, a.getAndUpdate(Atomic8Test::addInteger17));
        assertEquals((Integer) 18, a.getAndUpdate(Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.get());
    }

    /**
     * AtomicReference updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testReferenceUpdateAndGet() {
        AtomicReference<Integer> a = new AtomicReference<>(one);
        assertEquals((Integer) 18, a.updateAndGet(Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.updateAndGet(Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.get());
    }

    /**
     * AtomicReference getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testReferenceGetAndAccumulate() {
        AtomicReference<Integer> a = new AtomicReference<>(one);
        assertEquals((Integer) 1, a.getAndAccumulate(2, Atomic8Test::sumInteger));
        assertEquals((Integer) 3, a.getAndAccumulate(3, Atomic8Test::sumInteger));
        assertEquals((Integer) 6, a.get());
    }

    /**
     * AtomicReference accumulateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testReferenceAccumulateAndGet() {
        AtomicReference<Integer> a = new AtomicReference<>(one);
        assertEquals((Integer) 7, a.accumulateAndGet(6, Atomic8Test::sumInteger));
        assertEquals((Integer) 10, a.accumulateAndGet(3, Atomic8Test::sumInteger));
        assertEquals((Integer) 10, a.get());
    }

    /**
     * AtomicLongArray getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testLongArrayGetAndUpdate() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        assertEquals(1L, a.getAndUpdate(0, Atomic8Test::addLong17));
        assertEquals(18L, a.getAndUpdate(0, Atomic8Test::addLong17));
        assertEquals(35L, a.get(0));
    }

    /**
     * AtomicLongArray updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testLongArrayUpdateAndGet() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        assertEquals(18L, a.updateAndGet(0, Atomic8Test::addLong17));
        assertEquals(35L, a.updateAndGet(0, Atomic8Test::addLong17));
        assertEquals(35L, a.get(0));
    }

    /**
     * AtomicLongArray getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testLongArrayGetAndAccumulate() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        assertEquals(1L, a.getAndAccumulate(0, 2L, Long::sum));
        assertEquals(3L, a.getAndAccumulate(0, 3L, Long::sum));
        assertEquals(6L, a.get(0));
    }

    /**
     * AtomicLongArray accumulateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testLongArrayAccumulateAndGet() {
        AtomicLongArray a = new AtomicLongArray(1);
        a.set(0, 1);
        assertEquals(7L, a.accumulateAndGet(0, 6L, Long::sum));
        assertEquals(10L, a.accumulateAndGet(0, 3L, Long::sum));
        assertEquals(10L, a.get(0));
    }

    /**
     * AtomicIntegerArray getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testIntArrayGetAndUpdate() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        assertEquals(1, a.getAndUpdate(0, Atomic8Test::addInt17));
        assertEquals(18, a.getAndUpdate(0, Atomic8Test::addInt17));
        assertEquals(35, a.get(0));
    }

    /**
     * AtomicIntegerArray updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testIntArrayUpdateAndGet() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        assertEquals(18, a.updateAndGet(0, Atomic8Test::addInt17));
        assertEquals(35, a.updateAndGet(0, Atomic8Test::addInt17));
        assertEquals(35, a.get(0));
    }

    /**
     * AtomicIntegerArray getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testIntArrayGetAndAccumulate() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        assertEquals(1, a.getAndAccumulate(0, 2, Integer::sum));
        assertEquals(3, a.getAndAccumulate(0, 3, Integer::sum));
        assertEquals(6, a.get(0));
    }

    /**
     * AtomicIntegerArray accumulateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testIntArrayAccumulateAndGet() {
        AtomicIntegerArray a = new AtomicIntegerArray(1);
        a.set(0, 1);
        assertEquals(7, a.accumulateAndGet(0, 6, Integer::sum));
        assertEquals(10, a.accumulateAndGet(0, 3, Integer::sum));
    }

    /**
     * AtomicReferenceArray getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testReferenceArrayGetAndUpdate() {
        AtomicReferenceArray<Integer> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        assertEquals((Integer) 1, a.getAndUpdate(0, Atomic8Test::addInteger17));
        assertEquals((Integer) 18, a.getAndUpdate(0, Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.get(0));
    }

    /**
     * AtomicReferenceArray updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testReferenceArrayUpdateAndGet() {
        AtomicReferenceArray<Integer> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        assertEquals((Integer) 18, a.updateAndGet(0, Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.updateAndGet(0, Atomic8Test::addInteger17));
    }

    /**
     * AtomicReferenceArray getAndAccumulate returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testReferenceArrayGetAndAccumulate() {
        AtomicReferenceArray<Integer> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        assertEquals((Integer) 1, a.getAndAccumulate(0, 2, Atomic8Test::sumInteger));
        assertEquals((Integer) 3, a.getAndAccumulate(0, 3, Atomic8Test::sumInteger));
        assertEquals((Integer) 6, a.get(0));
    }

    /**
     * AtomicReferenceArray accumulateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testReferenceArrayAccumulateAndGet() {
        AtomicReferenceArray<Integer> a = new AtomicReferenceArray<>(1);
        a.set(0, one);
        assertEquals((Integer) 7, a.accumulateAndGet(0, 6, Atomic8Test::sumInteger));
        assertEquals((Integer) 10, a.accumulateAndGet(0, 3, Atomic8Test::sumInteger));
    }

    /**
     * AtomicLongFieldUpdater getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testLongFieldUpdaterGetAndUpdate() {
        AtomicLongFieldUpdater a = aLongFieldUpdater();
        a.set(this, 1);
        assertEquals(1L, a.getAndUpdate(this, Atomic8Test::addLong17));
        assertEquals(18L, a.getAndUpdate(this, Atomic8Test::addLong17));
        assertEquals(35L, a.get(this));
        assertEquals(35L, aLongField);
    }

    /**
     * AtomicLongFieldUpdater updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testLongFieldUpdaterUpdateAndGet() {
        AtomicLongFieldUpdater a = aLongFieldUpdater();
        a.set(this, 1);
        assertEquals(18L, a.updateAndGet(this, Atomic8Test::addLong17));
        assertEquals(35L, a.updateAndGet(this, Atomic8Test::addLong17));
        assertEquals(35L, a.get(this));
        assertEquals(35L, aLongField);
    }

    /**
     * AtomicLongFieldUpdater getAndAccumulate returns previous value
     * and updates with supplied function.
     */
    @Test
    public void testLongFieldUpdaterGetAndAccumulate() {
        AtomicLongFieldUpdater a = aLongFieldUpdater();
        a.set(this, 1);
        assertEquals(1L, a.getAndAccumulate(this, 2L, Long::sum));
        assertEquals(3L, a.getAndAccumulate(this, 3L, Long::sum));
        assertEquals(6L, a.get(this));
        assertEquals(6L, aLongField);
    }

    /**
     * AtomicLongFieldUpdater accumulateAndGet updates with supplied
     * function and returns result.
     */
    @Test
    public void testLongFieldUpdaterAccumulateAndGet() {
        AtomicLongFieldUpdater a = aLongFieldUpdater();
        a.set(this, 1);
        assertEquals(7L, a.accumulateAndGet(this, 6L, Long::sum));
        assertEquals(10L, a.accumulateAndGet(this, 3L, Long::sum));
        assertEquals(10L, a.get(this));
        assertEquals(10L, aLongField);
    }

    /**
     * AtomicIntegerFieldUpdater getAndUpdate returns previous value and updates
     * result of supplied function
     */
    @Test
    public void testIntegerFieldUpdaterGetAndUpdate() {
        AtomicIntegerFieldUpdater a = anIntFieldUpdater();
        a.set(this, 1);
        assertEquals(1, a.getAndUpdate(this, Atomic8Test::addInt17));
        assertEquals(18, a.getAndUpdate(this, Atomic8Test::addInt17));
        assertEquals(35, a.get(this));
        assertEquals(35, anIntField);
    }

    /**
     * AtomicIntegerFieldUpdater updateAndGet updates with supplied function and
     * returns result.
     */
    @Test
    public void testIntegerFieldUpdaterUpdateAndGet() {
        AtomicIntegerFieldUpdater a = anIntFieldUpdater();
        a.set(this, 1);
        assertEquals(18, a.updateAndGet(this, Atomic8Test::addInt17));
        assertEquals(35, a.updateAndGet(this, Atomic8Test::addInt17));
        assertEquals(35, a.get(this));
        assertEquals(35, anIntField);
    }

    /**
     * AtomicIntegerFieldUpdater getAndAccumulate returns previous value
     * and updates with supplied function.
     */
    @Test
    public void testIntegerFieldUpdaterGetAndAccumulate() {
        AtomicIntegerFieldUpdater a = anIntFieldUpdater();
        a.set(this, 1);
        assertEquals(1, a.getAndAccumulate(this, 2, Integer::sum));
        assertEquals(3, a.getAndAccumulate(this, 3, Integer::sum));
        assertEquals(6, a.get(this));
        assertEquals(6, anIntField);
    }

    /**
     * AtomicIntegerFieldUpdater accumulateAndGet updates with supplied
     * function and returns result.
     */
    @Test
    public void testIntegerFieldUpdaterAccumulateAndGet() {
        AtomicIntegerFieldUpdater a = anIntFieldUpdater();
        a.set(this, 1);
        assertEquals(7, a.accumulateAndGet(this, 6, Integer::sum));
        assertEquals(10, a.accumulateAndGet(this, 3, Integer::sum));
        assertEquals(10, a.get(this));
        assertEquals(10, anIntField);
    }

    /**
     * AtomicReferenceFieldUpdater getAndUpdate returns previous value
     * and updates result of supplied function
     */
    @Test
    public void testReferenceFieldUpdaterGetAndUpdate() {
        AtomicReferenceFieldUpdater<Atomic8Test,Integer> a = anIntegerFieldUpdater();
        a.set(this, one);
        assertEquals((Integer) 1, a.getAndUpdate(this, Atomic8Test::addInteger17));
        assertEquals((Integer) 18, a.getAndUpdate(this, Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.get(this));
        assertEquals((Integer) 35, anIntegerField);
    }

    /**
     * AtomicReferenceFieldUpdater updateAndGet updates with supplied
     * function and returns result.
     */
    @Test
    public void testReferenceFieldUpdaterUpdateAndGet() {
        AtomicReferenceFieldUpdater<Atomic8Test,Integer> a = anIntegerFieldUpdater();
        a.set(this, one);
        assertEquals((Integer) 18, a.updateAndGet(this, Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.updateAndGet(this, Atomic8Test::addInteger17));
        assertEquals((Integer) 35, a.get(this));
        assertEquals((Integer) 35, anIntegerField);
    }

    /**
     * AtomicReferenceFieldUpdater returns previous value and updates
     * with supplied function.
     */
    @Test
    public void testReferenceFieldUpdaterGetAndAccumulate() {
        AtomicReferenceFieldUpdater<Atomic8Test,Integer> a = anIntegerFieldUpdater();
        a.set(this, one);
        assertEquals((Integer) 1, a.getAndAccumulate(this, 2, Atomic8Test::sumInteger));
        assertEquals((Integer) 3, a.getAndAccumulate(this, 3, Atomic8Test::sumInteger));
        assertEquals((Integer) 6, a.get(this));
        assertEquals((Integer) 6, anIntegerField);
    }

    /**
     * AtomicReferenceFieldUpdater accumulateAndGet updates with
     * supplied function and returns result.
     */
    @Test
    public void testReferenceFieldUpdaterAccumulateAndGet() {
        AtomicReferenceFieldUpdater<Atomic8Test,Integer> a = anIntegerFieldUpdater();
        a.set(this, one);
        assertEquals((Integer) 7, a.accumulateAndGet(this, 6, Atomic8Test::sumInteger));
        assertEquals((Integer) 10, a.accumulateAndGet(this, 3, Atomic8Test::sumInteger));
        assertEquals((Integer) 10, a.get(this));
        assertEquals((Integer) 10, anIntegerField);
    }

    /**
     * All Atomic getAndUpdate methods throw NullPointerException on
     * null function argument
     */
    @Test
    public void testGetAndUpdateNPE() {
        Runnable[] throwingActions = {
            () -> new AtomicLong().getAndUpdate(null),
            () -> new AtomicInteger().getAndUpdate(null),
            () -> new AtomicReference().getAndUpdate(null),
            () -> new AtomicLongArray(1).getAndUpdate(0, null),
            () -> new AtomicIntegerArray(1).getAndUpdate(0, null),
            () -> new AtomicReferenceArray(1).getAndUpdate(0, null),
            () -> aLongFieldUpdater().getAndUpdate(this, null),
            () -> anIntFieldUpdater().getAndUpdate(this, null),
            () -> anIntegerFieldUpdater().getAndUpdate(this, null),
        };
        assertThrows(NullPointerException.class, throwingActions);
    }

    /**
     * All Atomic updateAndGet methods throw NullPointerException on null function argument
     */
    @Test
    public void testUpdateAndGetNPE() {
        Runnable[] throwingActions = {
            () -> new AtomicLong().updateAndGet(null),
            () -> new AtomicInteger().updateAndGet(null),
            () -> new AtomicReference().updateAndGet(null),
            () -> new AtomicLongArray(1).updateAndGet(0, null),
            () -> new AtomicIntegerArray(1).updateAndGet(0, null),
            () -> new AtomicReferenceArray(1).updateAndGet(0, null),
            () -> aLongFieldUpdater().updateAndGet(this, null),
            () -> anIntFieldUpdater().updateAndGet(this, null),
            () -> anIntegerFieldUpdater().updateAndGet(this, null),
        };
        assertThrows(NullPointerException.class, throwingActions);
    }

    /**
     * All Atomic getAndAccumulate methods throw NullPointerException
     * on null function argument
     */
    @Test
    public void testGetAndAccumulateNPE() {
        Runnable[] throwingActions = {
            () -> new AtomicLong().getAndAccumulate(1L, null),
            () -> new AtomicInteger().getAndAccumulate(1, null),
            () -> new AtomicReference().getAndAccumulate(one, null),
            () -> new AtomicLongArray(1).getAndAccumulate(0, 1L, null),
            () -> new AtomicIntegerArray(1).getAndAccumulate(0, 1, null),
            () -> new AtomicReferenceArray(1).getAndAccumulate(0, one, null),
            () -> aLongFieldUpdater().getAndAccumulate(this, 1L, null),
            () -> anIntFieldUpdater().getAndAccumulate(this, 1, null),
            () -> anIntegerFieldUpdater().getAndAccumulate(this, one, null),
        };
        assertThrows(NullPointerException.class, throwingActions);
    }

    /**
     * All Atomic accumulateAndGet methods throw NullPointerException
     * on null function argument
     */
    @Test
    public void testAccumulateAndGetNPE() {
        Runnable[] throwingActions = {
            () -> new AtomicLong().accumulateAndGet(1L, null),
            () -> new AtomicInteger().accumulateAndGet(1, null),
            () -> new AtomicReference().accumulateAndGet(one, null),
            () -> new AtomicLongArray(1).accumulateAndGet(0, 1L, null),
            () -> new AtomicIntegerArray(1).accumulateAndGet(0, 1, null),
            () -> new AtomicReferenceArray(1).accumulateAndGet(0, one, null),
            () -> aLongFieldUpdater().accumulateAndGet(this, 1L, null),
            () -> anIntFieldUpdater().accumulateAndGet(this, 1, null),
            () -> anIntegerFieldUpdater().accumulateAndGet(this, one, null),
        };
        assertThrows(NullPointerException.class, throwingActions);
    }

    /**
     * Object arguments for parameters of type T that are not
     * instances of the class passed to the newUpdater call will
     * result in a ClassCastException being thrown.
     */
    @Test
    public void testFieldUpdaters_ClassCastException() {
        // Use raw types to allow passing wrong object type, provoking CCE
        final AtomicLongFieldUpdater longUpdater = aLongFieldUpdater();
        final AtomicIntegerFieldUpdater intUpdater = anIntFieldUpdater();
        final AtomicReferenceFieldUpdater refUpdater = anIntegerFieldUpdater();
        final Object obj = new Object();
        for (Object x : new Object[]{ new Object(), null }) {
            Runnable[] throwingActions = {
                () -> longUpdater.get(x),
                () -> intUpdater.get(x),
                () -> refUpdater.get(x),

                () -> longUpdater.set(x, 17L),
                () -> intUpdater.set(x, 17),
                () -> refUpdater.set(x, (Integer) 17),

                () -> longUpdater.addAndGet(x, 17L),
                () -> intUpdater.addAndGet(x, 17),

                () -> longUpdater.getAndUpdate(x, y -> y),
                () -> intUpdater.getAndUpdate(x, y -> y),
                () -> refUpdater.getAndUpdate(x, y -> y),

                () -> longUpdater.compareAndSet(x, 17L, 42L),
                () -> intUpdater.compareAndSet(x, 17, 42),
                () -> refUpdater.compareAndSet(x, (Integer) 17, (Integer) 42),
            };
            assertThrows(ClassCastException.class, throwingActions);
        }
    }

}
