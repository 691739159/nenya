//
// $Id$
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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * Represents a streaming source of sound data.
 */
public abstract class Stream
{
    /**
     * Creates a new stream.  Call {@link #dispose} when finished with the
     * stream.
     *
     * @param soundmgr a reference to the sound manager that will update the
     * stream
     */
    public Stream (SoundManager soundmgr)
    {
        _soundmgr = soundmgr;
        
        // create the source and buffers
        _nbuf = BufferUtils.createIntBuffer(NUM_BUFFERS);
        _nbuf.limit(1);
        AL10.alGenSources(_nbuf);
        _sourceId = _nbuf.get();
        _nbuf.clear();
        AL10.alGenBuffers(_nbuf);
        _nbuf.get(_bufferIds);
        
        // register with sound manager
        _soundmgr.addStream(this);
    }
    
    /**
     * Sets the base gain of the stream.
     */
    public void setGain (float gain)
    {
        _gain = gain;
        if (_fadeMode == FadeMode.NONE) {
            AL10.alSourcef(_sourceId, AL10.AL_GAIN, _gain);
        }
    }
    
    /**
     * Sets the pitch of the stream.
     */
    public void setPitch (float pitch)
    {
        _pitch = pitch;
        AL10.alSourcef(_sourceId, AL10.AL_PITCH, _pitch);
    }
    
    /**
     * Determines whether this stream is currently playing.
     */
    public boolean isPlaying ()
    {
        return _state == AL10.AL_PLAYING;
    }
    
    /**
     * Starts playing this stream.
     */
    public void play ()
    {
        if (_state == AL10.AL_PLAYING) {
            Log.warning("Tried to play stream already playing.");
            return;
        }
        if (_state == AL10.AL_INITIAL) {
            _qidx = _qlen = 0;
            queueBuffers(NUM_BUFFERS);
        }
        AL10.alSourcePlay(_sourceId);
        _state = AL10.AL_PLAYING;
    }
    
    /**
     * Pauses this stream.
     */
    public void pause ()
    {
        if (_state != AL10.AL_PLAYING) {
            Log.warning("Tried to pause stream that wasn't playing.");
            return;
        }
        AL10.alSourcePause(_sourceId);
        _state = AL10.AL_PAUSED;
    }
    
    /**
     * Stops this stream.
     */
    public void stop ()
    {
        if (_state == AL10.AL_STOPPED) {
            Log.warning("Tried to stop stream that was already stopped.");
            return;
        }
        AL10.alSourceStop(_sourceId);
        _state = AL10.AL_STOPPED;
    }
    
    /**
     * Fades this stream in over the specified interval.  If the stream isn't
     * playing, it will be started.
     */
    public void fadeIn (float interval)
    {
        if (_state != AL10.AL_PLAYING) {
            play();
        }
        AL10.alSourcef(_sourceId, AL10.AL_GAIN, 0f);
        _fadeMode = FadeMode.IN;
        _fadeInterval = interval;
        _fadeElapsed = 0f;
    }
    
    /**
     * Fades this stream out over the specified interval.
     *
     * @param dispose if true, dispose of the stream when done fading out
     */
    public void fadeOut (float interval, boolean dispose)
    {
        _fadeMode = dispose ? FadeMode.OUT_DISPOSE : FadeMode.OUT;
        _fadeInterval = interval;
        _fadeElapsed = 0f;
    }
    
    /**
     * Releases the resources held by this stream and removes it from
     * the manager.
     */
    public void dispose ()
    {
        // make sure the stream is stopped
        if (_state != AL10.AL_STOPPED) {
            stop();
        }
        
        // delete the source and buffers
        _nbuf.clear();
        _nbuf.put(_sourceId).flip();
        AL10.alDeleteSources(_nbuf);
        _nbuf.clear();
        _nbuf.put(_bufferIds).flip();
        AL10.alDeleteBuffers(_nbuf);
        
        // remove from manager
        _soundmgr.removeStream(this);
    }

