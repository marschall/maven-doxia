package org.apache.maven.doxia.util;

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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.text.html.HTML.Tag;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.doxia.markup.HtmlMarkup;
import org.codehaus.plexus.util.StringUtils;

/**
 * The <code>HtmlTools</code> class defines methods to HTML handling.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 1.0
 */
public class HtmlTools
{
    private static final Tag[] ALL_TAGS  =
    {
        HtmlMarkup.A, HtmlMarkup.ABBR, HtmlMarkup.ACRONYM, HtmlMarkup.ADDRESS, HtmlMarkup.APPLET,
        HtmlMarkup.AREA, HtmlMarkup.B, HtmlMarkup.BASE, HtmlMarkup.BASEFONT, HtmlMarkup.BDO,
        HtmlMarkup.BIG, HtmlMarkup.BLOCKQUOTE, HtmlMarkup.BODY, HtmlMarkup.BR, HtmlMarkup.BUTTON,
        HtmlMarkup.CAPTION, HtmlMarkup.CENTER, HtmlMarkup.CITE, HtmlMarkup.CODE, HtmlMarkup.COL,
        HtmlMarkup.COLGROUP, HtmlMarkup.DD, HtmlMarkup.DEL, HtmlMarkup.DFN, HtmlMarkup.DIR,
        HtmlMarkup.DIV, HtmlMarkup.DL, HtmlMarkup.DT, HtmlMarkup.EM, HtmlMarkup.FIELDSET,
        HtmlMarkup.FONT, HtmlMarkup.FORM, HtmlMarkup.FRAME, HtmlMarkup.FRAMESET, HtmlMarkup.H1,
        HtmlMarkup.H2, HtmlMarkup.H3, HtmlMarkup.H4, HtmlMarkup.H5, HtmlMarkup.H6, HtmlMarkup.HEAD,
        HtmlMarkup.HR, HtmlMarkup.HTML, HtmlMarkup.I, HtmlMarkup.IFRAME, HtmlMarkup.IMG,
        HtmlMarkup.INPUT, HtmlMarkup.INS, HtmlMarkup.ISINDEX, HtmlMarkup.KBD, HtmlMarkup.LABEL,
        HtmlMarkup.LEGEND, HtmlMarkup.LI, HtmlMarkup.LINK, HtmlMarkup.MAP, HtmlMarkup.MENU,
        HtmlMarkup.META, HtmlMarkup.NOFRAMES, HtmlMarkup.NOSCRIPT, HtmlMarkup.OBJECT, HtmlMarkup.OL,
        HtmlMarkup.OPTGROUP, HtmlMarkup.OPTION, HtmlMarkup.P, HtmlMarkup.PARAM, HtmlMarkup.PRE,
        HtmlMarkup.Q, HtmlMarkup.S, HtmlMarkup.SAMP, HtmlMarkup.SCRIPT, HtmlMarkup.SELECT,
        HtmlMarkup.SMALL, HtmlMarkup.SPAN, HtmlMarkup.STRIKE, HtmlMarkup.STRONG, HtmlMarkup.STYLE,
        HtmlMarkup.SUB, HtmlMarkup.SUP, HtmlMarkup.TABLE, HtmlMarkup.TBODY, HtmlMarkup.TD,
        HtmlMarkup.TEXTAREA, HtmlMarkup.TFOOT, HtmlMarkup.TH, HtmlMarkup.THEAD, HtmlMarkup.TITLE,
        HtmlMarkup.TR, HtmlMarkup.TT, HtmlMarkup.U, HtmlMarkup.UL, HtmlMarkup.VAR
    };

    private static final Hashtable TAG_HASHTABLE = new Hashtable( ALL_TAGS.length );

    private static final int ASCII = 0x7E;

    static
    {
        for ( int i = 0; i < ALL_TAGS.length; i++ )
        {
            TAG_HASHTABLE.put( ALL_TAGS[i].toString(), ALL_TAGS[i] );
        }
    }

    /**
     * Returns a tag for a defined HTML tag name (i.e. one of
     * the tags defined in {@link org.apache.maven.doxia.markup.HtmlMarkup}.
     * If the given name does not represent one of the defined tags, then
     * <code>null</code> will be returned.
     *
     * @param tagName the <code>String</code> name requested.
     * @return a tag constant corresponding to the <code>tagName</code>,
     *    or <code>null</code> if not found.
     * @see <a href="http://www.w3.org/TR/html401/index/elements.html">http://www.w3.org/TR/html401/index/elements.html</a>
     * @since 1.1
     */
    public static Tag getHtmlTag( String tagName )
    {
        Object t =  TAG_HASHTABLE.get( tagName );

        return ( t == null ? null : (Tag) t );
    }

