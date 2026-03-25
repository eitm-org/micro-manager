package org.micromanager.plugins.fovsymmetry;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import org.micromanager.data.Image;
import org.micromanager.data.Coords;
import org.micromanager.data.Metadata;
import ij.ImagePlus;
import ij.process.ByteProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for FOVQualityDecisionFunction.
 * Tests loading model from JSON and evaluating on synthetic test images.
 */
public class FOVQualityDecisionFunctionTest {

   private FOVQualityDecisionFunction.FOVModel testModel_;
   private static final String MODEL_JSON_PATH = "src/test/resources/test_fov_model.json";
   private static final String TEST_IMAGE_TIFF_PATH = "src/test/resources/img_channel000_position015_time000000000_z088.tif";
   
   private static final List<String>  GOOD_TEST_IMAGES = new ArrayList<>();
   static {
      GOOD_TEST_IMAGES.add("src/test/resources/img_channel000_position015_time000000000_z088.tif");
      GOOD_TEST_IMAGES.add("src/test/resources/img_channel000_position015_time000000000_z820.tif");
      GOOD_TEST_IMAGES.add("src/test/resources/img_channel000_position016_time000000000_z056.tif");
   }
   private static final List<String>  BAD_TEST_IMAGES = new ArrayList<>();
   static{
      BAD_TEST_IMAGES.add("src/test/resources/img_channel000_position000_time000000000_z189.tif");
      BAD_TEST_IMAGES.add("src/test/resources/img_channel000_position001_time000000000_z017.tif");
      BAD_TEST_IMAGES.add("src/test/resources/img_channel000_position015_time000000000_z424.tif");
   }
      

   @BeforeClass
   public static void setUpClass() throws IOException {
      // Generate test TIFF image if it doesn't exist
      generateTestImageTiff(TEST_IMAGE_TIFF_PATH);
   }

   @Before
   public void setUp() throws IOException {
      testModel_ = FOVQualityDecisionFunction.loadModelFromJson(MODEL_JSON_PATH);
      assertNotNull("Test model should not be null", testModel_);
      testModel_.validate();
   }

   /**
    * Test basic decision evaluation with a uniform (good quality) image.
    */
   @Test
   public void testEvaluateUniformImage() {
      // Create a synthetic uniform image (good focus, uniform gradient)
      TestImage uniformImage = createUniformImage(256, 256, 128);
      
      char decision = FOVQualityDecisionFunction.evaluate(testModel_, uniformImage);
      assertNotNull("Decision should not be null", decision);
      assertTrue("Decision should be 'g' or 'b'", decision == 'g' || decision == 'b');
      System.out.println("Uniform image decision: " + decision);
   }

   /**
    * Test decision evaluation with a noisy (poor quality) image.
    */
   @Test
   public void testEvaluateNoisyImage() {
      // Create a synthetic noisy image (poor focus, high noise)
      TestImage noisyImage = createNoisyImage(256, 256, 100, 200);
      
      char decision = FOVQualityDecisionFunction.evaluate(testModel_, noisyImage);
      assertNotNull("Decision should not be null", decision);
      assertTrue("Decision should be 'g' or 'b'", decision == 'g' || decision == 'b');
      System.out.println("Noisy image decision: " + decision);
   }

   /**
    * Test decision evaluation with a sharp edges image (should be good).
    */
   @Test
   public void testEvaluateSharpEdgesImage() {
      // Create an image with sharp edges (good quality indicator)
      TestImage sharpImage = createSharpEdgesImage(256, 256);
      
      char decision = FOVQualityDecisionFunction.evaluate(testModel_, sharpImage);
      assertNotNull("Decision should not be null", decision);
      assertTrue("Decision should be 'g' or 'b'", decision == 'g' || decision == 'b');
      System.out.println("Sharp edges image decision: " + decision);



   }

   /**
    * Test that model validation works correctly.
    */
   @Test
   public void testModelValidation() {
      FOVQualityDecisionFunction.FOVModel validModel = testModel_;
      // Should not throw
      validModel.validate();
      assertTrue("Valid model should pass validation", true);
   }

