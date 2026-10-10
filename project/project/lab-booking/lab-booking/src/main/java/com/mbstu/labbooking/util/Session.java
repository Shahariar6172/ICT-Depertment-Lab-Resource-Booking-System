package com.mbstu.labbooking.util;

import com.mbstu.labbooking.model.User;

/** Holds the currently logged-in user. */
public class Session {
    private static User currentUser;

    public static void setUser(User user) { currentUser = user; }
    public static User getUser()          { return currentUser; }
    public static void clear()            { currentUser = null; }
}
