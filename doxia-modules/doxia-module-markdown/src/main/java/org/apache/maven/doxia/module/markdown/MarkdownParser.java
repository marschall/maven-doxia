package org.apache.maven.doxia.module.markdown;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.doxia.markup.HtmlMarkup;
import org.apache.maven.doxia.module.xhtml.XhtmlParser;
import org.apache.maven.doxia.parser.AbstractParser;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.sink.Sink;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link org.apache.maven.doxia.parser.Parser} for Markdown documents.
 * <p/>
 * Defers effective parsing to the <a href="https://github.com/vsch/flexmark-java">flexmark-java library</a>,
 * which generates HTML content then delegates parsing of this content to a slightly modified Doxia Xhtml parser.
 * (before 1.8, the <a href="http://pegdown.org">PegDown library</a> was used)
 *
 * @author Vladimir Schneider <vladimir@vladsch.com>
 * @author Julien Nicoulaud <julien.nicoulaud@gmail.com>
 * @since 1.3
 */
@Component( role = Parser.class, hint = "markdown" )
public class MarkdownParser
    extends AbstractParser
{

    /**
     * The role hint for the {@link MarkdownParser} Plexus component.
     */
    public static final String ROLE_HINT = "markdown";

    /**
     * Regex that identifies a multimarkdown-style metadata section at the start of the document
     */
    private static final String MULTI_MARKDOWN_METADATA_SECTION =
        "^(((?:[^\\s:][^:]*):(?:.*(?:\r?\n\\p{Blank}+[^\\s].*)*\r?\n))+)(?:\\s*\r?\n)";

    /**
     * Regex that captures the key and value of a multimarkdown-style metadata entry.
     */
    private static final String MULTI_MARKDOWN_METADATA_ENTRY =
        "([^\\s:][^:]*):(.*(?:\r?\n\\p{Blank}+[^\\s].*)*)\r?\n";

    /**
     * In order to ensure that we have minimal risk of false positives when slurping metadata sections, the
     * first key in the metadata section must be one of these standard keys or else the entire metadata section is
     * ignored.
     */
    private static final String[] STANDARD_METADATA_KEYS =
        { "title", "author", "date", "address", "affiliation", "copyright", "email", "keywords", "language", "phone",
            "subtitle" };

    public int getType()
    {
        return TXT_TYPE;
    }

    @Requirement
    private MarkdownHtmlParser parser;

    public void parse( Reader source, Sink sink )
        throws ParseException
    {
        try
        {
            // Markdown to HTML (using flexmark-java library)
            String html = toHtml( source );
            // then HTML to Sink API
            parser.parse( new StringReader( html ), sink );
        }
        catch ( IOException e )
        {
            throw new ParseException( "Failed reading Markdown source document", e );
        }
    }

    /**
     * uses flexmark-java library to parse content and generate HTML output.
     *
     * @param source the Markdown source
     * @return HTML content generated by flexmark-java
     * @throws IOException passed through
     */
    private String toHtml( Reader source )
        throws IOException
    {
        String text = IOUtil.toString( source );
        MutableDataHolder flexmarkOptions = PegdownOptionsAdapter.flexmarkOptions(
                Extensions.ALL & ~( Extensions.HARDWRAPS | Extensions.ANCHORLINKS ) ).toMutable();
        ArrayList<Extension> extensions = new ArrayList<Extension>();
        for ( Extension extension : flexmarkOptions.get( com.vladsch.flexmark.parser.Parser.EXTENSIONS ) )
        {
            extensions.add( extension );
        }

        extensions.add( FlexmarkDoxiaExtension.create() );
        flexmarkOptions.set( com.vladsch.flexmark.parser.Parser.EXTENSIONS, extensions );
        flexmarkOptions.set( HtmlRenderer.HTML_BLOCK_OPEN_TAG_EOL, false );
        flexmarkOptions.set( HtmlRenderer.HTML_BLOCK_CLOSE_TAG_EOL, false );
        flexmarkOptions.set( HtmlRenderer.MAX_TRAILING_BLANK_LINES, -1 );

        com.vladsch.flexmark.parser.Parser parser = com.vladsch.flexmark.parser.Parser.builder( flexmarkOptions )
                .build();
        HtmlRenderer renderer = HtmlRenderer.builder( flexmarkOptions ).build();

        StringBuilder html = new StringBuilder( 1000 );
        html.append( "<html>" );
        html.append( "<head>" );
        Pattern metadataPattern = Pattern.compile( MULTI_MARKDOWN_METADATA_SECTION, Pattern.MULTILINE );
        Matcher metadataMatcher = metadataPattern.matcher( text );
        boolean haveTitle = false;
        if ( metadataMatcher.find() )
        {
            metadataPattern = Pattern.compile( MULTI_MARKDOWN_METADATA_ENTRY, Pattern.MULTILINE );
            Matcher lineMatcher = metadataPattern.matcher( metadataMatcher.group( 1 ) );
            boolean first = true;
            while ( lineMatcher.find() )
            {
                String key = StringUtils.trimToEmpty( lineMatcher.group( 1 ) );
                if ( first )
                {
                    boolean found = false;
                    for ( String k : STANDARD_METADATA_KEYS )
                    {
                        if ( k.equalsIgnoreCase( key ) )
                        {
                            found = true;
                            break;
                        }
                    }
                    if ( !found )
                    {
                        break;
                    }
                    first = false;
                }
                String value = StringUtils.trimToEmpty( lineMatcher.group( 2 ) );
                if ( "title".equalsIgnoreCase( key ) )
                {
                    haveTitle = true;
                    html.append( "<title>" );
                    html.append( StringEscapeUtils.escapeXml( value ) );
                    html.append( "</title>" );
                }
                else if ( "author".equalsIgnoreCase( key ) )
                {
                    html.append( "<meta name=\'author\' content=\'" );
                    html.append( StringEscapeUtils.escapeXml( value ) );
                    html.append( "\' />" );
                }
                else if ( "date".equalsIgnoreCase( key ) )
                {
                    html.append( "<meta name=\'date\' content=\'" );
                    html.append( StringEscapeUtils.escapeXml( value ) );
                    html.append( "\' />" );
                }
                else
                {
                    html.append( "<meta name=\'" );
                    html.append( StringEscapeUtils.escapeXml( key ) );
                    html.append( "\' content=\'" );
                    html.append( StringEscapeUtils.escapeXml( value ) );
                    html.append( "\' />" );
                }
            }
            if ( !first )
            {
                text = text.substring( metadataMatcher.end() );
            }
        }

        Node rootNode = parser.parse( text );
        String markdownHtml = renderer.render( rootNode );

        if ( !haveTitle && rootNode.hasChildren() )
        {
            // use the first (non-comment) node only if it is a heading
            Node firstNode = rootNode.getFirstChild();
            while ( firstNode != null && !( firstNode instanceof Heading ) )
            {
                if ( !( firstNode instanceof HtmlCommentBlock ) )
                {
                    break;
                }
                firstNode = firstNode.getNext();
            }

            if ( firstNode instanceof Heading )
            {
                html.append( "<title>" );
                TextCollectingVisitor collectingVisitor = new TextCollectingVisitor();
                String headingText = collectingVisitor.collectAndGetText( firstNode );
                html.append( StringEscapeUtils.escapeXml( headingText ) );
                html.append( "</title>" );
            }
        }
        html.append( "</head>" );
        html.append( "<body>" );
        html.append( markdownHtml );
        html.append( "</body>" );
        html.append( "</html>" );

        return html.toString();
    }

    /**
     * Internal parser for HTML generated by the Markdown library.
     */
    @Component( role = MarkdownHtmlParser.class )
    public static class MarkdownHtmlParser
        extends XhtmlParser
    {
        public MarkdownHtmlParser()
        {
            super();
        }

        @Override
        protected boolean baseEndTag( XmlPullParser parser, Sink sink )
        {
            boolean visited = super.baseEndTag( parser, sink );
            if ( !visited )
            {
                if ( parser.getName().equals( HtmlMarkup.DIV.toString() ) )
                {
                    handleUnknown( parser, sink, TAG_TYPE_END );
                    visited = true;
                }
            }
            return visited;
        }

        @Override
        protected boolean baseStartTag( XmlPullParser parser, Sink sink )
        {
            boolean visited = super.baseStartTag( parser, sink );
            if ( !visited )
            {
                if ( parser.getName().equals( HtmlMarkup.DIV.toString() ) )
                {
                    handleUnknown( parser, sink, TAG_TYPE_START );
                    visited = true;
                }
            }
            return visited;
        }
    }
}
