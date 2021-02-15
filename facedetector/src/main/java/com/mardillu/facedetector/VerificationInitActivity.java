package com.mardillu.facedetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class VerificationInitActivity extends AppCompatActivity {

    TextView notificationText, farmerNameText, phoneNumberText;
    ImageView farmerImageLarge, notificationImage;
    CircleImageView farmerImageSmall;
    Button btnProceed, btnStart;
    LinearLayout buttonGroup;
    RelativeLayout contenLayout;

    String imageName;
    String phoneNumber;
    String farmerName;
    String modelPath;
    String farmerIdString;
    int farmerContext;
    double threshHold;

    double verificationScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        notificationText = findViewById(R.id.notification);
        farmerNameText = findViewById(R.id.farmer_name);
        phoneNumberText = findViewById(R.id.phone_number);
        farmerImageLarge = findViewById(R.id.farmer_image_large);
        farmerImageSmall = findViewById(R.id.farmer_image_small);
        btnProceed = findViewById(R.id.btn_proceed);
        btnStart = findViewById(R.id.button_start);
        buttonGroup = findViewById(R.id.button_group);
        notificationImage = findViewById(R.id.notification_image);
        contenLayout = findViewById(R.id.conten_layout);

        Intent intent = getIntent();
        imageName = intent.getStringExtra("image_name");
        phoneNumber = intent.getStringExtra("farmer_phone_number");
        farmerName = intent.getStringExtra("farmer_name");
        farmerContext = intent.getIntExtra("image_context", 1);
        modelPath = intent.getStringExtra("model_name");
        threshHold = intent.getDoubleExtra("threshold", 0.70);
        farmerIdString = intent.getStringExtra("farmer_id_string");

        boolean canProceed = true;
        String message = "";
        File file;
        if (imageName == null || imageName.isEmpty()){
            canProceed = false;
            message += "Image name cannot be null or empty | ";
        }else {
            if (farmerContext == 1) {
                file = new File(Environment.getExternalStorageDirectory() + "/MERGDATA/Images/" + imageName);
            }else {
                file = new File(Environment.getExternalStorageDirectory() + "/MERGDATA/reference_Images/"+imageName);
            }
            if (!file.exists()){
                canProceed = false;
                message += "Image does not exist on device | ";
            }
        }

        if (farmerName == null || farmerName.isEmpty()){
            canProceed = false;
            message += "Farmer name cannot be null or empty |";
        }

        if (modelPath != null && !modelPath.isEmpty()){
            file = new File(Environment.getExternalStorageDirectory() + "/MERGDATA/Models/"+modelPath);
            if (!file.exists()){
                canProceed = false;
                message += "Model does not exist on device | ";
            }
        }else {
            message += "Model name cannot be null or empty |";
        }

        if (canProceed) {
            startAct(null);
        }else {
            if (phoneNumber == null || phoneNumber.isEmpty()){
                phoneNumber = "-";
            }
            setFinishActivity(message);
            return;
        }
    }

    public void startAct(View view){
//        imageName = "78.jpg";
//        phoneNumber = "5005670914";
//        farmerName = "Ezekiel sebastine";
//        farmerContext = 1;

        Intent intent = new Intent(this, VerificationActivity.class);
        intent.putExtra("image_name", imageName);
        intent.putExtra("image_context", farmerContext);
        intent.putExtra("farmer_name", farmerName);
        intent.putExtra("farmer_phone_number", phoneNumber);
        intent.putExtra("threshold", threshHold);
        intent.putExtra("farmer_id_string", farmerIdString);
        intent.putExtra("model_name", modelPath==null?null:Environment.getExternalStorageDirectory() + "/MERGDATA/Models/"+modelPath);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            if (resultCode == RESULT_OK){
                Log.d("TAG", "onActivityResult: OK");
                contenLayout.setVisibility(View.VISIBLE);
                btnStart.setVisibility(View.GONE);
                String status = data.getStringExtra("status");
                double score = data.getDoubleExtra("score", 0.0);
                if (status != null && status.equals("success")){
                    verificationScore = score;
                    showViewSuccessfulMatch();
                }else{
                    verificationScore = score;
                    showViewNoMatch();
                }
            }else{
                contenLayout.setVisibility(View.VISIBLE);
                btnStart.setVisibility(View.GONE);
                Log.d("TAG", "onActivityResult: CANCEL");
                verificationScore = -1.0;
                showViewNoMatch();
            }
        }
    }

    public void setFinishActivity(double score){
        Intent it = new Intent(this, VerificationInitActivity.class);
        it.putExtra("score", score);
        it.putExtra("message", "");
        if (score >= threshHold){
            it.putExtra("status", "success");
        }else{
            it.putExtra("status", "failed");
        }
        setResult(RESULT_OK, it);
        finish();
    }


    public void setFinishActivity(String message){
        Intent it = new Intent(this, VerificationInitActivity.class);
        it.putExtra("score", Math.max(verificationScore, 0.0));
        it.putExtra("status", "failed");
        it.putExtra("message", message);
        setResult(RESULT_OK, it);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent it = new Intent(this, VerificationInitActivity.class);
        it.putExtra("score", Math.max(verificationScore, 0.0));
        it.putExtra("status", "failed");
        it.putExtra("message", "Canceled");
        setResult(RESULT_CANCELED, it);
        finish();
    }

    void showViewSuccessfulMatch(){
        notificationText.setText("Verification Successful");
        notificationText.setTextColor(Color.WHITE);
        notificationImage.setVisibility(View.VISIBLE);
        farmerNameText.setText(farmerName);
        phoneNumberText.setText(phoneNumber);
        btnProceed.setVisibility(View.VISIBLE);
        buttonGroup.setVisibility(View.GONE);
        showImageOnImageView(true);
    }

    void showViewNoMatch(){
        notificationText.setText("Verification failed. No match found");
        notificationText.setTextColor(Color.RED);
        notificationImage.setVisibility(View.GONE);
        farmerNameText.setText(farmerName);
        phoneNumberText.setText(phoneNumber);
        btnProceed.setVisibility(View.GONE);
        buttonGroup.setVisibility(View.VISIBLE);
        showImageOnImageView(false);
    }

    void showImageOnImageView(boolean isVerificationSuccessful) {
        String img;
        if (farmerContext == 1) {
            img = Environment.getExternalStorageDirectory() + "/MERGDATA/Images/" + (isVerificationSuccessful?imageName:"temp_face_image.jpg");
        }else {
            img = Environment.getExternalStorageDirectory() + "/MERGDATA/"+(isVerificationSuccessful?"reference_Images/"+imageName:"Images/temp_face_image.jpg");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options. inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(img, options);

        farmerImageLarge.setImageBitmap(rotateImageBitmap(bitmap, img));
        farmerImageSmall.setImageBitmap(rotateImageBitmap(bitmap, img));
    }

    public void proceedClicked(View view){
        setFinishActivity(verificationScore);
    }

    public void retryClicked(View view){
        startAct(null);
    }

    public void closeClicked(View view){
        setFinishActivity("No match found");
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private Bitmap rotateImageBitmap(Bitmap bitmap, String path){
        try {
            ExifInterface ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            Bitmap rotatedBitmap;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }

            return rotatedBitmap;
        }catch (Exception e){
            e.printStackTrace();
        }

        return bitmap;
    }
}