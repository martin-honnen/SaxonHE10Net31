////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.s9api.AbstractDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.serialize.SerializationProperties;

/**
 * DotNetDomDestination is a Destination that constructs an XmlDocument, the .NET implementation of a DOM
 *  @since 9.9
 */
public class DotNetDomDestination extends AbstractDestination {

    private DotNetDomBuilder builder;


    public DotNetDomDestination(DotNetDomBuilder builder){
        this.builder = builder;
    }

    @Override
    public Receiver getReceiver(PipelineConfiguration pipe, SerializationProperties params) throws SaxonApiException {
        builder.setPipelineConfiguration(pipe);
        return builder;
    }

    @Override
    public void close() throws SaxonApiException {
        // no action
    }
}
