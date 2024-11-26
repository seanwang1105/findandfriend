package com.example.findandfriend;

public class MeetingFriend {
    private int Mid;

    public String name;
    public String meet_location_name;
    public String meet_status;
    public double latitude;
    public double longitude;

    public MeetingFriend(int Mid,String name, String meet_location_name, double latitude, double longitude,String meet_status) {
        this.Mid=Mid;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.meet_location_name=meet_location_name;
        this.meet_status=meet_status;
    }

    public String getLocationName() {return meet_location_name;
    }

    public double getLongitude() {return longitude;
    }

    public double getLatitude() {return latitude;
    }

    public String getSenderEmail() {return name;
    }

    public int getMid() {
        return Mid;
    }
}
