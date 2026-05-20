package com.example.studentassistantappv1.activities;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentassistantappv1.R;
import java.util.ArrayList;
import java.util.List;

/**
 * 🔥 ফাইলটির নাম অবশ্যই PdfViewerActivity.java হতে হবে (P বড় হাতের)।
 */
public class PdfViewerActivity extends AppCompatActivity {

    private RecyclerView rvPdfPages;
    private PdfRenderer pdfRenderer;
    private final List<Bitmap> pageBitmaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        rvPdfPages = findViewById(R.id.rvPdfPages);
        ImageView btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String uriString = getIntent().getStringExtra("pdf_uri");
        if (uriString != null) {
            renderPdf(Uri.parse(uriString));
        }
    }

    private void renderPdf(Uri uri) {
        try {
            // URI থেকে ফাইল ডেসক্রিপ্টর ওপেন করা (Read Mode)
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r");
            if (fd != null) {
                pdfRenderer = new PdfRenderer(fd);
                for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);
                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    pageBitmaps.add(bitmap);
                    page.close();
                }

                rvPdfPages.setLayoutManager(new LinearLayoutManager(this));
                rvPdfPages.setAdapter(new PdfPageAdapter(pageBitmaps));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // PDF Pages-এর জন্য অভ্যন্তরীণ অ্যাডাপ্টার
    private static class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> {
        private final List<Bitmap> bitmaps;
        PdfPageAdapter(List<Bitmap> bitmaps) { this.bitmaps = bitmaps; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.ivPage.setImageBitmap(bitmaps.get(position));
        }
        @Override public int getItemCount() { return bitmaps.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPage;
            ViewHolder(View v) { super(v); ivPage = v.findViewById(R.id.ivPdfPage); }
        }
    }
}
