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
 *   Sep 27, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density.nodepotential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.al.nodes.score.density.AbstractNeighborhoodModel;
import org.knime.al.nodes.score.density.KeyMap;
import org.knime.al.nodes.score.density.NeighborhoodStructure;
import org.knime.al.nodes.score.density.ProcessingUtil;
import org.knime.al.nodes.score.density.UnknownRowException;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;

/**
 * Model for the potential based density measure used in
 * http://www.uni-konstanz.de/bioml/bioml2/publications/Papers2009/CeBe09.pdf.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PotentialNeighborhoodModel extends AbstractNeighborhoodModel {

    private double m_beta;

    /**
     * Stores for each data point the neighborhood-index of the first data point with a larger index. The neighborhoods
     * are stored in {@link AbstractDensityScorerModel} as arrays of indices in ascending order. They are accessible via
     * the getNeighbors(index).
     */
    private int[] m_idxOfFirstLargerNeighbor;

    /**
     * Stores the squared distances for all neighboring data points. In order to save memory, each data point only
     * stores the distances of neighbors with a larger index than itself. Example: For neighboring data points with
     * indices i, j and i < j m_squaredDistances[i] contains the distance between i and j, while m_squaredDistances[j]
     * does not. The distances are stored in ascending order of the neighboring indices.
     */
    private double[][] m_squaredDistances;

    private PotentialNeighborhoodModel(final KeyMap keyMap, final NeighborhoodStructure neighborhoods,
        final double[][] squaredDistances, final int[] idxOfFirstLarger, final double beta) {
        super(keyMap, neighborhoods);
        m_squaredDistances = squaredDistances;
        m_idxOfFirstLargerNeighbor = idxOfFirstLarger;
        m_beta = beta;
    }

    /**
     * Framework constructor for serialization.
     *
     * @noreference Don't reference in client code
     */
    public PotentialNeighborhoodModel() {
    }

    static PotentialNeighborhoodModel create(final KeyMap keyMap, final NeighborhoodStructure neighborhoods,
        final double beta, final Collection<PotentialDataPoint> dataPoints, final ExecutionMonitor monitor)
        throws CanceledExecutionException {
        return new Creator(keyMap, neighborhoods, beta).create(dataPoints, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        m_idxOfFirstLargerNeighbor = (int[])in.readObject();
        m_squaredDistances = (double[][])in.readObject();
        m_beta = in.readDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(m_idxOfFirstLargerNeighbor);
        out.writeObject(m_squaredDistances);
        out.writeDouble(m_beta);
    }

    @Override
    protected double calculateDecrementWeight(final int idx, final int neighborIdx) {
        final double squaredDistance = getSquaredDistance(idx, neighborIdx);
        return Math.exp(-m_beta * squaredDistance);
    }

    private double getSquaredDistance(final int current, final int idxInNeighborhood) {
        final int neighbor = getNeighborhoods().getNeighborhood(current)[idxInNeighborhood];
        CheckUtils.checkState(current != neighbor, "A data point can't be its own neighbor.");
        if (current < neighbor) {
            // the distance is stored by current
            return m_squaredDistances[current][idxInNeighborhood - m_idxOfFirstLargerNeighbor[current]];
        } else {
            // the distance is stored by the neighbor
            return getSquaredDistanceOrdered(neighbor, current);
        }
    }

    private double getSquaredDistanceOrdered(final int smaller, final int larger) {
        final int distIdx = findDistanceIdx(smaller, larger);
        return m_squaredDistances[smaller][distIdx];
    }

    private int findDistanceIdx(final int smaller, final int larger) {
        final int[] neighborsOfSmaller = getNeighborhoods().getNeighborhood(smaller);
        // here we trade off runtime for memory because we avoid storing each distance twice
        final int firstLargerNeighbor = m_idxOfFirstLargerNeighbor[smaller];
        CheckUtils.checkState(firstLargerNeighbor > -1,
            "The data point at index %s has no neighbors with larger index.", smaller);
        final int distIdx =
            Arrays.binarySearch(neighborsOfSmaller, firstLargerNeighbor, neighborsOfSmaller.length, larger)
                - firstLargerNeighbor;
        CheckUtils.checkState(distIdx >= 0 && distIdx < neighborsOfSmaller.length,
            "The data point at index %s is not a neighbor of the datapoint at index %s.", larger, smaller);
        return distIdx;
    }

    /**
     * Creator for PotentialNeighborhood objects.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static final class Creator {
        private final KeyMap m_keyMap;

        private final NeighborhoodStructure m_neighborhoods;

        private int[] m_idxOfFirstLarger;

        private final double m_beta;

        public Creator(final KeyMap keyMap, final NeighborhoodStructure neighborhoods, final double beta) {
            m_keyMap = keyMap;
            m_neighborhoods = neighborhoods;
            m_beta = beta;
        }

        PotentialNeighborhoodModel create(final Collection<PotentialDataPoint> dataPoints,
            final ExecutionMonitor monitor) throws CanceledExecutionException {
            m_idxOfFirstLarger = createIdxOfFirstLargerNeighborsArray(dataPoints, monitor.createSubProgress(0.1));
            final double[][] squaredDistances = createSquaredDistances(dataPoints, monitor.createSubProgress(0.9));
            return new PotentialNeighborhoodModel(m_keyMap, m_neighborhoods, squaredDistances, m_idxOfFirstLarger,
                m_beta);
        }

        private int[] createIdxOfFirstLargerNeighborsArray(final Collection<PotentialDataPoint> dataPoints,
            final ExecutionMonitor monitor) throws CanceledExecutionException {
            return ProcessingUtil.toArrayWithProgress(dataPoints,
                (i, p) -> findFirstLargerNeighbor(i, m_neighborhoods.getNeighborhood(i)),
                ProcessingUtil.progressWithTemplate(monitor, "Searching first larger neighbor for row %s of %s."));
        }

        private static int findFirstLargerNeighbor(final int idx, final int[] neighborhood) {
            final int insertionPoint = Arrays.binarySearch(neighborhood, idx);
            if (insertionPoint < 0) {
                return -(insertionPoint + 1);
            } else {
                throw new IllegalStateException(String.format("The data point at idx %s has itself as neighbor.", idx));
            }
        }

        private double[][] createSquaredDistances(final Collection<PotentialDataPoint> dataPoints,
            final ExecutionMonitor monitor) throws CanceledExecutionException {
            assert m_idxOfFirstLarger != null : "Coding error: First initialize the idxOfFirstLarger array.";
            return ProcessingUtil.toArrayWithProgress(dataPoints, double[][]::new, this::createSquaredDistances,
                ProcessingUtil.progressWithTemplate(monitor, "Creating neighborhood distances for row %s of %s."));
        }

        private double[] createSquaredDistances(final int idx, final PotentialDataPoint dataPoint) {
            final List<Double> squaredDistances = dataPoint.getSquaredDistances();
            final List<PotentialDataPoint> neighbors = dataPoint.getNeighbors();
            final int firstLarger = m_idxOfFirstLarger[idx];
            return IntStream.range(0, neighbors.size()).boxed()
                .sorted((i, j) -> compareNeighborIndices(i, j, neighbors)).skip(firstLarger)
                .mapToDouble(squaredDistances::get).toArray();
        }

        private int compareNeighborIndices(final int i, final int j, final List<PotentialDataPoint> neighbors) {
            try {
                final int idxLeft = m_keyMap.getIndex(neighbors.get(i).getKey());
                final int idxRight = m_keyMap.getIndex(neighbors.get(j).getKey());
                return Integer.compare(idxLeft, idxRight);
            } catch (UnknownRowException ex) {
                throw new IllegalStateException("Unknown row during model creation.", ex);
            }
        }
    }

}
