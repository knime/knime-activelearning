package org.knime.al.nodes.score.density.graphdensity;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a DataPoint in the feature space.
 *
 * @author dietzc
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 *
 */
class GraphDensityDataPoint {

    //
    private final List<GraphDensityDataPoint> m_neighbors;

    // Vector containing information
    private final double[] m_vector;

    // The current potential of the data-point
    private double m_density;

    // Counter
    private int m_numNeighbours = 0;

    /**
     * @param vector
     *            the vector of this datapoint's location in the featurespace.
     */
    GraphDensityDataPoint(final double[] vector) {
        m_vector = vector;
        m_neighbors = new ArrayList<GraphDensityDataPoint>();
    }

    /**
     * @return the vector
     */
    double[] getVector() {
        return m_vector;
    }

    /**
     * @return the density
     */
    double getDensity() {
        return m_density;
    }

    /**
     * Increases the density by the given amount.
     *
     * @param amount
     *            the amount.
     */
    void increaseDensity(final double amount) {
        m_density += amount;
    }

    /**
     * Sets the density to the given amount.
     *
     * @param density
     *            the desired density
     */
    void setDensity(final double density) {
        m_density = density;
    }

    /**
     * Decreases the density by the given amount.
     *
     * @param amount
     *            the amount
     */
    void decreaseDensity(final double amount) {
        m_density = m_density - amount;
    }

    /**
     * Increases the Neighbour counter by one.
     */
    void increaseNeighbourCounter() {
        m_numNeighbours++;
    }

    /**
     * Normalizes the density of the point.
     */
    void normalizeDensity() {
        if (m_numNeighbours == 0) {
            m_density = 0;
        } else {
            m_density /= m_numNeighbours;
            m_numNeighbours = 0;
        }
    }

    /**
     * Adds the given datapoint as a neighbor.
     *
     * @param dataPoint
     *            the new neighbor
     */
    void registerNeighbor(final GraphDensityDataPoint dataPoint) {
        m_neighbors.add(dataPoint);
    }

    /**
     * decreases the density of all neighbors of this datapoint using the given
     * weight.
     *
     * @param weight
     *            the weight to use when decreasing the the density
     */
    void decreaseDensityOfNeighbors(final double weight) {
        for (final GraphDensityDataPoint nn : m_neighbors) {
            if (nn == this) {
                continue;
            }
            nn.decreaseDensity(m_density * weight);
        }
        m_density -= m_density * weight;
    }
}
