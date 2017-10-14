package com.example.android.inventoryappproject;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryappproject.data.ProductContract;


public class ProductCursorAdapter extends CursorAdapter {

    public ProductCursorAdapter(Context context, Cursor cursor, boolean autoReQuery) {
        super(context, cursor, autoReQuery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView productName = view.findViewById(R.id.text_view_name);
        TextView productPrice = view.findViewById(R.id.text_view_price);
        TextView productQuantity = view.findViewById(R.id.text_view_quantity);
        ImageView productImage = view.findViewById(R.id.product_image);


        String name = cursor.getString(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_NAME));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_PRICE));
        final int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_QUANTITY));
        byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_IMAGE));
        if (imageBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        }
        final Uri uri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI,
                cursor.getInt(cursor.getColumnIndexOrThrow(ProductContract.ProductEntry._ID)));


        productName.setText(name);
        productPrice.setText(context.getString(R.string.label_price) + " " + price);
        productQuantity.setText(quantity + " " + context.getString(R.string.label_quantity));

        Button saleButton = view.findViewById(R.id.button_sale);

        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (quantity > 0) {

                    int newQuantity = quantity - 1;

                    ContentValues values = new ContentValues();
                    values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, newQuantity);

                    context.getContentResolver().update(uri, values, null, null);
                } else {

                    Toast.makeText(context, context.getString(R.string.product_out_of_stock_toast),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
