////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerator;
import cli.System.Text.RegularExpressions.Match;
import cli.System.Text.RegularExpressions.Regex;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.value.StringValue;

/**
 * A DotNetTokenIterator is an iterator over the strings that result from tokenizing
 * a string using a regular expression
 */

public class DotNetTokenIterator implements AtomicIterator {

    private final String input;
    private final Regex pattern;
    private final IEnumerator matches;
    private int prevEnd = 0;


    /**
     * Construct a DotNetTokenIterator.
     */

    public DotNetTokenIterator(CharSequence input, Regex pattern) {
        this.input = input.toString();
        this.pattern = pattern;
        matches = pattern.Matches(this.input).GetEnumerator();
        prevEnd = 0;
    }

    @Override
    public StringValue next() {
        if (prevEnd < 0) {
            return null;
        }

        CharSequence current;
        if (matches.MoveNext()) {
            Match match = (Match) matches.get_Current();
            current = input.subSequence(prevEnd, match.get_Index());
            prevEnd = match.get_Index() + match.get_Length();
        } else {
            current = input.subSequence(prevEnd, input.length());
            prevEnd = -1;
        }
        return StringValue.makeStringValue(current);
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
        // no-op
    }
}

