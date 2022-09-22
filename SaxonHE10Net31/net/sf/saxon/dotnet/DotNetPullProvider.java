////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Xml.*;
import cli.System.Xml.Schema.XmlSchemaException;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.ReceiverOption;
import net.sf.saxon.expr.parser.Loc;
import net.sf.saxon.om.*;
import net.sf.saxon.pull.PullProvider;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.CompressedWhitespace;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.AtomicValue;

import java.util.List;
import java.util.Stack;


/**
 * This class implements the Saxon PullProvider interface as a wrapper around a .NET XmlReader.
 */

public class DotNetPullProvider implements PullProvider, Location {

    private PipelineConfiguration pipe;
    private final XmlReader parser;
    private String baseURI;
    private boolean isEmptyElement = false;
    private Event current = Event.START_OF_INPUT;
    private boolean expandDefaults = true;
    private final Stack<NamespaceMap> namespaceStack = new Stack<>();

    /**
     * Create a PullProvider that wraps a .NET XML parser
     *
     * @param parser the .NET XML parser. In practice, the code relies on this being an XMLValidatingReader
     */

    public DotNetPullProvider(XmlReader parser) {
        this.parser = parser;
        namespaceStack.push(NamespaceMap.emptyMap());
    }

    /**
     * Set the base URI to be used. This is used only if the XmlReader cannot supply
     * a base URI.
     *
     * @param base the base URI
     */

    public void setBaseURI(String base) {
        baseURI = base;
    }

    /**
     * Close the event reader. This indicates that no further events are required.
     * It is not necessary to close an event reader after {@link Event#END_OF_INPUT} has
     * been reported, but it is recommended to close it if reading terminates
     * prematurely. Once an event reader has been closed, the effect of further
     * calls on next() is undefined.
     */

    @Override
    public void close() {
        parser.Close();
    }

    /**
     * Get the event most recently returned by next(), or by other calls that change
     * the position, for example getStringValue() and skipToMatchingEnd(). This
     * method does not change the position of the PullProvider.
     *
     * @return the current event
     */

    @Override
    public Event current() {
        return current;
    }

    /**
     * Get an atomic value. This call may be used only when the last event reported was
     * ATOMIC_VALUE. This indicates that the PullProvider is reading a sequence that contains
     * a free-standing atomic value; it is never used when reading the content of a node.
     */

    @Override
    public AtomicValue getAtomicValue() {
        return null;
    }

    /**
     * Get the attributes associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. The contents
     * of the returned AttributeCollection are guaranteed to remain unchanged
     * until the next START_ELEMENT event, but may be modified thereafter. The object
     * should not be modified by the client.
     * <p>Attributes may be read before or after reading the namespaces of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>
     *
     * @return an AttributeCollection representing the attributes of the element
     *         that has just been notified.
     */

    @Override
    public AttributeMap getAttributes() {
        if (parser.get_HasAttributes()) {
            AttributeMap atts = EmptyAttributeMap.getInstance();
            for (int i = 0; i < parser.get_AttributeCount(); i++) {
                parser.MoveToAttribute(i);

                final String prefix = parser.get_Prefix();
                final String namespaceURI = parser.get_NamespaceURI();
                final String localName = parser.get_LocalName();
                if ("xmlns".equals(prefix) || ("".equals(prefix) && "xmlns".equals(localName))) {
                    // skip the namespace declaration
                } else if (expandDefaults || !parser.get_IsDefault()) {
                    NodeName nc = new FingerprintedQName(prefix, namespaceURI, localName);
                    // .NET does not report the attribute type (even if it's an ID...)
                    atts = atts.put(new AttributeInfo(nc, BuiltInAtomicType.UNTYPED_ATOMIC, parser.get_Value(),
                                                      Loc.NONE, ReceiverOption.NONE));
                }
            }
            parser.MoveToElement();
            return atts;
        } else {
            return EmptyAttributeMap.getInstance();
        }
    }

