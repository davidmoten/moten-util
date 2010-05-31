package moten.david.markup;

import java.awt.Color;
import java.io.Serializable;

public class Tag<T extends Serializable> {
    private final Color color;
    private final String name;
    private final Class<T> type;
    private final TagScope scope;

    public Tag(Class<T> type, String name, TagScope scope, Color color) {
        this.type = type;
        this.name = name;
        this.scope = scope;
        this.color = color;
    }

    public TagScope getScope() {
        return scope;
    }

    public Color getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public Class<T> getType() {
        return type;
    }
}
