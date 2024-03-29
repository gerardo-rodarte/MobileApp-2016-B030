package ipn.mobileapp.presenter.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import ipn.mobileapp.R;
import ipn.mobileapp.model.enums.Crud;
import ipn.mobileapp.model.enums.RequestType;
import ipn.mobileapp.model.enums.Servlets;
import ipn.mobileapp.model.pojo.Vehicle;
import ipn.mobileapp.model.service.OkHttpServletRequest;
import ipn.mobileapp.model.utility.JsonUtils;
import ipn.mobileapp.presenter.adapter.VehicleAdapter;
import ipn.mobileapp.presenter.dialogs.VehicleDialog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VehiclesActivity extends BaseActivity {
    private static final String SELECT_ALL = "SELECT_ALL";

    private ListView lvVehicles;
    private FloatingActionButton addVehicle;
    private View contentView;
    private TextView tvEmpty;
    private boolean usingAnotherVehicle = false;

    private ArrayList<Vehicle> vehicles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        contentView = inflater.inflate(R.layout.activity_list_view, null, false);
        drawer.addView(contentView, 0);

        getComponents();
        setComponentAttributes();
        getVehicles();
    }

    private void setListView() {
        if (vehicles == null)
            vehicles = new ArrayList<>();

        VehicleAdapter adapter = new VehicleAdapter(this, R.layout.listview_vehicle_item, vehicles, dismissDialog, usingAnotherVehicle);
        lvVehicles.setAdapter(adapter);
        if (lvVehicles.getCount() != 0)
            tvEmpty.setVisibility(View.GONE);
        else
            tvEmpty.setVisibility(View.VISIBLE);
    }

    private void getComponents() {
        lvVehicles = (ListView) findViewById(R.id.lv_items);
        addVehicle = (FloatingActionButton) findViewById(R.id.fltng_btn_add_item);
        tvEmpty = (TextView) findViewById(R.id.tv_empty_list_view);
    }

    private void setComponentAttributes() {
        tvEmpty.setText(R.string.msj_no_vehicles);
        addVehicle.setOnClickListener(new VehicleDialog(this, null, dismissDialog, Crud.CREATE));
    }

    public void getVehicles() {
         Map<String, String> params = new ArrayMap<>();
        params.put("expression", SELECT_ALL);
        params.put("owner", id);
        if (isSubUser)
            params.put("id", userId);

        OkHttpServletRequest request = new OkHttpServletRequest(VehiclesActivity.this);
        Request builtRequest = request.buildRequest(Servlets.VEHICLE, RequestType.GET, params);
        OkHttpClient client = request.buildClient();
        client.newCall(builtRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                processResults(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                processResults(response.body().string());
            }
        });
    }

    private void processResults(final String response) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (response != null && JsonUtils.isValidJson(response)) {
                    JsonObject json = (JsonObject) new JsonParser().parse(response);
                    if (json.has("data")) {
                        TypeToken type = new TypeToken<ArrayList<Vehicle>>() {
                        };
                        vehicles = new Gson().fromJson(json.get("data").getAsJsonArray(), type.getType());
                        usingAnotherVehicle = false;
                        for(Vehicle vehicle: vehicles)
                            if(vehicle.getUserData() != null && vehicle.getUserData().getId() != null && vehicle.getUserData().getId().equalsIgnoreCase(id)) {
                                usingAnotherVehicle = true;
                                break;
                            }
                    } else if (json.has("warnings")) {
                        vehicles = null;
                    }
                    setListView();
                } else
                    Toast.makeText(VehiclesActivity.this, getString(R.string.error_server), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return super.onPrepareOptionsMenu(menu);
    }

    private DialogInterface.OnDismissListener dismissDialog = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            getVehicles();
        }
    };
}
