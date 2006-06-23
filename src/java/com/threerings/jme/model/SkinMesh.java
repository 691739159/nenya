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

package com.threerings.jme.model;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.jme.bounding.BoundingVolume;
import com.jme.math.Matrix4f;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.SharedBatch;
import com.jme.scene.batch.TriangleBatch;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.HashIntMap;

import com.threerings.jme.Log;

/**
 * A triangle mesh that deforms according to a bone hierarchy.
 */
public class SkinMesh extends ModelMesh
{
    /** Represents the vertex weights of a group of vertices influenced by the
     * same set of bones. */
    public static class WeightGroup
        implements Serializable
    {
        /** The number of vertices in this weight group. */
        public int vertexCount;
        
        /** The bones influencing this group. */
        public Bone[] bones;
        
        /** The array of interleaved weights (of length <code>vertexCount *
         * boneIndices.length</code>): weights for first vertex, weights for
         * second, etc. */
        public float[] weights;

        /**
         * Rebinds this weight group for a prototype instance.
         *
         * @param bmap the mapping from prototype to instance bones
         */
        public WeightGroup rebind (HashMap<Bone, Bone> bmap)
        {
            WeightGroup wgroup = new WeightGroup();
            wgroup.vertexCount = vertexCount;
            wgroup.bones = new Bone[bones.length];
            for (int ii = 0; ii < bones.length; ii++) {
                wgroup.bones[ii] = bmap.get(bones[ii]);
            }
            wgroup.weights = weights;
            return wgroup;
        }
        
        private static final long serialVersionUID = 1;
    }

    /** Represents a bone that influences the mesh. */
    public static class Bone
        implements Serializable
    {
        /** The node that defines the bone's position. */
        public ModelNode node;
        
        /** The inverse of the bone's model space reference transform. */
        public transient Matrix4f invRefTransform;
        
        /** The bone's current transform in model space. */
        public transient Matrix4f transform;
        
        public Bone (ModelNode node)
        {
            this.node = node;
            transform = new Matrix4f();
        }
        
        /**
         * Rebinds this bone for a prototype instance.
         *
         * @param pnodes a mapping from prototype nodes to instance nodes
         */
        public Bone rebind (HashMap pnodes)
        {
            Bone bone = new Bone((ModelNode)pnodes.get(node));
            bone.invRefTransform = invRefTransform;
            bone.transform = new Matrix4f();
            return bone;
        }
        
        /**
         * Initializes the bone's transient state.
         */
        private void readObject (ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            in.defaultReadObject();
            transform = new Matrix4f();
        }
     
        private static final long serialVersionUID = 1;
    }
    
    /**
     * No-arg constructor for deserialization.
     */
    public SkinMesh ()
    {
    }
    
    /**
     * Creates an empty mesh.
     */
    public SkinMesh (String name)
    {
        super(name);
    }
    
    /**
     * Sets the array of weight groups that determine how bones affect
     * each vertex.
     */
    public void setWeightGroups (WeightGroup[] weightGroups)
    {
        _weightGroups = weightGroups;
        
        // compile a list of all referenced bones
        HashSet<Bone> bones = new HashSet<Bone>();
        for (WeightGroup group : weightGroups) {
            Collections.addAll(bones, group.bones);
        }
        _bones = bones.toArray(new Bone[bones.size()]);
    }
    
    @Override // documentation inherited
    public void reconstruct (
        ByteBuffer vertices, ByteBuffer normals, ByteBuffer colors,
        ByteBuffer textures, ByteBuffer indices)
    {
        super.reconstruct(vertices, normals, colors, textures, indices);
        
        // store the current buffers as the originals
        storeOriginalBuffers();
        
        // initialize the quantized frame table
        _frames = new HashIntMap<Object>();
    }
    
    @Override // documentation inherited
    public void centerVertices ()
    {
        super.centerVertices();
        storeOriginalBuffers();
    }
    
