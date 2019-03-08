// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import <Flutter/Flutter.h>
#import <GoogleMaps/GoogleMaps.h>

// Defines polygon UI options writable from Flutter.
@protocol FLTGoogleMapPolygonOptionsSink
- (void)setConsumeTapEvents:(BOOL)consume;
- (void)setPoints:(GMSPath*)points;
- (void)setClickable:(BOOL)clickable;
- (void)setStrokeColor:(UIColor*)color;
- (void)setFillColor:(UIColor*)color;
- (void)setGeodesic:(BOOL)geodesic;
- (void)setPattern:(NSArray<GMSStyleSpan*>*)pattern;
- (void)setVisible:(BOOL)visible;
- (void)setStrokeWidth:(CGFloat)width;
- (void)setZIndex:(int)zIndex;
@end

// Defines polygon controllable by Flutter.
@interface FLTGoogleMapPolygonController : NSObject <FLTGoogleMapPolygonOptionsSink>
@property(atomic, readonly) NSString* polygonId;
- (instancetype)initWithPath:(GMSPath*)path mapView:(GMSMapView*)mapView;
@end
