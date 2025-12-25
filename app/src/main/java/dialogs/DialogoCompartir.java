package dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.secloud.R;
public class DialogoCompartir extends Dialog {
    private EditText etEmail;
    private Button btnCancelar, btnCompartir;
    private OnCompartirArchivoListener mListener;

    public DialogoCompartir(Context context, OnCompartirArchivoListener listener) {
        super(context);
        mListener = listener;
        setTitle("Compartir archivo");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_shared);

        etEmail = findViewById(R.id.et_email);
        btnCancelar = findViewById(R.id.btn_cancel);
        btnCompartir = findViewById(R.id.btn_share);

        // Configurar el botón Cancelar
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // Cerrar el diálogo
            }
        });

        // Configurar el botón Compartir
        btnCompartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener el correo electrónico introducido por el usuario
                String email = etEmail.getText().toString();
                mListener.onCompartirArchivo(email);
                dismiss(); // Cerrar el diálogo
            }
        });
    }

    public interface OnCompartirArchivoListener {
        void onCompartirArchivo(String email);
    }
}