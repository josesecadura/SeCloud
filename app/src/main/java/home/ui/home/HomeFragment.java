package home.ui.home;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.R;
import com.example.secloud.databinding.FragmentHomeBinding;

import adaptadores.AdaptadorExternoHome;

import dialogs.DialogoCompartir;
import dialogs.DialogoRename;
import modelos.Archivo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.Collections;
import java.util.Comparator;


public class HomeFragment extends Fragment implements DialogoRename.OnNuevoNombreArchivoListener, AdaptadorExternoHome.OnArchivoClickListener {

    private FragmentHomeBinding binding;
    //Tengo que recoger el id del usuario que esta logueado para poder borrar el archivo
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String uid = mAuth.getCurrentUser().getUid();
    private Archivo archivoRecogido;
    private static ArrayList<Archivo> datos = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        root.setAlpha(0f); // Establecer la opacidad de la vista raíz como 0 (invisible)
        root.animate().alpha(1f).setDuration(1500); // Animar la opacidad de la vista raíz a 1 (visible) en 1000 milisegundos
        datos.clear();
        final RecyclerView recyclerView = binding.recyclerView;

        AdaptadorExternoHome adaptadorExternoHome = AdaptadorExternoHome.getInstancia(datos);
        adaptadorExternoHome.setOnArchivoClickListener(this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        registerForContextMenu(recyclerView);
        recyclerView.setAdapter(adaptadorExternoHome);

        cargarDatos();

        return root;
    }

