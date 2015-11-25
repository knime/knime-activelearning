package org.knime.al.util.novelty.knfst;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.knime.al.util.noveltydetection.knfst.ClassWrapper;

public class ClassWrapperTest {

    @Test
    public void testClasses() {
        final String[] labels1 = new String[] { "1" };
        final ClassWrapper[] expected1 =
                new ClassWrapper[] { new ClassWrapper("1", 1) };
        assertArrayEquals(expected1, ClassWrapper.classes(labels1));

        final String[] labels2 = new String[] { "1", "1", "1" };
        final ClassWrapper[] expected2 =
                new ClassWrapper[] { new ClassWrapper("1", 3) };
        assertArrayEquals(expected2, ClassWrapper.classes(labels2));

        final String[] labels3 =
                new String[] { "1", "2", "2", "2", "3", "3", "4" };
        final ClassWrapper[] expected3 = new ClassWrapper[] {
                new ClassWrapper("1", 1), new ClassWrapper("2", 3),
                new ClassWrapper("3", 2), new ClassWrapper("4", 1) };
        assertArrayEquals(expected3, ClassWrapper.classes(labels3));
    }

}
