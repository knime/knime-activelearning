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

import org.knime.al.nodes.score.ExceptionHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.CombinedColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * Node dialog for the Exploration/Exploitation Score Combiner node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class UncertaintyDistributionScorer2NodeDialog extends DefaultNodeSettingsPane {

    @SuppressWarnings("unchecked")
    UncertaintyDistributionScorer2NodeDialog() {
        final DialogComponentColumnNameSelection diaCompExploitationSelection = new DialogComponentColumnNameSelection(
            UncertaintyDistributionScorer2NodeModel.createExploitationColumnModel(), "Exploitation score column", 0,
            true, DoubleValue.class);
        addDialogComponent(diaCompExploitationSelection);

        // filters the currently selected exploitation column
        final ColumnFilter columnFilter = new ColumnFilter() {

            @Override
            public boolean includeColumn(final DataColumnSpec colSpec) {
                return !colSpec.getName().equals(diaCompExploitationSelection.getSelected());
            }

            @Override
            public String allFilteredMsg() {
                return null;
            }

        };
        final CombinedColumnFilter combinedColumnFilter =
            new CombinedColumnFilter(new DataValueColumnFilter(DoubleValue.class), columnFilter) {
                @Override
                public String allFilteredMsg() {
                    return "At least two numeric columns must be in the input.";
                }
            };
        final DialogComponentColumnNameSelection diaCompExplorationSelection = new DialogComponentColumnNameSelection(
            UncertaintyDistributionScorer2NodeModel.createExplorationColumnModel(), "Exploration score column", 0, true,
            combinedColumnFilter);
        addDialogComponent(diaCompExplorationSelection);

        // update the exploration column list if another exploitation column is selected
        diaCompExploitationSelection.getModel().addChangeListener(l -> {
            try {
                final ColumnSelectionPanel columnSelectionPanel =
                    (ColumnSelectionPanel)diaCompExplorationSelection.getComponentPanel().getComponent(1);
                columnSelectionPanel.update(columnSelectionPanel.getDataTableSpec(),
                    columnSelectionPanel.getSelectedColumn());
            } catch (NotConfigurableException ex) {
                // should never happen
            }
        });

        addDialogComponent(new DialogComponentNumber(
            UncertaintyDistributionScorer2NodeModel.createExploitationFactorModel(), "Exploitation factor", 0.1));

        addDialogComponent(new DialogComponentString(
            UncertaintyDistributionScorer2NodeModel.createOutputColumnNameModel(), "Output column name", true, 20));

        addDialogComponent(
            new DialogComponentButtonGroup(UncertaintyDistributionScorer2NodeModel.createMissingValueHandlingModel(),
                "In case of missing values...", true, ExceptionHandling.values()));
    }
}
