package org.knime.al.nodes.loop.end;
/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */

import java.io.File;
import java.io.IOException;

import org.knime.al.nodes.loop.start.ActiveLearningLoopStartNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;

/**
 * Extension to the Recursive Loop End node for Active Learning. Has three inputs: 1. Port Object used for recursion, 2.
 * Table collecting outputs, 3. Table used for recursion. Outputs are the (1) input Port Object of the last iteration
 * and (2) the collected table.
 *
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class ActiveLearningLoopEndNodeModel extends NodeModel implements LoopEndNode {

    private BufferedDataContainer m_outcontainer;

    private BufferedDataTable m_recursiveInTable;

    private PortObject m_recursiveInPort;

    private int m_iterationnr = 0;

    private final SettingsModelIntegerBounded m_maxIterations = ActiveLearningLoopEndNodeDialog.createIterationsModel();

    private final SettingsModelInteger m_minNumberOfRows = ActiveLearningLoopEndNodeDialog.createNumOfRowsModel();

    private final SettingsModelBoolean m_onlyLastResult = ActiveLearningLoopEndNodeDialog.createOnlyLastModel();

    private final SettingsModelString m_endLoopVariableName = ActiveLearningLoopEndNodeDialog.createEndLoopVarModel();

    private final SettingsModelBoolean m_useVariable = ActiveLearningLoopEndNodeDialog.createUseVariable();

    private final SettingsModelBoolean m_addIterationNr = ActiveLearningLoopEndNodeDialog.createAddIterationColumn();

    /**
     * Constructor for the node model.
     */
    protected ActiveLearningLoopEndNodeModel() {
        super(new PortType[]{PortObject.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE},
            new PortType[]{PortObject.TYPE, BufferedDataTable.TYPE});
    }

    private static int RECURSIVE_IN_PORT_INDEX = 0;

    private static int COLLECTING_IN_PORT_INDEX = 1;

    private static int RECURSIVE_IN_TABLE_INDEX = 2;

    /**
     * Check if the loop end is connected to its loop start counterpart, i.e. {@link ActiveLearningLoopStartNodeModel}.
     * @throws InvalidSettingsException
     */
    protected void validateLoopStart() throws InvalidSettingsException {
        if (!(this.getLoopStartNode() instanceof ActiveLearningLoopStartNodeModel)) {
            throw new InvalidSettingsException(
                "Loop End is connected to the wrong Loop Start node. Please use the Active Learning Loop Start node.");
        }
    }

    /**
     * Check if the datatable size is smaller than the threshold.
     *
     * @param minNrRows the minimal number of rows to continue the loop.
     * @return true when the data table size is smaller than the number of rows.
     */
    protected boolean checkDataTableSize(final int minNrRows) {
        return m_recursiveInTable.size() < minNrRows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        // in port 0: recursive port fed back to loop start node
        // in port 1: collects the data provided at the output port
        // in port 2: recursive table fed back to loop start node
        validateLoopStart();
        // Table needs to be copied as the underlying data storage will be deleted on a reset of the loop end
        // node (every loop iteration). Then successor nodes of the loop start would not be able to access
        // the table anymore.
        m_recursiveInTable = copyTable((BufferedDataTable)inData[RECURSIVE_IN_TABLE_INDEX], exec);
        m_recursiveInPort = inData[RECURSIVE_IN_PORT_INDEX];

        BufferedDataTable collectingTable = (BufferedDataTable)inData[COLLECTING_IN_PORT_INDEX];
        PortObject recursivePort = inData[RECURSIVE_IN_PORT_INDEX];

        boolean endLoopFromVariable = false;
        if (m_useVariable.getBooleanValue()) {
            final String value = peekFlowVariableString(m_endLoopVariableName.getStringValue());
            if (value == null) {
                throw new InvalidSettingsException(
                    "The selected flow variable '" + m_endLoopVariableName.getStringValue() + "' does not exist.");
            }
            endLoopFromVariable = "true".equalsIgnoreCase(value);
        }

        // Stopping conditions, break recursion if either:
        // 1. recursion table has less than user specified number of rows OR
        // 2. max number of iterations has been reached OR
        // 3. selected flow variable for stopping condition evaluates to "true"
        final boolean endLoop = checkDataTableSize(m_minNumberOfRows.getIntValue())
            || ((m_iterationnr + 1) >= m_maxIterations.getIntValue()) || endLoopFromVariable;

        if (m_onlyLastResult.getBooleanValue()) {
            if (endLoop) {
                return new PortObject[]{recursivePort, collectingTable};
            }
        } else {
            if (m_outcontainer == null) {
                final DataTableSpec dts = createSpec(collectingTable.getDataTableSpec());
                m_outcontainer = exec.createDataContainer(dts);
            }
            if (m_addIterationNr.getBooleanValue()) {
                final IntCell currIterCell = new IntCell(m_iterationnr);
                appendToContainer(exec, collectingTable, currIterCell);
            } else {
                appendToContainer(exec, collectingTable);
            }

            if (endLoop) {
                m_outcontainer.close();
                return new PortObject[]{recursivePort, m_outcontainer.getTable()};
            }
        }
        m_iterationnr++;
        // go on with loop
        super.continueLoop();
        return new PortObject[2];
    }

    private void appendToContainer(final ExecutionContext exec, final BufferedDataTable table,
        final DataCell... additionalCells) throws CanceledExecutionException {
        for (final DataRow row : table) {
            exec.checkCanceled();
            exec.setMessage("Collecting data for output.");
            final RowKey newKey = new RowKey(row.getKey() + "#" + m_iterationnr);
            DataRow newRow;
            if (additionalCells.length > 0) {
                newRow = new AppendedColumnRow(new DefaultRow(newKey, row), additionalCells);
            } else {
                newRow = new DefaultRow(newKey, row);
            }
            m_outcontainer.addRowToTable(newRow);
        }
    }

    private static BufferedDataTable copyTable(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        final BufferedDataContainer loopData = exec.createDataContainer(table.getDataTableSpec());
        for (final DataRow row : table) {
            exec.checkCanceled();
            exec.setMessage("Copying input table.");
            loopData.addRowToTable(row);
        }
        loopData.close();
        return loopData.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iterationnr = 0;
        m_outcontainer = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        //validateLoopStart();

        if (m_onlyLastResult.getBooleanValue()) {
            // The spec of the collecting table may change in the loop, however we could still return this spec
            // nevertheless. For now, keeping the same behavior as in the original Recursive Loop End.
            return new PortObjectSpec[]{inSpecs[RECURSIVE_IN_PORT_INDEX], null};
        }
        if (m_useVariable.getBooleanValue()
            && getAvailableFlowVariables().get(m_endLoopVariableName.getStringValue()) == null) {
            throw new InvalidSettingsException(
                "Selected flow variable: '" + m_endLoopVariableName.getStringValue() + "' not available!");
        }
        DataTableSpec collectingTableSpecs = (DataTableSpec)inSpecs[COLLECTING_IN_PORT_INDEX];
        return new PortObjectSpec[]{inSpecs[RECURSIVE_IN_PORT_INDEX], createSpec(collectingTableSpecs)};
    }

    private DataTableSpec createSpec(final DataTableSpec inSpec) {
        if (m_addIterationNr.getBooleanValue()) {
            final DataColumnSpecCreator crea =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec, "Iteration"), IntCell.TYPE);
            return new DataTableSpec(inSpec, new DataTableSpec(crea.createSpec()));
        } else {
            return inSpec;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_maxIterations.saveSettingsTo(settings);
        m_minNumberOfRows.saveSettingsTo(settings);
        m_onlyLastResult.saveSettingsTo(settings);
        m_endLoopVariableName.saveSettingsTo(settings);
        m_useVariable.saveSettingsTo(settings);
        m_addIterationNr.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_maxIterations.loadSettingsFrom(settings);
        m_minNumberOfRows.loadSettingsFrom(settings);
        m_onlyLastResult.loadSettingsFrom(settings);
        m_addIterationNr.loadSettingsFrom(settings);
        m_endLoopVariableName.loadSettingsFrom(settings);
        m_useVariable.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_maxIterations.validateSettings(settings);
        m_minNumberOfRows.validateSettings(settings);
        m_onlyLastResult.validateSettings(settings);
        m_addIterationNr.validateSettings(settings);
        m_endLoopVariableName.validateSettings(settings);
        m_useVariable.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * Retrieves the table connected to the recursive table input of the current recursive loop iteration.
     *
     * @return recursive table
     */
    public BufferedDataTable getRecursiveInTable() {
        return m_recursiveInTable;
    }

    /**
     * Retrieves the port object connected to the recursive port input of the current recursive loop iteration.
     *
     * @return recursive port object
     */
    public PortObject getRecursiveInPort() {
        return m_recursiveInPort;
    }
}
