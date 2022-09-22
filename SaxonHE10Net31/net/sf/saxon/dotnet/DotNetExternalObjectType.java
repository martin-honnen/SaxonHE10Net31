////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Whitespace;

/**
 * This class represents the type of an external Java object returned by
 * an extension function, or supplied as an external variable/parameter.
 */

public class DotNetExternalObjectType extends ExternalObjectType {

    private final transient cli.System.Type dotNetType;
    private final Configuration config;
    private final StructuredQName qName;

    public DotNetExternalObjectType(cli.System.Type dotNetType, Configuration config) {
        this.dotNetType = dotNetType;
        this.config = config;
        final String localName = dotNetType.get_Name().replace('$', '_');
        qName = new StructuredQName("", NamespaceConstant.DOT_NET_TYPE, localName);
    }

    /**
     * Get the corresponding {@link net.sf.saxon.type.UType}. A UType is a union of primitive item
     * types.
     *
     * @return the smallest UType that subsumes this item type
     */
    @Override
    public UType getUType() {
        return UType.EXTENSION;
    }

    /**
     * Get the local name of this type
     *
     * @return the local name of this type definition, if it has one. Return null in the case of an
     *         anonymous type.
     */

    @Override
    public String getName() {
        return qName.getLocalPart();
    }

    /**
     * Get the target namespace of this type
     *
     * @return the target namespace of this type definition. For .net this is always
     * {@link NamespaceConstant#DOT_NET_TYPE}.
     */

    @Override
    public String getTargetNamespace() {
        return NamespaceConstant.DOT_NET_TYPE;
    }

    /**
     * Get the name of this type as an EQName, that is, a string in the format Q{uri}local.
     *
     * @return an EQName identifying the type. In the case of an anonymous type, an internally-generated
     *         name is returned
     */
    public String getEQName() {
        return "Q{" + getTargetNamespace() + "}" + getName();
    }

    /**
     * Determine whether this is a built-in type or a user-defined type
     *
     * @return false, this type is not built-in
     */

    public boolean isBuiltInType() {
        return false;
    }

    /**
     * Get the name of this type as a StructuredQName, unless the type is anonymous, in which case
     * return null
     *
     * @return the name of the atomic type, or null if the type is anonymous.
     */

    @Override
    public StructuredQName getTypeName() {
        return null;
    }

    /**
     * Get the redefinition level. This is zero for a component that has not been redefined;
     * for a redefinition of a level-0 component, it is 1; for a redefinition of a level-N
     * component, it is N+1. This concept is used to support the notion of "pervasive" redefinition:
     * if a component is redefined at several levels, the top level wins, but it is an error to have
     * two versions of the component at the same redefinition level.
     *
     * @return the redefinition level
     */

    public int getRedefinitionLevel() {
        return 0;
    }

    /**
     * Determine whether the type is abstract, that is, whether it cannot have instances that are not also
     * instances of some concrete subtype
     *
     * @return false, this type is not abstract
     */

    public boolean isAbstract() {
        return false;
    }




    /**
     * Get the URI of the schema document where the type was originally defined.
     *
     * @return the URI of the schema document. Returns null if the information is unknown or if this
     *         is a built-in type
     */

    /*@Nullable*/
    public String getSystemId() {
        return null;  //AUTO
    }



    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link net.sf.saxon.type.SchemaType#DERIVATION_LIST} and {@link net.sf.saxon.type.SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public final int getBlock() {
        return 0;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link net.sf.saxon.type.SchemaType#DERIVATION_RESTRICTION}
     */

