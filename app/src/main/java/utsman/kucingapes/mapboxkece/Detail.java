package utsman.kucingapes.mapboxkece;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Detail extends AppCompatActivity {
    private TextView judul, sub, lokasi;
    private FirebaseDatabase database;
    private DatabaseReference reference;

    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        judul = findViewById(R.id.judul);
        sub = findViewById(R.id.sub);
        lokasi = findViewById(R.id.loc);

        title = getIntent().getStringExtra("title");

        settingFirebase(title);
    }

    private void settingFirebase(String title) {
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("data").child(title);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String sJudul = dataSnapshot.child("title").getValue(String.class);
                String sSub = dataSnapshot.child("subtitle").getValue(String.class);
                Double dLng = dataSnapshot.child("lng").getValue(Double.class);
                Double dLat = dataSnapshot.child("lat").getValue(Double.class);

                String loc = Double.toString(dLng)+","+Double.toString(dLat);

                judul.setText(sJudul);
                sub.setText(sSub);
                lokasi.setText(loc);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
