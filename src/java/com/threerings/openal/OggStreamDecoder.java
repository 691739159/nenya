//
// $Id: OggFileStream.java 119 2007-01-24 00:22:12Z dhoover $
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2005 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.openal;

import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;

import com.jmex.sound.openAL.objects.util.OggInputStream;

/**
 * Decodes Ogg Vorbis streams.
 */
public class OggStreamDecoder extends StreamDecoder
{
    // documentation inherited
    public void init (InputStream in)
        throws IOException
    {
        _istream = new OggInputStream(in);
    }

    // documentation inherited
    public int getFormat ()
    {
        return (_istream.getFormat() == OggInputStream.FORMAT_MONO16) ?
            AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
    }

    // documentation inherited
    public int getFrequency ()
    {
        return _istream.getRate();
    }

    // documentation inherited
    public int read (ByteBuffer buf)
        throws IOException
    {
        return _istream.read(buf, buf.position(), buf.remaining());
    }

    /** The underlying Ogg input stream. */
    protected OggInputStream _istream;
}
