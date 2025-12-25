package home.ui.bin;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.databinding.FragmentBinBinding;
import adaptadores.AdaptadorExternoBin;
import adaptadores.AdaptadorExternoHome;
import modelos.Archivo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class PapeleraFragment extends Fragment implements AdaptadorExternoBin.OnArchivoClickListener {

    private FragmentBinBinding binding;

    //Tengo que recoger el id del usuario que esta logueado para poder borrar el archivo
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String uid = mAuth.getCurrentUser().getUid();
    private Archivo archivoRecogido;
    private ArrayList<Archivo> datos = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentBinBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        root.setAlpha(0f); // Establecer la opacidad de la vista raíz como 0 (invisible)
        root.animate().alpha(1f).setDuration(1500); // Animar la opacidad de la vista raíz a 1 (visible) en 1000 milisegundos
        datos.clear();
        final RecyclerView recyclerView = binding.recyclerView;
        //Elimino el boton flotante uploadFiles
        AdaptadorExternoBin adaptadorExternoBin = new AdaptadorExternoBin(datos);
        adaptadorExternoBin.setOnArchivoClickListener(this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adaptadorExternoBin);

        cargarDatosPapelera();
        return root;
    }

    private void cargarDatosPapelera() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("recycler_bin/" + uid + "/");

        Task<ListResult> listResultTask = storageRef.listAll();
        listResultTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<StorageReference> items = task.getResult().getItems();
                for (StorageReference item : items) {
                    item.getMetadata().addOnSuccessListener(storageMetadata -> {
                        String nombre = storageMetadata.getCustomMetadata("Name");
                        String extension = storageMetadata.getCustomMetadata("Extension");
                        String autor = storageMetadata.getCustomMetadata("Autor");
                        String descripcion = storageMetadata.getCustomMetadata("Descripcion");
                        boolean favorito = Boolean.parseBoolean(storageMetadata.getCustomMetadata("Favorito"));
                        Archivo archivo = new Archivo(storageMetadata.getName(), nombre, extension, favorito,false,false);
                        archivo.setDescripcion(descripcion);
                        archivo.setAutor(autor);
                        datos.add(archivo);
                        ((AdaptadorExternoBin) binding.recyclerView.getAdapter()).notifyDataSetChanged();
                        StorageReference storageRef1 = FirebaseStorage.getInstance().getReference();
                        String path = storageMetadata.getPath();
                        storageRef1.child(path).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                archivo.setImagen(uri.toString());
                                ((AdaptadorExternoBin) binding.recyclerView.getAdapter()).notifyDataSetChanged();
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

    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final int BORRAR = 122;
        FirebaseStorage storage = FirebaseStorage.getInstance();
        ArrayList<Archivo> datos = ((AdaptadorExternoBin) binding.recyclerView.getAdapter()).getDatos();
        switch (item.getItemId()) {
            case BORRAR:
                //Elimino el archivo de la papelera
                archivoRecogido = datos.get(item.getGroupId());
                StorageReference storageRef = storage.getReference();
                StorageReference desertRef = storageRef.child("recycler_bin/" + uid + "/" + archivoRecogido.getUriArchivo());
                desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "Archivo eliminado", Toast.LENGTH_SHORT).show();
                        datos.remove(item.getGroupId());
                        ((AdaptadorExternoBin) binding.recyclerView.getAdapter()).notifyDataSetChanged();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(getContext(), "Error al eliminar el archivo", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public void onArchivoClick(int position) {
        //Le muestro un dialog para ver si lo quiere restaurar
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Restaurar archivo");
        builder.setMessage("¿Desea restaurar el archivo?");
        builder.setPositiveButton("Restaurar", (dialog, which) -> {
            Archivo b = datos.get(position);
            // Obtener referencia al archivo original en Firebase Storage
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference archivoRef = storageRef.child("recycler_bin/" + uid + "/" + b.getUriArchivo());

            // Obtener una referencia al directorio local donde se descargará el archivo
            File localDirectory = new File(getContext().getFilesDir(), "archivos_descargados");
            if (!localDirectory.exists()) {
                localDirectory.mkdirs();
            }

            // Obtener una referencia al archivo local donde se guardará el archivo descargado
            File localFile = new File(localDirectory, b.getUriArchivo());

            // Descargar el archivo a la ruta local
            archivoRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // El archivo se ha descargado correctamente a la ruta local

                    // Obtener referencia al nuevo directorio en Firebase Storage
                    StorageReference nuevoDirectorioRef = FirebaseStorage.getInstance().getReference().child("users/" + uid + "/" + b.getUriArchivo());

                    // Obtener metadatos del archivo original
                    archivoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                        @Override
                        public void onSuccess(StorageMetadata metadata) {
                            // Establecer los metadatos al nuevo archivo en la otra ruta
                            StorageMetadata.Builder metadataBuilder = new StorageMetadata.Builder();
                            metadataBuilder.setCustomMetadata("Name", metadata.getCustomMetadata("Name"));
                            metadataBuilder.setCustomMetadata("Favorito", metadata.getCustomMetadata("Favorito"));
                            metadataBuilder.setCustomMetadata("Extension", metadata.getCustomMetadata("Extension"));
                            metadataBuilder.setCustomMetadata("Extension", metadata.getCustomMetadata("Extension"));
                            metadataBuilder.setCustomMetadata("Autor", metadata.getCustomMetadata("Autor"));
                            Archivo archivo = new Archivo(datos.get(position).getUriArchivo(), datos.get(position).getNameMetadata(),  datos.get(position).getExtension()
                                    , datos.get(position).isFavorito(),   false,  datos.get(position).isCarpeta());
                            archivo.setImagen(datos.get(position).getImagen());
                            // Subir el archivo desde la ruta local al nuevo directorio en Firebase Storage con los metadatos
                            UploadTask uploadTask = nuevoDirectorioRef.putFile(Uri.fromFile(localFile), metadataBuilder.build());
                            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    archivoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // El archivo original se ha borrado correctamente
                                            Toast.makeText(getContext(), "Archivo movido al nuevo directorio", Toast.LENGTH_SHORT).show();
                                            AdaptadorExternoHome adaptadorExternoHome = AdaptadorExternoHome.getInstancia();
                                            adaptadorExternoHome.agregarArchivo(archivo);
                                            adaptadorExternoHome.notifyDataSetChanged();
                                            // Actualizar la lista de datos y notificar al adaptador
                                            datos.remove(datos.get(position));
                                            ((AdaptadorExternoBin) binding.recyclerView.getAdapter()).notifyDataSetChanged();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Manejar el error si no se puede borrar el archivo original
                                            Toast.makeText(getContext(), "Error al borrar el archivo", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Manejar el error si no se puede subir el archivo al nuevo directorio
                                    Toast.makeText(getContext(), "Error al mover el archivo al nuevo directorio", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Manejar el error si no se pueden obtener los metadatos del archivo original
                            Toast.makeText(getContext(), "Error al obtener los metadatos del archivo original", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            //Cierro el dialog
            dialog.dismiss();
        });
        builder.create().show();
    }


}
