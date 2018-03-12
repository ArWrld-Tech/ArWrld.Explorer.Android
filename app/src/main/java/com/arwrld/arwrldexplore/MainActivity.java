package com.arwrld.arwrldexplore;

import android.Manifest;
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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.arwrld.arwrldexplore.ar.ArOverlayView;
import com.arwrld.arwrldexplore.ar.ArPoint;
import com.arwrld.arwrldexplore.fragments.ArNearbyFragment;
import com.arwrld.arwrldexplore.fragments.ArWrldViewerFragment;
import com.arwrld.arwrldexplore.fragments.PhaseOnePropsFragment;
import com.arwrld.arwrldexplore.fragments.PropertiesFragment;
import com.arwrld.arwrldexplore.location.LocationApi;
import com.arwrld.arwrldexplore.models.Geosearch;
import com.arwrld.arwrldexplore.models.WikiResponse;
import com.arwrld.arwrldexplore.utils.Constants;
import com.arwrld.arwrldexplore.utils.MapCircleTransform;
import com.arwrld.arwrldexplore.utils.Utils;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.cameraview.CameraView;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.ArrayList;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private BottomBar bottomBar;
    private Context mContext;

    private FragmentTransaction fragmentTransaction;
    private FragmentManager fragmentManager;

    private ArNearbyFragment arNearbyFragment;

    private static final String extraVal = "ExplorerAndroid";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomBar = findViewById(R.id.bottomBar);
        mContext = this;

        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        fragmentTransaction.disallowAddToBackStack();

        MainActivityPermissionsDispatcher.showCameraWithPermissionCheck(MainActivity.this);
    }

    private void setupFragments(){
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int tabId) {
                if(tabId == R.id.tab_nearby){
                    getSupportActionBar().setTitle("Nearby Explorer");
                    arNearbyFragment = ArNearbyFragment.newInstance();
                    swapFragment(arNearbyFragment);
                }

                if(tabId == R.id.tab_properties){
                    getSupportActionBar().setTitle("Properties Estimator");
                    swapFragment(PropertiesFragment.newInstance());
                }

                if(tabId == R.id.tab_phase_one){
                    getSupportActionBar().setTitle("Phase 1 Properties");
                    swapFragment(PhaseOnePropsFragment.newInstance());
                }

                if(tabId == R.id.tab_arwrld){
                    getSupportActionBar().setTitle("ArWrld Explorer");
                    swapFragment(ArWrldViewerFragment.newInstance("http://arwrld.com" + "?ref=" + extraVal + "?utm_source=" + extraVal + "?from=" + extraVal));
                }
            }
        });
    }

    private void swapFragment(Fragment fragment) {
        try {
            fragmentManager.popBackStack();
            fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_container, fragment);
            fragmentTransaction.commit();
        } catch (IllegalStateException e) {
            Log.e("ArWrld", "SwapFragment: " + e.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Answers.getInstance().logContentView(new ContentViewEvent().putContentName("Main Activity"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void processTouchEvent(final int objectId) {
        if(arNearbyFragment != null) {
            arNearbyFragment.processTouchEvent(objectId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.
                onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void showCamera() {
        setupFragments();
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void showRationaleForCamera(final PermissionRequest request) {

    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void showDeniedForCamera() {
        Toast.makeText(mContext, "Augmented Reality features require camera permissions!", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void showNeverAskForCamera() {
        Toast.makeText(mContext, "Augmented Reality features require camera permissions!", Toast.LENGTH_SHORT).show();
    }
}
