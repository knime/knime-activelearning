/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.al.nodes.score.pbac;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.al.nodes.loop.ActiveLearnLoopUtils;
import org.knime.al.util.MathUtils;
import org.knime.al.util.NodeTools;
import org.knime.al.util.NodeUtils;
import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.MutableDouble;

/**
 * @author dietzc, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class PBACScorerNodeModel extends NodeModel {

    private static final double FACTOR_RB = 1.25d;

    private final SettingsModelDouble m_radiusAlphaModel =
            createConstRAlphaModel();
    private final SettingsModelDouble m_exploitationModel =
            createExploitationModel();
    private final SettingsModelInteger m_numNeighborsModel =
            createSMNumNeighbors();
    private final SettingsModelString m_classColModel = createClassColModel();

    private double m_beta, m_alpha, m_exploitation;

    private Map<RowKey, DataPoint> m_dataPoints;
    private KDTree<DataPoint> m_dataPointsKDTree;
    private KDTreeBuilder<DataPoint> m_prototypeTreeBuilder;
    private Set<String> m_labels;

    private ColumnRearranger m_resSpec;
    private int m_classIdx;

    private List<Integer> m_doubleIndices;

    /**
     * @return settings model to store the name of the class col.
     */
    static SettingsModelString createClassColModel() {
        return new SettingsModelString("class_col", "");
    }

    /**
     * @return settings model to store the number of neighbors
     */
    static SettingsModelInteger createSMNumNeighbors() {
        return new SettingsModelInteger("num_neighbors", 5);
    }

    /**
     * @return settings model to store the value of the constant R Alpha.
     */
    static SettingsModelDouble createConstRAlphaModel() {
        return new SettingsModelDoubleBounded("const_r_alpha", 0.4, 0,
                Integer.MAX_VALUE);
    }

    /**
     * @return settings model to store the value of the constant exploitation
     *         factor.
     */
    static SettingsModelDouble createExploitationModel() {
        return new SettingsModelDoubleBounded("const_exploitation", 0.5, 0, 1);
    }

    /**
     * InPort 1 = All unlabeled.
     *
     * InPort 2 = Newly labeled
     */
    public PBACScorerNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // a^2
        final double alphaValue = m_radiusAlphaModel.getDoubleValue();
        m_alpha = 4.0d / (alphaValue * alphaValue);

        // a^2*rb^2
        m_beta = 4.0d / (alphaValue * alphaValue * FACTOR_RB * FACTOR_RB);

        m_exploitation = m_exploitationModel.getDoubleValue();

        m_classIdx = NodeUtils.autoColumnSelection(inSpecs[1], m_classColModel,
                StringValue.class, PBACScorerNodeModel.class);

        m_doubleIndices = NodeTools
                .collectAllColumnIndicesOfType(DoubleValue.class, inSpecs[0]);

        m_resSpec = createResRearranger(inSpecs[0]);

        return new DataTableSpec[] { m_resSpec.createSpec() };
    }

    private ColumnRearranger createResRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        rearranger.append(new CellFactory() {

            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor exec) {
                exec.setProgress((double) curRowNr / rowCount);

            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return new DataColumnSpec[] {
                        new DataColumnSpecCreator("Potential", DoubleCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("Entropy", DoubleCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("Score", DoubleCell.TYPE)
                                .createSpec() };
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                return new DataCell[] {
                        new DoubleCell(
                                m_dataPoints.get(row.getKey()).getPotential()),
                        new DoubleCell(
                                m_dataPoints.get(row.getKey()).getEntropy()),
                        new DoubleCell(
                                m_dataPoints.get(row.getKey()).getScore()) };
            }
        });

        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        final FlowVariable currentIteration =
                getAvailableFlowVariables().get(ActiveLearnLoopUtils.AL_STEP);

        m_doubleIndices.size();

        // Init data structures and potentials
        if ((currentIteration == null)
                || (currentIteration.getIntValue() == 0)) {
            exec.setProgress("Init data structures and potentials");

            m_prototypeTreeBuilder =
                    new KDTreeBuilder<DataPoint>(m_doubleIndices.size());
            m_dataPoints = new HashMap<RowKey, DataPoint>();
            final KDTreeBuilder<DataPoint> allDataPointsBuilder =
                    new KDTreeBuilder<DataPoint>(m_doubleIndices.size());

            for (final DataRow row : inData[0]) {
                final DataPoint p = new DataPoint(row.getKey(),
                        NodeTools.toDoubleArray(row, m_doubleIndices));

                allDataPointsBuilder.addPattern(p.getVector(), p);
                m_dataPoints.put(row.getKey(), p);
            }

            // also add inital stuff
            for (final DataRow row : inData[1]) {

                // skip cells with missing label.
                if (row.getCell(m_classIdx).isMissing()) {
                    continue;
                }

                final DataPoint p = new DataPoint(row.getKey(),
                        NodeTools.toDoubleArray(row, m_doubleIndices));

                allDataPointsBuilder.addPattern(p.getVector(), p);
                m_dataPoints.put(row.getKey(), p);
            }

            // Calculate initial Potentials
            m_dataPointsKDTree = allDataPointsBuilder.buildTree();
            initPotentials();

            // initialise empty
            m_labels = new HashSet<String>();
        }

        exec.setProgress("Calculate utility score ...");

        // // Resolve query from last iteration and decrease potentials
        final long rowCount = inData[1].size();
        int rowIdx = 0;
        for (final DataRow row : inData[1]) {

            // skip missing or padded classes
            if (row.getCell(m_classIdx).isMissing()) {
                continue;
            }

            final DataPoint dataPoint = m_dataPoints.get(row.getKey());
            dataPoint.setLabel(
                    ((StringValue) row.getCell(m_classIdx)).getStringValue());

            m_labels.add(dataPoint.getLabel());

            // Add selected example to neighborhood
            m_prototypeTreeBuilder.addPattern(dataPoint.getVector(), dataPoint);

            final double pointPotential = dataPoint.getPotential();
            final double maxDist =
                    m_radiusAlphaModel.getDoubleValue() * FACTOR_RB;
            for (final NearestNeighbour<DataPoint> p : m_dataPointsKDTree
                    .getMaxDistanceNeighbours(dataPoint.getVector(), maxDist)) {
                decreasePotential(pointPotential, p);
            }
            exec.checkCanceled();
            exec.setProgress(rowIdx++ / rowCount);
        }

        // Querying next samples
        final KDTree<DataPoint> prototypeTree =
                m_prototypeTreeBuilder.buildTree();

        m_dataPoints.forEach((key, value) -> {
            value.setEntropy(
                    calculateEntropy(value, m_labels.size(), prototypeTree));
            value.setScore(calculateScore(value));
        });

        final BufferedDataTable resTable =
                exec.createColumnRearrangeTable(inData[0], m_resSpec, exec);

        return new BufferedDataTable[] { resTable };
    }

    private double calculateScore(final DataPoint point) {
        return (point.getPotential() * (1 - m_exploitation))
                + (m_exploitation * point.getEntropy());
    }

    /**
     * @param allDataPoint
     * @param allDataPointsTree
     * @throws CanceledExecutionException
     * @throws InterruptedException
     */
    private void initPotentials()
            throws CanceledExecutionException, InterruptedException {

        // Initializing potentials
        double maxPotential = -Double.MAX_VALUE;
        double minPotential = Double.MAX_VALUE;

        final double alphavalue = m_radiusAlphaModel.getDoubleValue();

        for (final DataPoint p : m_dataPoints.values()) {
            final List<NearestNeighbour<DataPoint>> maxDistanceNeighbours =
                    m_dataPointsKDTree.getMaxDistanceNeighbours(p.getVector(),
                            alphavalue);

            for (final NearestNeighbour<DataPoint> nn : maxDistanceNeighbours) {

                // don't compare to itself
                if (nn.getData() == p) {
                    continue;
                }

                final double dist = nn.getDistance();
                p.increasePotential(Math.exp(dist * dist * -m_alpha));
            }

        }

        // min/max potential
        for (final DataPoint p : m_dataPoints.values()) {
            maxPotential = Math.max(maxPotential, p.getPotential());
            minPotential = Math.min(minPotential, p.getPotential());
        }

        final double factor = 1.0d / (maxPotential - minPotential);
        for (final DataPoint p : m_dataPoints.values()) {
            final double potential = p.getPotential();
            p.setPotential((potential - minPotential) * factor);
        }
    }

    /**
     * @param potential
     * @param point
     */
    private void decreasePotential(final double potential,
            final NearestNeighbour<DataPoint> point) {
        point.getData().decreasePotential(potential * Math
                .exp(-m_beta * point.getDistance() * point.getDistance()));
    }

    private double calculateEntropy(final DataPoint point, final int nrclasses,
            final KDTree<DataPoint> prototypeTree) {

        HashMap<String, MutableDouble> classWeights =
                new HashMap<String, MutableDouble>();

        // return potential as score if we have not discovered two classes yet
        if (nrclasses < 2) {
            return point.getPotential();
        }

        // compute rankings for all rows
        classWeights = new HashMap<String, MutableDouble>();
        final List<NearestNeighbour<DataPoint>> nearestn =
                prototypeTree.getKNearestNeighbours(point.getVector(),
                        Math.min(m_numNeighborsModel.getIntValue(),
                                prototypeTree.size()));
        double totalweight = 0;
        for (final NearestNeighbour<DataPoint> n : nearestn) {
            MutableDouble count = classWeights.get(n.getData().getLabel());
            if (count == null) {
                count = new MutableDouble(0);
                classWeights.put(n.getData().getLabel(), count);
            }

            // TODO: Different implementation as in paper: we use only
            // 1/distance, not 1/distance² to be consistent with KNIME KNN
            final double distance = n.getDistance();
            count.add(1.0d / distance);
            totalweight += (1.0d / distance);
        }

        double entropy = 0;
        for (final Map.Entry<String, MutableDouble> e : classWeights
                .entrySet()) {
            final double weight = e.getValue().doubleValue();
            entropy += (weight / totalweight)
                    * MathUtils.log2(weight / totalweight);
        }

        entropy /= MathUtils.log2(nrclasses);

        if (Double.isNaN(entropy)) {
            entropy = 0;
        }
        return -entropy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_radiusAlphaModel.saveSettingsTo(settings);
        m_exploitationModel.saveSettingsTo(settings);
        m_numNeighborsModel.saveSettingsTo(settings);
        m_classColModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_radiusAlphaModel.validateSettings(settings);
        m_exploitationModel.validateSettings(settings);
        m_numNeighborsModel.validateSettings(settings);
        m_classColModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_radiusAlphaModel.loadSettingsFrom(settings);
        m_exploitationModel.loadSettingsFrom(settings);
        m_numNeighborsModel.loadSettingsFrom(settings);
        m_classColModel.loadSettingsFrom(settings);
    }

}
