package com.example.projekt;

import android.graphics.drawable.Drawable;

public class Item {
    private String appPackage;
    private int position;
    private int TXSend,RXReceived;
    private String lastUsed;

    public Item(String appPackage,int position){
        this.appPackage=appPackage;
        this.position=position;
    }

    public Item(String appPackage, int TXSend, int RXReceived, String lastUsed) {
        this.appPackage = appPackage;
        this.TXSend = TXSend;
        this.RXReceived = RXReceived;
        this.lastUsed = lastUsed;
    }

    public int getTXSend() {
        return TXSend;
    }

    public void setTXSend(int TXSend) {
        this.TXSend = TXSend;
    }

    public int getRXReceived() {
        return RXReceived;
    }

    public void setRXReceived(int RXReceived) {
        this.RXReceived = RXReceived;
    }

    public String getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(String lastUsed) {
        this.lastUsed = lastUsed;
    }

    public String getAppPackage() {
        return appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
