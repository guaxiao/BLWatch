package com.tau.blwatch.util;

import android.os.Parcel;
import android.os.Parcelable;

public class UserEntity implements Parcelable {

    private String userID;
    private String userName;
    private String imgPath;

    public UserEntity(){
        userID = "-1";
        userName = "N/A";
        imgPath = "N/A";
    }

    public UserEntity(Parcel source){
        userID = source.readString();
        userName = source.readString();
        imgPath = source.readString();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserImgPath() {
        return imgPath;
    }

    public void setUserImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userID);
        dest.writeString(userName);
        dest.writeString(imgPath);
    }

    public static final Parcelable.Creator<?> CREATOR = new Parcelable.Creator() {
        @Override
        public UserEntity createFromParcel(Parcel source) {
            return new UserEntity(source);
        }

        @Override
        public UserEntity[] newArray(int size) {
            return new UserEntity[size];
        }
    };
}