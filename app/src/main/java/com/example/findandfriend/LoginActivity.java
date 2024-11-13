package com.example.findandfriend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private static final String FILE_NAME = "user_credentials.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // initial UI
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // SET LOGIN EVENT
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        // SET REGISTER EVENT
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegister();
                Toast.makeText(LoginActivity.this, "Register clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Verify input fields
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save email and password to local file
        saveCredentials(email, password);
        new RegisterTask().execute(email, password);
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // verify
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load saved credentials and verify
        verifyWithServer(email, password, success -> {
            if (success) {
                // login to server and save to local
                saveCredentials(email, password);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // fail to login on server check local file
                String[] savedCredentials = loadCredentials();
                if (savedCredentials != null) {
                    String savedEmail = savedCredentials[0];
                    String savedPassword = savedCredentials[1];
                    //new RegisterTask().execute(email, password);
                    // local username and password verificaiton
                    if (email.equals(savedEmail) && password.equals(savedPassword)) {
                        Toast.makeText(this, "Logged in with locally saved credentials", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No registered users. Please register first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Save credentials to local file
    private void saveCredentials(String email, String password) {
        String credentials = email + "," + password;
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos.write(credentials.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String[] loadCredentials() {
        FileInputStream fis = null;
        try {
            fis = openFileInput(FILE_NAME);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            String credentials = new String(buffer);
            return credentials.split(",");  // split by comma to separate email and password
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void verifyWithServer(String email, String password, OnLoginResultCallback callback) {
        String serverUrl = getString(R.string.IP) +"/login";
        System.out.println("serverurl is "+ serverUrl);
        JSONObject loginData = new JSONObject();
        try {
            loginData.put("email", email);
            loginData.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // create request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, serverUrl, loginData,
                response -> {
                    try {

                        if (response.has("status") && response.getString("status").equals("Login successful")) {
                            String token = response.getString("token");
                            saveToken(token);  //save token
                            callback.onResult(true);
                        } else {
                            callback.onResult(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onResult(false);
                    }
                },
                error -> {
                    if (error instanceof TimeoutError) {
                        Toast.makeText(this, "Server timed out. Try again.", Toast.LENGTH_SHORT).show();
                    } else if (error instanceof NoConnectionError) {
                        Toast.makeText(this, "No connection to server.", Toast.LENGTH_SHORT).show();
                    } else if (error instanceof AuthFailureError) {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    } else if (error instanceof ServerError) {
                        Toast.makeText(this, "Server error.", Toast.LENGTH_SHORT).show();
                    } else if (error instanceof NetworkError) {
                        Toast.makeText(this, "Network error.", Toast.LENGTH_SHORT).show();
                    } else if (error instanceof ParseError) {
                        Toast.makeText(this, "Error parsing response.", Toast.LENGTH_SHORT).show();
                    }
                    error.printStackTrace();
                    callback.onResult(false);
                }
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                200000, // Initial timeout in ms (e.g., 10 seconds)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // Retry count
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // add to request quene
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }
    interface OnLoginResultCallback {
        void onResult(boolean success);
    }

    // save JWT Token
    private void saveToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        System.out.println("token is"+token);
        editor.putString("token", token);
        editor.apply();
    }


    private class RegisterTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String email = params[0];
            String password = params[1];

            HttpURLConnection conn = null;
            InputStream inputStream = null;
            BufferedReader in = null;

            try {
                URL url = new URL(getString(R.string.IP) + "/register");
                conn = (HttpURLConnection) url.openConnection();

                // Set timeouts and method
                conn.setConnectTimeout(30000); // Connection timeout
                conn.setReadTimeout(30000);    // Read timeout
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Create JSON request
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("password", password);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Get response code and read response
                int responseCode = conn.getResponseCode();
                inputStream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                in = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                // Check response
                JSONObject responseJson = new JSONObject(response.toString());
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    return "Registration successful";
                } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    return responseJson.getString("error");
                } else {
                    return "Registration failed";
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }
}
