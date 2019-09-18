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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WeakLabelModelPredictorSettings {

    static SettingsModelString createLabelColumnName() {
        return new SettingsModelString("labelColumnName", "class");
    }

    static SettingsModelBoolean createOutputProbabilities() {
        return new SettingsModelBoolean("outputProbabilities", false);
    }

    static SettingsModelBoolean createRemoveSourceColumns() {
        return new SettingsModelBoolean("removeOriginalColumns", false);
    }

    private final SettingsModelString m_labelColumnName = createLabelColumnName();

    private final SettingsModelBoolean m_removeSourceColumns = createRemoveSourceColumns();

    private final SettingsModelBoolean m_outputProbabilities = createOutputProbabilities();

    void saveSettingsTo(final NodeSettingsWO settings) {
        m_removeSourceColumns.saveSettingsTo(settings);
        m_labelColumnName.saveSettingsTo(settings);
        m_outputProbabilities.saveSettingsTo(settings);
    }

    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeSourceColumns.validateSettings(settings);
        m_labelColumnName.validateSettings(settings);
        m_outputProbabilities.validateSettings(settings);
    }

    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeSourceColumns.loadSettingsFrom(settings);
        m_labelColumnName.loadSettingsFrom(settings);
        m_outputProbabilities.loadSettingsFrom(settings);
    }

    boolean isRemoveSourceColumns() {
        return m_removeSourceColumns.getBooleanValue();
    }

    boolean isAppendProbabilities() {
        return m_outputProbabilities.getBooleanValue();
    }

    SettingsModelBoolean getOutputProbabilities() {
        return m_outputProbabilities;
    }

    String getPredictionColumnName() {
        return m_labelColumnName.getStringValue();
    }
}
