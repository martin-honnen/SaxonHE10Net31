////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Xml.XmlDocument;
import cli.System.Xml.XmlNode;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.PJConverter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExternalObjectModel;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.wrapper.VirtualNode;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.*;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * The DotNetObjectModel is an ExternalObjectModel that recognizes nodes as defined in the .Net
 * System.Xml namespace, and also recognizes the wrapper objects defined in the Saxon.Api interface. This
 * allows the Saxon.Api objects to be used withing extension functions. This is an abstract class because
 * it is designed to have no references to the classes in Saxon.Api; instead there is a concrete subclass in
 * the Saxon.Api package that has this knowledge.
 */
public abstract class DotNetObjectModel implements ExternalObjectModel {

    /**
     * Get the name of a characteristic class, which, if it can be loaded, indicates that the supporting libraries
     * for this object model implementation are available on the classpath
     *
     * @return by convention (but not necessarily) the class that implements a document node in the relevant
     * external model
     */
    @Override
    public String getDocumentClassName() {
        return "cli.System.Xml.XmlDocument";
    }

    /**
     * Get the URI of the external object model as used in the JAXP factory interfaces for obtaining
     * an XPath implementation
     */

    @Override
    public String getIdentifyingURI() {
        return NamespaceConstant.OBJECT_MODEL_DOT_NET_DOM;
    }

