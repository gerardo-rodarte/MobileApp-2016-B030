package ipn.mobileapp.view.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;


import ipn.mobileapp.view.dialogs.AlertDialog;
import ipn.mobileapp.R;

public class AlertsActivity extends BaseActivity {
//    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_alerts, null, false);
        drawer.addView(contentView, 0);

        ImageButton alertPreview = (ImageButton) findViewById(R.id.alert_preview);
        alertPreview.setOnClickListener(new AlertDialog(this));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return super.onPrepareOptionsMenu(menu);
    }
}

