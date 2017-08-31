# KNIME Active Learning

The KNIME Active Learning plugin comprises a set of KNIME nodes for
modular active learning and novelty detection in KNIME. Active learning
methods use feedback from the user to selectively sample training data.

 _Please note: KNIME - Active Learning is contained
 in [KNIME Labs](http://tech.knime.org/knime-labs)._

## Concept

KNIME Active Learning models the active learning process with the
**Active Learn Loop**. The management of the data takes place in the
*Active Learn Loop Start*, the labeling (assigning class labels to rows)
in the node end. The creation of the query for the oracle takes place
inside the loop.

## Example

![Image](http://i.imgur.com/D2qPzgn.png)

This example illustrates the active learning process with KNIME Active
Learning:

-   It starts with the *Active Learn Loop Start* node and ends with one
    of the *Active Learn Loop End* nodes.
-   Each unlabeled row is assigned a score in the **Score** module.
-   In the **Select** module, one (or more) rows are selected
    for labeling.
-   The selected rows are then assigned a class label in the *Active
    Learn Loop End *node.

#### Example Workflows

You can download the example workflows from the KNIME public example
server (002\_DataMining/002009\_ActiveLearning - see [here how to
connect...](https://www.knime.org/example-workflows))

## Contained Nodes

### Active Learn Loop

The "Active Learn Loop" nodes provide the framework for the active
learning process. Each active learning process starts with the *Active
Learn Loop Start* node and ends with one of the *Active Learn Loop End*
nodes:

-   **Active Learn Loop End:** This node provides an interface for a
    human oracle to label the selected rows.
-   **Auto Active Learn Loop End:** This node provides an automated
    oracle for fully labeled datasets. It can be used for verification
    and testing.

### Scorer Nodes

Scorer nodes are nodes which calculate a score for each row that
describes its relevance for the active learning process. KNIME Active
Learning provides scorer nodes grouped in the following categories:

-   **Uncertainty:** Nodes in this category calculate their score based
    on a class probability distribution which is a configurable output
    of many predictor nodes.
-   **Density:** Nodes in this category calculate and update a score
    initially based on the density of the feature space.
-   **Novelty Detection:** Nodes in this category calculate their score
    based on novelty detection methods, e.g. a Kernel Null
    Foley-Sammon Transformation.
-   **Combiner:** Nodes in this category calculate aggregation scores
    out of the combination of scores calculated by other scorers.
-   **All in one:** Nodes in this category provide scorers which package
    modular algorithms into a fixed package for increased performance.

### Selector Node

The "Element Selector Node" selects the n elements with the highest
score.


### Development Notes
You can find instructions on how to work with our code or develop extensions for
KNIME Analytics Platform in the _knime-sdk-setup_ repository
on [BitBucket](https://bitbucket.org/KNIME/knime-sdk-setup)
or [GitHub](http://github.com/knime/knime-sdk-setup).

### Join the Community!
* [KNIME Forum](https://tech.knime.org/forum)



