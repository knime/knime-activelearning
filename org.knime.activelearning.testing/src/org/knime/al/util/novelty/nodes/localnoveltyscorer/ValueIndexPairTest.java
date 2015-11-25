package org.knime.al.util.novelty.nodes.localnoveltyscorer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.knime.al.nodes.score.novelty.localnoveltyscorer.ValueIndexPair;

public class ValueIndexPairTest {

    private final ValueIndexPair[] testArray = new ValueIndexPair[] {
            new ValueIndexPair(1, 0), new ValueIndexPair(2, 1),
            new ValueIndexPair(3, 2), new ValueIndexPair(4, 3) };
    private final Comparator<ValueIndexPair> comparator =
            new Comparator<ValueIndexPair>() {
                @Override
                public int compare(final ValueIndexPair o1,
                        final ValueIndexPair o2) {
                    if (o1.getValue() < o2.getValue()) {
                        return -1;
                    } else if (o1.getValue() > o2.getValue()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            };

    @Test
    public void testTransformArray2ValueIndexPairArray() {
        final double[] array = new double[] { 1, 2, 3, 4 };

        final ValueIndexPair[] result =
                ValueIndexPair.transformArray2ValueIndexPairArray(array);
        for (int i = 0; i < result.length; i++) {
            assertEquals(testArray[i], result[i]);
        }
    }

    @Test
    public void testGetKSimple() {
        final ValueIndexPair[] expected = new ValueIndexPair[] {
                new ValueIndexPair(1, 0), new ValueIndexPair(2, 1) };
        final ArrayList<ValueIndexPair> result = new ArrayList<ValueIndexPair>(
                Arrays.asList(ValueIndexPair.getK(testArray, 2, comparator)));
        for (int i = 0; i < expected.length; i++) {
            assertTrue(result.contains(expected[i]));
        }
    }

    @Test
    public void testGetKRandomMinima() {
        final double[] array = new double[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = Math.random();
        }
        final ValueIndexPair[] valIndexArray =
                ValueIndexPair.transformArray2ValueIndexPairArray(array);
        final ArrayList<ValueIndexPair> expectedFullList =
                new ArrayList<ValueIndexPair>(Arrays.asList(valIndexArray));

        expectedFullList.sort(comparator);
        final List<ValueIndexPair> expected = expectedFullList.subList(0, 19);

        final ArrayList<ValueIndexPair> result =
                new ArrayList<ValueIndexPair>(Arrays.asList(
                        ValueIndexPair.getK(valIndexArray, 20, comparator)));

        for (final ValueIndexPair pair : expected) {
            assertTrue(result.contains(pair));
        }
    }

    @Test
    public void testGetKRandomMaxima() {
        final double[] array = new double[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = Math.random();
        }
        final ValueIndexPair[] valIndexArray =
                ValueIndexPair.transformArray2ValueIndexPairArray(array);
        final ArrayList<ValueIndexPair> expectedFullList =
                new ArrayList<ValueIndexPair>(Arrays.asList(valIndexArray));

        final Comparator<ValueIndexPair> comparator =
                new Comparator<ValueIndexPair>() {
                    @Override
                    public int compare(final ValueIndexPair o1,
                            final ValueIndexPair o2) {
                        if (o1.getValue() < o2.getValue()) {
                            return 1;
                        } else if (o1.getValue() > o2.getValue()) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                };

        expectedFullList.sort(comparator);
        final List<ValueIndexPair> expected = expectedFullList.subList(0, 19);

        final ArrayList<ValueIndexPair> result =
                new ArrayList<ValueIndexPair>(Arrays.asList(
                        ValueIndexPair.getK(valIndexArray, 20, comparator)));

        for (final ValueIndexPair pair : expected) {
            assertTrue(result.contains(pair));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unused")
    public void testExceptionKTooSmall() {
        final ValueIndexPair[] expected = new ValueIndexPair[] {
                new ValueIndexPair(1, 0), new ValueIndexPair(2, 1) };
        final ArrayList<ValueIndexPair> result = new ArrayList<ValueIndexPair>(
                Arrays.asList(ValueIndexPair.getK(testArray, 0, comparator)));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unused")
    public void testExceptionKTooLarge() {
        final ValueIndexPair[] expected = new ValueIndexPair[] {
                new ValueIndexPair(1, 0), new ValueIndexPair(2, 1) };
        final ArrayList<ValueIndexPair> result = new ArrayList<ValueIndexPair>(
                Arrays.asList(ValueIndexPair.getK(testArray, 230, comparator)));
    }
}
