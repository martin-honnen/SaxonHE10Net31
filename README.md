# SaxonHE10Net31

This is a sample solution showing the results of my experiments to cross-compile Saxon 10.8 HE Java to .NET 3.1 Core.

It contains a project SaxonHE10Net31 that pulls the mayority of the Saxon HE 10.8 Java source directly from Maven but adds the glue classes
to have the Java code interact with the .NET API which is in the project SaxonHE10Net31Api.

Then there are three console applications:

- SaxonHE10Net31Samples: my (slight) adaption of the Saxon HE 10 .NET samples
- SaxonHE10Net31Xslt: a console app to run XSLT 3.0 from the command line
- SaxonHE10Net31XQuery: a console app to run XQuery 3.1 from the command line

Please understand that this is the result of my personal experiments with cross-compiling Saxon HE 10.8 Java to .NET 3.1 using the latest version of IKVM and IKVM.NET.Sdk.
While it is based on the original Saxon sources/product from Saxonica, it is in no way an officially tested or approved or supported product from Saxonica, just a demonstration
that using Saxon HE is possible with .NET 3.1 core.

As with all projects based on Saxon HE, this work is licensed under MPL 2.

Feel free to use it under this conditions and report back whether you run into mayor problems or could smoothly use it in .NET core for XSLT 3.0, XQuery 3.1 and XPath 3.1.