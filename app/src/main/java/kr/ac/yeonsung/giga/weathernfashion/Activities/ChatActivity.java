package kr.ac.yeonsung.giga.weathernfashion.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;
import kr.ac.yeonsung.giga.weathernfashion.Fragment.ChatListFragment;
import kr.ac.yeonsung.giga.weathernfashion.Fragment.PostViewFragment;
import kr.ac.yeonsung.giga.weathernfashion.R;
import kr.ac.yeonsung.giga.weathernfashion.VO.ChatUser;

public class ChatActivity extends AppCompatActivity {
    EditText editText;
    Button button;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    private String chatRoomUid; //????????? ?????? id
    private String myuid;       //?????? id
    private String destUid;     //????????? uid
    private RecyclerView recyclerView;
    private ChatUser destUser;
    List<ChatModel.Comment> comments;
    private int peopleCount;
    ArrayList<String> test = new ArrayList<>(Arrays.asList("0"));
    private String user_profile;
    private String user_name;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyy.MM.dd HH:mm");
    private FirebaseDatabase firebaseDatabase;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();
    StorageReference riversRef = storageRef.child("profile");
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        init();
        sendMsg();

    }
    private void sendMsg()
    {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatModel chatModel = new ChatModel();
                chatModel.users.put(myuid,true);
                chatModel.users.put(destUid,true);
                //push() ???????????? ????????? ?????? ????????? key??? ??????
                if(chatRoomUid == null){
                    Toast.makeText(ChatActivity.this, "????????? ??????", Toast.LENGTH_SHORT).show();
                    button.setEnabled(false);
                    firebaseDatabase.getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });
                }else{
                    sendMsgToDataBase();
                }
            }
        });
    }

    private void checkChatRoom()
    {
        //?????? key == true ?????? chatModel ????????????.
        /* chatModel
        public Map<String,Boolean> users = new HashMap<>(); //????????? ??????
        public Map<String, ChatModel.Comment> comments = new HashMap<>(); //?????? ?????????
        */
        firebaseDatabase.getReference().child("chatrooms").orderByChild("users/"+myuid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()) //???, ????????? id ????????????.
                {
                    ChatModel chatModel = dataSnapshot.getValue(ChatModel.class);
                    if(chatModel.users.containsKey(destUid)){           //????????? id ????????? ????????? ????????? key ?????????
                        chatRoomUid = dataSnapshot.getKey();
                        button.setEnabled(true);

                        //?????????
                        recyclerView.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());

                        //????????? ?????????
                        sendMsgToDataBase();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendMsgToDataBase()
    {
        if(!editText.getText().toString().equals(""))
        {

            ChatModel chatModel2 = new ChatModel();
            chatModel2.chatAlert.put(myuid, false);
            chatModel2.chatAlert.put(destUid, true);
            ChatModel.Comment comment = new ChatModel.Comment();
            comment.uid = myuid;
            comment.message = editText.getText().toString();
            comment.timestamp = ServerValue.TIMESTAMP;



            firebaseDatabase.getReference().child("chatrooms").child(chatRoomUid).child("alert").setValue(chatModel2.chatAlert).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    firebaseDatabase.getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            editText.setText("");
                        }
                    });
                }
            });

        }
    }
    private void init()
    {
        myuid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Intent intent = getIntent();
        destUid = intent.getStringExtra("id");        //?????? ??????

        recyclerView = (RecyclerView)findViewById(R.id.chat_recyclerview);
        button=(Button)findViewById(R.id.send_btn);
        editText = (EditText)findViewById(R.id.send_chat);

        firebaseDatabase = FirebaseDatabase.getInstance();

        if(editText.getText().toString() == null) button.setEnabled(false);
        else button.setEnabled(true);

        checkChatRoom();
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
    {
        List<ChatModel.Comment> comments;

        public RecyclerViewAdapter(){
            comments = new ArrayList<>();

            getDestUid();
        }

        //????????? uid ??????(single) ??????
        private void getDestUid()
        {
            firebaseDatabase.getReference().child("users").child(destUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    user_name = snapshot.child("user_name").getValue().toString();
                    user_profile = snapshot.child("user_profile").getValue().toString();

                    //?????? ?????? ????????????
                    getMessageList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
        public void setReadCounter(final int position, final TextView textView)
        {
            if(peopleCount == 0) {

                firebaseDatabase.getReference().child("chatrooms").child(chatRoomUid).child("users")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //????????? ?????? ????????? ??????????????? ?????????.
                                Map<String, Boolean> users = (Map<String, Boolean>) snapshot.getValue();
                                peopleCount = users.size();
                                System.out.println("peoplecount : " + peopleCount);
                                System.out.println("position : " + comments.get(position).readuser.size());
                                int count = peopleCount - comments.get(position).readuser.size();
                                System.out.println("count : " + count);
                                if (count > 0) {
                                    textView.setVisibility(View.VISIBLE);
                                    textView.setText(String.valueOf(count));
                                    System.out.println("count > 0 : ");
                                } else {
                                    textView.setVisibility(View.INVISIBLE);
                                    System.out.println("count < 0 : ");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }else
            {
                int count = peopleCount - comments.get(position).readuser.size();
                if (count > 0) {
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(String.valueOf(count));
                } else {
                    textView.setVisibility(View.INVISIBLE);
                }
            }
        }
        //?????? ?????? ????????????
        private void getMessageList()
        {
            databaseReference = firebaseDatabase.getReference().child("chatrooms").child(chatRoomUid).child("comments");
            valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    comments.clear();

                    Map<String, Object> readUserMap = new HashMap<>();

                    for(DataSnapshot dataSnapshot : snapshot.getChildren())
                    {
                        String key = dataSnapshot.getKey();
                        ChatModel.Comment commentOrigin = dataSnapshot.getValue(ChatModel.Comment.class);
                        ChatModel.Comment commentModify = dataSnapshot.getValue(ChatModel.Comment.class);

                        commentModify.readuser.put(myuid, true);
                        readUserMap.put(key,commentModify);

                        comments.add(commentOrigin);
                    }

                    //???????????? ??????
                    try{
                    if(!comments.get(comments.size()-1).readuser.containsKey(myuid)){
                        firebaseDatabase.getReference().child("chatrooms").child(chatRoomUid).child("comments").updateChildren(readUserMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        //??????
                                        notifyDataSetChanged();
                                        recyclerView.scrollToPosition(comments.size()-1);
                                    }
                                });
                    }else{
                        notifyDataSetChanged();
                        recyclerView.scrollToPosition(comments.size()-1);
                    }}catch (ArrayIndexOutOfBoundsException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_left,parent,false);
            if (viewType == 1) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_right, parent, false);
            }
            return new ViewHolder(view);
        }
        @Override
        public int getItemViewType(int position){
            if(comments.get(position).uid.equals(myuid)){
                return 1;
            } else {
                return 2;
            }
        }
        @Override
        public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
            ViewHolder viewHolder = ((ViewHolder)holder);


                viewHolder.textViewMsg.setText(comments.get(position).message);

                //????????? ????????? ??????
                riversRef.child(user_profile).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(getApplicationContext()).load(uri)
                                .into(holder.imageViewProfile);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                    }
                });
                viewHolder.textViewName.setText(user_name);
                viewHolder.textViewMsg.setText(comments.get(position).message);

            viewHolder.textViewTimeStamp.setText(getDateTime(position));

            setReadCounter(position, viewHolder.readCount);

        }
        public String getDateTime(int position)
        {
            long unixTime=(long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            return time;
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder
        {
            public TextView textViewMsg;   //????????? ??????
            public TextView textViewName;
            public TextView textViewTimeStamp;
            public TextView readCount;
            public CircleImageView imageViewProfile;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                textViewMsg = (TextView)itemView.findViewById(R.id.item_messagebox_textview_msg);
                textViewName = (TextView)itemView.findViewById(R.id.item_messagebox_TextView_name);
                textViewTimeStamp = (TextView)itemView.findViewById(R.id.item_messagebox_textview_timestamp);
                readCount = itemView.findViewById(R.id.item_messagebox_textview_readCounterLeft);
                imageViewProfile = itemView.findViewById(R.id.item_messagebox_ImageView_profile);
            }
        }
    }

    public static class ChatModel {

        public Map<String,Boolean> users = new HashMap<>(); //????????? ??????
        public Map<String, Comment> comments = new HashMap<>(); //?????? ?????????
        public Map<String, Boolean> chatAlert = new HashMap<>();

        public static class Comment
        {
            public String uid;
            public String message;
            public HashMap<String,Boolean> readuser = new HashMap<>();
            public Object timestamp;

        }
    }

    @Override
    public void onBackPressed() {
        databaseReference.removeEventListener(valueEventListener);
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

//        Bundle result = new Bundle();
//        result.putString("id", user.getUid());
//        FragmentManager fm = getSupportFragmentManager();
//        FragmentTransaction fragmentTransaction;
//        ChatListFragment chatListFragment = new ChatListFragment();
//        chatListFragment.setArguments(result);
//        fragmentTransaction = fm.beginTransaction();
//        fragmentTransaction.addToBackStack(null)
//                .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
//                .replace(R.id.main_ly, chatListFragment)
//                .commit();
    }
}