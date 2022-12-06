package com.milyeong.mushroomml;

import android.graphics.Bitmap;

public class DataHolder {
 private static Bitmap bitmap = null;

 public void setBitmap(Bitmap b){
     bitmap = b;
 }

 public Bitmap getBitmap(){
     return bitmap;
 }
}

// activity 간 데이터 전송을 위한 클래스
// bitmap의 크기가 intent에서 전송할 수 있는 크기를 넘어서서 클래스로 구현.
