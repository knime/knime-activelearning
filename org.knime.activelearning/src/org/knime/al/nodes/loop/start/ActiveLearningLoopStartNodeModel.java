package org.knime.al.nodes.loop.start;
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

import org.knime.al.nodes.loop.end.ActiveLearningLoopEndNodeModel;
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
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;


/**
 * Extension to the Recursive Loop Start node for Active Learning. Has one general port object and one table input
 * and the same as outputs. These are fed by the corresponding Active Learning Loop End node.
 *
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class ActiveLearningLoopStartNodeModel extends NodeModel implements LoopStartNode {

    private int m_currentiteration;

    /**
     * Constructor for the node model.
     */
    protected ActiveLearningLoopStartNodeModel() {
        super(new PortType[]{PortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{PortObject.TYPE, BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        BufferedDataTable table_res;
        PortObject port_res;

        if (m_currentiteration == 0) {
            port_res = inObjects[0];
            table_res = (BufferedDataTable)inObjects[1];
        } else {

            LoopEndNode endNode = getLoopEndNode();

            if (!(endNode instanceof ActiveLearningLoopEndNodeModel)) {
                throw new InvalidSettingsException(
                    "Loop Start is connected to the wrong Loop End node. Please use the Active Learning Loop End node.");
            }

            ActiveLearningLoopEndNodeModel end = (ActiveLearningLoopEndNodeModel)endNode;
            port_res = end.getRecursiveInPort();
            table_res = end.getRecursiveInTable();
        }
        pushFlowVariableInt("currentIteration", m_currentiteration);

        m_currentiteration++;
        return new PortObject[]{port_res, table_res};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currentiteration = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        pushFlowVariableInt("currentIteration", m_currentiteration);
        return new PortObjectSpec[]{inSpecs[0], inSpecs[1]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
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
}
