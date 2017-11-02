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
 * ---------------------------------------------------------------------
 *
 * History
 *   2 Dec 2014 (gabriel): created
 */
package org.knime.al.nodes.select.elementselector;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

// TODO: Auto-generated Javadoc
/**
 * Settings Models for the Element Selector Node.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
final class ElementSelectorSettingsModels {

    /** The Constant DEFAULT_ELEMENTS. */
    static final int DEFAULT_ELEMENTS = 5;

    /**
     * The element selection strategies.
     */
    protected enum ElementSelectionStrategy {

        /** Take the the biggest. */
        BIGGEST("Biggest Elements"), /** Take the smallest. */
        SMALLEST("Smallest Elements");

        /** The name. */
        private String m_name;

        /**
         * Instantiates a new element selection strategy.
         *
         * @param describingName
         *            the describing name
         */
        private ElementSelectionStrategy(final String describingName) {
            m_name = describingName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_name;
        }
    }

    /**
     * Instantiates a new element selector settings models.
     */
    private ElementSelectorSettingsModels() {
        // Utility class
    }

    /**
     * Creates the num elements model.
     *
     * @return Settings model to store the number
     */
    static SettingsModelIntegerBounded createNumElementsModel() {
        return new SettingsModelIntegerBounded("number_of_elements",
                DEFAULT_ELEMENTS, 1, Integer.MAX_VALUE);
    }

    /**
     * Creates the column model.
     *
     * @return settings model to store the selected column
     */
    static SettingsModelString createColumnModel() {
        return new SettingsModelString("columnName", "");
    }

    /**
     * Creates the select from top model.
     *
     * @return the settings model boolean
     */
    static SettingsModelBoolean createSelectFromTopModel() {
        return new SettingsModelBoolean("select_from_top", true);
    }

    /**
     * Creates the element selection strategy model.
     *
     * @return the settings model string
     */
    static SettingsModelString createElementSelectionStrategyModel() {
        return new SettingsModelString("element_selection_mode",
                ElementSelectionStrategy.BIGGEST.toString());
    }

}
