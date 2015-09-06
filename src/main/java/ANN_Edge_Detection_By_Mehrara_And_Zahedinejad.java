import com.heatonresearch.book.introneuralnet.neural.feedforward.FeedforwardLayer;
import com.heatonresearch.book.introneuralnet.neural.feedforward.FeedforwardNetwork;
import com.heatonresearch.book.introneuralnet.neural.matrix.Matrix;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author Vladimir Nizamutdinov (astartes91@gmail.com)
 * Artificial neural networks based edge detection plugin for ImageJ
 * Implememnted on the basis of Mehrara and Zahedinejad algorithm (2011)
 * http://www.sid.ir/EN/VEWSSID/J_pdf/1035220110202.pdf
 */
public class ANN_Edge_Detection_By_Mehrara_And_Zahedinejad implements PlugInFilter{
    private FeedforwardNetwork network;
    /*private final String WHITE_EDGES_CHOICE = "White edges, black background";
    private final String BLACK_EDGES_CHOICE = "Black edges, white background";*/

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        return DOES_8G + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        /*GenericDialog gd = new GenericDialog("Algorithm settings");
        gd.addRadioButtonGroup(
                "",
                new String[]{
                        WHITE_EDGES_CHOICE,
                        BLACK_EDGES_CHOICE
                },
                2,
                1,
                WHITE_EDGES_CHOICE
        );

        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("Plugin run cancelled!");
            return;
        }*/

        //String choice = gd.getNextRadioButton();
        network = getNetwork(/*choice*/);

        int width = imageProcessor.getWidth();
        int height = imageProcessor.getHeight();

        //long startTime = System.nanoTime();
        int[] histogram = imageProcessor.getHistogram();
        int threshold = getThresholdValue(histogram, width * height);

        ImagePlus binaryImagePlus = getBinaryImage(imageProcessor, threshold);
        ImageProcessor binaryImageProcessor = binaryImagePlus.getProcessor();
        //binaryImagePlus.show();

        ImagePlus edgeImagePlus = getEdgeImage(binaryImageProcessor);
        ImageProcessor edgeImageProcessor = edgeImagePlus.getProcessor();
        //edgeImagePlus.show();

        ImagePlus finalImagePlus = getFinalImage(edgeImageProcessor/*, choice*/);

        //long endTime = System.nanoTime();

