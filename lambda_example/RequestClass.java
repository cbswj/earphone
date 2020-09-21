package com.smartfarm.www;



public class RequestClass {
    String imgName;

    public String getImgName() {
        return imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public RequestClass(String imgName) {
        this.imgName = imgName;
    }

    public RequestClass() {
    }
}