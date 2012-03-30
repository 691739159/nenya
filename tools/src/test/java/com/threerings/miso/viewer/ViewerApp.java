//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

package com.threerings.miso.viewer;

import java.io.IOException;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.samskivert.swing.util.SwingUtil;

import com.threerings.resource.ResourceManager;

import com.threerings.media.FrameManager;
import com.threerings.media.image.ClientImageManager;
import com.threerings.media.image.ColorPository;
import com.threerings.media.tile.bundle.BundledTileSetRepository;

import com.threerings.miso.data.SimpleMisoSceneModel;
import com.threerings.miso.tile.MisoTileManager;
import com.threerings.miso.tools.xml.SimpleMisoSceneParser;
import com.threerings.miso.util.MisoContext;

import com.threerings.cast.CharacterManager;
import com.threerings.cast.bundle.BundledComponentRepository;

import static com.threerings.miso.Log.log;

/**
 * The ViewerApp is a scene viewing application that allows for trying out game scenes in a
 * pseudo-runtime environment.
 */
public class ViewerApp
{
    /**
     * Construct and initialize the ViewerApp object.
     */
    public ViewerApp (String[] args)
        throws IOException
    {
        if (args.length < 1) {
            System.err.println("Usage: ViewerApp scene_file.xml");
            System.exit(-1);
        }

        // get the graphics environment
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // get the target graphics device
        GraphicsDevice gd = env.getDefaultScreenDevice();
        log.info("Graphics device", "dev", gd, "mem", gd.getAvailableAcceleratedMemory(),
            "displayChange", gd.isDisplayChangeSupported(),
            "fullScreen", gd.isFullScreenSupported());

        // get the graphics configuration and display mode information
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        DisplayMode dm = gd.getDisplayMode();
        log.info("Display mode", "bits", dm.getBitDepth(), "wid", dm.getWidth(),
            "hei", dm.getHeight(), "refresh", dm.getRefreshRate());

        // create the window
        _frame = new ViewerFrame(gc);
        _framemgr = FrameManager.newInstance(_frame);

        // we don't need to configure anything
        ResourceManager rmgr = new ResourceManager("rsrc");
        rmgr.initBundles(null, "config/resource/manager.properties", null);
        ClientImageManager imgr = new ClientImageManager(rmgr, _frame);
        _tilemgr = new MisoTileManager(rmgr, imgr);
        _tilemgr.setTileSetRepository(new BundledTileSetRepository(rmgr, imgr, "tilesets"));

        // create the context object
        MisoContext ctx = new ContextImpl();

        // create the various managers
        BundledComponentRepository crepo =
            new BundledComponentRepository(rmgr, imgr, "components");
        CharacterManager charmgr = new CharacterManager(imgr, crepo);
        ColorPository cpos = ColorPository.loadColorPository(rmgr);

        // create our scene view panel
        _panel = new ViewerSceneViewPanel(ctx, charmgr, crepo, cpos);
        _frame.setPanel(_panel);

        // load up the scene specified by the user
        try {
            SimpleMisoSceneParser parser = new SimpleMisoSceneParser("");
            SimpleMisoSceneModel model = parser.parseScene(args[0]);
            if (model == null) {
                log.warning("No miso scene found in scene file", "path", args[0]);
                System.exit(-1);
            }
            _panel.setSceneModel(model);

        } catch (Exception e) {
            log.warning("Unable to parse scene", "path", args[0], e);
            System.exit(-1);
        }

        // size and position the window, entering full-screen exclusive
        // mode if available
        if (gd.isFullScreenSupported()) {
            log.info("Entering full-screen exclusive mode.");
            gd.setFullScreenWindow(_frame);

        } else {
            log.warning("Full-screen exclusive mode not available.");
            // _frame.pack();
            _frame.setSize(600, 400);
            SwingUtil.centerWindow(_frame);
        }
    }

    /**
     * The implementation of the MisoContext interface that provides
     * handles to the config and manager objects that offer commonly used
     * services.
     */
    protected class ContextImpl implements MisoContext
    {
        public MisoTileManager getTileManager () {
            return _tilemgr;
        }

        public FrameManager getFrameManager () {
            return _framemgr;
        }
    }

    /**
     * Run the application.
     */
    public void run ()
    {
        // show the window
        _frame.setVisible(true);
        _framemgr.start();
    }

    /**
     * Instantiate the application object and start it running.
     */
    public static void main (String[] args)
    {
        try {
            ViewerApp app = new ViewerApp(args);
            app.run();
        } catch (IOException ioe) {
            System.err.println("Error initializing viewer app.");
            ioe.printStackTrace();
        }
    }

    /** The tile manager object. */
    protected MisoTileManager _tilemgr;

    /** The frame manager. */
    protected FrameManager _framemgr;

    /** The main application window. */
    protected ViewerFrame _frame;

    /** The main panel. */
    protected ViewerSceneViewPanel _panel;
}
