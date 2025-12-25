package home.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.secloud.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Date;

import home.HomeActivity;

public class ArchivoClick extends Fragment {
    private ImageView imageArchivo;
    private EditText edtNombreArchivo;
    private TextView txtDescripcionArchivo;
    private TextView txtFechaSubida;
    private TextView txtUltimaModificacion;
    private TextView tamanoArchivo;
    private TextView txtAutor;
    private TextView txtCompartido;
    private Button btnEditar;
    private Button btnAceptar;
    private Button btnCancelar;
    private Button btnDescargar;
    private String nombreArchivo;
    private String extensionArchivo;
    private String descripcionArchivo;
    private StorageReference archivoRef;
    private String imagen;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_archivo_click, container, false);
        initView(root);

        // Obtener los datos del archivo seleccionado y establecerlos en las vistas correspondientes
        String ruta = getArguments().getString("ruta");
        imagen = getArguments().getString("imagen");
        Toast.makeText(getContext(), ruta, Toast.LENGTH_SHORT).show();
        archivoRef = FirebaseStorage.getInstance().getReference().child(ruta);
        archivoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                // Obtener los metadatos del archivo
                nombreArchivo = storageMetadata.getCustomMetadata("Name");
                ((HomeActivity) getActivity()).getSupportActionBar().setTitle(nombreArchivo);
                descripcionArchivo = storageMetadata.getCustomMetadata("Description");
                extensionArchivo = storageMetadata.getCustomMetadata("Extension");
                long fechaSubida = storageMetadata.getCreationTimeMillis();
                long ultimaModificacion = storageMetadata.getUpdatedTimeMillis();
                String autor = storageMetadata.getCustomMetadata("Autor");
                boolean compartido = Boolean.parseBoolean(storageMetadata.getCustomMetadata("Compartido"));
                long tamano = storageMetadata.getSizeBytes();
                tamanoArchivo.setText(tamano+" bytes");
                // Establecer los datos en las vistas correspondientes
                edtNombreArchivo.setText(nombreArchivo+"."+extensionArchivo);
                edtNombreArchivo.setEnabled(false);
                txtDescripcionArchivo.setText(descripcionArchivo);
                txtFechaSubida.setText(new Date(fechaSubida).toString().substring(0, 10));
                txtUltimaModificacion.setText(new Date(ultimaModificacion).toString().substring(0, 10));
                txtAutor.setText(autor);
                txtCompartido.setText(compartido ? "Sí" : "No");

                // Mostrar la imagen o el icono correspondiente según la extensión del archivo
                switch (extensionArchivo.toLowerCase()) {
                    case "pdf":
                        // Mostrar una vista previa del pdf
                        break;
                    case "docx":
                        imageArchivo.setImageResource(R.drawable.icon_pdf);
                        break;
                    case "txt":
                        imageArchivo.setImageResource(R.drawable.icon_audio);
                        break;
                    case "jpg":
                    case "png":
                    case "jpeg":
                        Picasso.get().load(imagen).resize(2000, 3000).into(imageArchivo);
                        break;
                    default:
                        imageArchivo.setImageResource(R.drawable.icon_pdf);
                        break;
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Manejar cualquier error al obtener los metadatos del archivo
            }
        });

        return root;
    }

    private void initView(View root) {

        imageArchivo = root.findViewById(R.id.imageArchivo);
        edtNombreArchivo = root.findViewById(R.id.editNombreArchivo);
        txtDescripcionArchivo = root.findViewById(R.id.editDescripcionArchivo);
        txtFechaSubida = root.findViewById(R.id.txtFechaSubida);
        txtUltimaModificacion = root.findViewById(R.id.txtUltimaModificacion);
        txtAutor = root.findViewById(R.id.txtAutor);
        txtCompartido = root.findViewById(R.id.txtCompartido);
        btnEditar = root.findViewById(R.id.btnEditar);
        tamanoArchivo = root.findViewById(R.id.txtPesoArchivo);
        btnAceptar = root.findViewById(R.id.btnAceptar);
        btnCancelar = root.findViewById(R.id.btnCancelar);
        btnDescargar = root.findViewById(R.id.btnDescargar);

        btnEditar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Habilitar la edición de nombre y descripción
                edtNombreArchivo.setText(nombreArchivo);
                edtNombreArchivo.setEnabled(true);
                txtDescripcionArchivo.setEnabled(true);

                // Mostrar los botones Aceptar y Cancelar
                btnAceptar.setVisibility(View.VISIBLE);
                btnCancelar.setVisibility(View.VISIBLE);

                // Ocultar el botón Editar
                btnEditar.setVisibility(View.GONE);
                btnDescargar.setVisibility(View.GONE);
            }
        });

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Guardar los cambios en el nombre y descripción
                String nuevoNombre = edtNombreArchivo.getText().toString().trim();
                String nuevaDescripcion = txtDescripcionArchivo.getText().toString().trim();
                if (nuevoNombre.isEmpty()) {
                    Toast.makeText(getActivity(), "El nombre del archivo no puede estar vacío", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Error");
                    builder.setMessage("El nombre del archivo no puede estar vacío");
                    builder.setPositiveButton("Aceptar", null);
                    builder.create().show();
                    return;
                }

                // Actualizar el nombre y descripción en los metadatos del archivo
                String ruta = getArguments().getString("ruta");
                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                StorageReference archivoRef = storageRef.child(ruta);
                archivoRef.updateMetadata(new StorageMetadata.Builder()
                                .setCustomMetadata("Name", nuevoNombre + "." + extensionArchivo)
                                .setCustomMetadata("Description", nuevaDescripcion)
                                .build())
                        .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                            @Override
                            public void onSuccess(StorageMetadata storageMetadata) {
                                // Los metadatos se han actualizado exitosamente

                                // Deshabilitar la edición de nombre y descripción
                                edtNombreArchivo.setEnabled(false);
                                txtDescripcionArchivo.setEnabled(false);

                                // Ocultar los botones Aceptar y Cancelar
                                btnAceptar.setVisibility(View.GONE);
                                btnCancelar.setVisibility(View.GONE);

                                // Mostrar el botón Editar
                                btnEditar.setVisibility(View.VISIBLE);
                                btnDescargar.setVisibility(View.VISIBLE);
                                // Actualizar el nombre del archivo en el título de la actividad
                                ((HomeActivity) getActivity()).getSupportActionBar().setTitle(nuevoNombre);

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Manejar cualquier error al actualizar los metadatos del archivo
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("Error");
                                builder.setMessage("No se han podido guardar los cambios");
                                builder.setPositiveButton("Aceptar", null);
                                builder.create().show();
                            }
                        });
            }
        });

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Deshabilitar la edición de nombre y descripción
                edtNombreArchivo.setEnabled(false);
                txtDescripcionArchivo.setEnabled(false);

                // Ocultar los botones Aceptar y Cancelar
                btnAceptar.setVisibility(View.GONE);
                btnCancelar.setVisibility(View.GONE);

                // Mostrar el botón Editar
                btnEditar.setVisibility(View.VISIBLE);
            }
        });

        btnDescargar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                descargarArchivo();
            }
        });

        ((HomeActivity) getActivity()).getSupportActionBar().setTitle(nombreArchivo);
    }

    private void descargarArchivo() {
        // Obtener la ruta de la carpeta de descargas del dispositivo
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // Crear un archivo con la ruta de la carpeta de descargas y el nombre del archivo
        File localFile = new File(downloadsDir, nombreArchivo);

        archivoRef.getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Archivo descargado exitosamente

                        // Mostrar un diálogo indicando que se ha descargado correctamente
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Descarga completada");
                        builder.setMessage("El archivo se ha descargado correctamente en la carpeta de descargas de tu dispositivo");
                        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Error");
                        builder.setMessage("Ha ocurrido un error al descargar el archivo");
                        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                });
    }
}