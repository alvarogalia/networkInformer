package com.alvarogalia.networkInformer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.*;

public class Main {
    private static final SimpleDateFormat formatLong = new SimpleDateFormat("yyyyMMddHHmmss");
    public static void main(String[] args){
        String holding = "NOGALES";
        String ubicacion = "PRINCIPAL";
        FileInputStream serviceAccount = null;
        try {
            serviceAccount = new FileInputStream("controlacceso-fc68c-firebase-adminsdk-22zra-efe9ebaead.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://controlacceso-fc68c.firebaseio.com/")
                    .build();
            FirebaseApp.initializeApp(options);

            while(true){
                try {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    Timestamp timestamp = new Timestamp(currentTimeMillis());
                    InetAddress thisIp = InetAddress.getLocalHost();

                    //check parameter
                    String path = "HOLDING/" + holding + "/UBICACION/" + ubicacion + "/PING/";
                    DatabaseReference refPing = database.getReference(path);
                    refPing.child(thisIp.getHostName()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            boolean shouldReboot = false;
                            Map<String, Object> mapPing = new HashMap<>();
                            try{
                                System.out.println(path);
                                System.out.println(Utils.getIp());
                                System.out.println(thisIp.getHostAddress());
                                System.out.println(Long.parseLong(formatLong.format(timestamp)));

                                mapPing.put("publicIP", Utils.getIp());
                                mapPing.put("localIP", thisIp.getHostAddress());
                                mapPing.put("timestamp", Long.parseLong(formatLong.format(timestamp)));
                                mapPing.put("shouldReboot", false);

                                for(DataSnapshot snap : dataSnapshot.getChildren()){
                                    if(snap.getKey().equals("shouldReboot")){
                                        shouldReboot = (boolean) snap.getValue();
                                    }
                                }
                                if(shouldReboot){
                                    mapPing.put("rebooting", true);

                                }else{
                                    mapPing.put("rebooting", false);
                                }
                                final boolean shouldReboot2 = shouldReboot;
                                refPing.child(thisIp.getHostName()).setValue(mapPing, (databaseError, databaseReference) -> {
                                    out.println("EJECUTADO!:" + databaseError);
                                    if(shouldReboot2){
                                        System.exit(0);
                                    }
                                });
                            }catch(Exception e){

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    TimeUnit.SECONDS.sleep(60);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}