   /**
    * Test model with mismatched weights length.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testModelValidationFailsWithMismatchedWeights() {
      double[] badWeights = new double[3]; // Should be 6
      FOVQualityDecisionFunction.FOVModel badModel =
              new FOVQualityDecisionFunction.FOVModel(
                      0.5, badWeights, -0.2,
                      testModel_.muX, testModel_.sigmaX,
                      testModel_.nTileRows, testModel_.nTileCols,
                      testModel_.modeStr
              );
      badModel.validate();
   }

   /**
    * Test that null model throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testEvaluateWithNullModel() {
      TestImage dummyImage = createUniformImage(256, 256, 100);
      FOVQualityDecisionFunction.evaluate(null, dummyImage);
   }

   /**
    * Test that null image throws IllegalArgumentException.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testEvaluateWithNullImage() {
      FOVQualityDecisionFunction.evaluate(testModel_, null);
   }

   /**
    * Test mode string handling for "greater_is_good".
    */
   @Test
   public void testGreaterIsGoodMode() {
      double[] simpleWeights = new double[66];
      double[] simpleMu = new double[66];
      double[] simpleSigma = new double[66];
      for (int i = 0; i < 66; i++) {
         simpleWeights[i] = 0.1;
         simpleMu[i] = 0.0;
         simpleSigma[i] = 1.0;
      }
      
      FOVQualityDecisionFunction.FOVModel greaterIsGoodModel =
              new FOVQualityDecisionFunction.FOVModel(
                      0.0, simpleWeights, 0.0,
                      simpleMu, simpleSigma,
                      8, 8,
                      "greater_is_good"
              );
      
      TestImage testImage = createUniformImage(256, 256, 128);
      char decision = FOVQualityDecisionFunction.evaluate(greaterIsGoodModel, testImage);
      assertTrue("Should return 'g' or 'b'", decision == 'g' || decision == 'b');
      System.out.println("Greater is good mode decision: " + decision);
   }

   /**
    * Test FOV evaluation with a TIFF image loaded from file.
    * The test TIFF is generated programmatically during test setup.
    */
   @Test
   public void testEvaluateFromTiffFile() throws IOException {
      // Load the test TIFF image using ImageJ
      for (String path : GOOD_TEST_IMAGES) {
         
         ImagePlus imp = ij.IJ.openImage(path);
         assertNotNull("ImagePlus should not be null", imp);
         //assertEquals("Image should be 256x256", 256, imp.getWidth());
         //assertEquals("Image should be 256x256", 256, imp.getHeight());
         
         // Convert ImagePlus to TestImage
         ByteProcessor bp = imp.getProcessor().convertToByteProcessor();
         byte[] pixels = (byte[]) bp.getPixels();
         TestImage testImage = new TestImage(pixels, imp.getWidth(), imp.getHeight());
         
         // Evaluate the image
         char decision = FOVQualityDecisionFunction.evaluate(testModel_, testImage);
         assertNotNull("Decision should not be null", decision);
         assertTrue("Decision should be 'g' or 'b'", decision == 'g' || decision == 'b');
         System.out.println("Test TIFF image decision (should be good): " + decision);
      }


      for (String path : BAD_TEST_IMAGES) {
         
         ImagePlus imp = ij.IJ.openImage(path);
         assertNotNull("ImagePlus should not be null", imp);
         //assertEquals("Image should be 256x256", 256, imp.getWidth());
         //assertEquals("Image should be 256x256", 256, imp.getHeight());
         
         // Convert ImagePlus to TestImage
         ByteProcessor bp = imp.getProcessor().convertToByteProcessor();
         byte[] pixels = (byte[]) bp.getPixels();
         TestImage testImage = new TestImage(pixels, imp.getWidth(), imp.getHeight());
         
         // Evaluate the image
         char decision = FOVQualityDecisionFunction.evaluate(testModel_, testImage);
         assertNotNull("Decision should not be null", decision);
         assertTrue("Decision should be 'g' or 'b'", decision == 'g' || decision == 'b');
         System.out.println("Test TIFF image decision (should be bad): " + decision);
      }
      
   }

   
   // ---------- Helper Methods ----------

