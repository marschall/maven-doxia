package org.apache.maven.doxia.module.confluence.parser;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;

/**
 * Re-usable builder that can be used to generate paragraph and list item text from a string containing all the content
 * and wiki formatting.
 * 
 * @author Dave Syer
 */
public class ChildBlocksBuilder
{

    /**
     * Utility method to convert marked up content into blocks for rendering.
     * 
     * @param input a String with no line breaks
     * @return a list of Blocks that can be used to render it
     */
    public List getBlocks( String input )
    {

        boolean insideBold = false;
        boolean insideItalic = false;
        boolean insideLink = false;

        List blocks = new ArrayList();

        StringBuffer text = new StringBuffer();

        for ( int i = 0; i < input.length(); i++ )
        {
            char c = input.charAt( i );

            switch ( c )
            {
                case '*':
                    if ( insideBold )
                    {
                        TextBlock tb = new TextBlock( text.toString() );
                        blocks.add( new BoldBlock( Arrays.asList( new Block[] { tb } ) ) );
                        text = new StringBuffer();
                    }
                    else
                    {
                        text = addTextBlockIfNecessary( blocks, text );
                        insideBold = true;
                    }

                    break;
                case '_':
                    if ( insideItalic )
                    {
                        TextBlock tb = new TextBlock( text.toString() );
                        blocks.add( new ItalicBlock( Arrays.asList( new Block[] { tb } ) ) );
                        text = new StringBuffer();
                    }
                    else
                    {
                        text = addTextBlockIfNecessary( blocks, text );
                        insideItalic = true;
                    }

                    break;
                case '[':
                    insideLink = true;
                    text = addTextBlockIfNecessary( blocks, text );
                    break;
                case ']':
                    if ( insideLink )
                    {
                        String link = text.toString();

                        if ( link.indexOf( "|" ) > 0 )
                        {
                            String[] pieces = StringUtils.split( text.toString(), "|" );
                            blocks.add( new LinkBlock( pieces[1], pieces[0] ) );
                        }
                        else
                        {
                            blocks.add( new LinkBlock( link, link ) );
                        }

                        text = new StringBuffer();
                    }

                    break;
                case '{':

                    if ( input.charAt( i + 1 ) == '{' ) // it's monospaced
                    {
                        i++;
                    }
                    // else it's a confluence macro...
                    text = addTextBlockIfNecessary( blocks, text );

                    break;
                case '}':

                    // System.out.println( "line = " + line );

                    if ( input.charAt( i + 1 ) == '}' )
                    {
                        i++;
                        TextBlock tb = new TextBlock( text.toString() );
                        blocks.add( new MonospaceBlock( Arrays.asList( new Block[] { tb } ) ) );
                        text = new StringBuffer();
                    }
                    else
                    {
                        String name = text.toString();
                        if ( name.startsWith( "anchor:" ) )
                        {
                            blocks.add( new AnchorBlock( name.substring( "anchor:".length() ) ) );
                        }
                        else
                        {
                            blocks.add( new TextBlock( "{" + name + "}" ) );
                        }
                        text = new StringBuffer();
                    }

                    break;
                case '\\':

                    // System.out.println( "line = " + line );

                    if ( input.charAt( i + 1 ) == '\\' )
                    {
                        i++;
                        text = addTextBlockIfNecessary( blocks, text );
                        blocks.add( new LinebreakBlock() );
                    }
                    else
                    {
                        i++;
                        text.append( input.charAt( i ) );
                    }

                    break;
                default:
                    text.append( c );
            }
        }

        if ( text.length() > 0 )
        {
            blocks.add( new TextBlock( text.toString() ) );
        }

        return blocks;
    }

    private StringBuffer addTextBlockIfNecessary( List blocks, StringBuffer text )
    {
        if ( text.length() == 0 )
        {
            return text;
        }
        blocks.add( new TextBlock( text.toString() ) );
        return new StringBuffer();
    }
}