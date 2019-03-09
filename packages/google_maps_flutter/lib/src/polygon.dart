part of google_maps_flutter;

class Polygon {
  Polygon(this._id, this._options);

  final String _id;

  String get id => _id;

  PolygonOptions _options;

  PolygonOptions get options => _options;
}

class PolygonOptions {
  const PolygonOptions(
    {this.points,
      this.holes,
      this.strokeWidth,
      this.strokeColor,
      this.strokeJointType,
      this.fillColor,
      this.fillColorAlpha,
      this.zIndex,
      this.visible,
      this.geodesic,
      this.clickable});

  final List<LatLng> points;
  final List<List<LatLng>> holes;
  final double strokeWidth;
  final int strokeColor;
  final int strokeJointType;
  final int fillColor;
  final double fillColorAlpha;
  final double zIndex;
  final bool visible;
  final bool geodesic;
  final bool clickable;

  static const PolygonOptions defaultOptions = PolygonOptions(
    points: null,
    holes: null,
    strokeWidth: 10.0,
    strokeColor: 0x80ff0000,
    strokeJointType: 1,
    fillColor: 0x35ff0000,
    fillColorAlpha: 0.2,
    zIndex: 0.5,
    visible: true,
    geodesic: true,
    clickable: true);

  @override
  String toString() {
    return 'PolygonOptions{points: $points, holes: $holes, strokeWidth: $strokeWidth, strokeColor: $strokeColor, strokeJointType: $strokeJointType, fillColor: $fillColor, fillColorAlpha: $fillColorAlpha, zIndex: $zIndex, visible: $visible, geodesic: $geodesic, clickable: $clickable}';
  }

  PolygonOptions copyWith(PolygonOptions changes) {
    if (changes == null) {
      return this;
    }
    return PolygonOptions(
      points: changes.points ?? points,
      holes: changes.holes ?? holes,
      strokeWidth: changes.strokeWidth ?? strokeWidth,
      strokeColor: changes.strokeColor ?? strokeColor,
      strokeJointType: changes.strokeJointType ?? strokeJointType,
      fillColor: changes.fillColor ?? fillColor,
      zIndex: changes.zIndex ?? zIndex,
      visible: changes.visible ?? visible,
      geodesic: changes.geodesic ?? geodesic,
      clickable: changes.clickable ?? clickable);
  }

  dynamic _toJson() {
    final Map<String, dynamic> json = <String, dynamic>{};

    void addIfPresent(String fieldName, dynamic value) {
      if (value != null) {
        json[fieldName] = value;
      }
    }

    if (points != null) {
      json['points'] = _pointsToJson();
    }

    if (holes != null) {
      json['holes'] = _holesToJson();
    }

    addIfPresent('strokeWidth', strokeWidth);
    addIfPresent('strokeColor', strokeColor);
    addIfPresent('strokeJointType', strokeJointType);
    addIfPresent('fillColor', fillColor);
    addIfPresent('visible', visible);
    addIfPresent('zIndex', zIndex);
    addIfPresent('geodesic', geodesic);
    addIfPresent('clickable', clickable);

    json['fillColorAlpha'] = fillColorAlpha ?? 0.2;

    return json;
  }

  dynamic _pointsToJson() {
    final List<dynamic> result = <dynamic>[];
    for (final LatLng point in points) {
      result.add(point._toJson());
    }
    return result;
  }

  dynamic _holesToJson() {
    final List<List<dynamic>> result = <List<dynamic>>[];
    for (final List<LatLng> holePositions in holes) {
      final List<dynamic> positionList = <dynamic>[];
      for (final LatLng position in holePositions) {
        positionList.add(position._toJson());
      }
      result.add(positionList);
    }

    return result;
  }
}
