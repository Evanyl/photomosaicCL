package photomosaic;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Scalar;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello world");
    }

    /*
     * @params mat
     *
     * Prints size of Mat given
     */
    private static void printRes(Mat mat) {
        int length = mat.cols();
        int height = mat.rows();
        System.out.println("Size: " + length + " X " + height);
    }

    private static void drawRect() {
        // Load openCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Open and store original image
        Mat orig = Imgcodecs.imread("./Resources/orig.jpg");
        printRes(orig);

        // Draw the rectangle
        Imgproc.rectangle(orig,
                new Point(orig.cols() / 2 - 350, orig.rows() / 2 - 350),
                new Point(orig.cols() / 2 + 350, orig.rows() / 2 + 350),
                new Scalar(255, 0, 0),
                5
        );

        // Write image
        Imgcodecs.imwrite("./Resources/new.jpg", orig);
    }

    private static void shrink() {
        // Load openCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Open original image
        Mat original = Imgcodecs.imread("./Resources/test.jpeg");
        printRes(original);

        // Create new image to be written
        Mat shrinked = new Mat();

        // Resize image
        Imgproc.resize(original, shrinked, new Size(original.cols(), original.rows()), 0, 0, Imgproc.INTER_AREA);
        printRes(shrinked);

        // Write new image
        Imgcodecs.imwrite("./Resources/new.jpg", shrinked);
    }

    private static void rotate180() {
        // Load openCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Open original image
        Mat orig = Imgcodecs.imread("./Resources/new.jpg");

        // Create matrix to store result
        Mat dst = new Mat();

        // Creating transformation matrix
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(orig.cols() / 2, orig.rows() / 2), 180, 1);

        // Rotate image
        Imgproc.warpAffine(orig, dst, rotationMatrix, new Size(orig.cols(), orig.rows()));

        // Write image
        Imgcodecs.imwrite("./Resources/orig.jpg", dst);
    }
}
