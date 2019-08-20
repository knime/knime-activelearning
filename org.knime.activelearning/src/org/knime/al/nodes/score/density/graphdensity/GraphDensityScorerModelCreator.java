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
 *   Aug 7, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density.graphdensity;

import java.util.List;

import org.knime.al.nodes.score.density.AbstractDensityScorerModelCreator;
import org.knime.al.nodes.score.density.DensityScorerModel;
import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.RowKey;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class GraphDensityScorerModelCreator extends AbstractDensityScorerModelCreator<GraphDataPoint> {

    private final double m_sigmaSquared;

    private final int m_nrNeighbors;

    /**
     * @param nrFeatures the number of features used to calculate distances
     * @param sigma for Gaussian kernel
     * @param nrNeighbors number of nearest neighbors to consider
     */
    public GraphDensityScorerModelCreator(final int nrFeatures, final double sigma, final int nrNeighbors) {
        super(nrFeatures);
        m_sigmaSquared = sigma * sigma;
        m_nrNeighbors = nrNeighbors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GraphDataPoint createDataPoint(final RowKey key, final double[] vector) {
        return new GraphDataPoint(key, vector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeUnnormalizedPotential(final KDTree<GraphDataPoint> kdTree,
        final GraphDataPoint dataPoint) {
        // we get the m_nrNeighbors + 1 because the data point itself will also be among the nearest neighbors
        final List<NearestNeighbour<GraphDataPoint>> nearestNeighbors =
            kdTree.getKNearestNeighbours(dataPoint.getVector(), m_nrNeighbors + 1);
        for (final NearestNeighbour<GraphDataPoint> neighbor : nearestNeighbors) {
            // the datapoint itself should not add to the density
            if (neighbor.getData() == dataPoint) {
                continue;
            }
            final double d = neighbor.getDistance();
            final double weight = Math.exp(-d / (2 * m_sigmaSquared));
            final GraphDataPoint ndp = neighbor.getData();
            dataPoint.registerNeighbor(ndp, weight);
            ndp.registerNeighbor(dataPoint, weight);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DensityScorerModel buildModel(final List<GraphDataPoint> dataPoints) {
        return new GraphDensityScorerModel(dataPoints);
    }

}
