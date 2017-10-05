/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
package org.knime.al.nodes.score.density.nodepotential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.loop.ActiveLearnLoopUtils;
import org.knime.al.util.NodeTools;
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
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;

/**
 * @author dietzc, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class NodePotentialScorerNodeModel extends AbstractALNodeModel {

    private static final int UNLABELED_PORT = 0;
    private static final int NEWLY_LABELED_PORT = 1;

    private static final double FACTOR_RB = 1.25d;

    private final SettingsModelDouble m_radiusAlphaModel =
            createConstRAlphaModel();

    private final SettingsModelColumnFilter2 m_filterModel =
            createColumnFilterModel();

    private Map<RowKey, NodePotentialDataPoint> m_dataPoints;

    private KDTree<NodePotentialDataPoint> m_dataPointsKDTree;

    private double m_beta, m_alpha;

    private ColumnRearranger m_resSpec;

    /**
     * @return Settings model to store the value of the constant R Alpha.
     */
    protected static SettingsModelDouble createConstRAlphaModel() {
        return new SettingsModelDoubleBounded("const_r_alpha", 0.4, 0, 100);
    }

    /**
     * @return Settings model to store the column filter settings.
     */
    @SuppressWarnings("unchecked")
    protected static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("filter_string_model",
                DoubleValue.class);
    }

    /**
     * InPort 0 = All unlabeled.
     *
     * InPort 1 = Newly labeled
     *
     * OutPort 1 = Scored
     */
    public NodePotentialScorerNodeModel() {
        super(new PortType[] { BufferedDataTable.TYPE,
                BufferedDataTable.TYPE_OPTIONAL },
                new PortType[] { BufferedDataTable.TYPE });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        if (NodeTools.collectAllColumnIndicesOfType(DoubleValue.class,
                inSpecs[UNLABELED_PORT]).isEmpty()) {
            throw new InvalidSettingsException("No Double columns avaiable!");
        }

        m_beta = 4.0d / ((m_radiusAlphaModel.getDoubleValue() * FACTOR_RB)
                * (m_radiusAlphaModel.getDoubleValue() * FACTOR_RB));

        m_alpha = 4.0d / (m_radiusAlphaModel.getDoubleValue()
                * m_radiusAlphaModel.getDoubleValue());

        m_resSpec = createResRearranger(inSpecs[UNLABELED_PORT]);

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
                        new DataColumnSpecCreator("Node Potential Score",
                                DoubleCell.TYPE).createSpec() };
            }

            @Override
            public DataCell[] getCells(final DataRow row) {

                return new DataCell[] { new DoubleCell(
                        m_dataPoints.get(row.getKey()).getPotential()) };
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

        // Check whether active learning loop first iteration
        int step = 0;
        FlowVariable flow;
        if ((flow = getAvailableFlowVariables()
                .get(ActiveLearnLoopUtils.AL_STEP)) != null) {
            step = flow.getIntValue();
        }

        // Init data structures and potentials
        if (step == 0) {
            m_dataPoints = new HashMap<>();

            exec.setProgress("Init data structures and density");

            final List<Integer> doubleIndicies = NodeTools.getIndicesFromFilter(
                    inData[UNLABELED_PORT].getSpec(), m_filterModel,
                    DoubleValue.class, NodePotentialScorerNodeModel.class);

            final KDTreeBuilder<NodePotentialDataPoint> allDataPointsBuilder =
                    new KDTreeBuilder<NodePotentialDataPoint>(
                            doubleIndicies.size());
            try {
                for (final DataRow row : inData[UNLABELED_PORT]) {
                    final NodePotentialDataPoint p = new NodePotentialDataPoint(
                            NodeTools.toDoubleArray(row, doubleIndicies));

                    allDataPointsBuilder.addPattern(p.getVector(), p);
                    m_dataPoints.put(row.getKey(), p);
                }
            } catch (final ClassCastException e) {
                throw new CanceledExecutionException(
                        "Missing values are not allowed!");
            }
            m_dataPointsKDTree = allDataPointsBuilder.buildTree();

            // Calculate initial Potentials
            initPotentials();
        }

        // if optional second in-port is connected
        if (inData[NEWLY_LABELED_PORT] != null) {
            exec.setProgress("Decrease estimated density ...");
            // Resolve query from last iteration and decrease potentials
            final long rowCount = inData[NEWLY_LABELED_PORT].size();
            int rowIdx = 0;
            for (final DataRow row : inData[NEWLY_LABELED_PORT]) {
                final NodePotentialDataPoint dataPoint =
                        m_dataPoints.get(row.getKey());

                // decrease potentials
                if (dataPoint == null) {
                    continue;
                }
                final double potential = dataPoint.getPotential();
                for (final NearestNeighbour<NodePotentialDataPoint> p : m_dataPointsKDTree
                        .getMaxDistanceNeighbours(dataPoint.getVector(),
                                m_radiusAlphaModel.getDoubleValue()
                                        * FACTOR_RB)) {
                    decreasePotential(potential, p);
                }
                exec.checkCanceled();
                exec.setProgress((double) rowIdx++ / rowCount);
            }
        }

        final BufferedDataTable resTable = exec.createColumnRearrangeTable(
                inData[UNLABELED_PORT], m_resSpec, exec);

        return new BufferedDataTable[] { resTable };
    }

    /**
     * @param potential
     * @param p
     */
    private void decreasePotential(final double potential,
            final NearestNeighbour<NodePotentialDataPoint> p) {
        p.getData().decreasePotential(potential
                * Math.exp(-m_beta * p.getDistance() * p.getDistance()));
    }

    /**
     * @param allDataPoint
     * @param allDataPointsTree
     * @throws CanceledExecutionException
     * @throws InterruptedException
     */
    private void initPotentials()
            throws CanceledExecutionException, InterruptedException {

        for (final NodePotentialDataPoint p : m_dataPoints.values()) {
            final List<NearestNeighbour<NodePotentialDataPoint>> maxDistanceNeighbours =
                    m_dataPointsKDTree.getMaxDistanceNeighbours(p.getVector(),
                            m_radiusAlphaModel.getDoubleValue());
            for (final NearestNeighbour<NodePotentialDataPoint> nn : maxDistanceNeighbours) {

                // don't compare to itself
                if (nn.getData() == p) {
                    continue;
                }

                final double dist = nn.getDistance();
                p.increasePotential(Math.exp(dist * dist * -m_alpha));
            }
        }

        // Normalize
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (final NodePotentialDataPoint p : m_dataPoints.values()) {
            min = Math.min(min, p.getPotential());
            max = Math.max(max, p.getPotential());
        }

        for (final NodePotentialDataPoint p : m_dataPoints.values()) {
            p.setPotential((p.getPotential() - min) / (max - min));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>();
            m_settingsModels.add(m_filterModel);
            m_settingsModels.add(m_radiusAlphaModel);
        }
        return m_settingsModels;
    }

}
