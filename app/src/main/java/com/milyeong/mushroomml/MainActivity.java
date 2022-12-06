package com.milyeong.mushroomml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    DataHolder dataHolder = new DataHolder();

    Button btn_camera, btn_gallery;
    ImageView imageView;
    TextView result;
    int imageSize = 224;

    File file;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("시작", "확인");

        File sdcard = Environment.getExternalStorageDirectory();
        file = new File(sdcard,"capture,jpg");

        btn_camera = findViewById(R.id.btn_camera);
        btn_gallery = findViewById(R.id.btn_gallery);

        btn_camera.setOnClickListener(new OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(file));



                    setResult(3,cameraIntent);
                    Log.i("cameraIntent","setResult다음");
                    startActivityForResult(cameraIntent, 3);
                    //cameraResultLauncher.launch(cameraIntent);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        btn_gallery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //setResult(1,cameraIntent);

                Log.i("버튼","gallery 버튼 인텐트 생성");
                startActivityForResult(cameraIntent, 1);
                //mediaResultLauncher.launch(cameraIntent);
            }
        });
    }

    protected void onActivityResult(int requestCode, @Nullable int resultCode, @Nullable Intent data) {
        Log.i("onActivityResult", "함수 호출" + resultCode);
        if (resultCode == RESULT_OK && ((data.getData() != null)||(data.getExtras() != null))){
            if (requestCode == 3) { // 사진 촬영 시
                Log.i("cameraIntent","requestCode==3");
                Bitmap image = (Bitmap) data.getExtras().get("data");
                //Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(),new BitmapFactory.Options());
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                dataHolder.setBitmap(image);
                startActivity(intent);

            } else { // 갤러리 사진 선택 시
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                dataHolder.setBitmap(image);
                startActivity(intent);

            }

        } else {
            Log.i("requestCode==1", String.format("%d, %d",resultCode, requestCode));
           //Log.i("requestCode==1", data.getData().getClass().getName());
            super.onActivityResult(requestCode, resultCode, data);}
        //}
    }
}