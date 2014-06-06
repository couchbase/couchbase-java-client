package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.element.Element;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public abstract class AbstractPath implements Path {

    private Element element;
    private AbstractPath parent;

    protected AbstractPath(AbstractPath parent) {
        this.parent = parent;
    }

    private String render() {
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent.render()).append(" ");
        }
        if (element != null) {
            sb.append(element.export());
        }
        return sb.toString();
    }

    protected void element(Element element) {
        this.element = element;
    }

    @Override
    public String toString() {
        return render().trim();
    }

}
