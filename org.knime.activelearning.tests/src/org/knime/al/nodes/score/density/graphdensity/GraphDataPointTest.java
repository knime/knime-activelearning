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
 *   Aug 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density.graphdensity;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.RowKey;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class GraphDataPointTest {

    private static GraphDataPoint create(final String key, final double... values) {
        return new GraphDataPoint(new RowKey(key), values);
    }

    private GraphDataPoint m_testInstance;

    GraphDataPoint m_n1 = create("n1", 0, 0);
    GraphDataPoint m_n2 = create("n2", 0, 0);

    @Before
    public void init() {
        m_testInstance = create("key", 1, 1);
    }

    @Test
    public void testRegisterNeighbor() throws Exception {
        m_testInstance.registerNeighbor(m_n1, 1.0);
        m_testInstance.registerNeighbor(m_n2, 2.0);
        // should be ignored
        m_testInstance.registerNeighbor(m_n1, 7.0);

        Collection<GraphDataPoint> neighbors = m_testInstance.getNeighbors();

        assertEquals(2, neighbors.size());
        Iterator<GraphDataPoint> iter = neighbors.iterator();
        assertEquals(m_n1, iter.next());
        assertEquals(m_n2, iter.next());
        assertEquals(3, m_testInstance.getDensity(), 1e-6);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testRegisterNeighborFailsOnSelfRegister() throws Exception {
        m_testInstance.registerNeighbor(m_testInstance, 0);
    }

    @Test
    public void testNormalizeDensity() throws Exception {
        m_testInstance.registerNeighbor(m_n1, 1.0);
        m_testInstance.registerNeighbor(m_n2, 2.0);
        assertEquals(3, m_testInstance.getDensity(), 1e-6);
        m_testInstance.normalizeDensity();
        assertEquals(1.5, m_testInstance.getDensity(), 1e-6);
        // only the first call to normalize should normalize the density
        m_testInstance.normalizeDensity();
        assertEquals(1.5, m_testInstance.getDensity(), 1e-6);
    }
}
