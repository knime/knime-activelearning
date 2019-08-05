The files in this folder are created using Snorkel Metal v0.5 with randomization and SGD momentum disabled.

l_test.csv: The label matrix used to create the LabelModel in Snorkel Metal.
mu_init.csv: The initial value of mu, the parameters learned by the LabelModel.
mu_final.csv: The final value of mu after training for 100 epochs with learning rate 0.01.
probabilistic_labels.csv: The probabilistic labels predicted by Snorkel Metal using the final model.