/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * --------------------------------------------------------------------- *
 *
 */

package org.knime.al.util.noveltydetection.kernel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PolynomialKernel implements KernelFunction {

    private double m_gamma;
    private double m_bias;
    private double m_power;

    // Framework constructor for loading
    // do not use for anything else!
    public PolynomialKernel() {
    }

    public PolynomialKernel(final double gamma, final double bias,
            final double power) {
        m_gamma = gamma;
        m_bias = bias;
        m_power = power;
    }

    @Override
    public void readExternal(final ObjectInput arg0)
            throws IOException, ClassNotFoundException {
        m_gamma = arg0.readDouble();
        m_bias = arg0.readDouble();
        m_power = arg0.readDouble();
    }

    @Override
    public void writeExternal(final ObjectOutput arg0) throws IOException {
        arg0.writeDouble(m_gamma);
        arg0.writeDouble(m_bias);
        arg0.writeDouble(m_power);
    }

    @Override
    public double calculate(final double[] sample1, final double[] sample2) {
        if (sample1.length != sample2.length) {
            throw new IllegalArgumentException(
                    "The arrays (vectors) must be of the same length.");
        }

        double result = 0;
        for (int i = 0; i < sample1.length; ++i) {
            final double oldresult = result;
            result = oldresult + sample1[i] * sample2[i];
        }
        result = m_gamma * result;
        result = result + m_bias;
        return Math.pow(result, m_power);
    }

    @Override
    public int numParameters() {
        return 3;
    }

    @Override
    public double getParameter(final int index) {
        if (index < 0 || index > 2) {
            throw new IndexOutOfBoundsException();
        }

        if (index == 0) {
            return m_gamma;
        } else if (index == 1) {
            return m_bias;
        } else {
            return m_power;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(m_bias);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(m_gamma);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(m_power);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PolynomialKernel)) {
            return false;
        }
        final PolynomialKernel other = (PolynomialKernel) obj;
        if (Double.doubleToLongBits(m_bias) != Double
                .doubleToLongBits(other.m_bias)) {
            return false;
        }
        if (Double.doubleToLongBits(m_gamma) != Double
                .doubleToLongBits(other.m_gamma)) {
            return false;
        }
        if (Double.doubleToLongBits(m_power) != Double
                .doubleToLongBits(other.m_power)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PolynomialKernel [m_gamma=" + m_gamma + ", m_bias=" + m_bias
                + ", m_power=" + m_power + "]";
    }

}
