package com.mardillu.faceapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.mardillu.facedetector.VerificationInitActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Button btn = findViewById(R.id.btn);

        btn.setOnClickListener(this::startt);

        downloadTestImage();
    }

    void downloadTestImage(){
        Picasso.get()
                .load("https://i.stack.imgur.com/NXVu6.jpg")
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        try {
                            String path;
                            String fileName = "NXVu6.jpg";
                            path = Environment.getExternalStorageDirectory() + "/MERGDATA/Images";
                            OutputStream fOut;
                            File file = new File(path, fileName);
                            fOut = new FileOutputStream(file);

                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                            fOut.flush(); // Not really required
                            fOut.close();


                            String pavth = MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        Log.d("TAG", "onPrepareLoad: ");
                    }
                });
    }

    /**
     * This is how you will send a initiate the verification
     *
     * Use an intent then {@link this#startActivityForResult(Intent, int)}
     *
     * And wait for the result in {@link this#onActivityResult(int, int, Intent)}
     * verification result will be contained in the Intent data
     */
    public void startt(View view){
        Intent intent = new Intent(this, VerificationInitActivity.class);
        intent.putExtra("image_name", "NXVu6.jpg");
        intent.putExtra("image_context", 1);
        intent.putExtra("farmer_name", "Ezekiel Sebastine");
        intent.putExtra("farmer_phone_number", "0550670914");
        intent.putExtra("model_name", "facenet.tflite");
        intent.putExtra("threshold", 0.60);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            if (resultCode == RESULT_OK){
                Log.d("TAG", "onActivityResult: OK");
                String status = data.getStringExtra("status");
                String reason = data.getStringExtra("message");
                double score = data.getDoubleExtra("score", 0.0);
                if (status != null && status.equals("success")){
                    //Face similarity score was 0.85 or higher
                    Log.d("TAG", "onActivityResult: MATCH FOUND "+score);
                }else{
                    //Face similarity score was below 0.85
                    Log.d("TAG", "onActivityResult: NO MATCH "+score);
                    Log.d("TAG", "onActivityResult: MESSAGE "+reason);
                }
            }else{
                //Result not ok. Reason will be found in message data
                String status = data.getStringExtra("message");
                Log.d("TAG", "onActivityResult: CANCEL "+status);
            }
        }
    }
}