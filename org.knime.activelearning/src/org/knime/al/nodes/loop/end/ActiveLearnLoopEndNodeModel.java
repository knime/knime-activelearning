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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 30, 2013 (hornm): created
 */
package org.knime.al.nodes.loop.end;

import java.awt.Component;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.al.nodes.AbstractALNodeModel;
import org.knime.al.nodes.loop.ActiveLearnLoopEnd;
import org.knime.al.nodes.loop.ActiveLearnLoopUtils.NodeModelState;
import org.knime.al.nodes.loop.end.components.ClassModel;
import org.knime.al.util.NodeUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;

/**
 *
 * @author dietzc, hornm, halej, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ActiveLearnLoopEndNodeModel extends AbstractALNodeModel
        implements ActiveLearnLoopEnd {

    private static final int DATA_PORT = 0;
    private static final int PASSTHROUGH_PORT = 1;

    private int m_repColIdx;

    private boolean m_isExecuting = false;

    private boolean m_isTerminated = false;

    private final Semaphore m_semaphore = new Semaphore(false);

    private DataValueRendererFamily m_renderer; // Renderer for a specific cell
    // of the table

    private final SettingsModelString m_classColModel =
            ActiveLearnLoopEndSettingsModels.createClassColumnModel();
    private final SettingsModelString m_repColModel =
            ActiveLearnLoopEndSettingsModels.createRepColumnModel();
    private final SettingsModelOptionalString m_defaultClassNameModel =
            ActiveLearnLoopEndSettingsModels.createDefaultClassModel();
    private final SettingsModelBoolean m_autoTerminateModel =
            ActiveLearnLoopEndSettingsModels.createAutoTerminateModel();

    private Map<RowKey, String> m_classMap; // User may enter the class labels
    // for certain rows (the hilited
    // ones). These entries are stored
    // in this classMap. The
    // LoopStartNode requests this map
    // the handle the labeled entires.

    private Map<RowKey, DataRow> m_rowMap; // maps rowkeys to rows

    private int m_curIterationIndex; // index oder the current Iteration

    private int m_classColIdx;

    private final ClassModel m_classModel; // Holds user defined classes

    private String[] m_colNames;

    /**
     *
     */
    protected ActiveLearnLoopEndNodeModel() {
        super(new PortType[] { BufferedDataTable.TYPE,
                BufferedDataTable.TYPE_OPTIONAL, },
                new PortType[] { BufferedDataTable.TYPE });

        m_isExecuting = false;
        m_isTerminated = false;

        // empty row map if no input data is present, yet.
        m_rowMap = new HashMap<RowKey, DataRow>();

        m_classModel = new ClassModel();

        m_curIterationIndex = 0;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        m_classColIdx = NodeUtils.autoColumnSelection(inSpecs[DATA_PORT],
                m_classColModel, StringValue.class, this.getClass());

        m_repColIdx = NodeUtils.autoColumnSelection(inSpecs[DATA_PORT],
                m_repColModel, DataValue.class, this.getClass());

        m_renderer = inSpecs[DATA_PORT].getColumnSpec(m_repColIdx).getType()
                .getRenderer(inSpecs[DATA_PORT].getColumnSpec(m_repColIdx));

        m_colNames = inSpecs[DATA_PORT].getColumnNames();

        // Pass through
        if (inSpecs[PASSTHROUGH_PORT] != null) {
            return new DataTableSpec[] { inSpecs[PASSTHROUGH_PORT] };
        }
        // else route the input port
        return new DataTableSpec[] { inSpecs[DATA_PORT] };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (m_classColIdx == -1) {
            m_classColIdx = NodeUtils.autoColumnSelection(
                    inData[DATA_PORT].getDataTableSpec(), m_classColModel,
                    StringValue.class, this.getClass());
        }

        if (m_repColIdx == -1) {
            m_repColIdx = NodeUtils.autoColumnSelection(
                    inData[DATA_PORT].getDataTableSpec(), m_repColModel,
                    DataValue.class, this.getClass());
        }

        m_isExecuting = true;

        // Automatically terminate if there are no more rows
        if (!m_autoTerminateModel.getBooleanValue()
                || (inData[DATA_PORT].size() > 0)) {

            m_rowMap = new HashMap<>((int)inData[DATA_PORT].size());
            for (final DataRow row : inData[DATA_PORT]) {
                m_rowMap.put(row.getKey(), row);
            }

            exec.setProgress("Waiting for user input ...");

            suspendExecution(exec);
        } else {
            terminate();
        }

        exec.setProgress("Processing ...");

        // Tabelle mit hilites (keys sind rowkeys von tabelle)
        if ((inData[DATA_PORT].size() > 0) && !m_isTerminated) {
            super.continueLoop();
            // This called after each loop, nothing is returned on outport of
            // node
            return null;
        } else {
            // Pass through
            if (inData[PASSTHROUGH_PORT] != null) {
                return new BufferedDataTable[] { inData[PASSTHROUGH_PORT] };
            }
            // If not available return the input data
            return new BufferedDataTable[] { inData[DATA_PORT] };
        }
    }

    /**
     * Wait for the user to continue or terminate the loop.
     *
     * @param exec
     *            the execution context
     * @throws InterruptedException
     * @throws CanceledExecutionException
     */
    private void suspendExecution(final ExecutionContext exec)
            throws InterruptedException, CanceledExecutionException {
        m_semaphore.setState(false);
        stateChanged();

        while (!m_semaphore.getState()) {
            Thread.sleep(1000);
            try {
                exec.checkCanceled();
            } catch (final CanceledExecutionException e) {
                m_semaphore.setState(true);
                stateChanged();
                throw e;
            }
        }
        m_curIterationIndex++;
    }

    /**
     * @return a map that
     */
    public Map<RowKey, DataRow> getRowMap() {
        return m_rowMap;
    }

    /**
     * Pass a class Map (classified/labeled RowKeys) to the NodeModel
     *
     * @param classMap
     */
    protected void setClassMap(final Map<RowKey, String> classMap) {
        m_classMap = new HashMap<RowKey, String>(classMap);
    }

    /**
     * Step out of suspended state and continue executm_classColModelion
     */
    protected void continueExecution() {
        m_semaphore.setState(true);
    }

    /**
     * Terminate execution, move to terminated state
     */
    protected void terminate() {
        m_isExecuting = false;
        m_isTerminated = true;
        m_curIterationIndex = 0;

        m_semaphore.setState(true);
    }

    @Override
    public Map<RowKey, String> getNewlyLabeledRows() {
        return m_classMap;
    }

    /**
     * @return the current state of node (NodeModelState)
     */
    protected NodeModelState getNodeState() {
        if (m_isExecuting) {
            return m_semaphore.getState() ? NodeModelState.EXECUTING
                    : NodeModelState.SUSPENDED;
        } else {
            return m_isTerminated ? NodeModelState.TERMINATED
                    : NodeModelState.CONFIGURED;
        }
    }

    public int getClassColumnIndex() {
        return m_classColIdx;
    }

    public int getCurrentIterationIndex() {
        return m_curIterationIndex;
    }

    /**
     * Get renderer of data referenced by RowKey parameter.
     *
     * @param key
     *            the key of the desired row
     * @return Component which represents a renderer for the data referenced by
     *         key
     */
    public Component requireRenderer(final RowKey key) {
        final DataRow dataRow = m_rowMap.get(key);
        if (dataRow == null) {
            throw new IllegalStateException();
        }
        final DataCell cell = dataRow.getCell(m_repColIdx);
        return m_renderer.getRendererComponent(cell);
    }

    /**
     * Returns a List of user-defined classes
     *
     * @return List<String>, the classes defined by user
     */
    public List<String> getDefinedClasses() {
        return m_classModel.getDefinedClasses();
    }

    /**
     * Get the name of the class column
     *
     * @return String, name of the class column
     */
    public String getClassColumnName() {
        return m_classColModel.getStringValue();
    }

    public ClassModel getClassModel() {
        return m_classModel;
    }

    /*
     *
     * KNIME RELATED STUFF
     */

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>();
            m_settingsModels.add(m_repColModel);
            m_settingsModels.add(m_classColModel);
            m_settingsModels.add(m_defaultClassNameModel);
            m_settingsModels.add(m_autoTerminateModel);
        }
        return m_settingsModels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {

        final String path = nodeInternDir.getAbsolutePath();

        final File file = new File(path + "LoopEndNode.intern");

        final DataInputStream is =
                new DataInputStream(new FileInputStream(file));

        final int numClasses = is.readInt();

        for (int i = 0; i < numClasses; i++) {
            m_classModel.addClass(is.readUTF());
        }
        is.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {

        final String path = nodeInternDir.getAbsolutePath();

        final File file = new File(path + "LoopEndNode.intern");

        // save the defined classes
        final DataOutputStream os =
                new DataOutputStream(new FileOutputStream(file));

        os.writeInt(m_classModel.getSize());

        for (final String clsName : m_classModel.getDefinedClasses()) {
            os.writeUTF(clsName);
        }
        os.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_isExecuting = false;
        m_isTerminated = false;
    }

    // Helper class
    private class Semaphore {
        private boolean m_state;

        public Semaphore(final boolean state) {
            setState(state);
        }

        /**
         * @param state
         *            the state to set
         */
        public void setState(final boolean state) {
            m_state = state;
        }

        /**
         * @return the state
         */
        public boolean getState() {
            return m_state;
        }
    }

    /**
     * @return the column names
     */
    public String[] getColNames() {
        return m_colNames;
    }
}
