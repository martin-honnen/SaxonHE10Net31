////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.ArgumentException;
import cli.System.Text.RegularExpressions.Match;
import cli.System.Text.RegularExpressions.Regex;
import cli.System.Text.RegularExpressions.RegexOptions;
import net.sf.saxon.regex.RegexIterator;
import net.sf.saxon.regex.RegularExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.tree.util.FastStringBuffer;

import java.util.function.Function;

/**
 * A compiled regular expression implemented using the .NET regex package
 */
public class DotNetRegularExpression implements RegularExpression {

    Regex pattern;
    int groupCount;

    /**
     * Create (compile) a regular expression
     *
     * @param regex the source text of the regular expression, in native .NET syntax
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws net.sf.saxon.trans.XPathException
     *          if the syntax of the regular expression or flags is incorrect
     */

    public DotNetRegularExpression(CharSequence regex, CharSequence flags)
            throws XPathException {
        String translated = "";
        try {
            pattern = new Regex(regex.toString(), setFlags(flags));
            groupCount = pattern.GetGroupNumbers().length;
            if (false) {
                // to keep the compiler happy
                throw new ArgumentException();
            }
        } catch (ArgumentException e) {
            throw new XPathException("Error in translated regular expression. Input regex = " +
                    FastStringBuffer.diagnosticPrint(regex) + ". Translated regex = " +
                    FastStringBuffer.diagnosticPrint(translated) + ". Message = " + e.getMessage());
        }
    }

    /**
     * Use this regular expression to analyze an input string, in support of the XSLT
     * analyze-string instruction. The resulting RegexIterator provides both the matching and
     * non-matching substrings, and allows them to be distinguished. It also provides access
     * to matched subgroups.
     */

    @Override
    public RegexIterator analyze(CharSequence input) {
        return new DotNetRegexIterator(input.toString(), pattern);
    }

    /**
     * Determine whether the regular expression contains a match of a given string
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    @Override
    public boolean containsMatch(CharSequence input) {
        return pattern.IsMatch(input.toString());
    }

    /**
     * Determine whether the regular expression matches a given string
     *
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    @Override
    public boolean matches(CharSequence input) {
        Match m = pattern.Match(input.toString());
        return (m.get_Success() && m.get_Length() == input.length());
    }

    /**
     * Replace all substrings of a supplied input string that match the regular expression
     * with a replacement string.
     *
     * @param input       the input string on which replacements are to be performed
     * @param replacement the replacement string in the format of the XPath replace() function
     * @return the result of performing the replacement
     * @throws net.sf.saxon.trans.XPathException
     *          if the replacement string is invalid
     */

    @Override
    public CharSequence replace(CharSequence input, CharSequence replacement) throws XPathException {
        // preprocess the replacement string: .NET uses $$ to represent $, and doesn't treat \ specially
        // The calling code will already have validated the replacement string, so we can assume for example
        // that "\" will be followed by "\" or "$".
        FastStringBuffer sb = new FastStringBuffer(replacement.length() + 4);
        for (int i = 0; i < replacement.length(); i++) {
            final char ch = replacement.charAt(i);
            if (ch == '\\') {
                if (replacement.charAt(i + 1) == '\\') {
                    sb.cat('\\');
                } else if (replacement.charAt(i + 1) == '$') {
                    sb.append("$$");
                } else {
                    throw new IllegalArgumentException("bad replacement string");
                }
                i++;
            } else if (ch == '$') {
                int n = 0;
                while (true) {
                    if (i + 1 >= replacement.length()) {
                        break;
                    }
                    char d = replacement.charAt(i + 1);
                    int dval = "0123456789".indexOf(d);
                    if (dval < 0) {
                        break;
                    }
                    i++;
                    n = n * 10 + dval;
                }
                processGroupReference(n, sb);
            } else {
                sb.cat(ch);
            }
        }
        //System.err.println("original replacement string: " + replacement);
        //System.err.println("processed replacement string: " + sb);
        return pattern.Replace(input.toString(), sb.toString());
    }

