package com.example.trashmap.DBClasses;

public class SliderItem {

    private long idType;
    private String imgUri;

    public SliderItem(String ImgUri){
        this.imgUri = ImgUri;
    }

    public SliderItem(long IdType, String ImgUri){
        this.idType = IdType;
        this.imgUri = ImgUri;
    }

    public SliderItem(){

    }

    public long getIdType() {
        return idType;
    }

    public String getImgUri() {
        return imgUri;
    }

    public void setImgUri(String imgUri) {
        this.imgUri = imgUri;
    }

    public void setIdType(long idType) {
        this.idType = idType;
    }
}
