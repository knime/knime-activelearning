package org.knime.al.nodes.labelingWidget;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.node.table.TableConfig;
import org.knime.js.core.settings.table.TableSettings;

public class ActiveLabelingConfig implements TableConfig {

    static final String CFG_USE_NUM_COLS = "useNumCols";

    private static final boolean DEFAULT_USE_NUM_COLS = true;

    private boolean m_useNumCols = DEFAULT_USE_NUM_COLS;

    static final String CFG_USE_COL_WIDTH = "useColWidth";

    private static final boolean DEFAULT_USE_COL_WIDTH = false;

    private boolean m_useColWidth = DEFAULT_USE_COL_WIDTH;

    static final int INITIAL_PAGE_SIZE = 1;

    static final int[] ALLOWED_PAGE_SIZE = new int[]{1, 3, 6, 9, 20};

    static final String CFG_NUM_COLS = "numCols";

    static final int MIN_NUM_COLS = 1;

    static final int MAX_NUM_COLS = 100;

    static final int DEFAULT_NUM_COLS = 1;

    private int m_numCols = DEFAULT_NUM_COLS;

    static final String CFG_COL_WIDTH = "colWidth";

    static final int MIN_COL_WIDTH = 30;

    static final int MAX_COL_WIDTH = 5000;

    static final int DEFAULT_COL_WIDTH = 280;

    private int m_colWidth = DEFAULT_COL_WIDTH;

    static final String CFG_LABEL_COL = "labelCol";

    private static final String DEFAULT_LABEL_COL = null;

    private String m_labelCol = DEFAULT_LABEL_COL;

    static final String CFG_USE_EXISTING_LABELS = "useExistingLabels";
    // this actually controls ignoring existing labels, so false keeps labels
    private static final boolean DEFAULT_USE_EXISTING_LABELS = false;

    private boolean m_useExistingLabels = DEFAULT_USE_EXISTING_LABELS;

    static final String CFG_USE_ROW_ID = "useRowID";

    private static final boolean DEFAULT_USE_ROW_ID = false;

    private boolean m_useRowID = DEFAULT_USE_ROW_ID;

    static final String CFG_ALIGN_LEFT = "alignLeft";

    private static final boolean DEFAULT_ALIGN_LEFT = true;

    private boolean m_alignLeft = DEFAULT_ALIGN_LEFT;

    static final String CFG_ALIGN_RIGHT = "alignRight";

    private static final boolean DEFAULT_ALIGN_RIGHT = false;

    private boolean m_alignRight = DEFAULT_ALIGN_RIGHT;

    static final String CFG_ALIGN_CENTER = "alignCenter";

    private static final boolean DEFAULT_ALIGN_CENTER = false;

    private boolean m_alignCenter = DEFAULT_ALIGN_CENTER;

    static final String CFG_POSSIBLE_VALUES = "possibleValues";

    private static final String[] DEFAULT_POSSIBLE_VALUES = new String[0];

    private String[] m_possibleValues = DEFAULT_POSSIBLE_VALUES;

    static final String CFG_REPLACE_COL = "replaceCol";

    private static final String DEFAULT_REPLACE_COL = new String();

    private String m_replaceCol = DEFAULT_REPLACE_COL;

    static final String CFG_REPLACE_RADIO = "isReplaceRadio";

    private static final boolean DEFAULT_REPLACE_RADIO = false;

    private boolean m_isReplaceRadio = DEFAULT_REPLACE_RADIO;

    static final String CFG_APPEND_COL = "appendCol";

    private static final String DEFAULT_APPEND_COL = "Output_Column";

    private String m_appendCol = DEFAULT_APPEND_COL;

    static final String CFG_APPEND_RADIO = "isAppendRadio";

    private static final boolean DEFAULT_APPEND_RADIO = true;

    private boolean m_isAppendRadio = DEFAULT_APPEND_RADIO;

    static final String CFG_COLOR_SCHEME = "colorScheme";

    private static final String DEFAULT_COLOR_SCHEME = "Scheme 1";

    private String m_colorScheme = DEFAULT_COLOR_SCHEME;

    static final String CFG_ADD_LABELS_DYNAMICALLY = "addLabelsDynamically";

    private static final boolean DEFAULT_ADD_LABELS_DYNAMICALLY = false;

