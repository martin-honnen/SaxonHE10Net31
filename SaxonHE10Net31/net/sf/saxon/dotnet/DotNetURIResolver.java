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
import net.sf.saxon.functions.ResolveURI;
import net.sf.saxon.lib.AugmentedSource;
import net.sf.saxon.lib.RelativeURIResolver;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

/**
 * This class implements the JAXP URIResolver as a wrapper around
 * a .NET XmlResolver. The class also acts as an EntityResolver, so that it resolves
 * entity references on behalf of the XML parser as well as XSLT-level references.
 */

public class DotNetURIResolver implements RelativeURIResolver, EntityResolver {

    private final XmlResolver resolver;

    /**
     * Create a URIResolver that wraps a .NET XmlResolver
     *
     * @param resolver the XmlResolver to be wrapped
     */

    public DotNetURIResolver(XmlResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Get the .NET XmlResolver underpinning this URIResolver
     *
     * @return the wrapped .NET XmlResolver
     */

    public XmlResolver getXmlResolver() {
        return resolver;
    }

    /**
     * Create an absolute URI from a relative URI and a base URI. This method performs the
     * process which is correctly called "URI resolution": this is purely a syntactic operation
     * on the URI strings, and does not retrieve any resources.
     *
     * @param href A relative or absolute URI, to be resolved against the specified base URI
     * @param base The base URI against which the first argument will be made
     *             absolute if the absolute URI is required.
     * @return A string containing the absolute URI that results from URI resolution. If the resource
     *         needs to be fetched, this absolute URI will be supplied as the href parameter in a subsequent
     *         call to the <code>resolve</code> method.
     */

    @Override
    public String makeAbsolute(String href, /*@Nullable*/ String base) throws TransformerException {
        if (base == null || base.isEmpty()) {
            try {
                //noinspection ConstantIfStatement
                if (false) throw new cli.System.UriFormatException();
                return new Uri(href).ToString();
            } catch (cli.System.UriFormatException e) {
                // if the base URI is null, or is itself a relative URI, we
                // try to expand it relative to the current working directory
                String expandedBase = ResolveURI.tryToExpand(base);
                if (!expandedBase.equals(base)) { // prevent infinite recursion
                    return makeAbsolute(href, expandedBase);
                } else {
                    XPathException de = new XPathException("Invalid URI: " + e.getMessage());
                    de.setErrorCode("FODC0005");
                    throw de;
                }
            } catch (Exception e) {
                XPathException de = new XPathException("Invalid URI: " + e.getMessage());
                de.setErrorCode("FODC0005");
                throw de;
            }
        } else {
            try {
                //noinspection ConstantIfStatement
                if (false) throw new cli.System.UriFormatException();
                return resolver.ResolveUri(new Uri(base), href).ToString();
            } catch (cli.System.UriFormatException e) {
                XPathException de = new XPathException("Failure making absolute URI (base=" +
                        base + ", relative=" + href + "): " + e.getMessage());
                de.setErrorCode("FODC0005");
                throw de;
            } catch (Exception e) {
                XPathException de = new XPathException("Failure making absolute URI (base=" +
                        base + ", relative=" + href + "): " + e.getMessage());
                de.setErrorCode("FODC0005");
                throw de;
            }
        }
    }

    /**
     * Called by an XSLT processor when it encounters
     * an xsl:include, xsl:import, or document() function.
     *
     * @param href An href attribute, holding a relative or absolute URI.
     * @param base The base URI, ignored if href is absolute.
     * @return A Source object, or null if the href cannot be resolved,
     *         and the processor should try to resolve the URI itself.
     * @throws javax.xml.transform.TransformerException
     *          if an error occurs when trying to
     *          resolve the URI.
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        return dereference(makeAbsolute(href, base));
    }

    /**
     * Called by the processor when it encounters
     * an xsl:include, xsl:import, or document() function.
     *
     * @param uri The absolute URI to be dereferenced
     * @return A Source object, or null if the href cannot be dereferenced,
     *         and the processor should try to resolve the URI itself.
     * @throws javax.xml.transform.TransformerException
     *          if an error occurs when trying to
     *          dereference the URI.
     */
    @Override
    public Source dereference(String uri) throws XPathException {
        //System.err.println("Resolving " + href + " against " + base);
        try {
            Uri abs = new Uri(uri);
            Object obj = resolver.GetEntity(abs, null, Type.GetType("System.IO.Stream"));
            // expect cli.System.IO.FileNotFoundException if this fails
            if (obj instanceof Stream) {
                StreamSource source = new StreamSource(new DotNetInputStream((Stream) obj));
                source.setSystemId(abs.toString());
                AugmentedSource as = AugmentedSource.makeAugmentedSource(source);
                as.setPleaseCloseAfterUse(true);
                return as;
            } else if (obj instanceof TextReader) {
                StreamSource source = new StreamSource(new DotNetReader((TextReader) obj));
                source.setSystemId(abs.toString());
                return source;
            } else if (obj instanceof Source) {
                return ((Source) obj);
            } else if(obj == null){
                return null;
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

    /**
     * Allow the application to resolve external entities.
     * <p>The parser will call this method before opening any external
     * entity except the top-level document entity.  Such entities include
     * the external DTD subset and external parameter entities referenced
     * within the DTD (in either case, only if the parser reads external
     * parameter entities), and external general entities referenced
     * within the document element (if the parser reads external general
     * entities).  The application may request that the parser locate
     * the entity itself, that it use an alternative URI, or that it
     * use data provided by the application (as a character or byte
     * input stream).</p>
     * <p>Application writers can use this method to redirect external
     * system identifiers to secure and/or local URIs, to look up
     * public identifiers in a catalogue, or to read an entity from a
     * database or other input source (including, for example, a dialog
     * box).  Neither XML nor SAX specifies a preferred policy for using
     * public or system IDs to resolve resources.  However, SAX specifies
     * how to interpret any InputSource returned by this method, and that
     * if none is returned, then the system ID will be dereferenced as
     * a URL.  </p>
     * <p>If the system identifier is a URL, the SAX parser must
     * resolve it fully before reporting it to the application.</p>
     *
     * @param publicId The public identifier of the external entity
     *                 being referenced, or null if none was supplied.
     * @param systemId The system identifier of the external entity
     *                 being referenced.
     * @return An InputSource object describing the new input source,
     *         or null to request that the parser open a regular
     *         URI connection to the system identifier.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *                                  wrapping another exception.
     * @throws java.io.IOException      A Java-specific IO exception,
     *                                  possibly the result of creating a new InputStream
     *                                  or Reader for the InputSource.
     * @see org.xml.sax.InputSource
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        try {
            Uri abs = new Uri(systemId);
            Object obj = resolver.GetEntity(abs, null, Type.GetType("System.IO.Stream"));
            // expect cli.System.IO.FileNotFoundException if this fails
            if (obj instanceof Stream) {
                InputSource source = new InputSource(new DotNetInputStream((Stream) obj));
                source.setSystemId(abs.toString());
                return source;
            } else {
                throw new XPathException(
                        "Unrecognized object returned by XmlResolver (type " + obj.getClass().getName());
            }
        } catch (Exception e) {
            throw new SAXException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new SAXException(e.getMessage());
        }
    }
}

