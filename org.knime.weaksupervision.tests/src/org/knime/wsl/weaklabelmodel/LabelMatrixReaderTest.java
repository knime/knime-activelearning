package org.knime.wsl.weaklabelmodel;

import static org.knime.wsl.testing.TestUtil.assertMatrixEquals;
import static org.knime.wsl.testing.TestUtil.fa;
import static org.knime.wsl.testing.TestUtil.fm;
import static org.knime.wsl.testing.TestUtil.ia;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.UniqueNameGenerator;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LabelMatrixReaderTest {

    private static final String[] CLASSES = new String[]{"A", "B", "C"};

    private SourceParser[] m_parsers;

    private List<DataRow> m_rows;

    @Before
    public void init() {
        List<DataCell> classes = Arrays.stream(CLASSES).map(StringCell::new).collect(Collectors.toList());
        IntStream.range(0, classes.size()).boxed().collect(Collectors.toMap(classes::get, Function.identity()));
        final List<int[]> idxs = Lists.newArrayList(ia(0, 1, 2), ia(1, 0, 2), ia(2, 1, 0));
        m_rows = IntStream.range(0, idxs.size())
            .mapToObj(i -> new DefaultRow(RowKey.createRowKey((long)i),
                Arrays.stream(idxs.get(i)).mapToObj(classes::get).toArray(DataCell[]::new)))
            .collect(Collectors.toList());
        m_rows.add(new DefaultRow(new RowKey("missing"), new MissingCell("missing"), classes.get(0), classes.get(2)));
        m_parsers =
            new SourceParser[]{mockParser(3, 0, 1, 2, -1), mockParser(3, 1, 0, 1, 0), mockParser(3, 2, 2, 0, 2)};
    }

    private static SourceParser mockParser(final int nrLabels, final int... labels) {
        final Float[] array = Arrays.stream(labels).boxed()
            .flatMap(i -> IntStream.range(0, nrLabels).mapToObj(l -> l == i ? 1.0F : 0.0F)).toArray(Float[]::new);
        return mockParser(array[0], Arrays.copyOfRange(array, 1, array.length));
    }

    private static SourceParser mockParser(final Float head, final Float... tail) {
        SourceParser parser = mock(SourceParser.class);
        when(parser.parseProbability(any(), anyInt())).thenReturn(head, tail);
        return parser;
    }

    @Test
    public void testReadAndAugmentLabelMatrix() throws Exception {
        final LabelMatrixReader reader = new LabelMatrixReader(m_parsers, 3);
        float[][] matrix = reader.readAndAugmentLabelMatrix(m_rows.iterator(), m_rows.size(), new ExecutionMonitor());
        float[][] expected = fm(fa(1, 0, 0, 0, 1, 0, 0, 0, 1), fa(0, 1, 0, 1, 0, 0, 0, 0, 1),
            fa(0, 0, 1, 0, 1, 0, 1, 0, 0), fa(0, 0, 0, 1, 0, 0, 0, 0, 1));
        assertMatrixEquals(expected, matrix, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailOnWrongNumberOfCells() throws Exception {
        new LabelMatrixReader(Arrays.copyOf(m_parsers, 2), 3).readAndAugmentLabelMatrix(m_rows.iterator(),
            m_rows.size(), new ExecutionMonitor());
    }

    @Test
    public void testReadCovarianceMatrix() throws Exception {
        final LabelMatrixReader reader = new LabelMatrixReader(createActualParsers(), 3);
        final float[][] covMat = reader.readCovarianceMatrix(m_rows.iterator(), m_rows.size(), new ExecutionMonitor());
        final float[][] expected = fm(fa(0.25f, 0, 0, 0, 0.25f, 0, 0, 0, 0.25f),
            fa(0, 0.25f, 0, 0.25f, 0, 0, 0, 0, 0.25f), fa(0, 0, 0.25f, 0, 0.25f, 0, 0.25f, 0, 0),
            fa(0, 0.25f, 0, 0.5f, 0, 0, 0, 0, 0.5f), fa(0.25f, 0, 0.25f, 0, 0.5f, 0, 0.25f, 0, 0.25f),
            fa(0, 0, 0, 0, 0, 0, 0, 0, 0), fa(0, 0, 0.25f, 0, 0.25f, 0, 0.25f, 0, 0), fa(0, 0, 0, 0, 0, 0, 0, 0, 0),
            fa(0.25f, 0.25f, 0, 0.5f, 0.25f, 0, 0, 0, 0.75f));
        assertMatrixEquals(expected, covMat, 0);
    }

    private SourceParser[] createActualParsers() {
        return SourceParserFactory.createParsers(createTableSpec(),
            Arrays.stream(CLASSES).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private static DataTableSpec createTableSpec() {
        DataTableSpecCreator creator = new DataTableSpecCreator();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(creator.createSpec());
        creator.addColumns(IntStream.range(0, 3).mapToObj(i -> "column" + 1)
            .map(n -> nameGen.newColumn(n, StringCell.TYPE)).toArray(DataColumnSpec[]::new));
        return creator.createSpec();
    }
}
