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
 * Created on 11.03.2013 by dietyc
 */
package org.knime.al.nodes.score.density.graphdensity;

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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;

/**
 * @author dietzc, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class GraphDensityScorerNodeModel extends AbstractALNodeModel {

    private static final int UNLABELED_PORT = 0;
    private static final int NEWLY_LABELED_PORT = 1;

    private final SettingsModelInteger m_numNeighbors =
            createNumNeighborsModel();
    private final SettingsModelDouble m_sigmaModel = createSigmaModel();
    private final SettingsModelColumnFilter2 m_columnFilterModel =
            createColumnFilterModel();

    private ColumnRearranger m_resRearranger;

    private Map<RowKey, GraphDensityDataPoint> m_dataPoints;
    private KDTree<GraphDensityDataPoint> m_dataPointsKDTree;

    /**
     * @return Settings model for the number of Neighbors
     */
    protected static SettingsModelInteger createNumNeighborsModel() {
        return new SettingsModelInteger("numNeighbors", 5);
    }

    /**
     * @return Settings model for the Sigma
     */
    protected static SettingsModelDouble createSigmaModel() {
        return new SettingsModelDouble("sigmaModel", 1);
    }

    /**
     * @return Settings model for the column filter
     */
    @SuppressWarnings("unchecked")
    protected static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("collumn_filter_model",
                DoubleValue.class);
    }

    /**
     * Constructor.
     */
    public GraphDensityScorerNodeModel() {
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
            throw new InvalidSettingsException("No valid columns avaiable!");
        }
        m_resRearranger = createResRearranger(inSpecs[UNLABELED_PORT]);

        return new DataTableSpec[] { m_resRearranger.createSpec() };
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
                        new DataColumnSpecCreator("Graph Density Score",
                                DoubleCell.TYPE).createSpec() };
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                return new DataCell[] { new DoubleCell(
                        m_dataPoints.get(row.getKey()).getDensity()) };
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

        // Check if this is the first iteration
        int step = 0;
        FlowVariable flow;
        if ((flow = getAvailableFlowVariables()
                .get(ActiveLearnLoopUtils.AL_STEP)) != null) {
            step = flow.getIntValue();
        }

        // Init data structures and potentials in the first iteration
        if (step == 0) {
            m_dataPoints = new HashMap<>();
            exec.setProgress("Init data structures and density");

            final List<Integer> selectedIndicies = NodeTools
                    .getIndicesFromFilter(inData[UNLABELED_PORT].getSpec(),
                            m_columnFilterModel, DoubleValue.class,
                            this.getClass());
            if (selectedIndicies.isEmpty()) {
                throw new InvalidSettingsException("No Columns selected!");
            }

            final KDTreeBuilder<GraphDensityDataPoint> allDataPointsBuilder =
                    new KDTreeBuilder<>(selectedIndicies.size());

            for (final DataRow row : inData[UNLABELED_PORT]) {
                final GraphDensityDataPoint p = new GraphDensityDataPoint(
                        NodeTools.toDoubleArray(row, selectedIndicies));

                allDataPointsBuilder.addPattern(p.getVector(), p);
                m_dataPoints.put(row.getKey(), p);
            }
            m_dataPointsKDTree = allDataPointsBuilder.buildTree();

            // Calculate initial Potentials
            initPotentials();
        }

        // decrease potential of nodes if optional second in-port is connected
        if (inData[NEWLY_LABELED_PORT] != null) {

            exec.setProgress("Decrease estimated density ...");
            final long rowCount = inData[NEWLY_LABELED_PORT].size();
            int rowIdx = 0;
            for (final DataRow row : inData[UNLABELED_PORT]) {

                final GraphDensityDataPoint graphDensityDataPoint =
                        m_dataPoints.get(row.getKey());

                graphDensityDataPoint.decreaseDensityOfNeighbors(1);

                exec.checkCanceled();
                exec.setProgress((double) rowIdx++ / rowCount);
            }
        }

        final BufferedDataTable resTable = exec.createColumnRearrangeTable(
                inData[UNLABELED_PORT], m_resRearranger, exec);

        return new BufferedDataTable[] { resTable };
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
        final double sigma = m_sigmaModel.getDoubleValue();

        for (final GraphDensityDataPoint p : m_dataPoints.values()) {
            final List<NearestNeighbour<GraphDensityDataPoint>> maxDistanceNeighbours =
                    m_dataPointsKDTree.getKNearestNeighbours(p.getVector(),
                            m_numNeighbors.getIntValue());
            for (final NearestNeighbour<GraphDensityDataPoint> nn : maxDistanceNeighbours) {

                p.registerNeighbor(nn.getData());

                // don't compare to itself
                if (nn.getData() == p) {
                    continue;
                }

                final double dist = nn.getDistance();
                nn.getData().increaseDensity(Math.exp(-dist / sigma));
                nn.getData().increaseNeighbourCounter();
            }
        }

        // Normalize
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (final GraphDensityDataPoint p : m_dataPoints.values()) {
            p.normalizeDensity();
            min = Math.min(min, p.getDensity());
            max = Math.max(max, p.getDensity());
        }

        for (final GraphDensityDataPoint p : m_dataPoints.values()) {
            p.setDensity((p.getDensity() - min) / (max - min));
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
            m_settingsModels.add(m_columnFilterModel);
            m_settingsModels.add(m_numNeighbors);
            m_settingsModels.add(m_sigmaModel);
        }
        return m_settingsModels;
    }
}
