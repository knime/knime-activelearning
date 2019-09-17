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
 *   Aug 2, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.RowKey;
import org.knime.core.node.util.CheckUtils;

/**
 * Abstract implementation of a DensityScorerModel that jointly manages the neighborhoods of individual data points, as
 * well as their potentials and the mapping of {@link RowKey RowKeys} to these entities.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the kind of {@link DensityDataPoint} the DensityScorerModel is created from
 */
public abstract class AbstractDensityScorerModel<V extends DensityDataPoint<V>> implements DensityScorerModel {

    private static final long serialVersionUID = 1774893723409655259L;

    private Map<String, Integer> m_keyToIdx;

    private int[][] m_neighbors;

    private double[] m_potentials;

    private int m_numDataPoints;

    /**
     * Constructor for normal object creation (NOT serialization).
     *
     * @param dataPoints the data points that are part of the model
     */
    public AbstractDensityScorerModel(final List<V> dataPoints) {
        m_numDataPoints = dataPoints.size();
        m_keyToIdx = createKeyToIdxMap(dataPoints);
        m_neighbors = createNeighborMatrix(dataPoints);
        m_potentials = createPotentials(dataPoints);
    }

    /**
     * Serialization constructor. Not meant for any other purpose.
     */
    protected AbstractDensityScorerModel() {
        // no op serialization constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(m_numDataPoints);
        out.writeObject(m_keyToIdx);
        out.writeObject(m_neighbors);
        out.writeObject(m_potentials);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_numDataPoints = in.readInt();
        @SuppressWarnings("unchecked")
        Map<String, Integer> keyToIdx = (Map<String, Integer>)in.readObject();
        m_keyToIdx = keyToIdx;
        m_neighbors = (int[][])in.readObject();
        m_potentials = (double[])in.readObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPotential(final RowKey key) throws UnknownRowException {
        return m_potentials[getIdx(key)];
    }

    @Override
    public final void updateNeighbors(final RowKey key) throws UnknownRowException {
        final int idx = getIdx(key);
        final double potential = getPotential(idx);
        final int[] neighbors = getNeighbors(idx);
        for (int i = 0; i < neighbors.length; i++) {
            final int neighbor = neighbors[i];
            final double decrement = potential * calculateDecrementWeight(idx, i);
            decreasePotential(neighbor, decrement);
        }
        decreasePotential(idx, potential);
    }

    /**
     * @param idx of the update data point
     * @param neighborIdx idx of the current neighbor in the neighborhood of <b>idx</b>
     * @return the weight for the potential of the data point at <b>idx</b> in the update of the data point at
     *         <b>neighborIdx</b>
     */
    protected abstract double calculateDecrementWeight(final int idx, final int neighborIdx);

    /**
     * Retrieves the internal index for {@link RowKey key}.
     *
     * @param key the {@link RowKey} of the row for which the index is required
     * @return the internal index corresponding to {@link RowKey key}
     * @throws UnknownRowException if <b>key</b> is unknown
     */
    protected final int getIdx(final RowKey key) throws UnknownRowException {
        final Integer idx = m_keyToIdx.get(key.getString());
        if (idx == null) {
            throw new UnknownRowException(key);
        }
        return idx.intValue();
    }

    /**
     * Reduces the potential of the row corresponding to <b>idx</b> by <b>decrement</b>.
     *
     * @param idx the internal idx of the row
     * @param decrement the amount by which the potential needs to be reduced
     */
    private void decreasePotential(final int idx, final double decrement) {
        CheckUtils.checkArgument(decrement >= 0, "The decrement must be >= 0 but was %s.", decrement);
        final double potential = getPotential(idx);
        m_potentials[idx] = Math.max(0, potential - decrement);
    }

    /**
     * @param idx the internal idx of the row for which the potential is required
     * @return the potential of the row at position <b>idx</b>
     */
    protected final double getPotential(final int idx) {
        return m_potentials[idx];
    }

    /**
     * @param idx the internal idx of the row for which the neighbors are required
     * @return the sorted indices of the neighbors of the row at position <b>idx</b>
     */
    protected final int[] getNeighbors(final int idx) {
        return m_neighbors[idx];
    }

    private static <V extends DensityDataPoint<V>> double[] createPotentials(final Collection<V> dataPoints) {
        return dataPoints.stream().sequential().mapToDouble(DensityDataPoint::getDensity).toArray();
    }

    private static <V extends DensityDataPoint<V>> Map<String, Integer>
        createKeyToIdxMap(final Collection<V> dataPoints) {
        final Map<String, Integer> keyToIdx = new LinkedHashMap<>(dataPoints.size());
        dataPoints.forEach(p -> keyToIdx.put(p.getKey().getString(), keyToIdx.size()));
        return keyToIdx;
    }

    private int[][] createNeighborMatrix(final Collection<V> dataPoints) {
        return dataPoints.stream().map(this::createNeighborsArray).toArray(int[][]::new);
    }

    private int[] createNeighborsArray(final V dataPoint) {
        final Collection<V> neighbors = dataPoint.getNeighbors();
        final int[] neighborArray = new int[neighbors.size()];
        final Iterator<V> iter = neighbors.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            Integer idx = m_keyToIdx.get(iter.next().getKey().getString());
            CheckUtils.checkState(idx != null, "One of the data points has a neighbor that is not in the dataset. "
                + "This is likely a coding issue.");
            @SuppressWarnings("null") // we explicitly check that the idx is not null
            final int intIdx = idx.intValue();
            neighborArray[i] = intIdx;
        }
        Arrays.sort(neighborArray);
        return neighborArray;
    }

    @Override
    public int getNrRows() {
        return m_numDataPoints;
    }

}