    /**
     * Updates the state of this stream, loading data into buffers and
     * adjusting gain as necessary.  Called periodically by the
     * {@link SoundManager}.
     *
     * @param time the amount of time elapsed since the last update
     */
    protected void update (float time)
    {
        // update fade, which may stop playing
        updateFade(time);
        if (_state != AL10.AL_PLAYING) {
            return;
        }
        
        // find out how many buffers have been played and unqueue them
        int played = AL10.alGetSourcei(_sourceId, AL10.AL_BUFFERS_PROCESSED);
        if (played == 0) {
            return;
        }
        _nbuf.clear();
        for (int ii = 0; ii < played; ii++) {
            _nbuf.put(_bufferIds[_qidx]);
            _qidx = (_qidx + 1) % NUM_BUFFERS;
            _qlen--;
        }
        _nbuf.flip();
        AL10.alSourceUnqueueBuffers(_sourceId, _nbuf);
        
        // enqueue up to the number of buffers played
        queueBuffers(played);
        
        // find out if we're still playing; if not and we have buffers queued,
        // we must restart
        _state = AL10.alGetSourcei(_sourceId, AL10.AL_SOURCE_STATE);
        if (_qlen > 0 && _state != AL10.AL_PLAYING) {
            play();
        }
    }
    
    /**
     * Updates the gain of the stream according to the fade state.
     */
    protected void updateFade (float time)
    {
        if (_fadeMode == FadeMode.NONE) {
            return;
        }
        float alpha = Math.min((_fadeElapsed += time) / _fadeInterval, 1f);
        AL10.alSourcef(_sourceId, AL10.AL_GAIN, _gain *
            (_fadeMode == FadeMode.IN ? alpha : (1f - alpha)));
        if (alpha == 1f) {
            if (_fadeMode == FadeMode.OUT) {
                stop();
            } else if (_fadeMode == FadeMode.OUT_DISPOSE) {
                dispose();
            }
            _fadeMode = FadeMode.NONE;
        }
    }
    
    /**
     * Queues (up to) the specified number of buffers.
     */
    protected void queueBuffers (int buffers)
    {
        _nbuf.clear();
        for (int ii = 0; ii < buffers; ii++) {
            int bufferId = _bufferIds[(_qidx + _qlen) % NUM_BUFFERS];
            if (populateBuffer(bufferId)) {
                _nbuf.put(bufferId);
                _qlen++;
            } else {
                break;
            }
        }
        _nbuf.flip();
        AL10.alSourceQueueBuffers(_sourceId, _nbuf);
    }
    
    /**
     * Populates the identified buffer with as much data as it can hold.
     *
     * @return true if data was read into the buffer and it should be enqueued,
     * false if the end of the stream has been reached and no data was read
     * into the buffer
     */
    protected boolean populateBuffer (int bufferId)
    {
        if (_abuf == null) {
            _abuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        }
        _abuf.clear();
        int read = 0;
        try {
            read = Math.max(populateBuffer(_abuf), 0);
        } catch (IOException e) {
            Log.warning("Error reading audio stream [error=" + e + "].");
        }
        if (read <= 0) {
            return false;
        }
        _abuf.rewind().limit(read);
        AL10.alBufferData(bufferId, getFormat(), _abuf, getFrequency());
        return true;
    }
    
    /**
     * Returns the OpenAL audio format of the stream.
     */
    protected abstract int getFormat ();
    
    /**
     * Returns the stream's playback frequency in samples per second.
     */
    protected abstract int getFrequency ();
    
    /**
     * Populates the given buffer with audio data.
     *
     * @return the total number of bytes read into the buffer, or -1 if the
     * end of the stream has been reached
     */
    protected abstract int populateBuffer (ByteBuffer buf)
        throws IOException;
    
    /** The manager to which the stream was added. */
    protected SoundManager _soundmgr;
    
    /** The source through which the stream plays. */
    protected int _sourceId;
    
    /** The buffers through which we cycle. */
    protected int[] _bufferIds = new int[NUM_BUFFERS];

    /** The starting index and length of the current queue in
     * {@link #_bufferIds}. */
    protected int _qidx, _qlen;
    
    /** The pitch of the stream. */
    protected float _pitch = 1f;
    
    /** The gain of the stream. */
    protected float _gain = 1f;
    
    /** The interval and elapsed time for fading. */
    protected float _fadeInterval, _fadeElapsed;
    
    /** The type of fading being performed. */
    protected FadeMode _fadeMode = FadeMode.NONE;
    
    /** The buffer used to store names. */
    protected IntBuffer _nbuf;
    
    /** The buffer used to store audio data temporarily. */
    protected ByteBuffer _abuf;
    
    /** The OpenAL state of the stream. */
    protected int _state = AL10.AL_INITIAL;
    
    /** The size of the buffers in bytes. */
    protected static final int BUFFER_SIZE = 131072;
    
    /** The number of buffers to use. */
    protected static final int NUM_BUFFERS = 4;
    
    /** Fading modes. */
    protected enum FadeMode { NONE, IN, OUT, OUT_DISPOSE };
}
