package adaptadores;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secloud.R;
import modelos.Archivo;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;

public class AdaptadorExternoShared extends RecyclerView.Adapter<AdaptadorExternoShared.ContenedorVistas> {

    //Voy a hacer que sea singletoon para solo tener una instancia de la clase y que no se creen varias

    private ArrayList<Archivo> datos;

    //Un metodo para vaciar el arraylist
    public void vaciarDatos() {
        datos.clear();
    }

    //Un metodo para añadir un arraylist
    public void addDatos(ArrayList<Archivo> datos) {
        this.datos.addAll(datos);
    }

    public AdaptadorExternoShared(ArrayList<Archivo> datos) {
        this.datos = datos;
    }

    private OnArchivoClickListener archivoClickListener;

    public interface OnDataUpdateListenerH {
        void onDataUpdatedHome();
    }

    private OnDataUpdateListenerH onDataUpdateListenerH;

    public void setOnDataUpdateListener(AdaptadorExternoShared.OnDataUpdateListenerH listener) {
        this.onDataUpdateListenerH = listener;
    }

    public ArrayList<Archivo> getDatos() {
        return datos;
    }

    public interface OnArchivoClickListener {
        void onArchivoClick(int position);

        void onBorrarClick(Archivo archivo);

        void onCompartirClick(int position);
    }



    public void setOnArchivoClickListener(OnArchivoClickListener listener) {
        this.archivoClickListener = listener;
    }

    @NonNull
    @Override
    public ContenedorVistas onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        item = inflater.inflate(R.layout.item_layout_shared, parent, false);
        ContenedorVistas contenedorVistas = new ContenedorVistas(item);
        return contenedorVistas;
    }

    @Override
    public void onBindViewHolder(@NonNull ContenedorVistas holder, @SuppressLint("RecyclerView") int position) {
        Archivo archivo = datos.get(position);
        holder.nameArchivo.setText(archivo.getNameMetadata() + "." + archivo.getExtension());
        holder.nameAutor.setText(archivo.getAutor());

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
                // Agregarla al ImageView con Picasso
                Transformation transformation = new Transformation() {
                    @Override
                    public Bitmap transform(Bitmap source) {
                        int targetWidth = holder.ivImagen.getWidth();
                        int targetHeight = holder.ivImagen.getHeight();
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
                        .load(archivo.getImagen())
                        .fit() // Ajustar la imagen al tamaño del ImageView
                        .centerCrop()
                        .transform(transformation) // Aplicar la transformación circular
                        .placeholder(R.drawable.placeholder_image)
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


    public class ContenedorVistas extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView nameArchivo;
        private TextView nameAutor;
        private ImageView ivImagen;
        private ImageButton btnMenu;

        public ContenedorVistas(View vista) {
            super(vista);
            btnMenu = vista.findViewById(R.id.imageButton);
            nameArchivo = vista.findViewById(R.id.nombreArchivo);
            nameAutor = vista.findViewById(R.id.nombreUsuario);
            ivImagen = vista.findViewById(R.id.imageArchivo);

            // Agregar el listener de clic para el botón de menú
            btnMenu.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.imageButton) {
                // Mostrar el menú personalizado al hacer clic en el botón de menú
                showCustomPopupMenu(v);
            }
        }

        private void showCustomPopupMenu(View anchorView) {
            PopupMenu popupMenu = new PopupMenu(anchorView.getContext(), anchorView);
            //popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

            // Agregar los elementos de menú personalizados
            final int BORRAR = 121;
            final int COMPARTIR = 122;
            popupMenu.getMenu().add(0, BORRAR, 0, "Quitar de compartidos");
            popupMenu.getMenu().add(0, COMPARTIR, 1, "Compartir con...");

            // Establecer el listener de clic para los elementos de menú
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // Manejar los clics en los elementos de menú
                    int position = getAdapterPosition();
                    switch (item.getItemId()) {
                        case BORRAR:
                            // Acción para el elemento "Cambiar Nombre"
                            if (position != RecyclerView.NO_POSITION) {
                                //Elimino el archivo de la lista y de la base de datos
                                Archivo archivo = datos.get(position);
                                archivoClickListener.onBorrarClick(archivo);
                            }
                            return true;
                        case COMPARTIR:
                            // Acción para el elemento "Borrar"
                            if (position != RecyclerView.NO_POSITION) {
                                archivoClickListener.onCompartirClick(position);
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });

            // Mostrar el menú personalizado
            popupMenu.show();
        }
    }
}
