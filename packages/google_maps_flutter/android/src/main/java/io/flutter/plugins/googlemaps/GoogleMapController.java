// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.CREATED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.DESTROYED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.PAUSED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.RESUMED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.STARTED;
import static io.flutter.plugins.googlemaps.GoogleMapsPlugin.STOPPED;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Controller of a single GoogleMaps MapView instance. */
final class GoogleMapController
    implements Application.ActivityLifecycleCallbacks,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnPolygonClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMapOptionsSink,
        MethodChannel.MethodCallHandler,
        OnMapReadyCallback,
        OnMarkerTappedListener,
        OnMarkerDragListener,
        OnPolygonTappedListener,
        PlatformView {
  private static final String TAG = "GoogleMapController";
  private final int id;
  private final AtomicInteger activityState;
  private final MethodChannel methodChannel;
  private final PluginRegistry.Registrar registrar;
  private final MapView mapView;
  private final Map<String, MarkerController> markers;
  private final Map<String, PolygonController> polygons;
  private GoogleMap googleMap;
  private boolean trackCameraPosition = false;
  private boolean myLocationEnabled = false;
  private boolean disposed = false;
  private final float density;
  private MethodChannel.Result mapReadyResult;
  private final int registrarActivityHashCode;
  private final Context context;

  GoogleMapController(
      int id,
      Context context,
      AtomicInteger activityState,
      PluginRegistry.Registrar registrar,
      GoogleMapOptions options) {
    this.id = id;
    this.context = context;
    this.activityState = activityState;
    this.registrar = registrar;
    this.mapView = new MapView(context, options);
    this.markers = new HashMap<>();
    this.polygons = new HashMap<>();
    this.density = context.getResources().getDisplayMetrics().density;
    methodChannel =
        new MethodChannel(registrar.messenger(), "plugins.flutter.io/google_maps_" + id);
    methodChannel.setMethodCallHandler(this);
    this.registrarActivityHashCode = registrar.activity().hashCode();
  }

  @Override
  public View getView() {
    return mapView;
  }

  void init() {
    switch (activityState.get()) {
      case STOPPED:
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();
        mapView.onPause();
        mapView.onStop();
        break;
      case PAUSED:
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();
        mapView.onPause();
        break;
      case RESUMED:
        mapView.onCreate(null);
        mapView.onStart();
        mapView.onResume();
        break;
      case STARTED:
        mapView.onCreate(null);
        mapView.onStart();
        break;
      case CREATED:
        mapView.onCreate(null);
        break;
      case DESTROYED:
        // Nothing to do, the activity has been completely destroyed.
        break;
      default:
        throw new IllegalArgumentException(
            "Cannot interpret " + activityState.get() + " as an activity state");
    }
    registrar.activity().getApplication().registerActivityLifecycleCallbacks(this);
    mapView.getMapAsync(this);
  }

  private void moveCamera(CameraUpdate cameraUpdate) {
    googleMap.moveCamera(cameraUpdate);
  }

  private void animateCamera(CameraUpdate cameraUpdate) {
    googleMap.animateCamera(cameraUpdate);
  }

  private CameraPosition getCameraPosition() {
    return trackCameraPosition ? googleMap.getCameraPosition() : null;
  }

  private MarkerBuilder newMarkerBuilder() {
    return new MarkerBuilder(this);
  }

  Marker addMarker(MarkerOptions markerOptions, boolean consumesTapEvents) {
    final Marker marker = googleMap.addMarker(markerOptions);
    markers.put(marker.getId(), new MarkerController(marker, consumesTapEvents, this, this));
    return marker;
  }

  private void removeMarker(String markerId) {
    final MarkerController markerController = markers.remove(markerId);
    if (markerController != null) {
      markerController.remove();
    }
  }

  private MarkerController marker(String markerId) {
    final MarkerController marker = markers.get(markerId);
    if (marker == null) {
      throw new IllegalArgumentException("Unknown marker: " + markerId);
    }
    return marker;
  }

  private PolygonBuilder newPolygonBuilder() {
    return new PolygonBuilder(this);
  }

  Polygon addPolygon(PolygonOptions polygonOptions, boolean consumesTapEvents) {
    final Polygon polygon = googleMap.addPolygon(polygonOptions);
    polygons.put(polygon.getId(), new PolygonController(polygon, consumesTapEvents, this));
    return polygon;
  }

  private void removePolygon(String polygonId) {
    final PolygonController polygonController = polygons.remove(polygonId);
    if (polygonController != null) {
      polygonController.remove();
    }
  }

  private PolygonController polygon(String polygonId) {
    final PolygonController polygon = polygons.get(polygonId);
    if (polygon == null) {
      throw new IllegalArgumentException("Unknown polygon: " + polygonId);
    }
    return polygon;
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    this.googleMap = googleMap;
    googleMap.setOnInfoWindowClickListener(this);
    if (mapReadyResult != null) {
      mapReadyResult.success(null);
      mapReadyResult = null;
    }
    googleMap.setOnCameraMoveStartedListener(this);
    googleMap.setOnCameraMoveListener(this);
    googleMap.setOnCameraIdleListener(this);
    googleMap.setOnMarkerClickListener(this);
    googleMap.setOnMarkerDragListener(this);
    googleMap.setOnPolygonClickListener(this);
    googleMap.setOnMapLoadedCallback(this);
    googleMap.setOnMapLongClickListener(this);
    googleMap.setOnMapClickListener(this);
    googleMap.setOnMyLocationButtonClickListener(this);
    googleMap.setOnMyLocationClickListener(this);
    updateMyLocationEnabled();
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    switch (call.method) {
      case "map#waitForMap":
        if (googleMap != null) {
          result.success(null);
          return;
        }
        mapReadyResult = result;
        break;
      case "map#update":
        {
          Convert.interpretGoogleMapOptions(call.argument("options"), this);
          result.success(Convert.toJson(getCameraPosition()));
          break;
        }
      case "map#takeSnapshot":
        {
          final String filePath = call.argument("filePath");
          takeSnapShot(filePath);
          result.success(null);
          break;
        }
      case "camera#move":
        {
          final CameraUpdate cameraUpdate =
              Convert.toCameraUpdate(call.argument("cameraUpdate"), density);
          moveCamera(cameraUpdate);
          result.success(null);
          break;
        }
      case "camera#animate":
        {
          final CameraUpdate cameraUpdate =
              Convert.toCameraUpdate(call.argument("cameraUpdate"), density);
          animateCamera(cameraUpdate);
          result.success(null);
          break;
        }
      case "marker#add":
        {
          final MarkerBuilder markerBuilder = newMarkerBuilder();
          Convert.interpretMarkerOptions(call.argument("options"), markerBuilder);
          final String markerId = markerBuilder.build();
          result.success(markerId);
          break;
        }
      case "marker#drag":
        {
          final MarkerBuilder markerBuilder = newMarkerBuilder();
          Convert.interpretMarkerOptions(call.argument("options"), markerBuilder);
          final String markerId = markerBuilder.build();
          result.success(markerId);
          break;
        }
      case "marker#dragStart":
        {
          final MarkerBuilder markerBuilder = newMarkerBuilder();
          Convert.interpretMarkerOptions(call.argument("options"), markerBuilder);
          final String markerId = markerBuilder.build();
          result.success(markerId);
          break;
        }
      case "marker#dragEnd":
        {
          final MarkerBuilder markerBuilder = newMarkerBuilder();
          Convert.interpretMarkerOptions(call.argument("options"), markerBuilder);
          final String markerId = markerBuilder.build();
          result.success(markerId);
          break;
        }
      case "marker#remove":
        {
          final String markerId = call.argument("marker");
          removeMarker(markerId);
          result.success(null);
          break;
        }
      case "marker#update":
        {
          final String markerId = call.argument("marker");
          final MarkerController marker = marker(markerId);
          Convert.interpretMarkerOptions(call.argument("options"), marker);
          result.success(null);
          break;
        }
      case "polygon#add":
        {
          final PolygonBuilder polygonBuilder = newPolygonBuilder();
          Convert.interpretPolygonOptions(call.argument("options"), polygonBuilder);
          final String polygonId = polygonBuilder.build();
          result.success(polygonId);
          break;
        }
      case "polygon#getAreaInMeters":
        {
          final List<LatLng> points = Convert.extractPointsFromOptions(call.argument("options"));
          final Double area = SphericalUtil.computeArea(points);
          result.success(area);
          break;
        }
      case "polygon#remove":
        {
          final String polygonId = call.argument("polygon");
          removePolygon(polygonId);
          result.success(null);
          break;
        }
      case "polygon#update":
        {
          final String polygonId = call.argument("polygon");
          final PolygonController polygon = polygon(polygonId);
          Convert.interpretPolygonOptions(call.argument("options"), polygon);
          result.success(null);
          break;
        }
      default:
        result.notImplemented();
    }
  }

  @Override
  public void onMapLoaded() {
    final Map<String, Object> arguments = new HashMap<>();
    methodChannel.invokeMethod("map#onLoaded", arguments);
  }

  @Override
  public void onCameraMoveStarted(int reason) {
    final Map<String, Object> arguments = new HashMap<>(2);
    boolean isGesture = reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE;
    arguments.put("isGesture", isGesture);
    methodChannel.invokeMethod("camera#onMoveStarted", arguments);
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("marker", marker.getId());
    methodChannel.invokeMethod("infoWindow#onTap", arguments);
  }

  @Override
  public void onCameraMove() {
    if (!trackCameraPosition) {
      return;
    }
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("position", Convert.toJson(googleMap.getCameraPosition()));
    methodChannel.invokeMethod("camera#onMove", arguments);
  }

  @Override
  public void onCameraIdle() {
    methodChannel.invokeMethod("camera#onIdle", Collections.singletonMap("map", id));
  }

  @Override
  public void onMarkerTapped(Marker marker) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("marker", marker.getId());
    methodChannel.invokeMethod("marker#onTap", arguments);
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("marker", marker.getId());
    arguments.put("latitude", marker.getPosition().latitude);
    arguments.put("longitude", marker.getPosition().longitude);
    methodChannel.invokeMethod("marker#onDrag", arguments);
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("marker", marker.getId());
    arguments.put("latitude", marker.getPosition().latitude);
    arguments.put("longitude", marker.getPosition().longitude);
    methodChannel.invokeMethod("marker#onDragStart", arguments);
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("marker", marker.getId());
    arguments.put("latitude", marker.getPosition().latitude);
    arguments.put("longitude", marker.getPosition().longitude);
    methodChannel.invokeMethod("marker#onDragEnd", arguments);
  }

  @Override
  public void onPolygonTapped(Polygon polygon) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("polygon", polygon.getId());
    methodChannel.invokeMethod("polygon#onTap", arguments);
  }

  @Override
  public void onMapClick(LatLng latLng) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("latitude", latLng.latitude);
    arguments.put("longitude", latLng.longitude);
    methodChannel.invokeMethod("map#onTap", arguments);
  }

  public void takeSnapShot(String filePath) {
    final String path = filePath;

    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
      Bitmap bitmap;

      @Override
      public void onSnapshotReady(Bitmap snapshot) {
        bitmap = snapshot;
        String returnPath;

        try {
          File file = new File(path);
          FileOutputStream fout = new FileOutputStream(file);

          // Write the string to the file
          bitmap.compress(Bitmap.CompressFormat.PNG, 90, fout);
          fout.flush();
          fout.close();
          returnPath = path;
        } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          Log.d("ImageCapture", "FileNotFoundException");
          Log.d("ImageCapture", e.getMessage());
          returnPath = "";
        } catch (IOException e) {
          // TODO Auto-generated catch block
          Log.d("ImageCapture", "IOException");
          Log.d("ImageCapture", e.getMessage());
          returnPath = "";
        }

        final Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("filePath", returnPath);
        methodChannel.invokeMethod("map#onSnapshotReady", arguments);
      }
    };

    googleMap.snapshot(callback);
  }

  @Override
  public void onMapLongClick(LatLng latLng) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("latitude", latLng.latitude);
    arguments.put("longitude", latLng.longitude);
    methodChannel.invokeMethod("map#onLongTap", arguments);
  }

  @Override
  public boolean onMyLocationButtonClick() {
    final Map<String, Object> arguments = new HashMap<>();
    methodChannel.invokeMethod("location#buttonClick", arguments);
    return false;
  }

  @Override
  public void onMyLocationClick(Location location) {
    final Map<String, Object> arguments = new HashMap<>(2);
    arguments.put("latitude", location.getLatitude());
    arguments.put("longitude", location.getLongitude());
    methodChannel.invokeMethod("location#locationClick", arguments);
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    final MarkerController markerController = markers.get(marker.getId());
    return (markerController != null && markerController.onTap());
  }

  @Override
  public void onPolygonClick(Polygon polygon) {
    final PolygonController polygonController = polygons.get(polygon.getId());
    polygonController.onTap();
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    methodChannel.setMethodCallHandler(null);
    mapView.onDestroy();
    registrar.activity().getApplication().unregisterActivityLifecycleCallbacks(this);
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onCreate(savedInstanceState);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onStart();
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onResume();
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onPause();
  }

  @Override
  public void onActivityStopped(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onStop();
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    if (disposed || activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    mapView.onDestroy();
  }

  // GoogleMapOptionsSink methods

  @Override
  public void setCameraTargetBounds(LatLngBounds bounds) {
    googleMap.setLatLngBoundsForCameraTarget(bounds);
  }

  @Override
  public void setCompassEnabled(boolean compassEnabled) {
    googleMap.getUiSettings().setCompassEnabled(compassEnabled);
  }

  @Override
  public void setMapType(int mapType) {
    googleMap.setMapType(mapType);
  }

  @Override
  public void setTrackCameraPosition(boolean trackCameraPosition) {
    this.trackCameraPosition = trackCameraPosition;
  }

  @Override
  public void setRotateGesturesEnabled(boolean rotateGesturesEnabled) {
    googleMap.getUiSettings().setRotateGesturesEnabled(rotateGesturesEnabled);
  }

  @Override
  public void setScrollGesturesEnabled(boolean scrollGesturesEnabled) {
    googleMap.getUiSettings().setScrollGesturesEnabled(scrollGesturesEnabled);
  }

  @Override
  public void setTiltGesturesEnabled(boolean tiltGesturesEnabled) {
    googleMap.getUiSettings().setTiltGesturesEnabled(tiltGesturesEnabled);
  }

  @Override
  public void setMapToolbarEnabled(boolean mapToolbarEnabled) {
    googleMap.getUiSettings().setMapToolbarEnabled(mapToolbarEnabled);
  }

  @Override
  public void setMyLocationButtonEnabled(boolean myLocationButtonEnabled) {
    googleMap.getUiSettings().setMyLocationButtonEnabled(myLocationButtonEnabled);
  }

  @Override
  public void setMinMaxZoomPreference(Float min, Float max) {
    googleMap.resetMinMaxZoomPreference();
    if (min != null) {
      googleMap.setMinZoomPreference(min);
    }
    if (max != null) {
      googleMap.setMaxZoomPreference(max);
    }
  }

  @Override
  public void setZoomGesturesEnabled(boolean zoomGesturesEnabled) {
    googleMap.getUiSettings().setZoomGesturesEnabled(zoomGesturesEnabled);
  }

  @Override
  public void setMyLocationEnabled(boolean myLocationEnabled) {
    if (this.myLocationEnabled == myLocationEnabled) {
      return;
    }
    this.myLocationEnabled = myLocationEnabled;
    if (googleMap != null) {
      updateMyLocationEnabled();
    }
  }

  private void updateMyLocationEnabled() {
    if (hasLocationPermission()) {
      // The plugin doesn't add the location permission by default so that apps that don't need
      // the feature won't require the permission.
      // Gradle is doing a static check for missing permission and in some configurations will
      // fail the build if the permission is missing. The following disables the Gradle lint.
      //noinspection ResourceType
      googleMap.setMyLocationEnabled(myLocationEnabled);
    } else {
      // TODO(amirh): Make the options update fail.
      // https://github.com/flutter/flutter/issues/24327
      Log.e(TAG, "Cannot enable MyLocation layer as location permissions are not granted");
    }
  }

  private boolean hasLocationPermission() {
    return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
  }

  private int checkSelfPermission(String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(
        permission, android.os.Process.myPid(), android.os.Process.myUid());
  }
}
