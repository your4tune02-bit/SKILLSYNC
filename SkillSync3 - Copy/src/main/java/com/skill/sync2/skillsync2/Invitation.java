package com.skill.sync2.skillsync2;

public class Invitation {
    private int id;
    private String receiverName;
    private String senderInfo;
    private String message;
    private String status;
    private String type;
    private String relatedTitle;

    public Invitation(int id, String receiverName, String senderInfo, String message, String status, String type, String relatedTitle) {
        this.id = id;
        this.receiverName = receiverName;
        this.senderInfo = senderInfo;
        this.message = message;
        this.status = status;
        this.type = type;
        this.relatedTitle = relatedTitle;
    }

    public int getId() { return id; }
    public String getReceiverName() { return receiverName; }
    public String getSenderInfo() { return senderInfo; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public String getRelatedTitle() { return relatedTitle; }
}