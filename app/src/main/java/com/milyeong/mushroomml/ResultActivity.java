package com.milyeong.mushroomml;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class ResultActivity extends AppCompatActivity {
    ImageView imageView;
    TextView result;
    int imageSize = 224;
    DataHolder dataHolder = new DataHolder();
    String POISON_LABEL = "poison_label.txt";
    String SPECIES_LABEL = "species_label.txt";

    String[] species_name;


    //ListView listView = null;
    LinearLayout layout =null;
    LayoutInflater inflater = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        imageView = findViewById(R.id.imageView2);
        result = findViewById(R.id.tv_posionResult);

        layout = (LinearLayout)findViewById(R.id.l_layout_result);
        inflater = LayoutInflater.from(this);


        try{species_name = readTxt(SPECIES_LABEL);}
        catch(IOException ioe){

        }

        Bitmap image = dataHolder.getBitmap();
        imageView.setImageBitmap(image);
        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
        classifyImage(image);


        //inflated view가 보이지 않는 현상 해결을 위해 추가.
        //resultview의 linearlayout layout height를 wrap_content로 바꾸고 해결.
        //View view  = inflater.inflate(R.layout.content_species, layout, false);
        /*View view = getLayoutInflater().inflate(R.layout.content_species, layout, false);
        TextView tv_species = (TextView) view.findViewById(R.id.tv_content_species);
        TextView tv_confidence = (TextView) view.findViewById(R.id.tv_content_confidence);

        tv_species.setText(" ");
        tv_confidence.setText(" ");
        // set item content in view
        layout.addView(view);*/
        Log.i("inflate", "확인");
    }

    // 이미지 분류.
    public void classifyImage(Bitmap image){
        try {

            MushroomPoison1123 model = MushroomPoison1123.newInstance(getApplicationContext());
            MushroomSpecies1125 model_s = MushroomSpecies1125.newInstance(getApplicationContext());

            // 입력 데이터 생성
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

            // 모델 실행 및 결과 도출
            MushroomPoison1123.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

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

            // 결과 확인
            int maxPos_p = getMaxPos(outputFeature0);
            int maxPos_s = getMaxPos(outputFeature0_s);
            ArrayList<Confidence> list = getMaxPosArray(outputFeature0_s);
            Log.i("list"," " + list.size());

            // 결과 화면에 출력
            for (int i = 0; i<list.size(); i++) {
                //View view  = inflater.inflate(R.layout.content_species, layout, false);
                View view = getLayoutInflater().inflate(R.layout.content_species, layout, false);
                TextView tv_species = (TextView) view.findViewById(R.id.tv_content_species);
                TextView tv_confidence = (TextView) view.findViewById(R.id.tv_content_confidence);

                tv_species.setText(list.get(i).getSpecies());
                float confidence = list.get(i).getConfidence()*100;
                String value = String.format("%.2f%%" ,confidence);
                tv_confidence.setText(value);
                view.setId(i);
                // set item content in view
                layout.addView(view);
                Log.i("inflate", " " +i);
            }


            String[] classes_p = readTxt("poison_label.txt");
            String[] classes_s = readTxt("species_label.txt");
            float poisonFloat = getPoisonConfidence(outputFeature0);
            String poison = String.format("%.2f%% 확률로 독버섯입니다.",poisonFloat);
            SpannableStringBuilder builder = getTextStyle(poison,poisonFloat);

            result.setText(classes_p[maxPos_p]+ " " +classes_s[maxPos_s]);
            //result.setText(  poison +"% 확률로 독버섯입니다.");
            result.setText(builder);

            // Releases model resources if no longer used.
            model.close();
            model_s.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    // 라벨 텍스트 파일 읽어와서 리스트로 만들기
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
            //Log.i("list",list.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.toArray(new String[0]);
    }


    // 제일 신뢰도 높은 값 받아오기
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

    private float getPoisonConfidence(TensorBuffer outputFeature0)  {
        float[] confidences = outputFeature0.getFloatArray();
        return confidences[1]*100;
    }


    // 제일 신뢰도 높은 세 개 리스트로 받아오기기
    private ArrayList<Confidence> getMaxPosArray(TensorBuffer outputFeature0){
        float[] confidences = outputFeature0.getFloatArray();
        ArrayList<Confidence> itemlist = new ArrayList<>();
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
            cList[i].setSpecies(species_name[cList[i].getPos()]);
            itemlist.add(cList[i]);
        }

        return itemlist;
    }

    private SpannableStringBuilder getTextStyle(String str, float confidence){
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        ForegroundColorSpan colorSpan;

        if (confidence > 70.0){
            colorSpan = new ForegroundColorSpan(Color.parseColor("#CD1039"));
            builder.setSpan(colorSpan,0,6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }else if(confidence >30.0){
            colorSpan = new ForegroundColorSpan(Color.parseColor("#FF8200"));
            builder.setSpan(colorSpan,0,6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }else{
            colorSpan = new ForegroundColorSpan(Color.parseColor("#64CD3C"));
            builder.setSpan(colorSpan,0,6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}