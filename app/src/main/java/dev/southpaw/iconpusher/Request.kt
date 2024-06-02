package dev.southpaw.iconpusher

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

class Request {
    //    String getPlayer() {
    //        return player;
    //    }
    //    void setPlayer(String player) {
    //        this.player = player;
    //    }
    @JvmField
    var selected = false
    private val pkg: String? = null // The package
    private val comp: String? = null // The component
    var icon: Drawable? = null
    @JvmField
    var info: ApplicationInfo? = null
}
