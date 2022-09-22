////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.ObjectValue;


/**
 * An XPath value that encapsulates a .NET object. Such a value can only be obtained by
 * calling an extension function that returns it.
 */

public class DotNetObjectValue extends ObjectValue {

    public DotNetObjectValue(Object value) {
        super(value);
    }

    /**
     * Determine the data type of the expression
     *
     * @param th the type hierarchy cache
     * @return Type.OBJECT
     */

    /*@NotNull*/
    @Override
    public ItemType getItemType(/*@Nullable*/ TypeHierarchy th) {
        if (th == null) {
            return AnyItemType.getInstance();
        }
        return new DotNetExternalObjectType(((cli.System.Object) getObject()).GetType(), th.getConfiguration());
    }

}

