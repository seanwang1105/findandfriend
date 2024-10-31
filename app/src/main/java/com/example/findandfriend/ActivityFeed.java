package com.example.findandfriend;

public class ActivityFeed {
    private String userName;
    private String activityDescription;
    private String rating;

    // Initial constructor
    public ActivityFeed(String userName, String activityDescription, String rating) {
        this.userName = userName;
        this.activityDescription = activityDescription;
        this.rating = rating;
    }

    // Getter method
    public String getUserName() {
        return userName;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public String getRating() {
        return rating;
    }
}

