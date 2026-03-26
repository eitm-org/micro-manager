package org.micromanager.plugins.fovsymmetry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.micromanager.data.Image;

public class FOVQualityDecisionFunction {

   public static final double EPS = 1e-12;

   public static class FOVModel {
      public final double thrComb;
      public final double[] weights; // length = 2 + nTileRows*nTileCols
      public final double bias;
      public final double[] muX;
      public final double[] sigmaX;
      public final int nTileRows;
      public final int nTileCols;
      public final String modeStr;

      public FOVModel(double thrComb, double[] weights, double bias,
              double[] muX, double[] sigmaX,
              int nTileRows, int nTileCols, String modeStr) {
         this.thrComb = thrComb;
         this.weights = weights;
         this.bias = bias;
         this.muX = muX;
         this.sigmaX = sigmaX;
         this.nTileRows = nTileRows;
         this.nTileCols = nTileCols;
         this.modeStr = modeStr;
      }

      public void validate() {
         int expectedNumFeatures = 2 + nTileRows * nTileCols;
         if (weights == null || weights.length != expectedNumFeatures) {
            throw new IllegalArgumentException("Model weights length (" +
                    (weights == null ? 0 : weights.length) +
                    ") does not match expected number of features (" +
                    expectedNumFeatures + ")");
         }
         if (muX == null || muX.length != expectedNumFeatures) {
            throw new IllegalArgumentException("Model muX length (" +
                    (muX == null ? 0 : muX.length) +
                    ") does not match expected number of features (" +
                    expectedNumFeatures + ")");
         }
         if (sigmaX == null || sigmaX.length != expectedNumFeatures) {
            throw new IllegalArgumentException("Model sigmaX length (" +
                    (sigmaX == null ? 0 : sigmaX.length) +
                    ") does not match expected number of features (" +
                    expectedNumFeatures + ")");
         }
      }
   }

   /**
    * Convert the MATLAB decision function into a Java implementation.
    *
    * @param model model data (weights, bias, normalization, thresholds)
    * @param image image from Micro-Manager data API
    * @return 'g' (good) or 'b' (bad)
    */
   public static char evaluate(FOVModel model, Image image) {
      if (model == null) {
         throw new IllegalArgumentException("Model may not be null");
      }
      model.validate();

      double[] xraw = extractSelectedFeatures(image, model.nTileRows, model.nTileCols);
      if (xraw.length != model.muX.length || xraw.length != model.sigmaX.length
              || xraw.length != model.weights.length) {
         throw new IllegalStateException("Feature vector length does not match model.");
      }

      double[] xz = new double[xraw.length];
      for (int i = 0; i < xraw.length; i++) {
         double sigma = model.sigmaX[i];
         if (!Double.isFinite(sigma) || sigma == 0.0) {
            sigma = 1.0;
         }
         xz[i] = (xraw[i] - model.muX[i]) / sigma;
      }

      double combinedScore = model.bias;
      for (int i = 0; i < xz.length; i++) {
         combinedScore += xz[i] * model.weights[i];
      }

      return applyThresholdGeneral(combinedScore, model.thrComb, model.modeStr);
   }

   private static char applyThresholdGeneral(double value, double thr, String modeStr) {
      if (modeStr == null) {
         throw new IllegalArgumentException("modeStr cannot be null");
      }
      switch (modeStr.trim().toLowerCase()) {
         case "less_is_good":
            return value <= thr ? 'g' : 'b';
         case "greater_is_good":
            return value >= thr ? 'g' : 'b';
         default:
            throw new IllegalArgumentException("Unknown modeStr: " + modeStr);
      }
   }