    public final int getDerivationMethod() {
        return SchemaType.DERIVATION_RESTRICTION;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link net.sf.saxon.type.SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public final boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Get the namecode of the name of this type. This includes the prefix from the original
     * type declaration: in the case of built-in types, there may be a conventional prefix
     * or there may be no prefix.
     */

    public int getNameCode() {
        return config.getNamePool().allocateFingerprint(getTargetNamespace(), getName());
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public final boolean isComplexType() {
        return false;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     */

    public final SchemaType getBaseType() {
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    /*@NotNull*/
    @Override
    public ItemType getPrimitiveItemType() {
        return this;
    }

    /**
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    @Override
    public int getPrimitiveType() {
        return StandardNames.XS_ANY_ATOMIC_TYPE;
    }

    public String toString() {
        String name = dotNetType.ToString();
        name = name.replace('$', '-');
        return "type:" + name;
    }



    /**
     * Get the types of derivation that are not permitted, by virtue of the "final" property.
     *
     * @return the types of derivation that are not permitted, as a bit-significant integer
     *         containing bits such as {@link net.sf.saxon.type.SchemaType#DERIVATION_EXTENSION}
     */
    public int getFinalProhibitions() {
        return 0;
    }



    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    public SchemaType getKnownBaseType() {
        return getBaseType();
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return other.getFingerprint() == getFingerprint();
    }

    /**
     * Get the relationship of this external object type to another external object type
     *
     * @param other the other external object type
     * @return the type relationship, for example {@link Affinity#SUBSUMES}
     */

    public Affinity getRelationship(DotNetExternalObjectType other) {
        cli.System.Type j2 = other.dotNetType;
        if (dotNetType.equals(j2)) {
            return Affinity.SAME_TYPE;
        } else if (dotNetType.IsAssignableFrom(j2)) {
            return Affinity.SUBSUMES;
        } else if (j2.IsAssignableFrom(dotNetType)) {
            return Affinity.SUBSUMED_BY;
        } else if (dotNetType.get_IsInterface() || j2.get_IsInterface()) {
            return Affinity.OVERLAPS; // there may be an overlap, we play safe
        } else {
            return Affinity.DISJOINT;
        }
    }

    public String getDescription() {
        return getDisplayName();
    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) {
        // no action;
    }

    /**
     * Returns true if this SchemaType is a SimpleType
     *
     * @return true (always)
     */

    public final boolean isSimpleType() {
        return true;
    }

    /**
     * Test whether this Simple Type is an atomic type
     *
     * @return true, this is considered to be an atomic type
     */

    @Override
    public boolean isAtomicType() {
        return true;
    }

    /**
     * Ask whether this type is an ID type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:ID: that is, it includes types derived
     * from ID by restriction, list, or union. Note that for a node to be treated
     * as an ID, its typed value must be a *single* atomic value of type ID; the type of the
     * node, however, can still allow a list.
     */

    public boolean isIdType() {
        return false;
    }

    /**
     * Ask whether this type is an IDREF or IDREFS type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:IDREF: that is, it includes types derived
     * from IDREF or IDREFS by restriction, list, or union
     */

    public boolean isIdRefType() {
        return false;
    }

    /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     *
     * @return true if this is a list type
     */

    public boolean isListType() {
        return false;
    }

    /**
     * Return true if this type is a union type (that is, if its variety is union)
     *
     * @return true for a union type
     */

    public boolean isUnionType() {
        return false;
    }

    /**
     * Determine the whitespace normalization required for values of this type
     *
     * @return one of PRESERVE, REPLACE, COLLAPSE
     */

    public int getWhitespaceAction() {
        return Whitespace.PRESERVE;
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     *
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public CharSequence applyWhitespaceNormalization(CharSequence value) {
        return value;
    }



    /**
     * Test whether this simple type is namespace-sensitive, that is, whether
     * it is derived from xs:QName or xs:NOTATION
     *
     * @return true if this type is derived from xs:QName or xs:NOTATION
     */

    public boolean isNamespaceSensitive() {
        return false;
    }

    /**
     * Test whether this is an anonymous type
     *
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
        return false;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type.
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    public AtomicSequence atomize(NodeInfo node) throws XPathException {
        throw new IllegalStateException("The type annotation of a node cannot be an external object type");
    }

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     *
     * @param value    the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     *                 in the content of values. Can supply null, in which case any namespace-sensitive content
     *                 will be rejected.
     * @param rules    the conversion rules for the configuration
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    public AtomicSequence getTypedValue(CharSequence value, NamespaceResolver resolver, ConversionRules rules)
            throws ValidationException {
        throw new ValidationFailure("Cannot validate a string against an external object type").makeException();
    }


    /**
     * Validate that a primitive atomic value is a valid instance of a type derived from the
     * same primitive type.
     *
     * @param primValue    the value in the value space of the primitive type.
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
     *                     is used. This value is checked against the pattern facet (if any)
     * @param rules        the conversion rules for the configuration
     * @return null if the value is valid; otherwise, a ValidationFailure object indicating
     *         the nature of the error.
     * @throws UnsupportedOperationException in the case of an external object type
     */

    public ValidationFailure validate(AtomicValue primValue, CharSequence lexicalValue, ConversionRules rules) {
        throw new UnsupportedOperationException("validate");
    }


    /**
     * Get the .NET type to which this external XPath type corresponds
     *
     * @return the corresponding .NET type
     */

    public cli.System.Type getDotNetType() {
        return dotNetType;
    }

    /**
     * Test whether a given item conforms to this type
     *
     *
     *
     * @param item    The item to be tested
     * @param th      The type hierarchy cache
     * @return true if the item is an instance of this type; false otherwise
     */
    @Override
    public boolean matches(Item item, TypeHierarchy th) {
        if (item instanceof DotNetObjectValue) {
            cli.System.Object obj = (cli.System.Object) ((DotNetObjectValue) item).getObject();
            return dotNetType.IsAssignableFrom(obj.GetType());
        }
        return false;
    }

    /**
     * Get a StringConverter, an object which converts strings in the lexical space of this
     * data type to instances (in the value space) of the data type.
     *
     * @return a StringConverter to do the conversion, or null if no built-in converter is available.
     */
    public StringConverter getStringConverter(ConversionRules rules) {
        return null;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     *
     * @param value      the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     *                   is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     *                   content will throw an UnsupportedOperationException.
     * @param rules      the conversion rules for the configuration
     * @return null if validation succeeds; return a ValidationException describing the validation failure
     *         if validation fails, unless throwException is true, in which case the exception is thrown rather than
     *         being returned.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     *                                       resolver is supplied
     */

    public ValidationFailure validateContent(/*@NotNull*/ CharSequence value, NamespaceResolver nsResolver, /*@NotNull*/ ConversionRules rules) {
        throw new UnsupportedOperationException("Cannot use an external object type for validation");
    }

    public int getFingerprint() {
        return getNameCode() & NamePool.FP_MASK;
    }

    public String getDisplayName() {
        return toString();
    }

    /**
     * Apply any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess
     *
     * @param input the value to be preprocessed
     * @return the value after preprocessing
     */

    public CharSequence preprocess(CharSequence input) {
        return input;
    }

    /**
     * Reverse any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess. This is called when converting a value of this type to
     * a string
     *
     * @param input the value to be postprocessed: this is the "ordinary" result of converting
     *              the value to a string
     * @return the value after postprocessing
     */

    public CharSequence postprocess(CharSequence input) {
        return input;
    }


    @Override
    public double getDefaultPriority() {
        return 0.5;
    }

}

