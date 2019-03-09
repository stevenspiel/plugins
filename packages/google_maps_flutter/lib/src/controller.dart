// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

part of google_maps_flutter;

enum MeasurementType { Meters }

/// Controller for a single GoogleMap instance running on the host platform.
///
/// Change listeners are notified upon changes to any of
///
/// * the [options] property
/// * the collection of [Marker]s added to this map
/// * the [isCameraMoving] property
/// * the [cameraPosition] property
///
/// Listeners are notified after changes have been applied on the platform side.
///
/// Marker tap events can be received by adding callbacks to [onMarkerTapped].
class GoogleMapController extends ChangeNotifier {
  GoogleMapController._(
      this._id, MethodChannel channel, CameraPosition initialCameraPosition)
      : assert(_id != null),
        assert(channel != null),
        _channel = channel {
    _cameraPosition = initialCameraPosition;
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  static Future<GoogleMapController> init(
      int id, CameraPosition initialCameraPosition) async {
    assert(id != null);
    final MethodChannel channel =
        MethodChannel('plugins.flutter.io/google_maps_$id');
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    await channel.invokeMethod('map#waitForMap');
    return GoogleMapController._(id, channel, initialCameraPosition);
  }

  final MethodChannel _channel;

  /// Callbacks to receive tap events for markers placed on this map.
  final ArgumentCallbacks<Marker> onMarkerTapped = ArgumentCallbacks<Marker>();

  /// Callbacks to receive drag events for markers on this map.
  final ArgumentDragCallbacks<Marker, LatLng> onMarkerDrag = ArgumentDragCallbacks<Marker, LatLng>();

  /// Callbacks to receive drag events for markers on this map.
  final ArgumentDragCallbacks<Marker, LatLng> onMarkerDragStart = ArgumentDragCallbacks<Marker, LatLng>();

  /// Callbacks to receive drag events for markers on this map.
  final ArgumentDragCallbacks<Marker, LatLng> onMarkerDragEnd = ArgumentDragCallbacks<Marker, LatLng>();

  /// Callbacks to receive tap events for markers placed on this map.
  final ArgumentCallbacks<Polygon> onPolygonTapped =
      ArgumentCallbacks<Polygon>();

  /// Callbacks to receive tap events for markers placed on this map.
  final ArgumentCallbacks<LatLng> onMapTapped = ArgumentCallbacks<LatLng>();

  /// Callbacks to receive tap events for markers placed on this map.
  final ArgumentCallbacks<LatLng> onMapLongTapped = ArgumentCallbacks<LatLng>();

  /// Callbacks to receive location click, ie when a user taps on the blue location circle the lat lon is returned.
  final ArgumentCallbacks<LatLng> onLocationClick = ArgumentCallbacks<LatLng>();

  /// Callbacks to receive click on the location button. The button must be enabled and mylocation must also be enabled.
  final VoidCallbacks onLocationButtonClick = VoidCallbacks();

  /// Callbacks to receive tap events for info windows on markers
  final ArgumentCallbacks<Marker> onInfoWindowTapped =
      ArgumentCallbacks<Marker>();

  /// The current set of markers on this map.
  ///
  /// The returned set will be a detached snapshot of the markers collection.
  Set<Marker> get markers => Set<Marker>.from(_markers.values);
  final Map<String, Marker> _markers = <String, Marker>{};

  /// The current set of polygons on this map.
  ///
  /// The returned set will be a detached snapshot of the polygons collection.
  Set<Polygon> get polygons => Set<Polygon>.from(_polygons.values);
  final Map<String, Polygon> _polygons = <String, Polygon>{};

  /// True if the map camera is currently moving.
  bool get isCameraMoving => _isCameraMoving;
  bool _isCameraMoving = false;

  /// Returns the most recent camera position reported by the platform side.
  /// Will be null, if [GoogleMap.trackCameraPosition] is false.
  CameraPosition get cameraPosition => _cameraPosition;
  CameraPosition _cameraPosition;

  final int _id;

  Future<dynamic> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'infoWindow#onTap':
        final String markerId = call.arguments['marker'];
        final Marker marker = _markers[markerId];
        if (marker != null) {
          onInfoWindowTapped(marker);
        }
        break;

      case 'marker#onTap':
        final String markerId = call.arguments['marker'];
        final Marker marker = _markers[markerId];
        if (marker != null) {
          onMarkerTapped(marker);
        }
        break;

      case 'marker#onDrag':
        final String markerId = call.arguments['marker'];
        final Marker marker = _markers[markerId];
        final double latitude = call.arguments['latitude'];
        final double longitude = call.arguments['longitude'];
        final LatLng latLng = LatLng(latitude, longitude);
        if (marker != null) {
          onMarkerDrag(marker, latLng);
        }
        break;

      case 'marker#onDragStart':
        final String markerId = call.arguments['marker'];
        final Marker marker = _markers[markerId];
        final double latitude = call.arguments['latitude'];
        final double longitude = call.arguments['longitude'];
        final LatLng latLng = LatLng(latitude, longitude);
        if (marker != null) {
          onMarkerDragStart(marker, latLng);
        }
        break;

      case 'marker#onDragEnd':
        final String markerId = call.arguments['marker'];
        final Marker marker = _markers[markerId];
        final double latitude = call.arguments['latitude'];
        final double longitude = call.arguments['longitude'];
        final LatLng latLng = LatLng(latitude, longitude);
        if (marker != null) {
          onMarkerDragEnd(marker, latLng);
        }
        break;

      case 'polygon#onTap':
        final String polygonId = call.arguments['polygon'];
        final Polygon polygon = _polygons[polygonId];
        if (polygon != null) {
          onPolygonTapped(polygon);
        }
        break;

      case 'map#onTap':
        final double latitude = call.arguments['latitude'];
        final double longitude = call.arguments['longitude'];
        final LatLng latLng = LatLng(latitude, longitude);
        if (latLng != null) {
          onMapTapped(latLng);
        }
        break;

      case 'map#onLongTap':
        final double latitude = call.arguments['latitude'];
        final double longitude = call.arguments['longitude'];
        final LatLng latLng = LatLng(latitude, longitude);
        if (latLng != null) {
          onMapLongTapped(latLng);
        }
        break;

      case 'location#locationClick':
        final double latitude = call.arguments['latitude'];
        final double longitude = call.arguments['longitude'];
        final LatLng latLng = LatLng(latitude, longitude);
        if (latLng != null) {
          onLocationClick(latLng);
        }
        break;

      case 'location#buttonClick':
        onLocationButtonClick();
        break;

      case 'camera#onMoveStarted':
        _isCameraMoving = true;
        notifyListeners();
        break;
      case 'camera#onMove':
        _cameraPosition = CameraPosition.fromMap(call.arguments['position']);
        notifyListeners();
        break;
      case 'camera#onIdle':
        _isCameraMoving = false;
        notifyListeners();
        break;
      default:
        throw MissingPluginException();
    }
  }

  /// Updates configuration options of the map user interface.
  ///
  /// Change listeners are notified once the update has been made on the
  /// platform side.
  ///
  /// The returned [Future] completes after listeners have been notified.
  Future<void> _updateMapOptions(Map<String, dynamic> optionsUpdate) async {
    assert(optionsUpdate != null);
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    final dynamic json = await _channel.invokeMethod(
      'map#update',
      <String, dynamic>{
        'options': optionsUpdate,
      },
    );
    _cameraPosition = CameraPosition.fromMap(json);
    notifyListeners();
  }

  /// Starts an animated change of the map camera position.
  ///
  /// The returned [Future] completes after the change has been started on the
  /// platform side.
  Future<void> animateCamera(CameraUpdate cameraUpdate) async {
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    await _channel.invokeMethod('camera#animate', <String, dynamic>{
      'cameraUpdate': cameraUpdate._toJson(),
    });
  }

  /// Changes the map camera position.
  ///
  /// The returned [Future] completes after the change has been made on the
  /// platform side.
  Future<void> moveCamera(CameraUpdate cameraUpdate) async {
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    await _channel.invokeMethod('camera#move', <String, dynamic>{
      'cameraUpdate': cameraUpdate._toJson(),
    });
  }

  /// Adds a marker to the map, configured using the specified custom [options].
  ///
  /// Change listeners are notified once the marker has been added on the
  /// platform side.
  ///
  /// The returned [Future] completes with the added marker once listeners have
  /// been notified.
  Future<Marker> addMarker(MarkerOptions options) async {
    final MarkerOptions effectiveOptions =
        MarkerOptions.defaultOptions.copyWith(options);
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    final String markerId = await _channel.invokeMethod(
      'marker#add',
      <String, dynamic>{
        'options': effectiveOptions._toJson(),
      },
    );
    final Marker marker = Marker(markerId, effectiveOptions);
    _markers[markerId] = marker;
    notifyListeners();
    return marker;
  }

  /// Adds a polygon to the map, configured using the specified custom [options].
  ///
  /// Change listeners are notified once the polygon has been added on the
  /// platform side.
  ///
  /// The returned [Future] completes with the added polygon once listeners have
  /// been notified.
  Future<Polygon> addPolygon(PolygonOptions options) async {
    final PolygonOptions effectiveOptions =
        PolygonOptions.defaultOptions.copyWith(options);
    final String polygonId = await _channel.invokeMethod(
      'polygon#add',
      <String, dynamic>{
        'options': effectiveOptions._toJson(),
      },
    );
    final Polygon polygon = Polygon(polygonId, effectiveOptions);
    _polygons[polygonId] = polygon;
    notifyListeners();
    return polygon;
  }

  Future<double> getPolygonArea(PolygonOptions options, MeasurementType measurementType) async {
    double area;
    String method;

    switch (measurementType) {
      case MeasurementType.Meters: method = "getAreaInMeters";
    }

    try {
      area = await _channel.invokeMethod(
        'polygon#$method',
        <String, dynamic>{
          'options': options._toJson(),
        },
      );
    } catch (e) {
      print('error calculating area: $e');
    }

    return area;
  }

  /// Updates the specified [marker] with the given [changes]. The marker must
  /// be a current member of the [markers] set.
  ///
  /// Change listeners are notified once the marker has been updated on the
  /// platform side.
  ///
  /// The returned [Future] completes once listeners have been notified.
  Future<void> updateMarker(Marker marker, MarkerOptions changes) async {
    assert(marker != null);
    assert(_markers[marker._id] == marker);
    assert(changes != null);
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    await _channel.invokeMethod('marker#update', <String, dynamic>{
      'marker': marker._id,
      'options': changes._toJson(),
    });
    marker._options = marker._options.copyWith(changes);
    notifyListeners();
  }

  /// Updates the specified [polygon] with the given [changes]. The polygon must
  /// be a current member of the [polygons] set.
  ///
  /// Change listeners are notified once the polygon has been updated on the
  /// platform side.
  ///
  /// The returned [Future] completes once listeners have been notified.
  Future<void> updatePolygon(
      Polygon polygon, PolygonOptions changes) async {
    assert(polygon != null);
    assert(_polygons[polygon._id] == polygon);
    assert(changes != null);
    changes = polygon._options.copyWith(changes);
    // Code copied from updateMarker
    // ignore: strong_mode_implicit_dynamic_method
    await _channel.invokeMethod('polygon#update', <String, dynamic>{
      'polygon': polygon._id,
      'options': changes._toJson(),
    });
    polygon._options = polygon._options.copyWith(changes);
    notifyListeners();
  }

  /// Removes the specified [marker] from the map. The marker must be a current
  /// member of the [markers] set.
  ///
  /// Change listeners are notified once the marker has been removed on the
  /// platform side.
  ///
  /// The returned [Future] completes once listeners have been notified.
  Future<void> removeMarker(Marker marker) async {
    assert(marker != null);
    assert(_markers[marker._id] == marker);
    await _removeMarker(marker._id);
    notifyListeners();
  }

  /// Removes the specified [polygon] from the map. The polygons must be a current
  /// member of the [polygons] set.
  ///
  /// Change listeners are notified once the polygon has been removed on the
  /// platform side.
  ///
  /// The returned [Future] completes once listeners have been notified.
  Future<void> removePolygon(Polygon polygon) async {
    assert(polygon != null);
    assert(_polygons[polygon._id] == polygon);
    await _removePolygon(polygon._id);
    notifyListeners();
  }

  /// Removes all [markers] from the map.
  ///
  /// Change listeners are notified once all markers have been removed on the
  /// platform side.
  ///
  /// The returned [Future] completes once listeners have been notified.
  Future<void> clearMarkers() async {
    assert(_markers != null);
    final List<String> markerIds = List<String>.from(_markers.keys);
    for (String id in markerIds) {
      await _removeMarker(id);
    }
    notifyListeners();
  }

  /// Removes all [polygons] from the map.
  ///
  /// Change listeners are notified once all polygons have been removed on the
  /// platform side.
  ///
  /// The returned [Future] completes once listeners have been notified.
  Future<void> clearPolygons() async {
    assert(_polygons != null);
    final List<String> polygonIds = List<String>.from(_polygons.keys);
    for (String id in polygonIds) {
      await _removePolygon(id);
    }
    notifyListeners();
  }

  /// Helper method to remove a single marker from the map. Consumed by
  /// [removeMarker] and [clearMarkers].
  ///
  /// The returned [Future] completes once the marker has been removed from
  /// [_markers].
  Future<void> _removeMarker(String id) async {
    // TODO(amirh): remove this on when the invokeMethod update makes it to stable Flutter.
    // https://github.com/flutter/flutter/issues/26431
    // ignore: strong_mode_implicit_dynamic_method
    await _channel.invokeMethod('marker#remove', <String, dynamic>{
      'marker': id,
    });
    _markers.remove(id);
  }

  /// Helper method to remove a single polygon from the map. Consumed by
  /// [removePolygon] and [clearPolygons].
  ///
  /// The returned [Future] completes once the marker has been removed from
  /// [_polygons].
  Future<void> _removePolygon(String id) async {
    // Code copied from removeMarker
    // ignore: strong_mode_implicit_dynamic_method
    await _channel.invokeMethod('polygon#remove', <String, dynamic>{
      'polygon': id,
    });
    _polygons.remove(id);
  }
}
