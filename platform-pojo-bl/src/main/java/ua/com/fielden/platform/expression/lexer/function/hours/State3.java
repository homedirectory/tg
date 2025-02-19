package ua.com.fielden.platform.expression.lexer.function.hours;

import ua.com.fielden.platform.expression.automata.AbstractState;
import ua.com.fielden.platform.expression.automata.NoTransitionAvailable;

public class State3 extends AbstractState {

    public State3() {
        super("S3", false);
    }

    @Override
    protected AbstractState transition(final char symbol) throws NoTransitionAvailable {
        if (symbol == 'r' || symbol == 'R') {
            return getAutomata().getState("S4");
        }
        throw new NoTransitionAvailable("Invalid symbol '" + symbol + "'", this, symbol);
    }

}
