//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
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

package com.threerings.resource;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.net.AttachableURLFactory;
import com.samskivert.util.StringUtil;

import com.threerings.geom.GeomUtil;

import static com.threerings.resource.Log.log;

/**
 * This class is not used directly, except by a registering ResourceManager
 * so that we can load data from the resource manager using URLs of the form
 * <code>resource://&lt;resourceSet&gt;/&lt;path&gt;</code>. ResourceSet may
 * be the empty string to load from the default resource sets.
 */
public class Handler extends URLStreamHandler
{
    /**
     * Register this class to handle "resource" urls
     * ("resource://<i>resourceSet</i>/<i>path</i>") with the specified
     * ResourceManager.
     */
    public static void registerHandler (ResourceManager rmgr)
    {
        // we only want to wire up our Handler once, but if a new resource
        // manager comes along, we'll point the existing handler there
        if (_rmgr == null) {
            // wire up our handler with the handy dandy attachable URL factory
            AttachableURLFactory.attachHandler("resource", Handler.class);
        }
        _rmgr = rmgr;
    }

    @Override
    protected int hashCode (URL url)
    {
        return String.valueOf(url).hashCode();
    }

    @Override
    protected boolean equals (URL u1, URL u2)
    {
        return String.valueOf(u1).equals(String.valueOf(u2));
    }

    @Override
    protected URLConnection openConnection (URL url)
        throws IOException
    {
        return new URLConnection(url) {
            @Override
            public void connect ()
                throws IOException
            {
                // the host is the bundle name
                String bundle = this.url.getHost();
                // and we need to remove the leading '/' from path;
                String path = this.url.getPath().substring(1);
                try {
                    // if there are query parameters, we need special magic
                    String query = url.getQuery();
                    if (!StringUtil.isBlank(query)) {
                        _stream = getStream(bundle, path, query);
                    } else if (StringUtil.isBlank(bundle)) {
                        _stream = _rmgr.getResource(path);
                    } else {
                        _stream = _rmgr.getResource(bundle, path);
                    }
                    this.connected = true;

                } catch (IOException ioe) {
                    log.warning("Could not find resource [url=" + this.url +
                        ", error=" + ioe.getMessage() + "].");
                    throw ioe; // rethrow
                }
            }

            @Override
            public InputStream getInputStream ()
                throws IOException
            {
                if (!this.connected) {
                    connect();
                }
                return _stream;
            }

            @Override
            public Permission getPermission ()
                throws IOException
            {
                // We allow anything in the resource bundle to be loaded
                // without any permission restrictions.
                return null;
            }

            protected InputStream _stream;
        };
    }

    /**
     * Does some magic to allow a subset of an image to be extracted,
     * reencoded as a PNG and then spat back out to the Java content
     * handler system for inclusion in internal documentation.
     */
    protected InputStream getStream (String bundle, String path, String query)
        throws IOException
    {
        // we can only do this with PNGs
        if (!path.endsWith(".png")) {
            log.warning("Requested sub-tile of non-PNG resource " +
                        "[bundle=" + bundle + ", path=" + path +
                        ", dims=" + query + "].");
            return _rmgr.getResource(bundle, path);
        }

        // parse the query string
        String[] bits = StringUtil.split(query, "&");
        int width = -1, height = -1, tidx = -1;
        try {
            for (int ii = 0; ii < bits.length; ii++) {
                if (bits[ii].startsWith("width=")) {
                    width = Integer.parseInt(bits[ii].substring(6));
                } else if (bits[ii].startsWith("height=")) {
                    height = Integer.parseInt(bits[ii].substring(7));
                } else if (bits[ii].startsWith("tile=")) {
                    tidx = Integer.parseInt(bits[ii].substring(5));
                }
            }
        } catch (NumberFormatException nfe) {
        }
        if (width <= 0 || height <= 0 || tidx < 0) {
            log.warning("Bogus sub-image dimensions [bundle=" + bundle +
                        ", path=" + path + ", dims=" + query + "].");
            throw new FileNotFoundException(path);
        }

        // locate the tile image, then write that subimage back out in PNG format into memory and
        // return an input stream for that
        BufferedImage src = StringUtil.isBlank(bundle) ?
            _rmgr.getImageResource(path) : _rmgr.getImageResource(bundle, path);
        Rectangle trect = GeomUtil.getTile(
            src.getWidth(), src.getHeight(), width, height, tidx);
        BufferedImage tile = src.getSubimage(
            trect.x, trect.y, trect.width, trect.height);
        ByteArrayOutInputStream data = new ByteArrayOutInputStream();
        ImageIO.write(tile, "PNG", data);
        return data.getInputStream();
    }

    /** Our singleton resource manager. */
    protected static ResourceManager _rmgr;
}
