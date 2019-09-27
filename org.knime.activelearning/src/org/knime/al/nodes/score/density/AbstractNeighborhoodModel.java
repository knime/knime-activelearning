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
import java.util.UUID;

import org.knime.core.data.RowKey;

/**
 * Abstract implementation of a NeighborhoodModel that jointly manages the neighborhoods of individual data points, as
 * well as the mapping of {@link RowKey RowKeys} to these entities.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractNeighborhoodModel implements NeighborhoodModel {

    private static final long serialVersionUID = 1774893723409655259L;

    private KeyMap m_keyMap;

    private NeighborhoodStructure m_neighborhoods;

    private UUID m_id;

    /**
     * Constructor for normal object creation (NOT serialization).
     * @param keyMap maps from {@link RowKey rowKeys} to neighborhood indices
     * @param neighborhoods contains neighborhoods for all rows in this model
     *
     */
    protected AbstractNeighborhoodModel(final KeyMap keyMap, final NeighborhoodStructure neighborhoods) {
        m_keyMap = keyMap;
        m_neighborhoods = neighborhoods;
        m_id = UUID.randomUUID();
    }

    /**
     * Serialization constructor. Not meant for any other purpose.
     */
    protected AbstractNeighborhoodModel() {
        // no op serialization constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(m_keyMap);
        out.writeObject(m_neighborhoods);
        out.writeObject(m_id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        m_keyMap = (KeyMap)in.readObject();
        m_neighborhoods = (NeighborhoodStructure)in.readObject();
        m_id = (UUID)in.readObject();
    }

    @Override
    public final void updateNeighbors(final PotentialUpdater potentialUpdater, final RowKey key)
        throws UnknownRowException {
        final int idx = getIndex(key);
        final double potential = potentialUpdater.getPotential(idx);
        final int[] neighbors = m_neighborhoods.getNeighborhood(idx);
        for (int i = 0; i < neighbors.length; i++) {
            final int neighbor = neighbors[i];
            final double decrement = potential * calculateDecrementWeight(idx, i);
            potentialUpdater.decreasePotential(neighbor, decrement);
        }
        potentialUpdater.decreasePotential(idx, potential);
    }

    /**
     * @return the neighborhoods
     */
    protected final NeighborhoodStructure getNeighborhoods() {
        return m_neighborhoods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getNrRows() {
        return m_keyMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final UUID getId() {
        return m_id;
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
    @Override
    public final int getIndex(final RowKey key) throws UnknownRowException {
        return m_keyMap.getIndex(key);
    }

}
