package org.knime.al.nodes.score.density.graphdensity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.al.nodes.score.density.AbstractDensityDataPoint;
import org.knime.core.data.RowKey;
import org.knime.core.node.util.CheckUtils;

/**
 * Representation of a DataPoint in the feature space.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
final class GraphDataPoint extends AbstractDensityDataPoint<GraphDataPoint> {

    private final Set<GraphDataPoint> m_neighbors = new LinkedHashSet<>();

    private boolean m_isNormalized = false;

    /**
     * @param vector the vector of this datapoint's location in the featurespace.
     */
    GraphDataPoint(final RowKey key, final double[] vector) {
        super(key, vector);
    }

    /**
     * Normalizes the density of the point.
     */
    @Override
    public void normalizeDensity() {
        if (!m_isNormalized) {
            setDensity(getDensity() / m_neighbors.size());
            m_isNormalized = true;
        }
    }

    /**
     * Adds the given datapoint as a neighbor.
     *
     * @param dataPoint the new neighbor
     * @param the weight of the new neighbor edge
     */
    void registerNeighbor(final GraphDataPoint dataPoint, final double weight) {
        CheckUtils.checkArgument(dataPoint != this, "A data point must not be its own neighbor.");
        if (m_neighbors.add(dataPoint)) {
            increaseDensity(weight);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<GraphDataPoint> getNeighbors() {
        return m_neighbors;
    }


}
