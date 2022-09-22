////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Xml.*;
import net.sf.saxon.event.NamespaceReducer;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.tree.util.SteppingNavigator;
import net.sf.saxon.tree.util.SteppingNode;
import net.sf.saxon.tree.wrapper.AbstractNodeWrapper;
import net.sf.saxon.tree.wrapper.SiblingCountingNode;
import net.sf.saxon.type.Type;

import java.util.*;
import java.util.function.Predicate;


/**
 * A node in the XML parse tree representing an XML element, character content, or attribute.
 * <p>This is the implementation of the NodeInfo interface used as a wrapper for DOM nodes as
 * provided in the .NET System.Xml implementation.</p>
 */

public class DotNetNodeWrapper extends AbstractNodeWrapper implements SteppingNode<DotNetNodeWrapper>, SiblingCountingNode {

    protected XmlNode node;
    protected short nodeKind;
    protected DotNetNodeWrapper parent;     // null means unknown
    protected DotNetDocumentWrapper docWrapper;
    protected int index;            // -1 means unknown
    protected int span = 1;         // the number of adjacent text nodes wrapped by this NodeWrapper.
    // If span>1, node will always be the first of a sequence of adjacent text nodes

    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     *
     * @param node   The DOM node to be wrapped
     * @param parent The NodeWrapper that wraps the parent of this node
     * @param index  Position of this node among its siblings
     */
    protected DotNetNodeWrapper(XmlNode node, DotNetNodeWrapper parent, int index) {
        //System.err.println("Creating NodeWrapper for " +node);
        this.node = node;
        this.parent = parent;
        this.index = index;
    }

    DotNetNodeWrapper() {
    }

    /**
     * Factory method to wrap a DOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     *
     * @param node       The DOM node
     * @param docWrapper The wrapper for the containing Document node
     * @return The new wrapper for the supplied node
     * @throws NullPointerException if the node or the document wrapper are null
     */
    protected static DotNetNodeWrapper makeWrapper(XmlNode node, DotNetDocumentWrapper docWrapper) {
        if (node == null) {
            throw new NullPointerException("NodeWrapper#makeWrapper: Node must not be null");
        }
        if (docWrapper == null) {
            throw new NullPointerException("NodeWrapper#makeWrapper: DocumentWrapper must not be null");
        }
        return makeWrapper(node, docWrapper, null, -1);
    }

    /**
     * Factory method to wrap a DOM node with a wrapper that implements the Saxon
     * NodeInfo interface.
     *
     * @param node       The DOM node
     * @param docWrapper The wrapper for the containing Document node     *
     * @param parent     The wrapper for the parent of the JDOM node
     * @param index      The position of this node relative to its siblings
     * @return The new wrapper for the supplied node
     */

    protected static DotNetNodeWrapper makeWrapper(XmlNode node, DotNetDocumentWrapper docWrapper,
                                            /*@Nullable*/ DotNetNodeWrapper parent, int index) {
        DotNetNodeWrapper wrapper;
        switch (node.get_NodeType().Value) {
            case XmlNodeType.Document:
                //case Node.DOCUMENT_FRAGMENT_NODE:
                wrapper = (DotNetNodeWrapper)docWrapper.getRootNode();
                if (wrapper == null) {
                    wrapper = new DotNetNodeWrapper(node, parent, index);
                    wrapper.nodeKind = Type.DOCUMENT;
                }
                break;
            case XmlNodeType.Element:
                wrapper = new DotNetNodeWrapper(node, parent, index);
                wrapper.nodeKind = Type.ELEMENT;
                break;
            case XmlNodeType.Attribute:
                wrapper = new DotNetNodeWrapper(node, parent, index);
                wrapper.nodeKind = Type.ATTRIBUTE;
                break;
            case XmlNodeType.Text:
            case XmlNodeType.CDATA:
            case XmlNodeType.Whitespace:
            case XmlNodeType.SignificantWhitespace:
                wrapper = new DotNetNodeWrapper(node, parent, index);
                wrapper.nodeKind = Type.TEXT;
                break;

            case XmlNodeType.Comment:
                wrapper = new DotNetNodeWrapper(node, parent, index);
                wrapper.nodeKind = Type.COMMENT;
                break;
            case XmlNodeType.ProcessingInstruction:
                wrapper = new DotNetNodeWrapper(node, parent, index);
                wrapper.nodeKind = Type.PROCESSING_INSTRUCTION;
                break;
            default:
                throw new IllegalArgumentException("Unsupported node type in DOM! " + node.get_NodeType() + " instance " + node.toString());
        }
        wrapper.docWrapper = docWrapper;
        wrapper.treeInfo = docWrapper;
        return wrapper;
    }

