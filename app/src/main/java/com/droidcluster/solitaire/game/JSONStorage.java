package com.droidcluster.solitaire.game;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidcluster.solitaire.model.Stats;
import com.droidcluster.solitaire.model.Table;

public class JSONStorage implements Storage {
    private static final String UTF_8 = "UTF-8";
    private static final String PERSISTENCE_FILENAME = "tbl.dc";
    private static final String STATS_FILENAME = "stats.dc";
    private static final String CONFIG_FILENAME = "config.dc";
    private static final int FILE_READ_BUFFER_SIZE = 512;
    private JSONSerializer serializer = new JSONSerializer();
    private final File dir;
    private boolean[] hintSeen;

    public JSONStorage(File dir) {
        this.dir = dir;
    }

    private File getPersistenceFile() {
        return new File(dir, PERSISTENCE_FILENAME);
    }

    private File getStatsFile(boolean drawThree) {
        String filename = STATS_FILENAME;
        if (drawThree) {
            filename = filename + "_draw3";
        }
        return new File(dir, filename);
    }

    private File getConfigFile() {
        return new File(dir, CONFIG_FILENAME);
    }

    @Override
    public void saveTable(Table table) {
        JSONObject json;
        try {
            json = serializer.table2json(table);
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        json2file(json, getPersistenceFile());
    }

    private void json2file(JSONObject json, File file) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(json.toString().getBytes(UTF_8));
        } catch (IOException e) {
            // ignore
        } finally {
            close(os);
        }
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public Table loadOrCreateTable(boolean drawThree) {
        try {
            String string = file2string(getPersistenceFile());
            if (string != null) {
                Table res = serializer.json2table(new JSONObject(string));
                res.setDrawThree(drawThree);
                return res;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Table table = new Table();
        saveTable(table);
        table.setDrawThree(drawThree);
        return table;
    }

    private String file2string(File file) throws UnsupportedEncodingException {
        FileInputStream is = null;
        try {
            if (!file.isFile()) {
                return null;
            }
            is = new FileInputStream(file);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int read;
            byte[] b = new byte[FILE_READ_BUFFER_SIZE];
            while ((read = is.read(b)) > 0) {
                os.write(b, 0, read);
            }
            return new String(os.toByteArray(), UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            close(is);
        }
    }

    @Override
    public void saveStats(Stats s, boolean drawThree) {
        JSONObject json;
        try {
            json = serializer.stats2json(s);
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        json2file(json, getStatsFile(drawThree));
    }

    @Override
    public Stats loadOrCreateStats(boolean drawThree) {
        try {
            String string = file2string(getStatsFile(drawThree));
            if (string != null) {
                return serializer.json2stats(new JSONObject(string));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new Stats();
    }

    @Override
    public void saveConfig(Properties config) {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(getConfigFile());
            config.store(os, "Written by " + getClass());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            close(os);
        }
    }

    @Override
    public Properties loadOrCreateConfig() {
        Properties config = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(getConfigFile());
            config.load(is);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            close(is);
        }
        return config;
    }

    @Override
    public boolean isHintSeen(int i) {
        if (hintSeen == null) {
            hintSeen = new boolean[] { //
            new File(dir, "hint0.seen").isFile(), //
                    new File(dir, "hint1.seen").isFile(), //
                    new File(dir, "hint2.seen").isFile(), //
            };
        }

        return hintSeen[i];
    }

    @Override
    public void setHintSeen(int i) {
        if (hintSeen[i]) {
            return;
        }

        try {
            hintSeen[i] = true;
            new File(dir, "hint" + i + ".seen").createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
