package com.example.android.inventoryappproject;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryappproject.data.ProductContract;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.example.android.inventoryappproject.data.ProductProvider.LOG_TAG;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int SELECT_PHOTO = 10;
    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;
    private static final int URI_LOADER = 0;
    private EditText mNameEditText;
    private TextView mQuantityTextView;
    private int mProductQuantity;
    private EditText mPriceEditText;
    private String mProductName;
    private Button mIncreaseQuantityByOneButton;
    private Button mDecreaseQuantityByOneButton;
    private Button mSelectImageButton;
    private ImageView mProductImageView;
    private Bitmap mProductBitmap;
    private EditText phoneEditView;
    private Button mOrderButtonEmail;
    private Button mOrderButtonPhone;
    private Uri mProductUri;
    private EditText mContactEmailEditText;
    private boolean mProductHasChanged = false;

    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductHasChanged = true;
            return false;
        }
    };

    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 70, image.length);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent intent = getIntent();
        mProductUri = intent.getData();

        if (mProductUri == null) {

            setTitle(getString(R.string.add_new_product));
            invalidateOptionsMenu();
        } else {

            setTitle(getString(R.string.edit_existing_product));
            getLoaderManager().initLoader(URI_LOADER, null, this);
        }

        initialiseViews();

        setOnTouchListener();
    }

    private String parsePhone(String phoneString) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < phoneString.length(); i++) {
            char phoneChar = phoneString.charAt(i);
            if (phoneChar >= '0' && phoneChar <= '9') {
                result.append(phoneChar);
            }
        }
        return result.toString();
    }

    private void initialiseViews() {

        if (mProductUri != null) {
            mOrderButtonEmail = (Button) findViewById(R.id.email_order_from_supplier);
            mOrderButtonPhone = (Button) findViewById(R.id.phone_order_from_supplier);

            mOrderButtonEmail.setVisibility(View.VISIBLE);
            mOrderButtonEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    String to = mContactEmailEditText.getText().toString();
                    String supplier = mNameEditText.getText().toString();
                    String sep = System.getProperty("line.separator");
                    String message = "I would like to order 10 more copies of " + mProductName + " . " + sep + "Regards," + sep + "Amal";
                    emailIntent.setData(Uri.parse("mailto:" + to));

                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Order " + mProductName);
                    emailIntent.putExtra(Intent.EXTRA_TEXT, message);

                    try {
                        startActivity(emailIntent);
                        finish();
                        Log.i(LOG_TAG, "Finished sending email...");
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(DetailActivity.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            mOrderButtonPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    String phoneNumber = parsePhone(phoneEditView.getText().toString());
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                }
            });
        }

        mNameEditText = (EditText) findViewById(R.id.edit_text_name);
        mContactEmailEditText = (EditText) findViewById(R.id.detail_email_et);
        mPriceEditText = (EditText) findViewById(R.id.edit_text_price);
        phoneEditView = (EditText) findViewById(R.id.detail_phone_et);
        mQuantityTextView = (TextView) findViewById(R.id.text_view_quantity_final);

        mIncreaseQuantityByOneButton = (Button) findViewById(R.id.button_increase_one);
        mIncreaseQuantityByOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProductQuantity++;
                mQuantityTextView.setText(String.valueOf(mProductQuantity));
            }
        });

        mDecreaseQuantityByOneButton = (Button) findViewById(R.id.button_decrease_one);
        mDecreaseQuantityByOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mProductQuantity > 0) {

                    mProductQuantity--;

                    mQuantityTextView.setText(String.valueOf(mProductQuantity));
                } else {

                    Toast.makeText(DetailActivity.this, getString(R.string.invalid_product_quantity_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });


        mProductImageView = (ImageView) findViewById(R.id.detail_image);

        mSelectImageButton = (Button) findViewById(R.id.button_select_image);
        mSelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openImageSelector();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                        return;
                    }
                    Intent getIntent = new Intent(Intent.ACTION_PICK);
                    getIntent.setType("image/*");
                    startActivityForResult(getIntent, SELECT_PHOTO);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && data != null) {

            Uri selectedImage = data.getData();
            Log.v("DetailActivity", "Uri: " + selectedImage.toString());
            String[] filePatchColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePatchColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePatchColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            mProductBitmap = BitmapFactory.decodeFile(picturePath);

            mProductBitmap = getBitmapFromUri(selectedImage);
            mProductImageView = (ImageView) findViewById(R.id.detail_image);
            mProductImageView.setImageBitmap(mProductBitmap);
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty()) {
            return null;
        }

        mProductImageView = (ImageView) findViewById(R.id.detail_image);
        int targetW = mProductImageView.getWidth();
        int targetH = mProductImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e("AddActivity", "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e("AddActivity", "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_detail_activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mProductHasChanged) {
                    saveProduct();
                } else {
                    Toast.makeText(this, getString(R.string.no_changes_toast), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_delete:

                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:

                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                } else {

                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                                }
                            };

                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
        }


        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }


    private void saveProduct() {
        boolean nameIsEmpty = checkFieldEmpty(mNameEditText.getText().toString().trim());
        boolean priceIsEmpty = checkFieldEmpty(mPriceEditText.getText().toString().trim());

        if (nameIsEmpty) {
            Toast.makeText(this, getString(R.string.invalid_product_name_add_toast), Toast.LENGTH_SHORT).show();
        } else if (mProductQuantity <= 0) {
            Toast.makeText(this, getString(R.string.invalid_product_quantity_add_toast), Toast.LENGTH_SHORT).show();
        } else if (priceIsEmpty) {
            Toast.makeText(this, getString(R.string.invalid_product_price_add_toast), Toast.LENGTH_SHORT).show();
        } else if (mProductBitmap == null) {
            Toast.makeText(this, getString(R.string.invalid_product_image_add_toast), Toast.LENGTH_SHORT).show();
        } else {

            String name = mNameEditText.getText().toString().trim();
            double price = Double.parseDouble(mPriceEditText.getText().toString().trim());
            ContentValues values = new ContentValues();
            values.put(ProductContract.ProductEntry.COLUMN_NAME, name);
            values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, mProductQuantity);
            values.put(ProductContract.ProductEntry.COLUMN_PRICE, price);

            if (mProductBitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                boolean a = mProductBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                values.put(ProductContract.ProductEntry.COLUMN_IMAGE, byteArray);
            }


            if (mProductUri == null) {

                Uri newUri = getContentResolver().insert(ProductContract.ProductEntry.CONTENT_URI, values);

                if (newUri == null) {

                    Toast.makeText(this, getString(R.string.failed_insert_toast),
                            Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(this, getString(R.string.successful_insert_toast),
                            Toast.LENGTH_SHORT).show();
                }
            } else {

                int rowsAffected = getContentResolver().update(mProductUri, values, null, null);

                if (rowsAffected == 0) {

                    Toast.makeText(this, getString(R.string.failed_update_toast), Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(this, getString(R.string.successful_update_toast),
                            Toast.LENGTH_SHORT).show();
                }

            }
            finish();
        }
    }

    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO);
    }

    private boolean checkFieldEmpty(String string) {
        return TextUtils.isEmpty(string) || string.equals(".");
    }


    private void deleteProduct() {
        if (mProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mProductUri, null, null);
            if (rowsDeleted == 0) {

                Toast.makeText(this, getString(R.string.failed_delete_toast), Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(this, getString(R.string.successful_delete_toast), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.prompt_delete_product));
        builder.setPositiveButton(getString(R.string.prompt_delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
                finish();
            }
        });

        builder.setNegativeButton(getString(R.string.prompt_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.prompt_leave_without_save));
        builder.setPositiveButton(getString(R.string.prompt_yes), discardButtonClickListener);
        builder.setNegativeButton(getString(R.string.prompt_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setOnTouchListener() {
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mIncreaseQuantityByOneButton.setOnTouchListener(mTouchListener);
        mDecreaseQuantityByOneButton.setOnTouchListener(mTouchListener);
        mSelectImageButton.setOnTouchListener(mTouchListener);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductContract.ProductEntry._ID,
                ProductContract.ProductEntry.COLUMN_NAME,
                ProductContract.ProductEntry.COLUMN_PRICE,
                ProductContract.ProductEntry.COLUMN_QUANTITY,
                ProductContract.ProductEntry.COLUMN_IMAGE
        };

        switch (id) {
            case URI_LOADER:
                return new CursorLoader(
                        this,
                        mProductUri,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }

        if (data.moveToFirst()) {
            mProductName = data.getString(data.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME));
            mNameEditText = (EditText) findViewById(R.id.edit_text_name);
            mNameEditText.setText(mProductName);

            mPriceEditText = (EditText) findViewById(R.id.edit_text_price);
            mPriceEditText.setText(data.getString(data.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE)));

            mQuantityTextView = (TextView) findViewById(R.id.text_view_quantity_final);
            mProductQuantity = data.getInt(data.getColumnIndex(ProductContract.ProductEntry.COLUMN_QUANTITY));
            mQuantityTextView.setText(String.valueOf(mProductQuantity));

            byte[] bytesArray = data.getBlob(data.getColumnIndexOrThrow(ProductContract.ProductEntry.COLUMN_IMAGE));
            if (bytesArray != null) {
                mProductBitmap = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.length);
                mProductImageView = (ImageView) findViewById(R.id.detail_image);
                mProductImageView.setImageBitmap(mProductBitmap);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.getText().clear();
        mQuantityTextView.setText("");
    }
}