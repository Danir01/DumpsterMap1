package com.example.trashmap.DBClasses;

import android.widget.ImageView;

public class HelpItem {
    ImageView img;
    String name;

    public HelpItem(){

    }

    public HelpItem(String Name){
        this.name = Name;
    }

    public HelpItem(ImageView Img, String Name){
        this.img = Img;
        this.name = Name;
    }

    public String getName() {
        return name;
    }

    public ImageView getImg() {
        return img;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImg(ImageView img) {
        this.img = img;
    }
}
