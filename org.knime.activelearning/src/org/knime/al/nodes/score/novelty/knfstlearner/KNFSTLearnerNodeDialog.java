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

package org.knime.al.nodes.score.novelty.knfstlearner;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.al.util.noveltydetection.kernel.KernelCalculator.KernelType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class KNFSTLearnerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Default Constructor
     */
    @SuppressWarnings("unchecked")
    public KNFSTLearnerNodeDialog() {
        addDialogComponent(new DialogComponentColumnFilter(
                KNFSTLearnerNodeModel.createColumnSelectionModel(),
                KNFSTLearnerNodeModel.DATA_INPORT, false, DoubleValue.class));

        addDialogComponent(new DialogComponentColumnNameSelection(
                KNFSTLearnerNodeModel.createClassColumnSelectionModel(),
                "Select class column", 0, StringValue.class));

        final SettingsModelString kernelType =
                KNFSTLearnerNodeModel.createKernelFunctionSelectionModel();

        final KernelType[] availableKernels = KernelType.values();
        final String[] kernelNames = new String[availableKernels.length];
        for (int i = 0; i < availableKernels.length; i++) {
            kernelNames[i] = availableKernels[i].toString();
        }

        final SettingsModelDouble rbfSigma =
                KNFSTLearnerNodeModel.createRBFSigmaModel();
        final SettingsModelDouble polynomialGamma =
                KNFSTLearnerNodeModel.createPolynomialGammaModel();
        final SettingsModelDouble polynomialBias =
                KNFSTLearnerNodeModel.createPolynomialBiasModel();
        final SettingsModelDouble polynomialPower =
                KNFSTLearnerNodeModel.createPolynomialPower();

        kernelType.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {

                if (kernelType.getStringValue() == "RBF") {
                    rbfSigma.setEnabled(true);
                    polynomialBias.setEnabled(false);
                    polynomialGamma.setEnabled(false);
                    polynomialPower.setEnabled(false);
                } else if (kernelType.getStringValue() == "Polynomial") {
                    rbfSigma.setEnabled(false);
                    polynomialBias.setEnabled(true);
                    polynomialGamma.setEnabled(true);
                    polynomialPower.setEnabled(true);
                } else {
                    rbfSigma.setEnabled(false);
                    polynomialBias.setEnabled(false);
                    polynomialGamma.setEnabled(false);
                    polynomialPower.setEnabled(false);
                }
            }
        });

        addDialogComponent(new DialogComponentStringSelection(kernelType,
                "Kernel", kernelNames));

        addDialogComponent(new DialogComponentNumberEdit(rbfSigma, "Sigma: "));

        addDialogComponent(
                new DialogComponentNumberEdit(polynomialGamma, "Gamma: "));

        addDialogComponent(
                new DialogComponentNumberEdit(polynomialBias, "Bias: "));

        addDialogComponent(
                new DialogComponentNumberEdit(polynomialPower, "Power: "));

        addDialogComponent(new DialogComponentBoolean(
                KNFSTLearnerNodeModel.createSortTableModel(),
                "Sort Table (select only if table is not already sorted by class)"));
    }
}
