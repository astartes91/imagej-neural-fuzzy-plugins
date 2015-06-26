import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * @author Vladimir Nizamutdinov
 * Fuzzy logic based edge detection plugin for ImageJ
 * Realised on the basis of Suliman et. al algorithm (2011)
 * astartes91@gmail.com
 */
public class Fuzzy_Edge_Detection_By_Suliman_And_Others implements PlugInFilter {

    private static int BACKGROUND_CLASS_0 = 0;
    private static int EDGE_CLASS_1 = 1;
    private static int EDGE_CLASS_2 = 2;
    private static int EDGE_CLASS_3 = 3;
    private static int EDGE_CLASS_4 = 4;
    private static int NOISY_EDGE_CLASS_5 = 5;

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        return DOES_8G + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        GenericDialog gd = new GenericDialog("Algorithm settings");
        gd.addNumericField("Low value:", 4, 0);
        gd.addNumericField("High value:", 48, 0);
        gd.addNumericField("Weight:", 240, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("Plugin run cancelled!");
            return;
        }

        //long startTime = System.nanoTime();
        int lowValue = (int) gd.getNextNumber();
        int highValue = (int) gd.getNextNumber();
        int weight = (int) gd.getNextNumber();

        int[] backgroundClass0CenterVector = {lowValue, lowValue, lowValue, lowValue};
        int[] edgeClass1CenterVector = {lowValue, highValue, highValue, highValue};
        int[] edgeClass2CenterVector = {highValue, lowValue, highValue, highValue};
        int[] edgeClass3CenterVector = {highValue, highValue, lowValue, highValue};
        int[] edgeClass4CenterVector = {highValue, highValue, highValue, lowValue};
        int[] noisyEdgeClass5CenterVector = {highValue, highValue, highValue, highValue};

        int[][] centersOfClasses = {backgroundClass0CenterVector, edgeClass1CenterVector,
                edgeClass2CenterVector, edgeClass3CenterVector, edgeClass4CenterVector,
                noisyEdgeClass5CenterVector};

        int width = imageProcessor.getWidth();
        int height = imageProcessor.getHeight();

        int inputVector[][][] = new int[height][width][4];
        int classes[][] = new int[height][width];

        pixelClassification(imageProcessor, inputVector, classes, centersOfClasses, weight);

        ImagePlus edgeImagePlus = getEdgeImage(inputVector, classes, width, height);
        ImageProcessor edgeImageProcessor = edgeImagePlus.getProcessor();

        ImagePlus finalImagePlus = getFinalImage(edgeImageProcessor);

       // long endTime = System.nanoTime();

