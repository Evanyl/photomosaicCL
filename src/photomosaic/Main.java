package photomosaic;

import java.io.File;
import java.util.HashMap;
import java.lang.Math;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.Rect;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


public class Main {

    // Size of squares to use
    private static int qualitySize = 25;
    private static String inPath = "./Resources/test1.png";
    private static String outPath = "./Resources/new1.jpg";
    private static final File dir = new File(System.getProperty("user.dir"));

    public static void main(String[] args) {
        // Load library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        /*
        // Open original image
        Mat orig = Imgcodecs.imread(inPath);


        // Resize image to multiple of square
        Mat resized = resize(orig, qualitySize);

        // Loop through pixels to calculate average color of square x square
        double[][][] colorMatrix = calculateAvgOrig(resized, qualitySize);

        // Crop source files into a folder, generates hash map with file names linked to average color array
        System.out.println("Now processing src imgs...");
        HashMap<String, double[]> colorIndex = processSrc();

        // Match source files to img
        String[][] closeImgMatrix = closestValueMatrix(colorMatrix, colorIndex);
        System.out.println("Rows: " + closeImgMatrix.length);
        System.out.println("Cols: " + closeImgMatrix[0].length);

        // Build output image
        System.out.println("Now building finished img...");
        Mat photomosaic = buildMosaic(closeImgMatrix, qualitySize, resized);

        // Write output image
        Imgcodecs.imwrite(outPath, photomosaic);

         */

        Mat largeImg = Imgcodecs.imread("./Resources/new1.jpg");
        Mat compressedImg = new Mat();
        Imgproc.resize(largeImg, compressedImg, new Size(largeImg.cols() / 5, largeImg.rows() / 5), 0, 0, Imgproc.INTER_AREA);

        Imgcodecs.imwrite("./Resources/compressedMosaic.jpg", compressedImg);

        System.out.println("Success!");

        /*// Create pixelated image
        Mat pixelated = pixelated(colorMatrix, resized, qualitySize);

        // Write image to check
        Imgcodecs.imwrite(outPath, pixelated); */
    }

    private static Mat shrink(Mat mat, int square) {

        Mat resized = new Mat();

        Imgproc.resize(mat, resized, new Size(square * 10, square * 10), 0, 0, Imgproc.INTER_AREA);
        return resized;
    }

    private static Mat templateGen(Mat template, int rows, int cols) {
        Mat resized = new Mat();

        Imgproc.resize(template, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_AREA);
        return resized;
    }

    /**Copies image found at toCopy to copyTo mat
     * @param copyTo
     * @param toCopy
     * @param x
     * @param y
     * @param square
     */
    private static void copyImg(Mat copyTo, String toCopy, int x, int y, int square) {
        Mat imgToCopy = shrink(Imgcodecs.imread("./Resources/processed/" + toCopy), square);

        for (int row = 0; row < square * 10; row++) {
            for (int col = 0; col < square * 10; col++) {
                copyTo.put(y + row, x + col, imgToCopy.get(row, col));
            }
        }
    }

    /** Builds image of images
     * @params closestMatrix
     * @return photomosaic image
     */
   private static Mat buildMosaic(String[][] closestMatrix, int square, Mat template) {
       Mat mosaic = templateGen(template, closestMatrix.length * square * 10, closestMatrix[0].length * square * 10);
       for (int row = 0; row < closestMatrix.length; row++) {
           for (int col = 0; col < closestMatrix[0].length; col++) {
               copyImg(mosaic, closestMatrix[row][col], col * square * 10, row * square * 10, square);
           }
       }

       return mosaic;
   }

    /** Uses pythagorean to calculate distance
     * @params point1
     * @params point2
     * @return distance as a double
     */
    private static double pyth3D(double[] point1, double[] point2) {
        return Math.sqrt(
                Math.pow((point2[0] - point1[0]), 2) +
                Math.pow((point2[1] - point1[1]), 2) +
                Math.pow((point2[2] - point1[2]), 2)
                );
    }

