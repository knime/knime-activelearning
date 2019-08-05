package org.knime.wsl.labelmodel;

import static org.knime.wsl.testing.TestUtil.assertMatrixEquals;
import static org.knime.wsl.testing.TestUtil.fa;
import static org.knime.wsl.testing.TestUtil.fm;
import static org.knime.wsl.testing.TestUtil.ia;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

import com.google.common.collect.Lists;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
public class LabelMatrixReaderTest {

    private Map<DataCell, Integer> m_valueToIdxMap;

    private List<DataRow> m_rows;

    @Before
    public void init() {
        List<DataCell> classes = Stream.of("A", "B", "C").map(StringCell::new).collect(Collectors.toList());
        m_valueToIdxMap =
            IntStream.range(0, classes.size()).boxed().collect(Collectors.toMap(classes::get, Function.identity()));
        final List<int[]> idxs = Lists.newArrayList(ia(0, 1, 2), ia(1, 0, 2), ia(2, 1, 0));
        m_rows = IntStream.range(0, idxs.size())
            .mapToObj(i -> new DefaultRow(RowKey.createRowKey((long)i),
                Arrays.stream(idxs.get(i)).mapToObj(classes::get).toArray(DataCell[]::new)))
            .collect(Collectors.toList());
        m_rows.add(new DefaultRow(new RowKey("missing"), new MissingCell("missing"), classes.get(0), classes.get(2)));
    }

    @Test
    public void testReadAndAugmentLabelMatrix() throws Exception {
        final LabelMatrixReader reader = new LabelMatrixReader(m_valueToIdxMap, 3);
        float[][] matrix = reader.readAndAugmentLabelMatrix(m_rows.iterator(), m_rows.size(), new ExecutionMonitor());
        float[][] expected = fm(fa(1, 0, 0, 0, 1, 0, 0, 0, 1), fa(0, 1, 0, 1, 0, 0, 0, 0, 1),
            fa(0, 0, 1, 0, 1, 0, 1, 0, 0), fa(0, 0, 0, 1, 0, 0, 0, 0, 1));
        assertMatrixEquals(expected, matrix, 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testFailOnUnknownLabel() throws Exception {
        m_valueToIdxMap.remove(new StringCell("C"));
        new LabelMatrixReader(m_valueToIdxMap, 3).readAndAugmentLabelMatrix(m_rows.iterator(), m_rows.size(),
            new ExecutionMonitor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailOnWrongNumberOfCells() throws Exception {
        new LabelMatrixReader(m_valueToIdxMap, 2).readAndAugmentLabelMatrix(m_rows.iterator(), m_rows.size(),
            new ExecutionMonitor());
    }
}
