////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerator;

import java.util.Iterator;

/**
 * This class maps a .NET IEnumerator to a Java Iterator. It optionally accepts a mapping function which
 * converts the objects delivered by the .NET IEnumerator to the required Java class.
 */

public class DotNetIterator<T extends Object> implements Iterator<T> {

    public interface Mapper<T> {
        T convert(Object o);
    }

    private final IEnumerator enumerator;
    private Mapper<T> mapper;
    private boolean moreToCome;


    public DotNetIterator() {
        this.enumerator = null;
        moreToCome = false;
    }

    public DotNetIterator(IEnumerator enumerator) {
        this.enumerator = enumerator;
        this.moreToCome = enumerator.MoveNext();
    }

    public DotNetIterator(IEnumerator enumerator, Mapper<T> mapper) {
        this.enumerator = enumerator;
        this.mapper = mapper;
        this.moreToCome = enumerator.MoveNext();
    }

    @Override
    public boolean hasNext() {
        return moreToCome;
    }

    @Override
    public T next() {
        Object o = enumerator.get_Current();
        this.moreToCome = enumerator.MoveNext();
        if (mapper != null) {
            return mapper.convert(o);
        } else {
            return (T) o;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

