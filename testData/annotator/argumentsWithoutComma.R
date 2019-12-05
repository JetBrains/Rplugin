foo1(x, 10 <error descr="missing comma">y</error>yy)
foo2(x, 10 + 1 <info descr="R_NAMED_ARGUMENT"><error descr="missing comma">n</error>ame</info> = y + 2 z, 3)
<info descr="R_FUNCTION_CALL">ok</info>(x, 10 - y)
foo3(1 <error descr="missing comma">!</error> 2)
foo4(x, y
<error descr="missing comma">z</error>zz)
