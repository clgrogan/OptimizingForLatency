import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessingApp {
    /* Create an image processor which changes a color into another color.
        1. Split the three colors of the pixel into the RGB components.
            Color is actually argb in this case. So four bytes with each color or transparency taking up a
            byte in the sequence.
     */
    public static final String SOURCE_FILE = "./resources/many-flowers.jpg";
    public static final String TARGET_FILE = "./out/many-purple-flowers.jpg";
    public static final String MULTI_TARGET_FILE = "./out/many-purple-flowers-multi.jpg";
    public  static final int numberOfThreads = 3;

    public static void main(String[] args) throws IOException {

        BufferedImage originalImage = ImageIO.read(new File(SOURCE_FILE));
        BufferedImage modifiedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.TYPE_INT_RGB);

        long startTime = System.currentTimeMillis();

        recolorSingleThreaded(originalImage, modifiedImage);

        String sDuration = String.valueOf(System.currentTimeMillis() - startTime);
        System.out.println("\nSingle Thread Executes in: " + sDuration + " milliseconds.");

        File outputFile = new File(TARGET_FILE);
        ImageIO.write(modifiedImage, "jpg",outputFile);

        for (int i = 1; i < 5; i++) {
            startTime = System.currentTimeMillis();

            recolorMultiThreaded(originalImage,modifiedImage,i);

            sDuration = String.valueOf(System.currentTimeMillis() - startTime);
            System.out.println("\nMultithread (threads: "+i+") Executes in: " + sDuration + " milliseconds.");
            ImageIO.write(modifiedImage, "jpg",new File(MULTI_TARGET_FILE));
        }
    }

    public static void recolorSingleThreaded (BufferedImage biSource, BufferedImage biTarget){
        recolorImage(biSource, biTarget, 0,0,biSource.getWidth(), biSource.getHeight());
    }

    public static void recolorMultiThreaded (BufferedImage originalImage, BufferedImage modifiedImage, int numberOfThreads){
        /*
            - Use a list for the thread instances.
            - Split the work amongst the threads dividing the image into sections horizontally.
            - each thread will recolor a section of the image.
         */

        List<Thread> threads = new ArrayList<>();
        int width = originalImage.getWidth();
        int height = originalImage.getHeight() / numberOfThreads;

        for (int i = 0; i < numberOfThreads; i++) {
            int threadMultiplier = i;
            Thread thread = new Thread(() ->{
                int leftCorner = 0;
                int topCorner = height * threadMultiplier;
                recolorImage(originalImage,modifiedImage,leftCorner,topCorner,width,height);
            });
            threads.add(thread);
        }
        threads.stream().forEach( thread -> thread.start());

        threads.stream().forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void recolorPixel(BufferedImage biSource, BufferedImage biTarget, int x, int y){
        int rgb = biSource.getRGB(x,y);

        int red = getRed(rgb);
        int green = getGreen(rgb);
        int blue = getBlue(rgb);

        int newRed;
        int newGreen;
        int newBlue;

        if (isShadeOfGray(red, green, blue)){
            newRed = Math.min(255, red + 10);
            newGreen = Math.max(0, green -80);
            newBlue = Math.max(0, blue -20);
        } else {
            newRed = red;
            newGreen = green;
            newBlue = blue;
        }

        int newRgb = createRgbFromColors(newRed,newGreen,newBlue);

        setTargetRgb(biTarget, x, y, newRgb);

    }

    public static void setTargetRgb (BufferedImage biTarget, int x, int y, int rgb){
        biTarget.getRaster().setDataElements(x, y, biTarget.getColorModel().getDataElements(rgb,null));
    }

    public static void recolorImage(BufferedImage biSource, BufferedImage biTarget,
                                    int leftCorner, int topCorner, int width, int height){
        for (int x = leftCorner; x  < leftCorner + width && x < biSource.getWidth(); x++) {
            for (int y = topCorner; y < topCorner + height && y < biTarget.getHeight(); y++) {
                recolorPixel(biSource, biTarget, x, y);
            }
        }
    }

    public static boolean isShadeOfGray(int red, int green, int blue){
        return Math.abs(red - green) < 30
                && Math.abs(red - blue) < 30
                && Math.abs(green - blue) < 30;
    }

    public static int createRgbFromColors(int red, int green, int blue){
        int rgb = 0;
        int alpha = 0xFF000000;
        rgb |= alpha;
        rgb |= red << 16;
        rgb |= green << 8;
        rgb |= blue;

        return rgb;
    }

    public static int getRed(int rgb){
        // Red is the second byte so extract it into a new  value to represent red value
        // by shifting it to the right of the resulting number.
        return (rgb & 0x00FF0000) >> 16;
    }

    public static int getGreen(int rgb){
        return (rgb & 0x0000FF00) >> 8;
    }

    public static int getBlue(int rgb){
        return (rgb & 0x000000FF);
    }
}
