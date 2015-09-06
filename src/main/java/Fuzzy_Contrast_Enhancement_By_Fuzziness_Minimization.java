import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * @author Vladimir Nizamutdinov (astartes91@gmail.com)
 * Contrast enhancement ImageJ plugin
 * Implemented on the basis of fuzziness minimization algorithm:
 * http://www.ijstm.com/images/short_pdf/1419414679_P154-160.pdf
 */
public class Fuzzy_Contrast_Enhancement_By_Fuzziness_Minimization implements PlugInFilter {
    @Override
    public int setup(String s, ImagePlus imagePlus) {
        return DOES_8G + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        GenericDialog gd = new GenericDialog("Algorithm settings");
        gd.addNumericField("Fuzzy exponent:", 2, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("Plugin run cancelled!");
            return;
        }

        int width = imageProcessor.getWidth(), height =imageProcessor.getHeight();
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                int value = imageProcessor.get(columnIndex, rowIndex);

                if (value > max) {
                    max = value;
                }
                if (value < min) {
                    min = value;
                }
            }
        }

        double fuzzyExponent = gd.getNextNumber();
        double crossoverPoint = min + ((max-min+1)/2);
        double fuzzyDenominator = (max - crossoverPoint) / (Math.pow(2, (1 / fuzzyExponent)) - 1);
        //minimum allowed membership value
        double alpha = Math.pow(1.0 + (max - min) / fuzzyDenominator, -fuzzyExponent);

        /********************************************** Fuzzification *********************************************/
        double[][] membershipValuesMatrix = new double[height][width];
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                int value = imageProcessor.get(columnIndex, rowIndex);

                membershipValuesMatrix[rowIndex][columnIndex] = (Math.pow((1 + ((max - value) / fuzzyDenominator)),
                        -fuzzyExponent));
            }
        }
        /******************************************* End of Fuzzification ******************************************/

        /***************************** Fuzzy Intensification *******************************/
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                if (membershipValuesMatrix[rowIndex][columnIndex] <= 0.5) {
                    membershipValuesMatrix[rowIndex][ columnIndex] = 2 * Math.pow(
                            membershipValuesMatrix[rowIndex][columnIndex], 2);
                } else {
                    membershipValuesMatrix[rowIndex][columnIndex] = 1 - 2 * (Math.pow(1 -
                            membershipValuesMatrix[rowIndex][columnIndex], 2));
                }

                if (membershipValuesMatrix[rowIndex][columnIndex] < alpha){
                    membershipValuesMatrix[rowIndex][columnIndex] = alpha;
                }
            }
        }
        /************************** End of Fuzzy Intensification *************************/

        ImagePlus enhancedImagePlus = NewImage.createByteImage("Enhanced image", width, height, 1,
                NewImage.FILL_BLACK);
        ImageProcessor enhancedImageProcessor = enhancedImagePlus.getProcessor();

        /*********************************************** Defuzzification *********************************************/
        for (int rowIndex = 0; rowIndex < height; rowIndex++)
        {
            for (int columnIndex = 0; columnIndex < width; columnIndex++)
            {
                int value = (int) (max - (fuzzyDenominator * ((Math.pow(membershipValuesMatrix[rowIndex][columnIndex],
                        (-1/fuzzyExponent))) - 1)));

                //if (color < 0) color = min;
                enhancedImageProcessor.set(columnIndex, rowIndex, value);
            }
        }
        /******************************************* End of Defuzzification *****************************************/

        enhancedImagePlus.show();
    }
}
