/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 26, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Utility class that provides methods for transforming collections with progress monitoring.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ProcessingUtil {

    private ProcessingUtil() {}

    private static void updateProgress(final ExecutionMonitor monitor, final String template, final long step,
        final long totalSteps) throws CanceledExecutionException {
        monitor.checkCanceled();
        monitor.setProgress(step / ((double)totalSteps), () -> String.format(template, step, totalSteps));
    }

    /**
     * Creates a progress with <b>template</b>.
     *
     * @param monitor the {@link ExecutionMonitor} that is used for progress monitoring
     * @param template a String template (see {@link String#format(String, Object...)}) that accepts as first
     *            placeholder the current step and as second the total number of steps
     * @return a {@link Progress}
     */
    public static Progress progressWithTemplate(final ExecutionMonitor monitor, final String template) {
        return (s, t) -> updateProgress(monitor, template, s, t);
    }

    /**
     * @param collection the whose elements need to be transformed and returned as an array
     * @param transformation of elements in <b>collection</b>
     * @param progress for monitoring
     * @return the results of applying <b>transformation</b> on collection as array
     * @throws CanceledExecutionException if the node execution is canceled
     */
    public static <T> int[] toArrayWithProgress(final Collection<T> collection,
        final IndexedToIntFunction<T> transformation, final Progress progress) throws CanceledExecutionException {
        final int[] result = new int[collection.size()];
        collectWithProgress(collection, createTransformingArrayConsumer(result, transformation), progress);
        return result;
    }

    /**
     * @param collection the whose elements need to be transformed and returned as an array
     * @param arrayFactory factory for the result array
     * @param transformation of elements in <b>collection</b>
     * @param progress for monitoring
     * @return the results of applying <b>transformation</b> on collection as array
     * @throws CanceledExecutionException if the node execution is canceled
     */
    public static <T, R> R[] toArrayWithProgress(final Collection<T> collection, final IntFunction<R[]> arrayFactory,
        final IndexedFunction<T, R> transformation, final Progress progress) throws CanceledExecutionException {
        final R[] result = arrayFactory.apply(collection.size());
        collectWithProgress(collection, createTransformingArrayConsumer(result, transformation), progress);
        return result;
    }

    private static <T, R> IndexedConsumer<T> createTransformingArrayConsumer(final R[] dst,
        final IndexedFunction<T, R> transformation) {
        return (i, t) -> dst[i] = transformation.apply(i, t);
    }

    private static <T> IndexedConsumer<T> createTransformingArrayConsumer(final int[] dst,
        final IndexedToIntFunction<T> transformation) {
        return (i, t) -> dst[i] = transformation.apply(i, t);
    }

    /**
     * @param collection elements to consume
     * @param indexedConsumer consumer that accepts indices and elements
     * @param progress for monitoring
     * @throws CanceledExecutionException if the execution is canceled
     */
    public static <T> void collectWithProgress(final Collection<T> collection, final IndexedConsumer<T> indexedConsumer,
        final Progress progress) throws CanceledExecutionException {
        final Iterator<T> iter = collection.iterator();
        final int size = collection.size();
        for (int i = 0; iter.hasNext(); i++) {
            progress.update(i + 1L, size);
            indexedConsumer.accept(i, iter.next());
        }
    }

    /**
     * @param collection elements to consume
     * @param consumer that accepts elements
     * @param progress for monitoring
     * @throws CanceledExecutionException if the execution is canceled
     */
    public static <T> void collectWithProgress(final Collection<T> collection, final Consumer<T> consumer,
        final Progress progress) throws CanceledExecutionException {
        collectWithProgress(collection, (i, t) -> consumer.accept(t), progress);
    }

    /**
     * A {@link Consumer} that accepts an index as first argument. Similar to {@link ObjIntConsumer} but with the
     * reverse ordering of arguments in order to comply with the remaining functional interfaces in this class (e.g.
     * IndexedToIntFunction).
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> the type of the input to the operation
     */
    @FunctionalInterface
    public interface IndexedConsumer<T> {

        /**
         * @param idx index of <b>t</b>
         * @param t the input argument
         */
        void accept(final int idx, final T t);
    }

    /**
     * A {@link ToIntFunction} that accepts an index as first argument.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> the type of input to the operation
     */
    @FunctionalInterface
    public interface IndexedToIntFunction<T> {

        /**
         * @param idx index of <b>t</b>
         * @param t the function argument
         * @return the function result
         */
        int apply(final int idx, final T t);
    }

    /**
     * A {@link Function} that accepts and index as first argument.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> the function input type
     * @param <R> the function output type
     */
    @FunctionalInterface
    public interface IndexedFunction<T, R> {

        /**
         * @param idx index of <b>t</b>
         * @param t the function argument
         * @return the function result
         */
        R apply(final int idx, final T t);
    }

    /**
     * Functional interface for monitoring execution progress.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    @FunctionalInterface
    public interface Progress {

        /**
         * Checks if the execution is canceled and updates the execution progress.
         *
         * @param currentStep the index of the current execution step
         * @param totalSteps the total number of execution steps
         * @throws CanceledExecutionException if the execution is canceled
         */
        void update(long currentStep, long totalSteps) throws CanceledExecutionException;
    }

}
