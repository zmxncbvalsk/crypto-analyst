module crypto.analyst {
    requires com.google.gson;
    requires java.net.http;
    requires java.sql;
    requires java.xml;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.web;
    requires jdk.jsobject;
    requires org.apache.logging.log4j;

    exports pl.dzik;
    opens pl.dzik.dto;
}