package com.example.trashmap.DBClasses;

import java.io.Serializable;

public class GarbageType implements Serializable {
    public long idType;
    public String nameType;
    public String imgUri;

    public GarbageType(){

    }

    public GarbageType(long IdType, String NameType, String ImgUri){
        idType = IdType;
        nameType = NameType;
        imgUri = ImgUri;
    }
}