    /**
     * Get the underlying DOM node, to implement the VirtualNode interface
     */

    @Override
    public XmlNode getUnderlyingNode() {
        return node;
    }

    /**
     * Return the type of node.
     *
     * @return one of the values Node.ELEMENT, Node.TEXT, Node.ATTRIBUTE, etc.
     */

    @Override
    public int getNodeKind() {
        return nodeKind;
    }


    /**
     * Determine whether this is the same node as another node.
     * <p>Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b)</p>
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean equals(NodeInfo other) {
        // On .NET, the DOM appears to guarantee that the same node is always represented
        // by the same object

        return other instanceof DotNetNodeWrapper && node == ((DotNetNodeWrapper) other).node;

    }

    /**
     * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
     * (represent the same node) then they must have the same hashCode()
     *
     * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics.
     */

    public int hashCode() {
        FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.C64);
        generateId(buffer);
        return buffer.toString().hashCode();
    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this node
     * @return -1 if this node precedes the other node, +1 if it follows the other
     *         node, or 0 if they are the same node. (In this case, isSameNode() will always
     *         return true, and the two nodes will produce the same result for generateId())
     */

    @Override
    public int compareOrder(NodeInfo other) {
        if (other instanceof SiblingCountingNode) {
            return Navigator.compareOrder(this, (SiblingCountingNode) other);
        } else {
            // it's presumably a Namespace Node
            return -other.compareOrder(this);
        }
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    @Override
    public CharSequence getStringValueCS() {
        switch (nodeKind) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                return node.get_InnerText();

            case Type.ATTRIBUTE:
                return node.get_Value();

            case Type.TEXT:
                if (span == 1) {
                    return node.get_InnerText();
                } else {
                    FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C64);
                    XmlNode textNode = node;
                    for (int i = 0; i < span; i++) {
                        fsb.append(textNode.get_InnerText());
                        textNode = textNode.get_NextSibling();
                    }
                    return fsb.condense();
                }

            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
                return node.get_Value();

            default:
                return "";
        }
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns null, except for
     *         un unnamed namespace node, which returns "".
     */

    @Override
    public String getLocalPart() {
        switch (nodeKind) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
            case Type.PROCESSING_INSTRUCTION:
                return node.get_LocalName();
            default:
                return "";
        }
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *         or for a node with an empty prefix, return an empty
     *         string.
     */

