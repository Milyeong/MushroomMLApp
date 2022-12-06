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
