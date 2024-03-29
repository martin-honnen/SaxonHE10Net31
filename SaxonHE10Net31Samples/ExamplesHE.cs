﻿using System;
using System.IO;
using System.Collections;
using System.Xml;
using System.Net;
using Saxon.Api;
using System.Collections.Generic;

namespace SaxonHE
{
    class ExamplesHE
    {
        /// <summary>
        /// Run Saxon XSLT and XQuery sample applications in Saxon Home Edition on .NET
        /// </summary>
        /// <param name="argv">
        /// <para>Options:</para>
        /// <list>
        /// <item>-test:testname  run a specific test</item>
        /// <item>-dir:samplesdir directory containing the sample data files (default samples)</item>
        /// <item>-ask:yes|no     indicates whether to prompt for confirmation after each test (default yes)</item>
        /// </list>
        /// </param>


        public static void Main(String[] argv)
        {

            Example[] examples = {
                new XPathSimple(),
                new XPathSimple2(),
                new XPathVariables(),
                new XPathUndeclaredVariables(),
                new XPathWithStaticError(),
                new XPathWithDynamicError(),
                new XsltSimple1(),
                new XsltSimple2(),
                new XsltSimple3(),
                new XsltStripSpace(),
                new XsltReuseExecutable(),
                new XsltReuseTransformer(),
                new XsltFilterChain(),
                new XsltDomToDom(),
                new XsltXdmToXdm(),
                new XsltXdmElementToXdm(),
                new XsltUsingSourceResolver(),
                new XsltSettingOutputProperties(),
                new XsltDisplayingErrors(),
                new XsltCapturingErrors(),
                new XsltCapturingMessages(),
                new XsltProcessingInstruction(),
                new XsltMultipleOutput(),
                new XsltUsingResultHandler(),
                new XsltUsingIdFunction(),
                new XsltUsingRegisteredCollection(),
                new XsltUsingDirectoryCollection(),
                new XsltIntegratedExtension(),
                new XsltSimpleExtension(),
                new XQueryToStream(),
                new XQueryToAtomicValue(),
                new XQueryToSequence(),
                new XQueryToDom(),
                new XQueryToXdm(),
                new XQueryCallFunction(),
                new XQueryFromXmlReader(),
                new XQueryToSerializedSequence(),
                new XQueryUsingParameter(),
                new XQueryMultiModule()
            };

            Boolean ask = true;
            String test = "all";

            String samplesPath = null;
            Uri samplesDir;

            foreach (String s in argv)
            {
                if (s.StartsWith("-test:"))
                {
                    test = s.Substring(6);
                }
                else if (s.StartsWith("-dir:"))
                {
                    samplesPath = s.Substring(5);
                }
                else if (s == "-ask:yes")
                {
                    // no action
                }
                else if (s == "-ask:no")
                {
                    ask = false;
                }
                else if (s == "-?")
                {
                    Console.WriteLine("ExamplesHE -dir:samples -test:testname -ask:yes|no");
                }
                else
                {
                    Console.WriteLine("Unrecognized Argument: " + s);
                    return;
                }
            }
            if (samplesPath != null)
            {
                if (samplesPath.StartsWith("file:///"))
                {
                    samplesPath = samplesPath.Substring(8);
                }
                else if (samplesPath.StartsWith("file:/"))
                {
                    samplesPath = samplesPath.Substring(6);
                }

            }
            else
            {
                String home = Environment.CurrentDirectory;//Environment.GetEnvironmentVariable("SAXON_HOME");
                if (home == null)
                {
                    Console.WriteLine("No input directory supplied, and SAXON_HOME is not set");
                    return;
                }
                else
                {
                    if (!(home.EndsWith("/") || home.EndsWith("\\")))
                    {
                        home = home + "/";
                    }
                    samplesPath = Path.Combine(home, "samples");
                }
            }

            if (!(samplesPath.EndsWith("/") || samplesPath.EndsWith("\\")))
            {
                samplesPath = samplesPath + Path.DirectorySeparatorChar;
            }

            if (!File.Exists(Path.Combine(samplesPath, "data/books.xml")))
            {
                Console.WriteLine("Supplied samples directory " + samplesPath + " does not contain the Saxon sample data files");
                return;
            }

            try
            {
                samplesDir = new Uri(samplesPath);
            }
            catch
            {
                Console.WriteLine("Invalid URI for samples directory: " + samplesPath);
                return;
            }

            Boolean found = false;

            Console.WriteLine($"Examples run with .NET {Environment.Version} using {new Processor().ProductTitle}");

            foreach (Example ex in examples)
            {
                if (test == "all" || test == ex.testName)
                {
                    Console.WriteLine("\n\n===== " + ex.testName + " =======\n");
                    found = true;
                    try
                    {
                        ex.run(samplesDir);
                    }
                    catch (Saxon.Api.StaticError se)
                    {
                        Console.WriteLine("Test failed with static error " + (se.ErrorCode != null ? se.ErrorCode.LocalName : "") + ": " + se.Message);
                    }
                    catch (Saxon.Api.DynamicError de)
                    {
                        Console.WriteLine("Test failed with dynamic error " + (de.ErrorCode != null ? de.ErrorCode.LocalName : "") + ": " + de.Message);
                    }
                    catch (Exception exc)
                    {
                        Console.WriteLine("Test failed unexpectedly (" + exc.GetType() + "): " + exc.Message);
                        Console.WriteLine(exc.StackTrace);
                    }
                    if (ask)
                    {
                        Console.WriteLine("\n\nContinue? - type (Y(es)/N(o)/A(ll))");
                        String answer = Console.ReadLine();
                        if (answer == "N" || answer == "n")
                        {
                            break;
                        }
                        else if (answer == "A" || answer == "a")
                        {
                            ask = false;
                        }
                    }
                }
            }
            if (!found)
            {
                Console.WriteLine("Please supply a valid test name, or 'all' ('" + test + "' is invalid)");
            }
            Console.WriteLine("\n==== done! ====");
        }
    }

    ///<summary>
    /// Each of the example programs is implemented as a subclass of the abstract class Example
    ///</summary> 


    public abstract class Example
    {
        /// <summary>
        /// Read-only property: the name of the test example
        /// </summary>
        public abstract String testName { get; }
        /// <summary>
        /// Entry point for running the example
        /// </summary>
        public abstract void run(Uri samplesDir);
    }

    /// <summary>
    /// Evaluate an XPath expression selecting from a source document supplied as a URI
    /// </summary>

    public class XPathSimple : Example
    {

        public override String testName
        {
            get { return "XPathSimple"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create an XPath compiler
            XPathCompiler xpath = processor.NewXPathCompiler();

            // Enable caching, so each expression is only compiled once
            xpath.Caching = true;

            // Compile and evaluate some XPath expressions
            foreach (XdmItem item in xpath.Evaluate("//ITEM", input))
            {
                Console.WriteLine("TITLE: " + xpath.EvaluateSingle("string(TITLE)", item));
                Console.WriteLine("PRICE: " + xpath.EvaluateSingle("string(PRICE)", item));
            }
        }
    }

