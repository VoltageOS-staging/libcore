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

import java.util.concurrent.atomic.AtomicStampedReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class AtomicStampedReferenceTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.AtomicStampedReferenceTest");
    }
    // public static Test suite() {
    //     return new TestSuite(AtomicStampedReferenceTest.class);
    // }

    /**
     * constructor initializes to given reference and stamp
     */
    @Test
    public void testConstructor() {
        AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        assertSame(one, ai.getReference());
        assertEquals(0, ai.getStamp());
        AtomicStampedReference a2 = new AtomicStampedReference(null, 1);
        assertNull(a2.getReference());
        assertEquals(1, a2.getStamp());
    }

    /**
     * get returns the last values of reference and stamp set
     */
    @Test
    public void testGetSet() {
        int[] mark = new int[1];
        AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        assertSame(one, ai.getReference());
        assertEquals(0, ai.getStamp());
        assertSame(one, ai.get(mark));
        assertEquals(0, mark[0]);
        ai.set(two, 0);
        assertSame(two, ai.getReference());
        assertEquals(0, ai.getStamp());
        assertSame(two, ai.get(mark));
        assertEquals(0, mark[0]);
        ai.set(one, 1);
        assertSame(one, ai.getReference());
        assertEquals(1, ai.getStamp());
        assertSame(one, ai.get(mark));
        assertEquals(1, mark[0]);
    }

    /**
     * attemptStamp succeeds in single thread
     */
    @Test
    public void testAttemptStamp() {
        int[] mark = new int[1];
        AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        assertEquals(0, ai.getStamp());
        assertTrue(ai.attemptStamp(one, 1));
        assertEquals(1, ai.getStamp());
        assertSame(one, ai.get(mark));
        assertEquals(1, mark[0]);
    }

    /**
     * compareAndSet succeeds in changing values if equal to expected reference
     * and stamp else fails
     */
    @Test
    public void testCompareAndSet() {
        int[] mark = new int[1];
        AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        assertSame(one, ai.get(mark));
        assertEquals(0, ai.getStamp());
        assertEquals(0, mark[0]);

        assertTrue(ai.compareAndSet(one, two, 0, 0));
        assertSame(two, ai.get(mark));
        assertEquals(0, mark[0]);

        assertTrue(ai.compareAndSet(two, m3, 0, 1));
        assertSame(m3, ai.get(mark));
        assertEquals(1, mark[0]);

        assertFalse(ai.compareAndSet(two, m3, 1, 1));
        assertSame(m3, ai.get(mark));
        assertEquals(1, mark[0]);
    }

    /**
     * compareAndSet in one thread enables another waiting for reference value
     * to succeed
     */
    @Test
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!ai.compareAndSet(two, three, 0, 0))
                    Thread.yield();
            }});

        t.start();
        assertTrue(ai.compareAndSet(one, two, 0, 0));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(three, ai.getReference());
        assertEquals(0, ai.getStamp());
    }

    /**
     * compareAndSet in one thread enables another waiting for stamp value
     * to succeed
     */
    @Test
    public void testCompareAndSetInMultipleThreads2() throws Exception {
        final AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!ai.compareAndSet(one, one, 1, 2))
                    Thread.yield();
            }});

        t.start();
        assertTrue(ai.compareAndSet(one, one, 0, 1));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(one, ai.getReference());
        assertEquals(2, ai.getStamp());
    }

    /**
     * repeated weakCompareAndSet succeeds in changing values when equal
     * to expected
     */
    @Test
    public void testWeakCompareAndSet() {
        int[] mark = new int[1];
        AtomicStampedReference ai = new AtomicStampedReference(one, 0);
        assertSame(one, ai.get(mark));
        assertEquals(0, ai.getStamp());
        assertEquals(0, mark[0]);

        do {} while (!ai.weakCompareAndSet(one, two, 0, 0));
        assertSame(two, ai.get(mark));
        assertEquals(0, mark[0]);

        do {} while (!ai.weakCompareAndSet(two, m3, 0, 1));
        assertSame(m3, ai.get(mark));
        assertEquals(1, mark[0]);
    }

}
