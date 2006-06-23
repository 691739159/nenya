//
// $Id: SpriteManager.java 3733 2005-10-13 19:00:08Z ray $
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2004 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.media.sprite;

import java.awt.Graphics2D;
import java.awt.Shape;

import java.util.Collections;
import java.util.List;
import java.util.Iterator;

import com.threerings.media.AbstractMediaManager;
import com.threerings.media.MediaPanel;

/**
 * The sprite manager manages the sprites running about in the game.
 */
public class SpriteManager extends AbstractMediaManager
{
    /** A predicate used to operate on sprites (see {@link #removeSprites}. */
    public static interface Predicate
    {
        /** Returns true if this sprite is to be included by the predicate,
         * false if it should be excluded. */
        public boolean evaluate (Sprite sprite);
    }

    /**
     * Construct and initialize the sprite manager.
     */
    public SpriteManager (MediaPanel panel)
    {
        super(panel);
    }

    /**
     * When an animated view processes its dirty rectangles, it may
     * require an expansion of the dirty region which may in turn
     * require the invalidation of more sprites than were originally
     * invalid. In such cases, the animated view can call back to the
     * sprite manager, asking it to append the sprites that intersect
     * a particular region to the given list.
     *
     * @param list the list to fill with any intersecting sprites.
     * @param shape the shape in which we have interest.
     */
    public void getIntersectingSprites (List list, Shape shape)
    {
        int size = _media.size();
        for (int ii = 0; ii < size; ii++) {
            Sprite sprite = (Sprite)_media.get(ii);
            if (sprite.intersects(shape)) {
                list.add(sprite);
            }
        }
    }

    /**
     * When an animated view is determining what entity in its view is
     * under the mouse pointer, it may require a list of sprites that are
     * "hit" by a particular pixel. The sprites' bounds are first checked
     * and sprites with bounds that contain the supplied point are further
     * checked for a non-transparent at the specified location.
     *
     * @param list the list to fill with any intersecting sprites, the
     *        sprites with the highest render order provided first.
     * @param x the x (screen) coordinate to be checked.
     * @param y the y (screen) coordinate to be checked.
     */
    public void getHitSprites (List list, int x, int y)
    {
        for (int ii = _media.size() - 1; ii >= 0; ii--) {
            Sprite sprite = (Sprite)_media.get(ii);
            if (sprite.hitTest(x, y)) {
                list.add(sprite);
            }
        }
    }

    /**
     * Finds the sprite with the highest render order that hits the
     * specified pixel.
     *
     * @param x the x (screen) coordinate to be checked
     * @param y the y (screen) coordinate to be checked
     * @return the highest sprite hit
     */
    public Sprite getHighestHitSprite (int x, int y)
    {
        // since they're stored in lowest -> highest order..
        for (int ii = _media.size() - 1; ii >= 0; ii--) {
            Sprite sprite = (Sprite)_media.get(ii);
            if (sprite.hitTest(x, y)) {
                return sprite;
            }
        }
        return null;
    }
    
    /**
     * Add a sprite to the set of sprites managed by this manager.
     *
     * @param sprite the sprite to add.
     */
    public void addSprite (Sprite sprite)
    {
        if (insertMedia(sprite)) {
            // and invalidate the sprite's original position
            sprite.invalidate();
        }
    }

    /**
     * Returns a list of all sprites registered with the sprite manager.
     * The returned list is immutable, sprites should be added or removed
     * using {@link #addSprite} or {@link #removeSprite}.
     */
    public List getSprites ()
    {
        return Collections.unmodifiableList(_media);
    }

    /**
     * Returns an iterator over our managed sprites. Do not call
     * {@link Iterator#remove}.
     */
    public Iterator enumerateSprites ()
    {
        return _media.iterator();
    }

    /**
     * Removes the specified sprite from the set of sprites managed by
     * this manager.
     *
     * @param sprite the sprite to remove.
     */
    public void removeSprite (Sprite sprite)
    {
        removeMedia(sprite);
    }

    /**
     * Removes all sprites that match the supplied predicate.
     */
    public void removeSprites (Predicate pred)
    {
        int idxoff = 0;
        for (int ii = 0, ll = _media.size(); ii < ll; ii++) {
            Sprite sprite = (Sprite)_media.get(ii-idxoff);
            if (pred.evaluate(sprite)) {
                _media.remove(sprite);
                sprite.invalidate();
                sprite.shutdown();
                // we need to preserve the original "index" relative to the
                // current tick position, so we don't decrement ii directly
                idxoff++;
                if (ii <= _tickpos) {
                    _tickpos--;
                }
            }
        }
    }

    /**
     * Render the sprite paths to the given graphics context.
     *
     * @param gfx the graphics context.
     */
    public void renderSpritePaths (Graphics2D gfx)
    {
        for (int ii=0, nn=_media.size(); ii < nn; ii++) {
            Sprite sprite = (Sprite)_media.get(ii);
	    sprite.paintPath(gfx);
        }
    }

// NOTE- collision handling code is turned off for now. To re-implement,
// a new array should be kept with sprites sorted in some sort of x/y order
//
//    /**
//     * Check all sprites for collisions with others and inform any
//     * sprite observers.
//     */
//    protected void handleCollisions ()
//    {
//	// gather a list of all sprite collisions
//	int size = _sprites.size();
//	for (int ii = 0; ii < size; ii++) {
//            Sprite sprite = (Sprite)_sprites.get(ii);
//	    checkCollisions(ii, size, sprite);
//	}
//    }
//
//    /**
//     * Check a sprite for collision with any other sprites in the
//     * sprite list and notify the sprite observers associated with any
//     * sprites that do indeed collide.
//     *
//     * @param idx the starting sprite index.
//     * @param size the total number of sprites.
//     * @param sprite the sprite to check against other sprites for
//     * collisions.
//     */
//    protected void checkCollisions (int idx, int size, Sprite sprite)
//    {
//	// TODO: make this handle quickly moving objects that may pass
//	// through each other.
//
//	// if we're the last sprite we know we've already handled any
//	// collisions
//	if (idx == (size - 1)) {
//	    return;
//	}
//
//	// calculate the x-position of the right edge of the sprite we're
//	// checking for collisions
//	Rectangle bounds = sprite.getBounds();
//	int edgeX = bounds.x + bounds.width;
//
//	for (int ii = (idx + 1); ii < size; ii++) {
//	    Sprite other = (Sprite)_sprites.get(ii);
//	    Rectangle obounds = other.getBounds();
//	    if (obounds.x > edgeX) {
//		// since sprites are stored in the list sorted by
//		// ascending x-position, we know this sprite and any
//		// other sprites farther on in the list can't possibly
//		// intersect with the sprite we're checking, so we're
//		// done.
//                return;
//
//	    } else if (obounds.intersects(bounds)) {
//		sprite.notifyObservers(new CollisionEvent(sprite, other));
//	    }
//	}
//    }
//    /** The comparator used to sort sprites by horizontal position. */
//    protected static final Comparator SPRITE_COMP = new SpriteComparator();
//
//    /** Used to sort sprites. */
//    protected static class SpriteComparator implements Comparator
//    {
//	public int compare (Object o1, Object o2)
//	{
//	    Sprite s1 = (Sprite)o1;
//	    Sprite s2 = (Sprite)o2;
//	    return (s2.getX() - s1.getX());
//	}
//    }
}
