package com.eaw1805.fieldmap;


public class ImagePart {
    private String code;
    private int x;
    private int y;

    public ImagePart(String code, int x, int y) {
        this.code = code;
        this.x = x;
        this.y = y;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
