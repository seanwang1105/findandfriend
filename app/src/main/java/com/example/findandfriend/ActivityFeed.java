package com.example.findandfriend;

public class ActivityFeed {
    private String userName;
    private String activityDescription;
    private String rating;
    private String reviews;

    // Initial constructor
    public ActivityFeed(String userName, String activityDescription, String rating, String lastReviews) {
        this.userName = userName;
        this.activityDescription = activityDescription;
        this.rating = rating;
        this.reviews = lastReviews;
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
    public String getReviews() {
        return reviews;
    }
}