   private static double[] extractSelectedFeatures(Image image, int nTileRows, int nTileCols) {
      if (image == null) {
         throw new IllegalArgumentException("Image may not be null");
      }
      if (nTileRows <= 0 || nTileCols <= 0) {
         throw new IllegalArgumentException("nTileRows and nTileCols must be > 0");
      }

      int width = image.getWidth();
      int height = image.getHeight();
      double[] gray = convertToGrayDouble01(image);

      double[] gmag = sobelGradientMagnitude(gray, width, height);

      double[][] tileMap = computeTileMap(gmag, width, height, nTileRows, nTileCols);

      double[] tileVals = flattenNonNan(tileMap);
      double muTile = mean(tileVals);
      double stdTile = stdDev(tileVals, muTile);
      double gradMagCV = stdTile / Math.max(muTile, EPS);

      int midC = nTileCols / 2;
      double leftMean = mean(tileMap, 0, midC - 1);
      double rightMean = mean(tileMap, midC, nTileCols - 1);
      double lrSharpImbalance = Math.abs(leftMean - rightMean) / Math.max(leftMean + rightMean, EPS);

      double[][] tileMapNorm;
      if (Double.isFinite(muTile) && muTile > 0) {
         tileMapNorm = new double[nTileRows][nTileCols];
         for (int r = 0; r < nTileRows; r++) {
            for (int c = 0; c < nTileCols; c++) {
               double val = tileMap[r][c];
               tileMapNorm[r][c] = Double.isFinite(val) ? val / muTile : Double.NaN;
            }
         }
      } else {
         tileMapNorm = new double[nTileRows][nTileCols];
         for (int r = 0; r < nTileRows; r++) {
            for (int c = 0; c < nTileCols; c++) {
               tileMapNorm[r][c] = 0.0;
            }
         }
      }

      int featureCount = 2 + nTileRows * nTileCols;
      double[] features = new double[featureCount];
      features[0] = lrSharpImbalance;
      features[1] = gradMagCV;

      int idx = 2;
      // Column-major flattening to match MATLAB tileMap(:).' behavior
      for (int c = 0; c < nTileCols; c++) {
         for (int r = 0; r < nTileRows; r++) {
            features[idx++] = tileMapNorm[r][c];
         }
      }

      return features;
   }

