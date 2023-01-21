package edu.sunderland.babybuy.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.sunderland.babybuy.MainActivity;
import edu.sunderland.babybuy.Auth;
import edu.sunderland.babybuy.R;
import edu.sunderland.babybuy.register.RegisterActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText userEmail, userPassword;
    private Button submitBtn;

    private FirebaseAuth auth;
    private Intent mainActivity;

    private TextView registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);

        userEmail = findViewById(R.id.etLoginEmail);
        userPassword = findViewById(R.id.etLoginPassword);
        submitBtn = findViewById(R.id.loginSubmitBtn);
        registerBtn = findViewById(R.id.registerBtn);

        auth = FirebaseAuth.getInstance();

        mainActivity = new Intent(this, MainActivity.class);

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitBtn.setVisibility(View.INVISIBLE);

                final String email = userEmail.getText().toString();
                final String password = userPassword.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    showMessage("A valid email and password is required");
                    submitBtn.setVisibility(View.VISIBLE);
                } else {
                    logIn(email, password);
                }
            }
        });
    }

    private void logIn(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Auth auth = new Auth(LoginActivity.this);
                    auth.createLoginSession();
                    submitBtn.setVisibility(View.VISIBLE);
                    updateUI();
                } else {
                    showMessage(task.getException().getMessage());
                    submitBtn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateUI() {
        startActivity(mainActivity);
        finish();
    }

    private void showMessage(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            updateUI();
        }
    }
}