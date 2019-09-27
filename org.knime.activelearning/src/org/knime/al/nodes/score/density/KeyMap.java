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
 *   Sep 27, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Maps from {@link RowKey RowKeys} to integer indices.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class KeyMap implements Externalizable {

    private Map<String, Integer> m_keyMap;

    /**
     *
     */
    private KeyMap(final Map<String, Integer> keyMap) {
        m_keyMap = keyMap;
    }

    /**
     * Framework constructor for serialization.
     *
     * @noreference Not intended for use in client code
     */
    public KeyMap() {
    }

    /**
     * @param dataPoints to create a KeyMap for
     * @param monitor for reporting progress
     * @return a {@link KeyMap} containing the keys of the data points in <b>dataPoints</b>
     * @throws CanceledExecutionException
     */
    public static KeyMap create(final Collection<? extends DensityDataPoint<?>> dataPoints,
        final ExecutionMonitor monitor) throws CanceledExecutionException {
        final Map<String, Integer> keyMap = new LinkedHashMap<>(dataPoints.size());
        ProcessingUtil.collectWithProgress(dataPoints, (i, p) -> keyMap.put(p.getKey().getString(), i),
            ProcessingUtil.progressWithTemplate(monitor, "Reading key for row %s of %s."));
        return new KeyMap(keyMap);
    }

    /**
     * @param key the {@link RowKey} for which the index is required
     * @return the index for {@link RowKey key}
     * @throws UnknownRowException if {@link RowKey key} is unknown
     */
    public int getIndex(final RowKey key) throws UnknownRowException {
        final Integer index = m_keyMap.get(key.toString());
        if (index == null) {
            throw new UnknownRowException(key);
        }
        return index.intValue();
    }

    /**
     * @return the number of stored keys
     */
    public int size() {
        return m_keyMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        final Map<String, Integer> keyMap = (Map<String, Integer>)in.readObject();
        m_keyMap = keyMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(m_keyMap);
    }

}
