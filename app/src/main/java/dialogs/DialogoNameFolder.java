package dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.secloud.R;

    public class DialogoNameFolder extends Dialog {
        private EditText etNuevoNombre;
        private Button btnCancelar, btnRenombrar;
        private OnNombreCarpeta mListener;

        public DialogoNameFolder(Context context, OnNombreCarpeta listener) {
            super(context);
            mListener = listener;
            setTitle("Renombrar archivo");
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_name_folder);

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
                    mListener.onNameCarpeta(nuevoNombre);
                    dismiss(); // Cerrar el diálogo
                }
            });
        }

        public interface OnNombreCarpeta {
            void onNameCarpeta(String nuevoNombre);
        }
    }