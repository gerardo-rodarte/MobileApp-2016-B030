package ipn.mobileapp.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;

import ipn.mobileapp.R;
import ipn.mobileapp.model.enums.RequestType;
import ipn.mobileapp.model.enums.Servlets;
import ipn.mobileapp.model.pojo.Document;
import ipn.mobileapp.model.service.OkHttpServletRequest;
import ipn.mobileapp.model.service.SharedPreferencesManager;
import ipn.mobileapp.model.utility.JsonUtils;
import ipn.mobileapp.presenter.validation.TextValidator;
import ipn.mobileapp.presenter.validation.Validator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DocumentsActivity extends BaseActivity {
    private static final int READ_REQUEST_CODE = 1337;

    private View contentView;

    private EditText etDocumentName;
    private TextView tvDocumentPath;

    private Button btnModifyDocument;
    private Button btnViewDocument;
    private ImageButton imgBtnFindDocument;

    private String id;
    private TextView[] fields;
    private Validator validator = new Validator(this);

    private Document document = null;

    private File file;
    private byte[] fileBytes;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        contentView = inflater.inflate(R.layout.activity_documents, null, false);
        drawer.addView(contentView, 0);

        SharedPreferencesManager manager = new SharedPreferencesManager(this, getString(R.string.current_user_filename));
        id = (String) manager.getValue("id", String.class);

        getDocument();
        getComponents();
        setComponentAttributes();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            fileUri = resultData.getData();
            String path = fileUri.getPath();
            file = new File(path);
            int size = (int) file.length();
            fileBytes = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(getContentResolver().openInputStream(fileUri));
                buf.read(fileBytes, 0, fileBytes.length);
                buf.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*String displayName = null;

            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                displayName = file.getName();
            }*/

            tvDocumentPath.setText(path);
            btnModifyDocument.setEnabled(validator.validateFields(fields));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return super.onPrepareOptionsMenu(menu);
    }

    private void getComponents() {
        imgBtnFindDocument = (ImageButton) contentView.findViewById(R.id.ib_find_document);

        btnModifyDocument = (Button) contentView.findViewById(R.id.btn_modify_document);
        btnViewDocument = (Button) contentView.findViewById(R.id.btn_view_document);

        etDocumentName = (EditText) contentView.findViewById(R.id.et_document_name);
        tvDocumentPath = (TextView) contentView.findViewById(R.id.tv_document_path);

        fields = new TextView[]{etDocumentName, tvDocumentPath};
    }

    private void setComponentAttributes() {
        imgBtnFindDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }
        });
        btnViewDocument.setOnClickListener(viewDocument);
        btnModifyDocument.setOnClickListener(saveDocument);

        etDocumentName.addTextChangedListener(new TextValidator(etDocumentName) {
            @Override
            public void validate(TextView textView, String text) {
                if (text != null && !text.equals(""))
                    document.setName(text);
                btnModifyDocument.setEnabled(validator.validateFields(fields));
            }
        });
    }

    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void getDocument() {
        Map<String, String> params = new ArrayMap<>();
        params.put("userId", id);
        OkHttpServletRequest request = new OkHttpServletRequest(getBaseContext());
        Request builtRequest = request.buildRequest(Servlets.DOCUMENT, RequestType.GET, params);
        OkHttpClient client = request.buildClient();
        client.newCall(builtRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                processResults(null, true);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                processResults(response.body().string(), true);
            }
        });
    }

    private void processResults(final String response, final boolean get) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (response != null && JsonUtils.isValidJson(response)) {
                    JsonObject json = (JsonObject) new JsonParser().parse(response);
                    if (json.has("data")) {
                        document = new Gson().fromJson(json.get("data").getAsString(), Document.class);
                        if (get)
                            btnViewDocument.setVisibility(document.getId() != null ? View.VISIBLE : View.GONE);
                    } else if (json.has("warnings")) {
                        if (get)
                            document = new Document();
                    }
                } else {
                    Toast.makeText(DocumentsActivity.this, getString(R.string.error_server), Toast.LENGTH_SHORT).show();
                    if (get)
                        document = new Document();
                }
            }
        });
    }

    private View.OnClickListener viewDocument = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(document.getSource()));
            startActivity(intent);
        }
    };

    private FloatingActionButton.OnClickListener saveDocument = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(document != null)
                document.setUserId(id);

            int index = fileUri.toString().lastIndexOf(".");
            String finalName  = document.getName() + fileUri.toString().substring(index);
            document.setName(finalName);

            Map<String, String> params = new ArrayMap<>();
            params.put("document", document.toString());

            OkHttpServletRequest request = new OkHttpServletRequest(DocumentsActivity.this);
            String url = request.buildUrl(Servlets.DOCUMENT);
            okhttp3.MultipartBody.Builder multipartBodyBuilder = new okhttp3.MultipartBody.Builder();
            multipartBodyBuilder.setType(okhttp3.MultipartBody.FORM);
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    multipartBodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
                }
            }

            MediaType type = MediaType.parse(getContentResolver().getType(fileUri));
            multipartBodyBuilder.addFormDataPart(
                    "file",
                    file.getName(),
                    okhttp3.RequestBody.create(type, fileBytes)
            );
            Request.Builder requestBuilder = new Request.Builder().url(url);
            Request builtRequest = requestBuilder.post(multipartBodyBuilder.build()).build();
            //Request builtRequest = request.buildRequest(Servlets.DOCUMENT, RequestType.POST, params, file);
            OkHttpClient client = request.buildClient();
            client.newCall(builtRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    processResults(null, false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    processResults(response.body().string(), false);
                }
            });
            //new sendFile().execute(new String[] { url });
        }
    };

    private class sendFile extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection httpConn = null;

            try {
                InputStream inputStream = getAssets().open(file.getAbsolutePath());

                System.out.println("File to upload: " + file);

                // creates a HTTP connection
                URL url1 = new URL(urls[0]);
                httpConn = (HttpURLConnection) url1.openConnection();
                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("fileName", file.getName());
                httpConn.connect();
                // sets file name as a HTTP header


                OutputStream outputStream = httpConn.getOutputStream();

                // Opens input stream of the file for reading data


                byte[] buffer = new byte[255];
                int bytesRead = -1;

                System.out.println("Start writing data...");

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                System.out.println("Data was written.");
                outputStream.close();
                inputStream.close();
            } catch (SocketTimeoutException e) {
                Log.e("Debug", "error: " + e.getMessage(), e);
            } catch (MalformedURLException ex) {
                Log.e("Debug", "error: " + ex.getMessage(), ex);
            } catch (IOException ioe) {
                Log.e("Debug", "error: " + ioe.getMessage(), ioe);
            }

            try {

                // always check HTTP response code from server
                int responseCode = httpConn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // reads server's response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                    String response = reader.readLine();
                    System.out.println("Server's response: " + response);
                } else {
                    System.out.println("Server returned non-OK code: " + responseCode);
                }
            } catch (IOException ioex) {
                Log.e("Debug", "error: " + ioex.getMessage(), ioex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}