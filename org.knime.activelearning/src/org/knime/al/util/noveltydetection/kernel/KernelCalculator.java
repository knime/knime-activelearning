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
 * --------------------------------------------------------------------- *
 *
 */

package org.knime.al.util.noveltydetection.kernel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.ThreadPool;

public class KernelCalculator implements Externalizable {
    static final int DEFAULT_NUM_CORES = 4;

    public enum KernelType {
        RBF("RBF"), HIK("HIK"), EXPHIK("EXPHIK"), Polynomial("Polynomial");

        private final String m_name;

        private KernelType(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    // Holds the training data
    private double[][] m_trainingData;
    private int m_rowCount;
    private int m_colCount;

    private KernelFunction m_kernelFunction;

    public KernelCalculator() {

    }

    public KernelCalculator(final BufferedDataTable trainingData,
            final KernelFunction kernelFunction) {
        m_trainingData = readBufferedDataTable(trainingData);
        final long size = trainingData.size();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("The input table is too large.");
        }
        m_rowCount = (int) size;
        m_colCount = trainingData.getDataTableSpec().getNumColumns();
        m_kernelFunction = kernelFunction;
    }

    public KernelCalculator(final double[][] trainingData,
            final KernelFunction kernelFunction) {
        m_trainingData = trainingData;
        m_rowCount = trainingData.length;
        m_colCount = trainingData[0].length;
        m_kernelFunction = kernelFunction;
    }

    // public KernelCalculator(KernelFunction kernelFunction) {
    // m_kernelFunction = kernelFunction;
    // }

    /*
     * Returns kernel matrix containing similarities of the training data
     * Output: mxm matrix containing similarities of the training data
     */
    public RealMatrix kernelize(final ExecutionMonitor progMon)
            throws Exception {
        return calculateKernelMatrix(m_trainingData, m_trainingData, progMon);
    }

    public RealMatrix kernelize(final BufferedDataTable trainingData,
            final BufferedDataTable testData, final ExecutionMonitor progMon)
                    throws Exception {
        return calculateKernelMatrix(readBufferedDataTable(trainingData),
                readBufferedDataTable(testData), progMon);
    }

    /*
     * Returns kernel matrix containing similarities of test data with training
     * data Parameters: testData: BufferedDataTable containing the test data
     * Output: nxm matrix containing the similarities of n test samples with m
     * training samples
     */
    public RealMatrix kernelize(final BufferedDataTable testData,
            final ExecutionMonitor progMon) throws Exception {
        return calculateKernelMatrix(m_trainingData,
                readBufferedDataTable(testData), progMon);
    }

    public RealMatrix kernelize(final DataRow testInstance) {
        return calculateKernelVector(m_trainingData, readDataRow(testInstance),
                m_kernelFunction);
    }

