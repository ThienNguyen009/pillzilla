package com.example.pillzilla;

public class MedicationNotification {

    private String id;
    private String medicationId;
    private String title;
    private String message;
    private String time;

    public MedicationNotification() {}

    public MedicationNotification(String title, String message, String time, String medicationId) {
        this.title        = title;
        this.message      = message;
        this.time         = time;
        this.medicationId = medicationId;
    }

    public String getId()            { return id; }
    public void setId(String id)     { this.id = id; }

    public String getMedicationId()                    { return medicationId; }
    public void setMedicationId(String medicationId)    { this.medicationId = medicationId; }

    public String getTitle()             { return title; }
    public void setTitle(String title)   { this.title = title; }

    public String getMessage()               { return message; }
    public void setMessage(String message)   { this.message = message; }

    public String getTime()          { return time; }
    public void setTime(String time) { this.time = time; }
}