        finalImagePlus.show();
        //IJ.log(Double.valueOf((endTime - startTime) / 1000000000.0).toString());
    }

    private ImagePlus getBinaryImage(ImageProcessor sourceImageProcessor, int threshold){
        int width = sourceImageProcessor.getWidth();
        int height = sourceImageProcessor.getHeight();

        ImagePlus binaryImagePlus = NewImage.createByteImage("Binary", width, height, 1,
                NewImage.FILL_BLACK);
        ImageProcessor binaryImageProcessor = binaryImagePlus.getProcessor();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int originalColor = sourceImageProcessor.get(i, j);

                if (originalColor >= threshold) {
                    //set the new image's pixel
                    binaryImageProcessor.set(i, j, 255);
                }
            }
        }

        return binaryImagePlus;
    }

    private ImagePlus getEdgeImage(ImageProcessor binaryImageProcessor) {
        int width = binaryImageProcessor.getWidth();
        int height = binaryImageProcessor.getHeight();

        ImagePlus edgeImagePlus = NewImage.createByteImage("Edges", width, height, 1,
                NewImage.FILL_BLACK);
        ImageProcessor edgeImageProcessor = edgeImagePlus.getProcessor();

        for (int y = 0; y < height - 1; y += 1) {
            for (int x = 0; x < width - 1; x += 1) {
                double[] values = {
                        (binaryImageProcessor.get(x, y)) / 255.0,
                        (binaryImageProcessor.get(x + 1, y)) / 255.0,
                        (binaryImageProcessor.get(x, y + 1)) / 255.0,
                        (binaryImageProcessor.get(x + 1, y + 1)) / 255.0
                };

                double[] output = network.computeOutputs(values);

                double[][] outputValues = {
                        {
                                output[0], output[1]
                        },
                        {
                                output[2], output[3]
                        }
                };

                for (int y1 = 0; y1 < 2; y1++) {
                    for (int x1 = 0; x1 < 2; x1++) {
                        double value = outputValues[y1][x1];

                        int pixelValue = (int)(Math.round(value) * 255);
                        if (pixelValue == 255) {
                            edgeImageProcessor.set(x + x1, y + y1, pixelValue);
                        }
                    }
                }
            }
        }

        return edgeImagePlus;
    }

    private ImagePlus getFinalImage(ImageProcessor edgeImageProcessor/*, String choice*/) {
        int width = edgeImageProcessor.getWidth();
        int height = edgeImageProcessor.getHeight();

        ImagePlus finalImagePlus = NewImage.createByteImage("ANN Edge Detection", width, height, 1,
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
                } else {
                    finalImageProcessor.set(columnIndex, rowIndex, 0);
                }
            }
        }

        return finalImagePlus;
    }

    private FeedforwardNetwork getNetwork(/*String choice*/) {
        ArrayList<String> lines = new ArrayList<String>();
        try {
            /*String filename;
            if(choice.equals(WHITE_EDGES_CHOICE)){
                filename = "white_edges_weights.txt";
            } else {
                filename = "black_edges_weights.txt";
            }*/

            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader()
                    .getResourceAsStream("weights.txt")));
            String fileLine;
            while ((fileLine = br.readLine()) != null) {
                lines.add(fileLine);
            }

            double[][] inputLayerWeights = new double[5][12];
            double[][] hiddenLayerWeights = new double[13][4];
            boolean forInput = true;

            int counter = 0;
            for (String line: lines) {
                if (StringUtils.isBlank(line)) {
                    forInput = false;
                    counter = 0;
                } else {
                    if (forInput) {
                        String[] strings = line.split(" ");
                        for (int i = 0; i < strings.length; i++) {
                            inputLayerWeights[counter][i] = Double.parseDouble(strings[i].replace(',', '.'));
                        }
                    } else {
                        String[] strings = line.split(" ");
                        for (int i = 0; i < strings.length; i++) {
                            hiddenLayerWeights[counter][i] = Double.parseDouble(strings[i].replace(',', '.'));
                        }
                    }
                    counter++;
                }
            }

            Matrix inputLayerMatrix = new Matrix(inputLayerWeights);
            Matrix hiddenLayermatrix = new Matrix(hiddenLayerWeights);

            FeedforwardNetwork network = new FeedforwardNetwork();
            network.addLayer(new FeedforwardLayer(4));
            network.addLayer(new FeedforwardLayer(12));
            network.addLayer(new FeedforwardLayer(4));
            network.reset();

            FeedforwardLayer inputLayer = network.getInputLayer();
            inputLayer.setMatrix(inputLayerMatrix);

            FeedforwardLayer hiddenLayer = (FeedforwardLayer) network.getHiddenLayers().toArray()[0];
            hiddenLayer.setMatrix(hiddenLayermatrix);

            return network;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private int getThresholdValue(int[] histogram, long total) {
        int sum = 0;
        for (int i = 1; i < 256; ++i) {
            sum += i * histogram[i];
        }

        int sumB = 0;
        int wB = 0;
        double max = 0.0;
        int threshold1 = 0;
        int threshold2 = 0;
        for (int i = 0; i < 256; ++i) {
            wB += histogram[i];
            if (wB == 0) {
                continue;
            }

            long wF = total - wB;
            if (wF == 0) {
                break;
            }

            sumB += i * histogram[i];
            double mB = (double)sumB / wB;
            double mF = (double)(sum - sumB) / wF;
            double between = wB * wF * Math.pow(mB - mF, 2);
            if (between >= max) {
                threshold1 = i;
                if (between > max) {
                    threshold2 = i;
                }
                max = between;
            }
        }
        return (threshold1 + threshold2) / 2;
    }
}