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
package org.knime.al.nodes.score.density;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;

/**
 * Abstract implementation of a {@link DensityScorerModelCreator} that performs data reading, progress monitoring and
 * potential normalization.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> The type of {@link DensityDataPoint}
 */
public abstract class AbstractDensityScorerModelCreator<V extends DensityDataPoint<V>>
    implements DensityScorerModelCreator {

    private final List<V> m_dataPoints = new ArrayList<>();

    private String m_warning = null;

    private final KDTreeBuilder<V> m_kdTreeBuilder;

    private final int m_nrFeatures;

    private ExceptionHandling m_missingValueHandling = ExceptionHandling.FAIL;

    private int m_ignoredRows = 0;

    /**
     * @param nrFeatures the number of features used to calculate distances
     */
    public AbstractDensityScorerModelCreator(final int nrFeatures) {
        m_kdTreeBuilder = new KDTreeBuilder<>(nrFeatures);
        m_nrFeatures = nrFeatures;
    }

    @Override
    public void setMissingValueHandling(final ExceptionHandling missingValueHandling) {
        m_missingValueHandling = missingValueHandling;
    }

    /**
     * @param key of the row
     * @param vector of features
     * @return a {@link DensityDataPoint} with key <b>key</b> and featue vector <b>vector</b>
     */
    protected abstract V createDataPoint(final RowKey key, final double[] vector);

    /**
     * Initializes the unnormalized potential of <b>dataPoint</b>. This method may also modify other data points that
     * <b>dataPoint</b> interacts with.
     *
     * @param kdTree a {@link KDTree} for efficient neighborhood queries
     * @param dataPoint the dataPoint whose potential needs to be initialized
     */
    protected abstract void initializeUnnormalizedPotential(final KDTree<V> kdTree, final V dataPoint);

    /**
     * This method is called once all potentials have been calculated and normalized.
     *
     * @param dataPoints a list of the dataPoints
     * @param monitor for reporting progress
     * @return the final model
     * @throws CanceledExecutionException if the node execution is canceled
     */
    protected abstract NeighborhoodModel buildModel(final List<V> dataPoints, final ExecutionMonitor monitor)
        throws CanceledExecutionException;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getWarning() {
        if (m_ignoredRows > 0) {
            return Optional.of(String.format("%s row%s ignored due to missing values.", m_ignoredRows,
                m_ignoredRows == 1 ? " is" : "s are"));
        }
        return Optional.ofNullable(m_warning);
    }

    /**
     * @param warning the warning message
     */
    protected final void setWarning(final String warning) {
        m_warning = warning;
    }

    /**
     * @return the number of data points added via {@link AbstractDensityScorerModelCreator#addRow(DataRow)}
     */
    protected final int getNumberOfDataPoints() {
        return m_dataPoints.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addRow(final DataRow row) {
        CheckUtils.checkArgument(row.getNumCells() == m_nrFeatures,
            "The row %s has not the expected number of cells %s.", row, m_nrFeatures);
        final double[] vector = toVector(row);
        if (vector == null) {
            // the row is ignored
            return;
        }
        final V dataPoint = createDataPoint(row.getKey(), vector);
        m_dataPoints.add(dataPoint);
        m_kdTreeBuilder.addPattern(vector, dataPoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final DensityScorerModel buildModel(final ExecutionMonitor monitor) throws CanceledExecutionException {
        initializeUnnormalizedPotentials(monitor.createSubProgress(0.3));
        normalizePotentials(monitor.createSubProgress(0.3));
        final NeighborhoodModel neighborhoodModel = buildModel(m_dataPoints, monitor.createSubProgress(0.4));
        final double[] potentials = m_dataPoints.stream().mapToDouble(DensityDataPoint::getDensity).toArray();
        return new DefaultDensityScorerModel(potentials, neighborhoodModel);
    }

    private void initializeUnnormalizedPotentials(final ExecutionMonitor monitor) throws CanceledExecutionException {
        final KDTree<V> kdTree = m_kdTreeBuilder.buildTree(monitor.createSubProgress(0.2));
        initializeUnnormalizedPotentials(monitor.createSubProgress(0.8), kdTree);
    }

    private void initializeUnnormalizedPotentials(final ExecutionMonitor monitor, final KDTree<V> kdTree)
        throws CanceledExecutionException {
        ProcessingUtil.collectWithProgress(m_dataPoints, p -> initializeUnnormalizedPotential(kdTree, p),
            ProcessingUtil.progressWithTemplate(monitor, "Initializing potential for row %s of %s"));
    }

    private void normalizePotentials(final ExecutionMonitor monitor) throws CanceledExecutionException {
        final SummaryStatistics stats = new SummaryStatistics();
        final ExecutionMonitor minMaxProgress = monitor.createSubProgress(0.5);
        ProcessingUtil.collectWithProgress(m_dataPoints, p -> {
            p.normalizeDensity();
            stats.addValue(p.getDensity());
        }, ProcessingUtil.progressWithTemplate(minMaxProgress,
            "Searching for min and max potentials in row %s of %s."));
        final double min = stats.getMin();
        final double max = stats.getMax();
        if (min == max) {
            monitor.setProgress(1.0);
            return;
        }
        normalize(monitor.createSubProgress(0.5), getNormalizer(min, max));
    }

    private static DoubleUnaryOperator getNormalizer(final double min, final double max) {
        if (min == max) {
            // if all potentials are zero, then there is no density
            // but if all potentials are the same non-zero value all points have the highest density
            return min == 0 ? p -> 0 : p -> 1;
        } else {
            return p -> (p - min) / (max - min);
        }
    }

    private void normalize(final ExecutionMonitor normalizerProgress, final DoubleUnaryOperator normalizer)
        throws CanceledExecutionException {
        ProcessingUtil.collectWithProgress(m_dataPoints,
            p -> p.setDensity(normalizer.applyAsDouble(p.getDensity())),
            ProcessingUtil.progressWithTemplate(normalizerProgress, "Normalizing potential in row %s of %s."));
    }

    private double[] toVector(final DataRow row) {
        final double[] vector = new double[m_nrFeatures];
        for (int i = 0; i < m_nrFeatures; i++) {
            final DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                switch (m_missingValueHandling) {
                    case FAIL:
                        throw new IllegalArgumentException(
                            String.format("Missing value in row %s detected.", row.getKey()));
                    case IGNORE:
                        m_ignoredRows++;
                        return null;
                    default:
                        throw new IllegalStateException(
                            String.format("Unknown missing value handling %s detected.", m_missingValueHandling));

                }
            }
            CheckUtils.checkArgument(cell instanceof DoubleValue, "Non numeric cell in column %s of row %s detected.",
                i, row);
            vector[i] = ((DoubleValue)cell).getDoubleValue();
        }
        return vector;
    }

}
