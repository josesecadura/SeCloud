package home.ui.compartir;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.R;
import com.example.secloud.databinding.FragmentSharedBinding;

import adaptadores.AdaptadorExternoShared;
import dialogs.DialogoCompartir;
import modelos.Archivo;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CompartirFragment extends Fragment implements AdaptadorExternoShared.OnArchivoClickListener {

    private FragmentSharedBinding binding;

    //Tengo que recoger el id del usuario que esta logueado para poder borrar el archivo
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String uid = mAuth.getCurrentUser().getUid();
    private Archivo archivoRecogido;
    private ArrayList<Archivo> datos = new ArrayList<>();
    private AdaptadorExternoShared adaptadorExternoShared;

    @SuppressLint("RestrictedApi")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSharedBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        root.setAlpha(0f); // Establecer la opacidad de la vista raíz como 0 (invisible)
        root.animate().alpha(1f).setDuration(1500); // Animar la opacidad de la vista raíz a 1 (visible) en 1000 milisegundos
        datos.clear();
        final RecyclerView recyclerView = binding.recyclerViewShared;
        //Elimino el boton flotante uploadFiles
        adaptadorExternoShared = new AdaptadorExternoShared(datos);
        adaptadorExternoShared.setOnArchivoClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adaptadorExternoShared);

        cargarDatosPapelera();
        return root;
    }

    private void cargarDatosPapelera() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("compartidos/users/" + uid + "/");

        Task<ListResult> listResultTask = storageRef.listAll();
        listResultTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<StorageReference> items = task.getResult().getItems();
                for (StorageReference item : items) {
                    item.getMetadata().addOnSuccessListener(storageMetadata -> {
                        String nombre = storageMetadata.getCustomMetadata("Name");
                        String extension = storageMetadata.getCustomMetadata("Extension");
                        String autor = storageMetadata.getCustomMetadata("Autor");
                        String descripcion = storageMetadata.getCustomMetadata("Description");
                        Archivo archivo = new Archivo(storageMetadata.getName(), nombre, extension, false, false, false);
                        archivo.setAutor(autor);
                        archivo.setDescripcion(descripcion);
                        datos.add(archivo);
                        adaptadorExternoShared.notifyDataSetChanged();
                        StorageReference storageRef1 = FirebaseStorage.getInstance().getReference();
                        String path = storageMetadata.getPath();
                        storageRef1.child(path).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                archivo.setImagen(uri.toString());
                                adaptadorExternoShared.notifyDataSetChanged();
                            }
                        });
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onArchivoClick(int position) {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_home);
        Bundle bundle = new Bundle();
        bundle.putString("ruta", "compartidos/users/" + uid + "/" + datos.get(position).getUriArchivo());
        bundle.putString("imagen", datos.get(position).getImagen());
        navController.navigate(R.id.nav_archivo_click, bundle);
    }

    @Override
    public void onBorrarClick(Archivo archivo) {
        //Elimino el archivo de la base de datos primero muestro un dialog de que se quitara de sus compartidos
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Eliminar archivo");
        builder.setMessage("¿Estas seguro de que quieres eliminar el archivo de tus compartidos?");
        builder.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Elimino el archivo de la base de datos de compartidos
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference desertRef = storageRef.child("compartidos/users/" + uid + "/" + archivo.getUriArchivo());
                desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "Archivo eliminado de tus compartidos", Toast.LENGTH_SHORT).show();
                        datos.remove(archivo);
                        ((AdaptadorExternoShared) binding.recyclerViewShared.getAdapter()).notifyDataSetChanged();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Error al eliminar el archivo");
                        builder.setMessage("El archivo no se ha podido eliminar de tus compartidos");
                        builder.create().show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }

    @Override
    public void onCompartirClick(int position) {
        archivoRecogido = datos.get(position);
        // Obtener el correo electrónico introducido por el usuario
        final String[] correoElectronico = new String[1];

        // Crear un diálogo con un campo de entrada para solicitar el correo al usuario
        DialogoCompartir dialogoCompartir = new DialogoCompartir(getContext(), new DialogoCompartir.OnCompartirArchivoListener() {
            @Override
            public void onCompartirArchivo(String email) {
                correoElectronico[0] = email;

                // Consultar la base de datos para verificar si el correo electrónico existe
                DatabaseReference usuariosRef = FirebaseDatabase.getInstance().getReference("users");
                Query query = usuariosRef.orderByChild("email").equalTo(correoElectronico[0]);

                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && !correoElectronico[0].equals(mAuth.getCurrentUser().getEmail())) {
                            for (DataSnapshot usuarioSnapshot : dataSnapshot.getChildren()) {
                                String uidUsuarioDestino = usuarioSnapshot.getKey();
                                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                                StorageReference archivoRef = storageRef.child("compartidos/users/" + uid + "/" + archivoRecogido.getUriArchivo());
                                archivoRef.updateMetadata(new StorageMetadata.Builder().setCustomMetadata("Compartido", "true").setCustomMetadata("UidUsuarioDestino", uidUsuarioDestino).build());
                                // Obtener una referencia al directorio local donde se descargará el archivoRecogido
                                File localDirectory = new File(getContext().getFilesDir(), "archivos_descargados");
                                if (!localDirectory.exists()) {
                                    localDirectory.mkdirs();
                                }

                                // Obtener una referencia al archivoRecogido local donde se guardará el archivoRecogido descargado
                                File localFile = new File(localDirectory, archivoRecogido.getUriArchivo());

                                // Descargar el archivoRecogido a la ruta local
                                archivoRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        // El archivoRecogido se ha descargado correctamente a la ruta local

                                        // Obtener referencia al nuevo directorio en Firebase Storage
                                        StorageReference nuevoDirectorioRef = FirebaseStorage.getInstance().getReference().child("compartidos/users/" + uidUsuarioDestino + "/" + archivoRecogido.getUriArchivo());

                                        // Obtener metadatos del archivoRecogido original
                                        archivoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                            @Override
                                            public void onSuccess(StorageMetadata metadata) {
                                                // Establecer los metadatos al nuevo archivoRecogido en la otra ruta
                                                StorageMetadata.Builder metadataBuilder = new StorageMetadata.Builder();
                                                metadataBuilder.setCustomMetadata("Name", metadata.getCustomMetadata("Name"));
                                                metadataBuilder.setCustomMetadata("Extension", metadata.getCustomMetadata("Extension"));
                                                metadataBuilder.setCustomMetadata("Favorito", metadata.getCustomMetadata("Favorito"));
                                                metadataBuilder.setCustomMetadata("Autor", metadata.getCustomMetadata("Autor"));
                                                metadataBuilder.setCustomMetadata("Compartido", metadata.getCustomMetadata("Compartido"));
                                                metadataBuilder.setCustomMetadata("Description", metadata.getCustomMetadata("Description"));
                                                // Subir el archivoRecogido desde la ruta local al nuevo directorio en Firebase Storage con los metadatos
                                                UploadTask uploadTask = nuevoDirectorioRef.putFile(Uri.fromFile(localFile), metadataBuilder.build());
                                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                        crearDialog("Archivo compartido", "El archivo se ha compartido con éxito a " + correoElectronico[0], "Aceptar");
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Manejar el error si no se puede subir el archivoRecogido al nuevo directorio
                                                        crearDialog("Error al compartir el archivo", "No se ha podido compartir el archivo", "Aceptar");
                                                    }
                                                });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                crearDialog("Error al compartir el archivo", "No se ha podido compartir el archivo", "Aceptar");
                                            }
                                        });
                                    }
                                });
                            }
                        } else {
                            if (email.equalsIgnoreCase(mAuth.getCurrentUser().getEmail())) {
                                crearDialog("Error al compartir el archivo", "No puedes compartir un archivo contigo mismo", "Aceptar");
                            } else {
                                // El correo electrónico no existe en la base de datos
                                crearDialog("Correo electrónico no encontrado", "El correo electrónico introducido no existe en la base de datos", "Aceptar");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Error al consultar la base de datos
                        crearDialog("Error al consultar la base de datos", "No se ha podido consultar la base de datos", "Aceptar");
                    }
                });
            }
        });
        dialogoCompartir.show();
    }

    private void crearDialog(String titulo, String mensaje, String boton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(titulo);
        builder.setMessage(mensaje);
        builder.setCancelable(false); // Evitar que se cancele pulsando fuera del dialogo (por defecto true
        builder.setPositiveButton(boton, null);
        builder.create().show();
    }
}