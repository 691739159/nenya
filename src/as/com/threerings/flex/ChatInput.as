//
// $Id$

package com.threerings.flex {

import flash.events.FocusEvent;

import mx.controls.TextInput;

/**
 * The class name of an image to use as the input prompt.
 */
[Style(name="prompt", type="Class")]

/**
 * A special TextInput for entering Chat. One of these is used in ChatControl.
 *
 * A standard TextInput has the stupid behavior of selecting all the text when it receives
 * focus. Disable that so that we can receive focus and people can type and we don't blow away
 * whatever they had before.
 */
public class ChatInput extends TextInput
{
    public function ChatInput ()
    {
        width = 147;
        showPrompt(true);
    }

    override public function stylesInitialized () :void
    {
        super.stylesInitialized();

        checkShowPrompt();
    }

    override public function styleChanged (styleProp :String) :void
    {
        super.styleChanged(styleProp);

        if (styleProp == "prompt") {
            checkShowPrompt();
        }
    }

    protected function checkShowPrompt () :void
    {
        showPrompt(focusManager == null || focusManager.getFocus() != this);
    }

    protected function showPrompt (show :Boolean) :void
    {
        setStyle("backgroundImage", (show && ("" == text)) ? getStyle("prompt") : undefined);
    }

    override protected function focusInHandler (event :FocusEvent) :void
    {
        var oldValue :Boolean = textField.selectable;
        textField.selectable = false;
        try {
            super.focusInHandler(event);

        } finally {
            textField.selectable = oldValue;
        }
        showPrompt(false);
    }

    override protected function focusOutHandler (event :FocusEvent) :void
    {
        super.focusOutHandler(event);
        showPrompt(true);
    }
}
}
