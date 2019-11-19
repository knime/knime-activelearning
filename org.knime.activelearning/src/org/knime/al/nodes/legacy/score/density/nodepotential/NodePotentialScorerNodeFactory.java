package org.knime.al.nodes.legacy.score.density.nodepotential;

/*
 * ------------------------------------------------------------------
 * Copyright by
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * This file is part of the WEKA integration plugin for KNIME.
 *
 * The WEKA integration plugin is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 * Or contact us: contact@knime.com.
 * -------------------------------------------------------------------
 *
 * History
 *   29.09.2005 (cebron): created
 */

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

/**
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class NodePotentialScorerNodeFactory
        extends NodeFactory<NodePotentialScorerNodeModel> {

    /**
     * Constructor.
     */
    public NodePotentialScorerNodeFactory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<NodePotentialScorerNodeModel> createNodeView(
            final int viewIndex, final NodePotentialScorerNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodePotentialScorerNodeModel createNodeModel() {
        return new NodePotentialScorerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new DefaultNodeSettingsPane() {
            {
                createNewGroup("Scoring Settings");
                addDialogComponent(new DialogComponentNumber(
                        NodePotentialScorerNodeModel.createConstRAlphaModel(),
                        "Radius Alpha", 0.1));

                createNewGroup("Column Selection");
                addDialogComponent(new DialogComponentColumnFilter2(
                        NodePotentialScorerNodeModel.createColumnFilterModel(),
                        0));
                createNewGroup("Output Settings");
                addDialogComponent(new DialogComponentString(NodePotentialScorerNodeModel.createOutputColumnNameModel(), "Output Column Name"));
            }
        };
    }
}
