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
 *   Sep 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * Spec of the {@link WeakLabelModelPortObject}. It stores the columns used to train the model as well as the possible
 * classes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WeakLabelModelPortObjectSpec extends AbstractSimplePortObjectSpec {

    private static final String CFG_POSSIBLE_LABELS = "possibleLabels";

    /**
     * Serializer for {@link WeakLabelModelPortObjectSpec} objects.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<WeakLabelModelPortObjectSpec> {
    }

    private DataTableSpec m_trainSpec;

    private Set<String> m_possibleLabels;

    /**
     * @param trainSpec the training spec containing all the label source columns
     * @param possibleLabels the possible labels
     */
    public WeakLabelModelPortObjectSpec(final DataTableSpec trainSpec, final String[] possibleLabels) {
        m_trainSpec = trainSpec;
        m_possibleLabels = toLinkedHashSet(possibleLabels);
    }

    /**
     * Framework constructor for serialization.
     *
     * @noreference Don't use in node development
     */
    public WeakLabelModelPortObjectSpec() {
        // Framework constructor
    }

    /**
     * @return {@link DataTableSpec} containing all the label source columns used to train the model
     */
    public DataTableSpec getTrainingSpec() {
        return m_trainSpec;
    }

    /**
     * Returns an ordered set of possible classes.
     *
     * @return the ordered set of possible classes
     */
    public Set<String> getPossibleClasses() {
        return Collections.unmodifiableSet(m_possibleLabels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        m_trainSpec.save(model);
        model.addStringArray(CFG_POSSIBLE_LABELS, m_possibleLabels.toArray(new String[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_trainSpec = DataTableSpec.load(model);
        final String[] possibleLabelsArray = model.getStringArray(CFG_POSSIBLE_LABELS);
        m_possibleLabels = toLinkedHashSet(possibleLabelsArray);
    }

    private static LinkedHashSet<String> toLinkedHashSet(final String[] possibleLabelsArray) {
        return Arrays.stream(possibleLabelsArray).collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
