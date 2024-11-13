package com.example.findandfriend;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;

    private ImageView profileImageView;
    private EditText nameEditText, emailEditText;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileImageView = findViewById(R.id.edit_profile_image);
        nameEditText = findViewById(R.id.edit_profile_name);
        emailEditText = findViewById(R.id.edit_profile_email);
        Button saveButton = findViewById(R.id.btn_save_profile);

        // get ProfileActivity current data
        Intent intent = getIntent();
        nameEditText.setText(intent.getStringExtra("name"));
        emailEditText.setText(intent.getStringExtra("email"));
        imageUri = intent.getParcelableExtra("imageUri");
        if (imageUri != null) {
            profileImageView.setImageURI(imageUri);
        }

        // choose img
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // load photos
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

        // save edited info and return
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("name", nameEditText.getText().toString());
                returnIntent.putExtra("email", emailEditText.getText().toString());
                returnIntent.putExtra("imageUri", imageUri); // 传递选中的图片URI
                setResult(RESULT_OK, returnIntent);
                finish(); // 结束 EditProfileActivity，返回 ProfileActivity
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            // get img URI
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
