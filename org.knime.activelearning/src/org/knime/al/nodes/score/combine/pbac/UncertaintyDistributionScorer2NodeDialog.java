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
 *   Jul 31, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.combine.pbac;

import java.awt.Color;

import javax.swing.JLabel;

import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.util.CheckUtils;

/**
 * Node dialog for the Exploration/Exploitation Score Combiner node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class UncertaintyDistributionScorer2NodeDialog extends DefaultNodeSettingsPane {

    private final DialogComponentColumnNameSelection m_exploitationColumn;

    private final DialogComponentColumnNameSelection m_explorationColumn;

    private final DialogComponentLabel m_warningLabel;

    @SuppressWarnings("unchecked")
    UncertaintyDistributionScorer2NodeDialog() {
        m_exploitationColumn = new DialogComponentColumnNameSelection(
            UncertaintyDistributionScorer2NodeModel.createExploitationColumnModel(), "Exploitation score column", 0,
            true, DoubleValue.class);
        addDialogComponent(m_exploitationColumn);

        m_explorationColumn = new DialogComponentColumnNameSelection(
            UncertaintyDistributionScorer2NodeModel.createExplorationColumnModel(), "Exploration score column", 0, true,
            DoubleValue.class);
        addDialogComponent(m_explorationColumn);

        m_warningLabel = new DialogComponentLabel("");
        JLabel warningLabel = (JLabel)m_warningLabel.getComponentPanel().getComponent(0);
        warningLabel.setForeground(Color.RED);
        addDialogComponent(m_warningLabel);

        m_explorationColumn.getModel().addChangeListener(l -> updateWarning());

        m_exploitationColumn.getModel().addChangeListener(l -> updateWarning());

        addDialogComponent(new DialogComponentNumber(
            UncertaintyDistributionScorer2NodeModel.createExploitationFactorModel(), "Exploitation factor", 0.1));

        addDialogComponent(new DialogComponentString(
            UncertaintyDistributionScorer2NodeModel.createOutputColumnNameModel(), "Output column name", true, 20));

        createNewGroup("Missing value handling");
        addDialogComponent(
            new DialogComponentButtonGroup(UncertaintyDistributionScorer2NodeModel.createMissingValueHandlingModel(),
                null, true, ExceptionHandling.values()));
    }

    private void updateWarning() {
        if (isSelectionAvailable() && isSameColumnSelected()) {
            m_warningLabel.setText(UncertaintyDistributionScorer2NodeModel.SAME_COLUMN_ERROR);
        } else {
            m_warningLabel.setText("");
        }
    }

    private boolean isSameColumnSelected() {
        return m_exploitationColumn.getSelected().equals(m_explorationColumn.getSelected());
    }

    private boolean isSelectionAvailable() {
        return m_exploitationColumn.getSelected() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        CheckUtils.checkSetting(!isSameColumnSelected(), UncertaintyDistributionScorer2NodeModel.SAME_COLUMN_ERROR);
    }

}
