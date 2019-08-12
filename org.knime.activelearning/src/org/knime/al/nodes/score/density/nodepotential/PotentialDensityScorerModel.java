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
 *   Aug 5, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density.nodepotential;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.al.nodes.score.density.AbstractDensityScorerModel;
import org.knime.al.nodes.score.density.UnknownRowException;
import org.knime.core.node.util.CheckUtils;

/**
 * Model for the potential based density measure used in
 * http://www.uni-konstanz.de/bioml/bioml2/publications/Papers2009/CeBe09.pdf.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class PotentialDensityScorerModel extends AbstractDensityScorerModel<PotentialDataPoint> {

    private static final long serialVersionUID = 8548979519353936648L;

    private double[][] m_squaredDistances;

    private double m_beta;

    private int[] m_idxOfFirstLargerNeighbor;

    PotentialDensityScorerModel(final List<PotentialDataPoint> dataPoints, final double beta) {
        super(dataPoints);
        m_idxOfFirstLargerNeighbor = createIdxOfFirstLargerNeighborsArray();
        m_squaredDistances = createSquaredDistances(dataPoints);
        m_beta = beta;
    }

    public PotentialDensityScorerModel() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(m_squaredDistances);
        out.writeObject(m_idxOfFirstLargerNeighbor);
        out.writeDouble(m_beta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        m_squaredDistances = (double[][])in.readObject();
        m_idxOfFirstLargerNeighbor = (int[])in.readObject();
        m_beta = in.readDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double calculateDecrementWeight(final int idx, final int neighborIdx) {
        final double squaredDistance = getSquaredDistance(idx, neighborIdx);
        return Math.exp(-m_beta * squaredDistance);
    }

    private int[] createIdxOfFirstLargerNeighborsArray() {
        return IntStream.range(0, getNrRows()).map(this::findFirstLargerNeighbor).toArray();
    }

    private int findFirstLargerNeighbor(final int idx) {
        final int insertionPoint = Arrays.binarySearch(getNeighbors(idx), idx);
        if (insertionPoint < 0) {
            return -(insertionPoint + 1);
        } else {
            throw new IllegalStateException(String.format("The data point at idx %s has itself as neighbor.", idx));
        }
    }

    private double[][] createSquaredDistances(final List<PotentialDataPoint> dataPoints) {
        return IntStream.range(0, dataPoints.size()).mapToObj(i -> createSquaredDistances(i, dataPoints.get(i)))
            .toArray(double[][]::new);
    }

    private double[] createSquaredDistances(final int idx, final PotentialDataPoint dataPoint) {
        final List<Double> squaredDistances = dataPoint.getSquaredDistances();
        final int firstLarger = m_idxOfFirstLargerNeighbor[idx];
        List<PotentialDataPoint> neighbors = dataPoint.getNeighbors();
        return IntStream.range(0, neighbors.size()).boxed().sorted((i, j) -> compareNeighborIndices(i, j, neighbors))
            .skip(firstLarger).mapToDouble(squaredDistances::get).toArray();
    }

    private int compareNeighborIndices(final int i, final int j, final List<PotentialDataPoint> neighbors) {
        try {
            return Integer.compare(getIdx(neighbors.get(i).getKey()), getIdx(neighbors.get(j).getKey()));
        } catch (UnknownRowException ex) {
            // not reachable during execution
            throw new IllegalStateException("Unknown neighbor detected.", ex);
        }
    }

    private double getSquaredDistance(final int idx1, final int idx2) {
        CheckUtils.checkArgument(idx1 != idx2, "A data point can't be its own neighbor.");
        if (idx1 < idx2) {
            return getSquaredDistanceOrdered(idx1, idx2);
        } else {
            return getSquaredDistanceOrdered(idx2, idx1);
        }
    }

    private double getSquaredDistanceOrdered(final int smaller, final int larger) {
        final int distIdx = findDistanceIdx(smaller, larger);
        return m_squaredDistances[smaller][distIdx];
    }

    private int findDistanceIdx(final int smaller, final int larger) {
        final int[] neighborsOfSmaller = getNeighbors(smaller);
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

}