    /** Returns a matrix of string names that are the closest values
     * @params colors
     * @params colorMap
     * @return matrix of string names to files that are the closest values.
     */
    private static String[][] closestValueMatrix(double[][][] colors, HashMap<String, double[]> colorMap) {
        String[][] closestMatrix = new String[colors.length][colors[0].length];
        double lowest;
        double distance;
        String lowestKey;
        String key;

        for (int row = 0; row < colors.length; row++) {
            for (int col = 0; col < colors[0].length; col++) {
                lowest = 255;
                lowestKey = "img0";
                for (int i = 0; i < colorMap.size(); i++) {
                    key = "img" + i;
                    distance = pyth3D(colors[row][col], colorMap.get(key));
                    if (distance < lowest) {
                        lowest = distance;
                        lowestKey = key;
                    }
                }
                // Can add line here to remove the image from hashmap so it may not be used again.
                closestMatrix[row][col] = lowestKey + ".jpg";
            }
        }

        return closestMatrix;
    }

    /**
     * Crops the files in the src folder, and pastes them into the processed folder.
     */
    private static HashMap<String, double[]> processSrc() {
        File srcDir = new File(dir + "/Resources/src");
        File[] files = srcDir.listFiles();

        Mat orig;
        Mat cropped;
        int lowerRes;
        Rect cropSize;

        HashMap<String, double[]> srcMap = new HashMap<String, double[]>();

        for (int i = 0; i < files.length; i++) {
            orig = Imgcodecs.imread(files[i].getAbsolutePath());

            // Only images will be written. Rough implementation, can be changed in future.
            try {
                // Find side of lowest pixels
                if (orig.rows() < orig.cols()) {
                    lowerRes = orig.rows();
                } else {
                    lowerRes = orig.cols();
                }

                // Crop image into cropped
                cropSize = new Rect((orig.cols() - lowerRes) / 2, (orig.rows() - lowerRes) / 2, lowerRes, lowerRes);
                cropped = new Mat(orig, cropSize);

                Imgcodecs.imwrite("./Resources/processed/" + "img" + i + ".jpg", cropped);
                srcMap.put("img" + i, calculateAvg(cropped, 0, 0, lowerRes));
            } catch (Exception e) {
                // What happens when exception.
                System.out.println("Error on: " + i);
            }
        }

        return srcMap;
    }

    /**
     * Resizes the given image to a multiple of square x square
     *
     * @return An image that has resolution in a multiple of square x square
     * @params mat
     * @params square
     */
    private static Mat resize(Mat mat, int square) {
        int rows = mat.rows();
        int cols = mat.cols();

        rows -= rows % square;
        cols -= cols % square;

        Mat resized = new Mat();

        Imgproc.resize(mat, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_AREA);
        return resized;
    }

    /** Calculates average color of an image with a given size
     * @params image
     * @params startRow
     * @params startCol
     * @params square
     * @return an array that is the average color
     */
    private static double[] calculateAvg(Mat image, int startRow, int startCol, int square) {
        double red = 0;
        double green = 0;
        double blue = 0;

        double[] avgColor = new double[3];

        double[] color;

        // Loop through each pixel in square
        for (int i = 0; i < square; i++) {
            for (int j = 0; j < square; j++) {
                color = image.get(startRow + i, startCol + j);
                blue += color[0];
                green += color[1];
                red += color[2];
            }
        }

        avgColor[0] = blue / (square * square);
        avgColor[1] = green / (square * square);
        avgColor[2] = red / (square * square);

        return avgColor;
    }

    /**
     * Puts the average color of a square of size square of a given image into a 3d array
     *
     * @return a 3d array with average bgr values of square
     * @params image
     * @params square
     */
    private static double[][][] calculateAvgOrig(Mat image, int square) {

        // Loop through squares
        double[][][] colorMatrix = new double[image.rows() / square][image.cols() / square][3];

        for (int row = 0; row < image.rows(); row += square) {
            for (int col = 0; col < image.cols(); col += square) {
                colorMatrix[row / square][col / square] = calculateAvg(image, row, col, square);
            }
        }

        return colorMatrix;
    }

    /**
     * Returns pixelated image given average color matrix
     *
     * @return pixelated image
     * @params colors
     * @params orig
     * @params square
     */
    private static Mat pixelated(double[][][] colors, Mat orig, int square) {
        int rows = colors.length;
        int cols = colors[0].length;
        Mat image = orig;

        // Loop through each color
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Loop through a square to put colors.
                for (int i = 0; i < square; i++) {
                    for (int j = 0; j < square; j++) {
                        image.put(row * square + i, col * square + j, colors[row][col]);
                    }
                }
            }
        }

        return image;
    }
}
