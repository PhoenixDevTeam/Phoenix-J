/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.dealnote.xmpp.util;

/**
 * <p>Provides extra functionality for Java Number classes.</p>
 *
 * @author Apache Software Foundation
 * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
 * @author <a href="mailto:steve.downey@netfolio.com">Steve Downey</a>
 * @author Eric Pugh
 * @author Phil Steitz
 * @author Matthew Hawthorne
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 * @author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck</a>
 * @since 2.0
 * @version $Id$
 */
public class NumberUtils {
    //-----------------------------------------------------------------------
    /**
     * <p>Compares two <code>doubles</code> for order.</p>
     *
     * <p>This method is more comprehensive than the standard Java greater
     * than, less than and equals operators.</p>
     * <ul>
     *  <li>It returns <code>-1</code> if the first value is less than the second.</li>
     *  <li>It returns <code>+1</code> if the first value is greater than the second.</li>
     *  <li>It returns <code>0</code> if the values are equal.</li>
     * </ul>
     *
     * <p>
     * The ordering is as follows, largest to smallest:
     * <ul>
     *  <li>NaN
     *  <li>Positive infinity
     *  <li>Maximum double
     *  <li>Normal positive numbers
     *  <li>+0.0
     *  <li>-0.0
     *  <li>Normal negative numbers
     *  <li>Minimum double (<code>-Double.MAX_VALUE</code>)
     *  <li>Negative infinity
     * </ul>
     * </p>
     *
     * <p>Comparing <code>NaN</code> with <code>NaN</code> will
     * return <code>0</code>.</p>
     *
     * @param lhs  the first <code>double</code>
     * @param rhs  the second <code>double</code>
     * @return <code>-1</code> if lhs is less, <code>+1</code> if greater,
     *  <code>0</code> if equal to rhs
     */
    public static int compare(double lhs, double rhs) {
        if (lhs < rhs) {
            return -1;
        }
        if (lhs > rhs) {
            return +1;
        }
        // Need to compare bits to handle 0.0 == -0.0 being true
        // compare should put -0.0 < +0.0
        // Two NaNs are also == for compare purposes
        // where NaN == NaN is false
        long lhsBits = Double.doubleToLongBits(lhs);
        long rhsBits = Double.doubleToLongBits(rhs);
        if (lhsBits == rhsBits) {
            return 0;
        }
        // Something exotic! A comparison to NaN or 0.0 vs -0.0
        // Fortunately NaN's long is > than everything else
        // Also negzeros bits < poszero
        // NAN: 9221120237041090560
        // MAX: 9218868437227405311
        // NEGZERO: -9223372036854775808
        if (lhsBits < rhsBits) {
            return -1;
        } else {
            return +1;
        }
    }

    /**
     * <p>Compares two floats for order.</p>
     *
     * <p>This method is more comprehensive than the standard Java greater than,
     * less than and equals operators.</p>
     * <ul>
     *  <li>It returns <code>-1</code> if the first value is less than the second.
     *  <li>It returns <code>+1</code> if the first value is greater than the second.
     *  <li>It returns <code>0</code> if the values are equal.
     * </ul>
     *
     * <p> The ordering is as follows, largest to smallest:
     * <ul>
     * <li>NaN
     * <li>Positive infinity
     * <li>Maximum float
     * <li>Normal positive numbers
     * <li>+0.0
     * <li>-0.0
     * <li>Normal negative numbers
     * <li>Minimum float (<code>-Float.MAX_VALUE</code>)
     * <li>Negative infinity
     * </ul>
     *
     * <p>Comparing <code>NaN</code> with <code>NaN</code> will return
     * <code>0</code>.</p>
     *
     * @param lhs  the first <code>float</code>
     * @param rhs  the second <code>float</code>
     * @return <code>-1</code> if lhs is less, <code>+1</code> if greater,
     *  <code>0</code> if equal to rhs
     */
    public static int compare(float lhs, float rhs) {
        if (lhs < rhs) {
            return -1;
        }
        if (lhs > rhs) {
            return +1;
        }
        //Need to compare bits to handle 0.0 == -0.0 being true
        // compare should put -0.0 < +0.0
        // Two NaNs are also == for compare purposes
        // where NaN == NaN is false
        int lhsBits = Float.floatToIntBits(lhs);
        int rhsBits = Float.floatToIntBits(rhs);
        if (lhsBits == rhsBits) {
            return 0;
        }
        //Something exotic! A comparison to NaN or 0.0 vs -0.0
        //Fortunately NaN's int is > than everything else
        //Also negzeros bits < poszero
        //NAN: 2143289344
        //MAX: 2139095039
        //NEGZERO: -2147483648
        if (lhsBits < rhsBits) {
            return -1;
        } else {
            return +1;
        }
    }
}
