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
    private static final int qualitySize = 40;
    private static final String inPath = "./Resources/test.jpg";
    private static final String outPath = "./Resources/mosaic.jpg";
    private static final String srcPath = "./Resources/src/";
    private static final String cropPath = "./Resources/processed/";
    private static final int upscale = 1;

    public static void main(String[] args) {
        // Load library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

         createMosaic(inPath, outPath);

    }

    /** Generates a photomosaic.
     *
     * @param inputPath
     * @param outputPath
     */
    private static void createMosaic(String inputPath, String outputPath) {
        /* -------Handle original image------- */
        // Resize original image to multiple of square
        Mat resized = resize(Imgcodecs.imread(inPath), qualitySize);

        // Loop through pixels to calculate average color of square x square quadrants
        double[][][][] colorMatrix = generateAvgArray(resized, qualitySize);

        /* -------Handle source images------- */
        // Crop source files into a folder, generates hash map with file names linked to average color array
        System.out.println("Now processing src imgs...");
        HashMap<String, double[][]> colorIndex = processSrc();

        // Match source files to img
        System.out.println("Matching...");
        String[][] closeImgMatrix = closestValueMatrix(colorMatrix, colorIndex);
        System.out.println("Rows: " + closeImgMatrix.length);
        System.out.println("Cols: " + closeImgMatrix[0].length);

        // Build output image
        System.out.println("Now building finished img...");
        Mat photomosaic = buildMosaic(closeImgMatrix, qualitySize, resized, upscale);

        // Write output image
        Imgcodecs.imwrite(outPath, photomosaic);

        System.out.println("Success!");
    }

    /**
     * Resizes the given image to a multiple of square x square
     * @params mat
     * @params square
     * @return An image that has resolution in a multiple of square x square
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

    /**
     * Puts the average color of a square of size square of a given image into a 3d array
     *
     * @return a 3d array with average bgr values of square
     * @params image
     * @params square
     */
    private static double[][][][] generateAvgArray(Mat image, int square) {
        // Initialize matrix
        double[][][][] colorMatrix = new double[image.rows() / square][image.cols() / square][4][3];

        // Loop through squares to calculate avg color
        for (int row = 0; row < image.rows(); row += square) {
            for (int col = 0; col < image.cols(); col += square) {
                colorMatrix[row / square][col / square][0] = calculateAvgSquare(image, row, col, square / 2);
                colorMatrix[row / square][col / square][1] = calculateAvgSquare(image, row, col + square / 2, square / 2);
                colorMatrix[row / square][col / square][2] = calculateAvgSquare(image, row + square / 2, col + square / 2, square / 2);
                colorMatrix[row / square][col / square][3] = calculateAvgSquare(image, row + square / 2, col, square / 2);
            }
        }



        return colorMatrix;
    }

    /** Returns a hashmap of file names to average colors.
     *
     * @return hashmap of file names to average colors.
     */
    private static HashMap<String, double[][]> processSrc() {
        File srcDir = new File(srcPath);
        File[] files = srcDir.listFiles();

        Mat orig;
        Mat cropped;
        int lowerRes;
        Rect cropSize;

        HashMap<String, double[][]> srcMap = new HashMap<String, double[][]>();
        double[][] avgColorQuadrant = new double[4][3];

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

                // Crop image into centered square
                cropped = new Mat(orig,  new Rect((orig.cols() - lowerRes) / 2, (orig.rows() - lowerRes) / 2, lowerRes, lowerRes));

                Imgcodecs.imwrite(cropPath + "img" + i + ".jpg", cropped);

                avgColorQuadrant[0] = calculateAvgSquare(cropped, 0, 0, lowerRes / 2);
                avgColorQuadrant[1] = calculateAvgSquare(cropped, 0, lowerRes / 2, lowerRes / 2);
                avgColorQuadrant[2] = calculateAvgSquare(cropped, lowerRes / 2, lowerRes / 2, lowerRes / 2);
                avgColorQuadrant[3] = calculateAvgSquare(cropped, lowerRes / 2, 0, lowerRes / 2);

                srcMap.put("img" + i, new double[][] {avgColorQuadrant[0], avgColorQuadrant[1], avgColorQuadrant[2], avgColorQuadrant[3]});
            } catch (Exception e) {
                // What happens when exception.
                System.out.println(files[i].getAbsolutePath());
                System.out.println(e);
                i--;
            }
        }

        return srcMap;
    }

    /** Returns a matrix of string names that are the closest values
     * @params colors
     * @params colorMap
     * @return matrix of string names to files that are the closest values.
     */
    private static String[][] closestValueMatrix(double[][][][] colors, HashMap<String, double[][]> colorMap) {
        String[][] closestMatrix = new String[colors.length][colors[0].length];
        double closest;
        double closeness;
        String lowestKey;
        String key;

        for (int row = 0; row < colors.length; row++) {
            for (int col = 0; col < colors[0].length; col++) {
                closest = 255;
                lowestKey = "img0";
                for (int i = 0; i < colorMap.size(); i++) {
                    key = "img" + i;
                    closeness = calculateCloseness(colors[row][col], colorMap.get(key));
                    if (closeness < closest) {
                        closest = closeness;
                        lowestKey = key;
                    }
                }
                // Can add line here to remove the image from hashmap so it may not be used again.
                closestMatrix[row][col] = lowestKey + ".jpg";
            }
        }

        return closestMatrix;
    }

    private static double calculateCloseness(double[][] color1, double[][] test) {
        double sum = 0;

        for (int i = 0; i < 4; i++) {
            sum += pyth3D(color1[i], test[i]);
        }


        return sum / 4;
    }

    /** Builds image of images
     * @params closestMatrix
     * @return photomosaic image
     */
    private static Mat buildMosaic(String[][] closestMatrix, int square, Mat template, int upscale) {
        Mat mosaic = templateGen(template, closestMatrix.length * square * upscale, closestMatrix[0].length * square * upscale);
        for (int row = 0; row < closestMatrix.length; row++) {
            for (int col = 0; col < closestMatrix[0].length; col++) {
                copyImg(mosaic, closestMatrix[row][col], col * square * upscale, row * square * upscale, square * upscale);
            }
        }

        return mosaic;
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
     * @param srcResolution
     */
    private static void copyImg(Mat copyTo, String toCopy, int x, int y, int srcResolution) {
        Mat imgToCopy = shrink(Imgcodecs.imread("./Resources/processed/" + toCopy), srcResolution);

        for (int row = 0; row < srcResolution; row++) {
            for (int col = 0; col < srcResolution; col++) {
                copyTo.put(y + row, x + col, imgToCopy.get(row, col));
            }
        }
    }

    private static Mat shrink(Mat mat, int srcResolution) {

        Mat resized = new Mat();

        Imgproc.resize(mat, resized, new Size(srcResolution, srcResolution), 0, 0, Imgproc.INTER_AREA);
        return resized;
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

    /** Calculates average color of an image with a given size
     * @params image
     * @params startRow
     * @params startCol
     * @params square
     * @return an array that is the average color
     */
    private static double[] calculateAvgSquare(Mat image, int startRow, int startCol, int square) {
        double red = 0;
        double green = 0;
        double blue = 0;

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

        // Returns average BGR values
        double[] avgColor = {blue / (square * square), green / (square * square), red / (square * square)};

        return avgColor;
    }
}
