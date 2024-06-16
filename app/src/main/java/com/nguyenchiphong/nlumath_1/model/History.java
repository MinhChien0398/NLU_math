package com.nguyenchiphong.nlumath_1.model;

public class History {
    private String title, description;
    private int img;
    private boolean isChecked;

    public History() {
    }

    public History(String title, String description, int img) {
        this.title = title;
        this.description = description;
        this.img = img;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getImg() {
        return img;
    }

    public void setImg(int img) {
        this.img = img;
    }
}
