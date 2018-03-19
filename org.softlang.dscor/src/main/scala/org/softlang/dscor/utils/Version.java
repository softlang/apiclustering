package org.softlang.dscor.utils;

import java.io.Serializable;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;

/**
 * Created by Johannes on 03.11.2017. A serializable wrapper around aether version.
 */
public class Version implements Serializable, Comparable<Version> {

    private final String content;

    public Version(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }

    @Override
    public int compareTo(Version o) {
        GenericVersionScheme scheme = new GenericVersionScheme();
        try {
            org.eclipse.aether.version.Version v1 = scheme.parseVersion(content);
            org.eclipse.aether.version.Version v2 = scheme.parseVersion(o.content);
            return v1.compareTo(v2);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        return content.equals(version.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
