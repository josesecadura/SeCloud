package com.example.secloud.users;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.example.secloud.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import home.HomeActivity;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        View rootView = findViewById(android.R.id.content);
        rootView.setAlpha(0f); // Establecer la opacidad de la vista raíz como 0 (invisible)
        rootView.animate().alpha(1f).setDuration(1500); // Animar la opacidad de la vista raíz a 1 (visible) en 1000 milisegundos
        email = findViewById(R.id.textNumber);
        password = findViewById(R.id.textPassword);
        mAuth = FirebaseAuth.getInstance();

    }
    public void click_login(View view) {
        if (!email.getText().toString().isEmpty() || !password.getText().toString().isEmpty()) {
            mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful() && mAuth.getCurrentUser().isEmailVerified()) {
                        goHome(mAuth.getCurrentUser().getUid());
                    } else {
                        //Compruebo a ver que ha puesto mal y muestro el error
                        if(password.getText().toString().isEmpty()){
                            password.setError("Introduzca una contraseña");
                        }
                        if(email.getText().toString().isEmpty()){
                            email.setError("Introduzca un email");
                        }
                        if(!mAuth.getCurrentUser().isEmailVerified()){
                            email.setError("Verifique su email");
                        }
                    }
                }
            });
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle("Error");
            builder.setMessage("Por favor, rellene todos los campos");
            builder.setPositiveButton("Aceptar", null);
            builder.create().show();
        }
    }

    public void click_google(View view) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Cerrar la sesión actual de Google antes de iniciar el flujo de inicio de sesión
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Iniciar el flujo de inicio de sesión con Google
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Verificar si el usuario ya inició sesión con esta cuenta
            String email = account.getEmail();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail().equals(email)) {
                Toast.makeText(LoginActivity.this, "Ya has iniciado sesión con esta cuenta", Toast.LENGTH_SHORT).show();
                goHome(currentUser.getUid());
                return;
            }

            // Continuar con el inicio de sesión
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        String uid = mAuth.getCurrentUser().getUid();
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        //Si no existe la base de datos, la crea y si existe, la obtiene
                        DatabaseReference myRef = database.getReference("users");
                        // Añadimos a la ruta el uid del usuario actual para comprobar si existe en la base de datos
                        myRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Toast.makeText(LoginActivity.this, "El usuario ya existe", Toast.LENGTH_SHORT).show();
                                    goHome(uid);
                                } else {
                                    Toast.makeText(LoginActivity.this, "El usuario no existe", Toast.LENGTH_SHORT).show();
                                    myRef.child(uid).child("email").setValue(account.getEmail());
                                    myRef.child(uid).child("name").setValue(account.getDisplayName());
                                    myRef.child(uid).child("photo").setValue(account.getPhotoUrl().toString());
                                    goHome(currentUser.getUid());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(LoginActivity.this, "Error al acceder a la base de datos", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "Inicio de sesión fallido", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (ApiException e) {
            Toast.makeText(LoginActivity.this, "Inicio de sesión fallido", Toast.LENGTH_SHORT).show();
        }
    }


    public void goHome(String uid) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
    }

    public void click_register(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        //Le voy a pasar siempre el UID del usuario que se logea
        startActivity(intent);
    }

    public void click_recuperar(View view) {
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            if (currentUser.isEmailVerified() && currentUser.getDisplayName() != null) {
                goHome(currentUser.getUid());
            }
        }
    }
}