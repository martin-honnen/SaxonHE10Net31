////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerator;
import cli.System.Text.RegularExpressions.Group;
import cli.System.Text.RegularExpressions.GroupCollection;
import cli.System.Text.RegularExpressions.Match;
import cli.System.Text.RegularExpressions.Regex;
import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.functions.Count;
import net.sf.saxon.om.Item;
import net.sf.saxon.regex.ARegexIterator;
import net.sf.saxon.regex.RegexIterator;
import net.sf.saxon.regex.UnicodeString;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.z.IntHashMap;
import net.sf.saxon.z.IntToIntHashMap;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Class DotNetRegexIterator - provides an iterator over matched and unmatched substrings.
 * This implementation of RegexIterator uses the .NET regular expression engine.
 */

public class DotNetRegexIterator implements RegexIterator, LastPositionFinder {

    private final String theString;   // the input string being matched
    private final Regex pattern;      // the regex against which the string is matched
    private final IEnumerator matcher;    // the Matcher object that does the matching, and holds the state
    private Match match;        // the current match
    private String current;     // the string most recently returned by the iterator
    private String next;        // if the last string was a matching string, null; otherwise the next substring
    //        matched by the regex
    private int prevEnd = 0;    // the position in the input string of the end of the last match or non-match

    private IntToIntHashMap nestingTable = null;

    /**
     * Construct a RegexIterator. Note that the underlying matcher.find() method is called once
     * to obtain each matching substring. But the iterator also returns non-matching substrings
     * if these appear between the matching substrings.
     *
     * @param string  the string to be analysed
     * @param pattern the regular expression
     */

    public DotNetRegexIterator(String string, Regex pattern) {
        theString = string;
        this.pattern = pattern;
        matcher = pattern.Matches(string).GetEnumerator();
        next = null;
    }

    @Override
    public int getLength() throws XPathException {
        DotNetRegexIterator another = new DotNetRegexIterator(theString, pattern);
        return Count.steppingCount(another);
    }


    /**
     * Get the next item in the sequence
     *
     * @return the next item in the sequence
     */

    @Override
    public StringValue next() {
        if (next == null && prevEnd >= 0) {
            // we've returned a match (or we're at the start), so find the next match
            if (matcher.MoveNext()) {
                match = (Match) matcher.get_Current();
                int start = match.get_Index();
                int end = match.get_Index() + match.get_Length();
                if (prevEnd == start) {
                    // there's no intervening non-matching string to return
                    next = null;
                    current = theString.substring(start, end);
                    prevEnd = end;
                } else {
                    // return the non-matching substring first
                    current = theString.substring(prevEnd, start);
                    next = theString.substring(start, end);
                }
            } else {
                // there are no more regex matches, we must return the final non-matching text if any
                if (prevEnd < theString.length()) {
                    current = theString.substring(prevEnd);
                    next = null;
                } else {
                    // this really is the end...
                    current = null;
                    prevEnd = -1;
                    return null;
                }
                prevEnd = -1;
            }
        } else {
            // we've returned a non-match, so now return the match that follows it, if there is one
            if (prevEnd >= 0) {
                current = next;
                next = null;
                prevEnd = match.get_Index() + match.get_Length();
            } else {
                current = null;
                return null;
            }
        }
        return StringValue.makeStringValue(current);
    }

    /**
     * Get the current item in the sequence
     *
     * @return the item most recently returned by next()
     */

