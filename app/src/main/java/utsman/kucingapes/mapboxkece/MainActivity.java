package utsman.kucingapes.mapboxkece;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends MyLocation implements OnMapReadyCallback {
    FirebaseDatabase database;
    DatabaseReference reference;

    private Point originPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;

    private Double lat;
    private Double lng;
    private String title;
    private String snip;
    private Button jalanBtn, detailBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Mapbox.getInstance(this, "pk.eyJ1Ijoia3VjaW5nYXBlcyIsImEiOiJjaXAxazM0d3owMnFldGhtNGYzMTNnMmY0In0.oM0SseNi5PZOCA6y8NsFFw");
        mapView = findViewById(R.id.mapView);
        jalanBtn = findViewById(R.id.jalan);
        detailBtn = findViewById(R.id.btn_detail);
        jalanBtn.setEnabled(false);
        detailBtn.setEnabled(false);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        enableLocationPlugin();

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("data");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    lat = ds.child("lat").getValue(Double.class);
                    lng = ds.child("lng").getValue(Double.class);
                    title = ds.child("title").getValue(String.class);
                    snip = ds.child("subtitle").getValue(String.class);
                    setupMarker(lng, lat, title, snip);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setupMarker(Double lng, Double lat, String title, String snip) {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        LocationEngine locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        @SuppressLint("MissingPermission")
        Location lastLocation = locationEngine.getLastLocation();
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title)
                .snippet(snip));

        mapboxMap.setOnMarkerClickListener(marker -> {
            Toast.makeText(MainActivity.this, marker.getTitle(), Toast.LENGTH_SHORT).show();

            Double lngCurrent = lastLocation.getLongitude();
            Double latCurrent = lastLocation.getLatitude();

            Double lngMarker = marker.getPosition().getLongitude();
            Double latMarker = marker.getPosition().getLatitude();

            destinationPosition = Point.fromLngLat(lngMarker, latMarker);
            originPosition = Point.fromLngLat(lngCurrent, latCurrent);
            getRoute(originPosition, destinationPosition);
            mapboxMap.selectMarker(marker);
            jalanBtn.setEnabled(true);
            detailBtn.setEnabled(true);
            settingJlnButton(lngCurrent, latCurrent, lngMarker, latMarker);
            settingDetailButton(marker.getTitle());
            return true;
        });

        mapboxMap.addOnMapClickListener(point -> {
            navigationMapRoute.removeRoute();
            jalanBtn.setEnabled(false);
            detailBtn.setEnabled(false);
        });
    }

    private void settingDetailButton(String title) {
        detailBtn.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Detail.class);
            intent.putExtra("title", title);
            startActivity(intent);
        });
    }

    private void settingJlnButton(Double lngCurrent, Double latCurrent, Double lngMarker, Double latMarker) {
        jalanBtn.setOnClickListener(view -> {

            String url = "http://maps.google.com/maps?saddr=" + latCurrent + "," + lngCurrent + "&daddr=" + latMarker +
                    "," + lngMarker + "&mode=driving";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }


    @SuppressLint("LogNotTimber")
    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }
}
