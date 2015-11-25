/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */

package org.knime.al.nodes.score.novelty.localnoveltyscorer;

import java.util.Comparator;
import java.util.PriorityQueue;

/*
 * A simple pair class for a double and an int value
 */
public class ValueIndexPair {

    private final double m_value;
    private final int m_index;

    public ValueIndexPair(final double value, final int index) {
        m_value = value;
        m_index = index;
    }

    public double getValue() {
        return m_value;
    }

    public int getIndex() {
        return m_index;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof ValueIndexPair
                && ((ValueIndexPair) object).getValue() == getValue()
                && ((ValueIndexPair) object).getIndex() == getIndex()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "(" + m_value + ", " + m_index + ")";
    }

    /*
     * Transforms the input array to an array of ValueIndexPairs Parameters:
     * array: Double array that should be converted to ValueIndexPair array
     * Output: ValueIndexPair array containing the pairs of the input array
     */
    public static ValueIndexPair[]
            transformArray2ValueIndexPairArray(final double[] array) {
        final ValueIndexPair[] result = new ValueIndexPair[array.length];

        for (int i = 0; i < array.length; i++) {
            result[i] = new ValueIndexPair(array[i], i);
        }
        return result;
    }

    /*
     * Gets first k elements of array k depending on the ordering induced by the
     * comparator Parameters: array: ValueIndexPair array k : Number of elements
     * to be selected comparator: Comparator for the ValueIndexPair class
     * Output: ValueIndexPair array with k elements (NOTE: The array is NOT in
     * order)
     */
    public static ValueIndexPair[] getK(final ValueIndexPair[] array,
            final int k, final Comparator<ValueIndexPair> comparator) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be greater than zero!");
        }
        if (k > array.length) {
            throw new IllegalArgumentException(
                    "k must be smaller or equal to the length of the array!");
        }

        // heapComp induces the opposite ordering to comparator
        final Comparator<ValueIndexPair> heapComp =
                new Comparator<ValueIndexPair>() {
                    @Override
                    public int compare(final ValueIndexPair o1,
                            final ValueIndexPair o2) {
                        return -comparator.compare(o1, o2);
                    }
                };

        // heap structure to keep first k elements
        final PriorityQueue<ValueIndexPair> heap =
                new PriorityQueue<ValueIndexPair>(k, heapComp);

        for (int i = 0; i < array.length; i++) {
            // fill heap
            if (i < k) {
                heap.add(array[i]);
            } else {
                // check if head of heap is larger than new element
                if (comparator.compare(array[i], heap.peek()) == -1) {
                    // remove head
                    heap.poll();
                    // add new element and restore heap structure
                    heap.add(array[i]);
                }
            }

        }

        return heap.toArray(new ValueIndexPair[heap.size()]);

    }

}
