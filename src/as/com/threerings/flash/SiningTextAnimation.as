package com.threerings.flash {

import flash.text.TextFormat;

public class SiningTextAnimation extends TextCharAnimation
{
    public function SiningTextAnimation (text :String, format :TextFormat)
    {
        super(text, movementFn, format);
    }

    protected function movementFn (elapsed :Number, index :Number) :Number
    {
        return 10 * Math.sin((elapsed / 500) + (index * Math.PI / 5));
    }
}
}
