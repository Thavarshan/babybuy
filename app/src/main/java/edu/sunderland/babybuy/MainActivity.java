package edu.sunderland.babybuy;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.sunderland.babybuy.login.LoginActivity;
import edu.sunderland.babybuy.products.Collection;
import edu.sunderland.babybuy.products.Product;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    FirebaseAuth auth;
    FirebaseUser currentUser;

    FirebaseStorage storage;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference productRef = db.collection("refs");

    ArrayList<Product> products = new ArrayList<>();
    Collection collection;
    private String selectedId = "";

    EditText etName, etPrice, etDescription;
    Button btnImageUpload, btnAdd, btnEdit, btnDelete;
    TextView tvImagePath;
    ListView lvProducts;
    CheckBox cbPurchased;

    URL imageUrl = null;
    String  productImageUrl = "";
    String filePath = "";
    StorageReference storageRef ;

    private Intent loginActivity;

    private SensorManager sensorManager;
    private Sensor sensor;
    private long lastUpdated, actualTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lastUpdated = System.currentTimeMillis();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        loginActivity = new Intent(this, LoginActivity.class);

        storage = FirebaseStorage.getInstance();
        storageRef= storage.getReferenceFromUrl("gs://babybuy-65961.appspot.com");

        etName = findViewById(R.id.etProductName);
        etPrice = findViewById(R.id.etProductPrice);
        etDescription = findViewById(R.id.etProductDescription);
        cbPurchased = findViewById(R.id.cbPurchased);

        btnAdd = findViewById(R.id.btnAdd);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnImageUpload = findViewById(R.id.btnImageUpload);
        btnImageUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { chooseImage(); }
        });

        tvImagePath = findViewById(R.id.tvImagePath);

        lvProducts = findViewById(R.id.lvProducts);
        lvProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Product product = collection.getItem(position);
                etName.setText(product.getName());
                etDescription.setText(product.getDescription());
                etPrice.setText(product.getPrice());
                cbPurchased.setChecked(product.getPurchased().equals("Purchased") ? true : false);
                tvImagePath.setText(product.getFilePath());
                productImageUrl = product.getFilePath();
                selectedId = product.getDocId();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            logOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logOut() {
        auth.signOut();

        try {
            Auth auth = new Auth(MainActivity.this);
            auth.logoutSession();
            updateUI();
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    private void updateUI() {
        startActivity(loginActivity);
        finish();
    }

    private void showMessage(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    private void chooseImage() {
        Album.image(this) // Image selection.
                .multipleChoice()
                .camera(true)
                .columnCount(3)
                .selectCount(1)
                .onResult(new Action<ArrayList<AlbumFile>>() {
                    @Override
                    public void onAction(@NonNull ArrayList<AlbumFile> result) {
                        filePath = result.get(0).getPath();
                        tvImagePath.setText(filePath);
                    }
                })
                .onCancel(new Action<String>() {
                    @Override
                    public void onAction(@NonNull String result) {
                        // Operation canceled
                    }
                })
                .start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        productRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) { return; }

                products.clear();

                assert value != null;
                for (QueryDocumentSnapshot documentSnapshot : value) {
                    Product product = documentSnapshot.toObject(Product.class);
                    product.setDocId(documentSnapshot.getId());

                    if (product.getUserId().equals(currentUser.getUid())) {
                        products.add(product);
                    }
                }

                collection = new Collection(MainActivity.this, products);
                collection.notifyDataSetChanged();
                lvProducts.setAdapter(collection);
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tvImagePath.getText().toString().isEmpty()) {
                    Uri uri = Uri.fromFile(new File(filePath));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                    String dateTime = sdf.format(new Date());

                    final StorageReference childRef = storageRef.child(dateTime);
                    UploadTask uploadTask = childRef.putFile(uri);
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            childRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    productImageUrl = task.getResult().toString();
                                    Log.v("URL", productImageUrl);
                                    addProduct();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Error", e.getLocalizedMessage());
                            Toast.makeText(MainActivity.this, "Upload Failed -> " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    addProduct();
                }
            }
        });

        btnEdit.setOnClickListener(this::updateProduct);

        btnDelete.setOnClickListener(this::deleteProduct);
    }

    public void addProduct() {
        String name = etName.getText().toString();
        String description = etDescription.getText().toString();
        String price = etPrice.getText().toString();
        String purchased = cbPurchased.isChecked() ? "Purchased" : "Not purchased";

        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("purchased", purchased);
        product.put("filePath", productImageUrl);
        product.put("userId", currentUser.getUid());

        if (!name.isEmpty()) {
            try {
                productRef.add(product);

                etName.setText("");
                etDescription.setText("");
                etPrice.setText("");
                tvImagePath.setText("");
                productImageUrl = "";

                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error" + e, Toast.LENGTH_LONG).show();
                Log.e("Error", e.getLocalizedMessage());
            }
        } else {
            Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateProduct(View view) {
        String name = etName.getText().toString();
        String description = etDescription.getText().toString();
        String price = etPrice.getText().toString();
        String purchased = cbPurchased.isChecked() ? "Purchased" : "Not purchased";

        String filePath = productImageUrl;
        Product product = new Product(name, description, price, purchased, filePath, currentUser.getUid());

        if (!name.isEmpty()) {
            try {
                productRef.document(selectedId).set(product);

                etName.setText("");
                etDescription.setText("");
                etPrice.setText("");
                tvImagePath.setText("");
                productImageUrl = "";

                Toast.makeText(MainActivity.this, "Updated ", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error " + e, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteProduct(View view) {
        if (!selectedId.equals("")) {
            try {
                productRef.document(selectedId).delete();

                etName.setText("");
                etDescription.setText("");
                etPrice.setText("");
                tvImagePath.setText("");
                productImageUrl = "";

                Toast.makeText(MainActivity.this, "Deleted ", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error " + e, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 71 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            File myFile = new File(data.getData().getPath());

            try {
                imageUrl = myFile.toURI().toURL();
                tvImagePath.setText(imageUrl.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = sensorEvent.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            float EG = SensorManager.GRAVITY_EARTH;
            float deviceAccel = (x*x+y*y+z*z)/(EG*EG);

            if (deviceAccel >= 1.7) {
                actualTime = System.currentTimeMillis();
                if ((actualTime - lastUpdated) > 1000) {
                    etName.setText("");
                    etDescription.setText("");
                    etPrice.setText("");
                    tvImagePath.setText("");
                    productImageUrl = "";
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Not applicable.
    }

    @Override
    protected void onStop() {
        super.onStop();

        sensorManager.unregisterListener(this);
    }
}
