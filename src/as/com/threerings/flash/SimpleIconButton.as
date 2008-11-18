//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.SimpleButton;

import flash.geom.ColorTransform;

/**
 * Takes a BitmapData and makes a button that brightens on hover and depresses when pushed.
 */
public class SimpleIconButton extends SimpleButton
{
    /**
     * Constructor.
     * 
     * @param icon a BitmapData, or Bitmap (from which the BitmapData will be extracted), or
     *             a Class that instantiates into either a BitmapData or Bitmap.
     */
    public function SimpleIconButton (icon :*)
    {
        var bmp :BitmapData;
        if (icon is Class) {
            icon = new (Class(icon))();
        }
        if (icon is BitmapData ) {
            bmp = BitmapData(icon);
        } else if (icon is Bitmap) {
            bmp = Bitmap(icon).bitmapData;
        } else {
            throw new Error("Unknown icon spec: must be a Bitmap or BitmapData, or a Class " +
                "that becomes one.");
        }

        const bright :ColorTransform = new ColorTransform(1.25, 1.25, 1.25);
        upState = new Bitmap(bmp);
        overState = new Bitmap(bmp);
        overState.transform.colorTransform = bright;
        downState = new Bitmap(bmp);
        downState.y = 1;
        downState.transform.colorTransform = bright;
        hitTestState = upState;
    }
}
}
