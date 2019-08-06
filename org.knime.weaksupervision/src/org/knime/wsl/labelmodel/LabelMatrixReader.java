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
package org.knime.wsl.labelmodel;

import java.util.Iterator;
import java.util.Map;

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
final class LabelMatrixReader {

    private final Map<DataCell, Integer> m_labelToIdxMap;

    private final int m_cardinality;

    private final int m_numSources;

    LabelMatrixReader(final Map<DataCell, Integer> labelToIdxMap, final int numSources) {
        m_labelToIdxMap = labelToIdxMap;
        m_cardinality = m_labelToIdxMap.size();
        m_numSources = numSources;
    }

    float[][] readAndAugmentLabelMatrix(final Iterator<DataRow> iter, final int size, final ExecutionMonitor progress)
        throws CanceledExecutionException {
        final float[][] noisyLabelMatrix = new float[size][m_numSources * m_cardinality];
        for (int i = 0; iter.hasNext(); i++) {
            progress.checkCanceled();
            final int rowIdx = i + 1;
            progress.setProgress(rowIdx / ((double)size), String.format("Reading row %s of %s.", rowIdx, size));
            final DataRow row = iter.next();
            CheckUtils.checkArgument(row.getNumCells() == m_numSources, "The row %s should contain only %s cells.", row,
                m_numSources);
            for (int j = 0; j < m_numSources; j++) {
                final DataCell cell = row.getCell(j);
                if (!cell.isMissing()) {
                    final int label = getLabel(cell);
                    // the label function did vote
                    noisyLabelMatrix[i][j * m_cardinality + label] = 1;
                }
            }
        }
        return noisyLabelMatrix;
    }

    @SuppressWarnings("null") // we check that label is not null
    private int getLabel(final DataCell cell) {
        final Integer label = m_labelToIdxMap.get(cell);
        CheckUtils.checkState(label != null, "Unknown label encountered.");
        return label;
    }

}