   private static double[] convertToGrayDouble01(Image image) {
      int width = image.getWidth();
      int height = image.getHeight();
      int numComponents = image.getNumComponents();
      int nPixels = width * height;
      double[] raw = new double[nPixels];

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int idx = y * width + x;
            if (numComponents == 1) {
               raw[idx] = image.getIntensityAt(x, y);
            } else {
               long[] comps = image.getComponentIntensitiesAt(x, y);
               if (comps.length >= 3) {
                  raw[idx] = 0.2989 * comps[0] + 0.5870 * comps[1] + 0.1140 * comps[2];
               } else {
                  // fallback: average channels
                  double sum = 0.0;
                  for (long c : comps) {
                     sum += c;
                  }
                  raw[idx] = sum / comps.length;
               }
            }
         }
      }

      double min = Double.POSITIVE_INFINITY;
      double max = Double.NEGATIVE_INFINITY;
      for (double v : raw) {
         if (v < min) {
            min = v;
         }
         if (v > max) {
            max = v;
         }
      }

      if (max > min) {
         for (int i = 0; i < raw.length; i++) {
            raw[i] = (raw[i] - min) / (max - min);
         }
      } else {
         for (int i = 0; i < raw.length; i++) {
            raw[i] = 0.0;
         }
      }

      return raw;
   }

   private static double[] sobelGradientMagnitude(double[] gray, int width, int height) {
      double[] out = new double[gray.length];
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            double gx = 0.0;
            double gy = 0.0;
            for (int dy = -1; dy <= 1; dy++) {
               int yy = y + dy;
               if (yy < 0) {
                  yy = 0;
               } else if (yy >= height) {
                  yy = height - 1;
               }
               for (int dx = -1; dx <= 1; dx++) {
                  int xx = x + dx;
                  if (xx < 0) {
                     xx = 0;
                  } else if (xx >= width) {
                     xx = width - 1;
                  }
                  double val = gray[yy * width + xx];
                  int kx = 0;
                  int ky = 0;
                  if (dy == -1) {
                     if (dx == -1) {
                        kx = -1; ky = -1;
                     } else if (dx == 0) {
                        kx = 0; ky = -2;
                     } else {
                        kx = 1; ky = -1;
                     }
                  } else if (dy == 0) {
                     if (dx == -1) {
                        kx = -2; ky = 0;
                     } else if (dx == 0) {
                        kx = 0; ky = 0;
                     } else {
                        kx = 2; ky = 0;
                     }
                  } else {
                     if (dx == -1) {
                        kx = -1; ky = 1;
                     } else if (dx == 0) {
                        kx = 0; ky = 2;
                     } else {
                        kx = 1; ky = 1;
                     }
                  }
                  gx += kx * val;
                  gy += ky * val;
               }
            }
            out[y * width + x] = Math.hypot(gx, gy);
         }
      }
      return out;
   }

   private static double[][] computeTileMap(double[] gmag, int width, int height,
           int nTileRows, int nTileCols) {
      double[][] tileMap = new double[nTileRows][nTileCols];
      for (int r = 0; r < nTileRows; r++) {
         for (int c = 0; c < nTileCols; c++) {
            tileMap[r][c] = Double.NaN;
         }
      }

      int[] rEdges = new int[nTileRows + 1];
      int[] cEdges = new int[nTileCols + 1];
      for (int i = 0; i <= nTileRows; i++) {
         rEdges[i] = (int) Math.round(i * (double) height / nTileRows);
      }
      for (int j = 0; j <= nTileCols; j++) {
         cEdges[j] = (int) Math.round(j * (double) width / nTileCols);
      }

      for (int rr = 0; rr < nTileRows; rr++) {
         int r1 = rEdges[rr];
         int r2 = rEdges[rr + 1] - 1;
         if (r2 < r1) {
            continue;
         }
         for (int cc = 0; cc < nTileCols; cc++) {
            int c1 = cEdges[cc];
            int c2 = cEdges[cc + 1] - 1;
            if (c2 < c1) {
               continue;
            }
            double sum = 0.0;
            int count = 0;
            for (int y = r1; y <= r2; y++) {
               for (int x = c1; x <= c2; x++) {
                  sum += gmag[y * width + x];
                  count++;
               }
            }
            if (count > 0) {
               tileMap[rr][cc] = sum / count;
            }
         }
      }

      return tileMap;
   }

   private static double[] flattenNonNan(double[][] arr) {
      int rows = arr.length;
      int cols = arr[0].length;
      double[] out = new double[rows * cols];
      int k = 0;
      for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
            double v = arr[r][c];
            if (Double.isFinite(v)) {
               out[k++] = v;
            }
         }
      }
      if (k < out.length) {
         double[] trimmed = new double[k];
         System.arraycopy(out, 0, trimmed, 0, k);
         return trimmed;
      }
      return out;
   }

   private static double mean(double[] values) {
      if (values == null || values.length == 0) {
         return 0.0;
      }
      double sum = 0.0;
      int count = 0;
      for (double v : values) {
         if (Double.isFinite(v)) {
            sum += v;
            count++;
         }
      }
      return count > 0 ? sum / count : 0.0;
   }

   private static double stdDev(double[] values, double mean) {
      if (values == null || values.length == 0) {
         return 0.0;
      }
      double sumSq = 0.0;
      int count = 0;
      for (double v : values) {
         if (Double.isFinite(v)) {
            double d = v - mean;
            sumSq += d * d;
            count++;
         }
      }
      return count > 1 ? Math.sqrt(sumSq / (count - 1)) : 0.0;
   }

   private static double mean(double[][] arr, int colStart, int colEnd) {
      if (arr == null || arr.length == 0 || colStart > colEnd) {
         return 0.0;
      }
      double sum = 0.0;
      int count = 0;
      for (int r = 0; r < arr.length; r++) {
         for (int c = Math.max(0, colStart); c <= Math.min(arr[0].length - 1, colEnd); c++) {
            double v = arr[r][c];
            if (Double.isFinite(v)) {
               sum += v;
               count++;
            }
         }
      }
      return count > 0 ? sum / count : 0.0;
   }

    /**
    * Load a FOVModel from a JSON file.
    */
   public static FOVModel loadModelFromJson(String jsonPath)
           throws IOException {
      String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
      return parseJsonModel(jsonContent);
   }

   /**
    * Parse JSON string into FOVModel.
    * Uses simple string parsing since we want minimal dependencies.
    */
   private static FOVModel parseJsonModel(String json) {
      Map<String, Object> map = parseSimpleJson(json);

      double thrComb = ((Number) map.get("thrComb")).doubleValue();
      double bias = ((Number) map.get("bias")).doubleValue();
      int nTileRows = ((Number) map.get("nTileRows")).intValue();
      int nTileCols = ((Number) map.get("nTileCols")).intValue();
      String modeStr = (String) map.get("modeStr");

      double[] weights = parseDoubleArray((java.util.List<?>) map.get("weights"));
      double[] muX = parseDoubleArray((java.util.List<?>) map.get("muX"));
      double[] sigmaX = parseDoubleArray((java.util.List<?>) map.get("sigmaX"));

      return new FOVModel(
              thrComb, weights, bias,
              muX, sigmaX,
              nTileRows, nTileCols,
              modeStr
      );
   }

   /**
    * Simple JSON parser for basic objects and arrays.
    * Not a full JSON parser - handles the structure we need for the test model.
    */
   private static Map<String, Object> parseSimpleJson(String json) {
      Map<String, Object> result = new HashMap<>();
      
      // Remove surrounding braces and whitespace
      json = json.trim();
      if (json.startsWith("{")) json = json.substring(1);
      if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
      
      String[] pairs = splitJsonPairs(json);
      for (String pair : pairs) {
         int colonIdx = pair.indexOf(':');
         if (colonIdx > 0) {
            String key = pair.substring(0, colonIdx).trim().replaceAll("^\"|\"$", "");
            String value = pair.substring(colonIdx + 1).trim();
            
            if (value.startsWith("[")) {
               // Parse array
               value = value.substring(1);
               if (value.endsWith("]")) value = value.substring(0, value.length() - 1);
               java.util.List<Double> list = new java.util.ArrayList<>();
               String[] elements = value.split(",");
               for (String elem : elements) {
                  String trimmed = elem.trim();
                  if (!trimmed.isEmpty()) {
                     list.add(Double.parseDouble(trimmed));
                  }
               }
               result.put(key, list);
            } else if (value.startsWith("\"")) {
               // String value
               result.put(key, value.replaceAll("^\"|\"$", ""));
            } else {
               // Try to parse as number
               try {
                  if (value.contains(".")) {
                     result.put(key, Double.parseDouble(value));
                  } else {
                     result.put(key, Integer.parseInt(value));
                  }
               } catch (NumberFormatException e) {
                  result.put(key, value);
               }
            }
         }
      }
      return result;
   }

   /**
    * Split JSON pairs while respecting nested structures.
    */
   private static String[] splitJsonPairs(String json) {
      java.util.List<String> pairs = new java.util.ArrayList<>();
      StringBuilder current = new StringBuilder();
      int bracketDepth = 0;
      int braceDepth = 0;
      
      for (char c : json.toCharArray()) {
         if (c == '[') bracketDepth++;
         else if (c == ']') bracketDepth--;
         else if (c == '{') braceDepth++;
         else if (c == '}') braceDepth--;
         else if (c == ',' && bracketDepth == 0 && braceDepth == 0) {
            pairs.add(current.toString());
            current = new StringBuilder();
            continue;
         }
         current.append(c);
      }
      if (current.length() > 0) {
         pairs.add(current.toString());
      }
      return pairs.toArray(new String[0]);
   }

   /**
    * Convert list to double array.
    */
   private static double[] parseDoubleArray(java.util.List<?> list) {
      if (list == null) return new double[0];
      double[] arr = new double[list.size()];
      for (int i = 0; i < list.size(); i++) {
         arr[i] = ((Number) list.get(i)).doubleValue();
      }
      return arr;
   }
}
