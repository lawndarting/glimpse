/*
 * Copyright (c) 2012, Metron, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Metron, Inc. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL METRON, INC. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.metsci.glimpse.axis.tagged.painter;

import static com.metsci.glimpse.axis.tagged.Tag.TEX_COORD_ATTR;

import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GL;

import com.metsci.glimpse.axis.Axis1D;
import com.metsci.glimpse.axis.painter.label.AxisLabelHandler;
import com.metsci.glimpse.axis.tagged.Tag;
import com.metsci.glimpse.axis.tagged.TaggedAxis1D;
import com.sun.opengl.util.BufferUtil;

/**
 * A horizontal (x) axis painter which displays positions of tags in addition
 * to tick marks and labels. A color scale is also displayed which stretches
 * between specified tags.
 *
 * Tags which are to act as anchor points for the color scale must be given
 * the {@link com.metsci.glimpse.axis.tagged.Tag#TEX_COORD_ATTR} attribute.
 * The value of this attribute should be between 0.0 and 1.0 and indicates
 * where along the color scale this tag should be anchored. For example, a
 * tag with value 20.0 and TEX_COORD_ATTR attribute value 0.5 would indicate
 * that the color half-way along the color scale should map to value 20.0
 * on the axis.
 *
 * This axis must be added to a
 * {@link com.metsci.glimpse.layout.GlimpseAxisLayout1D} whose associated
 * axis is a {@link com.metsci.glimpse.axis.tagged.TaggedAxis1D}.
 *
 * @author ulman
 */
public class TaggedPartialColorXAxisPainter extends TaggedColorXAxisPainter
{
    protected FloatBuffer vertexCoords;
    protected FloatBuffer textureCoords;

    public TaggedPartialColorXAxisPainter( AxisLabelHandler ticks )
    {
        super( ticks );
    }

    @Override
    protected void paintColorScale( GL gl, Axis1D axis, int width, int height )
    {
        if ( colorTexture != null && axis instanceof TaggedAxis1D )
        {
            TaggedAxis1D taggedAxis = (TaggedAxis1D) axis;

            colorTexture.prepare( gl, 0 );

            int count = updateCoordinateBuffers( taggedAxis, width, height );

            gl.glTexEnvf( GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE );
            gl.glPolygonMode( GL.GL_FRONT, GL.GL_FILL );

            gl.glEnable( GL.GL_TEXTURE_1D );

            gl.glEnableClientState( GL.GL_VERTEX_ARRAY );
            gl.glEnableClientState( GL.GL_TEXTURE_COORD_ARRAY );

            gl.glVertexPointer( 2, GL.GL_FLOAT, 0, vertexCoords.rewind( ) );
            gl.glTexCoordPointer( 1, GL.GL_FLOAT, 0, textureCoords.rewind( ) );

            try
            {
                gl.glDrawArrays( GL.GL_QUAD_STRIP, 0, count );
            }
            finally
            {
                gl.glDisableClientState( GL.GL_VERTEX_ARRAY );
                gl.glDisableClientState( GL.GL_TEXTURE_COORD_ARRAY );
                gl.glDisable( GL.GL_TEXTURE_1D );
            }
        }

        gl.glDisable( GL.GL_TEXTURE_1D );

        outlineColorQuad( gl, axis, width, height );
    }

    protected int updateCoordinateBuffers( TaggedAxis1D taggedAxis, int width, int height )
    {
        List<Tag> tags = taggedAxis.getSortedTags( );

        int size = tags.size( );

        if ( vertexCoords == null || vertexCoords.capacity( ) < size * 4 ) vertexCoords = BufferUtil.newFloatBuffer( size * 4 );

        if ( textureCoords == null || textureCoords.capacity( ) < size * 2 ) textureCoords = BufferUtil.newFloatBuffer( size * 2 );

        vertexCoords.rewind( );
        textureCoords.rewind( );

        int y1 = getColorBarMinY( height );
        int y2 = getColorBarMaxY( height );

        int count = 0;
        for ( Tag tag : tags )
        {
            Object attr = tag.getAttribute( TEX_COORD_ATTR );
            if ( attr != null && attr.getClass( ) == Float.class )
            {
                float textureCoord = ( Float ) attr;
                float vertexCoord = ( float ) taggedAxis.valueToScreenPixel( tag.getValue( ) );

                vertexCoords.put( vertexCoord ).put( y1 ).put( vertexCoord ).put( y2 );
                textureCoords.put( textureCoord ).put( textureCoord );

                count += 2;
            }
        }

        return count;
    }
}
