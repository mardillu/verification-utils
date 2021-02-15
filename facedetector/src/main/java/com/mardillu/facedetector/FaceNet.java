package com.mardillu.facedetector;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Created on 14/02/2021 at 21:20.
 *
 * @author Ezekiel Sebastine.
 */
class FaceNet {
    private static final String MODEL_PATH = "facenet.tflite";

    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    private static final int BATCH_SIZE = 1;
    private static final int IMAGE_HEIGHT = 160;
    private static final int IMAGE_WIDTH = 160;
    private static final int NUM_CHANNELS = 3;
    private static final int NUM_BYTES_PER_CHANNEL = 4;
    private static final int EMBEDDING_SIZE = 512;
    private final int[] intValues = new int[IMAGE_HEIGHT * IMAGE_WIDTH];
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    Device device = Device.CPU;
    /**
     * An Interpreter for the TFLite model.
     */
    Interpreter interpreter;
    private GpuDelegate gpuDelegate;
    private int NUM_LITE_THREADS = 4;
    private ByteBuffer imgData;
    private MappedByteBuffer tfliteModel;
    private Interpreter tflite;

    public FaceNet(AssetManager assetManager) throws IOException {
        tfliteModel = loadModelFile(assetManager);
        tflite = getInterpreter(tfliteModel);
        imgData = ByteBuffer.allocateDirect(
                BATCH_SIZE
                        * IMAGE_HEIGHT
                        * IMAGE_WIDTH
                        * NUM_CHANNELS
                        * NUM_BYTES_PER_CHANNEL);
        imgData.order(ByteOrder.nativeOrder());
    }

    public FaceNet(String filePath) throws IOException {
        tfliteModel = loadModelFile(filePath);
        tflite = getInterpreter(tfliteModel);
        imgData = ByteBuffer.allocateDirect(
                BATCH_SIZE
                        * IMAGE_HEIGHT
                        * IMAGE_WIDTH
                        * NUM_CHANNELS
                        * NUM_BYTES_PER_CHANNEL);
        imgData.order(ByteOrder.nativeOrder());
    }

    /**
     * Get a TensorFlow interpreter
     *
     * @return
     * @throws IOException
     */
    private Interpreter getInterpreter(MappedByteBuffer model) throws IOException {
        if (interpreter != null) {
            return interpreter;
        }
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(NUM_LITE_THREADS);
        switch (device) {
            case CPU:
                break;
            case GPU:
                gpuDelegate = new GpuDelegate();
                options.addDelegate(gpuDelegate);
                break;
            case NNAPI:
                options.setUseNNAPI(true);
                break;
        }
        interpreter = new Interpreter(model, options);
        return interpreter;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private MappedByteBuffer loadModelFile(String path) throws IOException {
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < IMAGE_HEIGHT; ++i) {
            for (int j = 0; j < IMAGE_WIDTH; ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
    }

    private void addPixelValue(int pixelValue) {
        //imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        //imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        //imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat(((pixelValue >> 16) & 0xFF) / 255.0f);
        imgData.putFloat(((pixelValue >> 8) & 0xFF) / 255.0f);
        imgData.putFloat((pixelValue & 0xFF) / 255.0f);
    }

    public void inspectModel() {
        String tag = "Model Inspection";
        Log.i(tag, "Number of input tensors: " + tflite.getInputTensorCount());
        Log.i(tag, "Number of output tensors: " + tflite.getOutputTensorCount());

        Log.i(tag, tflite.getInputTensor(0).toString());
        Log.i(tag, "Input tensor data type: " + tflite.getInputTensor(0).dataType());
        Log.i(tag, "Input tensor shape: " + Arrays.toString(tflite.getInputTensor(0).shape()));
        Log.i(tag, "Output tensor 0 shape: " + Arrays.toString(tflite.getOutputTensor(0).shape()));
    }

    private Bitmap resizedBitmap(Bitmap bitmap, int height, int width) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private Bitmap croppedBitmap(Bitmap bitmap, int upperCornerX, int upperCornerY, int height, int width) {
        return Bitmap.createBitmap(bitmap, upperCornerX, upperCornerY, width, height);
    }

    private float[][] run(Bitmap bitmap) {
        bitmap = resizedBitmap(bitmap, IMAGE_HEIGHT, IMAGE_WIDTH);
        convertBitmapToByteBuffer(bitmap);

        float[][] embeddings = new float[1][512];
        tflite.run(imgData, embeddings);

        return embeddings;
    }

    //get similarity using Euclidean distance between two vectors
    public double getSimilarityScore(Bitmap face1, Bitmap face2) {
        float[][] face1_embedding = run(face1);
        float[][] face2_embedding = run(face2);

        double distance = 0.0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            distance += (face1_embedding[0][i] - face2_embedding[0][i]) * (face1_embedding[0][i] - face2_embedding[0][i]);
        }
        distance = Math.sqrt(distance);

        return distance;
    }

    public double computeCosineSimilarity(Bitmap face1, Bitmap face2) {
        float[][] vectorA = run(face1);
        float[][] vectorB = run(face2);
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < vectorA[0].length; i++) {
            dotProduct += vectorA[0][i] * vectorB[0][i];
            normA += Math.pow(vectorA[0][i], 2);
            normB += Math.pow(vectorB[0][i], 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        tfliteModel = null;
    }


    enum Device {CPU, NNAPI, GPU}
}
