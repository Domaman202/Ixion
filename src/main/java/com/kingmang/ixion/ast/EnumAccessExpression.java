package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;
import org.jetbrains.annotations.NotNull;

public class EnumAccessExpression extends Expression {
    public final Expression enumType;
    public final IdentifierExpression enumValue;
    
    public EnumAccessExpression(Position pos, Expression enumType, IdentifierExpression enumValue) {
        super(pos);
        this.enumType = enumType;
        this.enumValue = enumValue;
    }

    @Override
    public <R> R accept(@NotNull ExprVisitor<R> visitor) {
        return visitor.visitEnumAccess(this);
    }
}