package com.alvarogalia.networkInformer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.*;

public class Main {
    private static final SimpleDateFormat formatLong = new SimpleDateFormat("yyyyMMddHHmmss");
    public static void main(String[] args){

        FileInputStream serviceAccount = null;
        try {
            serviceAccount = new FileInputStream("controlacceso-fc68c-firebase-adminsdk-22zra-efe9ebaead.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://controlacceso-fc68c.firebaseio.com/")
                    .build();
            FirebaseApp.initializeApp(options);

            final InetAddress thisIp = InetAddress.getLocalHost();
            String macaddress = thisIp.getHostName();
            System.out.println(macaddress);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            out.println(database.getReference("MAC/"+macaddress).toString());
            while(true) {
                database.getReference("MAC/" + macaddress).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String holding = "";
                        String ubicacion = "";

                        for (DataSnapshot snap : dataSnapshot.getChildren()) {
                            out.println(snap);
                            if (snap.getKey().equals("holding")) {
                                holding = snap.getValue().toString();
                            }
                            if (snap.getKey().equals("ubicacion")) {
                                ubicacion = snap.getValue().toString();
                            }
                        }
                        if(holding != "" && ubicacion != "") {
                            System.out.println(holding + "/" + ubicacion);
                            try {
                                Timestamp timestamp = new Timestamp(currentTimeMillis());


                                String path = "HOLDING/" + holding + "/UBICACION/" + ubicacion + "/PING/";
                                DatabaseReference refPing = database.getReference(path);
                                refPing.child(thisIp.getHostName()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        boolean shouldReboot = false;
                                        Map<String, Object> mapPing = new HashMap<>();
                                        try {
                                            System.out.println(path);
                                            System.out.println(Utils.getIp());
                                            System.out.println(thisIp.getHostAddress());
                                            System.out.println(Long.parseLong(formatLong.format(timestamp)));

                                            mapPing.put("publicIP", Utils.getIp());
                                            mapPing.put("localIP", thisIp.getHostAddress());
                                            mapPing.put("timestamp", Long.parseLong(formatLong.format(timestamp)));
                                            mapPing.put("shouldReboot", false);

                                            for (DataSnapshot snap : dataSnapshot.getChildren()) {
                                                if (snap.getKey().equals("shouldReboot")) {
                                                    shouldReboot = (boolean) snap.getValue();
                                                }
                                            }
                                            if (shouldReboot) {
                                                mapPing.put("rebooting", true);

                                            } else {
                                                mapPing.put("rebooting", false);
                                            }
                                            final boolean shouldReboot2 = shouldReboot;
                                            refPing.child(thisIp.getHostName()).setValue(mapPing, (databaseError, databaseReference) -> {
                                                out.println("EJECUTADO!:" + databaseError);
                                                if (shouldReboot2) {
                                                    System.exit(0);
                                                }
                                            });
                                            TimeUnit.SECONDS.sleep(10);
                                        } catch (Exception e) {

                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                TimeUnit.SECONDS.sleep(30);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else{
                            out.println("PARAMETROS NO ENCONTRADOS!");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                TimeUnit.SECONDS.sleep(60);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}