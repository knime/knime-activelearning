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
 *   Jul 18, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.labelmodel;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * Node model for the Label Model node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class LabelModelNodeModel extends NodeModel {

    static final int LABEL_SOURCES_PORT = 0;

    private final LabelModelSettings m_settings = new LabelModelSettings();

    /**
     * The constructor used by the node factory.
     */
    protected LabelModelNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec labelSourceSpec = inSpecs[LABEL_SOURCES_PORT];
        CheckUtils.checkSetting(
            labelSourceSpec.stream().filter(c -> c.getType().isCompatible(NominalValue.class)).count() > 1,
            "The first input table must contain at least two nominal columns.");
        try (LabelModel helper = new LabelModel(m_settings, labelSourceSpec)) {

            // TODO uncomment once correlated columns are fully supported
            //            final DataTableSpec correlationSpec = inSpecs[1];
            //            if (correlationSpec != null) {
            //                CheckUtils.checkSetting(
            //                    correlationSpec.stream().filter(c -> c.getType().isCompatible(StringValue.class)).count() > 1,
            //                    "At least two string columns must be contained in the correlation table");
            //            }
            helper.getWarnings().forEach(this::setWarningMessage);

            return new DataTableSpec[]{helper.createProbabilisticLabelRearranger(labelSourceSpec, null).createSpec(),
                helper.createStatisticsSpec()};
        } catch (InvalidSettingsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSettingsException("Unexpected configuration exception.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final BufferedDataTable noisyLabelTable = inData[LABEL_SOURCES_PORT];
        final DataTableSpec sourcesSpec = noisyLabelTable.getDataTableSpec();
        try (final LabelModel helper = new LabelModel(m_settings, sourcesSpec)) {
            // TODO uncomment once correlated sources are supported
            //        final BufferedDataTable correlationsTable = inData[1];
            //            if (correlationsTable != null) {
            //                helper.readCorrelationsTable(correlationsTable);
            //            }
            helper.loadSavedModel();
            final float[][] probabilisticLabels = helper.train(noisyLabelTable, exec.createSubProgress(0.9));
            final BufferedDataTable statisticsTable = helper.createStatisticsTable(exec);
            helper.getWarnings().forEach(this::setWarningMessage);
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(noisyLabelTable,
                helper.createProbabilisticLabelRearranger(sourcesSpec, probabilisticLabels),
                exec.createSilentSubProgress(0)), statisticsTable};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

}
