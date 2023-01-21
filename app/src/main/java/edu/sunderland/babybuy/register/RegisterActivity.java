package edu.sunderland.babybuy.register;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import edu.sunderland.babybuy.MainActivity;
import edu.sunderland.babybuy.Auth;
import edu.sunderland.babybuy.R;
import edu.sunderland.babybuy.login.LoginActivity;

public class RegisterActivity extends AppCompatActivity {
    private final String TAG = "register";
    private EditText userEmail, userPassword, userPasswordConfirm, userName;
    private Button submitBtn;
    FirebaseFirestore fireStore;
    private FirebaseAuth auth;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userEmail = findViewById(R.id.etRegisterEmail);
        userPassword = findViewById(R.id.etRegisterPassword);
        userPasswordConfirm = findViewById(R.id.etRegisterPasswordConfirm);
        userName = findViewById(R.id.etRegisterName);
        submitBtn = findViewById(R.id.registerSubmitBtn);
        TextView loginBtn = findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        auth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitBtn.setVisibility(View.INVISIBLE);
                final String email = userEmail.getText().toString();
                final String password = userPassword.getText().toString();
                final String passwordConfirmation = userPasswordConfirm.getText().toString();
                final String name = userName.getText().toString();

                if (email.isEmpty() || name.isEmpty() || password.isEmpty() || !password.equals(passwordConfirmation)) {
                    showMessage("Valid details are required");
                    submitBtn.setVisibility(View.VISIBLE);
                } else {
                    register(email, name, password);
                }
            }
        });
    }

    private void register(String email, String name, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Auth auth = new Auth(RegisterActivity.this);
                            auth.createLoginSession();
                            showMessage("Account created");
                            userID = RegisterActivity.this.auth.getCurrentUser().getUid();
                            DocumentReference documentReference = fireStore.collection("users").document(userID);
                            Map<String, Object> user = new HashMap<>();
                            user.put("Name", name);
                            user.put("Email", email);
                            user.put("Password", password);
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.e(TAG, "onSuccess: New account created for user with user ID " + userID);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure: " + e.toString());
                                }
                            });
                            updateUserInfoWithoutPhoto(name, RegisterActivity.this.auth.getCurrentUser());
                        } else {
                            showMessage("Failed to create account " + task.getException().getMessage());
                            submitBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void updateUserInfoWithoutPhoto(String name, FirebaseUser user) {
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(request)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            showMessage("Registration Complete");
                            updateUI();
                        }
                    }
                });
    }

    private void updateUI() {
        Intent homeActivity = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(homeActivity);
        finish();
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}