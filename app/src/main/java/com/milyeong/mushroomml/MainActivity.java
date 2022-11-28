package com.milyeong.mushroomml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.milyeong.mushroomml.ml.MushroomPoison1123;
import com.milyeong.mushroomml.ml.MushroomSpecies1125;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    Button btn_camera, btn_gallery;
    ImageView imageView;
    TextView result;
    int imageSize = 224;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("시작", "확인");

        ActivityResultLauncher<Intent> activityResultLauncher;

        btn_camera = findViewById(R.id.button);
        btn_gallery = findViewById(R.id.button2);

        result = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //setResult(3,cameraIntent);
                    //activityResultLauncher.launch(cameraIntent);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //setResult(1,cameraIntent);
                //activityResultLauncher.launch(cameraIntent);
                Log.i("버튼","gallery 버튼 인텐트 생성");
                startActivityForResult(cameraIntent, 1);
            }
        });
    }

    public void classifyImage(Bitmap image){
        try {
            Log.i("?","???");
            MushroomPoison1123 model = MushroomPoison1123.newInstance(getApplicationContext());
            MushroomSpecies1125 model_s = MushroomSpecies1125.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            MushroomPoison1123.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Runs model inference and gets result.
            MushroomSpecies1125.Outputs outputs_s = model_s.process(inputFeature0);
            TensorBuffer outputFeature0_s = outputs_s.getOutputFeature0AsTensorBuffer();


            // 이 부분 함수로 만들기 TensorBuffer를 인수로 주고 maxPos를 리턴받기.
            /*float[] confidences = outputFeature0.getFloatArray();

            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }*/
            int maxPos_p = getMaxPos(outputFeature0);
            int maxPos_s = getMaxPos(outputFeature0_s);
            getMaxPosArray(outputFeature0_s);

            String[] classes_p = readTxt("poison_label.txt");
            String[] classes_s = readTxt("species_label.txt");
            result.setText(classes_p[maxPos_p]+ " " +classes_s[maxPos_s]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, @Nullable int resultCode, @Nullable Intent data) {
        Log.i("onActivityResult","함수 호출");
        if(resultCode == RESULT_OK && data.getData()!=null){
            if(requestCode == 3){
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }else {
                Log.i("requestCode == 1", "if문 내 실행");
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                Log.i("인텐트", String.valueOf(data));
                classifyImage(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String[] readTxt(String fileName) throws IOException {
        List<String> list = new ArrayList<>();
        BufferedReader reader;
        InputStream inputStream = getAssets().open(fileName);
        try{
          reader = new BufferedReader(new InputStreamReader(inputStream));
          String line = "";
          while((line = reader.readLine()) != null){
              Log.d("StackOverflow", line);
              list.add(line);
          }
          Log.i("list",list.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.toArray(new String[0]);
    }

    private int getMaxPos(TensorBuffer outputFeature0){
        float[] confidences = outputFeature0.getFloatArray();
        Log.i("tensorBuffer",confidences.toString());

        // find the index of the class with the biggest confidence.
        int maxPos = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidences.length; i++) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }

        return maxPos;
    }

    private void getMaxPosArray(TensorBuffer outputFeature0){
        float[] confidences = outputFeature0.getFloatArray();
        Log.i("tensorBuffer",confidences.toString());

        // find the index of the class with the biggest confidence.

        Confidence[] cList = new Confidence[3];
        for(int i =0; i < 3; i++){
            cList[i] = new Confidence();
        }

        for(int i = 0; i<confidences.length; i++){
            if(confidences[i] > cList[0].getConfidence()){
                cList[2].setConfidence(cList[1].getConfidence());
                cList[2].setPos(cList[1].getPos());
                cList[1].setConfidence(cList[0].getConfidence());
                cList[1].setPos(cList[0].getPos());
                cList[0].setConfidence(confidences[i]);
                cList[0].setPos(i);
            }else if(confidences[i] > cList[1].getConfidence()){
                cList[2].setConfidence(cList[1].getConfidence());
                cList[2].setPos(cList[1].getPos());
                cList[1].setConfidence(confidences[i]);
                cList[1].setPos(i);
            }else if(confidences[i] > cList[2].getConfidence()){
                cList[2].setConfidence(confidences[i]);
                cList[2].setPos(i);
            }
        }

        for(int i =0; i<3; i++){
            Log.i("cList",cList[i].getConfidence() + " " + cList[i].getPos());
        }
        //return maxPos;
    }
}