package com.arwrld.arwrldexplore.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.arwrld.arwrldexplore.MainActivity;
import com.arwrld.arwrldexplore.R;
import com.arwrld.arwrldexplore.ar.ArOverlayView;
import com.arwrld.arwrldexplore.ar.ArPoint;
import com.arwrld.arwrldexplore.location.LocationApi;
import com.arwrld.arwrldexplore.models.Geosearch;
import com.arwrld.arwrldexplore.models.WikiResponse;
import com.arwrld.arwrldexplore.utils.Constants;
import com.arwrld.arwrldexplore.utils.MapCircleTransform;
import com.arwrld.arwrldexplore.utils.Utils;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.cameraview.CameraView;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

import io.nlopez.smartlocation.OnLocationUpdatedListener;

public class ArNearbyFragment extends BaseFragment implements SensorEventListener {

    View view;
    CameraView cameraView;
    private SurfaceView surfaceView;
    private FrameLayout cameraContainerLayout;

    private Context mContext;
    private ArOverlayView arOverlayView;
    private Camera camera;

    private SensorManager sensorManager;

    private Location lastLocation;
    private boolean haveFetched = false;

    float[] projectionMatrix = new float[16];
    private final static float Z_NEAR = 0.5f;
    private final static float Z_FAR = 2000;

    ArrayList<ArPoint> arPoints = new ArrayList<>();

    private RequestOptions requestOptions;
    private int attrSize = 0;

    public static ArNearbyFragment newInstance() {
        return new ArNearbyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        arOverlayView = new ArOverlayView(mContext, ((MainActivity)getActivity()));
        sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        requestOptions = new RequestOptions();
        requestOptions.apply(RequestOptions.centerCropTransform());
        requestOptions.transform(new MapCircleTransform(mContext));
        requestOptions.priority(Priority.HIGH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_ar_nearby, container, false);
        cameraView = view.findViewById(R.id.camera);
        surfaceView = view.findViewById(R.id.surface_view);
        cameraContainerLayout = view.findViewById(R.id.camera_container_layout);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        cameraView.start();
        registerSensors();
        initAROverlayView();

        LocationApi.setUpLocationUpdates(mContext,
                new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        generateProjectionMatrix();
                        location.setBearing(location.getBearing());
                        arOverlayView.updateCurrentLocation(location);

                        lastLocation = location;

                        if (!haveFetched) {
                            haveFetched = true;
                            load();
                        }
                    }
                });

        Answers.getInstance().logContentView(new ContentViewEvent().putContentName("AR Nearby"));
    }

    @Override
    public void onDestroy() {
        releaseCamera();
        LocationApi.killAllUpdateListeners(mContext);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        LocationApi.killAllUpdateListeners(mContext);
        super.onPause();
        releaseCamera();
    }

    private void load() {
        String url = "https://en.wikipedia.org/w/api.php?action=query&list=geosearch&format=json&gscoord="
                + lastLocation.getLatitude() + "%7C" + lastLocation.getLongitude() + "&gsradius=10000&gslimit=20";

        Ion.with(mContext).load(url)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (e != null) {
                            Toast.makeText(mContext, "Error loading tweets", Toast.LENGTH_LONG).show();
                            return;
                        }
                        WikiResponse wikiResponse = Constants.gson.fromJson(result, WikiResponse.class);
                        attrSize = wikiResponse.getQuery().getGeosearch().size();

                        for (int i = 0; i < attrSize; i++) {
                            try {
                                final int index = i;
                                final Geosearch status = wikiResponse.getQuery().getGeosearch().get(index);
                                int marker = R.mipmap.ic_arwrld;
                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), marker);
                                bitmap = Bitmap.createScaledBitmap(bitmap, Utils.getSizeFromDistance(mContext, status.getDist()),
                                        Utils.getSizeFromDistance(mContext, status.getDist()), false);
                                arPoints.add(new ArPoint(status, bitmap));
                                arOverlayView.setDataPoints(arPoints);
                            }catch (IllegalStateException e1){
                                Crashlytics.logException(e1);
                            }
                        }

                    }
                });
    }

    public void initAROverlayView() {
        if (arOverlayView.getParent() != null) {
            ((ViewGroup) arOverlayView.getParent()).removeView(arOverlayView);
        }
        cameraContainerLayout.addView(arOverlayView);
    }

    private void releaseCamera() {
        cameraView.stop();
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void registerSensors() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrixFromVector = new float[16];
            float[] rotatedProjectionMatrix = new float[16];

            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, sensorEvent.values);

            Matrix.multiplyMM(rotatedProjectionMatrix, 0, this.projectionMatrix, 0, rotationMatrixFromVector, 0);
            this.arOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //do nothing
    }

    private void generateProjectionMatrix() {
        float ratio = cameraView.getAspectRatio().getX() / cameraView.getAspectRatio().getY();
        final int OFFSET = 0;
        final float LEFT = -ratio;
        final float RIGHT = ratio;
        final float BOTTOM = -1;
        final float TOP = 1;
        Matrix.frustumM(projectionMatrix, OFFSET, LEFT, RIGHT, BOTTOM, TOP, Z_NEAR, Z_FAR);
    }

    public void processTouchEvent(final int objectId) {
        String url = "https://en.wikipedia.org/?curid=" + objectId;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        mContext.startActivity(i);
    }
}
