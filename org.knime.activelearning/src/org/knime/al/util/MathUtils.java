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
 *   5 Dec 2014 (gabriel): created
 */
package org.knime.al.util;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.apache.commons.math3.util.FastMath;

/**
 * Math utility functions used in AL Nodes.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public final class MathUtils {
    // tollerance for double equality checks
    private static final double TOLLERANCE = 1E-7;

    // natural log of 2
    public static final double LOG2 = FastMath.log(2.0d);

    private static final Variance VAR = new Variance();
    private static final Sum SUM = new Sum();

    private MathUtils() {
        // NB: Utility Class
    }

    /**
     * Checks if the given class probability distribution is valid.
     *
     * @param distribution
     *            the distribution.
     * @return if the given distribution is valid.
     */
    public static final boolean checkDistribution(final double[] distribution) {
        if (Math.abs(1d - SUM.evaluate(distribution)) > TOLLERANCE) {
            return false;
        }
        return true;
    }

    /**
     * Computes the binary logarithm. (logarithm base 2)
     *
     * @param x
     *            the input
     * @return the log to the base 2 of x
     */
    public static final double log2(final double x) {
        if (x == 0) {
            return 0;
        } else {
            return FastMath.log(x) / LOG2;
        }
    }

    /**
     * Calculate the sum of an array of doubles.
     *
     * @param array
     *            of doubles
     * @return sum of the array
     */
    public static final double sumOfArray(final double[] array) {
        double sum = 0.0d;
        for (final double d : array) {
            sum += d;
        }
        return sum;
    }

    /**
     * Calculates the shanon entropy of the distribution of values in an double
     * array.
     *
     * @param distribution
     *            a double array
     * @return the entropy of the array
     * @throws ArithmeticException
     *             in case the sum of the elements of the distribution array is
     *             not 1.
     */
    public static final double entropy(final double[] distribution) {

        if (!checkDistribution(distribution)) {
            throw new ArithmeticException(
                    "The sum of the distribution elements is not 1");
        }

        // calculate the entropy
        double entropy = 0;
        for (final double d : distribution) {
            entropy += d * log2(d);
        }

        entropy /= log2(distribution.length);
        if (Double.isNaN(entropy)) {
            entropy = 0;
        }
        return -entropy;
    }

    /**
     * Calculates the Shannon entropy of the distribution of values in an double array.
     *
     * @param distribution a double array
     * @return the entropy of the array
     */
    public static final double entropyWithoutDistributionCheck(final double[] distribution) {
        double entropy = 0;
        for (final double d : distribution) {
            entropy += d * log2(d);
        }

        entropy /= log2(distribution.length);
        if (Double.isNaN(entropy)) {
            entropy = 0;
        }
        if (entropy == 0) {
            return entropy;
        }
        return -entropy;
    }

    /**
     *
     *
     * @return the variance of the array
     * @throws ArithmeticException
     *             if case the sum of the elements of the distribution array is
     *             not 1.
     */
    public static double variance(final double[] distribution) {

        // check if input array was a proper distribution.
        if (!checkDistribution(distribution)) {
            throw new ArithmeticException(
                    "The sum of the distribution elements is not 1");
        }
        return VAR.evaluate(distribution);
    }

    /**
     * Calculates the variance of the distribution of values in an double array.
     *
     * @param distribution a double array containing an distribution
     *
     * @return the variance of the array
     */
    public static double varianceWithoutDistributionCheck(final double[] distribution) {
        return VAR.evaluate(distribution);
    }
}