    @Override // documentation inherited
    public Spatial putClone (Spatial store, Model.CloneCreator properties)
    {
        SkinMesh mstore = (SkinMesh)properties.originalToCopy.get(this);
        if (mstore != null) {
            return mstore;
        } else if (store == null) {
            mstore = new SkinMesh(getName());
        } else {
            mstore = (SkinMesh)store;
        }
        properties.removeProperty("vertices");
        properties.removeProperty("normals");
        properties.removeProperty("displaylistid");
        super.putClone(mstore, properties);
        properties.addProperty("vertices");
        properties.addProperty("normals");
        properties.addProperty("displaylistid");
        mstore._frames = _frames;
        mstore._useDisplayLists = _useDisplayLists;
        mstore._invRefTransform = _invRefTransform;
        mstore._bones = new Bone[_bones.length];
        HashMap<Bone, Bone> bmap = new HashMap<Bone, Bone>();
        for (int ii = 0; ii < _bones.length; ii++) {
            bmap.put(_bones[ii], mstore._bones[ii] =
                _bones[ii].rebind(properties.originalToCopy));
        }
        mstore._weightGroups = new WeightGroup[_weightGroups.length];
        for (int ii = 0; ii < _weightGroups.length; ii++) {
            mstore._weightGroups[ii] = _weightGroups[ii].rebind(bmap);
        }
        mstore._ovbuf = _ovbuf;
        mstore._onbuf = _onbuf;
        mstore._vbuf = new float[_vbuf.length];
        mstore._nbuf = new float[_nbuf.length];
        return mstore;
    }
    
