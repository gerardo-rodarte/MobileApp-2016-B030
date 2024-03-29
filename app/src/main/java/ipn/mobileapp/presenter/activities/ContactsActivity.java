package ipn.mobileapp.presenter.activities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.ArrayMap;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import ipn.mobileapp.model.enums.RequestType;
import ipn.mobileapp.model.enums.Servlets;
import ipn.mobileapp.model.helper.SmsHelper;
import ipn.mobileapp.model.pojo.AlcoholTest;
import ipn.mobileapp.model.pojo.Coordinate;
import ipn.mobileapp.model.utility.JsonUtils;
import ipn.mobileapp.model.pojo.Contact;
import ipn.mobileapp.model.service.OkHttpServletRequest;
import ipn.mobileapp.model.service.SharedPreferencesManager;
import ipn.mobileapp.presenter.adapter.ContactAdapter;
import ipn.mobileapp.presenter.dialogs.ContactDialog;
import ipn.mobileapp.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ContactsActivity extends BaseActivity {
    private static final String SELECT_ALL = "SELECT_ALL";

    private ListView lvContacts;
    private FloatingActionButton addContact;
    private View contentView;
    private TextView tvEmpty;

    private static ArrayList<Contact> contacts;
    private AlcoholTest alcoholTest;

    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        contentView = inflater.inflate(R.layout.activity_list_view, null, false);
        drawer.addView(contentView, 0);

        SharedPreferencesManager manager = new SharedPreferencesManager(this, getString(R.string.current_user_filename));
        id = (String) manager.getValue("id", String.class);

        getComponents();
        setComponentAttributes();
        getContacts();

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null)
                alcoholTest = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssz").create().fromJson(extras.getString("alcoholTest"), AlcoholTest.class);
        }
    }

    private void setListView() {
        ContactAdapter adapter = new ContactAdapter(this, R.layout.listview_contact_item, contacts, dismissDialog);
        lvContacts.setAdapter(adapter);
        if (lvContacts.getCount() != 0)
            tvEmpty.setVisibility(View.GONE);
        else {
            if(alcoholTest != null)
                return;

            tvEmpty.setVisibility(View.VISIBLE);

            Button registerFirstContact = new Button(this);
            registerFirstContact.setOnClickListener(new ContactDialog(this, null, dismissDialog, false));
            registerFirstContact.performClick();
        }
    }

    private void getComponents() {
        lvContacts = (ListView) findViewById(R.id.lv_items);
        addContact = (FloatingActionButton) findViewById(R.id.fltng_btn_add_item);
        tvEmpty = (TextView) findViewById(R.id.tv_empty_list_view);
    }

    private void setComponentAttributes() {
        tvEmpty.setText(R.string.msj_no_emergency_contacts);
        addContact.setOnClickListener(new ContactDialog(this, null, dismissDialog));
    }

    public void getContacts() {
        Map<String, String> params = new ArrayMap<>();
        params.put("expression", SELECT_ALL);
        params.put("id", id);

        OkHttpServletRequest request = new OkHttpServletRequest(ContactsActivity.this);
        Request builtRequest = request.buildRequest(Servlets.CONTACT, RequestType.GET, params);
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
                        TypeToken type = new TypeToken<ArrayList<Contact>>() {
                        };

                        if (contacts == null)
                            contacts = new ArrayList<>();

                        ArrayList<Contact> temp = new Gson().fromJson(json.get("data").getAsString(), type.getType());
                        contacts.removeAll(temp);
                        contacts.addAll(temp);

                        if (alcoholTest != null)
                            sendMultipleSms();
                    } else if (json.has("warnings")) {
                        JsonObject warnings = json.getAsJsonObject("warnings");
                        if(warnings.has("noneFound"))
                            contacts = new ArrayList<>();
                        else
                            contacts = null;
                    }
                } else
                    Toast.makeText(ContactsActivity.this, getString(R.string.error_server), Toast.LENGTH_SHORT).show();

                if (contacts != null)
                    setListView();
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
            contacts = null;
            getContacts();
        }
    };

    private void sendMultipleSms() {
        SmsManager smsManager = SmsManager.getDefault();
        PendingIntent sentPI;
        String SENT = "SMS_SENT";

        SmsHelper helper = new SmsHelper(this);
        sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

        Coordinate coordinate = alcoholTest.getCoordinate();
        String fullName = user.getName() + " " + user.getPaternalSurname() + " " + user.getMaternalSurname();
        String sms = helper.generateSmsBody(alcoholTest.getAlcoholicState(), coordinate, fullName);
        try {
            ArrayList<String> parts = smsManager.divideMessage(sms);
            ArrayList<PendingIntent> sentList = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++)
                sentList.add(sentPI);

            if (contacts != null)
                for (Contact contact : contacts) {
                    smsManager.sendMultipartTextMessage(contact.getPhoneNumber(), null, parts, sentList, null);
                    Thread.sleep(500);
                    Toast.makeText(this, "Mensaje enviado a " + contact.getName() + " (" + contact.getPhoneNumber() + ")", Toast.LENGTH_SHORT).show();
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
