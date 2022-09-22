////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Globalization.CompareInfo;
import cli.System.Globalization.CompareOptions;
import cli.System.Globalization.CultureInfo;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.sort.AlphanumericCollator;
import net.sf.saxon.expr.sort.CaseFirstCollator;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.trans.XPathException;

import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Properties;

/**
 * A DotNetCollationFactory allows a Collation to be created given
 * a set of properties that the collation should have. This class uses the services
 * of the .NET platform; there is a corresponding class that uses the Java collation facilities.
 */

public abstract class DotNetCollationFactory {

    /**
     * The class is a never instantiated
     */
    private DotNetCollationFactory() {
    }

    /**
     * Make a collator with given properties
     *
     * @param config the Configuration
     * @param uri the collation URI
     * @param props  the desired properties of the collation  @return a collation with these properties
     * @throws net.sf.saxon.trans.XPathException
     *          if it is not possible to create a collation with the
     *          requested properties
     */

    /*@Nullable*/
    public static StringCollator makeCollation(Configuration config, String uri, Properties props) throws XPathException {

        StringCollator stringCollator = null;

        // If a specific collation class is requested, this overrides everything else. Note that
        // the class is loaded as a Java class, not as a .NET class.

        String classAtt = props.getProperty("class");
        if (classAtt != null) {
            java.lang.Object comparator = config.getInstance(classAtt, null);
            if (comparator instanceof Comparator) {
                stringCollator = new SimpleCollation(uri, (Comparator) comparator);
            } else if (comparator instanceof StringCollator) {
                return (StringCollator) comparator;
            } else {
                throw new XPathException("Requested collation class " + classAtt + " is not a Comparator");
            }
        }

        // If rules are specified, create a RuleBasedCollator. In this case we use the Java platform facilities
        // available from the OpenJDK Classpath library.

        String rulesAtt = props.getProperty("rules");
        if (rulesAtt != null && stringCollator == null) {
            try {
                stringCollator = new SimpleCollation(uri, new RuleBasedCollator(rulesAtt));
            } catch (ParseException e) {
                throw new XPathException("Invalid collation rules: " + e.getMessage());
            }
        }

        // Start with the lang attribute

        CompareInfo info;
        int options = 0;

        String langAtt = props.getProperty("lang");
        if (langAtt != null) {
            info = CultureInfo.CreateSpecificCulture(langAtt).get_CompareInfo();
        } else {
            info = CultureInfo.get_CurrentCulture().get_CompareInfo();  // use default locale
        }

        // See if there is a strength attribute
        String strengthAtt = props.getProperty("strength");
        if (strengthAtt != null && options == 0) {
            if (strengthAtt.equals("primary")) {
                options = CompareOptions.IgnoreCase | CompareOptions.IgnoreNonSpace | CompareOptions.IgnoreWidth;
            } else if (strengthAtt.equals("secondary")) {
                options = CompareOptions.IgnoreCase | CompareOptions.IgnoreWidth;
            } else if (strengthAtt.equals("tertiary")) {
                options = CompareOptions.IgnoreWidth;
            } else if (strengthAtt.equals("identical")) {
                options = 0;
            } else {
                throw new XPathException("strength must be primary, secondary, tertiary, or identical");
            }
        }

        // Look for the properties ignore-case, ignore-modifiers, ignore-width

        String ignore = props.getProperty("ignore-case");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreCase;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new XPathException("ignore-case must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-modifiers");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreNonSpace;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new XPathException("ignore-modifiers must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-symbols");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreSymbols;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new XPathException("ignore-symbols must be yes or no");
            }
        }

        ignore = props.getProperty("ignore-width");
        if (ignore != null) {
            if (ignore.equals("yes")) {
                options |= CompareOptions.IgnoreWidth;
            } else if (ignore.equals("no")) {
                // no-op
            } else {
                throw new XPathException("ignore-width must be yes or no");
            }
        }

        if (stringCollator == null) {
            stringCollator = new DotNetComparator(uri, info, CompareOptions.wrap(options));
        }

        // See if there is a case-order property
        String caseOrder = props.getProperty("case-order");
        if (caseOrder != null && !"#default".equals(caseOrder)) {
            // force the base collation to ignore case
            options |= CompareOptions.IgnoreCase;
            stringCollator = new DotNetComparator(uri, info, CompareOptions.wrap(options));
            stringCollator = CaseFirstCollator.makeCaseOrderedCollator(uri, stringCollator, caseOrder);
        }

        // See if there is an alphanumeric property
        String alphanumeric = props.getProperty("alphanumeric");
        if (alphanumeric != null && !"no".equals(alphanumeric)) {
            if (alphanumeric.equals("yes")) {
                stringCollator = new AlphanumericCollator(stringCollator);
            } else if (alphanumeric.equals("codepoint")) {
                stringCollator = new AlphanumericCollator(CodepointCollator.getInstance());
            } else {
                throw new XPathException("alphanumeric must be yes, no, or codepoint");
            }
        }

        return stringCollator;
    }

}

