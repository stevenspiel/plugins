package io.flutter.plugins.firebasemlvision;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import io.flutter.plugin.common.MethodChannel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FaceDetector implements Detector {
  private final FirebaseVisionFaceDetector detector;

  FaceDetector(FirebaseVision vision, Map<String, Object> options) {
    detector = vision.getVisionFaceDetector(parseOptions(options));
  }

  @Override
  public void handleDetection(final FirebaseVisionImage image, final MethodChannel.Result result) {
    detector
        .detectInImage(image)
        .addOnSuccessListener(
            new OnSuccessListener<List<FirebaseVisionFace>>() {
              @Override
              public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                List<Map<String, Object>> faces = new ArrayList<>(firebaseVisionFaces.size());
                for (FirebaseVisionFace face : firebaseVisionFaces) {
                  Map<String, Object> faceData = new HashMap<>();

                  faceData.put("left", (double) face.getBoundingBox().left);
                  faceData.put("top", (double) face.getBoundingBox().top);
                  faceData.put("width", (double) face.getBoundingBox().width());
                  faceData.put("height", (double) face.getBoundingBox().height());

                  faceData.put("headEulerAngleY", face.getHeadEulerAngleY());
                  faceData.put("headEulerAngleZ", face.getHeadEulerAngleZ());

                  if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    faceData.put("smilingProbability", face.getSmilingProbability());
                  }

                  if (face.getLeftEyeOpenProbability()
                      != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    faceData.put("leftEyeOpenProbability", face.getLeftEyeOpenProbability());
                  }

                  if (face.getRightEyeOpenProbability()
                      != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    faceData.put("rightEyeOpenProbability", face.getRightEyeOpenProbability());
                  }

                  if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                    faceData.put("trackingId", face.getTrackingId());
                  }

                  faceData.put("landmarks", getLandmarkData(face));

                  faces.add(faceData);
                }

                result.success(faces);
              }
            })
        .addOnFailureListener(
            new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception exception) {
                result.error("faceDetectorError", exception.getLocalizedMessage(), null);
              }
            });
  }

  private Map<String, double[]> getLandmarkData(FirebaseVisionFace face) {
    Map<String, double[]> landmarks = new HashMap<>();

    landmarks.put("bottomMouth", landmarkPosition(face, FirebaseVisionFaceLandmark.MOUTH_BOTTOM));
    landmarks.put("leftCheek", landmarkPosition(face, FirebaseVisionFaceLandmark.LEFT_CHEEK));
    landmarks.put("leftEar", landmarkPosition(face, FirebaseVisionFaceLandmark.LEFT_EAR));
    landmarks.put("leftEye", landmarkPosition(face, FirebaseVisionFaceLandmark.LEFT_EYE));
    landmarks.put("leftMouth", landmarkPosition(face, FirebaseVisionFaceLandmark.MOUTH_LEFT));
    landmarks.put("noseBase", landmarkPosition(face, FirebaseVisionFaceLandmark.NOSE_BASE));
    landmarks.put("rightCheek", landmarkPosition(face, FirebaseVisionFaceLandmark.RIGHT_CHEEK));
    landmarks.put("rightEar", landmarkPosition(face, FirebaseVisionFaceLandmark.RIGHT_EAR));
    landmarks.put("rightEye", landmarkPosition(face, FirebaseVisionFaceLandmark.RIGHT_EYE));
    landmarks.put("rightMouth", landmarkPosition(face, FirebaseVisionFaceLandmark.MOUTH_RIGHT));

    return landmarks;
  }

  private double[] landmarkPosition(FirebaseVisionFace face, int landmarkInt) {
    FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkInt);
    if (landmark != null) {
      return new double[] {landmark.getPosition().getX(), landmark.getPosition().getY()};
    }

    return null;
  }

  private FirebaseVisionFaceDetectorOptions parseOptions(Map<String, Object> options) {
    int classification =
        (boolean) options.get("enableClassification")
            ? FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS
            : FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS;

    int landmark =
        (boolean) options.get("enableLandmarks")
            ? FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS
            : FirebaseVisionFaceDetectorOptions.NO_LANDMARKS;

    int mode;
    switch ((String) options.get("mode")) {
      case "accurate":
        mode = FirebaseVisionFaceDetectorOptions.ACCURATE;
        break;
      case "fast":
        mode = FirebaseVisionFaceDetectorOptions.FAST;
        break;
      default:
        throw new IllegalArgumentException("Not a mode:" + options.get("mode"));
    }

    FirebaseVisionFaceDetectorOptions.Builder builder =
        new FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(classification)
            .setLandmarkMode(landmark)
            .setMinFaceSize((float) ((double) options.get("minFaceSize")))
            .setPerformanceMode(mode);

    if ((boolean) options.get("enableTracking")) {
      builder.enableTracking();
    }

    return builder.build();
  }

  @Override
  public void close() throws IOException {
    detector.close();
  }
}
