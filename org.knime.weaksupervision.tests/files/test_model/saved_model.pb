ì+
øÕ
x
Assign
ref"T

value"T

output_ref"T"	
Ttype"
validate_shapebool("
use_lockingbool(
8
Const
output"dtype"
valuetensor"
dtypetype
.
Identity

input"T
output"T"	
Ttype
e
MergeV2Checkpoints
checkpoint_prefixes
destination_prefix"
delete_old_dirsbool(

NoOp
M
Pack
values"T*N
output"T"
Nint(0"	
Ttype"
axisint 
C
Placeholder
output"dtype"
dtypetype"
shapeshape:
o
	RestoreV2

prefix
tensor_names
shape_and_slices
tensors2dtypes"
dtypes
list(type)(0
l
SaveV2

prefix
tensor_names
shape_and_slices
tensors2dtypes"
dtypes
list(type)(0
H
ShardedFilename
basename	
shard

num_shards
filename
N

StringJoin
inputs*N

output"
Nint(0"
	separatorstring 
s

VariableV2
ref"dtype"
shapeshape"
dtypetype"
	containerstring "
shared_namestring "
test_model*1.12.02
b'unknown' 
c
boolean_var/initial_valueConst*
valueB
Z *
dtype0
*
_output_shapes
:
s
boolean_var
VariableV2*
dtype0
*
shared_name *
shape:*
	container *
_output_shapes
:
²
boolean_var/AssignAssignboolean_varboolean_var/initial_value*
T0
*
use_locking(*
validate_shape( *
_class
loc:@boolean_var*
_output_shapes
:
l
boolean_var/readIdentityboolean_var*
T0
*
_class
loc:@boolean_var*
_output_shapes
:
R
zerosConst*
valueB*    *
dtype0*
_output_shapes
:
q
	float_var
VariableV2*
dtype0*
shared_name *
shape:*
	container *
_output_shapes
:

float_var/AssignAssign	float_varzeros*
T0*
use_locking(*
validate_shape( *
_class
loc:@float_var*
_output_shapes
:
f
float_var/readIdentity	float_var*
T0*
_class
loc:@float_var*
_output_shapes
:
X
boolean_placeholderPlaceholder*
dtype0
*
shape:*
_output_shapes
:
V
float_placeholderPlaceholder*
dtype0*
shape:*
_output_shapes
:
¦
boolean_assignAssignboolean_varboolean_placeholder*
T0
*
use_locking(*
validate_shape( *
_class
loc:@boolean_var*
_output_shapes
:

float_assignAssign	float_varfloat_placeholder*
T0*
use_locking(*
validate_shape( *
_class
loc:@float_var*
_output_shapes
:
4
initNoOp^boolean_var/Assign^float_var/Assign
P

save/ConstConst*
valueB Bmodel*
dtype0*
_output_shapes
: 

save/StringJoin/inputs_1Const*<
value3B1 B+_temp_23a153ebf9d645298671f21265d537a4/part*
dtype0*
_output_shapes
: 
u
save/StringJoin
StringJoin
save/Constsave/StringJoin/inputs_1*
N*
	separator *
_output_shapes
: 
Q
save/num_shardsConst*
value	B :*
dtype0*
_output_shapes
: 
k
save/ShardedFilename/shardConst"/device:CPU:0*
value	B : *
dtype0*
_output_shapes
: 

save/ShardedFilenameShardedFilenamesave/StringJoinsave/ShardedFilename/shardsave/num_shards"/device:CPU:0*
_output_shapes
: 

save/SaveV2/tensor_namesConst"/device:CPU:0*+
value"B Bboolean_varB	float_var*
dtype0*
_output_shapes
:
v
save/SaveV2/shape_and_slicesConst"/device:CPU:0*
valueBB B *
dtype0*
_output_shapes
:

save/SaveV2SaveV2save/ShardedFilenamesave/SaveV2/tensor_namessave/SaveV2/shape_and_slicesboolean_var	float_var"/device:CPU:0*
dtypes
2

 
save/control_dependencyIdentitysave/ShardedFilename^save/SaveV2"/device:CPU:0*
T0*'
_class
loc:@save/ShardedFilename*
_output_shapes
: 
¬
+save/MergeV2Checkpoints/checkpoint_prefixesPacksave/ShardedFilename^save/control_dependency"/device:CPU:0*

axis *
T0*
N*
_output_shapes
:

save/MergeV2CheckpointsMergeV2Checkpoints+save/MergeV2Checkpoints/checkpoint_prefixes
save/Const"/device:CPU:0*
delete_old_dirs(

save/IdentityIdentity
save/Const^save/MergeV2Checkpoints^save/control_dependency"/device:CPU:0*
T0*
_output_shapes
: 

save/RestoreV2/tensor_namesConst"/device:CPU:0*+
value"B Bboolean_varB	float_var*
dtype0*
_output_shapes
:
y
save/RestoreV2/shape_and_slicesConst"/device:CPU:0*
valueBB B *
dtype0*
_output_shapes
:
¤
save/RestoreV2	RestoreV2
save/Constsave/RestoreV2/tensor_namessave/RestoreV2/shape_and_slices"/device:CPU:0*
dtypes
2
*
_output_shapes

::

save/AssignAssignboolean_varsave/RestoreV2*
T0
*
use_locking(*
validate_shape( *
_class
loc:@boolean_var*
_output_shapes
:

save/Assign_1Assign	float_varsave/RestoreV2:1*
T0*
use_locking(*
validate_shape( *
_class
loc:@float_var*
_output_shapes
:
8
save/restore_shardNoOp^save/Assign^save/Assign_1
-
save/restore_allNoOp^save/restore_shard"<
save/Const:0save/Identity:0save/restore_all (5 @F8"£
	variables
T
boolean_var:0boolean_var/Assignboolean_var/read:02boolean_var/initial_value:0
:
float_var:0float_var/Assignfloat_var/read:02zeros:0*
assign_boolean_varl
0
float_placeholder
float_placeholder:0&
float_assign
float_assign:0eassign_float_var*
assign_float_varl
0
float_placeholder
float_placeholder:0&
float_assign
float_assign:0eassign_float_var