        finalImagePlus.show();
        //IJ.log(Double.valueOf((endTime - startTime)/1000000000.0).toString());
    }

    private void pixelClassification(ImageProcessor sourceImageProcessor, int inputVector[][][], int classes[][],
                                     int[][] centersOfClasses, int weight) {
        int width = sourceImageProcessor.getWidth();
        int height = sourceImageProcessor.getHeight();

        /*ImagePlus classificationImagePlus = NewImage.createByteImage("Classification", width, height, 1,
                NewImage.FILL_BLACK);
        ImageProcessor classificationImageProcessor = classificationImagePlus.getProcessor();*/

        //todo: для более быстрой обработки можно применить доступ через массив
        for (int rowIndex = 0; rowIndex < height; rowIndex++){
            for (int columnIndex = 0; columnIndex < width; columnIndex++){
                int neighbours[][] = new int[3][3];
                for (int relativeRowIndex = -1; relativeRowIndex < 2; relativeRowIndex++){
                    for (int relativeColumnIndex = -1; relativeColumnIndex < 2; relativeColumnIndex++){
                        int xCoordinate = columnIndex + relativeColumnIndex;
                        int yCoordinate = rowIndex + relativeRowIndex;

                        if (xCoordinate < 0){
                            xCoordinate = 0;
                        }
                        if (xCoordinate > width - 1){
                            xCoordinate = width - 1;
                        }
                        if (yCoordinate < 0){
                            yCoordinate = 0;
                        }
                        if (yCoordinate > height - 1){
                            yCoordinate = height - 1;
                        }
                        neighbours[relativeRowIndex + 1][relativeColumnIndex + 1] = sourceImageProcessor.get(
                                xCoordinate, yCoordinate);
                    }
                }

                int p1 = neighbours[0][0];
                int p2 = neighbours[0][1];
                int p3 = neighbours[0][2];
                int p4 = neighbours[1][0];
                int p5 = neighbours[1][1];
                int p6 = neighbours[1][2];
                int p7 = neighbours[2][0];
                int p8 = neighbours[2][1];
                int p9 = neighbours[2][2];

                inputVector[rowIndex][columnIndex][0] = Math.abs(p1 - p5) +  Math.abs(p9 - p5);
                inputVector[rowIndex][columnIndex][1] = Math.abs(p2 - p5) +  Math.abs(p8 - p5);
                inputVector[rowIndex][columnIndex][2] = Math.abs(p3 - p5) +  Math.abs(p7 - p5);
                inputVector[rowIndex][columnIndex][3] = Math.abs(p4 - p5) +  Math.abs(p6 - p5);

                /** array of classes' membership function values for particular pixels **/
                double[] membershipFunctionValuesArray = new double[6];
                for (int centerOfClassesIndex = 0; centerOfClassesIndex < centersOfClasses.length;
                     centerOfClassesIndex++){
                    int[] differencesArray = new int[4];
                    int sumOfSquares = 0;

                    for (int differencesIndex = 0; differencesIndex < differencesArray.length; differencesIndex++){
                        differencesArray[differencesIndex] = inputVector[rowIndex][columnIndex][differencesIndex] -
                                centersOfClasses[centerOfClassesIndex][differencesIndex];
                        sumOfSquares += differencesArray[differencesIndex] * differencesArray[differencesIndex];
                    }

                    double norm = Math.sqrt(sumOfSquares);
                    membershipFunctionValuesArray[centerOfClassesIndex] = Math.max(0, 1 - norm/weight);
                }

                int maxIndex = -1;
                double maxValue = Integer.MIN_VALUE;
                for (int membershipFunctionValuesIndex = 0; membershipFunctionValuesIndex <
                        membershipFunctionValuesArray.length; membershipFunctionValuesIndex++) {
                    if (membershipFunctionValuesArray[membershipFunctionValuesIndex] > maxValue){
                        maxIndex = membershipFunctionValuesIndex;
                        maxValue = membershipFunctionValuesArray[membershipFunctionValuesIndex];
                    }
                }

                classes[rowIndex][columnIndex] = maxIndex;

                /*if(maxIndex == BACKGROUND_CLASS_0) {
                    classificationImageProcessor.set(columnIndex, rowIndex, 0);
                } else {
                    classificationImageProcessor.set(columnIndex, rowIndex, 255);
                }*/
            }
        }

        //classificationImagePlus.show();
    }

    private ImagePlus getEdgeImage(int inputVector[][][], int classes[][], int width, int height) {
        ImagePlus edgeImagePlus = NewImage.createByteImage("Intermediary", width, height, 1,
                NewImage.FILL_BLACK);
        ImageProcessor edgeImageProcessor = edgeImagePlus.getProcessor();

        for (int rowIndex = 0; rowIndex < height; rowIndex++){
            for (int columnIndex = 0; columnIndex < width; columnIndex++){
                int pixelValue;
                pixelValue = 0;
                if (classes[rowIndex][columnIndex] == NOISY_EDGE_CLASS_5){
                    pixelValue = 255;
                }else if (classes[rowIndex][columnIndex] == EDGE_CLASS_1){
                    int index = 2;
                    int d3Value = inputVector[rowIndex][columnIndex][index];
                    int d3Neighbour1Value = Integer.MIN_VALUE;

                    int d3Neighbour1Row = rowIndex - 1;
                    int d3Neighbour1Column = columnIndex + 1;

                    if (d3Neighbour1Row >= 0 && d3Neighbour1Column < width){
                        d3Neighbour1Value = inputVector[d3Neighbour1Row][d3Neighbour1Column][index];
                    }

                    int d3Neighbour2Value = Integer.MIN_VALUE;

                    int d3Neighbour2Row = rowIndex + 1;
                    int d3Neighbour2Column = columnIndex - 1;

                    if (d3Neighbour2Row < height && d3Neighbour2Column >= 0){
                        d3Neighbour2Value = inputVector[d3Neighbour2Row][d3Neighbour2Column][index];
                    }

                    if (d3Value > d3Neighbour1Value && d3Value > d3Neighbour2Value){
                        pixelValue = 255;
                    }else{
                        pixelValue = 0;
                    }
                }else if (classes[rowIndex][columnIndex] == EDGE_CLASS_2){
                    int index = 3;
                    int d4Value = inputVector[rowIndex][columnIndex][index];
                    int d4Neighbour1Value = Integer.MIN_VALUE;

                    int d4Neighbour1Column = columnIndex - 1;

                    if (d4Neighbour1Column >= 0){
                        d4Neighbour1Value = inputVector[rowIndex][d4Neighbour1Column][index];
                    }

                    int d4Neighbour2Value = Integer.MIN_VALUE;

                    int d4Neighbour2Column = columnIndex + 1;

                    if (d4Neighbour2Column < width){
                        d4Neighbour2Value = inputVector[rowIndex][d4Neighbour2Column][index];
                    }

                    if (d4Value > d4Neighbour1Value && d4Value > d4Neighbour2Value){
                        pixelValue = 255;
                    }else{
                        pixelValue = 0;
                    }
                }else if (classes[rowIndex][columnIndex] == EDGE_CLASS_3){
                    int index = 0;
                    int d1Value = inputVector[rowIndex][columnIndex][index];
                    int d1Neighbour1Value = Integer.MIN_VALUE;

                    int d1Neighbour1Row = rowIndex - 1;
                    int d1Neighbour1Column = columnIndex - 1;

                    if (d1Neighbour1Row >= 0 && d1Neighbour1Column >= 0){
                        d1Neighbour1Value = inputVector[d1Neighbour1Row][d1Neighbour1Column][index];
                    }

                    int d1Neighbour2Value = Integer.MIN_VALUE;

                    int d1Neighbour2Row = rowIndex + 1;
                    int d1Neighbour2Column = columnIndex + 1;

                    if (d1Neighbour2Row < height && d1Neighbour2Column < width){
                        d1Neighbour2Value = inputVector[d1Neighbour2Row][d1Neighbour2Column][index];
                    }

                    if (d1Value > d1Neighbour1Value && d1Value > d1Neighbour2Value){
                        pixelValue = 255;
                    }else{
                        pixelValue = 0;
                    }
                }else if (classes[rowIndex][columnIndex] == EDGE_CLASS_4){
                    int index = 1;
                    int d2Value = inputVector[rowIndex][columnIndex][index];
                    int d2Neighbour1Value = Integer.MIN_VALUE;

                    int d2Neighbour1Row = rowIndex - 1;

                    if (d2Neighbour1Row >= 0){
                        d2Neighbour1Value = inputVector[d2Neighbour1Row][columnIndex][index];
                    }

                    int d2Neighbour2Value = Integer.MIN_VALUE;

                    int d2Neighbour2Row = rowIndex + 1;

                    if (d2Neighbour2Row < height){
                        d2Neighbour2Value = inputVector[d2Neighbour2Row][columnIndex][index];
                    }

                    if (d2Value > d2Neighbour1Value && d2Value > d2Neighbour2Value){
                        pixelValue = 255;
                    }else{
                        pixelValue = 0;
                    }
                }
                edgeImageProcessor.set(columnIndex, rowIndex, pixelValue);
            }
        }

        return edgeImagePlus;
    }

    private ImagePlus getFinalImage(ImageProcessor edgeImageProcessor) {
        int width = edgeImageProcessor.getWidth();
        int height = edgeImageProcessor.getHeight();

        ImagePlus finalImagePlus = NewImage.createByteImage("Fuzzy Edge Detection", width, height, 1,
                NewImage.FILL_BLACK);
        ImageProcessor finalImageProcessor = finalImagePlus.getProcessor();

        for (int rowIndex = 0; rowIndex < height; rowIndex++){
            for (int columnIndex = 0; columnIndex < width; columnIndex++) {
                int pixelValue = edgeImageProcessor.get(columnIndex, rowIndex);
                if (pixelValue == 255){
                    boolean isNeighbourWhite = false;
                    for (int relativeRowIndex = -1; relativeRowIndex < 2; relativeRowIndex++) {
                        for (int relativeColumnIndex = -1; relativeColumnIndex < 2; relativeColumnIndex++) {
                            if (relativeRowIndex == 0 && relativeColumnIndex == 0){
                                continue;
                            }

                            int xCoordinate = columnIndex + relativeColumnIndex;
                            int yCoordinate = rowIndex + relativeRowIndex;

                            if (xCoordinate < 0){
                                continue;
                            }
                            if (xCoordinate > width - 1){
                                continue;
                            }
                            if (yCoordinate < 0){
                                continue;
                            }
                            if (yCoordinate > height - 1){
                                continue;
                            }

                            int currentNeighbourPixelValue = edgeImageProcessor.get(xCoordinate, yCoordinate);
                            if (currentNeighbourPixelValue == 255){
                                isNeighbourWhite = true;
                                break;
                            }
                        }
                    }

                    if (!isNeighbourWhite){
                        finalImageProcessor.set(columnIndex, rowIndex, 0);
                    }else{
                        finalImageProcessor.set(columnIndex, rowIndex, 255);
                    }
                }else {
                    finalImageProcessor.set(columnIndex, rowIndex, 0);
                }
            }
        }

        return finalImagePlus;
    }
}
