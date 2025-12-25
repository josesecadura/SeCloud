package adaptadores;

import android.annotation.SuppressLint;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.R;
import modelos.Archivo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AdaptadorExternoBin extends RecyclerView.Adapter<AdaptadorExternoBin.ContenedorVistas> {

    private OnArchivoClickListener archivoClickListener;
    private ArrayList<Archivo> datos;
    public AdaptadorExternoBin(ArrayList<Archivo> datos) {
        this.datos = datos;
    }
    public ArrayList<Archivo> getDatos() {
        return datos;
    }
    public interface OnArchivoClickListener {
        void onArchivoClick(int position);
    }

    public void setOnArchivoClickListener(OnArchivoClickListener listener) {
        this.archivoClickListener = listener;
    }

    @NonNull
    @Override
    public ContenedorVistas onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        item = inflater.inflate(R.layout.item_layout_bin, null);

        ContenedorVistas contenedorVistas = new ContenedorVistas(item);
        return contenedorVistas;
    }

    @Override
    public void onBindViewHolder(@NonNull ContenedorVistas holder, @SuppressLint("RecyclerView") int position) {
        Archivo archivo = datos.get(position);
        holder.nameArchivo.setText(archivo.getNameMetadata() + "." + archivo.getExtension());


        //Voy a hacer un switch para añadir la imagen dependiendo de la extension
        switch (archivo.getExtension()) {
            case "pdf":
            case "odp":
            case "pps":
            case "ppt":
            case "pptx":
                holder.ivImagen.setImageResource(R.drawable.icon_pdf2);
                break;
            case "docx":
            case "doc":
            case "odt":
            case "rtf":
            case "tex":
            case "wpd":
            case "wps":
                holder.ivImagen.setImageResource(R.drawable.icon_word);
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
                holder.ivImagen.setImageResource(R.drawable.icon_txt);
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
                holder.ivImagen.setImageResource(R.drawable.icon_audio2);
                break;
            case "mp4":
            case "avi":
            case "mov":
            case "wmv":
            case "flv":
            case "3gp":
            case "mkv":
                holder.ivImagen.setImageResource(R.drawable.icon_video);
                break;
            default:
                holder.ivImagen.setImageResource(R.drawable.icon_txt);
                break;
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


    @Override
    public int getItemCount() {
        return datos.size();
    }


    public class ContenedorVistas extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private TextView nameArchivo;
        private ImageView ivImagen;


        public ContenedorVistas(View vista) {
            super(vista);
            nameArchivo = vista.findViewById(R.id.label_titulo);
            ivImagen = vista.findViewById(R.id.imageView);
            vista.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            final int BORRAR = 122;
            menu.setHeaderTitle("Selecciona una opción");
            menu.add(getAdapterPosition(), BORRAR, 0, "Borrar definitivamente");
        }
    }
}
