package ipn.mobileapp.model.pojo;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class Device {
    private String serialKey;
    private String version;

    public Device() {
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getSerialKey() {
        return serialKey;
    }

    public void setSerialKey(String serialKey) {
        this.serialKey = serialKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
