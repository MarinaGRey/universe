package com.example.universe.ui.formulario;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.universe.R;
import com.example.universe.ui.book.BookActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;



public class FormActivity extends Activity {

    private static final int PICK_FILE_REQUEST = 2;
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 1001;

    // Firebase
    private FirebaseFirestore db;
    private String userId;

    // Views
    private EditText title_write;
    private EditText author_write;
    private EditText review_write;
    private EditText buy_link;
    private Spinner categories;
    private RatingBar ratingBar;
    private Uri selectedFileUri;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.formulario);

        // Initialize Firebase and Views
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        title_write = findViewById(R.id.title_write);
        author_write = findViewById(R.id.author_write);
        review_write = findViewById(R.id.review_write);
        buy_link = findViewById(R.id.buy_link);
        categories = findViewById(R.id.categories);
        ratingBar = findViewById(R.id.ratingBar_formulario_view);

        // Initialize Spinner
        List<String> categoriesList = new ArrayList<>();
        categoriesList.add("History");
        categoriesList.add("Thriller");
        categoriesList.add("Sci-Fi");
        categoriesList.add("Fantasy");
        categoriesList.add("Romance");
        categoriesList.add("Adventure");
        categoriesList.add("Horror");
        categoriesList.add("Self-help");
        categoriesList.add("Essay");
        categoriesList.add("Comic/Graphic novel");
        categoriesList.add("Manga");
        categoriesList.add("Children");
        categoriesList.add("Poetry");
        categoriesList.add("Cook-book");
        categoriesList.add("Travel");
        categoriesList.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categories.setAdapter(adapter);



        // Set Click Listeners
        Button postButton = findViewById(R.id.post_button);
        postButton.setOnClickListener(v -> {
            // Send data to Firestore and BookActivity
            sendDataToFirestore();
        });

        Button insertImageButton = findViewById(R.id.insert_image_button);
        insertImageButton.setOnClickListener(v -> {
            // Create an intent to pick a file from the device
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");

            // Start the activity for result
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST);
        });

        // VVerify if the permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission was already granted");
        } else {
            // The permission is not granted, ask for it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted");
            } else {
                Toast.makeText(FormActivity.this, "Please, accept the permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the request code is the same as what is passed here
        if (requestCode == PICK_FILE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                selectedFileUri = data.getData();
            }
        }
    }

    private void sendDataToFirestore() {
        // Get data from form fields
        String title = title_write.getText().toString().trim();
        String author = author_write.getText().toString().trim();
        String review = review_write.getText().toString().trim();
        String link = buy_link.getText().toString().trim();
        String category = categories.getSelectedItem().toString();
        float rating = ratingBar.getRating();


        // Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Upload image to Firebase Storage
        Log.d(TAG, "Selected file URI: " + selectedFileUri);

        StorageReference imageRef = storageRef.child("images/" + selectedFileUri.getLastPathSegment());

        Log.d(TAG, "Selected file URI: " + imageRef);
        UploadTask uploadTask = imageRef.putFile(selectedFileUri);


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Get the user document reference
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Retrieve the username from the document
                                username = document.getString("username");

                            }
                        } else {
                            Log.d(TAG, "The user does not exist");
                        }
                    });
        }




        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Image uploaded successfully, get the download URL
            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                // Save post data to Firestore
                Map<String, Object> post = new HashMap<>();
                post.put("title", title);
                post.put("author", author);
                post.put("review", review);
                post.put("link", link);
                post.put("category", category);
                post.put("rating", rating);
                post.put("image_url", downloadUri.toString()); // Save image URL
                post.put("reviewer", username);

                // Add the post document to the current user's "posts" subcollection

                db.collection("users").document(userId).collection("posts")
                        .add(post)
                        .addOnSuccessListener(documentReference -> {
                            // Post added successfully
                            Toast.makeText(FormActivity.this, "Post added successfully", Toast.LENGTH_SHORT).show();
                            // Proceed to BookActivity
                            sendDataToBookActivity();
                        })
                        .addOnFailureListener(e -> {
                            // Handle errors
                            Toast.makeText(FormActivity.this, "Error adding post", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            // Handle image upload failure
            Toast.makeText(FormActivity.this, "Error uploading image", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendDataToBookActivity() {
        // Get data from form fields
        String title = title_write.getText().toString().trim();
        String author = author_write.getText().toString().trim();
        String review = review_write.getText().toString().trim();
        String link = buy_link.getText().toString().trim();
        String category = categories.getSelectedItem().toString();
        float rating = ratingBar.getRating();

        // Create intent to start BookActivity
        Intent intent = new Intent(FormActivity.this, BookActivity.class);
        // Pass data to BookActivity using extras or intent extras
        intent.putExtra("title", title);
        intent.putExtra("author", author);
        intent.putExtra("review", review);
        intent.putExtra("link", link);
        intent.putExtra("category", category);
        intent.putExtra("rating", rating);

        // Start BookActivity
        startActivity(intent);
    }
}