    @Override
    public String getURI() {
        NodeInfo element;
        if (nodeKind == Type.ELEMENT) {
            element = this;
        } else if (nodeKind == Type.ATTRIBUTE) {
            element = parent;
        } else {
            return "";
        }

        // The DOM methods getPrefix() and getNamespaceURI() do not always
        // return the prefix and the URI; they both return null, unless the
        // prefix and URI have been explicitly set in the node by using DOM
        // level 2 interfaces. There's no obvious way of deciding whether
        // an element whose name has no prefix is in the default namespace,
        // other than searching for a default namespace declaration. So we have to
        // be prepared to search.

        // If getPrefix() and getNamespaceURI() are non-null, however,
        // we can use the values.

        String uri = node.get_NamespaceURI();
        if (uri != null && uri.length() > 0) {
            return uri;
        }

        // Otherwise we have to work it out the hard way...

        if (node.get_Name().startsWith("xml:")) {
            return NamespaceConstant.XML;
        }

        String[] parts;
        try {
            parts = NameChecker.getQNameParts(node.get_Name());
        } catch (QNameException e) {
            throw new IllegalStateException("Invalid QName in DOM node. " + e);
        }

        if (nodeKind == Type.ATTRIBUTE && parts[0].isEmpty()) {
            // for an attribute, no prefix means no namespace
            uri = "";
        } else {
            AxisIterator nsiter = element.iterateAxis(AxisInfo.NAMESPACE);
            NodeInfo ns;
            while ((ns = nsiter.next()) != null) {
                if (ns.getLocalPart().equals(parts[0])) {
                    uri = ns.getStringValue();
                    break;
                }
            }
            if (uri == null) {
                if (parts[0].isEmpty()) {
                    uri = "";
                } else {
                    throw new IllegalStateException("Undeclared namespace prefix in DOM input: " + parts[0]);
                }
            }
        }
        return uri;
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     * This implementation simply returns the prefix defined in the DOM model; this is not strictly
     * accurate in all cases, but is good enough for the purpose.
     *
     * @return The prefix of the name of the node.
     */

    @Override
    public String getPrefix() {
        return node.get_Prefix();
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node.
     *         For a node with no name, return an empty string.
     */

    @Override
    public String getDisplayName() {
        switch (nodeKind) {
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
            case Type.PROCESSING_INSTRUCTION:
                return node.get_Name();
            default:
                return "";

        }
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     */

    @Override
    public DotNetNodeWrapper getParent() {
        if (parent == null) {
            switch (getNodeKind()) {
                case Type.ATTRIBUTE:
                    parent = makeWrapper(((XmlAttribute) node).get_OwnerElement(), docWrapper);
                    break;
                default:
                    XmlNode p = node.get_ParentNode();
                    if (p == null) {
                        return null;
                    } else {
                        parent = makeWrapper(p, docWrapper);
                    }
            }
        }
        return parent;
    }

    /**
     * Get the index position of this node among its siblings (starting from 0).
     * In the case of a text node that maps to several adjacent siblings in the DOM,
     * the numbering actually refers to the position of the underlying DOM nodes;
     * thus the sibling position for the text node is that of the first DOM node
     * to which it relates, and the numbering of subsequent XPath nodes is not necessarily
     * consecutive.
     */

    @Override
    public int getSiblingPosition() {
        if (index == -1) {
            switch (nodeKind) {
                case Type.ELEMENT:
                case Type.TEXT:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                    int ix = 0;
                    XmlNode start = node;
                    while (true) {
                        start = start.get_PreviousSibling();
                        if (start == null) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }
                case Type.ATTRIBUTE:
                    ix = 0;
                    AxisIterator iter = parent.iterateAxis(AxisInfo.ATTRIBUTE);
                    while (true) {
                        NodeInfo n = iter.next();
                        if (n == null || Navigator.haveSameName(this, n)) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }

                case Type.NAMESPACE:
                    ix = 0;
                    iter = parent.iterateAxis(AxisInfo.NAMESPACE);
                    while (true) {
                        NodeInfo n = iter.next();
                        if (n == null || Navigator.haveSameName(this, n)) {
                            index = ix;
                            return ix;
                        }
                        ix++;
                    }
                default:
                    index = 0;
                    return index;
            }
        }
        return index;
    }

    @Override
    protected AxisIterator iterateAttributes(Predicate<? super NodeInfo> nodeTest) {
        AxisIterator iter = new AttributeEnumeration(this);
        if (nodeTest != AnyNodeTest.getInstance()) {
            iter = new Navigator.AxisFilter(iter, nodeTest);
        }
        return iter;
    }

    @Override
    protected AxisIterator iterateChildren(Predicate<? super NodeInfo> nodeTest) {
        AxisIterator iter = new ChildEnumeration(docWrapper, this, true, true);
        if (nodeTest != AnyNodeTest.getInstance()) {
            iter = new Navigator.AxisFilter(iter, nodeTest);
        }
        return iter;
    }

    @Override
    protected AxisIterator iterateSiblings(Predicate<? super NodeInfo> nodeTest, boolean forwards) {
        AxisIterator iter = new ChildEnumeration(docWrapper, this, false, forwards);
        if (nodeTest != AnyNodeTest.getInstance()) {
            iter = new Navigator.AxisFilter(iter, nodeTest);
        }
        return iter;
    }

    @Override
    protected AxisIterator iterateDescendants(Predicate<? super NodeInfo> nodeTest, boolean includeSelf) {
        return new SteppingNavigator.DescendantAxisIterator(this, includeSelf, nodeTest);
    }


    @Override
    public NamespaceMap getAllNamespaces() {
        // Note: in a DOM created by the XML parser, all namespaces are present as attribute nodes. But
        // in a DOM created programmatically, this is not necessarily the case. So we need to add
        // namespace bindings for the namespace of the element and any attributes
        if (node.get_NodeType().Value == XmlNodeType.Element) {
            XmlElement elem = (XmlElement) node;
            XmlNamedNodeMap atts = elem.get_Attributes();

            NamespaceMap codes =  NamespaceMap.emptyMap();
            for (int i = 0; i < atts.get_Count(); i++) {
                XmlAttribute att = (XmlAttribute) atts.Item(i);
                String attName = att.get_Name();
                if (attName.equals("xmlns")) {
                    String prefix = "";
                    String uri = att.get_Value();
                    codes = codes.put(prefix, uri);
                } else if (attName.startsWith("xmlns:")) {
                    String prefix = attName.substring(6);
                    String uri = att.get_Value();
                    codes = codes.put(prefix, uri);
                } else if (att.get_NamespaceURI().length() != 0) {
                    codes = codes.put(att.get_Prefix(), att.get_NamespaceURI());
                }
            }

            if (elem.get_NamespaceURI().length() != 0) {
                codes = codes.put(elem.get_Prefix(), elem.get_NamespaceURI());
            }
            /*int count = codes.size();
            NamespaceMap result = new NamespaceBinding[count];

            int p = 0;
            for (NamespaceBinding code : codes) {
                result[p++] = code;
            } */
            return codes;
        } else {
            return null;
        }

    }

    /**
     * Get the string value of a given attribute of this node
     *
     * @param uri   the namespace URI of the attribute name. Supply the empty string for an attribute
     *              that is in no namespace
     * @param local the local part of the attribute name.
     * @return the attribute value if it exists, or null if it does not exist. Always returns null
     *         if this node is not an element.
     * @since 9.4
     */
    @Override
    public String getAttributeValue(/*@NotNull*/ String uri, /*@NotNull*/ String local) {
        NameTest test = new NameTest(Type.ATTRIBUTE, uri, local, getNamePool());
        AxisIterator iterator = iterateAxis(AxisInfo.ATTRIBUTE, test);
        NodeInfo attribute = iterator.next();
        if (attribute == null) {
            return null;
        } else {
            return attribute.getStringValue();
        }
    }

    /**
     * Determine whether the node has any children.
     * <p>Note: the result is equivalent to
     * <code>getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()</code></p>
     */

    @Override
    public boolean hasChildNodes() {
        return node.get_NodeType().Value != XmlNodeType.Attribute &&
                node.get_HasChildNodes();
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer a buffer to contain a string that uniquely identifies this node, across all
     *               documents
     */

    @Override
    public void generateId(FastStringBuffer buffer) {
        Navigator.appendSequentialKey(this, buffer, true);
    }

    /**
     * Copy this node to a given outputter (deep copy)
     */

    @Override
    public void copy(Receiver out, int copyOptions, Location locationId) throws XPathException {
        Receiver r = new NamespaceReducer(out);
        Navigator.copy(this, r, copyOptions, locationId);
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p>For a node other than an element, the method returns null.</p>
     */

    @Override
    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        // Note: in a DOM created by the XML parser, all namespaces are present as attribute nodes. But
        // in a DOM created programmatically, this is not necessarily the case. So we need to add
        // namespace bindings for the namespace of the element and any attributes
        if (node.get_NodeType().Value == XmlNodeType.Element) {
            XmlElement elem = (XmlElement) node;
            XmlNamedNodeMap atts = elem.get_Attributes();
            Set<NamespaceBinding> codes = new HashSet<NamespaceBinding>();
            for (int i = 0; i < atts.get_Count(); i++) {
                XmlAttribute att = (XmlAttribute) atts.Item(i);
                String attName = att.get_Name();
                if (attName.equals("xmlns")) {
                    String prefix = "";
                    String uri = att.get_Value();
                    codes.add(new NamespaceBinding(prefix, uri));
                } else if (attName.startsWith("xmlns:")) {
                    String prefix = attName.substring(6);
                    String uri = att.get_Value();
                    codes.add(new NamespaceBinding(prefix, uri));
                } else if (att.get_NamespaceURI().length() != 0) {
                    codes.add(new NamespaceBinding(att.get_Prefix(), att.get_NamespaceURI()));
                }
            }

            if (elem.get_NamespaceURI().length() != 0) {
                codes.add(new NamespaceBinding(elem.get_Prefix(), elem.get_NamespaceURI()));
            }
            int count = codes.size();
            NamespaceBinding[] result = new NamespaceBinding[count];
            int p = 0;
            for (NamespaceBinding code : codes) {
                result[p++] = code;
            }
            return result;
        } else {
            return null;
        }
    }

    private static boolean isTextNode(XmlNode node) {
        XmlNodeType type = node.get_NodeType();
        int code = type.Value;
        return code == XmlNodeType.Text || code == XmlNodeType.CDATA;
    }

    @Override
    public DotNetNodeWrapper getNextSibling() {
        XmlNode currNode = node;
        for (int i = 0; i < span; i++) {
            currNode = currNode.get_NextSibling();
        }
        if (currNode != null) {
            XmlNodeType type = currNode.get_NodeType();
            if (type.Value == XmlNodeType.DocumentType) {
                currNode = currNode.get_NextSibling();
            } else if (isTextNode(currNode)){
                return spannedWrapper(currNode);
            }
            return makeWrapper(currNode, docWrapper);
        }
        return null;
    }

    private DotNetNodeWrapper spannedWrapper(XmlNode currNode) {
        XmlNode currText = currNode;
        int thisSpan = 1;
        while (true) {
            currText = currText.get_NextSibling();
            if (currText != null && isTextNode(currText)) {
                thisSpan++;
            } else{
                break;
            }
        }
        DotNetNodeWrapper spannedText = makeWrapper(currNode, docWrapper);
        spannedText.span = thisSpan;
        return spannedText;
    }

    public DotNetNodeWrapper getFirstChild() {
        XmlNode currNode = node.get_FirstChild();
        if (currNode != null) {
            if (currNode.get_NodeType().Value == XmlNodeType.DocumentType) {
                currNode = currNode.get_NextSibling();
            }

            if (isTextNode(currNode)){
                return spannedWrapper(currNode);
            }
            return makeWrapper(currNode, docWrapper);
        }
        return null;
    }

    public DotNetNodeWrapper getPreviousSibling() {
        XmlNode currNode = node.get_PreviousSibling();
        if (currNode != null) {
            XmlNodeType type = currNode.get_NodeType();
            if (type.Value == XmlNodeType.DocumentType) {
                return null;
            } else if (isTextNode(currNode)){
                int span = 1;
                while (true) {
                    XmlNode prev = currNode.get_PreviousSibling();
                    if (prev != null && isTextNode(prev)){
                        span++;
                        currNode = prev;
                    } else {
                        break;
                    }
                }
                DotNetNodeWrapper wrapper = makeWrapper(currNode, docWrapper);
                wrapper.span = span;
                return wrapper;
            }
            return makeWrapper(currNode, docWrapper);
        }

        return null;
    }

    @Override
    public DotNetNodeWrapper getSuccessorElement(DotNetNodeWrapper anchor, String uri, String local) {
        XmlNode stop = (anchor == null ? null : anchor).node;
        XmlNode next = node;
        do {
            next = getSuccessorNode(next, stop);
        } while (next != null &&
                !(next.get_NodeType().Value == XmlNodeType.Element &&
                        (uri == null || uri.equals(next.get_NamespaceURI())) &&
                        (local == null || local.equals(next.get_LocalName()))));
        if (next == null) {
            return null;
        } else {
            return makeWrapper(next, docWrapper);
        }
    }

    /**
     * Get the following DOM node in an iteration of a subtree
     *
     * @param start  the start DOM node
     * @param anchor the DOM node marking the root of the subtree within which navigation takes place (may be null)
     * @return the next DOM node in document order after the start node, excluding attributes and namespaces
     */

    private static XmlNode getSuccessorNode(XmlNode start, XmlNode anchor) {
        if (start.get_HasChildNodes()) {
            return start.get_FirstChild();
        }
        if (anchor != null && start == anchor) {
            return null;
        }
        XmlNode p = start;
        while (true) {
            XmlNode s = p.get_NextSibling();
            if (s != null) {
                return s;
            }
            p = p.get_ParentNode();
            if (p == null || (anchor != null && p == anchor)) {
                return null;
            }
        }
    }


    private final class AttributeEnumeration implements AxisIterator, LookaheadIterator {

        private final ArrayList<XmlNode> attList = new ArrayList<>(10);
        private int ix = 0;
        private final DotNetNodeWrapper start;
        private DotNetNodeWrapper current;

        public AttributeEnumeration(DotNetNodeWrapper start) {
            this.start = start;
            XmlNamedNodeMap atts = start.node.get_Attributes();
            if (atts != null) {
                for (int i = 0; i < atts.get_Count(); i++) {
                    String name = atts.Item(i).get_Name();
                    if (!(name.startsWith("xmlns") &&
                            (name.length() == 5 || name.charAt(5) == ':'))) {
                        attList.add(atts.Item(i));
                    }
                }
            }
            ix = 0;
        }

        @Override
        public boolean hasNext() {
            return ix < attList.size();
        }

        @Override
        public NodeInfo next() {
            if (ix >= attList.size()) {
                return null;
            }
            current = makeWrapper(attList.get(ix), docWrapper, start, ix);
            ix++;
            return current;
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link net.sf.saxon.om.SequenceIterator.Property#GROUNDED},
         *         {@link net.sf.saxon.om.SequenceIterator.Property#LAST_POSITION_FINDER},
         *         and {@link net.sf.saxon.om.SequenceIterator.Property#LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        @Override
        public EnumSet<Property> getProperties() {
            return EnumSet.of(Property.LOOKAHEAD);
        }
    }


    /**
     * The class ChildEnumeration handles not only the child axis, but also the
     * following-sibling and preceding-sibling axes. It can also iterate the children
     * of the start node in reverse order, something that is needed to support the
     * preceding and preceding-or-ancestor axes (the latter being used by xsl:number)
     */

    private static class ChildEnumeration  implements AxisIterator, LookaheadIterator {
        private DotNetDocumentWrapper _docTree;
        private DotNetNodeWrapper start;
        private DotNetNodeWrapper commonParent;

        private List<DotNetNodeWrapper> items = new ArrayList<>(20);

        private int ix;
        private int position;
        private boolean downwards;
        private boolean forwards;
        private boolean elementOnly;
        private int currentSpan; // number of DOM nodes mapping to the current XPath node
        private XmlNodeList childNodes;
        private int childNodesLength;

        public ChildEnumeration(DotNetDocumentWrapper docTree, DotNetNodeWrapper start,
                         boolean downwards, boolean forwards)
        {
            this._docTree = docTree;
            this.start = start;
            this.downwards = downwards;
            this.forwards = forwards;
            this.elementOnly = false; // fast path for elements is not used
            position = 0;
            currentSpan = 1;
            if (downwards) {
                commonParent = start;
            } else {
                commonParent = ((DotNetNodeWrapper) start.getParent());
            }

            childNodes = commonParent.node.get_ChildNodes();
            childNodesLength = childNodes.get_Count();
            if (downwards) {
                currentSpan = 1;
                if (forwards) {
                    ix = -1; // just before first
                } else {
                    ix = childNodesLength; // just after last
                }
            } else {
                ix = start.getSiblingPosition(); // at current node
                currentSpan = start.span;
            }


        }

        // Starting with ix positioned at a node, which is the last in a span, calculate the length
        // of the span, that is the number of DOM nodes mapped to this XPath node.
        // @return the number of nodes spanned

        private int skipPrecedingTextNodes () {
            int count = 0;
            while (ix >= count) {
                XmlNode node = childNodes.Item(ix - count);
                if (isTextNode(node)) {
                    count++;
                } else{
                    break;
                }
            }
            return count == 0 ? 1 : count;
        }

        //  Starting with ix positioned at a node, which is the first in a span, calculate the length
        //  of the span, that is the number of DOM nodes mapped to this XPath node.
        //  @return the number of nodes spanned

        private int skipFollowingTextNodes () {
            int count = 0;
            int pos = ix;
            int len = childNodesLength;
            while (pos < len) {
                XmlNode node = childNodes.Item(pos);
                if (isTextNode(node)){
                    pos++;
                    count++;
                } else{
                    break;
                }
            }
            return count == 0 ? 1 : count;
        }

        public boolean supportsHasNext () {
            return true;
        }

        public boolean hasNext () {
            if (forwards) {
                return ix + currentSpan < childNodesLength;
            } else {
                return ix > 0;
            }
        }

        public NodeInfo next () {
            while (true) {
                if (forwards) {
                    ix += currentSpan;
                    if (ix >= childNodesLength) {
                        return null;
                    } else {
                        currentSpan = skipFollowingTextNodes();
                        XmlNode currentDomNode = childNodes.Item(ix);
                        switch (currentDomNode.get_NodeType().Value) {
                            case XmlNodeType.DocumentType:
                                continue;
                            case XmlNodeType.XmlDeclaration:
                                continue;
                            case XmlNodeType.Element:
                                break;
                            default:
                                if (elementOnly) {
                                    continue;
                                } else {
                                    break;
                                }

                        }

                        DotNetNodeWrapper wrapper = makeWrapper(currentDomNode, _docTree, commonParent, ix);
                        wrapper.span = currentSpan;
                        return wrapper;
                    }
                } else {
                    ix--;
                    if (ix < 0) {
                        return null;
                    } else {
                        currentSpan = skipPrecedingTextNodes();
                        ix -= currentSpan - 1;
                        XmlNode currentDomNode = childNodes.Item(ix);
                        switch (currentDomNode.get_NodeType().Value) {
                            case XmlNodeType.DocumentType:
                                continue;
                            case XmlNodeType.Element:
                                break;
                            default:
                                if (elementOnly) {
                                    continue;
                                } else {
                                    break;
                                }
                        }

                        DotNetNodeWrapper wrapper = makeWrapper(currentDomNode, _docTree, commonParent, ix);
                        wrapper.span = currentSpan;
                        return wrapper;
                    }
                }
            }
        }

        public void close () {
        }
    }

//    private final class ChildEnumeration implements AxisIterator, LookaheadIterator {
//
//        private final DotNetNodeWrapper start;
//        private final ArrayList<DotNetNodeWrapper> items = new ArrayList<DotNetNodeWrapper>(20);
//        private int ix;
//        private int position;
//        private boolean downwards;  // iterate children of start node (not siblings)
//        private boolean forwards;   // iterate in document order (not reverse order)
//
//        public ChildEnumeration(DotNetNodeWrapper start,
//                                boolean downwards, boolean forwards) {
//            this.start = start;
//            this.downwards = downwards;
//            this.forwards = forwards;
//            position = 0;
//
//            DotNetNodeWrapper commonParent;
//            if (downwards) {
//                commonParent = start;
//            } else {
//                commonParent = start.getParent();
//            }
//
//            XmlNodeList childNodes = commonParent.node.get_ChildNodes();
//            if (downwards) {
//                if (!forwards) {
//                    // backwards enumeration: go to the end
//                    ix = childNodes.get_Count() - 1;
//                }
//            } else {
//                ix = start.getSiblingPosition() + (forwards ? span : -1);
//            }
//
//            if (forwards) {
//                boolean previousText = false;
//                for (int i = ix; i < childNodes.get_Count(); i++) {
//                    boolean thisText = false;
//                    XmlNode node = childNodes.Item(i);
//                    switch (node.get_NodeType().Value) {
//                        case XmlNodeType.DocumentType:
//                        case XmlNodeType.XmlDeclaration:
//                            break;
//                        case XmlNodeType.EntityReference:
////                            System.err.println("Found an entity reference node:");
////                            System.err.println("Name: " + node.get_Name());
////                            System.err.println("InnerText: " + node.get_InnerText());
////                            System.err.println("Previous: " + node.get_PreviousSibling());
////                            System.err.println("Previous.InnerText: " + (node.get_PreviousSibling()==null ? "null" : node.get_PreviousSibling().get_InnerText()));
////                            System.err.println("Next: " + node.get_NextSibling());
////                            System.err.println("Next.InnerText: " + (node.get_NextSibling()==null ? "null" : node.get_NextSibling().get_InnerText()));
////                            break;
//                        case XmlNodeType.Text:
//                        case XmlNodeType.CDATA:
//                        case XmlNodeType.Whitespace:
//                        case XmlNodeType.SignificantWhitespace:
//                            thisText = true;
//                            if (previousText) {
////                                if (isAtomizing()) {
////                                    UntypedAtomicValue old = (UntypedAtomicValue)(items.get(items.size()-1));
////                                    String newval = old.getStringValue() + getStringValue(node, node.get_NodeType().Value);
////                                    items.set(items.size()-1, new UntypedAtomicValue(newval));
////                                } else {
//                                DotNetNodeWrapper old = items.get(items.size() - 1);
//                                old.span++;
////                                }
//                                break;
//                            }
//                            // otherwise fall through to default case
//                        default:
//                            previousText = thisText;
////                            if (isAtomizing()) {
////                                items.add(new UntypedAtomicValue(
////                                        getStringValue(node, node.get_NodeType().Value)));
////                            } else {
//                            items.add(makeWrapper(node, docWrapper, commonParent, i));
////                            }
//                    }
//                }
//            } else {
//                boolean previousText = false;
//                for (int i = ix; i >= 0; i--) {
//                    boolean thisText = false;
//                    XmlNode node = childNodes.Item(i);
//                    switch (node.get_NodeType().Value) {
//                        case XmlNodeType.DocumentType:
//                        case XmlNodeType.XmlDeclaration:
//                            break;
//                        case XmlNodeType.EntityReference:
////                            System.err.println("Found an entity reference node:");
////                            System.err.println("Name: " + node.get_Name());
////                            System.err.println("InnerText: " + node.get_InnerText());
////                            System.err.println("Previous: " + node.get_PreviousSibling());
////                            System.err.println("Previous.InnerText: " + (node.get_PreviousSibling()==null ? "null" : node.get_PreviousSibling().get_InnerText()));
////                            System.err.println("Next: " + node.get_NextSibling());
////                            System.err.println("Next.InnerText: " + (node.get_NextSibling()==null ? "null" : node.get_NextSibling().get_InnerText()));
////                            break;
//                        case XmlNodeType.Text:
//                        case XmlNodeType.CDATA:
//                        case XmlNodeType.Whitespace:
//                        case XmlNodeType.SignificantWhitespace:
//                            thisText = true;
//                            if (previousText) {
////                                if (isAtomizing()) {
////                                    UntypedAtomicValue old = (UntypedAtomicValue)(items.get(items.size()-1));
////                                    String newval = old.getStringValue() + getStringValue(node, node.get_NodeType().Value);
////                                    items.set(items.size()-1, new UntypedAtomicValue(newval));
////                                } else {
//                                DotNetNodeWrapper old = items.get(items.size() - 1);
//                                old.node = node;
//                                old.span++;
////                                }
//                                break;
//                            }
//                            // otherwise fall through to default case
//                        default:
//                            previousText = thisText;
////                            if (isAtomizing()) {
////                                items.add(new UntypedAtomicValue(
////                                        getStringValue(node, node.get_NodeType().Value)));
////                            } else {
//                            items.add(makeWrapper(node, docWrapper, commonParent, i));
////                            }
//                    }
//                }
//            }
//        }
//
//        @Override
//        public boolean hasNext() {
//            return position < items.size();
//        }
//
//        /*@Nullable*/
//        @Override
//        public NodeInfo next() {
//            if (position > -1 && position < items.size()) {
//                return items.get(position++);
//            } else {
//                position = -1;
//                return null;
//            }
//        }
//
//        /**
//         * Get properties of this iterator, as a bit-significant integer.
//         *
//         * @return the properties of this iterator. This will be some combination of
//         *         properties such as {@link net.sf.saxon.om.SequenceIterator.Property#GROUNDED},
//         *         {@link net.sf.saxon.om.SequenceIterator.Property#LAST_POSITION_FINDER},
//         *         and {@link net.sf.saxon.om.SequenceIterator.Property#LOOKAHEAD}. It is always
//         *         acceptable to return the value zero, indicating that there are no known special properties.
//         *         It is acceptable for the properties of the iterator to change depending on its state.
//         */
//
//        @Override
//        public EnumSet<Property> getProperties() {
//            return EnumSet.of(Property.LOOKAHEAD);
//        }
//
//    } // end of class ChildEnumeration


}

