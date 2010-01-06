//
// $Id: BundledComponentRepositoryTest.java 3720 2005-10-05 01:39:45Z mdb $
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.cast.bundle;

import java.util.Iterator;

import java.awt.Component;

import junit.framework.Test;
import junit.framework.TestCase;

import com.threerings.resource.ResourceManager;

import com.threerings.media.image.ClientImageManager;

import com.threerings.cast.ComponentClass;

public class BundledComponentRepositoryTest extends TestCase
{
    public BundledComponentRepositoryTest ()
    {
        super(BundledComponentRepositoryTest.class.getName());
    }

    @Override
    public void runTest ()
    {
        try {
            ResourceManager rmgr = new ResourceManager("rsrc");
            rmgr.initBundles(
                null, "config/resource/manager.properties", null);
            ClientImageManager imgr = new ClientImageManager(rmgr, (Component)null);
            BundledComponentRepository repo =
                new BundledComponentRepository(rmgr, imgr, "components");

//             System.out.println("Classes: " + StringUtil.toString(
//                                    repo.enumerateComponentClasses()));

//             System.out.println("Actions: " + StringUtil.toString(
//                                    repo.enumerateActionSequences()));

//             System.out.println("Action sets: " + StringUtil.toString(
//                                    repo._actionSets.values().iterator()));

            Iterator<ComponentClass> iter = repo.enumerateComponentClasses();
            while (iter.hasNext()) {
                // ComponentClass cclass = (ComponentClass)
                iter.next();
//                 System.out.println("IDs [" + cclass + "]: " +
//                                    StringUtil.toString(
//                                        repo.enumerateComponentIds(cclass)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public static void main (String[] args)
    {
        BundledComponentRepositoryTest test =
            new BundledComponentRepositoryTest();
        test.runTest();
    }

    public static Test suite ()
    {
        return new BundledComponentRepositoryTest();
    }
}
