package com.example.secloud.users;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.example.secloud.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import home.HomeActivity;
import modelos.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText name, email, number, password, password2;
    private Switch passwordSwitch;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        name = findViewById(R.id.textName);
        email = findViewById(R.id.textEmail);
        number = findViewById(R.id.textNumber);
        password = findViewById(R.id.textPassword);
        password2 = findViewById(R.id.textPassword2);
        passwordSwitch = findViewById(R.id.passwordSwitch);
        mAuth = FirebaseAuth.getInstance();

        passwordSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    //Pongo un hilo de 5 segundos para que se vea el texto y luego se oculte descativo el poder copiar y pegar y pulsar switch
                    password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    password2.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    passwordSwitch.setClickable(false);
                    passwordSwitch.setChecked(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(5000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        passwordSwitch.setClickable(true);
                                        passwordSwitch.setChecked(false);
                                        password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                        password2.setTransformationMethod(PasswordTransformationMethod.getInstance());
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        });
    }

    public void click_cancel(View view) {
        finish();
    }

    public void click_confirm(View view) {
        String inputName = name.getText().toString().trim();
        String inputEmail = email.getText().toString().trim();
        String inputNumber = number.getText().toString().trim();
        String inputPassword = password.getText().toString().trim();
        String inputPassword2 = password2.getText().toString().trim();

        boolean isValid = true;

        if (inputName.isEmpty()) {
            name.setError("El nombre no puede estar vacío");
            isValid = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
            email.setError("El correo electrónico no es válido");
            isValid = false;
        }

        if (!Patterns.PHONE.matcher(inputNumber).matches()) {
            number.setError("El número de teléfono no es válido");
            isValid = false;
        }

        if (TextUtils.isEmpty(inputPassword) || inputPassword.length() < 6) {
            password.setError("La contraseña debe tener al menos 6 caracteres");
            isValid = false;
        }

        if (!inputPassword.equals(inputPassword2)) {
            password2.setError("Las contraseñas no coinciden");
            isValid = false;
        }

        if (isValid) {
            mAuth.createUserWithEmailAndPassword(inputEmail, inputPassword).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Debo comprobar que el email no está ya registrado
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference("users");
                        User user = new User(inputName, inputEmail, inputNumber);
                        String userId = mAuth.getCurrentUser().getUid();
                        myRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                                    builder.setTitle("Error");
                                    builder.setMessage("El email ya está registrado");
                                    builder.setPositiveButton("Aceptar", null);
                                    email.setText("");
                                    name.setText("");
                                    number.setText("");
                                    password.setText("");
                                    password2.setText("");
                                } else {

                                    // Mover la llamada al método sendEmailVerification() aquí
                                    mAuth.getCurrentUser().sendEmailVerification()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        myRef.child(userId).child("email").setValue(inputEmail);
                                                        myRef.child(userId).child("name").setValue(inputName);
                                                        myRef.child(userId).child("number").setValue(inputNumber);
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                                                        builder.setTitle("Éxito");
                                                        builder.setMessage("El usuario se ha registrado correctamente se le ha enviado un correo de verificación");
                                                        // Cuando le dé al botón de aceptar, se cierra la actividad
                                                        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                //Le paso el email para que se rellene automáticamente en el login
                                                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                                intent.putExtra("email", inputEmail);
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        });
                                                        builder.setCancelable(false);
                                                        builder.create().show();
                                                    } else {
                                                        // Hubo un error al enviar el correo de verificación
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                                                        builder.setTitle("Error");
                                                        builder.setMessage("Hubo un error al enviar el correo de verificación");
                                                        builder.setPositiveButton("Aceptar", null);
                                                        builder.create().show();
                                                    }
                                                }
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage("Hubo un error al registrar el usuario");
                        builder.setPositiveButton("Aceptar", null);
                        builder.create().show();
                        email.setText("");
                        name.setText("");
                        number.setText("");
                        password.setText("");
                        password2.setText("");
                    }
                }
            });
        }
    }

}