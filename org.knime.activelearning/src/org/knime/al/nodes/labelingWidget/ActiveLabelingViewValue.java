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
 */
package org.knime.al.nodes.labelingWidget;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.node.table.AbstractTableValue;
import org.knime.js.core.settings.table.TableValueSettings;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class ActiveLabelingViewValue extends AbstractTableValue {

    private final static String CFG_POSSIBLE_VALUES = "possibleValues";

    private String[] m_possibleLabelValues = new String[]{};

    private final static String CFG_LABELS = "labels";

    private Map<String, String> m_labels = Collections.<String, String> emptyMap();

    private final static String CFG_COLORS = "colors";

    private Map<String, Integer> m_colors = Collections.<String, Integer> emptyMap();

    private TableValueSettings m_tableSettings = new TableValueSettings();

    @JsonProperty("tablesettings")
    public TableValueSettings getTableSettings() {
        return m_tableSettings;
    }

    @JsonProperty("tablesettings")
    public void setTableSettings(final TableValueSettings m_tableSettings) {
        this.m_tableSettings = m_tableSettings;
    }

    @JsonProperty("colors")
    public void setColors(final Map<String, Integer> value) {
        m_colors = value;
    }

    @JsonProperty("colors")
    public Map<String, Integer> getColors() {
        return m_colors;
    }

    @JsonProperty("labels")
    public Map<String, String> getLabels() {
        return m_labels;
    }

    @JsonProperty("labels")
    public void setLabels(final Map<String, String> values) {
        m_labels = values;
    }

    @JsonProperty("possiblevalues")
    public String[] getPossibleLabelValues() {
        return m_possibleLabelValues;
    }

    @JsonProperty("possiblevalues")
    public void setPossibleLabelValues(final String[] values) {
        m_possibleLabelValues = values;
    }

    private TableValueSettings m_settings = new TableValueSettings();

    /**
     * @return the settings
     */
    @Override
    @JsonUnwrapped
    public TableValueSettings getSettings() {
        return m_settings;
    }

    /**
     * @param settings the settings to set
     */
    @Override
    @JsonUnwrapped
    public void setSettings(final TableValueSettings settings) {
        m_settings = settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonIgnore
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_POSSIBLE_VALUES, m_possibleLabelValues);

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String labelsString = objectMapper.writeValueAsString(m_labels);
            String colorsString = objectMapper.writeValueAsString(m_colors);
            settings.addString(CFG_LABELS, labelsString.toString());
            settings.addString(CFG_COLORS, colorsString.toString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    @JsonIgnore
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        setPossibleLabelValues(settings.getStringArray(CFG_POSSIBLE_VALUES));
        Map<String, String> labels;
        Map<String, Integer> colors;
        try {
            labels = new ObjectMapper().readValue(settings.getString(CFG_LABELS), Map.class);
            colors = new ObjectMapper().readValue(settings.getString(CFG_COLORS), Map.class);
            setLabels(labels);
            setColors(colors);
        } catch (IOException e) {
            e.printStackTrace();
            // Nothing todo
        }

        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ActiveLabelingViewValue other = (ActiveLabelingViewValue)obj;
        return new EqualsBuilder().append(m_settings, other.m_settings)
            .append(m_possibleLabelValues, other.getPossibleLabelValues()).append(m_labels, other.m_labels).isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_settings).append(m_possibleLabelValues).append(m_labels).toHashCode();
    }

}