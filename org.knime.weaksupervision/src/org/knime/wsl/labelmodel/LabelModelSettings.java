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
 *   Jul 19, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.labelmodel;

import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Handles the settings for the Label Model node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class LabelModelSettings {

    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createLabelSourceColumns() {
        return new SettingsModelColumnFilter2("noisyLabelColumns", NominalValue.class);
    }

    static SettingsModelDoubleBounded createLearningRate() {
        return new SettingsModelDoubleBounded("learningRate", 0.01, 1e-6, 1);
    }

    static SettingsModelString createLabelColumnName() {
        return new SettingsModelString("labelColumnName", "class");
    }

//    static SettingsModelColumnName createFirstCorrelationColumn() {
//        return new SettingsModelColumnName("firstCorrelationColumn", "");
//    }
//
//    static SettingsModelColumnName createSecondCorrelationColumn() {
//        return new SettingsModelColumnName("secondCorrelationColumn", "");
//    }

    static SettingsModelBoolean createRemoveSourceColumns() {
        return new SettingsModelBoolean("removeOriginalColumns", true);
    }

    static SettingsModelIntegerBounded createEpochs() {
        return new SettingsModelIntegerBounded("epochs", 100, 1, Integer.MAX_VALUE);
    }


    private final SettingsModelColumnFilter2 m_noisyLabelColumns = createLabelSourceColumns();

    private final SettingsModelDoubleBounded m_learningRate = createLearningRate();

    private final SettingsModelString m_labelColumnName = createLabelColumnName();

    // TODO uncomment lines corresponding to options for correlated sources
//    private final SettingsModelColumnName m_firstCorrelationColumn = createFirstCorrelationColumn();
//
//    private final SettingsModelColumnName m_secondCorrelationColumn = createSecondCorrelationColumn();

    private final SettingsModelBoolean m_removeSourceColumns = createRemoveSourceColumns();

    private final SettingsModelIntegerBounded m_epochs = createEpochs();


    void saveSettingsTo(final NodeSettingsWO settings) {
        m_noisyLabelColumns.saveSettingsTo(settings);
//        m_firstCorrelationColumn.saveSettingsTo(settings);
//        m_secondCorrelationColumn.saveSettingsTo(settings);
        m_removeSourceColumns.saveSettingsTo(settings);
        m_epochs.saveSettingsTo(settings);
        m_labelColumnName.saveSettingsTo(settings);
    }

    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_noisyLabelColumns.validateSettings(settings);
//        m_firstCorrelationColumn.validateSettings(settings);
//        m_secondCorrelationColumn.validateSettings(settings);
        m_removeSourceColumns.validateSettings(settings);
        m_epochs.validateSettings(settings);
        m_labelColumnName.validateSettings(settings);
    }

    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_noisyLabelColumns.loadSettingsFrom(settings);
//        m_firstCorrelationColumn.loadSettingsFrom(settings);
//        m_secondCorrelationColumn.loadSettingsFrom(settings);
        m_removeSourceColumns.loadSettingsFrom(settings);
        m_epochs.loadSettingsFrom(settings);
        m_labelColumnName.loadSettingsFrom(settings);
    }

    SettingsModelColumnFilter2 getNoisyLabelsFilter() {
        return m_noisyLabelColumns;
    }

//    String getFirstCorrelationColumn() {
//        return m_firstCorrelationColumn.getColumnName();
//    }

//    String getSecondCorrelationColumn() {
//        return m_secondCorrelationColumn.getColumnName();
//    }

    double getLearningRate() {
        return m_learningRate.getDoubleValue();
    }

    boolean isRemoveSourceColumns() {
        return m_removeSourceColumns.getBooleanValue();
    }

    int getEpochs() {
        return m_epochs.getIntValue();
    }

    String getLabelColumnName() {
        return m_labelColumnName.getStringValue();
    }

}
