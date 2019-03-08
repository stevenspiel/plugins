// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "GoogleMapPolygonController.h"

static uint64_t _nextPolygonId = 0;

@implementation FLTGoogleMapPolygonController {
  GMSPolygon* _polygon;
  GMSMapView* _mapView;
}
- (instancetype)initWithPath:(GMSPath*)path mapView:(GMSMapView*)mapView {
  self = [super init];
  if (self) {
    _polygon = [GMSPolygon polygonWithPath:path];
    _mapView = mapView;
    _polygonId = [NSString stringWithFormat:@"%lld", _nextPolygonId++];
    _polygon.userData = @[ _polygonId, @(NO) ];
  }
  return self;
}

#pragma mark - FLTGoogleMapPolygonOptionsSink methods

- (void)setConsumeTapEvents:(BOOL)consumes {
  _polygon.userData[1] = @(consumes);
}
- (void)setPoints:(GMSPath*)points {
  _polygon.path = points;
}
- (void)setClickable:(BOOL)clickable {
  _polygon.tappable = clickable;
}
- (void)setStrokeColor:(UIColor*)color {
  _polygon.strokeColor = color;
}
- (void)setFillColor:(UIColor*)color {
  _polygon.fillColor = color;
}
- (void)setGeodesic:(BOOL)geodesic {
  _polygon.geodesic = geodesic;
}
- (void)setVisible:(BOOL)visible {
  _polygon.map = visible ? _mapView : nil;
}
- (void)setStrokeWidth:(CGFloat)strokeWidth {
  _polygon.strokeWidth = (strokeWidth / 4);
}
- (void)setZIndex:(int)zIndex {
  _polygon.zIndex = zIndex;
}
@end
