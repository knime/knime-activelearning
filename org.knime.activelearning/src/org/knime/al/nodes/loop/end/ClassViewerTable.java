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
 *   14 Jan 2015 (<a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>): created
 */
package org.knime.al.nodes.loop.end;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

/*
 * Table model for the hiliteTable
 *
 * @author Jonathan Hale
 */
class ClassViewerTable extends AbstractTableModel {
    /**
     *
     */
    private final ActiveLearnLoopEndNodeViewListener m_listener;

    private static final long serialVersionUID = 1956861398744750844L;

    private final List<DataRow> m_rows;
    private String[] m_columnNames;

    private int m_classColumnIndex = -1;

    /**
     * Default Constructor of HiliteTableModel
     *
     * @param listener
     *            the listener
     */
    public ClassViewerTable(final ActiveLearnLoopEndNodeViewListener listener) {
        super();
        m_listener = listener;

        m_rows = new ArrayList<DataRow>();
    }

    /**
     * Update the tables entries with a set of keys.
     *
     * @param keys
     *            the
     * @param dataMap
     * @param columnNames
     */
    public void updateEntries(final Set<RowKey> keys,
            final Map<RowKey, DataRow> dataMap, final String[] columnNames) {
        if ((keys == null) || (dataMap == null)) {
            return;
        }

        // one extra for row key
        m_columnNames = new String[columnNames.length + 1];
        m_columnNames[0] = "RowID";

        for (int i = 0; i < columnNames.length; i++) {
            m_columnNames[i + 1] = columnNames[i];
        }

        // FIXME: UGLY UGLY CODE
        for (final RowKey rowKey : dataMap.keySet()) {
            if (keys.contains(rowKey)) {
                boolean alreadyExists = false;
                for (final DataRow dataRow : m_rows) {
                    if (dataRow.getKey() == rowKey) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    m_rows.add(dataMap.get(rowKey));
                }
            }
        }

        fireTableStructureChanged(); // notify to redraw everything
    }

    /**
     * tells the HiliteTable which column is the class column With the next call
     * of updateEntities, the class column index is found.
     *
     * @param columnName
     * @param index
     */
    public void setClassColumn(final String columnName, final int index) {
        m_classColumnIndex = index + 1; // because of RowID in the beginning
    }

    /**
     * getClassColumnIndex
     *
     * @return the column index of the class column
     */
    public int getClassColumnIndex() {
        return m_classColumnIndex;
    }

    /**
     * Clears all current entries in the table
     */
    public void clearEntries() {
        m_rows.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        if (m_columnNames == null) {
            return 0;
        }

        return m_columnNames.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_rows.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int row, final int col) {
        if (m_rows.size() == 0) { // this will probably not be called if,
            // but making sure anyway
            return null;
        }

        if (col == 0) {
            return getRowKeyOf(row);
        } else if (col == m_classColumnIndex) {
            return m_listener.m_classMap.get(getRowKeyOf(row));
        }

        final DataRow data = m_rows.get(row); // get the row
        return data.getCell(col - 1); // return the column (<-1> because
        // first column is RowID and is not
        // contained in data)
    }

    /**
     * Returns the RowKey of the given row index
     *
     * @param row
     * @return the RowKey of the row'th row
     */
    public RowKey getRowKeyOf(final int row) {
        if ((row >= m_rows.size()) || (row < 0)) {
            return null; // Out of bounds
        }
        return m_rows.get(row).getKey();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Class getColumnClass(final int c) {
        return getValueAt(0, c).getClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int column) {
        return m_columnNames[column];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int column) {
        if (column == m_classColumnIndex) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object value, final int row,
            final int column) {
        if (column == m_classColumnIndex) {
            m_listener.setClass(getRowKeyOf(row), (String) value);

            fireTableCellUpdated(row, column);
        }
    }

    /**
     * @param classMap
     *            the class map
     */
    public void setClassmap(final Map<RowKey, String> classMap) {
    }
}