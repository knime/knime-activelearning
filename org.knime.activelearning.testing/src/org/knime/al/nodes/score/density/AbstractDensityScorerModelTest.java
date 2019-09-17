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
package org.knime.al.nodes.score.density;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.RowKey;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractDensityScorerModelTest {

    public interface TestDataPoint extends DensityDataPoint<TestDataPoint> {
        // dummy marker interface
    }

    private static class TestDensityScorerModel extends AbstractDensityScorerModel<TestDataPoint> {

        TestDensityScorerModel(final List<TestDataPoint> datapoints) {
            super(datapoints);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected double calculateDecrementWeight(final int idx, final int neighborIdx) {
            return 1.0;
        }

    }

    private TestDensityScorerModel m_testInstance;

    @Mock
    private TestDataPoint m_p1;

    @Mock
    private TestDataPoint m_p2;

    @Mock
    private TestDataPoint m_p3;

    private List<TestDataPoint> m_points;

    private void setupMock(final TestDataPoint mock, final String key, final double density,
        final TestDataPoint... neighbors) {
        Mockito.when(mock.getDensity()).thenReturn(density);
        Mockito.when(mock.getKey()).thenReturn(new RowKey(key));
        Mockito.when(mock.getNeighbors()).thenReturn(Arrays.stream(neighbors).collect(Collectors.toList()));
    }

    @Before
    public void init() {
        setupMock(m_p1, "p1", 1.0, m_p2);
        setupMock(m_p2, "p2", 2.0, m_p1);
        setupMock(m_p3, "p3", 0.0);
        m_points = Lists.newArrayList(m_p1, m_p2, m_p3);
        m_testInstance = new TestDensityScorerModel(m_points);
    }

    @Test
    public void testGetPotential() throws Exception {
        for (TestDataPoint p : m_points) {
            assertEquals(p.getDensity(), m_testInstance.getPotential(p.getKey()), 0);
        }
    }

    @Test
    public void testGetPotentialProtected() throws Exception {
        IntStream.range(0, m_points.size())
            .forEachOrdered(i -> assertEquals(m_points.get(i).getDensity(), m_testInstance.getPotential(i), 0));
    }

    @Test
    public void testGetIdx() throws Exception {
        for (int i = 0; i < m_points.size(); i++) {
            assertEquals(i, m_testInstance.getIdx(m_points.get(i).getKey()));
        }
    }

    @Test
    public void testGetNeighbors() throws Exception {
        assertArrayEquals(new int[]{1}, m_testInstance.getNeighbors(0));
        assertArrayEquals(new int[]{0}, m_testInstance.getNeighbors(1));
        assertArrayEquals(new int[0], m_testInstance.getNeighbors(2));
    }

    @Test
    public void testGetNrRows() throws Exception {
        assertEquals(m_points.size(), m_testInstance.getNrRows());
    }

}