    private double[] readDataRow(final DataRow row) {
        final double[] data = new double[row.getNumCells()];
        for (int i = 0; i < row.getNumCells(); i++) {
            final DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                throw new IllegalArgumentException(
                        "Missing values are not supported.");
            } else if (!cell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "Only numerical data types are currently supported.");
            } else {
                data[i] = ((DoubleValue) cell).getDoubleValue();
            }
        }
        return data;
    }

    public RealMatrix kernelize(final double[][] testData,
            final ExecutionMonitor progMon) throws Exception {
        return calculateKernelMatrix(m_trainingData, testData, progMon);
    }

    public RealMatrix kernelize(final double[][] trainingData,
            final double[][] testData, final ExecutionMonitor progMon)
                    throws Exception {

        return calculateKernelMatrix(trainingData, testData, progMon);
    }

    public int getNumTrainingSamples() {
        return m_rowCount;
    }

    public RealMatrix calculateKernelMatrix(final double[][] training,
            final double[][] test, final ExecutionMonitor progMon)
                    throws Exception {

        final ThreadPool pool = KNIMEConstants.GLOBAL_THREAD_POOL;
        final int procCount =
                (int) (Runtime.getRuntime().availableProcessors() * (2.0 / 3));
        final Semaphore semaphore = new Semaphore(procCount);
        RealMatrix result = null;
        try {
            result = pool.runInvisible(new Callable<RealMatrix>() {

                @Override
                public RealMatrix call() throws Exception {
                    final double[][] resultArrayMatrix =
                            new double[training.length][test.length];
                    final CalculateKernelValuesRunnable[] kct =
                            new CalculateKernelValuesRunnable[test.length];
                    final int numberOfRunnables = kct.length;
                    for (int i = 0; i < numberOfRunnables; i++) {
                        kct[i] = new CalculateKernelValuesRunnable(0,
                                training.length, i, i + 1, training, test,
                                resultArrayMatrix, m_kernelFunction, semaphore);
                    }
                    final Future<?>[] threads =
                            new Future<?>[numberOfRunnables];
                    double progCounter = 0;
                    for (int i = 0; i < numberOfRunnables; i++) {
                        try {
                            progMon.checkCanceled();
                        } catch (final Exception e) {
                            for (int j = 0; j < i; j++) {
                                if (threads[j] != null) {
                                    threads[j].cancel(true);
                                }
                            }
                            throw e;
                        }
                        semaphore.acquire();
                        threads[i] = pool.enqueue(kct[i]);
                        progMon.setProgress(
                                progCounter / (2 * numberOfRunnables),
                                "Kernel calculation started (" + i + "/"
                                        + numberOfRunnables + ")");
                        progCounter += 1;
                    }
                    for (int i = 0; i < numberOfRunnables; i++) {
                        try {
                            progMon.checkCanceled();
                        } catch (final Exception e) {
                            for (int j = 0; j < numberOfRunnables; j++) {
                                if (threads[j] != null) {
                                    threads[j].cancel(true);
                                }
                            }
                            throw e;
                        }
                        semaphore.acquire();
                        threads[i].get();
                        semaphore.release();
                        progMon.setProgress(
                                progCounter / (2 * numberOfRunnables),
                                "Kernel calculation finished (" + i + "/"
                                        + numberOfRunnables + ")");
                        progCounter += 1;
                    }
                    return MatrixUtils.createRealMatrix(resultArrayMatrix);
                }

            });
        } catch (final Exception e) {
            throw e;
        }

        return result;
    }

    private RealMatrix calculateKernelVector(final double[][] training,
            final double[] test, final KernelFunction kernelFunction) {
        final double[] result = new double[training.length];

        for (int r = 0; r < training.length; r++) {
            result[r] = kernelFunction.calculate(training[r], test);
        }
        return MatrixUtils.createColumnRealMatrix(result);
    }

    private class CalculateKernelValuesRunnable implements Runnable {

        private final int m_trainingStart;
        private final int m_trainingEnd;
        private final int m_testStart;
        private final int m_testEnd;
        private final double[][] m_training;
        private final double[][] m_test;
        private final double[][] m_result;
        private final KernelFunction m_kernelFunction;
        private final Semaphore m_semaphore;

        public CalculateKernelValuesRunnable(final int trainingStart,
                final int trainingEnd, final int testStart, final int testEnd,
                final double[][] training, final double[][] test,
                final double[][] result, final KernelFunction kernelFunction,
                final Semaphore semaphore) {
            m_trainingStart = trainingStart;
            m_trainingEnd = trainingEnd;
            m_testStart = testStart;
            m_testEnd = testEnd;
            m_training = training;
            m_test = test;
            m_result = result;
            m_kernelFunction = kernelFunction;
            m_semaphore = semaphore;
        }

        @Override
        public void run() {
            for (int r = m_trainingStart; r < m_trainingEnd; r++) {
                for (int c = m_testStart; c < m_testEnd; c++) {
                    m_result[r][c] = m_kernelFunction.calculate(m_training[r],
                            m_test[c]);
                }
            }
            m_semaphore.release();

        }

    }

    public RealMatrix calculateKernelMatrix_singleThread(
            final double[][] training, final double[][] test) {
        final double[][] result = new double[training.length][test.length];

        for (int s1 = 0; s1 < training.length; s1++) {
            for (int s2 = 0; s2 < test.length; s2++) {
                result[s1][s2] =
                        m_kernelFunction.calculate(training[s1], test[s2]);
            }
        }

        return MatrixUtils.createRealMatrix(result);
    }

    // private RealMatrix calculateKernelMatrix(double[][] training,
    // BufferedDataTable test) {
    // final RealMatrix kernelMatrix =
    // MatrixUtils.createRealMatrix(training.length, test.getRowCount());
    //
    // for (int r = 0; r < training.length; r++) {
    // Iterator<DataRow> testIterator = test.iterator();
    // for (int c = 0; c < test.getRowCount(); c++) {
    // kernelMatrix.setEntry(r, c, m_kernelFunction.calculate(training[r],
    // testIterator.next()));
    // }
    // }
    //
    // return kernelMatrix;
    // }

    public static double[][]
            readBufferedDataTable(final BufferedDataTable table) {
        final long size = table.size();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("The input table is too large.");
        }
        final double[][] data = new double[(int) size][table.getDataTableSpec()
                .getNumColumns()];

        final Iterator<DataRow> iterator = table.iterator();

        for (int r = 0; iterator.hasNext(); r++) {
            final DataRow next = iterator.next();
            for (int c = 0; c < table.getDataTableSpec().getNumColumns(); c++) {
                final DataCell cell = next.getCell(c);
                if (cell.isMissing()) {
                    throw new IllegalArgumentException(
                            "Missing values are not supported.");
                } else if (!cell.getType().isCompatible(DoubleValue.class)) {
                    throw new IllegalArgumentException(
                            "Only numerical data types are currently supported.");
                } else {
                    data[r][c] = ((DoubleValue) cell).getDoubleValue();
                }
            }
        }

        return data;
    }

    /************ Externalizable methods *****************/
    @Override
    public void readExternal(final ObjectInput in)
            throws IOException, ClassNotFoundException {
        try {
            // read kernelFunction
            m_kernelFunction =
                    (KernelFunction) Class.forName(in.readUTF()).newInstance();
            m_kernelFunction.readExternal(in);

            // read trainingData
            // rows
            m_rowCount = in.readInt();
            // columns
            m_colCount = in.readInt();
            // data
            m_trainingData = new double[m_rowCount][m_colCount];
            for (int r = 0; r < m_rowCount; r++) {
                for (int c = 0; c < m_colCount; c++) {
                    m_trainingData[r][c] = in.readDouble();
                }
            }

        } catch (InstantiationException | IllegalAccessException e) {
            throw new IOException(e);
        }

    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        // write kernelFunction
        out.writeUTF(m_kernelFunction.getClass().getName());
        m_kernelFunction.writeExternal(out);

        // write trainingData
        // rows
        out.writeInt(m_rowCount);
        // columns
        out.writeInt(m_colCount);
        // data
        for (final double[] row : m_trainingData) {
            for (final double col : row) {
                out.writeDouble(col);
            }
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_colCount;
        result = prime * result + ((m_kernelFunction == null) ? 0
                : m_kernelFunction.hashCode());
        result = prime * result + m_rowCount;
        result = prime * result + Arrays.hashCode(m_trainingData);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof KernelCalculator)) {
            return false;
        }
        final KernelCalculator other = (KernelCalculator) obj;
        if (m_colCount != other.m_colCount) {
            return false;
        }
        if (m_kernelFunction == null) {
            if (other.m_kernelFunction != null) {
                return false;
            }
        } else if (!m_kernelFunction.equals(other.m_kernelFunction)) {
            return false;
        }
        if (m_rowCount != other.m_rowCount) {
            return false;
        }
        if (!Arrays.deepEquals(m_trainingData, other.m_trainingData)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "KernelCalculator [m_trainingData="
                + (m_trainingData != null ? Arrays.asList(m_trainingData)
                        .subList(0, Math.min(m_trainingData.length, maxLen))
                        : null)
                + ", m_rowCount=" + m_rowCount + ", m_colCount=" + m_colCount
                + ", m_kernelFunction=" + m_kernelFunction + "]";
    }
}
