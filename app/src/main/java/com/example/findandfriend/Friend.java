package com.example.findandfriend;

import android.os.Parcel;
import android.os.Parcelable;

public class Friend implements Parcelable {
    public String id;
    String name;
    String email;
    double latitude;
    double longitude;
    String timeAtLocation;
    int avatarResourceId;

    // constructor, initial all variables
    public Friend(String id, String name, String email,double latitude, double longitude, String timeAtLocation, int avatarResourceId) {
        this.id = id;
        this.name = name;
        this.email=email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeAtLocation = timeAtLocation;
        this.avatarResourceId = avatarResourceId;
    }

    // Constructor to read data from Parcel
    protected Friend(Parcel in) {
        id = in.readString();
        name = in.readString();
        email = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        timeAtLocation = in.readString(); // read timeAtLocation
        avatarResourceId = in.readInt();  // read avatarResourceId
    }

    // define Parcelable.Creator
    public static final Creator<Friend> CREATOR = new Creator<Friend>() {
        @Override
        public Friend createFromParcel(Parcel in) {
            return new Friend(in);
        }

        @Override
        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };


    // Getter method
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimeAtLocation() {
        return timeAtLocation;
    }

    public int getAvatarResourceId() {
        return avatarResourceId;
    }

    // Parcelable method to write Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(email);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(timeAtLocation); // write timeAtLocation
        dest.writeInt(avatarResourceId);  // write avatarResourceId
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
