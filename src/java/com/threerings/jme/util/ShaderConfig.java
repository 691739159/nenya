//
// $Id: JmeUtil.java 229 2007-05-08 01:50:07Z andrzej $
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

package com.threerings.jme.util;

import java.util.ArrayList;

import com.jme.image.Texture;
import com.jme.light.Light;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

import com.samskivert.util.StringUtil;

/**
 * Tracks the configuration of a shader, which depends on (among other things) a set of
 * {@link RenderState}s.  When the state set changes, the shader must be reconfigured
 * (recompiled or refetched from the {@link ShaderCache}).
 */
public abstract class ShaderConfig
    implements Cloneable
{
    public ShaderConfig (ShaderCache scache)
    {
        _scache = scache;
        _state = DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
    }

    /**
     * Returns a reference to the render state controlled by this configuration.
     */
    public GLSLShaderObjectsState getState ()
    {
        return _state;
    }

    /**
     * Updates the configuration according to the provided state set.  If the configuration has
     * changed, the shader will be recompiled or refetched from the cache in order to reflect the
     * new configuration.
     *
     * @return true if all went all, false if the shader could not be compiled.
     */
    public boolean update (RenderState[] states)
    {
        // update the configurations to determine if we must reconfigure
        if (!updateConfigs(states) && _state.getProgramID() > 0) {
            return true;
        }

        // reconfigure the shader state, generating the derived definitions only if the
        // required configuration isn't in the cache
        String vert = getVertexShader(), frag = getFragmentShader();
        ArrayList<String> defs = new ArrayList<String>();
        getDefinitions(defs);
        String[] darray = defs.toArray(new String[defs.size()]), ddarray = null;
        if (!_scache.isLoaded(vert, frag, darray)) {
            ArrayList<String> ddefs = new ArrayList<String>();
            getDerivedDefinitions(ddefs);
            ddarray = ddefs.toArray(new String[ddefs.size()]);
        }
        return _scache.configureState(_state, vert, frag, darray, ddarray);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        ShaderConfig other = null;
        try {
            other = (ShaderConfig)super.clone();
            other._state =
                DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
            other._state.setProgramID(_state.getProgramID());
            other._state.attribs = _state.attribs;
            if (_lights != null) {
                other._lights = (LightConfig[])_lights.clone();
                for (int ii = 0; ii < _lights.length; ii++) {
                    other._lights[ii] = (LightConfig)_lights[ii].clone();
                }
            }
            if (_textures != null) {
                other._textures = (TextureConfig[])_textures.clone();
                for (int ii = 0; ii < _textures.length; ii++) {
                    other._textures[ii] = (TextureConfig)_textures[ii].clone();
                }
            }
        } catch (CloneNotSupportedException e) {
            // will not happen
        }
        return other;
    }

    /**
     * Updates the component configurations according to the supplied state set, returning
     * <code>true</code> if they have changed and the shader must be reconfigured.
     */
    protected boolean updateConfigs (RenderState[] states)
    {
        // this is one place where we don't want short-circuit evaluation
        boolean lchanged = updateLightConfigs((LightState)states[RenderState.RS_LIGHT]);
        boolean tchanged = updateTextureConfigs((TextureState)states[RenderState.RS_TEXTURE]);
        return lchanged || tchanged;
    }

    /**
     * Updates the light configurations, returning <code>true</code> if they have changed.
     */
    protected boolean updateLightConfigs (LightState lstate)
    {
        if (lstate == null || !lstate.isEnabled()) {
            LightConfig[] olights = _lights;
            _lights = null;
            return (olights != null);
        }
        int lcount = lstate.getQuantity();
        if (_lights == null || _lights.length != lcount) {
            _lights = new LightConfig[lcount];
            for (int ii = 0; ii < lcount; ii++) {
                _lights[ii] = new LightConfig();
                _lights[ii].update(lstate.get(ii));
            }
            return true;
        }
        boolean changed = false;
        for (int ii = 0; ii < lcount; ii++) {
            changed |= _lights[ii].update(lstate.get(ii));
        }
        return changed;
    }

    /**
     * Updates the texture configurations, returning <code>true</code> if they have changed.
     */
    protected boolean updateTextureConfigs (TextureState tstate)
    {
        if (tstate == null || !tstate.isEnabled()) {
            TextureConfig[] otextures = _textures;
            _textures = null;
            return (otextures != null);
        }
        int tcount = tstate.getNumberOfSetTextures();
        if (_textures == null || _textures.length != tcount) {
            _textures = new TextureConfig[tcount];
            for (int ii = 0; ii < tcount; ii++) {
                _textures[ii] = new TextureConfig();
                _textures[ii].update(tstate.getTexture(ii));
            }
            return true;
        }
        boolean changed = false;
        for (int ii = 0; ii < tcount; ii++) {
            changed |= _textures[ii].update(tstate.getTexture(ii));
        }
        return changed;
    }

    /**
     * Returns the resource name of the vertex shader (or <code>null</code> for none).
     */
    protected String getVertexShader ()
    {
        return null;
    }

    /**
     * Returns the resource name of the fragment shader (or <code>null</code> for none).
     */
    protected String getFragmentShader ()
    {
        return null;
    }

    /**
     * Adds the preprocessor definitions that this configuration requires for its shader to the
     * supplied list.
     */
    protected void getDefinitions (ArrayList<String> defs)
    {
        // the distinguishing definitions are just keys
        if (_lights != null) {
            defs.add("LIGHTS " + StringUtil.join(_lights, "/"));
        }
        if (_textures != null) {
            defs.add("TEXTURES " + StringUtil.join(_textures, "/"));
        }
    }

    /**
     * Adds the derived preprocessor definitions that this configuration requires to the supplied
     * list.  The derived definitions are not used to distinguish between cached shaders.
     */
    protected void getDerivedDefinitions (ArrayList<String> ddefs)
    {
        StringBuffer buf = new StringBuffer("SET_FRONT_COLOR ");
        if (_lights != null) {
            buf.append("gl_FrontColor.rgb = gl_FrontLightModelProduct.sceneColor.rgb; ");
            for (int ii = 0; ii < _lights.length; ii++) {
                LightConfig light = _lights[ii];
                if (light.type == -1) {
                    continue;
                }
                if (light.type == Light.LT_POINT) {
                    buf.append("vec3 lvec" + ii + " = gl_LightSource[" + ii +
                        "].position.xyz - eyeVertex;");
                    buf.append("float ldist" + ii + " = length(lvec" + ii + ");");
                }
                buf.append("gl_FrontColor.rgb += (gl_FrontLightProduct[" + ii + "].ambient.rgb + ");
                buf.append("gl_FrontLightProduct[" + ii + "].diffuse.rgb * max(dot(eyeNormal, ");
                if (light.type == Light.LT_POINT) {
                    buf.append("normalize(lvec" + ii + ")), 0.0)) ");
                    buf.append("/ (gl_LightSource[" + ii + "].constantAttenuation + " +
                        "ldist" + ii + " * gl_LightSource[" + ii + "].linearAttenuation + " +
                        "ldist" + ii + " * ldist" + ii + " * gl_LightSource[" + ii +
                            "].quadraticAttenuation);");
                } else {
                    buf.append("gl_LightSource[" + ii + "].position.xyz), 0.0));");
                }
            }
            buf.append("gl_FrontColor.a = gl_FrontMaterial.diffuse.a;");
        } else {
            buf.append("gl_FrontColor = vec4(1.0, 1.0, 1.0, 1.0);");
        }
        ddefs.add(buf.toString());
        buf = new StringBuffer("SET_TEX_COORDS");
        if (_textures != null) {
            for (int ii = 0; ii < _textures.length; ii++) {
                TextureConfig texture = _textures[ii];
                if (texture.envMapMode == -1) {
                    continue;
                }
                buf.append(" gl_TexCoord[" + ii + "] = ");
                if (texture.envMapMode == Texture.EM_SPHERE) {
                    buf.append("vec4(eyeNormal.xy * 0.5 + vec2(0.5, 0.5), 0.0, 1.0);");
                } else {
                    buf.append("gl_MultiTexCoord" + ii + ";");
                }
            }
        }
        ddefs.add(buf.toString());
    }

    /** The configuration of a single light in a {@link LightState}. */
    protected static class LightConfig
        implements Cloneable
    {
        /** The type of light (see {@link Light#getType}). */
        public int type = -1;

        public boolean update (Light light)
        {
            int otype = type;
            type = (light == null) ? -1 : light.getType();
            return (otype != type);
        }

        @Override // documentation inherited
        public Object clone ()
        {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                // will not happen
                return null;
            }
        }

        @Override // documentation inherited
        public String toString ()
        {
            return Integer.toString(type);
        }
    }

    /** The configuration of a single texture in a {@link TextureState}. */
    protected static class TextureConfig
        implements Cloneable
    {
        /** The environment map mode (see {@link Texture#getEnvironmentalMapMode}). */
        public int envMapMode = -1;

        public boolean update (Texture texture)
        {
            int omode = envMapMode;
            envMapMode = (texture == null) ? -1 : texture.getEnvironmentalMapMode();
            return (omode != envMapMode);
        }

        @Override // documentation inherited
        public Object clone ()
        {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                // will not happen
                return null;
            }
        }

        @Override // documentation inherited
        public String toString ()
        {
            return Integer.toString(envMapMode);
        }
    }

    /** The cache used to reconfigure shaders. */
    protected ShaderCache _scache;

    /** The state object to reconfigure. */
    protected GLSLShaderObjectsState _state;

    /** The current light configurations (or <code>null</code> if lighting is disabled). */
    protected LightConfig[] _lights;

    /** The current texture configurations (or <code>null</code> if texturing is disabled). */
    protected TextureConfig[] _textures;
}