package adaptadores;

import android.annotation.SuppressLint;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.R;
import modelos.Archivo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AdaptadorExternoHome extends RecyclerView.Adapter<AdaptadorExternoHome.ContenedorVistas> {

    //Voy a hacer que sea singletoon para solo tener una instancia de la clase y que no se creen varias
    private static AdaptadorExternoHome instancia = null;
    private ArrayList<Archivo> datos;
    //Un metodo para vaciar el arraylist
    public void vaciarDatos(){
        datos.clear();
    }
    //Un metodo para añadir un arraylist
    public void addDatos(ArrayList<Archivo> datos){
        this.datos.addAll(datos);
    }
    public AdaptadorExternoHome(ArrayList<Archivo> datos) {
        this.datos = datos;
    }

    public static AdaptadorExternoHome getInstancia(ArrayList<Archivo> datos) {
        if (instancia == null) {
            instancia = new AdaptadorExternoHome(datos);
        }
        return instancia;
    }
    public static AdaptadorExternoHome getInstancia() {
        return instancia;
    }
    private OnArchivoClickListener archivoClickListener;



    public ArrayList<Archivo> getDatos() {
        return datos;
    }

    public interface OnArchivoClickListener {
        void onArchivoClick(int position);
    }
    public void agregarArchivo(Archivo carpeta) {
        for (Archivo a: datos) {
            if (a.getNameMetadata().equals(carpeta.getNameMetadata())) {
                return;
            }
        }
        datos.add(carpeta);
        Collections.sort(datos, new Comparator<Archivo>() {
            @Override
            public int compare(Archivo archivo1, Archivo archivo2) {
                AdaptadorExternoHome.getInstancia().notifyDataSetChanged();
                return archivo1.getNameMetadata().compareTo(archivo2.getNameMetadata());
            }
        });
        //Debo notificar al adaptador la posicion en la que se ha añadido el archivo
        notifyItemInserted(datos.size()-1);
    }
    public void setOnArchivoClickListener(OnArchivoClickListener listener) {
        this.archivoClickListener = listener;
    }

    @NonNull
    @Override
    public ContenedorVistas onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        item = inflater.inflate(R.layout.item_layout, null);

        ContenedorVistas contenedorVistas = new ContenedorVistas(item);
        return contenedorVistas;
    }

    @Override
    public void onBindViewHolder(@NonNull ContenedorVistas holder, @SuppressLint("RecyclerView") int position) {
        Archivo archivo = datos.get(position);
        ImageButton btnFavorito = holder.btnFavorito;
        if(archivo.isCarpeta()){
            holder.nameArchivo.setText(archivo.getNameMetadata());
            //Quiero que no se muestre la estrella
            btnFavorito.setVisibility(View.INVISIBLE);
            holder.ivImagen.setImageResource(R.drawable.carpeta);
        }else {
            if(archivo.isFavorito()){
                btnFavorito.setImageResource(android.R.drawable.btn_star_big_on);
            }else{
                btnFavorito.setImageResource(android.R.drawable.btn_star_big_off);
            }
            holder.nameArchivo.setText(archivo.getNameMetadata()+"."+archivo.getExtension());

            btnFavorito.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Actualizar la imagen del ImageButton según el nuevo estado de la estrella
                    modificarEstrella(archivo, btnFavorito);
                }
            });

            //Voy a hacer un switch para añadir la imagen dependiendo de la extension
            switch (archivo.getExtension()) {
                case "pdf":
                case "odp":
                case "pps":
                case "ppt":
                case "pptx":
                    holder.ivImagen.setImageResource(R.drawable.icon_pdf);
                    break;
                case "docx":
                case "doc":
                case "odt":
                case "rtf":
                case "tex":
                case "wpd":
                case "wps":
                    //Quiero cargar una vista previa del archivo docx en el ImageView

                    break;
                case "txt":
                case "csv":
                case "xml":
                case "json":
                case "html":
                case "css":
                case "js":
                case "php":
                case "java":
                case "py":
                case "c":
                case "cpp":
                case "h":
                case "cs":
                case "vb":
                case "sql":

                    break;
                case "png":
                case "jpg":
                case "jpeg":
                case "gif":
                case "bmp":
                    // Agregarla al Imageiew con Picasso
                    // Agregarla al ImageView con Picasso
                    Picasso.get()
                            .load(archivo.getImagen())
                            .resize(2000, 3000)
                            .centerCrop()
                            .placeholder(R.drawable.placeholder_image) // Agrega un placeholder mientras se carga la imagen
                            .into(holder.ivImagen);
                    break;
                case "mp3":
                case "wav":
                case "ogg":
                    holder.ivImagen.setImageResource(R.drawable.icon_audio);
                    break;
                case "mp4":
                case "avi":
                case "mov":
                case "wmv":
                case "flv":
                case "3gp":
                case "mkv":
                    holder.ivImagen.setImageResource(R.drawable.icon_imagen);
                    break;
                case "carpeta":
                    holder.ivImagen.setImageResource(R.drawable.carpeta);
                    break;
                default:
                    holder.ivImagen.setImageResource(R.drawable.placeholder_image);
                    break;
            }
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (archivoClickListener != null) {
                    archivoClickListener.onArchivoClick(position);
                }
            }
        });

    }

    private void modificarEstrella(Archivo archivo, ImageButton btnFavorito) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        StorageReference storageRef = storage.getReference().child("users/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/" + archivo.getUriArchivo());

        if (archivo.isFavorito()) {
            btnFavorito.setImageResource(android.R.drawable.btn_star_big_off);
            archivo.setFavorito(false);
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setCustomMetadata("Favorito", "false")
                    .build();
            storageRef.updateMetadata(metadata)
                    .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                        @Override
                        public void onSuccess(StorageMetadata storageMetadata) {
                            // Metadatos actualizados con éxito

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Error al actualizar los metadatos
                        }
                    });
        } else {
            btnFavorito.setImageResource(android.R.drawable.btn_star_big_on);
            archivo.setFavorito(true);
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setCustomMetadata("Favorito", "true")
                    .build();
            storageRef.updateMetadata(metadata)
                    .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                        @Override
                        public void onSuccess(StorageMetadata storageMetadata) {
                            // Metadatos actualizados con éxito

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Error al actualizar los metadatos
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }


    public class ContenedorVistas extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private TextView nameArchivo;
        private ImageView ivImagen;

        private ImageButton btnFavorito;

        public ContenedorVistas(View vista) {
            super(vista);
            btnFavorito = vista.findViewById(R.id.btn_fav);
            nameArchivo = vista.findViewById(R.id.label_titulo);
            ivImagen = vista.findViewById(R.id.imageView);
            vista.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            final int EDITAR = 121;
            final int BORRAR = 122;
            final int COMPARTIR = 123;
            menu.setHeaderTitle("Selecciona una opción");
            if(!datos.get(getAdapterPosition()).isCarpeta()) {
                menu.add(getAdapterPosition(), EDITAR, 0, "Cambiar Nombre");
            }
            menu.add(getAdapterPosition(), BORRAR, 1, "Borrar");
            menu.add(getAdapterPosition(), COMPARTIR, 2, "Compartir");
        }
    }
}
