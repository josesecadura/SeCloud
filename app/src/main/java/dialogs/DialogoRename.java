package dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.secloud.R;

public class DialogoRename extends Dialog {
    private EditText etNuevoNombre;
    private Button btnCancelar, btnRenombrar;
    private OnNuevoNombreArchivoListener mListener;

    public DialogoRename(Context context, OnNuevoNombreArchivoListener listener) {
        super(context);
        mListener = listener;
        setTitle("Renombrar archivo");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_rename);

        etNuevoNombre = findViewById(R.id.et_new_name);
        btnCancelar = findViewById(R.id.btn_cancel);
        btnRenombrar = findViewById(R.id.btn_rename);

        // Configurar el botón Cancelar
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // Cerrar el diálogo
            }
        });

        // Configurar el botón Renombrar
        btnRenombrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Aquí se puede hacer algo con el nuevo nombre del archivo
                String nuevoNombre = etNuevoNombre.getText().toString();
                mListener.onNuevoNombreArchivo(nuevoNombre);
                dismiss(); // Cerrar el diálogo
            }
        });
    }

    public interface OnNuevoNombreArchivoListener {
        void onNuevoNombreArchivo(String nuevoNombre);
    }
}