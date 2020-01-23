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
 *   Sep 13, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel.learner;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.wsl.weaklabelmodel.WeakLabelModelContent;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObject;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObjectSpec;

/**
 * Node model of the Weak Label Learner node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class WeakLabelModelLearnerNodeModel extends NodeModel {

    /**
     * Input port of the label source data table.
     */
    static final int DATA_PORT = 0;

    private final WeakLabelModelLearnerSettings m_settings = new WeakLabelModelLearnerSettings();

    /**
     */
    protected WeakLabelModelLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{WeakLabelModelPortObject.TYPE, BufferedDataTable.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataSpec = (DataTableSpec)inSpecs[DATA_PORT];
        CheckUtils.checkSetting(dataSpec.stream().filter(WeakLabelModelLearnerNodeModel::isPossibleSource).count() > 1,
            "The input table must contain at least two possible source columns.");
        WeakLabelModelLearnerSettings.checkLabelSources(dataSpec, m_settings.getLabelSourcesFilter());
        final WeakLabelModelLearner learner = new WeakLabelModelLearner(m_settings, dataSpec);
        CheckUtils.checkSetting(learner.getClassNames().size() > 1,
            "The selected label columns contain only a single class.");
        learner.getWarnings().forEach(this::setWarningMessage);
        final WeakLabelModelPortObjectSpec modelSpec = learner.createPortObjectSpec();
        return new PortObjectSpec[]{modelSpec, new StatisticsTableCreator(modelSpec).createSpec()};
    }

    private static boolean isPossibleSource(final DataColumnSpec spec) {
        final DataType type = spec.getType();
        if (type.isCompatible(NominalValue.class)) {
            return spec.getDomain().hasValues();
        }
        return type.isCompatible(NominalDistributionValue.class);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final BufferedDataTable data = (BufferedDataTable)inObjects[DATA_PORT];
        final DataTableSpec dataSpec = data.getDataTableSpec();
        final WeakLabelModelLearner learner = new WeakLabelModelLearner(m_settings, dataSpec);
        final WeakLabelModelContent model = learner.learn(data, exec.createSubProgress(0.95));
        learner.getWarnings().forEach(this::setWarningMessage);
        final WeakLabelModelPortObject po = new WeakLabelModelPortObject(learner.createPortObjectSpec(), model);
        final BufferedDataTable statisticsTable =
            new StatisticsTableCreator(po.getSpec()).createStatisticsTable(model, exec.createSubExecutionContext(0.05));
        return new PortObject[]{po, statisticsTable};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals

    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // no state to reset
    }

}
