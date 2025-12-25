package home.ui.help;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.secloud.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HelpFragment extends Fragment {


    private EditText emailEditText;
    private TextInputEditText incidenciaEditText; // Actualización aquí
    private Button sendButton;

    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_help, container, false);
        emailEditText = root.findViewById(R.id.emailEditText);
        incidenciaEditText = root.findViewById(R.id.incidenciaEditText); // Actualización aquí
        sendButton = root.findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmailToSupport();
            }
        });

        // Inicializar la instancia de Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Obtener el correo electrónico del usuario desde la base de datos (reemplaza "users" y "email" con tu estructura de datos real)
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = databaseReference.child("users").child(uid);
        userRef.child("email").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    String userEmail = task.getResult().getValue(String.class);
                    emailEditText.setText(userEmail);
                } else {
                    //Le permito al usuario ingresar su correo electrónico manualmente permitiendo que edite el campo
                    emailEditText.setEnabled(true);
                }
            }
        });

        return root;
    }

    private void sendEmailToSupport() {
        String email = emailEditText.getText().toString().trim();
        String incidencia = incidenciaEditText.getText().toString().trim();

        // Validar que se haya ingresado un correo y una incidencia
        if (email.isEmpty() || incidencia.isEmpty()) {
            // Mostrar un mensaje de error al usuario indicando que debe llenar ambos campos
            incidenciaEditText.setError("Debe ingresar una incidencia");
            return;
        }

        // Enviar el mensaje al soporte de la aplicación utilizando Firebase (reemplaza "support" con tu nodo de soporte real)
        DatabaseReference supportRef = databaseReference.child("support").push();
        supportRef.child("email").setValue(email);
        supportRef.child("incidencia").setValue(incidencia).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Mostrar un diálogo de éxito indicando que el mensaje se envió correctamente
                    clearFields();
                    showSuccessDialog();
                } else {
                    // Mostrar un diálogo de error indicando que no se pudo enviar el mensaje
                    showErrorDialog();
                }
            }
        });
    }

    private void clearFields() {
        incidenciaEditText.setText("");
    }

    private void showSuccessDialog() {
        // Mostrar un diálogo de éxito utilizando AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Mensaje enviado")
                .setMessage("Tu mensaje ha sido enviado correctamente.")
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void showErrorDialog() {
        // Mostrar un diálogo de error utilizando AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Error")
                .setMessage("No se pudo enviar el mensaje. Por favor, intenta nuevamente.")
                .setPositiveButton("Aceptar", null)
                .show();
    }
}