    /**
     * Get the NodeName identifying the name of the current node. This method
     * can be used after the {@link Event#START_ELEMENT}, {@link Event#PROCESSING_INSTRUCTION},
     * {@link Event#ATTRIBUTE}, or {@link Event#NAMESPACE} events. With some PullProvider implementations,
     * it can also be used after {@link Event#END_ELEMENT}, but this is not guaranteed: a client who
     * requires the information at that point (for example, to do serialization) should insert an
     * {@link com.saxonica.xqj.pull.ElementNameTracker} into the pipeline.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is null.
     *
     * @return the NodeName. The NodeName can be used to obtain the prefix, local name,
     * and namespace URI.
     */
    @Override
    public NodeName getNodeName() {
        return new FingerprintedQName(parser.get_Prefix(), parser.get_NamespaceURI(), parser.get_LocalName());
    }

    /**
     * Get the namespace declarations associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. In the case of a top-level
     * START_ELEMENT event (that is, an element that either has no parent node, or whose parent
     * is not included in the sequence being read), the NamespaceDeclarations object returned
     * will contain a namespace declaration for each namespace that is in-scope for this element
     * node. In the case of a non-top-level element, the NamespaceDeclarations will contain
     * a set of namespace declarations and undeclarations, representing the differences between
     * this element and its parent.
     * <p>It is permissible for this method to return namespace declarations that are redundant.</p>
     * <p>The NamespaceDeclarations object is guaranteed to remain unchanged until the next START_ELEMENT
     * event, but may then be overwritten. The object should not be modified by the client.</p>
     * <p>Namespaces may be read before or after reading the attributes of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>
     */

    @Override
    public NamespaceBinding[] getNamespaceDeclarations() throws XPathException {
        if (parser.get_HasAttributes()) {
            int limit = parser.get_AttributeCount();
            NamespaceBinding[] nsBindings = new NamespaceBinding[limit];
            int used = 0;
            for (int i = 0; i < limit; i++) {
                parser.MoveToAttribute(i);
                final String prefix = parser.get_Prefix();
                final String localName = parser.get_LocalName();
                if ("xmlns".equals(prefix)) {
                    NamespaceBinding nscode = new NamespaceBinding(localName, parser.get_Value());
                    nsBindings[used++] = nscode;
                } else if (prefix.isEmpty() && "xmlns".equals(localName)) {
                    NamespaceBinding nscode = new NamespaceBinding("", parser.get_Value());
                    nsBindings[used++] = nscode;
                } else {
                    // ignore real attributes
                }
            }
            if (used < limit) {
                nsBindings[used] = null;
            }
            parser.MoveToElement();
            return nsBindings;
        } else {
            return NamespaceBinding.EMPTY_ARRAY;
        }

    }

    /**
     * Get configuration information.
     */

    @Override
    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Get the location of the current event.
     * For an event stream representing a real document, the location information
     * should identify the location in the lexical XML source. For a constructed document, it should
     * identify the location in the query or stylesheet that caused the node to be created.
     * A value of null can be returned if no location information is available.
     */

    @Override
    public Location getSourceLocator() {
        if (parser instanceof IXmlLineInfo || parser instanceof XmlValidatingReader) {
            return this;
        } else {
            return null;
        }
    }

    /**
     * Get the string value of the current element, text node, processing-instruction,
     * or top-level attribute or namespace node, or atomic value.
     * <p>In other situations the result is undefined and may result in an IllegalStateException.</p>
     * <p>If the most recent event was a {@link net.sf.saxon.pull.PullProvider.Event#START_ELEMENT}, this method causes the content
     * of the element to be read. The current event on completion of this method will be the
     * corresponding {@link net.sf.saxon.pull.PullProvider.Event#END_ELEMENT}. The next call of next() will return the event following
     * the END_ELEMENT event.</p>
     *
     * @return the String Value of the node in question, defined according to the rules in the
     *         XPath data model.
     */

    @Override
    public CharSequence getStringValue() throws XPathException {
        if (current == Event.TEXT) {
            return CompressedWhitespace.compress(parser.get_Value());
        } else {
            return parser.get_Value();
        }
    }

