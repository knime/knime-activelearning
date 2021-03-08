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
package org.knime.al.nodes.legacy.score.density.nodepotential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.legacy.loop.ActiveLearnLoopUtils;
import org.knime.al.util.NodeTools;
import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.UniqueNameGenerator;

/**
 * @author dietzc, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
final class NodePotentialScorerNodeModel extends AbstractALNodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodePotentialScorerNodeModel.class);

    private static final int UNLABELED_PORT = 0;

    private static final int NEWLY_LABELED_PORT = 1;

    private static final double FACTOR_RB = 1.25d;

    private final SettingsModelDouble m_radiusAlphaModel = createConstRAlphaModel();

    private final SettingsModelColumnFilter2 m_filterModel = createColumnFilterModel();

    private final SettingsModelString m_outputColumnName = createOutputColumnNameModel();

    private Map<RowKey, NodePotentialDataPoint> m_dataPoints;

    private KDTree<NodePotentialDataPoint> m_dataPointsKDTree;

    private double m_beta;

    private double m_alpha;

    private ColumnRearranger m_resSpec;



    /**
     * @return Settings model to store the value of the constant R Alpha.
     */
    static SettingsModelDouble createConstRAlphaModel() {
        return new SettingsModelDoubleBounded("const_r_alpha", 0.4, 0, 100);
    }

    /**
     * @return Settings model to store the column filter settings.
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("filter_string_model", DoubleValue.class);
    }

    static SettingsModelString createOutputColumnNameModel() {
        return new SettingsModelString("output_column_name", "Potential");
    }

    /**
     * InPort 0 = All unlabeled.
     *
     * InPort 1 = Newly labeled
     *
     * OutPort 1 = Scored
     */
    public NodePotentialScorerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL},
            new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        if (NodeTools.collectAllColumnIndicesOfType(DoubleValue.class, inSpecs[UNLABELED_PORT]).isEmpty()) {
            throw new InvalidSettingsException("No Double columns avaiable!");
        }

        final double radiusAlpha = m_radiusAlphaModel.getDoubleValue();
        final double radiusBeta = radiusAlpha * FACTOR_RB;
        m_alpha = 4.0d / (radiusAlpha * radiusAlpha);
        m_beta = 4.0d / (radiusBeta * radiusBeta);

        m_resSpec = createResRearranger(inSpecs[UNLABELED_PORT]);

        return new DataTableSpec[]{m_resSpec.createSpec()};
    }

    private ColumnRearranger createResRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(inSpec);
        final DataColumnSpec newColumn = nameGen.newColumn(m_outputColumnName.getStringValue(), DoubleCell.TYPE);
        rearranger.append(new SingleCellFactory(newColumn) {

            @Override
            public DataCell getCell(final DataRow row) {
                return new DoubleCell(m_dataPoints.get(row.getKey()).getPotential());
            }
        });

        return rearranger;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        // Check whether active learning loop first iteration
        int step = 0;
        FlowVariable flow;
        if ((flow = getAvailableFlowVariables().get(ActiveLearnLoopUtils.AL_STEP)) != null) {
            step = flow.getIntValue();
        }

        final BufferedDataTable unlabeled = inData[UNLABELED_PORT];
        // Init data structures and potentials
        if (step == 0) {
            initStructures(exec, unlabeled);
            initPotentials();
        }

        // if optional second in-port is connected
        final BufferedDataTable newlyLabeled = inData[NEWLY_LABELED_PORT];
        if (newlyLabeled != null) {
            updatePotentials(exec, newlyLabeled);
        }

        final BufferedDataTable resTable = exec.createColumnRearrangeTable(unlabeled, m_resSpec, exec);

        return new BufferedDataTable[]{resTable};
    }

    private void initStructures(final ExecutionContext exec, final BufferedDataTable unlabeled)
        throws CanceledExecutionException {
        m_dataPoints = new HashMap<>();

        exec.setProgress("Init data structures and density");

        final List<Integer> doubleIndicies = NodeTools.getIndicesFromFilter(unlabeled.getSpec(), m_filterModel,
            DoubleValue.class, NodePotentialScorerNodeModel.class);

        final KDTreeBuilder<NodePotentialDataPoint> allDataPointsBuilder = new KDTreeBuilder<>(doubleIndicies.size());
        try {
            for (final DataRow row : unlabeled) {
                final NodePotentialDataPoint p =
                    new NodePotentialDataPoint(NodeTools.toDoubleArray(row, doubleIndicies));

                allDataPointsBuilder.addPattern(p.getVector(), p);
                m_dataPoints.put(row.getKey(), p);
            }
        } catch (final ClassCastException e) {
            LOGGER.error("Missing values are not allowed!", e);
            CanceledExecutionException ex = new CanceledExecutionException("Missing values are not allowed!");
            ex.initCause(e);
            throw ex;
        }
        m_dataPointsKDTree = allDataPointsBuilder.buildTree();
    }

    private void initPotentials() {
        for (final NodePotentialDataPoint p : m_dataPoints.values()) {
            final List<NearestNeighbour<NodePotentialDataPoint>> maxDistanceNeighbours =
                m_dataPointsKDTree.getMaxDistanceNeighbours(p.getVector(), m_radiusAlphaModel.getDoubleValue());
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

    private void updatePotentials(final ExecutionContext exec, final BufferedDataTable newlyLabeled)
        throws CanceledExecutionException {
        exec.setProgress("Decrease estimated density ...");
        // Resolve query from last iteration and decrease potentials
        final long rowCount = newlyLabeled.size();
        int rowIdx = 0;
        for (final DataRow row : newlyLabeled) {
            final NodePotentialDataPoint dataPoint = m_dataPoints.get(row.getKey());

            // decrease potentials
            if (dataPoint == null) {
                continue;
            }
            final double potential = dataPoint.getPotential();
            for (final NearestNeighbour<NodePotentialDataPoint> p : m_dataPointsKDTree
                .getMaxDistanceNeighbours(dataPoint.getVector(), m_radiusAlphaModel.getDoubleValue() * FACTOR_RB)) {
                decreasePotential(potential, p);
            }
            exec.checkCanceled();
            exec.setProgress((double)rowIdx / rowCount);
            rowIdx++;
        }
    }

    private void decreasePotential(final double potential, final NearestNeighbour<NodePotentialDataPoint> p) {
        final double distance = p.getDistance();
        p.getData().decreasePotential(potential * Math.exp(-m_beta * distance * distance));
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>();
            m_settingsModels.add(m_filterModel);
            m_settingsModels.add(m_radiusAlphaModel);
            m_settingsModels.add(m_outputColumnName);
        }
        return m_settingsModels;
    }

}
