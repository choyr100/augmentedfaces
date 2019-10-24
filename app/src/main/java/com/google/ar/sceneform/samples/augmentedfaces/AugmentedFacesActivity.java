/*
 * Copyright 2019 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.augmentedfaces;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.AugmentedFaceNode;
import com.google.ar.sceneform.ux.TransformableNode;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is an example activity that uses the Sceneform UX package to make common Augmented Faces
 * tasks easier.
 */
public class AugmentedFacesActivity extends AppCompatActivity {
    private static final String TAG = AugmentedFacesActivity.class.getSimpleName();

    private static final double MIN_OPENGL_VERSION = 3.0;

    private FaceArFragment arFragment;

    private ModelRenderable faceRegionsRenderable;
    private Texture faceMeshTexture;

    private TextView textView;

    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();

    private ModelRenderable headRegionsRenderable;
    private ModelRenderable graduationCapRegionsRenderable;

    private boolean isCreate;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_face_mesh);
        arFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.face_fragment);

        textView = (TextView) findViewById(R.id.textview1);
        textView.setText("hello");
        isCreate = false;
        // Load the face regions renderable.
        // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
        ModelRenderable.builder()
                .setSource(this, R.raw.fox_face)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            faceRegionsRenderable = modelRenderable;
//              faceRegionsRenderable = ShapeFactory.makeCube(
//                      new Vector3(.01f, .01f, )
//              )
                            modelRenderable.setShadowCaster(false);
                            modelRenderable.setShadowReceiver(false);
                        });
        ModelRenderable.builder().setSource(this, R.raw.base)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            headRegionsRenderable = modelRenderable;
                        }
                );
        ModelRenderable.builder().setSource(this, R.raw.graduationcap)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            graduationCapRegionsRenderable = modelRenderable;
                        }
                );

        // Load the face mesh texture.
        Texture.builder()
                .setSource(this, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept(texture -> faceMeshTexture = texture);

        ArSceneView sceneView = arFragment.getArSceneView();

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        sceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

        Scene scene = sceneView.getScene();

        scene.addOnUpdateListener(
                (FrameTime frameTime) -> {
                    if (faceRegionsRenderable == null || graduationCapRegionsRenderable == null || headRegionsRenderable == null) {
                        return;
                    }

                    Collection<AugmentedFace> faceList =
                            sceneView.getSession().getAllTrackables(AugmentedFace.class);


                    // Make new AugmentedFaceNodes for any new faces.
                    for (AugmentedFace face : faceList) {
                        if (!faceNodeMap.containsKey(face)) {
                            AugmentedFaceNode faceNode = new AugmentedFaceNode(face);
                            faceNode.setParent(scene);
                            faceNode.setFaceRegionsRenderable(faceRegionsRenderable);
                            faceNode.setFaceMeshTexture(faceMeshTexture);

                            AugmentedFaceNode node = new AugmentedFaceNode(face);
                            node.setParent(scene);
                            node.setRenderable(headRegionsRenderable);
                            node.setLocalScale(new Vector3(0.1f,0.1f,0.1f));
                            node.setName("head");

                            AugmentedFaceNode capNode = new AugmentedFaceNode(face);
                            capNode.setParent(scene);
                            capNode.setRenderable(graduationCapRegionsRenderable);
                            capNode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));

//                            MaterialFactory.makeTransparentWithColor(getApplicationContext(), new Color(244, 244, 244))
//                                    .thenAccept(
//                                            material -> {
//                                                Vector3 vector3 = new Vector3(0.05f, 0.05f, 0.05f);
//                                                ModelRenderable model = ShapeFactory.makeCube(vector3,
//                                                        Vector3.zero(), material);
//                                                model.setShadowCaster(false);
//                                                model.setShadowReceiver(false);
//
//                                                AugmentedFaceNode transformableNode = new AugmentedFaceNode(face);
//                                                transformableNode.setParent(scene);
//                                                transformableNode.setRenderable(model);
//                                            }
//                                    );


//              FloatBuffer fb = face.getMeshVertices().asReadOnlyBuffer();
//              ShortBuffer sb = face.getMeshTriangleIndices().asReadOnlyBuffer();
//              float[] points;
//              short[] indices;
//              if(fb.hasArray()){
//                points = fb.array();
//              }
//              else {
//                points = new float[fb.limit()];
//                fb.get(points);
//              }
//              if(sb.hasArray()){
//                indices = sb.array();
//              }
//              else {
//                indices = new short[sb.limit()];
//                sb.get(indices);
//              }
//              int pointsize=points.length;
//              int indciesize=indices.length;
                            //textView.setText(Float.toString(points[42])+"\n"+Float.toString(points[43])+"\n"+Float.toString(points[44])+"\n"+Float.toString(indices[2]));
                            faceNodeMap.put(face, faceNode);
                            faceNodeMap.put(face, node);
                            faceNodeMap.put(face, capNode);
                        }
                    }

                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter =
                            faceNodeMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
                        AugmentedFace face = entry.getKey();
                        if (face.getTrackingState() == TrackingState.STOPPED) {
                            AugmentedFaceNode faceNode = entry.getValue();
                            Log.i("head",faceNode.getName());
                            faceNode.setParent(null);
                            iter.remove();
                        }
                        else if (face.getTrackingState() == TrackingState.PAUSED) {
                            AugmentedFaceNode faceNode = entry.getValue();
                            Log.i("head",faceNode.getName());
                            faceNode.setParent(null);
                            iter.remove();
                        }
                    }
                });
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (ArCoreApk.getInstance().checkAvailability(activity)
                == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "Augmented Faces requires ARCore.");
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
