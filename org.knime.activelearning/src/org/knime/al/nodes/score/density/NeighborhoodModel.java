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
 *   Sep 19, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.al.nodes.score.density;

import java.io.Externalizable;
import java.util.UUID;

import org.knime.core.data.RowKey;

/**
 * Represents the static part of a {@link DensityScorerModel} that doesn't change after it has been initialized.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface NeighborhoodModel extends Externalizable {

    /**
     * @return the id of this neighborhood (for caching)
     */
    UUID getId();

    /**
     * @param potentialUpdater used to reduce the potentials in the neighborhood of <b>row</b>
     * @param row the {@link RowKey} of the row for which the potentials have to be updated
     * @throws UnknownRowException if {@link RowKey} row is unknown to the model
     */
    void updateNeighbors(PotentialUpdater potentialUpdater, final RowKey row) throws UnknownRowException;

    /**
     * @param key the {@link RowKey} of the row for which the index is required
     * @return the index of the row with {@link RowKey key} in this neighborhood model
     * @throws UnknownRowException if {@link RowKey key} is unknown to the model
     */
    int getIndex(final RowKey key) throws UnknownRowException;

    /**
     * @return the number of rows in this neighborhood model
     */
    int getNrRows();
}
