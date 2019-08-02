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
 *   Jul 30, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.labelmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class MetaData {

    private final Set<DataCell> m_possibleClasses;

    private final int[] m_nonEmptyPossibleValues;

    private final List<String> m_emptyColumns;

    private final Set<String> m_nonEmptyColumns;

    private final Map<DataCell, Integer> m_valueToIdxMap;

    MetaData(final String[] sourceColumnNames, final DataTableSpec spec) {
        m_possibleClasses = new LinkedHashSet<>();
        final List<Integer> nonEmptyPossibleValues = new ArrayList<>();
        m_emptyColumns = new ArrayList<>();
        m_nonEmptyColumns = new LinkedHashSet<>();
        for (int i = 0; i < sourceColumnNames.length; i++) {
            final String name = sourceColumnNames[i];
            final DataColumnSpec colSpec = spec.getColumnSpec(name);
            final Set<DataCell> possibleValues = colSpec.getDomain().getValues();
            if (possibleValues == null || possibleValues.isEmpty()) {
                m_emptyColumns.add(name);
            } else {
                m_nonEmptyColumns.add(name);
                m_possibleClasses.addAll(possibleValues);
                nonEmptyPossibleValues.add(spec.findColumnIndex(name));
            }
        }
        m_nonEmptyPossibleValues = nonEmptyPossibleValues.stream().mapToInt(Integer::intValue).toArray();
        m_valueToIdxMap = createValueToIdxMap();
    }

    private Map<DataCell, Integer> createValueToIdxMap() {
        final Set<DataCell> possibleClasses = getPossibleClasses();
        final Map<DataCell, Integer> valueToIdx = new HashMap<>(possibleClasses.size());
        int idx = 0;
        for (DataCell cell : possibleClasses) {
            valueToIdx.put(cell, idx);
            idx++;
        }
        return valueToIdx;
    }

    Set<DataCell> getPossibleClasses() {
        return m_possibleClasses;
    }

    int[] getNonEmptyIndices() {
        return m_nonEmptyPossibleValues;
    }

    List<String> getEmptyColumns() {
        return m_emptyColumns;
    }

    Set<String> getNonEmptyColumns() {
        return m_nonEmptyColumns;
    }

    int getCardinality() {
        return m_possibleClasses.size();
    }

    Map<DataCell, Integer> getLabelToIdxMap() {
        return m_valueToIdxMap;
    }

    int getNumSources() {
        return m_nonEmptyPossibleValues.length;
    }
}
