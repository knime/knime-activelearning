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

package org.knime.al.nodes.score.novelty.knfstnoveltyscorer;

import java.util.ArrayList;

import org.knime.al.util.noveltydetection.knfst.KNFST;
import org.knime.al.util.noveltydetection.knfst.NoveltyScores;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;

public class KNFSTNoveltyScorerCellFactory extends AbstractCellFactory {

    private final KNFST m_model;
    private final boolean m_appendNoveltyScore;
    private final boolean m_appendNullspaceCoordinates;
    private final double m_normalizer;

    public KNFSTNoveltyScorerCellFactory(final DataColumnSpec[] newColSpecs,
            final KNFST knfstModel, final boolean appendNoveltyScore,
            final boolean appendNullspaceCoordinates, final double normalizer) {
        super(newColSpecs);
        m_model = knfstModel;
        m_appendNoveltyScore = appendNoveltyScore;
        m_appendNullspaceCoordinates = appendNullspaceCoordinates;
        m_normalizer = normalizer;
        setParallelProcessing(true);
    }

    @Override
    public DataCell[] getCells(final DataRow row) {

        final NoveltyScores noveltyScores = m_model.scoreTestData(row);
        final double score = noveltyScores.getScores()[0] / m_normalizer;
        final double[] nullspaceCoordinates =
                noveltyScores.getCoordinates().getRow(0);

        final ArrayList<DataCell> cells = new ArrayList<DataCell>();
        if (m_appendNoveltyScore) {
            cells.add(new DoubleCell(score));
        }
        if (m_appendNullspaceCoordinates) {
            for (final double coord : nullspaceCoordinates) {
                cells.add(new DoubleCell(coord));
            }
        }

        if (cells.isEmpty()) {
            return new DataCell[] {};
        }

        return cells.toArray(new DataCell[cells.size()]);

    }

}