    @Override
    public PJConverter getPJConverter(Class<?> targetClass) {
        if (isRecognizedNodeClass(targetClass)) {
            return new PJConverter() {
                @Override
                public Object convert(Sequence value, Class<?> targetClass, XPathContext context) throws XPathException {
                    if (value instanceof ZeroOrOne) {
                        NodeInfo node = (NodeInfo) ((ZeroOrOne) value).head();
                        if (node instanceof VirtualNode) {
                            Object u = ((VirtualNode) node).getRealNode();
                            if (targetClass.isAssignableFrom(u.getClass())) {
                                return u;
                            }
                        }
                    } else if (value instanceof VirtualNode) {
                        Object u = ((VirtualNode) value).getRealNode();
                        if (targetClass.isAssignableFrom(u.getClass())) {
                            return u;
                        }
                    }
                    return null;
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public JPConverter getJPConverter(Class sourceClass, Configuration config) {
        if (isRecognizedNodeClass(sourceClass)) {
            return new JPConverter() {
                @Override
                public Sequence convert(Object object, XPathContext context) throws XPathException {
                    return unwrapXdmValue(object);
                }

                @Override
                public ItemType getItemType() {
                    return AnyNodeTest.getInstance();
                }
            };
        } else {
            return null;
        }
    }

    /**
     * Get a converter that converts a sequence of XPath nodes to this model's representation
     * of a node list.
     *
     * @param node an example of the kind of node used in this model
     * @return if the model does not recognize this node as one of its own, return null. Otherwise
     *         return a PJConverter that takes a list of XPath nodes (represented as NodeInfo objects) and
     *         returns a collection of nodes in this object model
     */

    @Override
    public PJConverter getNodeListCreator(Object node) {
        return null;
    }

    /**
     * Test whether the supplied object is an XDM value as defined in Saxon.Api
     * (implemented this way to avoid a reference to the Saxon.Api package)
     *
     * @param object the object under test
     * @return true if it is an instance of XdmValue
     */

    public abstract boolean isXdmValue(Object object);

    /**
     * Test whether the supplied type is an XDM atomic value type as defined in Saxon.Api
     * (implemented this way to avoid a reference to the Saxon.Api package)
     *
     * @param type the type under test
     * @return true if the type is a subtype of XdmAtomicValue
     */

    public abstract boolean isXdmAtomicValueType(cli.System.Type type);

    /**
     * Test whether the supplied type is an XDM value type as defined in Saxon.Api
     * (implemented this way to avoid a reference to the Saxon.Api package)
     *
     * @param type the type under test
     * @return true if the type is a subtype of XdmValue
     */

    public abstract boolean isXdmValueType(cli.System.Type type);

    /**
     * Unwrap an XdmValue
     *
     * @param object the supplied XdmValue
     * @return the underlying Value
     */

    public abstract Sequence unwrapXdmValue(Object object);

    /**
     * Wrap a Value as an XdmValue
     *
     * @param value the value to be wrapped
     * @return the resulting XdmValue
     */

    public abstract Object wrapAsXdmValue(Sequence value);

    /**
     * Test whether the supplied type is a subtype of System.Xml.XmlNode
     *
     * @param type the supplied type
     * @return true if the supplied type is System.Xml.XmlNode or a subtype thereof
     */

    public abstract boolean isXmlNodeType(cli.System.Type type);

    /**
     * Convert a Java object to an XPath value. If the supplied object is recognized as a representation
     * of a value using this object model, the object model should convert the value to an XPath value
     * and return this as the result. If not, it should return null. If the object is recognized but cannot
     * be converted, an exception should be thrown
     *
     * @param object the object to be converted
     * @param config the Saxon configuration
     * @return the result of the conversion if the object can be converted, or null otherwise
     * @throws net.sf.saxon.trans.XPathException
     *          if conversion fails
     */

    public Sequence convertObjectToXPathValue(Object object, Configuration config) throws XPathException {
        if (object instanceof XmlNode) {
            XmlNode node = (XmlNode) object;
            DotNetDocumentWrapper dw = new DotNetDocumentWrapper(
                    node.get_OwnerDocument(), node.get_BaseURI(), config);
            return dw.wrap(node);
        } else if (isXdmValue(object)) {
            return unwrapXdmValue(object);
        } else {
            return null;
        }
    }

    /**
     * Convert an XPath value to an object in this object model. If the supplied value can be converted
     * to an object in this model, of the specified class, then the conversion should be done and the
     * resulting object returned. If the value cannot be converted, the method should return null. Note
     * that the supplied class might be a List, in which case the method should inspect the contents of the
     * Value to see whether they belong to this object model.
     *
     * @param value       the value to be converted
     * @param targetClass the required class of the result of the conversion
     * @return the result of the conversion; always an instance of targetClass, or null if the value
     *         cannot be converted.
     * @throws net.sf.saxon.trans.XPathException
     *          if the target class is explicitly associated with this object model, but the
     *          supplied value cannot be converted to the appropriate class
     */

    public Object convertXPathValueToObject(Sequence value, cli.System.Type targetClass) throws XPathException {
        //System.err.println("CONVERT TO: " + targetClass.toString());
        if (isXmlNodeType(targetClass) &&
                value instanceof DotNetNodeWrapper) {
            return ((DotNetNodeWrapper) value).getRealNode();
        } else if (isXdmAtomicValueType(targetClass)) {
            SequenceIterator atomIterator = Atomizer.getAtomizingIterator(value.iterate(), false);
            GroundedValue extent = atomIterator.materialize();
            if (extent.getLength() == 0) {
                return null;
            } else if (extent instanceof AtomicValue) {
                return wrapAsXdmValue(extent);
            } else {
                throw new XPathException("Calling external method in " + targetClass +
                        ": supplied value is a sequence of more than one item");
            }
        } else if (isXdmValueType(targetClass)) {
            return wrapAsXdmValue(value);
        }
        return null;
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     *
     * @param result a JAXP result object
     * @return a Receiver that writes to that result, if available; or null otherwise
     */

    @Override
    public Receiver getDocumentBuilder(Result result) throws XPathException {
        return null;
    }

    /**
     * Test whether this object model recognizes a given node as one of its own. This method
     * will generally be called at run time.
     *
     * @param object An object that possibly represents a node
     * @return true if the object is a representation of a node in this object model
     */

    public boolean isRecognizedNode(Object object) {
        return object instanceof XmlNode;
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * node in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    private boolean isRecognizedNodeClass(Class nodeClass) {
        return XmlNode.class.isAssignableFrom(nodeClass);
    }


    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object,
     * and if it does, send the contents of the document to a supplied Receiver, and return true.
     * Otherwise, return false.
     *
     * @param source   a JAXP Source object
     * @param receiver the Receiver that is to receive the data from the Source
     * @return true if the data from the Source has been sent to the Receiver, false otherwise
     */

    @Override
    public boolean sendSource(Source source, Receiver receiver) throws XPathException {
        return false;
    }

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     *
     * @param source a JAXP Source object
     * @param config the Saxon configuration
     * @return a NodeInfo corresponding to the Source, if this can be constructed; otherwise null
     */

    @Override
    public NodeInfo unravel(Source source, Configuration config) {
        return null;
    }

    /**
     * Wrap a document node in the external object model in a document wrapper that implements
     * the Saxon DocumentInfo interface
     *
     * @param node    a node (any node) in the third party document
     * @param baseURI the base URI of the node (supply "" if unknown)
     * @param config  the Saxon configuration (which among other things provides access to the NamePool)
     * @return the wrapper, which must implement DocumentInfo
     */

    public TreeInfo wrapDocument(Object node, String baseURI, Configuration config) {
        if (node instanceof XmlDocument) {
            return new DotNetDocumentWrapper((XmlDocument) node, baseURI, config);
        } else {
            return null;
        }
    }

    /**
     * Wrap a node within the external object model in a node wrapper that implements the Saxon
     * VirtualNode interface (which is an extension of NodeInfo)
     *
     * @param document the document wrapper, as a DocumentInfo object
     * @param node     the node to be wrapped. This must be a node within the document wrapped by the
     *                 DocumentInfo provided in the first argument
     * @return the wrapper for the node, as an instance of VirtualNode
     */

    /*@Nullable*/
    public NodeInfo wrapNode(TreeInfo document, Object node) {
        if (document instanceof DotNetDocumentWrapper && node instanceof XmlNode) {
            return ((DotNetDocumentWrapper) document).wrap((XmlNode) node);
        } else {
            return null;
        }
    }
}