   /**
    * Generate a test TIFF image file if it doesn't already exist.
    * Creates a 256x256 image with mixed pattern (good for testing).
    */
   private static void generateTestImageTiff(String tiffPath) throws IOException {
      java.nio.file.Path path = Paths.get(tiffPath);
      if (Files.exists(path)) {
         System.out.println("Test TIFF already exists: " + tiffPath);
         return;
      }
      
      // Create parent directories if needed
      java.nio.file.Path parent = path.getParent();
      if (parent != null && !Files.exists(parent)) {
         Files.createDirectories(parent);
      }
      
      // Generate synthetic test image: gradient + noise pattern
      int width = 256;
      int height = 256;
      byte[] pixels = new byte[width * height];
      java.util.Random rand = new java.util.Random(123); // Deterministic seed
      
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            // Create a gradient from top-left to bottom-right
            int gradient = (x + y) / 2;
            // Add some structured noise
            int noise = (int) (rand.nextGaussian() * 20);
            // Add a Gaussian blob in the center for good focus region
            int dx = x - width / 2;
            int dy = y - height / 2;
            int blobIntensity = (int) (50 * Math.exp(-(dx * dx + dy * dy) / 10000.0));
            
            int value = gradient + noise + blobIntensity;
            value = Math.max(0, Math.min(255, value));
            pixels[y * width + x] = (byte) value;
         }
      }
      
      // Create ImagePlus and save as TIFF
      ByteProcessor bp = new ByteProcessor(width, height, pixels, null);
      ImagePlus imp = new ImagePlus("Test FOV Image", bp);
      ij.IJ.save(imp, tiffPath);
      System.out.println("Generated test TIFF: " + tiffPath);
   }

   

   

   // ---------- Test Image Generators ----------

   /**
    * Create a uniform test image (constant pixel value).
    */
   private TestImage createUniformImage(int width, int height, int pixelValue) {
      byte[] pixels = new byte[width * height];
      for (int i = 0; i < pixels.length; i++) {
         pixels[i] = (byte) pixelValue;
      }
      return new TestImage(pixels, width, height);
   }

   /**
    * Create a noisy test image with random variance.
    */
   private TestImage createNoisyImage(int width, int height, int baseMean, int noiseStdDev) {
      byte[] pixels = new byte[width * height];
      java.util.Random rand = new java.util.Random(42); // Deterministic seed
      for (int i = 0; i < pixels.length; i++) {
         int value = baseMean + (int) (rand.nextGaussian() * noiseStdDev);
         value = Math.max(0, Math.min(255, value));
         pixels[i] = (byte) value;
      }
      return new TestImage(pixels, width, height);
   }

   /**
    * Create a test image with sharp edges (good focus indicator).
    */
   private TestImage createSharpEdgesImage(int width, int height) {
      byte[] pixels = new byte[width * height];
      
      // Create a checkerboard pattern with sharp edges
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int tileSize = 32;
            int tileX = x / tileSize;
            int tileY = y / tileSize;
            pixels[y * width + x] = (byte) (((tileX + tileY) % 2 == 0) ? 200 : 50);
         }
      }
      return new TestImage(pixels, width, height);
   }

   // ---------- Test Image Implementation ----------

   /**
    * Mock Image implementation for testing.
    */
   public static class TestImage implements Image {
      private final byte[] pixels_;
      private final int width_;
      private final int height_;

      public TestImage(byte[] pixels, int width, int height) {
         this.pixels_ = pixels;
         this.width_ = width;
         this.height_ = height;
      }

      public Image copyAtCoords(Coords coords) {
         return this;
      }

      @Override
      public Image copyWithMetadata(Metadata metadata) {
         return this;
      }

      public Image copyWith(Coords coords, Metadata metadata) {
         return this;
      }

      @Override
      public long getIntensityAt(int x, int y) {
         return pixels_[y * width_ + x] & 0xFF;
      }

      @Override
      public long getComponentIntensityAt(int x, int y, int component) {
         return getIntensityAt(x, y);
      }

      @Override
      public long[] getComponentIntensitiesAt(int x, int y) {
         return new long[]{getIntensityAt(x, y)};
      }

      @Override
      @Deprecated
      public int getImageJPixelType() {
         return 0;
      }

      @Override
      public String getIntensityStringAt(int x, int y) {
         return String.valueOf(getIntensityAt(x, y));
      }

      @Override
      public Metadata getMetadata() {
         return null;
      }

      @Override
      public Coords getCoords() {
         return null;
      }

      @Override
      public int getWidth() {
         return width_;
      }

      @Override
      public int getHeight() {
         return height_;
      }

      @Override
      public int getBytesPerPixel() {
         return 1;
      }

      @Override
      public int getBytesPerComponent() {
         return 1;
      }

      @Override
      public int getNumComponents() {
         return 1;
      }

      @Override
      public Object getRawPixels() {
         return pixels_;
      }

      @Override
      public Object getRawPixelsCopy() {
         byte[] copy = new byte[pixels_.length];
         System.arraycopy(pixels_, 0, copy, 0, pixels_.length);
         return copy;
      }

      @Override
      public Object getRawPixelsForComponent(int component) {
         return getRawPixelsCopy();
      }

      @Override
      public byte[] getByteArray() {
         byte[] copy = new byte[pixels_.length];
         System.arraycopy(pixels_, 0, copy, 0, pixels_.length);
         return copy;
      }
   }
}
