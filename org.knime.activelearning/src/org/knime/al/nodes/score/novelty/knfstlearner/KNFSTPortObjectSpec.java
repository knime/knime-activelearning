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

import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

public class KNFSTPortObjectSpec extends AbstractSimplePortObjectSpec {
    public static final class Serializer extends
            AbstractSimplePortObjectSpecSerializer<KNFSTPortObjectSpec> {
    }

    private String[] m_compatibleFeatures;

    private static final String CFGKEY_COMPATIBLEFEATURES =
            "CompatibleFeatures";

    public KNFSTPortObjectSpec(final List<String> compatibleFeatures) {
        m_compatibleFeatures = compatibleFeatures
                .toArray(new String[compatibleFeatures.size()]);
    }

    public KNFSTPortObjectSpec() {
    }

    @Override
    public JComponent[] getViews() {
        return null;
    }

    public List<String> getCompatibleFeatures() {
        return Arrays.asList(m_compatibleFeatures);
    }

    @Override
    protected void save(final ModelContentWO model) {
        model.addStringArray(CFGKEY_COMPATIBLEFEATURES, m_compatibleFeatures);

    }

    @Override
    protected void load(final ModelContentRO model)
            throws InvalidSettingsException {
        m_compatibleFeatures = model.getStringArray(CFGKEY_COMPATIBLEFEATURES);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_compatibleFeatures == null) ? 0
                : m_compatibleFeatures.hashCode());
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
        if (!(obj instanceof KNFSTPortObjectSpec)) {
            return false;
        }
        final KNFSTPortObjectSpec other = (KNFSTPortObjectSpec) obj;
        if (m_compatibleFeatures == null) {
            if (other.m_compatibleFeatures != null) {
                return false;
            }
        } else if (!m_compatibleFeatures.equals(other.m_compatibleFeatures)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return "KNFSTPortObjectSpec [m_compatibleFeatures="
                + (m_compatibleFeatures != null
                        ? Arrays.asList(m_compatibleFeatures)
                                .subList(0, Math.min(
                                        m_compatibleFeatures.length, maxLen))
                        : null)
                + "]";
    }

}
