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
 *   Aug 1, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel.learner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;

import com.google.common.collect.Sets;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class MetaDataTest {

    private static MetaData META_DATA;

    private static DataCell[] CLASSES;

    @BeforeClass
    public static void initClass() {
        CLASSES = Stream.of("A", "B", "C").map(StringCell::new).toArray(DataCell[]::new);
        DataColumnSpecCreator otherCol = new DataColumnSpecCreator("other", DoubleCell.TYPE);
        DataColumnSpecCreator lf1 = new DataColumnSpecCreator("lf1", StringCell.TYPE);
        lf1.setDomain(
            new DataColumnDomainCreator(Arrays.stream(CLASSES).limit(2).toArray(DataCell[]::new)).createDomain());
        DataColumnSpecCreator lf2 = new DataColumnSpecCreator("lf2", StringCell.TYPE);
        lf2.setDomain(
            new DataColumnDomainCreator(Arrays.stream(CLASSES).skip(1).toArray(DataCell[]::new)).createDomain());
        DataColumnSpecCreator lf3 = new DataColumnSpecCreator("lf3", StringCell.TYPE);
        lf3.setDomain(
            new DataColumnDomainCreator(Arrays.stream(CLASSES).limit(1).toArray(DataCell[]::new)).createDomain());
        DataColumnSpecCreator lfWithoutValues = new DataColumnSpecCreator("lfWithoutValues", StringCell.TYPE);
        DataTableSpec tableSpec = new DataTableSpec(otherCol.createSpec(), lf2.createSpec(), lf1.createSpec(),
            lfWithoutValues.createSpec(), lf3.createSpec());
        META_DATA = new MetaData(new String[]{"lf1", "lf2", "lf3", "lfWithoutValues"}, tableSpec);
    }

    @Test
    public void testGetPossibleClasses() throws Exception {
        assertEquals(Sets.newHashSet(CLASSES), META_DATA.getPossibleLabels());
    }

    @Test
    public void testGetCardinality() throws Exception {
        assertEquals(CLASSES.length, META_DATA.getNumClasses());
    }

    @Test
    public void testGetNonEmptyIndices() throws Exception {
        assertArrayEquals(new int[] {2, 1, 4}, META_DATA.getNonEmptyIndices());
    }

    @Test
    public void testGetNonEmptyColumns() throws Exception {
        assertEquals(Sets.newHashSet("lf1", "lf2", "lf3"), META_DATA.getNonEmptyColumns());
    }

    @Test
    public void testGetLabelToIdxMap() throws Exception {
        final Map<DataCell, Integer> expected = new HashMap<>();
        expected.put(CLASSES[0], 0);
        expected.put(CLASSES[1], 1);
        expected.put(CLASSES[2], 2);
        assertEquals(expected, META_DATA.getLabelToIdxMap());
    }

    @Test
    public void testGetNumSources() throws Exception {
        assertEquals(3, META_DATA.getNumSources());
    }

    @Test
    public void testGetEmptyColumns() throws Exception {
        assertEquals(Sets.newHashSet("lfWithoutValues"), META_DATA.getEmptyColumns());
    }

}
