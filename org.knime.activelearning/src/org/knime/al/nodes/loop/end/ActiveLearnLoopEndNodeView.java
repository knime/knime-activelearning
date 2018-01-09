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
 * Created on 01.03.2013 by Christian Dietz
 */
package org.knime.al.nodes.loop.end;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.NodeView;

/**
 *
 * @author Christian Dietz, Jonathan Hale
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ActiveLearnLoopEndNodeView
        extends NodeView<ActiveLearnLoopEndNodeModel> {

    // Simple container holding the view
    private final JPanel m_messageContainer;

    private ActiveLearnLoopEndNodeViewListener m_eventListener; // EventListener
    private ActiveLearnLoopEndNodeViewPanel m_gui; // GUI components (is a
    // JPanel)

    private final ActiveLearnLoopEndNodeModel m_nodeModel; // NodeModel

    /**
     * Constructor
     *
     * @param nodeModel
     */
    protected ActiveLearnLoopEndNodeView(
            final ActiveLearnLoopEndNodeModel nodeModel) {
        super(nodeModel);

        m_nodeModel = nodeModel;

        setShowNODATALabel(false);

        m_gui = new ActiveLearnLoopEndNodeViewPanel();
        // Panel to show "detail representation"

        m_messageContainer = new JPanel();
        m_messageContainer.setPreferredSize(new Dimension(600, 700)); // initializes
        // m_gui

        // initialize event listener
        m_eventListener = new ActiveLearnLoopEndNodeViewListener(m_nodeModel);

        // add event listener
        m_gui.addListener(m_eventListener);
    }

    @Override
    protected void onClose() {
        m_eventListener = null;
        m_gui = null;
    }

    @Override
    protected void onOpen() {
        if (m_nodeModel == null) {
            return;
        }
    }

    @Override
    protected void modelChanged() {
        switch (m_nodeModel.getNodeState()) {
        case EXECUTING:
            showExecuting();
            break;
        case SUSPENDED:
            showInteractiveGui();
            break;
        case CONFIGURED:
            showConfigured();
            break;
        case TERMINATED:
            showTermination();
            break;
        }
    }

    /**
     * Show gui for the executing state
     */
    private void showExecuting() {
        m_messageContainer.removeAll();
        m_messageContainer.add(new JLabel("Executing"));
        setComponent(m_messageContainer);
    }

    /**
     * Show gui for user-interaction state
     */
    private void showInteractiveGui() {
        m_eventListener.prepare();

        setComponent(m_gui);
    }

    /**
     * Show gui for configured (?) state
     */
    private void showConfigured() {
        m_messageContainer.removeAll();
        m_messageContainer
                .add(new JLabel("Configured... Waiting for Execution"));
        setComponent(m_messageContainer);
    }

    /**
     * Show gui for terminated State
     */
    private void showTermination() {
        m_messageContainer.removeAll();
        m_messageContainer.add(new JLabel("Terminated"));
        setComponent(m_messageContainer);
    }
}