    /**
     * Escape special HTML characters in a String in <code>xml</code> mode.
     *
     * @param text the String to escape, may be null.
     * @return The escaped text or the empty string if text == null.
     * @see #escapeHTML(String,boolean)
     */
    public static String escapeHTML( String text )
    {
        return escapeHTML( text, true );
    }

    /**
     * Escape special HTML characters in a String.
     *
     * <pre>
     * < becomes <code>&lt;</code>
     * > becomes <code>&gt;</code>
     * & becomes <code>&amp;</code>
     * " becomes <code>&quot;</code>
     * </pre>
     *
     * If <code>xmlMode</code> is true, every other character than the above remains unchanged,
     * if <code>xmlMode</code> is false, non-ascii characters get replaced by their hex code.
     *
     * <b>Note</b>: all characters are encoded, i.e.:
     * <pre>
     * \u0159       = &#38;#x159;
     * \uD835\uDFED = &#38;#x1d7ed;
     * </pre>
     *
     * @param text The String to escape, may be null.
     * @param xmlMode set to <code>false</code> to replace non-ascii characters.
     * @return The escaped text or the empty string if text == null.
     * @since 1.1
     */
    public static final String escapeHTML( String text, boolean xmlMode )
    {
        if ( text == null )
        {
            return "";
        }

        int length = text.length();
        StringBuffer buffer = new StringBuffer( length );

        for ( int i = 0; i < length; ++i )
        {
            char c = text.charAt(i);
            switch ( c )
            {
                case '<':
                    buffer.append( "&lt;" );
                    break;
                case '>':
                    buffer.append( "&gt;" );
                    break;
                case '&':
                    buffer.append( "&amp;" );
                    break;
                case '\"':
                    buffer.append( "&quot;" );
                    break;
                default:
                    if ( xmlMode )
                    {
                        buffer.append( c );
                    }
                    else
                    {
                        if ( c <= ASCII )
                        {
                            // ASCII.
                            buffer.append( c );
                        }
                        else
                        {
                            buffer.append( "&#x" );
                            if ( isHighSurrogate( c ) )
                            {
                                buffer.append( Integer.toHexString( toCodePoint( c, text.charAt( ++i ) ) ) );
                            }
                            else
                            {
                                buffer.append( Integer.toHexString( c ) );
                            }
                            buffer.append( ';' );
                        }
                    }
            }
        }

        return buffer.toString();
    }

    /**
     * Unescapes HTML entities in a string.
     *
     * <p> Unescapes a string containing entity escapes to a string
     * containing the actual Unicode characters corresponding to the
     * escapes. Supports HTML 4.0 entities.</p>
     *
     * <p>For example, the string "&amp;lt;Fran&amp;ccedil;ais&amp;gt;"
     * will become "&lt;Fran&ccedil;ais&gt;".</p>
     *
     * <b>Note</b>: all unicode entities are decoded, i.e.:
     * <pre>
     * &#38;#x159;   = \u0159
     * &#38;#x1d7ed; = \uD835\uDFED
     * </pre>
     *
     * @param text the <code>String</code> to unescape, may be null.
     * @return a new unescaped <code>String</code>, <code>null</code> if null string input.
     * @since 1.1.1.
     */
    public static String unescapeHTML( String text )
    {
        if ( text == null )
        {
            return null;
        }

        String unescaped = StringEscapeUtils.unescapeHtml( text );

        if ( !text.equals( unescaped ))
        {
            return unescaped;
        }

        String tmp = text;
        List entities = new ArrayList();
        while ( true )
        {
            int i = tmp.indexOf( "&#x" );
            if ( i == -1 )
            {
                break;
            }

            tmp = tmp.substring( i + 3 );
            if ( tmp.indexOf( ';' ) == -1 )
            {
                throw new IllegalArgumentException( "Wrong HTML near '..." + tmp + "'" );
            }

            String entity = tmp.substring( 0, tmp.indexOf( ';' ) );
            try
            {
                Integer.parseInt( entity, 16 );
            }
            catch ( Exception e )
            {
                throw new IllegalArgumentException( "Wrong HTML near '..." + tmp + "'" );
            }
            entities.add( entity );
        }

        for ( int i = 0; i < entities.size(); i++ )
        {
            String entity = (String) entities.get( i );

            int codePoint = Integer.parseInt( entity, 16 );
            text = StringUtils.replace( text, "&#x" + entity + ";", new String( toChars( codePoint ) ) );
        }

        return text;
    }

