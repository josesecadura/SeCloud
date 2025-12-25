package home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import com.example.secloud.R;
import adaptadores.AdaptadorExternoHome;
import modelos.Archivo;
import com.example.secloud.users.LoginActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.secloud.databinding.ActivityHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import dialogs.DialogoNameFolder;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DialogoNameFolder.OnNombreCarpeta {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHomeBinding binding;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String uid = "";

    private NavController navController;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private FirebaseUser currentUser = mAuth.getCurrentUser();
    private Handler handler;
    private Runnable signOutRunnable;
    private String autor;
    private DrawerLayout drawer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = findViewById(android.R.id.content);
        rootView.setAlpha(0f); // Establecer la opacidad de la vista raíz como 0 (invisible)
        rootView.animate().alpha(1f).setDuration(1500); // Animar la opacidad de la vista raíz a 1 (visible) en 1000 milisegundos
        uid = getIntent().getStringExtra("uid");

        storage = FirebaseStorage.getInstance();
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarHome.toolbar);
        binding.appBarHome.uploadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFloatingMenu(view);
            }
        });
        drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_favoritos, R.id.nav_papelera,R.id.nav_shared,R.id.nav_profile).setOpenableLayout(drawer).build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        cargarDatos();

        handler = new Handler();
        signOutRunnable = new Runnable() {
            @Override
            public void run() {
                signOutUser();
            }
        };
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void showFloatingMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
        popupMenu.inflate(R.menu.floating_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_upload_file) {
                    // Lógica para subir un archivo aquí
                    Intent intent = new Intent();
                    intent.setType("*/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo"), 1);
                    return true;
                } else if (itemId == R.id.menu_create_folder) {
                    Toast.makeText(getApplicationContext(), "Crear carpeta", Toast.LENGTH_SHORT).show();
                    DialogoNameFolder dialogoNameFolder = new DialogoNameFolder(HomeActivity.this, HomeActivity.this);
                    dialogoNameFolder.show();
                    return true;
                } else if (itemId == R.id.menu_upload_photo) {
                    //Hago un intent para abrir la galeria del usuario en vez de los archivos
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(
                            Intent.createChooser(intent, "Seleccione una imagen"),
                            2);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSignOutTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSignOutTimer();
    }

    private void startSignOutTimer() {
        handler.postDelayed(signOutRunnable, 600000); // Caducidad del acceso después de 10 minutos (600000 milisegundos)

    }

    private void stopSignOutTimer() {
        handler.removeCallbacks(signOutRunnable);
    }

    private void signOutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Tu sesión ha caducado", Toast.LENGTH_SHORT).show();
        //Cierra la actividad actual
        finish();
    }

    private void cargarDatos() {
        //Los datos los tenfo que sacar de Realtime Database
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                autor = snapshot.child("name").getValue().toString();
                String email = snapshot.child("email").getValue().toString();
                String foto="";
                //Cargo los datos del usuario en el header del navigation drawer
                View headerView = binding.navView.getHeaderView(0);
                TextView nombreUsuario = headerView.findViewById(R.id.nav_header_username);
                TextView emailUsuario = headerView.findViewById(R.id.nav_header_email);
                ImageView fotoUsuario = headerView.findViewById(R.id.nav_header_photo);
                //Quiero comprobar si el usuario tiene foto de perfil o no
                if (snapshot.hasChild("photo")) {
                    foto = snapshot.child("photo").getValue().toString();
                    Transformation transformation = new Transformation() {
                        @Override
                        public Bitmap transform(Bitmap source) {
                            int targetWidth = fotoUsuario.getWidth();
                            int targetHeight = fotoUsuario.getHeight();
                            int radius = Math.min(targetWidth, targetHeight) / 2;

                            Bitmap transformedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(transformedBitmap);
                            Paint paint = new Paint();
                            paint.setAntiAlias(true);
                            paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

                            canvas.drawCircle(targetWidth / 2, targetHeight / 2, radius, paint);
                            source.recycle();
                            return transformedBitmap;
                        }

                        @Override
                        public String key() {
                            return "circle";
                        }
                    };

                    Picasso.get()
                            .load(foto)
                            .fit() // Ajustar la imagen al tamaño del ImageView
                            .centerCrop()
                            .transform(transformation) // Aplicar la transformación circular
                            .placeholder(R.drawable.placeholder_image)
                            .into(fotoUsuario);
                }else{
                    fotoUsuario.setImageResource(R.drawable.user_default);
                }
                nombreUsuario.setText(autor);
                emailUsuario.setText(email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    //Método que se ejecuta cuando el usuario selecciona un archivo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Compruebo que el usuario ha seleccionado un archivo
        if (requestCode == 1 && resultCode == RESULT_OK) {
            //Obtengo la uri del archivo seleccionado
            Uri uri = data.getData();
            //Obtengo el nombre del archivo
            String nombreArchivo = uri.getLastPathSegment();
            //Obtengo el tipo del archivo
            ContentResolver cR = getContentResolver();
            String type = cR.getType(uri);
            Toast.makeText(this, type, Toast.LENGTH_SHORT).show();
            //Con el nombre del archivo mas el uid del usuario creo la ruta a guardar en la base de datos
            Archivo archivo = new Archivo(nombreArchivo, "", "", false, false, false);
            storageRef = storage.getReference().child("users/" + uid + "/" + nombreArchivo);
            //Tengo que comprobar que ese archivo no existe ya en la base de datos

            //Subo el archivo a la base de datos
            UploadTask uploadTask = storageRef.putFile(uri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //Si no se ha subido correctamente, muestro un mensaje de error
                    Toast.makeText(HomeActivity.this, "Error al subir el archivo", Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Si se ha subido correctamente, muestro un mensaje de éxito
                    Toast.makeText(HomeActivity.this, "Archivo subido correctamente", Toast.LENGTH_SHORT).show();

                    // Obtener los números del final del nombre
                    String[] parts = nombreArchivo.split(":");
                    String name = parts[parts.length - 1];
                    // Obtener la extensión del archivo
                    String extension = type.split("/")[1];
                    archivo.setNameMetadata(name);
                    if (extension.contains("vnd")) {
                        extension = "docx";
                    }
                    if (extension.contains("plain")) {
                        extension = "txt";
                    }
                    archivo.setExtension(extension);
                    // Añadir los metadatos al archivo
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setCustomMetadata("Name", name)
                            .setCustomMetadata("Extension", extension)
                            .setCustomMetadata("Favorito", "false")
                            .setCustomMetadata("Autor", autor)
                            .build();
                    storageRef.updateMetadata(metadata);
                    //Actualizo el recycler view volviendo a llamar a la vista home
                    AdaptadorExternoHome adaptadorExternoHome = AdaptadorExternoHome.getInstancia();
                    //Compruebo si es una foto o no
                    if (type.contains("image")) {
                        archivo.setImagen(uri.toString());
                    }
                    adaptadorExternoHome.agregarArchivo(archivo);
                    adaptadorExternoHome.notifyDataSetChanged();

                }
            });
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            //Obtengo la uri del archivo seleccionado
            Uri uri = data.getData();
            //Obtengo el nombre del archivo
            String nombreArchivo = uri.getLastPathSegment();
            //Obtengo el tipo del archivo
            ContentResolver cR = getContentResolver();
            String type = cR.getType(uri);
            Toast.makeText(this, type, Toast.LENGTH_SHORT).show();
            //Con el nombre del archivo mas el uid del usuario creo la ruta a guardar en la base de datos
            Archivo archivo = new Archivo(nombreArchivo, "", "", false, false, false);
            storageRef = storage.getReference().child("users/" + uid + "/" + nombreArchivo);

            //Subo el archivo a la base de datos
            UploadTask uploadTask = storageRef.putFile(uri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //Si no se ha subido correctamente, muestro un mensaje de error
                    Toast.makeText(HomeActivity.this, "Error al subir el archivo", Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Si se ha subido correctamente, muestro un mensaje de éxito
                    Toast.makeText(HomeActivity.this, "Archivo subido correctamente", Toast.LENGTH_SHORT).show();

                    // Obtener los números del final del nombre
                    String[] parts = nombreArchivo.split(":");
                    String name = parts[parts.length - 1];
                    // Obtener la extensión del archivo
                    String extension = type.split("/")[1];
                    archivo.setNameMetadata(name);
                    if (extension.contains("vnd")) {
                        extension = "docx";
                    }
                    if (extension.contains("plain")) {
                        extension = "txt";
                    }
                    archivo.setExtension(extension);
                    // Añadir los metadatos al archivo
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setCustomMetadata("Name", name)
                            .setCustomMetadata("Extension", extension)
                            .setCustomMetadata("Favorito", "false")
                            .setCustomMetadata("Autor", autor)
                            .build();
                    storageRef.updateMetadata(metadata);
                    //Actualizo el recycler view volviendo a llamar a la vista home
                    AdaptadorExternoHome adaptadorExternoHome = AdaptadorExternoHome.getInstancia();
                    archivo.setImagen(uri.toString());
                    adaptadorExternoHome.agregarArchivo(archivo);
                    adaptadorExternoHome.notifyDataSetChanged();

                }
            });

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        selectItemNav(item);

        return true;
    }


    @SuppressLint("NonConstantResourceId")
    private void selectItemNav(MenuItem item) {
        if(item.getItemId()==R.id.nav_signOut){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cerrar sesión");
            builder.setMessage("¿Estás seguro de que quieres cerrar sesión?");
            builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            builder.setNegativeButton("No", null);
            builder.create().show();
        }else if(item.getItemId()==R.id.nav_home){
            binding.appBarHome.uploadFiles.setVisibility(View.VISIBLE);
            navController.navigate(R.id.nav_home);
        }else if (item.getItemId()==R.id.nav_favoritos) {
            binding.appBarHome.uploadFiles.setVisibility(View.GONE);
            navController.navigate(R.id.nav_favoritos);
            //pongo visible gone el floating action button
        }else if (item.getItemId()==R.id.nav_shared) {
            binding.appBarHome.uploadFiles.setVisibility(View.GONE);
            navController.navigate(R.id.nav_shared);
        }else if (item.getItemId()==R.id.nav_papelera) {
            navController.navigate(R.id.nav_papelera);
            binding.appBarHome.uploadFiles.setVisibility(View.GONE);
        }else if(item.getItemId()==R.id.nav_profile){
            //Creo un dialogo para que el usuario introduzca el nombre de la carpeta
            navController.navigate(R.id.nav_profile);
            binding.appBarHome.uploadFiles.setVisibility(View.GONE);
        }
        //Cierro el drawer
        drawer.closeDrawer(GravityCompat.START);
    }


    @Override
    public void onNameCarpeta(String nuevoNombre) {
        StorageReference subcarpetaRef = storage.getReference().child("users/" + uid + "/" + nuevoNombre + "/");


        // Subir un archivo sin contenido a la subcarpeta
        byte[] emptyData = new byte[0];
        UploadTask uploadTask = subcarpetaRef.child("borrar.txt").putBytes(emptyData);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
                    // El archivo se ha subido exitosamente, lo que simula la creación de la subcarpeta vacía
                    // Puedes realizar las operaciones adicionales aquí
                    Archivo archivo = new Archivo(nuevoNombre, nuevoNombre, "carpeta", false, false, true);
                    AdaptadorExternoHome.getInstancia().agregarArchivo(archivo);
                    AdaptadorExternoHome.getInstancia().notifyDataSetChanged();
                    //Borro el archivo vacío
                    subcarpetaRef.child("empty.txt").delete();
                })
                .addOnFailureListener(exception -> {
                    //Un dialog de error
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Error");
                    builder.setMessage("No se ha podido crear la carpeta");
                    builder.setPositiveButton("Aceptar", null);
                    builder.create().show();
                });

    }
}
