package com.milyeong.mushroomml;

public class Confidence {
    private int pos = 0;
    private float confidence =0;
    private String species = null;

    public void setPos(int pos){
        this.pos = pos;
    }

    public int getPos(){
        return pos;
    }

    public void setConfidence(float confidence){
        this.confidence = confidence;
    }

    public float getConfidence(){
        return confidence;
    }

    public void setSpecies(String s){this.species = s;}

    public String getSpecies(){return species;}
}