    /**
     * Replace all substrings of a supplied input string that match the regular expression
     * with a replacement string.
     *
     * @param input       the input string on which replacements are to be performed
     * @param replacement a function that is called once for each matching substring, and
     *                    that returns a replacement for that substring
     * @return the result of performing the replacement
     * @throws XPathException if the replacement string is invalid
     */
    @Override
    public CharSequence replaceWith(CharSequence input, Function<CharSequence, CharSequence> replacement) throws XPathException {
        throw new XPathException("saxon:replace-with() is not supported with the .NET regex engine");
    }


    /**
     * Translate a group reference in the replacement string from XPath notation into .NET notation
     * This closely follows the algorithm in F+O section 7.6.3 fn:replace
     *
     * @param n  the consecutive sequence of digits following a "$" sign
     * @param sb teh buffer to contain the replacement string in .NET notation
     */

    private void processGroupReference(int n, FastStringBuffer sb) {
        if (n == 0) {
            sb.append("$0");
        } else if (n <= groupCount) {
            sb.append("${" + n + '}');
        } else if (n <= 9) {
            // no-op - group reference is replaced by zero-length string
        } else {
            // try replacing $67 by ${6}7
            int n0 = n / 10;
            int n1 = n % 10;
            processGroupReference(n0, sb);
            sb.append("" + n1);
        }
    }

    /**
     * Use this regular expression to tokenize an input string.
     *
     * @param input the string to be tokenized
     * @return a SequenceIterator containing the resulting tokens, as objects of type StringValue
     */

    @Override
    public AtomicIterator tokenize(CharSequence input) {
        return new DotNetTokenIterator(input, pattern);
    }

    /**
     * Set the Java flags from the supplied XPath flags.
     *
     * @param inFlags the flags as a string, e.g. "im"
     * @return the flags as a RegexOptions FlagsAttribute
     * @throws XPathException if the supplied value is invalid
     */

    public static RegexOptions setFlags(CharSequence inFlags) throws XPathException {
        int flags = 0;
        for (int i = 0; i < inFlags.length(); i++) {
            char c = inFlags.charAt(i);
            switch (c) {
                case 'm':
                    flags |= RegexOptions.Multiline;
                    break;
                case 'i':
                    flags |= RegexOptions.IgnoreCase;
                    break;
                case 's':
                    flags |= RegexOptions.Singleline;
                    break;
                case 'x':
                    flags |= RegexOptions.IgnorePatternWhitespace;
                    break;
                default:
                    XPathException err = new XPathException("Invalid character '" + c + "' in regular expression flags");
                    err.setErrorCode("FORX0001");
                    throw err;
            }
        }
        return RegexOptions.wrap(flags);
    }

    /**
     * Get the flags used at the time the regular expression was compiled.
     *
     * @return a string containing the flags
     */
    @Override
    public String getFlags() {
        String flags = "";
        RegexOptions options = pattern.get_Options();
        if ((options.Value & RegexOptions.IgnoreCase) != 0 ) {
            flags += "i";
        }
        if ((options.Value & RegexOptions.Multiline) != 0 ) {
            flags += "m";
        }
        if ((options.Value & RegexOptions.Singleline) != 0 ) {
            flags += "s";
        }
        if ((options.Value & RegexOptions.IgnorePatternWhitespace) != 0 ) {
            flags += "x";
        }
        return flags;
    }

    /**
     * Test whether the 'x' flag is set.
     *
     * @param inFlags the flags as a string, e.g. "im"
     * @return true if the 'x' flag is set
     */

    public static boolean isIgnoreWhitespace(CharSequence inFlags) {
        for (int i = 0; i < inFlags.length(); i++) {
            if (inFlags.charAt(i) == 'x') {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether the 'i' flag is set.
     *
     * @param inFlags the flags as a string, e.g. "im"
     * @return true if the 'i' flag is set
     */

    public static boolean isCaseBlind(CharSequence inFlags) {
        for (int i = 0; i < inFlags.length(); i++) {
            if (inFlags.charAt(i) == 'i') {
                return true;
            }
        }
        return false;
    }


}

