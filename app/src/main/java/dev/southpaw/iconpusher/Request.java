package dev.southpaw.iconpusher;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class Request {
    private boolean isSelected;
    private String pkg; // The package
    private String comp; // The component
    public Drawable icon;
    public Boolean selected = false;

    public ApplicationInfo info;

//    String getPlayer() {
//        return player;
//    }
//    void setPlayer(String player) {
//        this.player = player;
//    }
    boolean getSelected() {
        return isSelected;
    }
    void setSelected(boolean selected) {
        isSelected = selected;
    }

}
