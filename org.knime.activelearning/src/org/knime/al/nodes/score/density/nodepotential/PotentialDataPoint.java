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
 * Created on 11.03.2013 by dietyc
 */
package org.knime.al.nodes.score.density.nodepotential;

import java.util.ArrayList;
import java.util.List;

import org.knime.al.nodes.score.density.AbstractDensityDataPoint;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.RowKey;
import org.knime.core.node.util.CheckUtils;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Representation of a DataPoint in space.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
final class PotentialDataPoint extends AbstractDensityDataPoint<PotentialDataPoint> {

    private final List<PotentialDataPoint> m_neighbors = new ArrayList<>();

    private final TDoubleList m_squaredNeighborDistances = new TDoubleArrayList();

    /**
     * @param key {@link RowKey} of the corresponding row
     * @param vector the vector for this point
     */
    PotentialDataPoint(final RowKey key, final double[] vector) {
        super(key, vector);
    }

    void registerNeighbor(final NearestNeighbour<PotentialDataPoint> neighbor) {
        final PotentialDataPoint neighborPoint = neighbor.getData();
        CheckUtils.checkArgument(neighborPoint != this, "A data point must not be its own neighbor.");
        m_neighbors.add(neighborPoint);
        final double d = neighbor.getDistance();
        m_squaredNeighborDistances.add(d * d);
    }

    @Override
    public List<PotentialDataPoint> getNeighbors() {
        return m_neighbors;
    }

    TDoubleList getSquaredDistances() {
        return m_squaredNeighborDistances;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void normalizeDensity() {
        // nothing to normalize
    }

}
