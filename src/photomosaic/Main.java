package photomosaic;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Scalar;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {

    // Size of squares to use
    private static int square = 50;
    private static String inPath = "./Resources/test1.png";
    private static String outPath = "./Resources/new.jpg";

    public static void main(String[] args) {
        // Load library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Open original image
        Mat orig = Imgcodecs.imread(inPath);

        // Resize image to multiple of square
        Mat resized = resize(orig);

        // Loop through pixels to calculate average color of square x square
        double[][][] colorMatrix = calculateAvg(resized);

        // Create pixelated image
        Mat pixelated = pixelated(colorMatrix, resized);

        // Write image to check
        Imgcodecs.imwrite(outPath, pixelated);

        System.out.println("Success!");
    }

    /** Resizes the given image to a multiple of square x square
     * @params mat
     * @return An image that has resolution in a multiple of square x square
     */
    private static Mat resize(Mat mat) {
        int rows = mat.rows();
        int cols = mat.cols();

        rows -= rows % square;
        cols -= cols % square;

        Mat resized = new Mat();

        Imgproc.resize(mat, resized, new Size(cols, rows), 0, 0, Imgproc.INTER_AREA);
        return resized;
    }

    /** Puts the average color of a square of size square of a given image into a 3d array
     * @params image
     * @return a 3d array with average bgr values of square
     */
    private static double[][][] calculateAvg(Mat image) {
        // Loop through squares
        double[] color;
        double[][][] colorMatrix = new double[image.rows() / square][image.cols() / square][3];

        double red;
        double green;
        double blue;

        double avgRed;
        double avgGreen;
        double avgBlue;

        for (int row = 0; row < image.rows(); row += square) {
            for (int col = 0; col < image.cols(); col += square) {

                // Loop through each pixel in square
                red = 0;
                green = 0;
                blue = 0;
                for (int i = 0; i < square; i++) {
                    for (int j = 0; j < square; j++) {
                        color = image.get(row + i, col + j);
                        blue += color[0];
                        green += color[1];
                        red += color[2];
                    }
                }

                avgBlue = blue / (square * square);
                avgGreen = green / (square * square);
                avgRed = red / (square * square);
                colorMatrix[row / square][col / square] = new double[] {avgBlue, avgGreen, avgRed};
            }
        }

        return colorMatrix;
    }

    /** Returns pixelated image given average color matrix
     * @params colors
     * @params orig
     * @return pixelated image
     */
    private static Mat pixelated(double[][][] colors, Mat orig) {
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

    /** Prints size of given image
     * @params mat
     */
    private static void printRes(Mat mat) {
        int length = mat.cols();
        int height = mat.rows();
        System.out.println("Size: " + length + " X " + height);
    }
}
