/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *
 */
package org.knime.al.nodes.score.combine.pbac;

import java.util.ArrayList;
import java.util.List;

import org.knime.al.nodes.score.combine.AbstractCombinerNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author dietzc, University of Konstanz
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class UncertaintyDistributionScorerNodeModel
        extends AbstractCombinerNodeModel {

    private final SettingsModelString m_explorationColumnModel =
            createExplorationColumnModel();
    private final SettingsModelString m_exploitationColumnModel =
            createExploitationColumnModel();
    private final SettingsModelDouble m_exploitationFactorModel =
            createExploitationFactorModel();

    /**
     * {@inheritDoc} Entropy based uncertainty scoring.
     */
    @Override
    protected ColumnRearranger createResRearranger(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final DataColumnSpec newColSpec =
                new DataColumnSpecCreator("Uncertainty Distribution Score",
                        DoubleCell.TYPE).createSpec();

        // utility object that performs the calculation
        rearranger.append(new SingleCellFactory(newColSpec) {
            private final int m_explorationIndex = inSpec
                    .findColumnIndex(m_explorationColumnModel.getStringValue());
            private final int m_exploitationIndex = inSpec.findColumnIndex(
                    m_exploitationColumnModel.getStringValue());
            private final double m_exFactor =
                    m_exploitationFactorModel.getDoubleValue();

            @Override
            // Get the scores weighted by the exploration factor
            public DataCell getCell(final DataRow row) {
                final double explorationScore;
                final double exploitationScore;
                try {
                    explorationScore = m_exFactor
                            * ((DoubleValue) row.getCell(m_explorationIndex))
                                    .getDoubleValue();
                    exploitationScore = (1 - m_exFactor)
                            * ((DoubleValue) row.getCell(m_exploitationIndex))
                                    .getDoubleValue();
                } catch (final ClassCastException e) {
                    throw new IllegalArgumentException(
                            "Encountered missing value at row: "
                                    + row.getKey());
                }
                return new DoubleCell(exploitationScore + explorationScore);
            }
        });
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<SettingsModel> collectSettingsModels() {
        if (m_settingsModels == null) {
            m_settingsModels = new ArrayList<>();
            m_settingsModels.add(m_exploitationColumnModel);
            m_settingsModels.add(m_explorationColumnModel);
            m_settingsModels.add(m_exploitationFactorModel);
        }
        return m_settingsModels;
    }

    /**
     * @return Settingsmodel for the exploration factor.
     */
    static SettingsModelDoubleBounded createExploitationFactorModel() {
        return new SettingsModelDoubleBounded("Exploitation Factor", 0.4, 0, 1);
    }

    /**
     * @return Settings model for the name of the exploitation column.
     */
    static SettingsModelString createExploitationColumnModel() {
        return new SettingsModelString("Exploitation Score Column", "");
    }

    /**
     * @return Settings model for the name of the exploration column.
     */
    static SettingsModelString createExplorationColumnModel() {
        return new SettingsModelString("Exploration Score Column", "");
    }

}
