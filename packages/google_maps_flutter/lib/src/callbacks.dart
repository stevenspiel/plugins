// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

part of google_maps_flutter;

/// Callback function taking a single argument.
typedef void ArgumentCallback<T>(T argument);

/// Mutable collection of [ArgumentCallback] instances, itself an [ArgumentCallback].
///
/// Additions and removals happening during a single [call] invocation do not
/// change who gets a callback until the next such invocation.
///
/// Optimized for the singleton case.
class ArgumentCallbacks<T> {
  final List<ArgumentCallback<T>> _callbacks = <ArgumentCallback<T>>[];

  /// Callback method. Invokes the corresponding method on each callback
  /// in this collection.
  ///
  /// The list of callbacks being invoked is computed at the start of the
  /// method and is unaffected by any changes subsequently made to this
  /// collection.
  void call(T argument) {
    final int length = _callbacks.length;
    if (length == 1) {
      _callbacks[0].call(argument);
    } else if (0 < length) {
      for (ArgumentCallback<T> callback
          in List<ArgumentCallback<T>>.from(_callbacks)) {
        callback(argument);
      }
    }
  }

  /// Adds a callback to this collection.
  void add(ArgumentCallback<T> callback) {
    assert(callback != null);
    _callbacks.add(callback);
  }

  /// Removes a callback from this collection.
  ///
  /// Does nothing, if the callback was not present.
  void remove(ArgumentCallback<T> callback) {
    _callbacks.remove(callback);
  }

  /// Whether this collection is empty.
  bool get isEmpty => _callbacks.isEmpty;

  /// Whether this collection is non-empty.
  bool get isNotEmpty => _callbacks.isNotEmpty;
}

/// Callback function taking 2 arguments.
typedef void ArgumentDragCallback<T, Z>(T marker, Z latLong);

/// Mutable collection of [ArgumentDragCallback] instances, itself an [ArgumentDragCallback].
///
/// Additions and removals happening during a single [call] invocation do not
/// change who gets a callback until the next such invocation.
///
/// Optimized for the singleton case.
class ArgumentDragCallbacks<T, Z> {
  final List<ArgumentDragCallback<T, Z>> _callbacks = <ArgumentDragCallback<T, Z>>[];

  /// Callback method. Invokes the corresponding method on each callback
  /// in this collection.
  ///
  /// The list of callbacks being invoked is computed at the start of the
  /// method and is unaffected by any changes subsequently made to this
  /// collection.
  void call(T marker, Z latLong) {
    final int length = _callbacks.length;
    if (length == 1) {
      _callbacks[0].call(marker, latLong);
    } else if (0 < length) {
      for (ArgumentDragCallback<T, Z> callback
      in List<ArgumentDragCallback<T, Z>>.from(_callbacks)) {
        callback(marker, latLong);
      }
    }
  }

  /// Adds a callback to this collection.
  void add(ArgumentDragCallback<T, Z> callback) {
    assert(callback != null);
    _callbacks.add(callback);
  }

  /// Removes a callback from this collection.
  ///
  /// Does nothing, if the callback was not present.
  void remove(ArgumentDragCallback<T, Z> callback) {
    _callbacks.remove(callback);
  }

  /// Whether this collection is empty.
  bool get isEmpty => _callbacks.isEmpty;

  /// Whether this collection is non-empty.
  bool get isNotEmpty => _callbacks.isNotEmpty;
}

/// Mutable collection of [VoidCallback] instances.
///
/// Additions and removals happening during a single [call] invocation do not
/// change who gets a callback until the next such invocation.
///
/// Optimized for the singleton case.
class VoidCallbacks {
  final List<VoidCallback> _callbacks = <VoidCallback>[];

  /// Callback method. Invokes the corresponding method on each callback
  /// in this collection.
  ///
  /// The list of callbacks being invoked is computed at the start of the
  /// method and is unaffected by any changes subsequently made to this
  /// collection.
  void call() {
    final int length = _callbacks.length;
    if (length == 1) {
      _callbacks[0].call();
    } else if (0 < length) {
      for (VoidCallback callback in List<VoidCallback>.from(_callbacks)) {
        callback();
      }
    }
  }

  /// Adds a callback to this collection.
  void add(VoidCallback callback) {
    assert(callback != null);
    _callbacks.add(callback);
  }

  /// Removes a callback from this collection.
  ///
  /// Does nothing, if the callback was not present.
  void remove(VoidCallback callback) {
    _callbacks.remove(callback);
  }

  /// Whether this collection is empty.
  bool get isEmpty => _callbacks.isEmpty;

  /// Whether this collection is non-empty.
  bool get isNotEmpty => _callbacks.isNotEmpty;
}
