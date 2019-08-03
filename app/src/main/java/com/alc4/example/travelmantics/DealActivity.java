package com.alc4.example.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class DealActivity extends AppCompatActivity {

    public static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtPrice;
    EditText txtDescription;
    ImageView mImageView;
    TravelDeal deal;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        mImageView = findViewById(R.id.imageView);


        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        Intent dealIntent = getIntent();
        TravelDeal deal = (TravelDeal) dealIntent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());

        Button btnImage = findViewById(R.id.btn_addImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();
            final StorageReference ref = FirebaseUtil.mStorageref.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                    final String pictureName = taskSnapshot.getStorage().getPath();
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // showToast(uri.toString());
                            String url = uri.toString();
                            deal.setImageUrl(url);
                            showImage(url);
                            deal.setImageName(pictureName);

                        }
                    });
                }
            });

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.save_menu:

                saveDeal();
                showToast("Deal Saved");
                clear();
                backToList();

                return true;
            case R.id.delete_menu:
                deleteDeal();
                backToList();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clear() {

        txtTitle.setText("");
        txtPrice.setText("");
        txtDescription.setText("");
        txtTitle.requestFocus();

    }

    private void showToast(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }

    private void saveDeal() {

        deal.setTitle(txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDescription.getText().toString());

        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }

    }

    private void deleteDeal() {
        if (deal.getId() == null) {
            showToast("Please save the deal before deleting");


        } else {
            mDatabaseReference.child(deal.getId()).removeValue();

            if (deal.getImageName() != null && !deal.getImageName().isEmpty()) {

                StorageReference picRef = FirebaseUtil.mFirebaseStorage.getReference().child(deal.getImageName());
                picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Log.d("Delete Image", "Image Deleted Successfully");

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.d("Delete Image", e.getMessage());

                    }
                });
            }
            showToast("Deal Deleted");
        }
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.save_menu, menu);

        if (FirebaseUtil.isAdmin) {

            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
            findViewById(R.id.btn_addImage).setEnabled(true);

        } else {

            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
            findViewById(R.id.btn_addImage).setEnabled(false);

        }

        return true;
    }

    private void enableEditTexts(boolean isEnaabled) {

        txtTitle.setEnabled(isEnaabled);
        txtPrice.setEnabled(isEnaabled);
        txtDescription.setEnabled(isEnaabled);

    }

    public void showImage(String url) {

        if (url != null && !url.isEmpty()) {

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;

            Picasso.get().load(url)
                    .resize(width, width * 2 / 3)
                    .centerCrop()
                    .into(mImageView);
        }
    }
}
