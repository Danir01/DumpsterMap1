package com.example.trashmap.DBClasses;

import android.net.Uri;

import java.io.Serializable;

public class Markers implements Serializable{

    public String idMarker;
    public String name;
    public long typeGarbage;
    public String lat;
    public String lng;
    public String address;
    public String dateAdd;
    public String imgUri;

    public Markers(String IdMarker, String Name, long TypeGarbage, String Lat, String Lng, String Address, String DateAdd, String ImgUri){
        this.idMarker = IdMarker;
        this.name = Name;
        this.typeGarbage = TypeGarbage;
        this.lat = Lat;
        this.lng = Lng;
        this.address = Address;
        this.dateAdd = DateAdd;
        this.imgUri = ImgUri;
    }
    //Пустой конструктор
    public Markers (){
    }

}