    /**
     * Get the type annotation of the current attribute or element node, or atomic value.
     * The result of this method is undefined unless the most recent event was START_ELEMENT,
     * ATTRIBUTE, or ATOMIC_VALUE.  In the case of an attribute node, the additional bit NodeInfo.IS_DTD_TYPE
     * may be set to indicate a DTD-derived ID or IDREF/S type.
     *
     * @return the type annotation.
     */

    @Override
    public SchemaType getSchemaType() {
        return Untyped.getInstance();
    }

    /**
     * Get the next event
     *
     * @return an integer code indicating the type of event. The code
     *         {@link net.sf.saxon.pull.PullProvider.Event#END_OF_INPUT} is returned at the end of the sequence.
     */

    @Override
    public Event next() throws XPathException {
        //System.err.println("next(), current = " + current + " empty: " + isEmptyElement);
        if (current == Event.START_OF_INPUT) {
            current = Event.START_DOCUMENT;
            return current;
        } else if (current == Event.END_DOCUMENT || current == Event.END_OF_INPUT) {
            current = Event.END_OF_INPUT;
            return current;
        } else if (current == Event.START_ELEMENT && isEmptyElement) {
            current = Event.END_ELEMENT;
            return current;
        }

        do {
            try {
                parser.Read();
                //noinspection ConstantIfStatement
                if (false) throw new XmlException("dummy"); // keeps the compiler happy
                //noinspection ConstantIfStatement
                if (false) throw new XmlSchemaException("dummy", new XmlException("dummy")); // keeps the compiler happy
            } catch (XmlException e) {
                XPathException de = new XPathException("Error reported by XML parser: " + e.getMessage(), e);
                Loc loc = new Loc(getSystemId(), e.get_LineNumber(), e.get_LinePosition());
                de.setLocator(loc);
                throw de;
            } catch (XmlSchemaException e) {
                XPathException de = new XPathException("Validation error reported by XML parser: " + e.getMessage(), e);
                Loc loc = new Loc(getSystemId(), e.get_LineNumber(), e.get_LinePosition());
                de.setLocator(loc);
                throw de;
            } catch (Exception e) {
                // The Microsoft spec says that the only exception thrown is XmlException. But
                // we've seen others, for example System.IO.FileNotFoundException when the DTD can't
                // be located
                XPathException de = new XPathException("Error reported by XML parser: " + e.getMessage(), e);
                Loc loc = new Loc(getSystemId(), -1, -1);
                de.setLocator(loc);
                throw de;
            }
            int intype = parser.get_NodeType().Value;
            isEmptyElement = parser.get_IsEmptyElement();
            //System.err.println("Next event: " + intype + " at depth " + parser.get_Depth() + " empty: " + isEmptyElement + "," + parser.get_IsEmptyElement());
            if (parser.get_EOF()) {
                current = Event.END_DOCUMENT;
                return current;
            }
            if (intype == XmlNodeType.EntityReference) {
                //parser.ResolveEntity();
                current = null;
            } else {
                current = mapInputKindToOutputKind(intype);
                if (current == Event.TEXT && parser.get_Depth() == 0) {
                    current = null;
                } else if (current == Event.START_ELEMENT) {
                    NamespaceMap nsMap = namespaceStack.peek();
                    if (parser.get_HasAttributes()) {
                        int limit = parser.get_AttributeCount();
                        for (int i = 0; i < limit; i++) {
                            parser.MoveToAttribute(i);
                            final String prefix = parser.get_Prefix();
                            final String localName = parser.get_LocalName();
                            if ("xmlns".equals(prefix)) {
                                nsMap = nsMap.bind(localName, parser.get_Value());
                            } else if (prefix.isEmpty() && "xmlns".equals(localName)) {
                                nsMap = nsMap.bind("", parser.get_Value());
                            }
                        }
                    }
                    namespaceStack.push(nsMap);
                } else if (current == Event.END_ELEMENT) {
                    namespaceStack.pop();
                }
            }
        } while (current == null);

        return current;
    }

    /**
     * Map the numbers used to identify events in the .NET XMLReader interface to the numbers used
     * by the Saxon PullProvider interface
     *
     * @param in the XMLReader event number
     * @return the Saxon PullProvider event number
     */

