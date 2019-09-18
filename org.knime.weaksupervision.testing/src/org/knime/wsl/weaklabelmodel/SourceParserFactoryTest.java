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

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.ProbabilityDistributionCell;
import org.knime.core.data.probability.ProbabilityDistributionCellFactory;

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
        m_testInstance = new SourceParserFactory(
            IntStream.range(0, CLASSES.length).boxed().collect(Collectors.toMap(i -> CLASSES[i], Function.identity())));
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
            new DataColumnSpecCreator("test", ProbabilityDistributionCellFactory.TYPE);
        colSpecCreator.setElementNames(CLASSES);
        final SourceParser sp = m_testInstance.create(colSpecCreator.createSpec());
        return sp;
    }

    private static void testProbabilityRetrieval(final ProbabilityDistributionCell pdc, final SourceParser parser) {
        for (int i = 0; i < CLASSES.length; i++) {
            assertEquals(pdc.getProbability(i), parser.parseProbability(pdc, i), 1e-6);
        }
    }

    private static ProbabilityDistributionCell pdc(final double... values) {
        return ProbabilityDistributionCellFactory.createCell(values);
    }
}
