package com.example.enesi.nschatapp;

import android.app.ProgressDialog;
import android.media.Image;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName,mProfileStatus,mProfileFriendCount;
    private Button mProfileSendReqBtn,mDeclineBtn;
    private String mCurrent_state;

    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mRootRef;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseUser mCurrent_user;
    private ProgressDialog mProggresDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mCurrent_user = FirebaseAuth.getInstance().getCurrentUser();

        mProfileImage = (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_displayName);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mProfileFriendCount = (TextView) findViewById(R.id.profile_totalFriends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_req_btn);
        mDeclineBtn = (Button) findViewById(R.id.profile_decline_btn);

        mCurrent_state = "not_friends";

        mProggresDialog = new ProgressDialog(this);
        mProggresDialog.setTitle("Kullanıcı Verileri Yükleniyor !");
        mProggresDialog.setMessage("Kullanıcı verileri yüklenirken lütfen bekleyin !");
        mProggresDialog.setCanceledOnTouchOutside(false);
        mProggresDialog.show();

        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.avatar).resize(700,520).into(mProfileImage);

                // -------------------------------------- FRIEND LIST / REQUEST FEATURE --------------------------------------

                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id))
                        {
                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if(req_type.equals("received"))
                            {
                                mCurrent_state = "req_received";
                                mProfileSendReqBtn.setText("Arkadaşlık İsteğini Kabul Et");

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);
                            }

                            else if(req_type.equals("sent"))
                            {
                                mCurrent_state = "req_sent";
                                mProfileSendReqBtn.setText("Arkadaşlık İsteğini Reddet");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            }
                            mProggresDialog.dismiss();

                        }

                        else
                        {
                            mFriendDatabase.child(mCurrent_user.getUid()).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_id))
                                    {
                                        mCurrent_state = "friends";
                                        mProfileSendReqBtn.setText("Bu kişiyi arkadaşlıktan çıkar");

                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);
                                    }
                                    mProggresDialog.dismiss();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    mProggresDialog.dismiss();
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProfileSendReqBtn.setEnabled(false);

                // ------------------------------------ NOT FRIENDS STATE ------------------------------------

                if(mCurrent_state.equals("not_friends"))
                {

                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();
                    String newNotificationId = newNotificationref.getKey();

                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from",mCurrent_user.getUid());
                    notificationData.put("type","request");

                    Map requestMap = new HashMap<>();
                    requestMap.put("friend_req/" + mCurrent_user.getUid() + "/" + user_id + "request_type","sent");
                    requestMap.put("friend_req/" + user_id + "/" + mCurrent_user.getUid() + "request_type","received");
                    requestMap.put("notification/" + user_id + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError != null)
                            {
                                Toast.makeText(ProfileActivity.this, "İstek gönderilirken bir sorun oluştu.", Toast.LENGTH_LONG).show();
                            }

                        }
                    });
                }

                // ------------------------------------ CANCEL REQUEST STATE ------------------------------------

                if(mCurrent_state.equals("req_sent"))
                {
                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("Arkadaşlık İsteği Gönder");

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);

                                }
                            });

                        }
                    });
                }

                // ------------------------- REQ RECEIVED STATE -------------------------

                if(mCurrent_state.equals("req_received"))
                {
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    mFriendDatabase.child(mCurrent_user.getUid()).child(user_id).setValue(currentDate)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mFriendDatabase.child(user_id).child(mCurrent_user.getUid()).setValue(currentDate)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {

                                                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {

                                                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {

                                                                    mProfileSendReqBtn.setEnabled(true);
                                                                    mCurrent_state = "friends";
                                                                    mProfileSendReqBtn.setText("Bu kişiyi arkadaşlıktan çıkar");

                                                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                                                    mDeclineBtn.setEnabled(false);

                                                                }
                                                            });

                                                        }
                                                    });

                                                }
                                            });
                                }
                            });
                }

            }
        });


    }
}
