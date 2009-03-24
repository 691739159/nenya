//
// $Id$

package com.threerings.flex {

import mx.core.IFactory;
import mx.core.ScrollPolicy;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import mx.controls.List;

import com.threerings.util.Util;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.DSet_Entry;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EventListener;
import com.threerings.presents.dobj.NamedEvent;

/**
 * A list that renders the contents of a DSet.
 */
public class DSetList extends List
    implements EventListener
{
    public function DSetList (
        renderer :IFactory, sortFn :Function = null, filterFn :Function = null)
    {
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
        selectable = false;

        itemRenderer = renderer;

        _data = new ArrayCollection();
        var sort :Sort = new Sort();
        sort.compareFunction = sortFn;
        _data.sort = sort;
        _data.filterFunction = filterFn;
        _data.refresh();
        dataProvider = _data;
    }

    /**
     * Set the aata to be displayed by this List, if not being attached to a DObject.
     */
    public function setData (array :Array /* of DSet_Entry */) :void
    {
        _data.source = (array == null) ? [] : array;
        refresh();
    }

    /**
     * Refresh the data, which can be called if something external has changed
     * the sort order or the rendering.
     */
    public function refresh () :void
    {
        _data.refresh();
    }

    /**
     * Initialize listening on the specified object.
     *
     * @param object the DObject on which to listen.
     * @param field the name of the DSet field that we're displaying.
     * @param refreshFields additional fields that cause refresh() to be called should
     *        AttributeChangedEvents arrive on them.
     */
    public function init (object :DObject, field :String, ... refreshFields) :void
    {
        shutdown();

        _object = object;
        _field = field;
        _refreshFields = refreshFields;
        _object.addListener(this);

        setEntries(_object[_field] as DSet);
    }

    /**
     * Shut down listening.
     */
    public function shutdown () :void
    {
        if (_object != null) {
            _object.removeListener(this);
            _object = null;
            _field = null;
            _refreshFields = null;
            setData(null);
        }
    }

    /**
     * Safely set the entries, even if null.
     */
    protected function setEntries (entries :DSet) :void
    {
        setData((entries == null) ? null : entries.toArray());
    }

    /**
     * From com.threerings.presents.dobj.EventListener. Not actually part of the public API.
     */
    public function eventReceived (event :DEvent) :void
    {
        if (!(event is NamedEvent)) {
            return;
        }
        var name :String = NamedEvent(event).getName();
        if (name == _field) {
            if (event is EntryAddedEvent) {
                _data.list.addItem(EntryAddedEvent(event).getEntry());

            } else if (event is EntryUpdatedEvent) {
                var entry :DSet_Entry = EntryUpdatedEvent(event).getEntry();
                var upIdx :int = findKeyIndex(entry.getKey());
                if (upIdx != -1) {
                    _data.list.setItemAt(entry, upIdx);
                }

            } else if (event is EntryRemovedEvent) {
                var remIdx :int = findKeyIndex(EntryRemovedEvent(event).getKey());
                if (remIdx != -1) {
                    _data.list.removeItemAt(remIdx);
                }

            } else if (event is AttributeChangedEvent) {
                setEntries(AttributeChangedEvent(event).getValue() as DSet);
                return; // skip the full refresh
            }

        } else if (-1 == _refreshFields.indexOf(name)) {
            return; // this name is not our concern: do nothing
        }

        // we reach this line if the event was for a _refreshField, or a Set event on _field
        refresh();
    }

    /**
     * Find the index of the specified entry key from the raw list.
     */
    protected function findKeyIndex (key :Object) :int
    {
        for (var ii :int = 0; ii < _data.list.length; ii++) {
            if (Util.equals(key, DSet_Entry(_data.list.getItemAt(ii)).getKey())) {
                return ii;
            }
        }
        return -1;
    }

    /** The collection for the List, This is the filtered/sorted view. For the
     * the raw list, we access _data.list. */
    protected var _data :ArrayCollection;

    /** The object on which we're listening. */
    protected var _object :DObject;

    /** The fieldname of the DSet in the _object. */
    protected var _field :String;

    /** Fields on which to watch for updates and call refresh(). */
    protected var _refreshFields :Array;
}
}
