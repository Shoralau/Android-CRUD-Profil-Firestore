package com.cindyvalencia.latihanfirestore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText edKode, edNama, edSatuan, edHarga;
    Button btnSimpan;
    RecyclerView recyclerView;
    List<Item> itemList = new ArrayList<>();
    ItemAdapter adapter;

    private FirebaseAuth mAuth;
    private CollectionReference itemsRef;
    private Button btnLogout;
    private ListenerRegistration registration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = currentUser.getUid();
        itemsRef = db.collection("users").document(userId).collection("items");

        edKode = findViewById(R.id.edKode);
        edNama = findViewById(R.id.edNama);
        edSatuan = findViewById(R.id.edSatuan);
        edHarga = findViewById(R.id.edHarga);
        btnSimpan = findViewById(R.id.btnSimpan);
        recyclerView = findViewById(R.id.recyclerView);
        btnLogout = findViewById(R.id.btnLogout);

        adapter = new ItemAdapter(itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSimpan.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String kode = edKode.getText().toString();
                    String nama = edNama.getText().toString();
                    String satuan = edSatuan.getText().toString();
                    String hargaStr = edHarga.getText().toString();

                    if (kode.isEmpty() || nama.isEmpty() || satuan.isEmpty() || hargaStr.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Semua field harus diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int harga = Integer.parseInt(hargaStr);

                    Item item = new Item(kode, nama, satuan, harga);

                    itemsRef.document(kode).set(item)
                            .addOnSuccessListener(aVoid ->{
                                Toast.makeText(MainActivity.this, "Item berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                                edKode.setText("");
                                edNama.setText("");
                                edSatuan.setText("");
                                edHarga.setText("");
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(MainActivity.this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                } else {
                    Toast.makeText(this, "Akun sudah terhapus, silakan login kembali", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();
                    GoogleSignIn.getClient(MainActivity.this, gso).signOut().addOnCompleteListener(signOutTask -> {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    });
                }
            });
        });

        registration = itemsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                if (mAuth.getCurrentUser() != null) {
                    Toast.makeText(MainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (value != null) {
                itemList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Item item = doc.toObject(Item.class);
                    itemList.add(item);
                }
                adapter.notifyDataSetChanged();
            }
        });

        btnLogout.setOnClickListener(v -> {
            if (registration != null) {
                registration.remove();
            }
            mAuth.signOut();
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail().build();
            GoogleSignIn.getClient(MainActivity.this, gso).signOut();
            Toast.makeText(this, "Berhasil Logout!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }
}
