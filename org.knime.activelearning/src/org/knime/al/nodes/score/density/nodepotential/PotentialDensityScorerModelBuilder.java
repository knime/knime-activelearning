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
 *   Aug 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density.nodepotential;

import java.util.List;

import org.knime.al.nodes.score.density.AbstractDensityScorerModelBuilder;
import org.knime.al.nodes.score.density.DensityScorerModel;
import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.RowKey;

/**
 * Builder for PotentialDensityScorerModels.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PotentialDensityScorerModelBuilder extends AbstractDensityScorerModelBuilder<PotentialDataPoint> {

    /**
     * Threshold for a warning regarding neighborhood sizes. More precisely, if the ratio between the neighborhood of a
     * data point and the full dataset exceeds this threshold, a warning is displayed during the execution of the
     * intializer node.
     */
    private static final double WARNING_THRESHOLD = 0.2;

    private final double m_radiusAlpha;

    private final double m_alpha;

    private final double m_beta;

    private static final double FACTOR_RB = 1.25d;

    PotentialDensityScorerModelBuilder(final int numFeatures, final double radiusAlpha) {
        super(numFeatures);
        m_radiusAlpha = radiusAlpha;
        m_alpha = 4.0 / (radiusAlpha * radiusAlpha);
        final double rb = radiusAlpha * FACTOR_RB;
        m_beta = 4.0 / (rb * rb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PotentialDataPoint createDataPoint(final RowKey key, final double[] vector) {
        return new PotentialDataPoint(key, vector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeUnnormalizedPotential(final KDTree<PotentialDataPoint> kdTree,
        final PotentialDataPoint dataPoint) {
        final List<NearestNeighbour<PotentialDataPoint>> neighbors =
            kdTree.getMaxDistanceNeighbours(dataPoint.getVector(), m_radiusAlpha * FACTOR_RB);
        if (neighbors.size() / ((double)getNumberOfDataPoints()) > 0.2) {
            setWarning(String.format("Some rows have more than %s%% of the dataset in their neighborhood. "
                + "Consider reducing the radius alpha.", WARNING_THRESHOLD * 100));
        }

        for (final NearestNeighbour<PotentialDataPoint> nn : neighbors) {
            // don't compare to itself
            if (nn.getData() == dataPoint) {
                continue;
            }
            dataPoint.registerNeighbor(nn);
            final double dist = nn.getDistance();
            if (dist <= m_radiusAlpha) {
                dataPoint.increaseDensity(Math.exp(dist * dist * -m_alpha));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DensityScorerModel buildModel(final List<PotentialDataPoint> dataPoints) {
        return new PotentialDensityScorerModel(dataPoints, m_beta);
    }

}
