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
package org.knime.wsl.weaklabelmodel.predictor;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObject;
import org.knime.wsl.weaklabelmodel.WeakLabelModelPortObjectSpec;

/**
 * Node model for the Weak Label Model Predictor.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class WeakLabelModelPredictorNodeModel extends NodeModel {

    private static final int DATA_PORT = 1;

    private static final int MODEL_PORT = 0;

    private final WeakLabelModelPredictorSettings m_settings = new WeakLabelModelPredictorSettings();

    /**
     */
    protected WeakLabelModelPredictorNodeModel() {
        super(new PortType[]{WeakLabelModelPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final WeakLabelModelPortObjectSpec spec = (WeakLabelModelPortObjectSpec)inSpecs[MODEL_PORT];
        final DataTableSpec tableSpec = (DataTableSpec)inSpecs[DATA_PORT];
        try (final WeakLabelModelPredictor predictor = new WeakLabelModelPredictor(spec, m_settings)) {
            return new PortObjectSpec[]{createRearranger(tableSpec, spec, predictor).createSpec()};

        } catch (InvalidSettingsException ise) {
            throw ise;
        } catch (Exception ex) {
            // there is a serious bug in WeakLabelModelPredictor if this code is ever reached
            throw new IllegalStateException("An unexpected problem occurred.", ex);
        }
    }

    private ColumnRearranger createRearranger(final DataTableSpec tableSpec, final WeakLabelModelPortObjectSpec spec,
        final WeakLabelModelPredictor predictor) throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(tableSpec);
        rearranger.append(predictor.createCellFactory(tableSpec));
        if (m_settings.isRemoveSourceColumns()) {
            rearranger.remove(spec.getTrainingSpec().getColumnNames());
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final WeakLabelModelPortObject po = (WeakLabelModelPortObject)inObjects[MODEL_PORT];
        final BufferedDataTable table = (BufferedDataTable)inObjects[DATA_PORT];
        final DataTableSpec dataSpec = table.getDataTableSpec();
        final WeakLabelModelPortObjectSpec spec = po.getSpec();
        try (WeakLabelModelPredictor predictor = new WeakLabelModelPredictor(po, m_settings, exec)) {
            final ColumnRearranger rearranger = createRearranger(dataSpec, spec, predictor);
            final BufferedDataTable output = exec.createColumnRearrangeTable(table, rearranger, exec);
            return new PortObject[]{output};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final WeakLabelModelPortObject po =
                    (WeakLabelModelPortObject)((PortObjectInput)inputs[MODEL_PORT]).getPortObject();
                final DataTableSpec dataSpec = (DataTableSpec)inSpecs[DATA_PORT];
                final WeakLabelModelPortObjectSpec spec = po.getSpec();
                try (WeakLabelModelPredictor predictor = new WeakLabelModelPredictor(po, m_settings, exec)) {
                    final ColumnRearranger rearranger = createRearranger(dataSpec, spec, predictor);
                    final StreamableFunction func = rearranger.createStreamableFunction(1, 0);
                    func.runFinal(inputs, outputs, exec);
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals

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
        // nothing to reset
    }

}
