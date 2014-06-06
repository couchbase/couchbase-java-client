package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.java.query.dsl.Alias;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class LetElement implements Element {

    private final Alias[] aliases;

    public LetElement(Alias[] aliases) {
        this.aliases = aliases;
    }

    @Override
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("LET ");
        for (int i = 0; i < aliases.length; i++) {
            sb.append(aliases[i].toString());
            if (i < aliases.length-1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