    /// <summary>
    /// Evaluate an XPath expression against a source document, returning its effective boolean value
    /// </summary>

    public class XPathSimple2 : Example
    {

        public override String testName
        {
            get { return "XPathSimple2"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create an XPath compiler
            XPathCompiler xpath = processor.NewXPathCompiler();

            // Enable caching, so each expression is only compiled once
            xpath.Caching = true;
            
            // Compile and evaluate an XPath expression
            XPathSelector selector = xpath.Compile("//ITEM").Load();
            selector.ContextItem = input;
            Console.WriteLine(selector.EffectiveBooleanValue());

        }
    }

    /// <summary>
    /// Evaluate an XPath expression using variables (and no source document)
    /// </summary>

    public class XPathVariables : Example
    {

        public override String testName
        {
            get { return "XPathVariables"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.DeclareVariable(new QName("", "a"));
            compiler.DeclareVariable(new QName("", "b"));
            XPathSelector selector = compiler.Compile("$a + $b").Load();

            // Set the values of the variables
            selector.SetVariable(new QName("", "a"), new XdmAtomicValue(2));
            selector.SetVariable(new QName("", "b"), new XdmAtomicValue(3));

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// Evaluate an XPath expression using variables without explicit declaration
    /// </summary>

    public class XPathUndeclaredVariables : Example
    {

        public override String testName
        {
            get { return "XPathUndeclaredVariables"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.AllowUndeclaredVariables = true;
            XPathExecutable expression = compiler.Compile("$a + $b");
            XPathSelector selector = expression.Load();

            // Set the values of the variables
            IEnumerator<QName> vars = expression.EnumerateExternalVariables2();
            while (vars.MoveNext())
            {
                selector.SetVariable((QName)vars.Current, new XdmAtomicValue(10));
            }

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// Evaluate an XPath expression throwing a static error
    /// </summary>

    public class XPathWithStaticError : Example
    {

        public override String testName
        {
            get { return "XPathWithStaticError"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.AllowUndeclaredVariables = true;
            XPathExecutable expression = compiler.Compile("1 + unknown()");
            XPathSelector selector = expression.Load();

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// Evaluate an XPath expression throwing a dynamic error
    /// </summary>

    public class XPathWithDynamicError : Example
    {

        public override String testName
        {
            get { return "XPathWithDynamicError"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XPath expression.
            XPathCompiler compiler = processor.NewXPathCompiler();
            compiler.AllowUndeclaredVariables = true;
            XPathExecutable expression = compiler.Compile("$a gt $b");
            XPathSelector selector = expression.Load();

            // Set the values of the variables
            selector.SetVariable(new QName("", "a"), new XdmAtomicValue(10));
            selector.SetVariable(new QName("", "b"), new XdmAtomicValue("Paris"));

            // Evaluate the XPath expression
            Console.WriteLine(selector.EvaluateSingle().ToString());
        }
    }

    /// <summary>
    /// XSLT 2.0 transformation with source document and stylesheet supplied as URIs
    /// </summary>

    public class XsltSimple1 : Example
    {

        public override String testName
        {
            get { return "XsltSimple1"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/books.xsl")).Load30();

            // Set the root node of the source document to be the global context item
            transformer.GlobalContextItem = input;

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }
    }

    /// <summary>
    /// Run a transformation, sending the serialized output to a file
    /// </summary>

    public class XsltSimple2 : Example
    {

        public override String testName
        {
            get { return "XsltSimple2"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/identity.xsl")).Load30();

            // Create a serializer
            String outfile = "OutputFromXsltSimple2.xml";
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputStream(new FileStream(outfile, FileMode.Create, FileAccess.Write));

            // Transform the source XML and serialize the result to the output file.
            transformer.ApplyTemplates(input, serializer);

            Console.WriteLine("\nOutput written to " + outfile + "\n");
        }
    }

    /// <summary>
    /// XSLT 2.0 transformation with source document and stylesheet supplied as files
    /// </summary>

    public class XsltSimple3 : Example
    {

        public override String testName
        {
            get { return "XsltSimple3"; }
        }

        public override void run(Uri samplesDir)
        {
            if (samplesDir.Scheme != Uri.UriSchemeFile)
            {
                Console.WriteLine("Supplied URI must be a file directory");
            }
            String dir = samplesDir.AbsolutePath;
            String sourceFile = dir + "data/books.xml";
            String styleFile = dir + "styles/books.xsl";

            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.BaseUri = new Uri(samplesDir, "data/books.xml");

            XdmNode input = builder.Build(File.OpenRead(sourceFile));

            // Create a transformer for the stylesheet.
            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri(samplesDir, "styles/books.xsl");
            Xslt30Transformer transformer = compiler.Compile(File.OpenRead(styleFile)).Load30();

            // Set the root node of the source document to be the global context item
            transformer.GlobalContextItem = input;

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }
    }


    /// <summary>
    /// XSLT 2.0 transformation showing stripping of whitespace controlled by the stylesheet
    /// </summary>

    public class XsltStripSpace : Example
    {

        public override String testName
        {
            get { return "XsltStripSpace"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();

            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.BaseUri = samplesDir;

            String doc = "<doc>  <a>  <b>text</b>  </a>  <a/>  </doc>";
            MemoryStream ms = new MemoryStream();
            StreamWriter tw = new StreamWriter(ms);
            tw.Write(doc);
            tw.Flush();
            Stream instr = new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            XdmNode input = builder.Build(instr);

            // Create a transformer for the stylesheet.
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>" +
                "<xsl:strip-space elements='*'/>" +
                "<xsl:template match='/'>" +
                "  <xsl:copy-of select='.'/>" +
                "</xsl:template>" +
                "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = samplesDir;
            Xslt30Transformer transformer = compiler.Compile(new XmlTextReader(new StringReader(stylesheet))).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }
    }


	/// <summary>
	/// Run a transformation, compiling the stylesheet once (into an XsltExecutable) and using it to transform two 
	/// different source documents
	/// </summary>

    public class XsltReuseExecutable : Example
    {

        public override String testName
        {
            get { return "XsltReuseExecutable"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create a compiled stylesheet
            XsltExecutable templates = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/summarize.xsl"));

            // Note: we could actually use the same Xslt30Transformer in this case.
            // But in principle, the two transformations could be done in parallel in separate threads.

            String sourceFile1 = "data/books.xml";
            String sourceFile2 = "data/othello.xml";

            // Do the first transformation
            Console.WriteLine("\n\n----- transform of " + sourceFile1 + " -----");
            Xslt30Transformer transformer1 = templates.Load30();
            XdmNode input1 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, sourceFile1));
            transformer1.ApplyTemplates(input1, processor.NewSerializer(Console.Out));     // default destination is Console.Out

            // Do the second transformation
            Console.WriteLine("\n\n----- transform of " + sourceFile2 + " -----");
            Xslt30Transformer transformer2 = templates.Load30();
            XdmNode input2 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, sourceFile2));
            transformer2.ApplyTemplates(input2, processor.NewSerializer(Console.Out));     // default destination is Console.Out
        }
    }

    /// <summary>
    /// Show that the Xslt30Transformer is serially reusable; run a transformation twice using the same stylesheet
    /// and the same stylesheet parameters, but with a different input document.
    /// </summary>

    public class XsltReuseTransformer : Example
    {

        public override String testName
        {
            get { return "XsltReuseTransformer"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Compile the stylesheet
            XsltExecutable exec = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/summarize.xsl"));

            // Create a transformer 
            Xslt30Transformer transformer = exec.Load30();
            
            // Set the stylesheet parameters
            Dictionary<QName, XdmValue> params1 = new Dictionary<QName, XdmValue>();
            params1.Add(new QName("", "", "include-attributes"), new XdmAtomicValue(false));
            transformer.SetStylesheetParameters(params1);

            // Load the 1st source document, building a tree
            XdmNode input1 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Run the transformer once
            XdmDestination results = new XdmDestination();
            transformer.ApplyTemplates(input1, results);
            Console.WriteLine("1: " + results.XdmNode.OuterXml);

            // Load the 2nd source document, building a tree
            XdmNode input2 = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/more-books.xml"));

            // Run the transformer again
            results.Reset();
            transformer.ApplyTemplates(input2, results);
            Console.WriteLine("2: " + results.XdmNode.OuterXml);
        }
    }

    /// <summary>
    /// Run a sequence of transformations in a pipeline, each one acting as a filter
    /// </summary>

    public class XsltFilterChain : Example
    {

        public override String testName
        {
            get { return "XsltFilterChain"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile all three stylesheets
            Xslt30Transformer transformer1 = compiler.Compile(new Uri(samplesDir, "styles/identity.xsl")).Load30();
            Xslt30Transformer transformer2 = compiler.Compile(new Uri(samplesDir, "styles/books.xsl")).Load30();
            Xslt30Transformer transformer3 = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Now run them in series
            XdmDestination results1 = new XdmDestination();
            transformer1.ApplyTemplates(input, results1);
            //Console.WriteLine("After phase 1:");
            //Console.WriteLine(results1.XdmNode.OuterXml);

            XdmDestination results2 = new XdmDestination();
            transformer2.GlobalContextItem = results1.XdmNode;
            transformer2.ApplyTemplates(results1.XdmNode, results2);
            //Console.WriteLine("After phase 2:");
            //Console.WriteLine(results2.XdmNode.OuterXml);

            XdmDestination results3 = new XdmDestination();
            transformer3.ApplyTemplates(results2.XdmNode, results3);
            Console.WriteLine("After phase 3:");
            Console.WriteLine(results3.XdmNode.OuterXml);
        }
    }

    /// <summary>
    /// Transform from an XDM tree to an XDM tree
    /// </summary>

    public class XsltXdmToXdm : Example
    {

        public override String testName
        {
            get { return "XsltXdmToXdm"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile the stylesheet
            Xslt30Transformer transformer = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Run the transformation
            XdmDestination result = new XdmDestination();
            transformer.ApplyTemplates(input, result);

            // Serialize the result so we can see that it worked
            StringWriter sw = new StringWriter();
            result.XdmNode.WriteTo(new XmlTextWriter(sw));
            Console.WriteLine(sw.ToString());

            // Note: we don't do 
            //   result.XdmNode.WriteTo(new XmlTextWriter(Console.Out));
            // because that results in the Console.out stream being closed, 
            // with subsequent attempts to write to it being rejected.
        }
    }

    /// <summary>
    /// Run an XSLT transformation from an XDM tree, starting at a node that is not the document node
    /// </summary>

    public class XsltXdmElementToXdm : Example
    {

        public override String testName
        {
            get { return "XsltXdmElementToXdm"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/othello.xml"));

            // Navigate to the first grandchild
            XPathSelector eval = processor.NewXPathCompiler().Compile("/PLAY/FM[1]").Load();
            eval.ContextItem = input;
            input = (XdmNode)eval.EvaluateSingle();

            // Create an XSLT compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile the stylesheet
            Xslt30Transformer transformer = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Run the transformation
            XdmDestination result = new XdmDestination();
            transformer.ApplyTemplates(input, result);

            // Serialize the result so we can see that it worked
            Console.WriteLine(result.XdmNode.OuterXml);
        }
    }

    /// <summary>
    /// Run a transformation from a DOM (System.Xml.Document) input to a DOM output
    /// </summary>

    public class XsltDomToDom : Example
    {

        public override String testName
        {
            get { return "XsltDomToDom"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document (in practice, it would already exist as a DOM)
            XmlDocument doc = new XmlDocument();
            doc.Load(new XmlTextReader(samplesDir.AbsolutePath + "data/othello.xml"));
            XdmNode input = processor.NewDocumentBuilder().Wrap(doc);

            // Create a compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Compile the stylesheet
            Xslt30Transformer transformer = compiler.Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Run the transformation
            DomDestination result = new DomDestination();
            transformer.ApplyTemplates(input, result);

            // Serialize the result so we can see that it worked
            Console.WriteLine(result.XmlDocument.OuterXml);
        }
    }


    /// <summary>
    /// Run a transformation driven by an xml-stylesheet processing instruction in the source document
    /// </summary>

    public class XsltProcessingInstruction : Example
    {

        public override string testName
        {
            get { return "XsltProcessingInstruction"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();
            XsltExecutable exec;

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));
            //Console.WriteLine("=============== source document ===============");
            //Console.WriteLine(input.OuterXml);
            //Console.WriteLine("=========== end of source document ============");

            // Navigate to the xml-stylesheet processing instruction having the pseudo-attribute type=text/xsl;
            // then extract the value of the href pseudo-attribute if present

            String path = @"/processing-instruction(xml-stylesheet)[matches(.,'type\s*=\s*[''""]text/xsl[''""]')]" +
                    @"/replace(., '.*?href\s*=\s*[''""](.*?)[''""].*', '$1')";

            XPathSelector eval = processor.NewXPathCompiler().Compile(path).Load();
            eval.ContextItem = input;
            XdmAtomicValue hrefval = (XdmAtomicValue)eval.EvaluateSingle();
            String href = (hrefval == null ? null : hrefval.ToString());

            if (href == null || href == "")
            {
                Console.WriteLine("No suitable xml-stylesheet processing instruction found");
                return;

            }
            else if (href[0] == '#')
            {

                // The stylesheet is embedded in the source document and identified by a URI of the form "#id"

                Console.WriteLine("Locating embedded stylesheet with href = " + href);
                String idpath = "id('" + href.Substring(1) + "')";
                eval = processor.NewXPathCompiler().Compile(idpath).Load();
                eval.ContextItem = input;
                XdmNode node = (XdmNode)eval.EvaluateSingle();
                if (node == null)
                {
                    Console.WriteLine("No element found with ID " + href.Substring(1));
                    return;
                }
                exec = processor.NewXsltCompiler().Compile(node);

            }
            else
            {

                // The stylesheet is in an external document

                Console.WriteLine("Locating stylesheet at uri = " + new Uri(input.BaseUri, href));

                // Fetch and compile the referenced stylesheet
                exec = processor.NewXsltCompiler().Compile(new Uri(input.BaseUri, href.ToString()));
            }

            // Create a transformer 
            Xslt30Transformer transformer = exec.Load30();

            // Set the root node of the source document to be the global context item
            transformer.GlobalContextItem = input;

            // Run it       
            XdmDestination results = new XdmDestination();
            transformer.ApplyTemplates(input, results);
            Console.WriteLine(results.XdmNode.OuterXml);

        }
    }

    /// <summary>
    /// Run an XSLT transformation setting serialization properties from the calling application
    /// </summary>

    public class XsltSettingOutputProperties : Example
    {

        public override string testName
        {
            get { return "XsltSettingOutputProperties"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/books.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/summarize.xsl")).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputProperty(Serializer.METHOD, "xml");
            serializer.SetOutputProperty(Serializer.INDENT, "no");
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);
        }

    }

    /// <summary>
    /// Run an XSLT transformation making use of an XmlResolver to resolve URIs at document build time, at stylesheet compile time 
    /// and at transformation run-time
    /// </summary>

    public class XsltUsingSourceResolver : Example
    {

        public override string testName
        {
            get { return "XsltUsingSourceResolver"; }
        }

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            DocumentBuilder builder = processor.NewDocumentBuilder();
            UserXmlResolver buildTimeResolver = new UserXmlResolver();
            buildTimeResolver.Message = "** Calling build-time XmlResolver: ";
            builder.XmlResolver = buildTimeResolver;
            builder.BaseUri = samplesDir;

            String doc = "<!DOCTYPE doc [<!ENTITY e SYSTEM 'flamingo.txt'>]><doc>&e;</doc>";
            MemoryStream ms = new MemoryStream();
            StreamWriter tw = new StreamWriter(ms);
            tw.Write(doc);
            tw.Flush();
            Stream instr = new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            XdmNode input = builder.Build(instr);

            // Create a transformer for the stylesheet.
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>" +
                "<xsl:import href='empty.xslt'/>" +
                "<xsl:template match='/'>" +
                "<out note=\"{doc('heron.txt')}\" ><xsl:copy-of select='.'/></out>" +
                "</xsl:template>" +
                "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            UserXmlResolver compileTimeResolver = new UserXmlResolver();
            compileTimeResolver.Message = "** Calling compile-time XmlResolver: ";
            compiler.XmlResolver = compileTimeResolver;
            compiler.BaseUri = samplesDir;
            Xslt30Transformer transformer = compiler.Compile(new XmlTextReader(new StringReader(stylesheet))).Load30();

            // Set the user-written XmlResolver
            UserXmlResolver runTimeResolver = new UserXmlResolver();
            runTimeResolver.Message = "** Calling transformation-time XmlResolver: ";
            transformer.InputXmlResolver = runTimeResolver;

            // Create a serializer
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);

        }
    }

    /// <summary>
    /// Run an XSLT transformation displaying compile-time errors to the console
    /// </summary>

    public class XsltDisplayingErrors : Example
    {

        public override string testName
        {
            get { return "XsltDisplayingErrors"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();


            // Define a stylesheet containing errors
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>\n" +
                "<xsl:template name='eee:template'>\n" +
                "  <xsl:value-of select='32'/>\n" +
                "</xsl:template>\n" +
                "<xsl:template name='main'>\n" +
                "  <xsl:value-of select='$var'/>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";


            // Attempt to compile the stylesheet and display the errors
            try
            {
                compiler.BaseUri = new Uri("http://localhost/stylesheet");
                compiler.Compile(new XmlTextReader(new StringReader(stylesheet)));
                Console.WriteLine("Stylesheet compilation succeeded");
            }
            catch (Exception)
            {
                Console.WriteLine("Stylesheet compilation failed");
            }


        }
    }

    /// <summary>
    /// Run an XSLT transformation capturing compile-time errors within the application
    /// </summary>

    public class XsltCapturingErrors : Example
    {

        public override string testName
        {
            get { return "XsltCapturingErrors"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Create a list to hold the error information
            compiler.ErrorList = new List<StaticError>();

            // Define a stylesheet containing errors
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>\n" +
                "<xsl:template name='fff:template'>\n" +
                "  <xsl:value-of select='32'/>\n" +
                "</xsl:template>\n" +
                "<xsl:template name='main'>\n" +
                "  <xsl:value-of select='$var'/>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";


            // Attempt to compile the stylesheet and display the errors
            try
            {
                compiler.BaseUri = new Uri("http://localhost/stylesheet");
                compiler.Compile(new StringReader(stylesheet));
                Console.WriteLine("Stylesheet compilation succeeded");
            }
            catch (Exception)
            {
                Console.WriteLine("Stylesheet compilation failed with " + compiler.ErrorList.Count + " errors");
                foreach (StaticError error in compiler.ErrorList)
                {
                    Console.WriteLine("At line " + error.LineNumber + ": " + error.Message);
                }
            }
        }
    }

    /// <summary>
    /// Run an XSLT transformation capturing run-time messages within the application
    /// </summary>

    public class XsltCapturingMessages : Example
    {

        public override string testName
        {
            get { return "XsltCapturingMessages"; }
        }

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new Processor();

            // Create the XSLT Compiler
            XsltCompiler compiler = processor.NewXsltCompiler();

            // Define a stylesheet that generates messages
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                "<xsl:template name='main'>\n" +
                "  <xsl:message><a>starting</a></xsl:message>\n" +
                "  <out><xsl:value-of select='current-date()'/></out>\n" +
                "  <xsl:message><a>finishing</a></xsl:message>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";

            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));


            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Create a Listener to which messages will be written
            transformer.MessageListener = new UserMessageListener();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML, calling a named initial template, and serialize the result document
            transformer.CallTemplate(new QName("", "main"), serializer);
        }

    }

    ///
    /// Example user-written message listener
    ///

    public class UserMessageListener : IMessageListener
    {

        public void Message(XdmNode content, bool terminate, IXmlLocation location)
        {
            Console.Out.WriteLine("MESSAGE terminate=" + (terminate ? "yes" : "no") + " at " + DateTime.Now);
            Console.Out.WriteLine("From instruction at line " + location.LineNumber +
                    " of " + location.BaseUri);
            Console.Out.WriteLine(">>" + content.StringValue);
        }
    }


    /// <summary>
    /// Run an XSLT transformation producing multiple output documents
    /// </summary>

    public class XsltMultipleOutput : Example
    {

        public override string testName
        {
            get { return "XsltMultipleOutput"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();
            processor.SetProperty("http://saxon.sf.net/feature/timing", "true");

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/othello.xml"));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new Uri(samplesDir, "styles/play.xsl")).Load30();

            // Set the required stylesheet parameter
            Dictionary<QName, XdmValue> parameters = new Dictionary<QName, XdmValue>();
            parameters.Add(new QName("", "", "dir"), new XdmAtomicValue(samplesDir.ToString() + "play"));
            transformer.SetStylesheetParameters(parameters);

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);

            // Transform the source XML and serialize the result document
            transformer.ApplyTemplates(input, serializer);

        }

    }


    /// <summary>
    /// Run an XSLT transformation using the id() function, with DTD validation
    /// </summary>

    public class XsltUsingIdFunction : Example
    {

        public override string testName
        {
            get { return "XsltUsingIdFunction"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance
            Processor processor = new Processor();

            // Load the source document. The Microsoft .NET parser does not report attributes of type ID. The only
            // way to use the function is therefore (a) to use a different parser, or (b) to use xml:id instead. We
            // choose the latter course.

            String doc = "<!DOCTYPE table [" +
                "<!ELEMENT table (row*)>" +
                "<!ELEMENT row EMPTY>" +
                "<!ATTLIST row xml:id ID #REQUIRED>" +
                "<!ATTLIST row value CDATA #REQUIRED>]>" +
                "<table><row xml:id='A123' value='green'/><row xml:id='Z789' value='blue'/></table>";

            DocumentBuilder builder = processor.NewDocumentBuilder();
            builder.DtdValidation = true;
            builder.BaseUri = samplesDir;
            MemoryStream ms = new MemoryStream();
            StreamWriter tw = new StreamWriter(ms);
            tw.Write(doc);
            tw.Flush();
            Stream instr = new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            XdmNode input = builder.Build(instr);

            // Define a stylesheet that uses the id() function
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                "<xsl:template match='/'>\n" +
                "  <xsl:copy-of select=\"id('Z789')\"/>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet
            Xslt30Transformer transformer = exec.Load30();

            // Set the destination
            XdmDestination results = new XdmDestination();

            // Transform the XML
            transformer.ApplyTemplates(input, results);

            // Show the result
            Console.WriteLine(results.XdmNode.ToString());

        }

    }

    /// <summary>
    /// Show a transformation using a user-written result document handler. This example
    /// captures each of the result documents in a DOM, and creates a Hashtable that indexes
    /// the DOM trees according to their absolute URI. On completion, it writes all the DOMs
    /// to the standard output.
    /// </summary>

    public class XsltUsingResultHandler : Example
    {

        public override string testName
        {
            get { return "XsltUsingResultHandler"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Load the source document
            XdmNode input = processor.NewDocumentBuilder().Build(new Uri(samplesDir, "data/othello.xml"));

            // Define a stylesheet that splits the document up
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                "<xsl:template match='/'>\n" +
                "  <xsl:for-each select='//ACT'>\n" +
                "    <xsl:result-document href='{position()}.xml'>\n" +
                "      <xsl:copy-of select='TITLE'/>\n" +
                "    </xsl:result-document>\n" +
                "  </xsl:for-each>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Establish the result document handler
            Hashtable results = new Hashtable();
            transformer.ResultDocumentHandler = new UserResultDocumentHandler(results);

            // Transform the source XML to a NullDestination (because we only want the secondary result files).
            NullDestination destination = new NullDestination();
            destination.BaseUri = samplesDir;
            transformer.ApplyTemplates(input, destination);

            // Process the captured DOM results
            foreach (DictionaryEntry entry in results)
            {
                string uri = (string)entry.Key;
                Console.WriteLine("\nResult File " + uri);
                DomDestination dom = (DomDestination)results[uri];
                Console.Write(dom.XmlDocument.OuterXml);
            }

        }

    }

    /// <summary>
    /// Show a transformation using a registered collection
    /// </summary>

    public class XsltUsingRegisteredCollection : Example
    {

        public override string testName
        {
            get { return "XsltUsingRegisteredCollection"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Define a stylesheet that uses the collection() function
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                "<xsl:template name='main'>\n" +
                " <out>\n" +
                "  <xsl:for-each select=\"collection('http://www.example.org/my-collection')\">\n" +
                "    <document uri='{document-uri(.)}' nodes='{count(//*)}'/>\n" +
                "  </xsl:for-each><zzz/>\n" +
                "  <xsl:for-each select=\"collection('http://www.example.org/my-collection')\">\n" +
                "    <document uri='{document-uri(.)}' nodes='{count(//*)}'/>\n" +
                "  </xsl:for-each>\n" +
                " </out>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";

            // Register a named collection
            Uri[] documentList = new Uri[2];
            documentList[0] = new Uri(samplesDir, "data/othello.xml");
            documentList[1] = new Uri(samplesDir, "data/books.xml");
            processor.RegisterCollection(new Uri("http://www.example.org/my-collection"), documentList);

            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Set the destination
            XdmDestination results = new XdmDestination();

            // Transform the XML, calling a named initial template
            transformer.CallTemplate(new QName("", "main"), results);

            // Show the result
            Console.WriteLine(results.XdmNode.ToString());

        }
    }

    /// <summary>
    /// Show a transformation using a collection that maps to a directory
    /// </summary>

    public class XsltUsingDirectoryCollection : Example
    {

        public override string testName
        {
            get { return "XsltUsingDirectoryCollection"; }
        }

        public override void run(Uri samplesDir)
        {
            // Create a Processor instance.
            Processor processor = new Processor();

            // Define a stylesheet that uses the collection() function
            String stylesheet =
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>\n" +
                "<xsl:template name='main'>\n" +
                " <out>\n" +
                "  <xsl:for-each select=\"collection('" + samplesDir + "?recurse=yes;select=*.xml;on-error=warning')\">\n" +
                "    <document uri='{document-uri(.)}' nodes='{count(//*)}'/>\n" +
                "  </xsl:for-each><zzz/>\n" +
                " </out>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>";


            XsltCompiler compiler = processor.NewXsltCompiler();
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            XsltExecutable exec = compiler.Compile(new StringReader(stylesheet));

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = exec.Load30();

            // Set the destination
            XdmDestination results = new XdmDestination();

            // Transform the XML, calling a named initial template
            transformer.CallTemplate(new QName("", "main"), results);

            // Show the result
            Console.WriteLine(results.XdmNode.ToString());

        }

    }

    /// <summary>
    /// Show a transformation using calls to integrated extension functions (full API)
    /// </summary>

    public class XsltIntegratedExtension : Example
    {

        public override string testName
        {
            get { return "XsltIntegratedExtension"; }
        }

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new Processor();

            // Identify the Processor version
            Console.WriteLine(processor.ProductVersion);

            // Set diagnostics
            //processor.SetProperty("http://saxon.sf.net/feature/trace-external-functions", "true");

            // Create the stylesheet
            String s = @"<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                @" xmlns:math='http://example.math.co.uk/demo' " +
                @" xmlns:env='http://example.env.co.uk/demo' " +
                @" exclude-result-prefixes='math env'> " +
                @" <xsl:template name='go'> " +
                @" <out sqrt2='{math:sqrt(2.0e0)}' " +
                @" defaultNamespace='{env:defaultNamespace()}' " +
                @" sqrtEmpty='{math:sqrt(())}'> " +
                @" <defaultNS value='{env:defaultNamespace()}' xsl:xpath-default-namespace='http://default.namespace.com/' /> " +
                @" </out> " +
                @" </xsl:template></xsl:transform>";

            // Register the integrated extension functions math:sqrt and env:defaultNamespace
            processor.RegisterExtensionFunction(new Sqrt());
            processor.RegisterExtensionFunction(new DefaultNamespace());

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new StringReader(s)).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);
            serializer.SetOutputProperty(Serializer.INDENT, "yes");

            // Transform the source XML, calling a named initial template, and serialize the result document
            transformer.CallTemplate(new QName("go"), serializer);
        }

    }

    /// <summary>
    /// Example extension function to compute a square root, using the full API
    /// </summary>

    public class Sqrt : ExtensionFunctionDefinition
    {
        public override QName FunctionName
        {
            get
            {
                return new QName("http://example.math.co.uk/demo", "sqrt");
            }
        }

        public override int MinimumNumberOfArguments
        {
            get
            {
                return 1;
            }
        }

        public override int MaximumNumberOfArguments
        {
            get
            {
                return 1;
            }
        }

        public override XdmSequenceType[] ArgumentTypes
        {
            get
            {
                return new XdmSequenceType[]{
                    new XdmSequenceType(XdmAtomicType.BuiltInAtomicType(QName.XS_DOUBLE), '?')
                };
            }
        }

        public override XdmSequenceType ResultType(XdmSequenceType[] ArgumentTypes)
        {
            return new XdmSequenceType(XdmAtomicType.BuiltInAtomicType(QName.XS_DOUBLE), '?');
        }

        public override bool TrustResultType
        {
            get
            {
                return true;
            }
        }


        public override ExtensionFunctionCall MakeFunctionCall()
        {
            return new SqrtCall();
        }
    }

    internal class SqrtCall : ExtensionFunctionCall
    {
        public override IEnumerator<XdmItem> Call(IEnumerator<XdmItem>[] arguments, DynamicContext context)
        {
            Boolean exists = arguments[0].MoveNext();
            if (exists)
            {
                XdmAtomicValue arg = (XdmAtomicValue)arguments[0].Current;
                double val = (double)arg.Value;
                double sqrt = System.Math.Sqrt(val);
                return new XdmAtomicValue(sqrt).GetEnumerator();
            }
            else
            {
                return EmptyEnumerator<XdmItem>.INSTANCE;
            }
        }
    }

    /// <summary>
    /// Example extension function to return the default namespace from the static context
    /// </summary>

    public class DefaultNamespace : ExtensionFunctionDefinition
    {
        public override QName FunctionName
        {
            get
            {
                return new QName("http://example.env.co.uk/demo", "defaultNamespace");
            }
        }

        public override int MinimumNumberOfArguments
        {
            get
            {
                return 0;
            }
        }

        public override int MaximumNumberOfArguments
        {
            get
            {
                return 0;
            }
        }

        public override XdmSequenceType[] ArgumentTypes
        {
            get
            {
                return new XdmSequenceType[] { };
            }
        }

        public override bool DependsOnFocus
        {
            get
            {
                return true;
                // actually it depends on the static context rather than the focus; but returning true is necessary
                // to avoid the call being extracted to a global variable.
            }
        }

        public override XdmSequenceType ResultType(XdmSequenceType[] ArgumentTypes)
        {
            return new XdmSequenceType(XdmAtomicType.BuiltInAtomicType(QName.XS_STRING), '?');
        }

        public override bool TrustResultType
        {
            get
            {
                return true;
            }
        }

        public override ExtensionFunctionCall MakeFunctionCall()
        {
            return new DefaultNamespaceCall();
        }
    }

    internal class DefaultNamespaceCall : ExtensionFunctionCall
    {
        private string defaultNamespace;

        public override void SupplyStaticContext(StaticContext context)
        {
            defaultNamespace = context.GetNamespaceForPrefix("");
        }

        public override IEnumerator<XdmItem> Call(IEnumerator<XdmItem>[] arguments, DynamicContext context)
        {
            if (defaultNamespace != null)
            {
                return new XdmAtomicValue(defaultNamespace).GetEnumerator();
            }
            else
            {
                return EmptyEnumerator<XdmItem>.INSTANCE;
            }
        }
    }

    /// <summary>
    /// Show a transformation using calls to an integrated extension function (simple API)
    /// </summary>

    public class XsltSimpleExtension : Example
    {

        public override string testName
        {
            get { return "XsltSimpleExtension"; }
        }

        public override void run(Uri samplesDir)
        {

            // Create a Processor instance.
            Processor processor = new Processor();

            // Identify the Processor version
            Console.WriteLine(processor.ProductVersion);

            // Set diagnostics
            //processor.SetProperty("http://saxon.sf.net/feature/trace-external-functions", "true");

            // Create the stylesheet
            String s = @"<xsl:transform version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                @" xmlns:math='http://example.math.co.uk/demo'> " +
                @" <xsl:template name='go'> " +
                @" <out sqrt2='{math:sqrtSimple(2.0e0)}' " +
                @" sqrtEmpty='{math:sqrtSimple(())}'/> " +
                @" </xsl:template></xsl:transform>";

            // Register the integrated extension function math:sqrtSimple
            processor.RegisterExtensionFunction(new SqrtSimple());

            // Create a transformer for the stylesheet.
            Xslt30Transformer transformer = processor.NewXsltCompiler().Compile(new StringReader(s)).Load30();

            // Create a serializer, with output to the standard output stream
            Serializer serializer = processor.NewSerializer();
            serializer.SetOutputWriter(Console.Out);
            serializer.SetOutputProperty(Serializer.INDENT, "yes");

            // Transform the source XML, calling a named initial template, and serialize the result document
            transformer.CallTemplate(new QName("go"), serializer);
        }

    }

    /// <summary>
    /// Example extension function to compute a square root, using the simple API
    /// </summary>

    public class SqrtSimple : ExtensionFunction
    {
        public XdmValue Call(XdmValue[] arguments)
        {
            if (!(arguments[0] is XdmEmptySequence))
            {
                XdmAtomicValue arg = (XdmAtomicValue)arguments[0].ItemAt(0);
                double val = (double)arg.Value;
                double sqrt = System.Math.Sqrt(val);
                return new XdmAtomicValue(sqrt);
            }
            else
            {
                return XdmValue.MakeValue((double)0);
            }
        }

        public XdmSequenceType[] GetArgumentTypes()
        {
            return new XdmSequenceType[]{
                new XdmSequenceType(XdmAtomicType.BuiltInAtomicType(QName.XS_DOUBLE), '?')
            };
        }

        public QName GetName()
        {
            return new QName("http://example.math.co.uk/demo", "sqrtSimple");
        }

        public XdmSequenceType GetResultType()
        {
            return new XdmSequenceType(XdmAtomicType.BuiltInAtomicType(QName.XS_DOUBLE), ' ');
        }
    }

    /// <summary>
    /// Show a query producing a document as its result and serializing this to a FileStream
    /// </summary>

    public class XQueryToStream : Example
    {

        public override string testName
        {
            get { return "XQueryToStream"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.BaseUri = samplesDir.ToString();
            compiler.DeclareNamespace("saxon", "http://saxon.sf.net/");
            XQueryExecutable exp = compiler.Compile("<saxon:example>{static-base-uri()}</saxon:example>");
            XQueryEvaluator eval = exp.Load();
            Serializer qout = processor.NewSerializer();
            qout.SetOutputProperty(Serializer.METHOD, "xml");
            qout.SetOutputProperty(Serializer.INDENT, "yes");
            qout.SetOutputStream(new FileStream("testoutput.xml", FileMode.Create, FileAccess.Write));
            Console.WriteLine("Output written to testoutput.xml");
            eval.Run(qout);
        }

    }

    /// <summary>
    /// Show a query producing a single atomic value as its result and returning the value
    /// to the C# application
    /// </summary>

    public class XQueryToAtomicValue : Example
    {

        public override string testName
        {
            get { return "XQueryToAtomicValue"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("avg(for $i in 1 to 10 return $i * $i)");
            XQueryEvaluator eval = exp.Load();
            XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.Value.GetType());
            Console.WriteLine("Result value: " + (decimal)result.Value);
        }

	}

	/// <summary>
	/// Show a query producing a sequence as its result and returning the sequence
	/// to the C# application in the form of an iterator. For each item in the
	/// result, its string value is output.
	/// </summary>

	public class XQueryToSequence : Example
	{

		public override string testName
		{
			get { return "XQueryToSequence"; }
		}

		public override void run(Uri samplesDir)
		{
			Processor processor = new Processor();
			XQueryCompiler compiler = processor.NewXQueryCompiler();
			XQueryExecutable exp = compiler.Compile("for $i in 1 to 10 return $i * $i");
			XQueryEvaluator eval = exp.Load();
			XdmValue value = eval.Evaluate();
            foreach (XdmItem item in value)
            {
				Console.WriteLine(item.ToString());
			}

		}

	}

    /// <summary>
    /// Show a query producing a DOM as its input and producing a DOM as its output
    /// </summary>

    public class XQueryToDom : Example
    {

        public override string testName
        {
            get { return "XQueryToDom"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();

            XmlDocument input = new XmlDocument();
            input.Load(new Uri(samplesDir, "data/books.xml").ToString());
            XdmNode indoc = processor.NewDocumentBuilder().Build(new XmlNodeReader(input));

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("<doc>{reverse(/*/*)}</doc>");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = indoc;
            DomDestination qout = new DomDestination();
            eval.Run(qout);
            XmlDocument outdoc = qout.XmlDocument;
            Console.WriteLine(outdoc.OuterXml);
        }

    }

    /// <summary>
    /// Show a query producing a Saxon tree as its input and producing a Saxon tree as its output
    /// </summary>

    public class XQueryToXdm : Example
    {

        public override string testName
        {
            get { return "XQueryToXdm"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();

            DocumentBuilder loader = processor.NewDocumentBuilder();
            loader.BaseUri = new Uri(samplesDir, "data/books.xml");
            XdmNode indoc = loader.Build(loader.BaseUri);

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("<doc>{reverse(/*/*)}</doc>");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = indoc;
            XdmDestination qout = new XdmDestination();
            eval.Run(qout);
            XdmNode outdoc = qout.XdmNode;
            Console.WriteLine(outdoc.OuterXml);
        }

    }

    /// <summary>
    /// Show a query making a direct call to a user-defined function defined in the query
    /// </summary>

    public class XQueryCallFunction : Example
    {

        public override string testName
        {
            get { return "XQueryCallFunction"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();

            XQueryCompiler qc = processor.NewXQueryCompiler();
            Uri uri = new Uri(samplesDir, "data/books.xml");
            XQueryExecutable exp1 = qc.Compile("declare namespace f='f.ns';" +
                   "declare variable $z := 1 + xs:integer(doc-available('" + uri.ToString() + "'));" +
                   "declare variable $p as xs:integer external;" +
                   "declare function f:t1($v1 as xs:integer) { " +
                   "   $v1 div $z + $p" +
                   "};" +
                   "10");
            XQueryEvaluator ev = exp1.Load();
            ev.SetExternalVariable(new QName("", "p"), new XdmAtomicValue(39));
            XdmValue v1 = new XdmAtomicValue(10);
            XdmValue result = ev.CallFunction(new QName("f.ns", "f:t1"), new XdmValue[] { v1 });
            Console.WriteLine("First result (expected 44): " + result.ToString());
            v1 = new XdmAtomicValue(20);
            result = ev.CallFunction(new QName("f.ns", "f:t1"), new XdmValue[] { v1 });
            Console.WriteLine("Second result (expected 49): " + result.ToString());
        }

    }

    /// <summary>
    /// Show a query reading an input document using an XmlReader (the .NET XML parser)
    /// </summary>

    public class XQueryFromXmlReader : Example
    {

        public override string testName
        {
            get { return "XQueryFromXmlReader"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();

            String inputFileName = new Uri(samplesDir, "data/books.xml").ToString();
            //XmlTextReader reader = new XmlTextReader(inputFileName,
            //    UriConnection.getReadableUriStream(new Uri(samplesDir, "data/books.xml")));
            //reader.Normalization = true;

            // Add a validating reader - needed in case there are entity references
            XmlReaderSettings settings = new XmlReaderSettings();
            settings.ValidationType = ValidationType.DTD;
            settings.DtdProcessing = DtdProcessing.Parse;

            // for .NET core, need to set a default XmlResolver to be able to read/resolve the DTD
            settings.XmlResolver = new XmlUrlResolver();
            
            XmlReader validator = XmlReader.Create(inputFileName, settings);

            XdmNode doc = processor.NewDocumentBuilder().Build(validator);

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("/");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = doc;
            Serializer qout = processor.NewSerializer();
            qout.SetOutputProperty(Serializer.METHOD, "xml");
            qout.SetOutputProperty(Serializer.INDENT, "yes");
            qout.SetOutputStream(new FileStream("testoutput2.xml", FileMode.Create, FileAccess.Write));
            Console.WriteLine("Output written to testoutput2.xml");
            eval.Run(qout);
        }

    }

    /// <summary>
    /// Show a query producing a sequence as its result and returning the sequence
    /// to the C# application in the form of an iterator. The sequence is then
    /// output by serializing each item individually, with each item on a new line.
    /// </summary>

    public class XQueryToSerializedSequence : Example
    {

        public override string testName
        {
            get { return "XQueryToSerializedSequence"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();
            String inputFileName = new Uri(samplesDir, "data/books.xml").ToString();
            XmlTextReader reader = new XmlTextReader(inputFileName,
                UriConnection.getReadableUriStream(new Uri(samplesDir, "data/books.xml")));
            reader.Normalization = true;

            // Add a validating reader - needed in case there are entity references
            XmlReaderSettings settings = new XmlReaderSettings();
            settings.ValidationType = ValidationType.DTD;
            settings.DtdProcessing = DtdProcessing.Parse;
            XmlReader validator = XmlReader.Create(reader, settings);

            XdmNode doc = processor.NewDocumentBuilder().Build(reader);

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            XQueryExecutable exp = compiler.Compile("//ISBN");
            XQueryEvaluator eval = exp.Load();
            eval.ContextItem = doc;

            foreach (XdmNode node in eval)
            {
                Console.WriteLine(node.OuterXml);
            }
        }

    }

    /// <summary>
    /// Show a query that takes a parameter (external variable) as input.
    /// The query produces a single atomic value as its result and returns the value
    /// to the C# application. 
    /// </summary>

    public class XQueryUsingParameter : Example
    {

        public override string testName
        {
            get { return "XQueryUsingParameter"; }
        }

        public override void run(Uri samplesDir)
        {
            Processor processor = new Processor();
            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.DeclareNamespace("p", "http://saxon.sf.net/ns/p");
            XQueryExecutable exp = compiler.Compile(
                    "declare variable $p:in as xs:integer external; $p:in * $p:in");
            XQueryEvaluator eval = exp.Load();
            eval.SetExternalVariable(new QName("http://saxon.sf.net/ns/p", "p:in"), new XdmAtomicValue(12));
            XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.Value.GetType());
            Console.WriteLine("Result value: " + (long)result.Value);
        }

    }

    /// <summary>
    /// Show a query consisting of two modules, using a QueryResolver to resolve
    /// the "import module" declaration
    /// </summary>

    public class XQueryMultiModule : Example
    {

        public override string testName
        {
            get { return "XQueryMultiModule"; }
        }

        public override void run(Uri samplesDir)
        {

            String mod1 = "import module namespace m2 = 'http://www.example.com/module2';" +
                          "m2:square(3)";

            String mod2 = "module namespace m2 = 'http://www.example.com/module2';" +
                          "declare function m2:square($p) { $p * $p };";

            Processor processor = new Processor();
            XQueryCompiler compiler = processor.NewXQueryCompiler();

            InlineModuleResolver resolver = new InlineModuleResolver();
            resolver.AddModule(new Uri("http://www.example.com/module2"), mod2);
            compiler.QueryResolver = resolver;
            XQueryExecutable exp = compiler.Compile(mod1);
            XQueryEvaluator eval = exp.Load();

            XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
            Console.WriteLine("Result type: " + result.Value.GetType());
            Console.WriteLine("Result value: " + (long)result.Value);
        }

        // A simple QueryResolver designed to show that the actual query
        // text can come from anywhere: in this case, the resolver maintains
        // a simple mapping of module URIs onto strings.

        public class InlineModuleResolver : IQueryResolver
        {

            private Hashtable modules = new Hashtable();

            public void AddModule(Uri moduleName, String moduleText)
            {
                modules.Add(moduleName, moduleText);
            }

            public Uri[] GetModules(String moduleUri, Uri baseUri, String[] locationHints)
            {
                Uri[] result = { new Uri(moduleUri) };
                return result;
            }

            public Object GetEntity(Uri absoluteUri)
            {
                return modules[absoluteUri];
            }
        }

    }


    public class UriConnection
    {

        // Get a stream for reading from a file:// URI

        public static Stream getReadableUriStream(Uri uri)
        {
            WebRequest request = (WebRequest)WebRequest.Create(uri);
            return request.GetResponse().GetResponseStream();
        }

        // Get a stream for writing to a file:// URI

        public static Stream getWritableUriStream(Uri uri)
        {
            FileWebRequest request = (FileWebRequest)WebRequest.CreateDefault(uri);
            request.Method = "POST";
            return request.GetRequestStream();
        }
    }

    ///
    /// A sample XmlResolver. In the case of a URI ending with ".txt", it returns the
    /// URI itself, wrapped as an XML document. In the case of the URI "empty.xslt", it returns an empty
    /// stylesheet. In all other cases, it returns null, which has the effect of delegating
    /// processing to the standard XmlResolver.
    ///

    public class UserXmlResolver : XmlUrlResolver
    {

        public String Message = null;

        public override object GetEntity(Uri absoluteUri, String role, Type ofObjectToReturn)
        {
            if (Message != null)
            {
                Console.WriteLine(Message + absoluteUri + " (role=" + role + ")");
            }

            if (absoluteUri.ToString().EndsWith(".txt"))
            {
                MemoryStream ms = new MemoryStream();
                StreamWriter tw = new StreamWriter(ms);
                tw.Write("<uri>");
                tw.Write(absoluteUri);
                tw.Write("</uri>");
                tw.Flush();
                return new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            }
            if (absoluteUri.ToString().EndsWith("empty.xslt"))
            {
                String ss = "<transform xmlns='http://www.w3.org/1999/XSL/Transform' version='2.0'/>";
                MemoryStream ms = new MemoryStream();
                StreamWriter tw = new StreamWriter(ms);
                tw.Write(ss);
                tw.Flush();
                return new MemoryStream(ms.GetBuffer(), 0, (int)ms.Length);
            }
            else
            {
                return null;
            }
        }
    }

    public class UserResultDocumentHandler : IResultDocumentHandler
    {

        private Hashtable results;

        public UserResultDocumentHandler(Hashtable table)
        {
            this.results = table;
        }

        public XmlDestination HandleResultDocument(string href, Uri baseUri)
        {
            DomDestination destination = new DomDestination();
            results[href] = destination;
            return destination;
        }

    }
}


//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
