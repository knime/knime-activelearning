/*
 * ------------------------------------------------------------------------
 *
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.al.nodes.score.novelty.knfstnoveltyscorer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.knime.al.nodes.score.novelty.knfstlearner.KNFSTPortObject;
import org.knime.al.nodes.score.novelty.knfstlearner.KNFSTPortObjectSpec;
import org.knime.al.util.noveltydetection.knfst.KNFST;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Calculates novelty scores for test data based on a KNFST model from a
 * KNFSTLearner-node
 *
 * @author <a href="mailto:adrian.nembach@gmx.net">Adrian Nembach</a>
 */
public class KNFSTNoveltyScorerNodeModel extends NodeModel
        implements BufferedDataTableHolder {

    static final boolean DEFAULT_APPEND_NOVELTY_SCORE = true;
    static final boolean DEFAULT_APPEND_NULLSPACE_COORDINATES = false;
    static final boolean DEFAULT_NORMALIZE = true;

    /**
     * Helper
     *
     * @return SettingsModel to store img column
     */

    static SettingsModelBoolean createAppendNoveltyScoreModel() {
        return new SettingsModelBoolean("AppendNoveltyScoreModel",
                DEFAULT_APPEND_NOVELTY_SCORE);
    }

    static SettingsModelBoolean createAppendNullspaceCoordinates() {
        return new SettingsModelBoolean("AppendNullspaceCoordinatesModel",
                DEFAULT_APPEND_NULLSPACE_COORDINATES);
    }

    static SettingsModelBoolean createNormalizeModel() {
        return new SettingsModelBoolean("NormalizeModel", DEFAULT_NORMALIZE);
    }

    DataTableSpec m_inTableSpec;

    /* SettingsModels */

    private final SettingsModelBoolean m_appendNoveltyScore =
            createAppendNoveltyScoreModel();
    private final SettingsModelBoolean m_appendNullspaceCoordinates =
            createAppendNullspaceCoordinates();
    private final SettingsModelBoolean m_normalize = createNormalizeModel();

    /* Resulting BufferedDataTable */
    private BufferedDataTable m_data;

    /**
     * Constructor KNFSTNoveltyScorerNodeModel
     */
    public KNFSTNoveltyScorerNodeModel() {
        super(new PortType[] { KNFSTPortObject.TYPE, BufferedDataTable.TYPE },
                new PortType[] { BufferedDataTable.TYPE });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {

        m_inTableSpec = (DataTableSpec) inSpecs[1];
        final KNFSTPortObjectSpec knfstSpec = (KNFSTPortObjectSpec) inSpecs[0];
        final List<String> compatibleFeatures =
                knfstSpec.getCompatibleFeatures();

        for (final String feature : compatibleFeatures) {
            if (!m_inTableSpec.containsName(feature)) {
                throw new InvalidSettingsException(
                        "The input table does not contain the necessary columns needed by the KNFST model.");
            }
        }

        final boolean appendNoveltyScore =
                m_appendNoveltyScore.getBooleanValue();
        final boolean appendNullspaceCoordinates =
                m_appendNullspaceCoordinates.getBooleanValue();

        if (appendNullspaceCoordinates) {
            return new DataTableSpec[] { null };
        } else if (appendNoveltyScore) {
            return createOutSpec((DataTableSpec) inSpecs[1]);
        } else {
            throw new InvalidSettingsException("Select at least one option.");
        }

    }

    private DataTableSpec[] createOutSpec(final DataTableSpec inTableSpec) {

        final DataColumnSpec scoreSpec =
                new DataColumnSpecCreator("Novelty Score", DoubleCell.TYPE)
                        .createSpec();

        return new DataTableSpec[] {
                new DataTableSpec(inTableSpec, new DataTableSpec(scoreSpec)) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({})
    protected BufferedDataTable[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {

        final KNFST knfst = ((KNFSTPortObject) inData[0]).getKNFST();
        final BufferedDataTable data = (BufferedDataTable) inData[1];
        final DataTableSpec tableSpec = data.getDataTableSpec();

        if (data.size() == 0) {
            setWarningMessage("Node created an empty table");
            return new BufferedDataTable[] { data };
        }

        final KNFSTPortObjectSpec knfstSpec =
                ((KNFSTPortObject) inData[0]).getSpec();
        final List<String> includedFeatures = knfstSpec.getCompatibleFeatures();

        // Only use columns that are needed for testing
        final ColumnRearranger cr = new ColumnRearranger(tableSpec);
        cr.keepOnly(
                includedFeatures.toArray(new String[includedFeatures.size()]));
        final BufferedDataTable testData =
                exec.createColumnRearrangeTable(data, cr, exec);

        final boolean appendNoveltyScore =
                m_appendNoveltyScore.getBooleanValue();
        final boolean appendNullspaceCoordinates =
                m_appendNullspaceCoordinates.getBooleanValue();
        final double normalizer = m_normalize.getBooleanValue()
                ? getMin(knfst.getBetweenClassDistances()) : 1.0;

        final ArrayList<DataColumnSpec> outColSpecs =
                new ArrayList<DataColumnSpec>();

        if (appendNoveltyScore) {
            outColSpecs.add(
                    new DataColumnSpecCreator("Novelty Score", DoubleCell.TYPE)
                            .createSpec());
        }
        if (appendNullspaceCoordinates) {
            for (int i = 0; i < knfst.getNullspaceDimension(); i++) {
                outColSpecs.add(
                        new DataColumnSpecCreator("Dim " + i, DoubleCell.TYPE)
                                .createSpec());
            }
        }

        if (outColSpecs.isEmpty()) {
            throw new InvalidSettingsException(
                    "At least one option must be selected.");
        }

        final ColumnRearranger outColRearranger =
                new ColumnRearranger(testData.getDataTableSpec());
        outColRearranger
                .append(new KNFSTNoveltyScorerCellFactory(
                        outColSpecs.toArray(
                                new DataColumnSpec[outColSpecs.size()]),
                        knfst, appendNoveltyScore, appendNullspaceCoordinates,
                        normalizer));

        final BufferedDataTable result = exec.createColumnRearrangeTable(
                testData, outColRearranger, exec.createSubProgress(0.8));

        final ColumnRearranger resRearranger =
                new ColumnRearranger(result.getSpec());
        final String[] colNames = new String[outColSpecs.size()];
        for (int i = 0; i < colNames.length; i++) {
            colNames[i] = outColSpecs.get(i).getName();
        }
        resRearranger.keepOnly(colNames);
        m_data = exec.createJoinedTable(data,
                exec.createColumnRearrangeTable(result, resRearranger,
                        exec.createSubProgress(0)),
                exec.createSubProgress(0.1));

        return new BufferedDataTable[] { m_data };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[] { m_data };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendNoveltyScore.loadSettingsFrom(settings);
        m_appendNullspaceCoordinates.loadSettingsFrom(settings);
        m_normalize.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_appendNoveltyScore.saveSettingsTo(settings);
        m_appendNullspaceCoordinates.saveSettingsTo(settings);
        m_normalize.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_appendNoveltyScore.validateSettings(settings);
        m_appendNullspaceCoordinates.validateSettings(settings);
        m_normalize.validateSettings(settings);
    }

    /******************
     * Private helper methods
     *************************************************/

    private static double getMin(final double[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException(
                    "Array must contain at least one element!");
        }

        double min = array[0];

        for (final double d : array) {
            if (d < min) {
                min = d;
            }
        }
        return min;
    }
}
