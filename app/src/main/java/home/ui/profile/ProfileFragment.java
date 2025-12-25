package home.ui.profile;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.secloud.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_CAMERA = 101;
    private DatabaseReference usersRef;
    private ToggleButton toggleButton;
    private FirebaseUser currentUser;
    private TextView texntNameLabel, textEmailLabel, textNumberLabel;
    private ImageView userImage;
    private TextView textName;
    private TextView textEmail;
    private TextView textNumber;
    private Button btnChangePhoto;
    private LinearLayout layoutEdit;
    private EditText etNewName;
    private EditText etNewPhone;
    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnAccept;
    private Button btnCancel;

    private Button btnEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Obtener referencias a las vistas
        toggleButton = view.findViewById(R.id.btn_show_password);
        texntNameLabel = view.findViewById(R.id.text_name_label);
        textEmailLabel = view.findViewById(R.id.text_email_label);
        textNumberLabel = view.findViewById(R.id.text_telefono);
        userImage = view.findViewById(R.id.image_user);
        textName = view.findViewById(R.id.text_name);
        textEmail = view.findViewById(R.id.text_email);
        textNumber = view.findViewById(R.id.text_number);
        btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        layoutEdit = view.findViewById(R.id.layout_edit);
        etNewName = view.findViewById(R.id.et_new_name);
        etNewPhone = view.findViewById(R.id.et_new_phone);
        etOldPassword = view.findViewById(R.id.et_old_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        btnAccept = view.findViewById(R.id.btn_accept);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnEdit = view.findViewById(R.id.btn_edit);
        // Obtener referencia al nodo del usuario actual en la base de datos
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            usersRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
        }

        // Cargar los datos del usuario
        loadUserData();
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                togglePasswordVisibility(isChecked);
            }
        });
        // Configurar el botón de editar
        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Crear un PopupMenu para seleccionar la foto de perfil
                PopupMenu popupMenu = new PopupMenu(getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.popup_change_photo, popupMenu.getMenu());

                // Agregar un listener para manejar los elementos seleccionados del menú
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menu_gallery) {
                            openGallery();
                            return true;
                        } else if (item.getItemId() == R.id.menu_take) {
                            takePhoto();
                            return true;
                        }
                        return false;
                    }
                });

                // Mostrar el PopupMenu
                popupMenu.show();
            }
        });

        // Configurar el botón de editar
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleEditMode(true);
            }
        });

        // Configurar el botón de aceptar cambios
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });

        // Configurar el botón de cancelar cambios
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleEditMode(false);
            }
        });

        return view;
    }


    private void togglePasswordVisibility(boolean isChecked) {
        //Si lo pulsa se mostraran las contraseñas durante 5 segundos
        if (isChecked) {
            etOldPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            toggleButton.setEnabled(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleButton.setChecked(false);
                    toggleButton.setEnabled(true);
                    //Hago que no se peda pulsar el botón durante 5 segundos
                    etOldPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }, 5000);
        }
    }

    private void takePhoto() {
        //Abrir la cámara del dispositivo para tomar una foto
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        //Abrir la galería de imágenes del dispositivo para seleccionar una foto
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }
    //Recojo el resultado de la actividad de la cámara o la galería
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Compruebo si el resultado es de la cámara
        if(requestCode==REQUEST_CAMERA){
            if(resultCode==RESULT_OK){
                //Obtengo la imagen capturada por la cámara
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                //Guardo la imagen en la base de datos
                saveImage(bitmap);
            }
        }else if(requestCode==REQUEST_GALLERY){
            if(resultCode==RESULT_OK){
                //Obtengo la imagen seleccionada de la galería
                Uri imageUri = data.getData();
                try {
                    //Obtengo el bitmap de la imagen seleccionada
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    //Guardo la imagen en la base de datos
                    saveImage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveImage(Bitmap bitmap) {
        //Unicamente cambio la uri de la imagen en la base de datos y la imagen de la vista
        //Si la tomo de la camara debere guardarla en firebase storage y obtener la uri
        //Si la selecciono de la galeria obtengo la uri directamente
        if(bitmap!=null){
            //Obtengo la uri de la imagen
            Uri imageUri = getImageUri(bitmap);
            //Guardo la uri en la base de datos
            usersRef.child("photo").setValue(imageUri.toString());
            //Muestro la imagen en la vista
            userImage.setImageBitmap(bitmap);
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        //Obtengo la uri de la imagen
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void loadUserData() {
        if (usersRef != null) {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        if (snapshot.child("number").exists()) {
                            String phone = snapshot.child("number").getValue(String.class);
                            textNumber.setText(phone);
                        } else {
                            textNumber.setText("");
                        }
                        if (snapshot.child("photo").exists()) {
                            String photoUrl = snapshot.child("photo").getValue(String.class);
                            Picasso.get().load(photoUrl).into(userImage);
                        } else {
                            userImage.setImageResource(R.drawable.user_default);
                        }
                        // Mostrar los datos del usuario en las vistas correspondientes
                        textName.setText(name);
                        textEmail.setText(email);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Manejar el error al cargar los datos del usuario
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Error");
                    builder.setMessage(error.getMessage());
                    builder.setPositiveButton("Aceptar", null);
                    builder.create().show();
                }
            });
        }
    }

    private boolean validData = true;
    private boolean isGoogleProvider = false;

    private void toggleEditMode(boolean enabled) {
        if (enabled && validData) {
            texntNameLabel.setVisibility(View.GONE);
            textEmailLabel.setVisibility(View.GONE);
            textNumberLabel.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
            textName.setVisibility(View.GONE);
            textNumber.setVisibility(View.GONE);
            btnChangePhoto.setVisibility(View.VISIBLE);
            layoutEdit.setVisibility(View.VISIBLE);
            etNewName.setText(textName.getText());
            if (textNumber.getText().toString().isEmpty()) {
                etNewPhone.setText("");
            } else {
                etNewPhone.setText(textNumber.getText());
            }
            //solo permitir numeros en el campo de telefono
            etNewPhone.setInputType(InputType.TYPE_CLASS_NUMBER);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                List<? extends UserInfo> userInfoList = currentUser.getProviderData();
                for (UserInfo userInfo : userInfoList) {
                    if (userInfo.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
                        isGoogleProvider = true;
                        break;
                    }
                }
            }
            if (isGoogleProvider) {
                etOldPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                etOldPassword.setText("No se puede modificar al ser de Google");
                etOldPassword.setEnabled(false);
                etOldPassword.setVisibility(View.GONE);
                etNewPassword.setVisibility(View.GONE);
                etNewPassword.setVisibility(View.GONE);
                toggleButton.setVisibility(View.GONE);
            } else {
                etOldPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                etOldPassword.setEnabled(true);
                etNewPassword.setEnabled(true);
                toggleButton.setVisibility(View.VISIBLE);
                etConfirmPassword.setEnabled(true);
                etOldPassword.setText("");
                etNewPassword.setText("");
                etConfirmPassword.setText("");
            }
        } else {
            texntNameLabel.setVisibility(View.VISIBLE);
            textEmailLabel.setVisibility(View.VISIBLE);
            textNumberLabel.setVisibility(View.VISIBLE);
            toggleButton.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            textName.setVisibility(View.VISIBLE);
            textNumber.setVisibility(View.VISIBLE);
            btnChangePhoto.setVisibility(View.GONE);
            layoutEdit.setVisibility(View.GONE);
        }

    }

    private void saveChanges() {
        // Obtener los nuevos datos ingresados por el usuario

        String newName = etNewName.getText().toString().trim();
        String newPhone = etNewPhone.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        // Validar los datos ingresados por el usuario
        if (newName.isEmpty()) {
            etNewName.setError("Ingrese un nombre");
            etNewName.requestFocus();
            validData = false;
            return;
        } else {
            etNewName.setError(null);
            // Actualizar los datos del usuario en la base de datos
            usersRef.child("name").setValue(newName);
            textName.setText(newName);
            validData = true;
        }
        // El teléfono puede estar vacío pero debe seguir el formato de un número de teléfono
        if (!newPhone.isEmpty() && !Patterns.PHONE.matcher(newPhone).matches()) {
            etNewPhone.setError("Ingrese un número de teléfono válido");
            etNewPhone.requestFocus();
            validData = false;
            return;
        } else {
            etNewPhone.setError(null);
            validData = true;
            usersRef.child("number").setValue(newPhone);
            textNumber.setText(newPhone);
        }

        if (!oldPassword.isEmpty() && !isGoogleProvider) {
            // Autenticar al usuario con su correo electrónico y contraseña
            FirebaseAuth.getInstance().signInWithEmailAndPassword(textEmail.getText().toString(), oldPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Si la autenticación es exitosa, verifico que la nueva contraseña sea válida y que coincida con la confirmación
                            if (newPassword.isEmpty()) {
                                etNewPassword.setError("Ingrese una contraseña");
                                etNewPassword.requestFocus();
                                validData = false;
                                return;
                            }
                            if (newPassword.length() < 6) {
                                etNewPassword.setError("La contraseña debe tener al menos 6 caracteres");
                                etNewPassword.requestFocus();
                                validData = false;
                                return;
                            }
                            if (confirmPassword.isEmpty()) {
                                etConfirmPassword.setError("Confirme la contraseña");
                                etConfirmPassword.requestFocus();
                                validData = false;
                                return;
                            }
                            if (!newPassword.equals(confirmPassword)) {
                                etConfirmPassword.setError("Las contraseñas no coinciden");
                                etConfirmPassword.requestFocus();
                                validData = false;
                                return;
                            }
                            // Cambiar la contraseña del usuario
                            currentUser.updatePassword(newPassword)
                                    .addOnCompleteListener(updatePasswordTask -> {
                                        if (updatePasswordTask.isSuccessful()) {
                                            validData = true;
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                            builder.setTitle("Éxito");
                                            builder.setMessage("La contraseña se ha actualizado correctamente");
                                            builder.setPositiveButton("Aceptar", null);
                                            builder.create().show();
                                            // Desactivar el modo de edición
                                            toggleEditMode(false);
                                        } else {
                                            // Manejar el error al cambiar la contraseña
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                            builder.setTitle("Error");
                                            builder.setMessage(updatePasswordTask.getException().getMessage());
                                            builder.setPositiveButton("Aceptar", null);
                                            builder.create().show();
                                            validData = false;

                                        }
                                    });
                        }
                    }).addOnFailureListener(e -> {
                        validData = false;
                        //Pongo un error en el campo de contraseña
                        etOldPassword.setError("Contraseña incorrecta");
                        etOldPassword.requestFocus();
                    });
        } else {
            // Desactivar el modo de edición
            toggleEditMode(false);
        }
    }
}