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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONDataTable;
import org.knime.js.core.node.table.AbstractTableRepresentation;
import org.knime.js.core.settings.table.TableRepresentationSettings;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class ActiveLabelingViewRepresentation extends AbstractTableRepresentation {

    private TableRepresentationSettings m_tableSettings = new TableRepresentationSettings();

    private String[] m_possibleLabelValues = new String[]{};

    private String m_replaceCol = new String();

    private String m_appendCol = new String();

    private String[] m_secondPortLabelValues = new String[]{};

    private String m_labelColumnName;

    private boolean m_labelCreation;

    private Map<String, Integer> m_colors = Collections.<String, Integer> emptyMap();

    private String m_colorScheme;
//    private boolean m_useSecondInputPort = false;

    private boolean m_useNumCols;

    private boolean m_useColWidth;

    private int m_numCols;

    private int m_colWidth;

    private String m_labelCol;

    private boolean m_useRowID;

    private boolean m_alignLeft;

    private boolean m_alignRight;

    private boolean m_alignCenter;

    private boolean m_useProgressBar;

    private boolean m_autoSelectNextTile;

    private TableRepresentationSettings m_settings = new TableRepresentationSettings();

    public ActiveLabelingViewRepresentation() {
    }

    @JsonProperty("labelcolumn")
    public void setLabelColumnName(final String value) {
        m_labelColumnName = value;
    }

    @JsonProperty("labelcolumn")
    public String getLabelColumnName() {
        return m_labelColumnName;
    }

    @JsonProperty("colorscheme")
    public void setColorScheme(final String value) {
        m_colorScheme = value;
    }

    @JsonProperty("colorscheme")
    public String getColorScheme() {
        return m_colorScheme;
    }

    @JsonProperty("colors")
    public void setColors(final Map<String, Integer> value) {
        m_colors = value;
    }

    @JsonProperty("colors")
    public Map<String, Integer> getColors() {
        return m_colors;
    }

    @JsonProperty("labelcreation")
    public void setLabelCreation(final boolean value) {
        m_labelCreation = value;
    }

    @JsonProperty("labelcreation")
    public boolean getLabelCreation() {
        return m_labelCreation;
    }

    @JsonProperty("possiblevalues")
    public String[] getPossibleLabelValues() {
        return m_possibleLabelValues;
    }

    @JsonProperty("possiblevalues")
    public void setPossibleLabelValues(final String[] values) {
        m_possibleLabelValues = values;
    }

    @JsonProperty("replaceCol")
    public String getReplaceCol() {
        return m_replaceCol;
    }

    @JsonProperty("replaceCol")
    public void setReplaceCol(final String replaceCol) {
        m_replaceCol = replaceCol;
    }

    @JsonProperty("appendCol")
    public String getAppendCol() {
        return m_appendCol;
    }

    @JsonProperty("appendCol")
    public void setAppendCol(final String appendCol) {
        m_appendCol = appendCol;
    }

    @JsonProperty("secondportvalues")
    public void setSecondPortLabelValues(final String[] secondPortLabelValues) {
        m_secondPortLabelValues = secondPortLabelValues;
    }

    @JsonProperty("secondportvalues")
    public String[] getSecondPortLabelValues() {
        return m_secondPortLabelValues;
    }

    @JsonProperty("useProgressBar")
    public void setUseProgressBar(final boolean useProgressBar) {
        m_useProgressBar = useProgressBar;
    }

    @JsonProperty("useProgressBar")
    public boolean getUseProgressBar() {
        return m_useProgressBar;
    }

    @JsonProperty("autoSelectNextTile")
    public void setAutoSelectNextTile(final boolean autoSelectNextTile) {
        m_autoSelectNextTile = autoSelectNextTile;
    }

    @JsonProperty("autoSelectNextTile")
    public boolean isAutoSelectNextTile() {
        return m_autoSelectNextTile;
    }

    @JsonProperty("tablesettings")
    public TableRepresentationSettings getTableSettings() {
        return m_tableSettings;
    }

    @JsonProperty("tablesettings")
    public void setTableSettings(final TableRepresentationSettings settings) {
        m_tableSettings = settings;
    }

    //	@JsonProperty("useSecondInputPort")
    //	public void setUseSecondInputPort(final boolean useSecondInputPort) {
    //		m_useSecondInputPort = useSecondInputPort;
    //	}
    //
    //	/**
    //	 * @return the useSecondInputPort
    //	 */
    //	@JsonProperty("useSecondInputPort")
    //	public boolean getUseSecondInputPort() {
    //		return m_useSecondInputPort;
    //	}

    /**
     * @return the useNumCols
     */
    public boolean getUseNumCols() {
        return m_useNumCols;
    }

    /**
     * @param useNumCols the useNumCols to set
     */
    public void setUseNumCols(final boolean useNumCols) {
        m_useNumCols = useNumCols;
    }

    /**
     * @return the useColWidth
     */
    public boolean getUseColWidth() {
        return m_useColWidth;
    }

    /**
     * @param useColWidth the useColWidth to set
     */
    public void setUseColWidth(final boolean useColWidth) {
        m_useColWidth = useColWidth;
    }

    /**
     * @return the numCols
     */
    public int getNumCols() {
        return m_numCols;
    }

    /**
     * @param numCols the numCols to set
     */
    public void setNumCols(final int numCols) {
        m_numCols = numCols;
    }

    /**
     * @return the colWidth
     */
    public int getColWidth() {
        return m_colWidth;
    }

    /**
     * @param colWidth the colWidth to set
     */
    public void setColWidth(final int colWidth) {
        m_colWidth = colWidth;
    }

    /**
     * @return the labelCol
     */
    public String getLabelCol() {
        return m_labelCol;
    }

    /**
     * @param labelCol the labelCol to set
     */
    public void setLabelCol(final String labelCol) {
        m_labelCol = labelCol;
    }

    /**
     * @return the useRowID
     */
    public boolean getUseRowID() {
        return m_useRowID;
    }

    /**
     * @param useRowID the useRowID to set
     */
    public void setUseRowID(final boolean useRowID) {
        m_useRowID = useRowID;
    }

    /**
     * @return the alignLeft
     */
    public boolean getAlignLeft() {
        return m_alignLeft;
    }

    /**
     * @param alignLeft the alignLeft to set
     */
    public void setAlignLeft(final boolean alignLeft) {
        m_alignLeft = alignLeft;
    }

    /**
     * @return the alignRight
     */
    public boolean getAlignRight() {
        return m_alignRight;
    }

    /**
     * @param alignRight the alignRight to set
     */
    public void setAlignRight(final boolean alignRight) {
        m_alignRight = alignRight;
    }

    /**
     * @return the alignCenter
     */
    public boolean getAlignCenter() {
        return m_alignCenter;
    }

    /**
     * @param alignCenter the alignCenter to set
     */
    public void setAlignCenter(final boolean alignCenter) {
        m_alignCenter = alignCenter;
    }

    /**
     * @return the settings
     */
    @Override
    @JsonUnwrapped
    public TableRepresentationSettings getSettings() {
        return m_settings;
    }

    /**
     * @param settings the settings to set
     */
    @Override
    public void setSettings(final TableRepresentationSettings settings) {
        m_settings = settings;
    }

    /**
     * Copy settings from dialog keeping the existing table data
     *
     * @param settings the settings to set
     */
    @Override
    public void setSettingsFromDialog(final TableRepresentationSettings settings) {
        final JSONDataTable table = m_settings.getTable();
        m_settings = settings;
        m_settings.setTable(table);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonIgnore
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
        settings.addBoolean(ActiveLabelingConfig.CFG_USE_NUM_COLS, m_useNumCols);
        settings.addBoolean(ActiveLabelingConfig.CFG_USE_COL_WIDTH, m_useColWidth);
        settings.addInt(ActiveLabelingConfig.CFG_NUM_COLS, m_numCols);
        settings.addInt(ActiveLabelingConfig.CFG_COL_WIDTH, m_colWidth);
        settings.addString(ActiveLabelingConfig.CFG_LABEL_COL, m_labelCol);
        settings.addBoolean(ActiveLabelingConfig.CFG_USE_ROW_ID, m_useRowID);
        settings.addBoolean(ActiveLabelingConfig.CFG_ALIGN_LEFT, m_alignLeft);
        settings.addBoolean(ActiveLabelingConfig.CFG_ALIGN_RIGHT, m_alignRight);
        settings.addBoolean(ActiveLabelingConfig.CFG_ALIGN_CENTER, m_alignCenter);
        settings.addStringArray(ActiveLabelingConfig.CFG_POSSIBLE_VALUES, m_possibleLabelValues);
        settings.addString(ActiveLabelingConfig.CFG_COLOR_SCHEME, m_colorScheme);
        //		settings.addBoolean(ActiveLearningConfig.CFG_USE_SECOND_PORT, m_useSecondInputPort);
        //		settings.addStringArray(ActiveLearningConfig.CFG_SECOND_PORT_VALUES, m_secondPortLabelValues);
        // TODO label creation =adding labels dynamically?
        settings.addBoolean(ActiveLabelingConfig.CFG_ADD_LABELS_DYNAMICALLY, m_labelCreation);
        settings.addBoolean(ActiveLabelingConfig.CFG_USE_PROGRESS_BAR, m_useProgressBar);
        settings.addBoolean(ActiveLabelingConfig.CFG_AUTO_SELECT_NEXT_TILE, m_autoSelectNextTile);
        settings.addString(ActiveLabelingConfig.CFG_REPLACE_COL, m_replaceCol);
        settings.addString(ActiveLabelingConfig.CFG_APPEND_COL,  m_appendCol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonIgnore
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
        m_useNumCols = settings.getBoolean(ActiveLabelingConfig.CFG_USE_NUM_COLS);
        m_useColWidth = settings.getBoolean(ActiveLabelingConfig.CFG_USE_COL_WIDTH);
        m_numCols = settings.getInt(ActiveLabelingConfig.CFG_NUM_COLS);
        m_colWidth = settings.getInt(ActiveLabelingConfig.CFG_COL_WIDTH);
        m_labelCol = settings.getString(ActiveLabelingConfig.CFG_LABEL_COL);
        m_useRowID = settings.getBoolean(ActiveLabelingConfig.CFG_USE_ROW_ID);
        m_alignLeft = settings.getBoolean(ActiveLabelingConfig.CFG_ALIGN_LEFT);
        m_alignRight = settings.getBoolean(ActiveLabelingConfig.CFG_ALIGN_RIGHT);
        m_alignCenter = settings.getBoolean(ActiveLabelingConfig.CFG_ALIGN_CENTER);
        m_labelCreation = settings.getBoolean(ActiveLabelingConfig.CFG_ADD_LABELS_DYNAMICALLY);
        m_useProgressBar = settings.getBoolean(ActiveLabelingConfig.CFG_USE_PROGRESS_BAR);
        m_autoSelectNextTile = settings.getBoolean(ActiveLabelingConfig.CFG_AUTO_SELECT_NEXT_TILE);
        m_replaceCol = settings.getString(ActiveLabelingConfig.CFG_REPLACE_COL);
        m_appendCol = settings.getString(ActiveLabelingConfig.CFG_APPEND_COL);
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
        final ActiveLabelingViewRepresentation other = (ActiveLabelingViewRepresentation)obj;
        return new EqualsBuilder().append(m_settings, other.m_settings).append(m_useNumCols, other.getUseNumCols())
            .append(m_useColWidth, other.getUseColWidth()).append(m_numCols, other.getNumCols())
            .append(m_colWidth, other.getColWidth()).append(m_labelCol, other.getLabelCol())
            .append(m_useRowID, other.getUseRowID()).append(m_alignLeft, other.getAlignLeft())
            .append(m_alignRight, other.getAlignRight()).append(m_alignCenter, other.getAlignCenter())
            .append(m_useProgressBar, other.getUseProgressBar()).append(m_labelCreation, other.m_labelCreation)
            .append(m_autoSelectNextTile, other.isAutoSelectNextTile()).append(m_replaceCol,  other.m_replaceCol)
            .append(m_appendCol, other.m_appendCol).isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_settings).append(m_useNumCols).append(m_useColWidth).append(m_numCols)
            .append(m_colWidth).append(m_labelCol).append(m_useRowID).append(m_alignLeft).append(m_alignRight)
            .append(m_alignCenter).append(m_useProgressBar).append(m_labelCreation).append(m_autoSelectNextTile)
            .append(m_replaceCol).append(m_appendCol).toHashCode();
    }
}
