package home.ui.favoritos;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.R;
import com.example.secloud.databinding.FragmentFavoritosBinding;

import adaptadores.AdaptadorExternoFavs;
import adaptadores.AdaptadorExternoHome;
import modelos.Archivo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import dialogs.DialogoRename;


public class FavoritosFragment extends Fragment implements DialogoRename.OnNuevoNombreArchivoListener, AdaptadorExternoFavs.OnArchivoClickListener {

    private FragmentFavoritosBinding binding;

    //Tengo que recoger el id del usuario que esta logueado para poder borrar el archivo
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String uid = mAuth.getCurrentUser().getUid();
    private Archivo archivoRecogido;
    private ArrayList<Archivo> datosFavs = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritosBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(1500);
        final RecyclerView recyclerView = binding.recyclerViewFav;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();
        datosFavs.clear();

        AdaptadorExternoFavs adaptadorExternoFavs = new AdaptadorExternoFavs(datosFavs);
        adaptadorExternoFavs.setOnArchivoClickListener(this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adaptadorExternoFavs);

        actualizarDatosFavoritos();
        ((AdaptadorExternoFavs) binding.recyclerViewFav.getAdapter()).notifyDataSetChanged();
        return root;
    }

    public void actualizarDatosFavoritos() {
        datosFavs.clear();
        ArrayList<Archivo>datos= AdaptadorExternoHome.getInstancia().getDatos();
        for (Archivo fav : datos) {
            if (fav.isFavorito()) {
                datosFavs.add(fav);
                Collections.sort(datosFavs, new Comparator<Archivo>() {
                    @Override
                    public int compare(Archivo archivo1, Archivo archivo2) {
                        return archivo1.getNameMetadata().compareTo(archivo2.getNameMetadata());
                    }
                });
            }
        }
        if (binding != null && binding.recyclerViewFav != null) {
            binding.recyclerViewFav.getAdapter().notifyDataSetChanged();
        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final int EDITAR = 121;
        final int BORRAR = 122;
        switch (item.getItemId()) {
            case EDITAR:
                archivoRecogido = datosFavs.get(item.getGroupId());
                // Crear una instancia del diálogo personalizado
                DialogoRename dialogo = new DialogoRename(getContext(), this);
                dialogo.show();

                return true;
            case BORRAR:
                archivoRecogido = datosFavs.get(item.getGroupId());
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Borrar archivo");
                builder.setMessage("¿Estás seguro de que quieres borrar el archivo " + archivoRecogido.getNameMetadata() + "?");
                builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moverArchivoPapelera(item, datosFavs);
                    }
                });
                builder.setNegativeButton("No", null);
                builder.setCancelable(false);
                builder.create().show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }



    private void moverArchivoPapelera(@NonNull MenuItem item, ArrayList<Archivo> datos) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference archivoRef = storageRef.child("users/" + uid + "/" + archivoRecogido.getUriArchivo());

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
                StorageReference nuevoDirectorioRef = FirebaseStorage.getInstance().getReference().child("recycler_bin/" + uid + "/" + archivoRecogido.getUriArchivo());

                // Obtener metadatos del archivoRecogido original
                archivoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata metadata) {
                        // Establecer los metadatos al nuevo archivoRecogido en la otra ruta
                        StorageMetadata.Builder metadataBuilder = new StorageMetadata.Builder();
                        metadataBuilder.setCustomMetadata("Name", metadata.getCustomMetadata("Name"));
                        metadataBuilder.setCustomMetadata("Extension", metadata.getCustomMetadata("Extension"));
                        metadataBuilder.setCustomMetadata("Descripcion", metadata.getCustomMetadata("Descripcion"));
                        metadataBuilder.setCustomMetadata("Autor", metadata.getCustomMetadata("Autor"));

                        // Subir el archivoRecogido desde la ruta local al nuevo directorio en Firebase Storage con los metadatos
                        UploadTask uploadTask = nuevoDirectorioRef.putFile(Uri.fromFile(localFile), metadataBuilder.build());
                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Borrar el archivoRecogido original
                                archivoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // El archivoRecogido original se ha borrado correctamente
                                        Toast.makeText(getContext(), "Archivo movido a la papelera", Toast.LENGTH_SHORT).show();

                                        // Actualizar la lista de datos y notificar al adaptador
                                        datos.remove(item.getGroupId());
                                        ((AdaptadorExternoFavs) binding.recyclerViewFav.getAdapter()).notifyDataSetChanged();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Manejar el error si no se puede borrar el archivoRecogido original
                                        crearDialog("Error", "No se ha podido borrar el archivo", "Aceptar");
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Manejar el error si no se puede subir el archivoRecogido al nuevo directorio
                                crearDialog("Error", "No se ha podido eliminar el archivo", "Aceptar");
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Manejar el error si no se pueden obtener los metadatos del archivoRecogido original
                        crearDialog("Error", "No se han podido obtener los metadatos del archivoRecogido original", "Aceptar");
                    }
                });
            }
        });
    }
    @Override
    public void onArchivoClick(int position) {
        //Abro el archivo
        abrirArchivo(position);
    }

    private void abrirArchivo(int position) {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_home);
        Bundle bundle = new Bundle();
        bundle.putString("ruta", "users/"+uid+"/"+datosFavs.get(position).getUriArchivo());
        bundle.putString("imagen", datosFavs.get(position).getImagen());
        navController.navigate(R.id.nav_archivo_click, bundle);
    }
    @Override
    public void onNuevoNombreArchivo(String nuevoNombre) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String extension = archivoRecogido.getExtension();
        // Crea un objeto StorageMetadata con el nuevo nombre
        if (nuevoNombre.contains(" ")) {
            nuevoNombre = nuevoNombre.trim();
        }
        if (nuevoNombre.contains(".")) {
            Toast.makeText(getContext(), "El nombre no puede contener puntos", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("Name", nuevoNombre)
                .build();

        // Actualiza los metadatos del archivo para cambiar su nombre
        String finalNuevoNombre = nuevoNombre;
        storageRef.child("users/" + uid + "/" + archivoRecogido.getUriArchivo())
                .updateMetadata(metadata)
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        // El nombre del archivo ha sido actualizado con éxito
                        Toast.makeText(getContext(), "Archivo renombrado", Toast.LENGTH_SHORT).show();

                        // Actualiza el objeto Archivo en la lista de datos
                        archivoRecogido.setNameMetadata(finalNuevoNombre);
                        ((AdaptadorExternoFavs) binding.recyclerViewFav.getAdapter()).notifyDataSetChanged();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        crearDialog("Error", "No se ha podido renombrar el archivo", "Aceptar");
                    }
                });
    }
    private void crearDialog(String title, String message, String positiveButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton(positiveButton,null);
        builder.create().show();
    }
}
