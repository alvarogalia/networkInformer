package com.alvarogalia.networkInformer;

import com.alvarogalia.networkInformer.obj.Flag;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.*;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

public class Main {
    private static final SimpleDateFormat formatDate = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat formatHour = new SimpleDateFormat("HHmmss");
    public static void main(String[] args){

        FileInputStream serviceAccount = null;
        try {
            serviceAccount = new FileInputStream("controlacceso-fc68c-firebase-adminsdk-22zra-efe9ebaead.json");
            FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).setDatabaseUrl("https://controlacceso-fc68c.firebaseio.com/").build();
            FirebaseApp.initializeApp(options);
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            final String path = "CONFIGURACION-CAPTURADORES";
            final InetAddress thisIp = InetAddress.getLocalHost();
            final String hostName = thisIp.getHostName();
            final Flag flag = new Flag();
            database.getReference(path).child("DESKTOP-0TH4HNH").child("publicIP").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    flag.ipFTP = dataSnapshot.getValue().toString();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


            int timeout = 60;
            flag.continua = true;
            out.println(database.getReference(path).child(hostName).toString());
            database.getReference(path).child(hostName).child("streamIP").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        long cantidad = dataSnapshot.getChildrenCount();
                        long puntero = 0;

                        if(cantidad>0){
                            File fileOut = new File("ip.txt");
                            FileOutputStream fos = null;
                            fos = new FileOutputStream(fileOut);
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                            for(DataSnapshot snap : dataSnapshot.getChildren()) {
                                puntero++;
                                flag.ipEscrito = true;
                                bw.write(snap.getValue().toString());
                                if(puntero<cantidad){
                                    bw.newLine();
                                }
                            }
                            bw.close();
                            out.println("ip.txt Escrito");
                            Map<String, Object> mapPing = new HashMap<>();
                            mapPing.put("INFO_INFORMER", "EJECUTANDO DAEMON...");
                            database.getReference(path).child(hostName).updateChildrenAsync(mapPing);
                        }else{
                            File fileOut = new File("ip.txt");
                            if(fileOut.exists()){
                                fileOut.delete();
                            }
                        }
                    } catch (FileNotFoundException e) {
                        System.out.println("Error:" + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("Error:" + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            database.getReference(path).child(hostName).child("reiniciar").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean reiniciar = (boolean) dataSnapshot.getValue();
                    Map<String, Object> mapPing = new HashMap<>();
                    if(reiniciar){
                        flag.reinicia = true;
                        mapPing.put("INFO_INFORMER", "REINICIO SOLICITADO...");
                        mapPing.put("reiniciar", false);
                        database.getReference(path).child(hostName).updateChildrenAsync(mapPing);
                    }
                    database.getReference(path).child(hostName).updateChildrenAsync(mapPing);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Map<String, Object> mapPing = new HashMap<>();
                    mapPing.put("reiniciar", false);
                    database.getReference(path).child(hostName).updateChildrenAsync(mapPing);
                }
            });

            while(flag.continua) {
                try{
                    Timestamp timestamp = new Timestamp(currentTimeMillis());
                    Map<String, Object> mapPing = new HashMap<>();

                    if(flag.reinicia){
                        mapPing.put("INFO_INFORMER", "REINICIANDO...");
                        flag.continua = false;
                        timeout = 10;
                    }else{
                        if(!flag.ipEscrito){
                            mapPing.put("INFO_INFORMER", "Esperando por tag streamIP/0");
                        }
                    }
                    mapPing.put("localIP", thisIp.getHostAddress());
                    mapPing.put("publicIP", Utils.getIp());
                    mapPing.put("datePING", formatDate.format(timestamp));
                    mapPing.put("hourPING", formatHour.format(timestamp));
                    mapPing.put("ERROR_INFORMER", "");
                    database.getReference(path).child(hostName).updateChildrenAsync(mapPing);
                }catch(Exception e){
                    e.printStackTrace();
                    Map<String, Object> mapPing = new HashMap<>();
                    mapPing.put("ERROR_INFORMER", e.getMessage());
                    database.getReference(path).child(hostName).updateChildrenAsync(mapPing);
                }

                try{
                    File file = new File("salida/");
                    int cantidad = file.listFiles().length;

                    Map<String, Object> mapPing = new HashMap<>();
                    mapPing.put("INFO_FTP", "ENVIANDO " + cantidad + " imagenes a ip " + flag.ipFTP);
                    database.getReference(path).child(hostName).updateChildrenAsync(mapPing);

                    for(File child : file.listFiles()){
                        if(child.exists()){
                            String salida = Utils.enviaFTP(flag.ipFTP, 21, "Alvaro", "Alvarito3.", "salida/"+child.getName(), child.getName());
                            Map<String, Object> mapPing2 = new HashMap<>();
                            mapPing2.put("INFO_FTP2", salida + child.getName());
                            database.getReference(path).child(hostName).updateChildrenAsync(mapPing2);
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                TimeUnit.SECONDS.sleep(timeout);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}