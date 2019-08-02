package org.knime.wsl.labelmodel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
public class LabelModelUtilTest {

    @Test
    public void testClipFloat() throws Exception {
        assertEquals(0, LabelModelUtil.clip(-0.1f, 0, 1), 0);
        assertEquals(0.5, LabelModelUtil.clip(0.5f, 0, 1), 0);
        assertEquals(1, LabelModelUtil.clip(2, 0, 1), 0);
    }

    @Test
    public void testClipDouble() throws Exception {
        assertEquals(0, LabelModelUtil.clip(-0.1, 0, 1), 0);
        assertEquals(0.5, LabelModelUtil.clip(0.5, 0, 1), 0);
        assertEquals(1, LabelModelUtil.clip(2, 0, 1), 0);
    }
}