    /**
     * Encode an url
     *
     * @param url the String to encode, may be null
     * @return the text encoded, null if null String input
     */
    public static String encodeURL( String url )
    {
        if ( url == null )
        {
            return null;
        }

        StringBuffer encoded = new StringBuffer();
        int length = url.length();

        char[] unicode = new char[1];

        for ( int i = 0; i < length; ++i )
        {
            char c = url.charAt( i );

            switch ( c )
            {
                case ';':
                case '/':
                case '?':
                case ':':
                case '@':
                case '&':
                case '=':
                case '+':
                case '$':
                case ',':
                case '[':
                case ']': // RFC 2732 (IPV6)
                case '-':
                case '_':
                case '.':
                case '!':
                case '~':
                case '*':
                case '\'':
                case '(':
                case ')':
                case '#': // XLink mark
                    encoded.append( c );
                    break;
                default:
                    if ( ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || ( c >= '0' && c <= '9' ) )
                    {
                        encoded.append( c );
                    }
                    else
                    {
                        byte[] bytes;

                        try
                        {
                            unicode[0] = c;
                            bytes = ( new String( unicode, 0, 1 ) ).getBytes( "UTF8" );
                        }
                        catch ( UnsupportedEncodingException cannotHappen )
                        {
                            bytes = new byte[0];
                        }

                        for ( int j = 0; j < bytes.length; ++j )
                        {
                            String hex = DoxiaUtils.byteToHex( bytes[j] );

                            encoded.append( '%' );
                            if ( hex.length() == 1 )
                            {
                                encoded.append( '0' );
                            }
                            encoded.append( hex );
                        }
                    }
            }
        }

        return encoded.toString();
    }

    /**
     * Construct a valid id.
     *
     * <p>
     *   <b>Note</b>: this method is identical to
     *   {@link DoxiaUtils#encodeId(String,boolean) DoxiaUtils.encodeId( id, true)},
     *   the rules to encode an id are laid out there.
     * </p>
     *
     * @param id The id to be encoded.
     * @return The trimmed and encoded id, or null if id is null.
     * @see {@link DoxiaUtils#encodeId(java.lang.String,boolean)}.
     */
    public static String encodeId( String id )
    {
        return DoxiaUtils.encodeId( id, true );
    }

    /**
     * Determines if the specified text is a valid id according to the rules
     * laid out in {@link #encodeId(String)}.
     *
     * @param text The text to be tested.
     * @return <code>true</code> if the text is a valid id, otherwise <code>false</code>.
     * @see #encodeId(String).
     */
    public static boolean isId( String text )
    {
        return DoxiaUtils.isValidId( text );
    }

    private HtmlTools()
    {
        // utility class
    }

    //
    // Imported code from ASF Harmony project
    // http://svn.apache.org/repos/asf/harmony/enhanced/classlib/trunk/modules/luni/src/main/java/java/lang/Character.java
    //

    private static int toCodePoint( char high, char low )
    {
        // See RFC 2781, Section 2.2
        // http://www.faqs.org/rfcs/rfc2781.html
        int h = ( high & 0x3FF ) << 10;
        int l = low & 0x3FF;
        return ( h | l ) + 0x10000;
    }

    private static final char MIN_HIGH_SURROGATE = '\uD800';
    private static final char MAX_HIGH_SURROGATE = '\uDBFF';

    private static boolean isHighSurrogate( char ch )
    {
        return ( MIN_HIGH_SURROGATE <= ch && MAX_HIGH_SURROGATE >= ch );
    }

    private static final int MIN_CODE_POINT = 0x000000;
    private static final int MAX_CODE_POINT = 0x10FFFF;
    private static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;

    private static boolean isValidCodePoint( int codePoint )
    {
        return ( MIN_CODE_POINT <= codePoint && MAX_CODE_POINT >= codePoint );
    }

    private static boolean isSupplementaryCodePoint( int codePoint )
    {
        return ( MIN_SUPPLEMENTARY_CODE_POINT <= codePoint && MAX_CODE_POINT >= codePoint );
    }

    private static char[] toChars( int codePoint )
    {
        if ( !isValidCodePoint( codePoint ) )
        {
            throw new IllegalArgumentException();
        }

        if ( isSupplementaryCodePoint( codePoint ) )
        {
            int cpPrime = codePoint - 0x10000;
            int high = 0xD800 | ( ( cpPrime >> 10 ) & 0x3FF );
            int low = 0xDC00 | ( cpPrime & 0x3FF );
            return new char[] { (char) high, (char) low };
        }
        return new char[] { (char) codePoint };
    }
}
