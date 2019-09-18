import tensorflow as tf

"""
Adaptation of the LabelModel provided by Snorkel Metal v0.5 to TensorFlow
https://github.com/HazyResearch/metal/blob/v0.5.0/metal/label_model/label_model.py
"""

sess = tf.Session()

# configuration input
# max_epochs = tf.placeholder(tf.int32, name="max_epochs")
mu = tf.Variable(tf.zeros((20, 2)), name="mu", validate_shape=False)
p = tf.Variable(tf.zeros(1), name="P", validate_shape=False, trainable=False)
mask = tf.Variable(tf.zeros((2, 2), dtype=tf.bool), trainable=False, validate_shape=False, name='mask')
mask.set_shape([None, None])
o = tf.Variable(tf.zeros(0), trainable=False, validate_shape=False, name='O')
lr = tf.Variable(0.01, trainable=False, validate_shape=True, name='lr')


# data input
lr_init = tf.placeholder(tf.float32, [], name="learning_rate")
l_aug = tf.placeholder(tf.float32, name="l_aug")
mu_init = tf.placeholder(tf.float32, name="mu_init")
p_init = tf.placeholder(tf.float32, [None], name='p_init')
o_ph = tf.placeholder(tf.float32, name='o_init')

mask_init = tf.placeholder(tf.bool, shape=[None, None], name='mask_init')

mask_initialized = tf.assign(mask, mask_init, name='mask_initialized', validate_shape=False)


lr_initialized = tf.assign(lr, lr_init, validate_shape=False, name='lr_initialized')

n = tf.to_float(tf.shape(l_aug)[0])

o_init = tf.matmul(tf.transpose(l_aug), l_aug) / n


o_initialized = tf.assign(o, o_init, validate_shape=False, name='o_initialized')

o_setter = tf.assign(o, o_ph, validate_shape=False, name='o_setter')

o_diag = tf.diag_part(o, name='o_diag')

initialized_mu = tf.assign(mu, mu_init, validate_shape=False, name='mu_initialized')

p_initialized = tf.assign(p, tf.diag(p_init), validate_shape=False, name='p_initialized')

proba_mu = tf.clip_by_value(mu, 0.01, 0.99)

# TODO add support for dependencies
jtm = tf.ones(tf.shape(l_aug)[1])

x = tf.exp(tf.matmul(l_aug, tf.matmul(tf.diag(jtm), tf.log(proba_mu)) + tf.log(tf.diag_part(p))))
z = tf.reduce_sum(x, axis=1, keepdims=True)
probabilities = tf.div(x, z, name='probabilities')


loss1tmp = o - tf.matmul(mu, tf.matmul(p, tf.transpose(mu), name='PxMuT'), name='muxPxMuT')
masked = tf.boolean_mask(loss1tmp, mask)
loss_1 = tf.square(tf.norm(masked), name='loss_1')

loss_2 = tf.square(tf.norm(tf.reduce_sum(tf.matmul(mu, p, name='MuxP'), 1) - tf.diag_part(o)), name='loss_2')

# add optional l2 loss
loss_mu = loss_1 + loss_2

# learning rate could also be configurable via a placeholder
optimizer = tf.train.GradientDescentOptimizer(learning_rate=lr)
train_op = optimizer.minimize(loss_mu, var_list=[mu], name='train_op')

with tf.control_dependencies([train_op]):
    train_step = tf.identity(loss_mu, name='train_loss')

builder = tf.saved_model.builder.SavedModelBuilder('C:/tmp/labelModel')

generate_O_def = tf.saved_model.signature_def_utils.build_signature_def(
    inputs={'L_aug': tf.saved_model.build_tensor_info(l_aug)},
    outputs={'O': tf.saved_model.build_tensor_info(o_initialized)},
    method_name="generate_O"
)

sess.run(tf.global_variables_initializer())

builder.add_meta_graph_and_variables(sess=sess,
                                     tags=["LabelModel"],
                                     signature_def_map={'generate_O': generate_O_def })


builder.save()