    public Item current() {
        return StringValue.makeStringValue(current);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator.
     */

    @Override
    public EnumSet<Property> getProperties() {
        return EnumSet.of(Property.LAST_POSITION_FINDER);
    }

    /**
     * Determine whether the current item is a matching item or a non-matching item
     *
     * @return true if the current item (the one most recently returned by next()) is
     *         an item that matches the regular expression, or false if it is an item that
     *         does not match
     */

    @Override
    public boolean isMatching() {
        return next == null && prevEnd >= 0;
    }

    /**
     * Get a substring that matches a parenthesised group within the regular expression
     *
     * @param number the number of the group to be obtained
     * @return the substring of the current item that matches the n'th parenthesized group
     *         within the regular expression
     */

    @Override
    public String getRegexGroup(int number) {
        if (!isMatching()) return null;
        GroupCollection groups = match.get_Groups();
        if (number > groups.get_Count() || number < 0) return "";
        String s = groups.get_Item(number).get_Value();
        if (s == null) return "";
        return s;
    }

    /**
     * Get the number of captured groups
     */
    @Override
    public int getNumberOfGroups() {
        return match.get_Groups().get_Count();
    }

    /**
     * Process a matching substring, performing specified actions at the start and end of each captured
     * subgroup. This method will always be called when operating in "push" mode; it writes its
     * result to context.getReceiver(). The matching substring text is all written to the receiver,
     * interspersed with calls to the methods onGroupStart() and onGroupEnd().
     *
     * @param action  defines the processing to be performed at the start and end of a group
     */

    @Override
    public void processMatchingSubstring(MatchHandler action) throws XPathException {
        GroupCollection groups = match.get_Groups();
        int c = groups.get_Count();
        if (c == 0) {
            action.characters(current);
        } else {
            // Create a map from positions in the string to lists of actions.
            // The "actions" in each list are: +N: start group N; -N: end group N.
            IntHashMap<List<Integer>> actions = new IntHashMap<List<Integer>>(c);

            StringValue[] groupArray = new StringValue[c - 1];
            IEnumerator en = groups.GetEnumerator();
            int i = 0;
            // we're not interested in group 0
            en.MoveNext();
            en.get_Current();
            while (en.MoveNext()) {
                i++;
                Group g = (Group) en.get_Current();
                int start = g.get_Index();
                int end = start + g.get_Length();
                if (start < end) {
                    // Add the start action after all other actions on the list for the same position
                    List<Integer> s = actions.get(start);
                    if (s == null) {
                        s = new ArrayList<Integer>(4);
                        actions.put(start, s);
                    }
                    s.add(i);
                    // Add the end action before all other actions on the list for the same position
                    List<Integer> e = actions.get(end);
                    if (e == null) {
                        e = new ArrayList<Integer>(4);
                        actions.put(end, e);
                    }
                    e.add(0, -i);
                } else {
                    // zero-length group (start==end). The problem here is that the information available
                    // from Java isn't sufficient to determine the nesting of groups: match("a", "(a(b?))")
                    // and match("a", "(a)(b?)") will both give the same result for group 2 (start=1, end=1).
                    // So we need to go back to the original regex to determine the group nesting
                    if (nestingTable == null) {
                        nestingTable = ARegexIterator.computeNestingTable(UnicodeString.makeUnicodeString(pattern.toString()));
                    }
                    int parentGroup = nestingTable.get(i);
                    // insert the start and end events immediately before the end event for the parent group,
                    // if present; otherwise after all existing events for this position
                    List<Integer> s = actions.get(start);
                    if (s == null) {
                        s = new ArrayList<Integer>(4);
                        actions.put(start, s);
                        s.add(i);
                        s.add(-i);
                    } else {
                        int pos = s.size();
                        for (int e = 0; e < s.size(); e++) {
                            if (s.get(e) == -parentGroup) {
                                pos = e;
                                break;
                            }
                        }
                        s.add(pos, -i);
                        s.add(pos, i);
                    }

                }


            }
            FastStringBuffer buff = new FastStringBuffer(current.length());
            for (int k = 0; k < current.length() + 1; k++) {
                List<Integer> events = actions.get(k);
                if (events != null) {
                    if (buff.length() > 0) {
                        action.characters(buff);
                        buff.setLength(0);
                    }
                    for (Integer group : events) {
                        if (group > 0) {
                            action.onGroupStart(group);
                        } else {
                            action.onGroupEnd(-group);
                        }
                    }
                }
                if (k < current.length()) {
                    buff.cat(current.charAt(k));
                }
            }
            if (buff.length() > 0) {
                action.characters(buff);
            }
        }

    }

    /**
     * Close the iterator. This indicates to the supplier of the data that the client
     * does not require any more items to be delivered by the iterator. This may enable the
     * supplier to release resources. After calling close(), no further calls on the
     * iterator should be made; if further calls are made, the effect of such calls is undefined.
     * <p>(Currently, closing an iterator is important only when the data is being "pushed" in
     * another thread. Closing the iterator terminates that thread and means that it needs to do
     * no additional work. Indeed, failing to close the iterator may cause the push thread to hang
     * waiting for the buffer to be emptied.)</p>
     *
     * @since 9.1
     */
    @Override
    public void close() {
        //
    }
}

