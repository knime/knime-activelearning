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
 *   Aug 20, 2015 (gabriel): created
 */
package org.knime.al.util;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author gabriel
 */
public class MathUtilsTest {
    private static final double ERROR = 1e-7d;

    @Test
    public void testSum() {
        final double[] five = { 2.5d, 2.5d };
        Assert.assertEquals("Sum not equal", 5d, MathUtils.sumOfArray(five),
                ERROR);

        final double[] one = { 1d };
        Assert.assertEquals("Sum not equal", 1d, MathUtils.sumOfArray(one),
                ERROR);
    }

    @Test
    public void testEntropy() {
        final double[] twodot5 = { 0.5d, 0.5d };
        Assert.assertEquals(1d, MathUtils.entropy(twodot5), ERROR);

        final double[] array = { 0.2d, 0.2d, 0.2d, 0.4d };
        Assert.assertEquals(0.9609640474436811d, MathUtils.entropy(array),
                ERROR);

        final double[] two = { 1d, 0.0d };
        Assert.assertEquals(0d, MathUtils.entropy(two), ERROR);
    }

    @Test
    public void testVariance() {
        final double[] twodot5 = { 0.5d, 0.5d };
        Assert.assertEquals(0d, MathUtils.variance(twodot5), ERROR);

        final double[] array = { 0.2d, 0.2d, 0.2d, 0.2d, 0.2d };
        Assert.assertEquals(0.0d, MathUtils.variance(array), ERROR);

        final double[] two = { 1d, 0.0d };
        Assert.assertEquals(0.5d, MathUtils.variance(two), ERROR);
    }

    @Test(expected = ArithmeticException.class)
    public void testEntropyReject() {
        final double[] wrong = { 2.5d, 0.5d };
        MathUtils.entropy(wrong);
    }

}
