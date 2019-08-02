package org.knime.wsl.testing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.knime.wsl.labelmodel.SavedModelAdapterTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TestUtil {

    private TestUtil() {
        // utility class
    }

    public static File findInPlugin(final String name) throws IOException {
        Bundle thisBundle = FrameworkUtil.getBundle(SavedModelAdapterTest.class);
        URL url = FileLocator.find(thisBundle, new Path(name), null);
        if (url == null) {
            throw new FileNotFoundException(thisBundle.getLocation() + name);
        }
        return new File(FileLocator.toFileURL(url).getPath());
    }

    /**
     * Shorthand to create int arrays.
     *
     * @param values the values to create an array of
     * @return an int array containing <b>values</b>
     */
    public static int[] ia(final int... values) {
        return values;
    }

    /**
     * Shorthand to create float arrays.
     *
     * @param values the values to create an array of
     * @return an float array containing <b>values</b>
     */
    public static float[] fa(final float... values) {
        return values;
    }

    /**
     * Shorthand for creating float matrices.
     *
     * @param rows the arrays that make up the rows of a matrix
     * @return a float matrix consisting of the provided <b>rows</b>
     */
    public static float[][] fm(final float[]... rows) {
        return rows;
    }

    /**
     * Shorthand to create boolean arrays.
     *
     * @param values the values to create an array of
     * @return a boolean array containing <b>values</b>
     */
    public static boolean[] ba(final boolean... values) {
        return values;
    }

    /**
     * Shorthand for creating boolean matrices.
     *
     * @param rows the arrays that make up the rows of a matrix
     * @return a boolean matrix consisting of the provided <b>rows</b>
     */
    public static boolean[][] bm(final boolean[]... rows) {
        return rows;
    }

    public static void assertMatrixEquals(final float[][] expected, final float[][] actual, final float delta) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], delta);
        }
    }

    public static void assertMatrixEquals(final boolean[][] expected, final boolean[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }


    public static float[][] readFloatMatrixFromCSV(final File csv) throws FileNotFoundException {
        List<float[]> rows = new ArrayList<>();
        try (Scanner scanner = new Scanner(csv);) {
            while (scanner.hasNextLine()) {
                rows.add(rowToArray(scanner.nextLine()));
            }
        }
        return rows.toArray(new float[rows.size()][]);
    }

    private static float[] rowToArray(final String row) {
        List<Float> values = new ArrayList<>();
        try (Scanner rowScanner = new Scanner(row)) {
            rowScanner.useDelimiter(",");
            while (rowScanner.hasNextFloat()) {
                values.add(rowScanner.nextFloat());
            }
        }
        return toArray(values);
    }

    private static float[] toArray(final List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
