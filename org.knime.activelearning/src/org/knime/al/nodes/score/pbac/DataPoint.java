package org.knime.al.nodes.score.pbac;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.RowKey;

/**
 * Representation of a DataPoint in space.
 *
 * @author dietzc
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class DataPoint {

    // Vector containing information
    private final double[] m_vector;

    // Label of the datapoint (if exists)
    private String m_label = "?";

    // The current score of the datapoint
    private double m_score;

    // The current potential of the data-point
    private transient double m_potential;

    private double m_entropy;

    private final Map<String, Double> m_prob;

    private final RowKey m_key;

    /**
     * @param vector
     * @param key
     */
    public DataPoint(final RowKey key, final double[] vector) {
        m_key = key;
        m_vector = vector;
        m_prob = new HashMap<String, Double>();
    }

    /**
     * Gets the vector.
     *
     * @return the vector
     */
    public double[] getVector() {
        return m_vector;
    }

    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * Sets the label.
     *
     * @param label
     *            the new label
     */
    public void setLabel(final String label) {
        m_label = label;
    }

    /**
     * @return the m_potential
     */
    public double getPotential() {
        return m_potential;
    }

    /**
     * Increases the potential by the given amount.
     *
     * @param amount
     *            the amount.
     */
    public void increasePotential(final double amount) {
        m_potential += amount;
    }

    /**
     * sets the potential to the given amount.
     *
     * @param potential
     *            the desired potential
     *
     */
    public void setPotential(final double potential) {
        m_potential = potential;
    }

    /**
     * Decreases the potential by the given amount.
     *
     * @param amount
     *            the amount.
     */
    public void decreasePotential(final double amount) {
        m_potential = Math.max(0, m_potential - amount);
    }

    /**
     * Sets the score.
     *
     * @param score
     *            the score
     */
    public void setScore(final double score) {
        m_score = score;
    }

    /**
     * Gets the score.
     *
     * @return the score
     */
    public double getScore() {
        return m_score;
    }

    /**
     * Sets the entropy of the point.
     *
     * @param entropy
     *            the entropy
     */
    public void setEntropy(final double entropy) {
        m_entropy = entropy;
    }

    /**
     * Gets the entropy.
     *
     * @return the entropy
     */
    public double getEntropy() {
        return m_entropy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DataPoint [key=" + m_key + ", label=" + m_label + ", score="
                + m_score + ", prob=" + m_prob + "]";
    }

}