    private Event mapInputKindToOutputKind(int in) {
        // Note: we are losing unparsedEntities - see test expr02. Apparently unparsed entities are not
        // available via an XMLValidatingReader. We would have to build a DOM to get them, and that's too high
        // a price to pay.
        switch (in) {
            case XmlNodeType.Attribute:
                return Event.ATTRIBUTE;
            case XmlNodeType.CDATA:
                return Event.TEXT;
            case XmlNodeType.Comment:
                return Event.COMMENT;
            case XmlNodeType.Document:
                return Event.START_DOCUMENT;
            case XmlNodeType.DocumentFragment:
                return Event.START_DOCUMENT;
            case XmlNodeType.Element:
                return Event.START_ELEMENT;
            case XmlNodeType.EndElement:
                return Event.END_ELEMENT;
            case XmlNodeType.ProcessingInstruction:
                return Event.PROCESSING_INSTRUCTION;
            case XmlNodeType.SignificantWhitespace:
                //System.err.println("Significant whitespace");
                return Event.TEXT;
            case XmlNodeType.Text:
                return Event.TEXT;
            case XmlNodeType.Whitespace:
                //System.err.println("Plain whitespace");
                return Event.TEXT;
            //return -1;
            default:
                return null;
        }
    }

    /**
     * Set configuration information. This must only be called before any events
     * have been read.
     */

    @Override
    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        final Configuration config = pipe.getConfiguration();
        expandDefaults = config.isExpandAttributeDefaults();
    }

    /**
     * Skip the current subtree. This method may be called only immediately after
     * a START_DOCUMENT or START_ELEMENT event. This call returns the matching
     * END_DOCUMENT or END_ELEMENT event; the next call on next() will return
     * the event following the END_DOCUMENT or END_ELEMENT.
     *
     * @throws IllegalStateException if the method is called at any time other than
     *                               immediately after a START_DOCUMENT or START_ELEMENT event.
     */

    @Override
    public Event skipToMatchingEnd() throws XPathException {
        if (current == Event.START_ELEMENT) {
            current = Event.END_ELEMENT;
            parser.Skip();
        } else if (current == Event.START_DOCUMENT) {
            current = Event.END_DOCUMENT;
        } else {
            throw new IllegalStateException(current + "");
        }
        return current;
    }

    /**
     * Return the character position where the current document event ends.
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p>The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */
    @Override
    public int getColumnNumber() {
        if (parser instanceof IXmlLineInfo && ((IXmlLineInfo) parser).HasLineInfo()) {
            return ((IXmlLineInfo) parser).get_LinePosition();
        } else {
            return -1;
        }
    }

    /**
     * Return the line number where the current document event ends.
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p>The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The line number, or -1 if none is available.
     * @see #getColumnNumber
     */
    @Override
    public int getLineNumber() {
        if (parser instanceof IXmlLineInfo && ((IXmlLineInfo) parser).HasLineInfo()) {
            return ((IXmlLineInfo) parser).get_LineNumber();
        } else {
            return -1;
        }
    }

    /**
     * Return the public identifier for the current document event.
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    @Override
    public String getPublicId() {
        return null;
    }

    /**
     * Return the system identifier for the current document event.
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @return A string containing the system identifier, or null
     *         if none is available.
     * @see #getPublicId
     */
    @Override
    public String getSystemId() {
        String base = parser.get_BaseURI();
        if (base == null || base.isEmpty()) {
            return baseURI;
        } else {
            return base;
        }
    }

    /**
     * Get an immutable copy of this Location object. By default Location objects may be mutable, so they
     * should not be saved for later use. The result of this operation holds the same location information,
     * but in an immutable form.
     */
    @Override
    public Location saveLocation() {
        return new Loc(this);
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(int locationId) {
        return getColumnNumber();
    }

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    /**
     * Get a list of unparsed entities.
     *
     * @return a list of unparsed entities, or null if the information is not available, or
     *         an empty list if there are no unparsed entities.
     */

    /*@Nullable*/
    @Override
    public List<net.sf.saxon.pull.UnparsedEntity> getUnparsedEntities() {
        return null;
    }

}

