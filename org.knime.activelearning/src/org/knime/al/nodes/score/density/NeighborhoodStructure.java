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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.stream.IntStream;

import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Represents the neighborhoods of a set of rows.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class NeighborhoodStructure implements Externalizable {

    private int[][] m_neighborhoods;

    private boolean m_isSorted;

    private NeighborhoodStructure(final int[][] neighborhoods, final boolean isSorted) {
        m_neighborhoods = neighborhoods;
        m_isSorted = isSorted;
    }

    /**
     * @param keyMap contains the key mapping
     * @param sortNeigborhoods whether the neighborhoods should be sorted ascendingly by index
     * @param dataPoints {@link DensityDataPoint DensityDataPoints} for which the neighborhoods should be created
     * @param monitor for progress monitoring
     * @return a {@link NeighborhoodStructure}
     * @throws CanceledExecutionException
     */
    public static NeighborhoodStructure create(final KeyMap keyMap, final boolean sortNeigborhoods,
        final Collection<? extends DensityDataPoint<?>> dataPoints, final ExecutionMonitor monitor)
        throws CanceledExecutionException {
        return new Creator(keyMap, sortNeigborhoods).create(dataPoints, monitor);
    }

    /**
     * Framework constructor for serialization.
     *
     * @noreference Not intended for use in client code
     */
    public NeighborhoodStructure() {
    }

    /**
     * @param idx for which the neighborhood should be retrieved
     * @return neighborhood for <b>idx</b>
     */
    public int[] getNeighborhood(final int idx) {
        return m_neighborhoods[idx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_neighborhoods = (int[][])in.readObject();
        m_isSorted = in.readBoolean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(m_neighborhoods);
        out.writeBoolean(m_isSorted);
    }

    /**
     * Creator for {@link NeighborhoodStructure} objects.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static final class Creator {
        private final boolean m_sortNeighborhoods;

        private final KeyMap m_keyMap;

        Creator(final KeyMap keyMap, final boolean sortNeigborhoods) {
            m_sortNeighborhoods = sortNeigborhoods;
            m_keyMap = keyMap;
        }

        public NeighborhoodStructure create(final Collection<? extends DensityDataPoint<?>> dataPoints,
            final ExecutionMonitor monitor) throws CanceledExecutionException {
            final int[][] neighborhoods =
                ProcessingUtil.toArrayWithProgress(dataPoints, int[][]::new, (i, p) -> createNeighborsArray(p),
                    ProcessingUtil.progressWithTemplate(monitor, "Creating neighborhood for row %s of %s."));
            return new NeighborhoodStructure(neighborhoods, m_sortNeighborhoods);
        }

        @SuppressWarnings("resource") // toArray is a terminal operation
        private int[] createNeighborsArray(final DensityDataPoint<?> dataPoint) {
            IntStream stream = dataPoint.getNeighbors().stream().map(p -> p.getKey()).mapToInt(this::getIndex);
            if (m_sortNeighborhoods) {
                stream = stream.sorted();
            }
            return stream.toArray();
        }

        private int getIndex(final RowKey key) {
            try {
                return m_keyMap.getIndex(key);
            } catch (UnknownRowException ex) {
                throw new IllegalStateException("Unknown row during model creation.");
            }
        }
    }

}
