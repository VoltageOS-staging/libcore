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

import java.util.Vector;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class VectorTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.VectorTest");
    }

    // public static Test suite() {
    //     class Implementation implements CollectionImplementation {
    //         public Class<?> klazz() { return Vector.class; }
    //         public List emptyCollection() { return new Vector(); }
    //         public Object makeElement(int i) { return i; }
    //         public boolean isConcurrent() { return false; }
    //         public boolean permitsNulls() { return true; }
    //     }
    //     class SubListImplementation extends Implementation {
    //         public List emptyCollection() {
    //             return super.emptyCollection().subList(0, 0);
    //         }
    //     }
    //     return newTestSuite(
    //             VectorTest.class,
    //             CollectionTest.testSuite(new Implementation()),
    //             CollectionTest.testSuite(new SubListImplementation()));
    // }

    /**
     * tests for setSize()
     */
    @Test
    public void testSetSize() {
        final Vector v = new Vector();
        for (int n : new int[] { 100, 5, 50 }) {
            v.setSize(n);
            assertEquals(n, v.size());
            assertNull(v.get(0));
            assertNull(v.get(n - 1));
            assertThrows(
                    ArrayIndexOutOfBoundsException.class,
                    new Runnable() { public void run() { v.setSize(-1); }});
            assertEquals(n, v.size());
            assertNull(v.get(0));
            assertNull(v.get(n - 1));
        }
    }

}