    private boolean m_addLabelsDynamically = DEFAULT_ADD_LABELS_DYNAMICALLY;

    static final String CFG_USE_PROGRESS_BAR = "useProgressBar";

    private static final boolean DEFAULT_USE_PROGRESS_BAR = true;

    private boolean m_useProgressBar = DEFAULT_USE_PROGRESS_BAR;

    static final String CFG_AUTO_SELECT_NEXT_TILE = "autoSelectNextTile";

    private static final boolean DEFAULT_AUTO_SELECT_NEXT_TILE = true;

    private boolean m_autoSelectNextTile = DEFAULT_AUTO_SELECT_NEXT_TILE;

    private TableSettings m_settings = new TableSettings();

    @SuppressWarnings("javadoc")
    public ActiveLabelingConfig() {
        super();
        m_settings.getRepresentationSettings().setInitialPageSize(INITIAL_PAGE_SIZE);
        m_settings.getRepresentationSettings().setAllowedPageSizes(ALLOWED_PAGE_SIZE);
        m_settings.setSelectionColumnName("Selected (General Labeling Widget)");
    }

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
     * @return the useExistingLabels
     */
    public boolean getUseExistingLabels() {
        return m_useExistingLabels;
    }

    /**
     * @param useExistingLabels the useExistingLabels to set
     */
    public void setUseExistingLabels(final boolean useExistingLabels) {
        m_useExistingLabels = useExistingLabels;
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
     * @return true if progress bar should be used
     */
    public boolean getUseProgressBar() {
        return m_useProgressBar;
    }

    /**
     * @param useProgressBar true if progress bar should be used
     */
    public void setUseProgressBar(final boolean useProgressBar) {
        m_useProgressBar = useProgressBar;
    }

    /**
     * @return true if next tile should be automatically selected
     */
    public boolean isAutoSelectNextTile() {
        return m_autoSelectNextTile;
    }

    /**
     * @param useProgressBar true if progress bar should be used
     */
    public void setAutoSelectNextTile(final boolean autoSelectNextTile) {
        m_autoSelectNextTile = autoSelectNextTile;
    }

    /**
     * @return the m_possibleValues
     */
    public String[] getPossibleValues() {
        return m_possibleValues;
    }

    /**
     * @param possibleValues the possibleValues to set
     */
    public void setPossibleValues(final String[] possibleValues) {
        this.m_possibleValues = possibleValues;
    }

    /**
     * @return the m_replaceCol
     */
    public String getReplaceCol() {
        return m_replaceCol;
    }

    /**
     * @param replaceCol the column to replace
     */
    public void setReplaceCol(final String replaceCol) {
        this.m_replaceCol = replaceCol;
    }

    /**
     * @return the m_isReplaceRadio
     */
    public boolean isReplaceRadio() {
        return m_isReplaceRadio;
    }

    /**
     * @param isReplaceRadio true if column should be replaced
     */
    public void setReplaceRadio(final boolean isReplaceRadio) {
        this.m_isReplaceRadio = isReplaceRadio;
    }

    /**
     * @return the m_appendeCol
     */
    public String getAppendCol() {
        return m_appendCol;
    }

    /**
     * @param appendCol the column to append
     */
    public void setAppendCol(final String appendCol) {
        this.m_appendCol = appendCol;
    }

    /**
     * @return the m_isAppendRadio
     */
    public boolean isAppendRadio() {
        return m_isAppendRadio;
    }

    /**
     * @param isAppendRadio the possibleValues to set
     */
    public void setAppendRadio(final boolean isAppendRadio) {
        this.m_isAppendRadio = isAppendRadio;
    }

    /**
     * @return the m_colorScheme
     */
    public String getColorScheme() {
        return m_colorScheme;
    }

    /**
     * @param colorScheme the colorScheme to set
     */
    public void setColorScheme(final String colorScheme) {
        this.m_colorScheme = colorScheme;
    }

    /**
     * @return the addLabelsDynamically
     */
    public boolean isAddLabelsDynamically() {
        return m_addLabelsDynamically;
    }

    /**
     * @param addLabelsDynamically the addLabelsDynamically to set
     */
    public void setAddLabelsDynamically(final boolean addLabelsDynamically) {
        this.m_addLabelsDynamically = addLabelsDynamically;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableSettings getSettings() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettings(final TableSettings settings) {
        m_settings = settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
        settings.addBoolean(CFG_USE_NUM_COLS, m_useNumCols);
        settings.addBoolean(CFG_USE_COL_WIDTH, m_useColWidth);
        settings.addInt(CFG_NUM_COLS, m_numCols);
        settings.addInt(CFG_COL_WIDTH, m_colWidth);
        settings.addString(CFG_LABEL_COL, m_labelCol);
        settings.addBoolean(CFG_USE_ROW_ID, m_useRowID);
        settings.addBoolean(CFG_ALIGN_LEFT, m_alignLeft);
        settings.addBoolean(CFG_ALIGN_RIGHT, m_alignRight);
        settings.addBoolean(CFG_ALIGN_CENTER, m_alignCenter);
        settings.addBoolean(CFG_USE_PROGRESS_BAR, m_useProgressBar);
        settings.addBoolean(CFG_AUTO_SELECT_NEXT_TILE, m_autoSelectNextTile);
        settings.addBoolean(CFG_ADD_LABELS_DYNAMICALLY, m_addLabelsDynamically);
        settings.addString(CFG_COLOR_SCHEME, m_colorScheme);
        settings.addStringArray(CFG_POSSIBLE_VALUES, m_possibleValues);
        settings.addString(CFG_REPLACE_COL, m_replaceCol);
        settings.addBoolean(CFG_APPEND_RADIO, m_isAppendRadio);
        settings.addString(CFG_APPEND_COL, m_appendCol);
        settings.addBoolean(CFG_REPLACE_RADIO, m_isReplaceRadio);
        settings.addBoolean(CFG_USE_EXISTING_LABELS, m_useExistingLabels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final int numCols = settings.getInt(CFG_NUM_COLS);
        final int colWidth = settings.getInt(CFG_COL_WIDTH);
        final int initPageSize = settings.getInt("initialPageSize");
        final int maxRows = settings.getInt("maxRows");
        final int decimalPlaces = settings.getInt("globalNumberFormatDecimals");
        final boolean enableNumCols = settings.getBoolean(CFG_USE_NUM_COLS);
        final boolean enableColWidth = settings.getBoolean(CFG_USE_COL_WIDTH);
        final boolean enablePaging = settings.getBoolean("enablePaging");
        final boolean enableDecimalPlaces = settings.getBoolean("enableGlobalNumberFormat");
        validateConfig(numCols, colWidth, initPageSize, maxRows, decimalPlaces, enableNumCols, enableColWidth,
            enablePaging, enableDecimalPlaces);

        m_settings.loadSettings(settings);
        m_useNumCols = settings.getBoolean(CFG_USE_NUM_COLS);
        m_useColWidth = settings.getBoolean(CFG_USE_COL_WIDTH);
        m_labelCol = settings.getString(CFG_LABEL_COL);
        m_useExistingLabels = settings.getBoolean(CFG_USE_EXISTING_LABELS);
        m_useRowID = settings.getBoolean(CFG_USE_ROW_ID);
        m_alignLeft = settings.getBoolean(CFG_ALIGN_LEFT);
        m_alignRight = settings.getBoolean(CFG_ALIGN_RIGHT);
        m_alignCenter = settings.getBoolean(CFG_ALIGN_CENTER);
        m_addLabelsDynamically = settings.getBoolean(CFG_ADD_LABELS_DYNAMICALLY);
        m_colorScheme = settings.getString(CFG_COLOR_SCHEME);
        m_possibleValues = settings.getStringArray(CFG_POSSIBLE_VALUES);
        m_replaceCol = settings.getString(CFG_REPLACE_COL);
        m_appendCol = settings.getString(CFG_APPEND_COL);
        m_isReplaceRadio = settings.getBoolean(CFG_REPLACE_RADIO);
        m_isAppendRadio = settings.getBoolean(CFG_APPEND_RADIO);
        m_useProgressBar = settings.getBoolean(CFG_USE_PROGRESS_BAR);
        m_autoSelectNextTile = settings.getBoolean(CFG_AUTO_SELECT_NEXT_TILE);
        m_numCols = numCols;
        m_colWidth = colWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        m_settings.loadSettingsForDialog(settings, spec);
        m_useNumCols = settings.getBoolean(CFG_USE_NUM_COLS, DEFAULT_USE_NUM_COLS);
        m_useColWidth = settings.getBoolean(CFG_USE_COL_WIDTH, DEFAULT_USE_COL_WIDTH);
        m_numCols = settings.getInt(CFG_NUM_COLS, DEFAULT_NUM_COLS);
        m_colWidth = settings.getInt(CFG_COL_WIDTH, DEFAULT_COL_WIDTH);
        m_labelCol = settings.getString(CFG_LABEL_COL, DEFAULT_LABEL_COL);
        m_useExistingLabels = settings.getBoolean(CFG_USE_EXISTING_LABELS, DEFAULT_USE_EXISTING_LABELS);
        m_useRowID = settings.getBoolean(CFG_USE_ROW_ID, DEFAULT_USE_ROW_ID);
        m_alignLeft = settings.getBoolean(CFG_ALIGN_LEFT, DEFAULT_ALIGN_LEFT);
        m_alignRight = settings.getBoolean(CFG_ALIGN_RIGHT, DEFAULT_ALIGN_RIGHT);
        m_alignCenter = settings.getBoolean(CFG_ALIGN_CENTER, DEFAULT_ALIGN_CENTER);
        m_addLabelsDynamically = settings.getBoolean(CFG_ADD_LABELS_DYNAMICALLY, DEFAULT_ADD_LABELS_DYNAMICALLY);
        m_colorScheme = settings.getString(CFG_COLOR_SCHEME, DEFAULT_COLOR_SCHEME);
        m_possibleValues = settings.getStringArray(CFG_POSSIBLE_VALUES, DEFAULT_POSSIBLE_VALUES);
        m_replaceCol = settings.getString(CFG_REPLACE_COL, DEFAULT_REPLACE_COL);
        m_appendCol = settings.getString(CFG_APPEND_COL, DEFAULT_APPEND_COL);
        m_isReplaceRadio = settings.getBoolean(CFG_REPLACE_RADIO, DEFAULT_REPLACE_RADIO);
        m_isAppendRadio = settings.getBoolean(CFG_APPEND_RADIO, DEFAULT_APPEND_RADIO);
        m_useProgressBar = settings.getBoolean(CFG_USE_PROGRESS_BAR, DEFAULT_USE_PROGRESS_BAR);
        m_autoSelectNextTile = settings.getBoolean(CFG_AUTO_SELECT_NEXT_TILE, DEFAULT_AUTO_SELECT_NEXT_TILE);
    }

    static void validateConfig(final int numCols, final int colWidth, final int initPageSize, final int maxRows,
        final int decimalPlaces, final boolean enableNumCols, final boolean enableColWidth, final boolean enablePaging,
        final boolean enableDecimalPlaces) throws InvalidSettingsException {
        String errorMsg = "";
        if (maxRows < 0) {
            errorMsg += "No. of rows to display (" + maxRows + ") cannot be negative.\n";
        }
        if ((numCols < MIN_NUM_COLS || numCols > MAX_NUM_COLS) && enableNumCols) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n";
            }
            errorMsg += "Invalid number of tiles per row, expected an integer between " + MIN_NUM_COLS + " and "
                + MAX_NUM_COLS + " but received " + numCols + ".\n";
        }
        if ((colWidth < MIN_COL_WIDTH || colWidth > MAX_COL_WIDTH) && enableColWidth) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n";
            }
            errorMsg += "Invalid tile width, expected an integer between 3" + MIN_COL_WIDTH + " and " + MAX_COL_WIDTH
                + " but received " + colWidth + ".\n";
        }
        if (initPageSize < 1 && enablePaging) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n";
            }
            errorMsg += "Initial page size (" + initPageSize + ") cannot be less than 1.\n";
        }
        if (numCols > initPageSize && enablePaging && enableNumCols) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n";
            }
            errorMsg += "The number of tiles per row (" + numCols + ") cannot be greater than the initial page size ("
                + initPageSize + "). Check the \"Options\" and \"Interactivity\" tabs.\n";
        }
        if (decimalPlaces < 0 && enableDecimalPlaces) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n";
            }
            errorMsg += "Decimal places (" + decimalPlaces + ") cannot be negative.\n";
        }
        if (!errorMsg.isEmpty()) {
            throw new InvalidSettingsException(errorMsg);
        }
    }
}
