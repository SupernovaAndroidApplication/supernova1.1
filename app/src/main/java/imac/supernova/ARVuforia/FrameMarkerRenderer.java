/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package imac.supernova.ARVuforia;

import java.nio.Buffer;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.qualcomm.vuforia.Marker;
import com.qualcomm.vuforia.MarkerResult;
import com.qualcomm.vuforia.MarkerTracker;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vuforia;

import imac.supernova.ARVuforia.dataobjects.bohregon.FighterBohregon;
import imac.supernova.ARVuforia.utils.CubeShaders;
import imac.supernova.ARVuforia.utils.ObjectType;
import imac.supernova.ARVuforia.utils.PlaneObject;
import imac.supernova.ARVuforia.utils.SampleUtils;
import imac.supernova.ARVuforia.utils.Texture;


// The renderer class for the FrameMarkers sample. 
public class FrameMarkerRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "FrameMarkerRenderer";
    
    SampleApplicationSession vuforiaAppSession;
    FrameMarkers mActivity;
    
    public boolean mIsActive = false;
    
    private Vector<Texture> mTextures;

    private double prevTime;
    private float rotateAngle;
    
    // OpenGL ES 2.0 specific:
    private int shaderProgramID = 0;
    private int vertexHandle = 0;
    private int normalHandle = 0;
    private int textureCoordHandle = 0;
    private int mvpMatrixHandle = 0;
    private int texSampler2DHandle = 0;
    
    // Constants:
    static private float shipScale = 1.0f;
    static private float kLetterTranslate = 0.0f;
    static private float lifeHUDScale = 30.0f;

    private FighterBohregon fighterBohregon = new FighterBohregon();
    private PlaneObject plane = new PlaneObject();

    public FrameMarkerRenderer(FrameMarkers activity,SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
    }

    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call function to initialize rendering:
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
    }

    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }

    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;
        
        // Call our function to render content
        renderFrame();
    }

    void initRendering()
    {
        Log.d(LOGTAG, "initRendering");
        
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);
        
        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");
    }

    void renderFrame()
    {
        // Clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // Get the state from Vuforia and mark the beginning of a rendering section
        State state = Renderer.getInstance().begin();
        
        // Explicitly render the Video Background
        Renderer.getInstance().drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
        // We must detect if background reflection is active and adjust the culling direction.
        // If the reflection is active, this means the post matrix has been reflected as well,
        // therefore standard counter clockwise face culling will result in "inside out" models.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW);  // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW);   // Back camera
            
        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);
            float[] modelViewMatrixShip = Tool.convertPose2GLMatrix(trackableResult.getPose()).getData();
            float[] modelViewMatrixLife = Tool.convertPose2GLMatrix(trackableResult.getPose()).getData();
            
            // Choose the texture based on the target name:
            int textureIndex = 0;
            
            // Check the type of the trackable:
            assert (trackableResult.getType() == MarkerTracker.getClassType());
            MarkerResult markerResult = (MarkerResult) (trackableResult);
            Marker marker = (Marker) markerResult.getTrackable();

            // TODO retrouver facilement un marker avec un modele
            textureIndex = marker.getMarkerId();
            
            assert (textureIndex < mTextures.size());
            Texture shipTexture;
            Texture lifeTexture;
            
            // Select which model to draw:
            Buffer verticesShip = null;
            Buffer normalsShip = null;
            Buffer texCoordsShip = null;
            int numVertObjectShip = 0;

            Buffer verticesLife = null;
            Buffer normalsLife = null;
            Buffer texCoordsLife = null;
            int numVertObjectLife = 0;

            switch (marker.getMarkerId())
            {
                case ObjectType.BOHREGON_FIGHTER:
                    shipTexture =  mTextures.get(ObjectType.BOHREGON_FIGHTER);
                    lifeTexture = mTextures.get(1); //TODO communiquer avec le modele pour savoir combien de vie restante / vie totale

                    verticesShip = fighterBohregon.getVertices();
                    normalsShip = fighterBohregon.getNormals();
                    texCoordsShip = fighterBohregon.getTexCoords();
                    numVertObjectShip = fighterBohregon.getNumObjectVertex();

                    verticesLife = plane.getVertices();
                    normalsLife = plane.getNormals();
                    texCoordsLife = plane.getTexCoords();
                    numVertObjectLife = plane.getNumObjectVertex();
                    break;
                case ObjectType.BOHREGONG_CRUISER:
                    shipTexture =  mTextures.get(ObjectType.BOHREGONG_CRUISER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.BOHREGON_BOMBER:
                    shipTexture =  mTextures.get(ObjectType.BOHREGON_BOMBER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.TERRAN_FIGHTER:
                    shipTexture =  mTextures.get(ObjectType.TERRAN_FIGHTER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.TERRAN_CRUISER:
                    shipTexture =  mTextures.get(ObjectType.TERRAN_CRUISER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.TERRAN_BOMBER:
                    shipTexture =  mTextures.get(ObjectType.TERRAN_BOMBER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.NERENIDE_FIGHTER:
                    shipTexture =  mTextures.get(ObjectType.NERENIDE_FIGHTER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.NERENIDE_CRUISER:
                    shipTexture =  mTextures.get(ObjectType.NERENIDE_CRUISER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.NERENIDE_BOMBER:
                    shipTexture =  mTextures.get(ObjectType.NERENIDE_BOMBER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.YTTRIKT_FIGHTER:
                    shipTexture =  mTextures.get(ObjectType.YTTRIKT_FIGHTER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.YTTRIKT_CRUISER:
                    shipTexture =  mTextures.get(ObjectType.YTTRIKT_CRUISER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.YTTRIKT_BOMBER:
                    shipTexture =  mTextures.get(ObjectType.YTTRIKT_BOMBER);
                    lifeTexture = mTextures.get(1);
                    break;
                case ObjectType.ASTEROID:
                    shipTexture =  mTextures.get(ObjectType.ASTEROID);
                    lifeTexture = mTextures.get(1);
                    break;
                default:
                    shipTexture =  mTextures.get(0);
                    lifeTexture = mTextures.get(1);

                    verticesShip = fighterBohregon.getVertices();
                    normalsShip = fighterBohregon.getNormals();
                    texCoordsShip = fighterBohregon.getTexCoords();
                    numVertObjectShip = fighterBohregon.getNumObjectVertex();

                    verticesLife = plane.getVertices();
                    normalsLife = plane.getNormals();
                    texCoordsLife = plane.getTexCoords();
                    numVertObjectLife = plane.getNumObjectVertex();
                    break;
            }
            
            float[] modelViewProjectionShip = new float[16];
            float[] modelViewProjectionLife = new float[16];

            Matrix.translateM(modelViewMatrixShip, 0, 0, 0, 30);
            Matrix.scaleM(modelViewMatrixShip, 0, shipScale, shipScale, shipScale);
            Matrix.multiplyMM(modelViewProjectionShip, 0, vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixShip, 0);

            GLES20.glUseProgram(shaderProgramID);

            /** Ship */
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, verticesShip);
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalsShip);
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordsShip);
            GLES10.glTexCoordPointer(2, GLES10.GL_FLOAT, 0, texCoordsShip);

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shipTexture.mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionShip, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertObjectShip);

            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            /** Life */
            if(marker.getMarkerId() != ObjectType.ASTEROID)
            {
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                animateLifeHUD(modelViewMatrixLife);

                Matrix.translateM(modelViewMatrixLife, 0, 0, 0, 25);
//                Matrix.rotateM(modelViewMatrixLife, 0, 0, 0, 0, 0);
                Matrix.scaleM(modelViewMatrixLife, 0, lifeHUDScale, lifeHUDScale, lifeHUDScale);
                Matrix.multiplyMM(modelViewProjectionLife, 0, vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixLife, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, verticesLife);
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalsLife);
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordsLife);
                GLES10.glTexCoordPointer(2, GLES10.GL_FLOAT, 0, texCoordsLife);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lifeTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionLife, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertObjectLife);

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                GLES20.glDisable(GLES20.GL_BLEND);
            }
            SampleUtils.checkGLError("FrameMarkers render frame");
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        Renderer.getInstance().end();
    }

    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }

    private void animateLifeHUD(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis(); // Get real time difference
        float dt = (float) (time - prevTime) / 1000; // from frame to frame

        rotateAngle += dt * 0.25f * 180.0f / 3.1415f; // Animate angle based on time
        rotateAngle %= 360;
        Log.d(LOGTAG, "Delta animation time: " + rotateAngle);

        Matrix.rotateM(modelViewMatrix, 0, rotateAngle, 0.0f, 0.0f, 1.0f);

        prevTime = time;
    }
}
