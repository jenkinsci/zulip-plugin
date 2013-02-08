package hudson.plugins.humbug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Room {
    private Humbug humbug;
    private String name;
    private String id;

    public Room(Humbug cf, String name, String id) {
        super();
        this.humbug = cf;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public void speak(String message) throws IOException {
        humbug.post("room/" + id + "/speak.xml", "<message><type>TextMessage</type><body>" + message + "</body></message>");
    }
}
