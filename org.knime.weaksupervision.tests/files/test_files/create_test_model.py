import tensorflow as tf

with tf.Session() as sess:

    boolean_var = tf.Variable([False], trainable=False, validate_shape=False, name='boolean_var')
    float_var = tf.Variable(tf.zeros((1,)), trainable=False, validate_shape=False, name='float_var')

    boolean_placeholder = tf.placeholder(tf.bool, name='boolean_placeholder')
    float_placeholder = tf.placeholder(tf.float32, name='float_placeholder')

    boolean_assign = tf.assign(boolean_var, boolean_placeholder, validate_shape=False, name='boolean_assign')
    float_assign = tf.assign(float_var, float_placeholder, validate_shape=False, name='float_assign')

    builder = tf.saved_model.builder.SavedModelBuilder('C:/tmp/test_model')
    assign_boolean_var_def = tf.saved_model.build_signature_def(
        inputs={'boolean_placeholder': tf.saved_model.build_tensor_info(boolean_placeholder)},
        outputs={'boolean_assign': tf.saved_model.build_tensor_info(boolean_assign)},
        method_name='assign_boolean_var'
    )

    assign_float_var_def = tf.saved_model.build_signature_def(
        inputs={'float_placeholder': tf.saved_model.build_tensor_info(float_placeholder)},
        outputs={'float_assign': tf.saved_model.build_tensor_info(float_assign)},
        method_name='assign_float_var'
    )
    sess.run(tf.global_variables_initializer())
    builder.add_meta_graph_and_variables(sess=sess, tags=['test_model'],
                                         signature_def_map={
                                             'assign_boolean_var': assign_float_var_def,
                                             'assign_float_var': assign_float_var_def
                                         })
    builder.save()
