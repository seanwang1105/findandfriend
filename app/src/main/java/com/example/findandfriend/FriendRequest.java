package com.example.findandfriend;

public class FriendRequest {
    private int requestId;
    private String fromEmail;
    private String fromName;

    public FriendRequest(int requestId,String fromEmail, String fromName) {
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.requestId=requestId;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getFromName() {
        return fromName;
    }
    public int getRequestId() {
        return requestId;
    }
}
