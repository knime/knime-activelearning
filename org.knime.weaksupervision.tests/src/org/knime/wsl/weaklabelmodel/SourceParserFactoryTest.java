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
 *   Sep 17, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.nominal.NominalDistributionCellFactory;
import org.knime.core.data.probability.nominal.NominalDistributionValue;

import com.google.common.collect.Sets;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class SourceParserFactoryTest {

    private static final String[] CLASSES = new String[]{"A", "B", "C"};

    private SourceParserFactory m_testInstance;

    private static StringCell sc(final String value) {
        return new StringCell(value);
    }

    @Before
    public void init() {
        m_testInstance = new SourceParserFactory(CLASSES);
    }

    @Test
    public void testNominalValueParser() throws Exception {
        final SourceParser sp = createNominalSourceParser();
        for (int i = 0; i < CLASSES.length; i++) {
            final StringCell label = sc(CLASSES[i]);
            for (int j = 0; j < CLASSES.length; j++) {
                assertEquals(i == j ? 1.0 : 0.0, sp.parseProbability(label, j), 0);
            }
        }
        testParsingOfMissingValue(sp);
    }

    private static void testParsingOfMissingValue(final SourceParser sp) {
        final DataCell missingCell = new MissingCell("missing");
        for (int i = 0; i < CLASSES.length; i++) {
            assertEquals(0.0, sp.parseProbability(missingCell, i), 0);
        }
    }

    private SourceParser createNominalSourceParser() {
        final DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator("test", StringCell.TYPE);
        return m_testInstance.create(colSpecCreator.createSpec());
    }

    @Test(expected = IllegalStateException.class)
    public void testNominalValueParserFailsOnUnknownLabel() throws Exception {
        final SourceParser sp = createNominalSourceParser();
        sp.parseProbability(sc("Z"), 0);
    }

    @Test
    public void testProbabilityDistributionParser() throws Exception {
        final SourceParser sp = createProbabilitySourceParser();
        testParsingOfMissingValue(sp);
        testProbabilityRetrieval(pdc(0.1, 0.9, 0.0), sp);
        testProbabilityRetrieval(pdc(0.0, 0.0, 1.0), sp);
        testProbabilityRetrieval(pdc(0.4, 0.3, 0.3), sp);
    }

    private SourceParser createProbabilitySourceParser() {
        final DataColumnSpecCreator colSpecCreator =
            new DataColumnSpecCreator("test", NominalDistributionCellFactory.TYPE);
        final SourceParser sp = m_testInstance.create(colSpecCreator.createSpec());
        return sp;
    }

    private static void testProbabilityRetrieval(final DataCell cell, final SourceParser parser) {
        assert cell instanceof NominalDistributionValue;
        NominalDistributionValue pdc = (NominalDistributionValue)cell;
        for (int i = 0; i < CLASSES.length; i++) {
            assertEquals(pdc.getProbability(CLASSES[i]), parser.parseProbability(cell, i), 1e-6);
        }
    }

    @SuppressWarnings("serial")
    private static class MockNominalDistributionCell extends DataCell implements NominalDistributionValue {

        private final double[] m_probs;

        private final String[] m_values;

        private final int m_maxIdx;

        MockNominalDistributionCell(final String[] values, final double[] probs) {
            assert probs.length == values.length;
            m_probs = probs;
            m_values = values;
            int maxIdx = 0;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < m_probs.length; i++) {
                if (m_probs[i] > max) {
                    max = m_probs[i];
                    maxIdx = i;
                }
            }
            m_maxIdx = maxIdx;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public double getProbability(final String value) {
            for (int i = 0; i < m_values.length; i++) {
                if (value.equals(m_values[i])) {
                    return m_probs[i];
                }
            }
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMostLikelyValue() {
            return m_values[m_maxIdx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getMaximalProbability() {
            return m_probs[m_maxIdx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isKnown(final String value) {
            return Arrays.stream(m_values).anyMatch(v -> value.equals(v));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<String> getKnownValues() {
            return Sets.newHashSet(m_values);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Arrays.toString(m_probs);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return 31 + Arrays.hashCode(m_probs) + Arrays.hashCode(m_values);
        }

    }

    private static DataCell pdc(final double... values) {
        return new MockNominalDistributionCell(CLASSES, values);
    }
}
