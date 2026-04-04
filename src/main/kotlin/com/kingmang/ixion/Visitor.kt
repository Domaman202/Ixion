package com.kingmang.ixion

interface Visitor<R> : StatementVisitor<R>, ExprVisitor<R>
