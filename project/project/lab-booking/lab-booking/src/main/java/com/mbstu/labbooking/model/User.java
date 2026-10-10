package com.mbstu.labbooking.model;

public class User {
    private int id;
    private String name;
    private String studentId;
    private String email;
    private String role;   // STUDENT / ADMIN

    public User(int id, String name, String studentId, String email, String role) {
        this.id = id;
        this.name = name;
        this.studentId = studentId;
        this.email = email;
        this.role = role;
    }

    public int getId()            { return id; }
    public String getName()       { return name; }
    public String getStudentId()  { return studentId; }
    public String getEmail()      { return email; }
    public String getRole()       { return role; }
    public boolean isAdmin()      { return "ADMIN".equals(role); }
}
