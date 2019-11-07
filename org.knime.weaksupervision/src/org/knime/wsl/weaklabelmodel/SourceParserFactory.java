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
 *   Sep 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.wsl.weaklabelmodel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.NominalValue;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.node.util.CheckUtils;

/**
 * A factory for {@link SourceParser SourceParsers} for string and probability distribution source columns.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SourceParserFactory {

    private final Map<String, Integer> m_labelIdxMap = new HashMap<>();

    private final String[] m_idxToLabel;

    /**
     * @param labelIdxMap associates the string representation of a label with its index used inside the model
     */
    SourceParserFactory(final String[] labels) {
        m_idxToLabel = labels;
        Arrays.stream(labels).forEach(l -> m_labelIdxMap.put(l, m_labelIdxMap.size()));
    }

    /**
     * @param sourceSpec a {@link DataTableSpec} containing only the label sources used for training
     * @param possibleClasses the possible class values
     * @return an array of {@link SourceParser} for the columns in <b>sourceSpec</b>
     * @throws IllegalArgumentException if any of the columns in {@link DataTableSpec sourceSpec}
     * are of an unsupported type
     */
    public static SourceParser[] createParsers(final DataTableSpec sourceSpec, final Set<String> possibleClasses) {
        final LinkedHashMap<String, Integer> labelIdxMap = new LinkedHashMap<>();
        possibleClasses.forEach(c -> labelIdxMap.put(c, labelIdxMap.size()));
        final SourceParserFactory factory = new SourceParserFactory(possibleClasses.toArray(new String[0]));
        return sourceSpec.stream().map(factory::create).toArray(SourceParser[]::new);
    }

    /**
     * @param columnSpec the {@link DataColumnSpec} for which to create a {@link SourceParser}
     * @return a {@link SourceParser} for {@link DataColumnSpec columnSpec}
     * @throws IllegalArgumentException if {@link DataColumnSpec columnSpec} is of an unsupported type
     */
    SourceParser create(final DataColumnSpec columnSpec) {
        final DataType type = columnSpec.getType();
        if (type.isCompatible(NominalValue.class)) {
            return this::parseNominalSource;
        } else if (type.isCompatible(NominalDistributionValue.class)) {
            return this::parseProbabilityDistribution;
        } else {
            throw new IllegalArgumentException(String.format("Unsupported source type %s encountered.", type));
        }
    }

    private float parseProbabilityDistribution(final DataCell cell, final int label) {
        if (cell.isMissing()) {
            return 0.0f;
        }
        CheckUtils.checkArgument(cell instanceof NominalDistributionValue,
            "The provided cell %s is not a probability distribution value.", cell);
            final NominalDistributionValue value = (NominalDistributionValue)cell;
            return (float)value.getProbability(m_idxToLabel[label]);
    }

    private float parseNominalSource(final DataCell cell, final int label) {
        if (cell.isMissing()) {
            return 0.0f;
        }
        return getLabel(cell) == label ? 1.0f : 0.0f;
    }

    private int getLabel(final DataCell cell) {
        return getLabelIndex(cell.toString());
    }

    @SuppressWarnings("null") // we check that label is not null
    private int getLabelIndex(final String labelValue) {
        final Integer label = m_labelIdxMap.get(labelValue);
        CheckUtils.checkState(label != null, "Unknown label encountered.");
        return label;
    }

}