    public void cargarDatos() {
        datos.clear();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference().child("users/" + uid + "/");

        listRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for (StorageReference prefix : listResult.getPrefixes()) {
                    // All the prefixes under listRef.
                    // You may call listAll() recursively on them.
                    //Aqui obtendre las carpetas que tenga el usuario dentro de la carpeta files
                    String name = prefix.getName();
                    Archivo archivo = new Archivo(name, name, "", false, false, true);
                    datos.add(archivo);
                    ((AdaptadorExternoHome) binding.recyclerView.getAdapter()).notifyDataSetChanged();
                }

                for (StorageReference item : listResult.getItems()) {
                    // All the items under listRef.
                    item.getMetadata().addOnSuccessListener(storageMetadata -> {
                        String nombre = storageMetadata.getCustomMetadata("Name");
                        String extension = storageMetadata.getCustomMetadata("Extension");
                        boolean favorito = Boolean.parseBoolean(storageMetadata.getCustomMetadata("Favorito"));
                        Archivo archivo = new Archivo(storageMetadata.getName(), nombre, extension, favorito, false, false);
                        datos.add(archivo);
                        Collections.sort(datos, new Comparator<Archivo>() {
                            @Override
                            public int compare(Archivo archivo1, Archivo archivo2) {
                                AdaptadorExternoHome.getInstancia().notifyDataSetChanged();
                                return archivo1.getNameMetadata().compareTo(archivo2.getNameMetadata());
                            }
                        });
                        ((AdaptadorExternoHome) binding.recyclerView.getAdapter()).notifyDataSetChanged();
                        StorageReference storageRef1 = FirebaseStorage.getInstance().getReference();
                        String path = storageMetadata.getPath();
                        storageRef1.child(path).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                //Tengo que controlar si la uri es de una imagen o de un archivo normal
                                if(extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg") || extension.equals("gif") || extension.equals("bmp") || extension.equals("webp") || extension.equals("psd") || extension.equals("svg") || extension.equals("tiff")) {
                                    archivo.setImagen(uri.toString());
                                    ((AdaptadorExternoHome) binding.recyclerView.getAdapter()).notifyDataSetChanged();

                                }
                            }
                        });
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Uh-oh, an error occurred!
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final int EDITAR = 121;
        final int BORRAR = 122;
        final int COMPARTIR = 123;
        ArrayList<Archivo> datos = ((AdaptadorExternoHome) binding.recyclerView.getAdapter()).getDatos();
        switch (item.getItemId()) {
            case EDITAR:

                archivoRecogido = datos.get(item.getGroupId());
                // Crear una instancia del diálogo personalizado
                DialogoRename dialogo = new DialogoRename(getContext(), this);
                dialogo.show();

                return true;
            case BORRAR:

                archivoRecogido = datos.get(item.getGroupId());
                // Obtener referencia al archivoRecogido original en Firebase Storage
                if (!archivoRecogido.isCarpeta()) {
                    moverArchivoPapelera(item, datos);
                } else {
                    eliminarCarpeta(item, datos);
                }

                return true;
            case COMPARTIR:
                archivoRecogido = datos.get(item.getGroupId());
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
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot usuarioSnapshot : dataSnapshot.getChildren()) {
                                        String uidUsuarioDestino = usuarioSnapshot.getKey();
                                        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                                        StorageReference archivoRef = storageRef.child("users/" + uid + "/" + archivoRecogido.getUriArchivo());
                                        archivoRef.updateMetadata(new StorageMetadata.Builder().setCustomMetadata("Compartido", "true").setCustomMetadata("UidUsuarioDestino", uidUsuarioDestino).build()).addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                            @Override
                                            public void onSuccess(StorageMetadata storageMetadata) {
                                                Toast.makeText(getContext(), "Archivo compartido con éxito", Toast.LENGTH_SHORT).show();
                                            }
                                        });
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
                                                        metadataBuilder.setCustomMetadata("Descripcion", metadata.getCustomMetadata("Descripcion"));
                                                        // Subir el archivoRecogido desde la ruta local al nuevo directorio en Firebase Storage con los metadatos
                                                        UploadTask uploadTask = nuevoDirectorioRef.putFile(Uri.fromFile(localFile), metadataBuilder.build());
                                                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                            @Override
                                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                                // El archivoRecogido se ha compartido correctamente
                                                                // Puedes agregar aquí cualquier lógica adicional después de compartir el archivo
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                // Manejar el error si no se puede subir el archivoRecogido al nuevo directorio
                                                                Toast.makeText(getContext(), "Error al compartir el archivo", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Manejar el error si no se pueden obtener los metadatos del archivoRecogido original
                                                        Toast.makeText(getContext(), "Error al obtener los metadatos del archivoRecogido original", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                } else {
                                    // El correo electrónico no existe en la base de datos
                                    Toast.makeText(getContext(), "El correo electrónico no existe", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Error al consultar la base de datos
                                Toast.makeText(getContext(), "Error al consultar la base de datos", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                dialogoCompartir.show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    private void eliminarCarpeta(@NonNull MenuItem itemM, ArrayList<Archivo> datos) {
        //Avisar de que se eliminaran todos los datos y no se podran recuperar
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Eliminar carpeta");
        builder.setMessage("¿Estás seguro de que quieres eliminar la carpeta? Se eliminarán todos los archivos que contenga y no se podrán recuperar.");
        //Añado un boton si dice que si se elimina la carpeta y sus datos
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                StorageReference archivoRef = storageRef.child("users/" + uid + "/" + archivoRecogido.getUriArchivo() + "/");
                archivoRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            item.delete();
                        }
                        datos.remove(itemM.getGroupId());
                        AdaptadorExternoHome.getInstancia().notifyDataSetChanged();


                    }
                });
            }
        });
        //Añado un boton si dice que no no se elimina la carpeta
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
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
                        metadataBuilder.setCustomMetadata("Description", metadata.getCustomMetadata("Description"));
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
                                        Toast.makeText(getContext(), "Archivo movido al nuevo directorio", Toast.LENGTH_SHORT).show();

                                        // Actualizar la lista de datos y notificar al adaptador
                                        datos.remove(item.getGroupId());
                                        //Notifico la posicion del archivoRecogido que se ha movido
                                        ((AdaptadorExternoHome) binding.recyclerView.getAdapter()).notifyItemRemoved(item.getGroupId());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Manejar el error si no se puede borrar el archivoRecogido original
                                        Toast.makeText(getContext(), "Error al borrar el archivoRecogido", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Manejar el error si no se puede subir el archivoRecogido al nuevo directorio
                                Toast.makeText(getContext(), "Error al mover el archivoRecogido al nuevo directorio", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Manejar el error si no se pueden obtener los metadatos del archivoRecogido original
                        Toast.makeText(getContext(), "Error al obtener los metadatos del archivoRecogido original", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onArchivoClick(int position) {
        Toast.makeText(getContext(), "Archivo seleccionado", Toast.LENGTH_SHORT).show();
        if (datos.get(position).isCarpeta()) {
            //Si es una carpeta se abre la carpeta
            abrirCarpeta(position);
            Toast.makeText(getContext(), "Carpeta abierta", Toast.LENGTH_SHORT).show();
        } else {
            //Abro el archivo
            abrirArchivo(position);
        }
    }

    private void abrirArchivo(int position) {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_home);
        Bundle bundle = new Bundle();
        bundle.putString("ruta", "users/"+uid+"/"+datos.get(position).getUriArchivo());
        bundle.putString("imagen", datos.get(position).getImagen());
        navController.navigate(R.id.nav_archivo_click, bundle);
    }

    private void abrirCarpeta(int position) {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_home);
        Bundle bundle = new Bundle();
        bundle.putString("nombreCarpeta", datos.get(position).getNameMetadata());
        bundle.putString("imagen", datos.get(position).getImagen());
        navController.navigate(R.id.nav_carpeta, bundle);
    }

    @Override
    public void onNuevoNombreArchivo(String nuevoNombre) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String extension = archivoRecogido.getExtension();
        // Crea un objeto StorageMetadata con el nuevo nombre
        if (nuevoNombre.contains(" ")) {
            Toast.makeText(getContext(), "El nombre no puede contener espacios", Toast.LENGTH_SHORT).show();
            nuevoNombre = nuevoNombre.replace(" ", "_");
        }
        if (nuevoNombre.contains(".")) {
            Toast.makeText(getContext(), "El nombre no puede contener puntos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!archivoRecogido.isCarpeta()) {
            StorageMetadata metadata = new StorageMetadata.Builder().setCustomMetadata("Name", nuevoNombre).build();

            // Actualiza los metadatos del archivo para cambiar su nombre
            String finalNuevoNombre = nuevoNombre;
            storageRef.child("users/" + uid + "/" + archivoRecogido.getUriArchivo()).updateMetadata(metadata).addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    // El nombre del archivo ha sido actualizado con éxito
                    Toast.makeText(getContext(), "Archivo renombrado", Toast.LENGTH_SHORT).show();

                    // Actualiza el objeto Archivo en la lista de datos
                    archivoRecogido.setNameMetadata(finalNuevoNombre);
                    AdaptadorExternoHome.getInstancia().notifyDataSetChanged();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), "Error al renombrar el archivo", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            //Lo que tengo que cambiar es el nombre de la subruta que es una carpeta
            StorageReference storageReference = storage.getReference();
            StorageReference archivoRef = storageReference.child("users/" + uid + "/" + archivoRecogido.getUriArchivo() + "/");


        }
    }

}
