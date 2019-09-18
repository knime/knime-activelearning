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
 */
package org.knime.wsl.weaklabelmodel;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;

/**
 * Reads the noisy labels into an one-hot encoded matrix expected by the backend.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
public final class LabelMatrixReader {

    private final int m_nrClasses;

    private final int m_numSources;

    private final SourceParser[] m_parsers;

    /**
     * Instantiates a LabelMatrixReader.
     *
     * @param parsers the {@link SourceParser parsers} for the different source columns
     * @param nrClasses the number of possible classes
     */
    public LabelMatrixReader(final SourceParser[] parsers, final int nrClasses) {
        m_parsers = parsers;
        m_nrClasses = nrClasses;
        m_numSources = parsers.length;
    }

    /**
     * @param iter an {@link Iterator} of {@link DataRow rows}
     * @param size the number of rows in {@link Iterator iter}
     * @param progress monitor
     * @return the covariance matrix of the one-hot encoded source labels
     * @throws CanceledExecutionException if the node execution is canceled
     */
    public float[][] readCovarianceMatrix(final Iterator<DataRow> iter, final long size,
        final ExecutionMonitor progress) throws CanceledExecutionException {
        final CovarianceMatrixConsumer covMatConsumer = new CovarianceMatrixConsumer(m_parsers, m_nrClasses, size);
        for (int i = 1; iter.hasNext(); i++) {
            updateProgress(size, progress, i);
            final DataRow row = iter.next();
            covMatConsumer.consume(row);
        }
        return covMatConsumer.getCovarianceMatrix();
    }

    private static class CovarianceMatrixConsumer {
        private final SourceParser[] m_parsers;

        private final int m_nrSources;

        private final int m_nrClasses;

        private final float[][] m_covMat;

        private final long m_nrRows;

        long m_rowCounter = 0;

        CovarianceMatrixConsumer(final SourceParser[] parsers, final int nrClasses, final long nrRows) {
            CheckUtils.checkArgument(nrRows > 0, "The number of rows to consume must be positive but was %s", nrRows);
            m_parsers = parsers;
            m_nrSources = parsers.length;
            m_nrClasses = nrClasses;
            final int dim = nrClasses * m_nrSources;
            m_covMat = new float[dim][dim];
            m_nrRows = nrRows;
        }

        void consume(final DataRow row) {
            CheckUtils.checkArgument(m_nrSources == row.getNumCells(),
                "Unexpected number of cells in row %s (expected %s but received %s).", row.getKey(), m_nrSources,
                row.getNumCells());
            CheckUtils.checkState(m_rowCounter < m_nrRows, "More rows consumed (%s) than expected (%s).", m_rowCounter,
                m_nrRows);
            for (int j = 0; j < m_nrSources; j++) {
                final DataCell s1 = row.getCell(j);
                for (int k = j; k < m_nrSources; k++) {
                    final DataCell s2 = row.getCell(k);
                    loopOverClasses(j, s1, k, s2);
                }
            }
            m_rowCounter++;
        }

        private void loopOverClasses(final int firstIndex, final DataCell firstCell, final int secondIndex,
            final DataCell secondCell) {
            final SourceParser parser1 = m_parsers[firstIndex];
            final SourceParser parser2 = m_parsers[secondIndex];
            for (int l1 = 0; l1 < m_nrClasses; l1++) {
                int covMatRow = firstIndex * m_nrClasses + l1;
                float p1 = parser1.parseProbability(firstCell, l1);
                for (int l2 = 0; l2 < m_nrClasses; l2++) {
                    final float p2 = parser2.parseProbability(secondCell, l2);
                    final float increment = p1 * p2;
                    final int covMatCol = secondIndex * m_nrClasses + l2;
                    m_covMat[covMatRow][covMatCol] += increment;
                    if (firstIndex != secondIndex) {
                        m_covMat[covMatCol][covMatRow] += increment;
                    }
                }
            }
        }

        float[][] getCovarianceMatrix() {
            CheckUtils.checkState(m_rowCounter == m_nrRows, "Fewer rows consumed (%s) than expected (%s).",
                m_rowCounter, m_nrRows);
            return convertToNormalizedFloatCovMat();
        }

        private float[][] convertToNormalizedFloatCovMat() {
            final float[][] covMat = new float[m_covMat.length][m_covMat.length];
            for (int i = 0; i < m_covMat.length; i++) {
                for (int j = 0; j < m_covMat[i].length; j++) {
                    covMat[i][j] = m_covMat[i][j] / m_nrRows;
                }
            }
            return covMat;
        }
    }

    /**
     * @param iter {@link Iterator} of {@link DataRow rows} where each row contains only the source columns
     * @param size the number of rows in <b>iter</b>
     * @param progress monitor
     * @return a float matrix holding the augmented (i.e. one-hot encoded) labels
     * @throws CanceledExecutionException if the node execution is canceled
     */
    public float[][] readAndAugmentLabelMatrix(final Iterator<DataRow> iter, final int size,
        final ExecutionMonitor progress) throws CanceledExecutionException {
        final float[][] noisyLabelMatrix = new float[size][m_numSources * m_nrClasses];
        for (int i = 0; iter.hasNext(); i++) {
            updateProgress(size, progress, i);
            final DataRow row = iter.next();
            noisyLabelMatrix[i] = readAndAugmentRow(row);
        }
        return noisyLabelMatrix;
    }

    /**
     * Reads the values in {@link DataRow row} into an array where each cell in {@link DataRow row} is represented
     * as probabilities over the possible classes.</br>
     * Example: Consider classes A, B and C, then a row [A, C] becomes an array {1, 0, 0, 0, 0, 1}.
     *
     * @param row {@link DataRow} of label sources
     * @return an array representing {@link DataRow row} as probabilities over classes
     */
    public float[] readAndAugmentRow(final DataRow row) {
        CheckUtils.checkArgument(row.getNumCells() == m_numSources, "The row %s should contain only %s cells.", row,
            m_numSources);
        final float[] augmented = new float[m_numSources * m_nrClasses];
        for (int j = 0; j < m_numSources; j++) {
            final DataCell cell = row.getCell(j);
            if (!cell.isMissing()) {
                // the label function did vote
                extractProbabilities(augmented, j, cell);
            }
        }
        return augmented;
    }

    private void extractProbabilities(final float[] dest, final int sourceIndex, final DataCell cell) {
        final SourceParser sourceParser = m_parsers[sourceIndex];
        for (int k = 0; k < m_nrClasses; k++) {
            dest[sourceIndex * m_nrClasses + k] = sourceParser.parseProbability(cell, k);
        }
    }

    private static void updateProgress(final double size, final ExecutionMonitor progress, final int i)
        throws CanceledExecutionException {
        progress.checkCanceled();
        final int rowIdx = i + 1;
        progress.setProgress(rowIdx / size, () -> String.format("Reading row %s of %s.", rowIdx, (int)size));
    }

}