    @Override // documentation inherited
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeObject(_weightGroups);
    }
    
    @Override // documentation inherited
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        setWeightGroups((WeightGroup[])in.readObject());
    }
    
    @Override // documentation inherited
    public void expandModelBounds ()
    {
        BoundingVolume obound =
            (BoundingVolume)getBatch(0).getModelBound().clone(null);
        updateModelBound();
        getBatch(0).getModelBound().mergeLocal(obound);
    }
    
    @Override // documentation inherited
    public void setReferenceTransforms ()
    {
        _invRefTransform = new Matrix4f();
        if (parent instanceof ModelNode) {
            Matrix4f transform = new Matrix4f();
            ModelNode.setTransform(getLocalTranslation(), getLocalRotation(),
                getLocalScale(), transform);
            ((ModelNode)parent).getModelTransform().mult(transform,
                _invRefTransform);
            _invRefTransform.invertLocal();
        }
        for (Bone bone : _bones) {
            bone.invRefTransform =
                _invRefTransform.mult(bone.node.getModelTransform()).invert();
        }
    }
    
    @Override // documentation inherited
    public void lockStaticMeshes (
        Renderer renderer, boolean useVBOs, boolean useDisplayLists)
    {
        // we can use VBOs for color, texture, and indices
        if (useVBOs && renderer.supportsVBO()) {
            VBOInfo vboinfo = new VBOInfo(false);
            vboinfo.setVBOColorEnabled(true);
            vboinfo.setVBOTextureEnabled(true);
            vboinfo.setVBOIndexEnabled(true);
            setVBOInfo(vboinfo);
        }
        _useDisplayLists = useDisplayLists;
    }
    
    @Override // documentation inherited
    public void storeMeshFrame (int frameId, boolean blend)
    {
        _storeFrameId = frameId;
        _storeBlend = blend;
    }
    
    @Override // documentation inherited
    public void setMeshFrame (int frameId)
    {
        TriangleBatch batch = getBatch(0),
            tbatch = (TriangleBatch)_frames.get(frameId);
        if (batch instanceof SharedBatch) {
            ((SharedBatch)batch).setTarget(tbatch);
        } else {
            clearBatches();
            addBatch(new SharedBatch(tbatch));
            getBatch(0).updateRenderState();
        }
    }
    
    @Override // documentation inherited
    public void blendMeshFrames (int frameId1, int frameId2, float alpha)
    {
        BlendFrame frame1 = (BlendFrame)_frames.get(frameId1),
            frame2 = (BlendFrame)_frames.get(frameId2);
        frame1.blend(frame2, alpha, _vbuf, _nbuf);
        FloatBuffer vbuf = getVertexBuffer(0), nbuf = getNormalBuffer(0);
        vbuf.rewind();
        vbuf.put(_vbuf);
        nbuf.rewind();
        nbuf.put(_nbuf);
    }
    
    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);
        if (_weightGroups == null || _storeFrameId == -1) {
            return;
        }
        // update the bone transforms
        for (Bone bone : _bones) {
            _invRefTransform.mult(bone.node.getModelTransform(),
                bone.transform);
            bone.transform.multLocal(bone.invRefTransform);
        }
        
        // deform the mesh according to the positions of the bones (this code
        // is ugly as sin because it's optimized at a low level)
        Bone[] bones;
        int vertexCount, jj, kk, ww;
        float[] weights;
        Matrix4f m;
        float weight, ovx, ovy, ovz, onx, ony, onz, vx, vy, vz, nx, ny, nz;
        for (int ii = 0, bidx = 0; ii < _weightGroups.length; ii++) {
            vertexCount = _weightGroups[ii].vertexCount;
            bones = _weightGroups[ii].bones;
            weights = _weightGroups[ii].weights;
            for (jj = 0, ww = 0; jj < vertexCount; jj++) {
                ovx = _ovbuf[bidx];
                ovy = _ovbuf[bidx + 1];
                ovz = _ovbuf[bidx + 2];
                onx = _onbuf[bidx];
                ony = _onbuf[bidx + 1];
                onz = _onbuf[bidx + 2];
                vx = vy = vz = 0f;
                nx = ny = nz = 0f;
                for (kk = 0; kk < bones.length; kk++) {
                    m = bones[kk].transform;
                    weight = weights[ww++];
                    
                    vx += (ovx*m.m00 + ovy*m.m01 + ovz*m.m02 + m.m03) * weight;
                    vy += (ovx*m.m10 + ovy*m.m11 + ovz*m.m12 + m.m13) * weight;
                    vz += (ovx*m.m20 + ovy*m.m21 + ovz*m.m22 + m.m23) * weight;
                    
                    nx += (onx*m.m00 + ony*m.m01 + onz*m.m02) * weight;
                    ny += (onx*m.m10 + ony*m.m11 + onz*m.m12) * weight;
                    nz += (onx*m.m20 + ony*m.m21 + onz*m.m22) * weight;
                }
                _vbuf[bidx] = vx;
                _vbuf[bidx + 1] = vy;
                _vbuf[bidx + 2] = vz;
                _nbuf[bidx++] = nx;
                _nbuf[bidx++] = ny;
                _nbuf[bidx++] = nz;
            }
        }
        
        // if skinning in real time, copy the data from arrays to buffers;
        // otherwise, store the mesh as an animation frame
        if (_storeFrameId == 0) {
            FloatBuffer vbuf = getVertexBuffer(0), nbuf = getNormalBuffer(0);
            vbuf.rewind();
            vbuf.put(_vbuf);
            nbuf.rewind();
            nbuf.put(_nbuf);
        } else {
            storeFrame();
            _storeFrameId = -1;
        }
    }
    
    /**
     * Stores the current frame data for later use.
     */
    protected void storeFrame ()
    {
        if (_storeBlend) {
            _frames.put(_storeFrameId, new BlendFrame(
                (float[])_vbuf.clone(), (float[])_nbuf.clone()));
        } else {
            TriangleBatch batch = getBatch(0), tbatch = new TriangleBatch();
            tbatch.setParentGeom(DUMMY_MESH);
            tbatch.setColorBuffer(batch.getColorBuffer());
            tbatch.setTextureBuffer(batch.getTextureBuffer(0), 0);
            tbatch.setIndexBuffer(batch.getIndexBuffer());
            tbatch.setVertexBuffer(BufferUtils.createFloatBuffer(_vbuf));
            tbatch.setNormalBuffer(BufferUtils.createFloatBuffer(_nbuf));
            VBOInfo ovboinfo = batch.getVBOInfo();
            if (ovboinfo != null) {
                VBOInfo vboinfo = new VBOInfo(true);
                vboinfo.setVBOIndexEnabled(true);
                vboinfo.setVBOColorID(ovboinfo.getVBOColorID());
                vboinfo.setVBOTextureID(0, ovboinfo.getVBOTextureID(0));
                vboinfo.setVBOIndexID(ovboinfo.getVBOIndexID());
                tbatch.setVBOInfo(vboinfo);
            } else if (_useDisplayLists) {
                tbatch.lockMeshes(
                    DisplaySystem.getDisplaySystem().getRenderer());
            }
            _frames.put(_storeFrameId, tbatch);
        }
    }
    
    /**
     * Stores the current vertex and normal buffers for later deformation.
     */
    protected void storeOriginalBuffers ()
    {
        FloatBuffer vbuf = getVertexBuffer(0), nbuf = getNormalBuffer(0);
        vbuf.rewind();
        nbuf.rewind();
        FloatBuffer.wrap(_ovbuf = new float[vbuf.capacity()]).put(vbuf);
        FloatBuffer.wrap(_onbuf = new float[nbuf.capacity()]).put(nbuf);
        _vbuf = new float[_ovbuf.length];
        _nbuf = new float[_onbuf.length];
    }
    
    /** A stored frame used for linear blending. */
    protected static class BlendFrame
    {
        /** The skinned vertex and normal values. */
        public float[] vbuf, nbuf;
        
        public BlendFrame (float[] vbuf, float[] nbuf)
        {
            this.vbuf = vbuf;
            this.nbuf = nbuf;
        }
        
        public void blend (
            BlendFrame next, float alpha, float[] rvbuf, float[] rnbuf)
        {
            float[] nvbuf = next.vbuf, nnbuf = next.nbuf;
            float ialpha = 1f - alpha;
            for (int ii = 0, nn = vbuf.length; ii < nn; ii++) {
                rvbuf[ii] = vbuf[ii] * ialpha + nvbuf[ii] * alpha;
                rnbuf[ii] = nbuf[ii] * ialpha + nnbuf[ii] * alpha;
            }
        }
    } 
    
    /** Pre-skinned {@link TriangleBatch}es or {@link BlendFrame}s shared
     * between all instances corresponding to frame ids from
     * {@link #storeAnimationFrame}. */
    protected HashIntMap<Object> _frames;
    
    /** Whether or to use display lists if VBOs are unavailable for quantized
     * meshes. */
    protected boolean _useDisplayLists;
    
    /** The inverse of the model space reference transform. */
    protected Matrix4f _invRefTransform;
    
    /** The groups of vertices influenced by different sets of bones. */
    protected WeightGroup[] _weightGroups;
    
    /** The bones referenced by the weight groups. */
    protected Bone[] _bones;
    
    /** The original (undeformed) vertex and normal buffers and the deformed
     * versions. */
    protected float[] _ovbuf, _onbuf, _vbuf, _nbuf;

    /** The frame id to store on the next update.  If 0, don't store any frame
     * and skin the mesh as normal.  If -1, a frame has been stored and thus
     * skinning should only take place when further frames are requested. */
    protected int _storeFrameId;
    
    /** Whether or not the stored frame id will be used for blending. */
    protected boolean _storeBlend;
    
    /** A dummy mesh that simply hold transformation values. */
    protected static final TriMesh DUMMY_MESH = new TriMesh();
    
    private static final long serialVersionUID = 1;
}
