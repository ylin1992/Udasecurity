module com.udacity.catpoint.security {
    requires miglayout;
    requires java.desktop;
    requires com.udacity.catpoint.image;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    opens com.udacity.catpoint.security.data to com.google.gson;
}
