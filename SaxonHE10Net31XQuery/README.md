# .NET 6/8 command app/dotnet tool to run XQuery 3.1 with IKVM cross-compiled Saxon HE 10.9 from the command line

This is part of the result of my experiments with IKVM and IKVM.NET.Sdk to cross compile the Java version of Saxon 10.9 HE to .NET 6/8.

While using the original sources from the HE product of Saxonica, this is simply my personal, experimental work and the software is in
no way an officially supported, tested or approved product of Saxonica.

As with all projects based on Saxon HE, this work is licensed under MPL 2.

Feel free to use it under this conditions and report back whether you run into mayor problems or could smoothly use it in .NET or .NET 8 for XQuery 3.1 and XPath 3.1.

Understand that this is work in progress and kind of experimental, I don't have access to a complete test suite of unit tests to rigorously test this, 
I nevertheless feel it can be useful for folks to at least know about this option to run XSLT 3.0 and/or XQuery 3.1 and/or XPath 3.1 with .NET 6 or .NET 8,
without depending on the so far commercial only SaxonCS from Saxonica. 