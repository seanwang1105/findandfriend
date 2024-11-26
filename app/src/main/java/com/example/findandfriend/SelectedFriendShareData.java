package com.example.findandfriend;

import java.util.ArrayList;
import java.util.List;

public class SelectedFriendShareData {
    private static SelectedFriendShareData instance;
    private List<Friend> selectedFriends;

    private SelectedFriendShareData() {
        selectedFriends = new ArrayList<>();
    }

    public static SelectedFriendShareData getInstance() {
        if (instance == null) {
            instance = new SelectedFriendShareData();
        }
        return instance;
    }

    public List<Friend> getSelectedFriends() {
        return selectedFriends;
    }

    public void setSelectedFriends(List<Friend> friends) {
        this.selectedFriends = friends;
    }
}

