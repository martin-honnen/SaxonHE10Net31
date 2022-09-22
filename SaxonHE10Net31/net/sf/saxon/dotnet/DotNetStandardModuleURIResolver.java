////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.IO.Stream;
import cli.System.IO.TextReader;
import cli.System.Type;
import cli.System.Uri;
import cli.System.Xml.XmlResolver;
import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.stream.StreamSource;
import java.net.URISyntaxException;


/**
 * This class is the standard ModuleURIResolver used to implement the "import module" declaration
 * in a Query Prolog. It is used when no user-defined ModuleURIResolver has been specified, or when
 * the user-defined ModuleURIResolver decides to delegate to the standard ModuleURIResolver.
 *
 */

public class DotNetStandardModuleURIResolver implements ModuleURIResolver {

    private transient XmlResolver resolver;

    public DotNetStandardModuleURIResolver() {
    }

    public DotNetStandardModuleURIResolver(XmlResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Resolve a module URI and associated location hints.
     *
     * @param moduleURI The module namespace URI of the module to be imported; or null when
     *                  loading a non-library module.
     * @param baseURI   The base URI of the module containing the "import module" declaration;
     *                  null if no base URI is known
     * @param locations The set of URIs specified in the "at" clause of "import module",
     *                  which serve as location hints for the module
     * @return an array of StreamSource objects each identifying the contents of a module to be
     *         imported. Each StreamSource must contain a
     *         non-null absolute System ID which will be used as the base URI of the imported module,
     *         and either an InputSource or a Reader representing the text of the module. The method
     *         may also return null, in which case the system attempts to resolve the URI using the
     *         standard module URI resolver.
     * @throws net.sf.saxon.trans.XPathException
     *          if the module cannot be located
     */

    @Override
    public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException {
        if (locations.length == 0) {
            XPathException err = new XPathException("Cannot locate module for namespace " + moduleURI);
            err.setErrorCode("XQST0059");
            err.setIsStaticError(true);
            throw err;
        } else {
            // One or more locations given: import modules from all these locations
            if(baseURI == null) {
                if(baseURI == null) {
                    try {
                        baseURI =  getClass().getProtectionDomain().getCodeSource().getLocation().toURI().toString();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        XPathException se = new XPathException("Cannot resolve relative URI " + moduleURI, e);
                        se.setErrorCode("XQST0059");
                        se.setIsStaticError(true);
                        throw se;
                    }
                }
            }
            Uri base = new Uri(baseURI);
            StreamSource[] sources = new StreamSource[locations.length];
            for (int m = 0; m < locations.length; m++) {
                String href = locations[m];
                Uri absoluteURI;
                try {
                    absoluteURI = resolver.ResolveUri(base, href);
                } catch (Throwable err) {
                    XPathException se = new XPathException("Cannot resolve relative URI " + href, err);
                    se.setErrorCode("XQST0059");
                    se.setIsStaticError(true);
                    throw se;
                }
                sources[m] = getQuerySource(absoluteURI);
            }
            return sources;
        }
    }

    /**
     * Get a StreamSource object representing the source of a query, given its URI.
     * If the encoding can be determined, it returns a StreamSource containing a Reader that
     * performs the required decoding. Otherwise, it returns a StreamSource containing an
     * InputSource, leaving the caller to sort out encoding problems.
     *
     * @param abs the absolute URI of the source query
     * @return a StreamSource containing a Reader or InputSource, as well as a systemID representing
     *         the base URI of the query.
     * @throws XPathException if the URIs are invalid or cannot be resolved or dereferenced, or
     *                        if any I/O error occurs
     */

    private StreamSource getQuerySource(Uri abs)
            throws XPathException {

        try {

            Object obj = resolver.GetEntity(abs, "application/xquery", Type.GetType("System.IO.Stream"));
            // expect cli.System.IO.FileNotFoundException if this fails
            if (obj instanceof Stream) {
                StreamSource source = new StreamSource(new DotNetInputStream((Stream) obj));
                source.setSystemId(abs.toString());
                return source;
            } else if (obj instanceof TextReader) {
                StreamSource source = new StreamSource(new DotNetReader((TextReader) obj));
                source.setSystemId(abs.toString());
                return source;
            } else if (obj instanceof StreamSource) {
                return ((StreamSource) obj);
            } else {
                throw new XPathException(
                        "Unrecognized object returned by XmlResolver (type " + obj.getClass().getName());
            }
        } catch (XPathException e) {
            throw e;
        } catch (Throwable e) {
            throw new XPathException(e.getMessage(), e);
        }

    }
}

