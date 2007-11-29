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

package com.threerings.flash {

import flash.events.EventDispatcher;
import flash.events.IEventDispatcher;
import flash.events.KeyboardEvent;
import flash.events.TimerEvent;

import flash.utils.Dictionary;
import flash.utils.Timer;

/**
 * A very simple class that adapts the KeyboardEvents generated by some source and altering
 * (or blocking) the key repeat rate.
 */
public class KeyRepeatLimiter extends EventDispatcher
{
    /**
     * Create a KeyRepeatLimiter that will be limiting key repeat events from
     * the specified source.
     *
     * @param limitRate 0 to block all key repeats, or a millisecond value specifying how often
     * to dispatch KEY_DOWN events while the key is being held down. The rate will be limted
     * by the frame rate of the enclosing SWF.
     */
    public function KeyRepeatLimiter (source :IEventDispatcher, limitRate :int = 0)
    {
        _source = source;

        _source.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown);
        _source.addEventListener(KeyboardEvent.KEY_UP, handleKeyUp);

        if (limitRate > 0) {
            _timer = new Timer(limitRate);
            _timer.addEventListener(TimerEvent.TIMER, handleTimerEvent);
        }
    }

    /**
     * Dispose of this KeyRepeatBlocker.
     */
    public function shutdown () :void
    {
        _source.removeEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown);
        _source.removeEventListener(KeyboardEvent.KEY_UP, handleKeyUp);

        _down = new Dictionary();
        _keysDown = 0;
        if (_timer != null) {
            _timer.stop();
        }
    }

    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        if (undefined === _down[event.keyCode]) {
            _keysDown++;
            _down[event.keyCode] = event.clone();

            // maybe start the timer if we need to
            if (_timer != null && !_timer.running) {
                _timer.reset();
                _timer.start();
            }

            // only dispatch if we're not limiting this key
            dispatchEvent(event);
        }
    }

    protected function handleKeyUp (event :KeyboardEvent) :void
    {
        if (undefined !== _down[event.keyCode]) {
            delete _down[event.keyCode];
            _keysDown--;

            if (_timer != null && _keysDown == 0) {
                _timer.stop();
            }
        }

        // always dispatch an up
        dispatchEvent(event);
    }

    protected function handleTimerEvent (event :TimerEvent) :void
    {
        // dispatch any saved key events
        for each (var keyEvent :KeyboardEvent in _down) {
            dispatchEvent(keyEvent);
        }
    }

    /** Our source. */
    protected var _source :IEventDispatcher;

    /** Tracks whether a key is currently being held down. */
    protected var _down :Dictionary = new Dictionary();

    /** How many keys are currently down? */
    protected var _keysDown :int;

    /** The timer generating key repeats, if any. */
    protected var _timer :Timer;
}
}