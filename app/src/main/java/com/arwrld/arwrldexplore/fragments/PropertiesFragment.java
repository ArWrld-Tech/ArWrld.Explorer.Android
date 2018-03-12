package com.arwrld.arwrldexplore.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arwrld.arwrldexplore.R;
import com.arwrld.arwrldexplore.activity.ArMeasureActivity;
import com.arwrld.arwrldexplore.location.LocationApi;
import com.arwrld.arwrldexplore.utils.GeoUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

import io.nlopez.smartlocation.OnLocationUpdatedListener;

public class PropertiesFragment extends BaseFragment {

    MapView mapView;
    RelativeLayout sizeHolder;
    TextView sizeText;
    FloatingActionButton arBtn;

    private Context mContext;
    private MapboxMap map;
    private View view;
    private Location lastLocation;
    private List<LatLng> latLngs = new ArrayList<>();
    private List<Location> locations = new ArrayList<>();
    private boolean firstCamera = true;
    double size = 0.0;

    private IconFactory iconFactory;
    private Icon defIcon;

    public static PropertiesFragment newInstance() {
        return new PropertiesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_properties, container, false);
        mapView = view.findViewById(R.id.map);
        sizeHolder = view.findViewById(R.id.sizeholder);
        sizeText = view.findViewById(R.id.size);
        arBtn = view.findViewById(R.id.ar_btn);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView.onCreate(savedInstanceState);
        iconFactory = IconFactory.getInstance(mContext);
        defIcon = iconFactory.fromResource(R.mipmap.ic_arwrld);

        sizeHolder.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        if (map == null) {
            setUpMap();
        }

        Answers.getInstance().logContentView(new ContentViewEvent().putContentName("Map Measure"));
    }

    private void setUpMap() {
        mapView.setStyleUrl("mapbox://styles/arwrld/cjcye9ekk1zoo2slsafcfyei5");
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                map = mapboxMap;

                map.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        latLngs.add(point);
                        Location location = new Location("Mapbox");
                        location.setLatitude(point.getLatitude());
                        location.setLongitude(point.getLongitude());
                        locations.add(location);
                        drawMarker(point);
                        processAndDrawLine();
                        size = GeoUtils.returnArWrldPrice(locations);
                        sizeText.setText("ArWrld Tokens: " + size);
                        if(size > 0.0){
                            sizeHolder.setVisibility(View.VISIBLE);
                            Answers.getInstance().logCustom(new CustomEvent("Query")
                                    .putCustomAttribute("Value", size).putCustomAttribute("inApp", "Map"));
                        }else{
                            sizeHolder.setVisibility(View.GONE);
                        }
                    }
                });

                map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(final @NonNull Marker marker) {
                        return false;
                    }
                });

                map.setOnScrollListener(new MapboxMap.OnScrollListener() {
                    @Override
                    public void onScroll() {
                    }
                });

                map.setOnFlingListener(new MapboxMap.OnFlingListener() {
                    @Override
                    public void onFling() {
                    }
                });

                updateCamera();

                map.setMyLocationEnabled(true);
                map.getTrackingSettings().setDismissBearingTrackingOnGesture(true);

                map.clear();
                map.removeAnnotations();

                arBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent arIntent = new Intent(mContext, ArMeasureActivity.class);
                        startActivity(arIntent);
                    }
                });

            }
        });
    }

    private void updateCamera() {
        try {
            LocationApi.fetchNewLocation(mContext, new OnLocationUpdatedListener() {
                @Override
                public void onLocationUpdated(Location location) {
                    lastLocation = location;

                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                            .zoom(15)
                            .tilt(45)
                            .bearing(lastLocation.getBearing())
                            .build();

                    map.animateCamera(CameraUpdateFactory
                            .newCameraPosition(position), 500, new MapboxMap.CancelableCallback() {
                        @Override
                        public void onCancel() {
                            Log.d("CAMERA", "CANCEL CALLED");
                        }

                        @Override
                        public void onFinish() {
                            Log.d("CAMERA", "FINISH CALLED");
                            if (firstCamera) {
                                firstCamera = false;
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.d("CAMERA", "EXCEPTION CALLED");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (map != null) {
            map.setMyLocationEnabled(false);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (map != null) {
            map.setMyLocationEnabled(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    private void drawMarker(final LatLng latLng) {
        map.addMarker(new MarkerOptions()
                .position(latLng));
    }

    private void processAndDrawLine() {
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(latLngs)
                .color(Color.RED)
                .width(2f);
        this.map.addPolyline(polylineOptions);
    }
}
