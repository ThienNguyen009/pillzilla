package com.example.pillzilla;

public class Medication {

    private String id;
    private String name;
    private String dosage;
    private String scheduleTime;
    private String date;
    private String repeat;
    private String notes;
    private boolean taken;

    public Medication() {}

    public Medication(String name, String dosage, String scheduleTime,
                      String date, String repeat, String notes) {
        this.name         = name;
        this.dosage       = dosage;
        this.scheduleTime = scheduleTime;
        this.date         = date;
        this.repeat       = repeat;
        this.notes        = notes;
    }

    public String getId()           { return id; }
    public void setId(String id)    { this.id = id; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public String getDosage()                { return dosage; }
    public void setDosage(String dosage)     { this.dosage = dosage; }

    public String getScheduleTime()                    { return scheduleTime; }
    public void setScheduleTime(String scheduleTime)   { this.scheduleTime = scheduleTime; }

    public String getDate()              { return date; }
    public void setDate(String date)     { this.date = date; }

    public String getRepeat()                { return repeat; }
    public void setRepeat(String repeat)     { this.repeat = repeat; }

    public String getNotes()             { return notes; }

    public boolean isTaken()                { return taken; }
    public void setTaken(boolean taken)     { this.taken = taken; }
}