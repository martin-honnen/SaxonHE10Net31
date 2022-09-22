////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerable;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceCollection;
import net.sf.saxon.resource.AbstractResourceCollection;
import net.sf.saxon.resource.FailedResource;
import net.sf.saxon.trans.XPathException;

import java.util.Iterator;

/**
 * This class implements the {@link ResourceCollection} interface by wrapping an IEnumerable which
 * returns Uri values (the URIs of the documents in the collection). This is provided for backwards
 * compatibility with APIs offered on .NET.
 */

public class DotNetEnumerableCollection extends AbstractResourceCollection {

    IEnumerable list;

    /**
     * Create a DotNetEnumerableCollection.
     * @param config the Saxon Configuration
     * @param list a list (typically of System.Uri objects, but strings will do) of URIs of the
     *             resources in the collection
     */

    public DotNetEnumerableCollection(Configuration config, IEnumerable list) {
        super(config);
        this.list = list;
    }

    @Override
    public Iterator<? extends Resource> getResources(XPathContext context) throws XPathException {
        final Configuration config = context.getConfiguration();
        return new DotNetIterator<Resource>(list.GetEnumerator(), new DotNetIterator.Mapper<Resource>() {
            @Override
            public Resource convert(Object o) {
                try {
                    return makeResource(config, o.toString());
                } catch (XPathException e) {
                    return new FailedResource(o.toString(), e);
                }
            }
        });
    }

    @Override
    public Iterator<String> getResourceURIs(XPathContext context) throws XPathException {
        return new DotNetIterator<String>(list.GetEnumerator(), new DotNetIterator.Mapper<String>() {
            @Override
            public String convert(Object o) {
                return o.toString();
            }
        });
    }


}

